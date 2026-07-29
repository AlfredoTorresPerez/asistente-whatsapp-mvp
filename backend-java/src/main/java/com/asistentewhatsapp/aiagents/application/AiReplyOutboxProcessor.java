package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aesthetic.application.AestheticCenterService;
import com.asistentewhatsapp.aiagents.infrastructure.AiReplyOutboxJdbcRepository;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebChannelJdbcRepository;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AiReplyOutboxProcessor {

	private final AiReplyOutboxJdbcRepository outboxRepository;
	private final WhatsAppWebChannelJdbcRepository channelRepository;
	private final AestheticCenterService aestheticCenterService;
	private final AgentCoordinatorService agentCoordinatorService;
	private final ChannelDispatchService channelDispatchService;
	private final AiAgentProperties properties;
	private final int batchSize;
	private final long processingTimeoutMs;
	private final long baseRetryDelayMs;
	private final long maxRetryDelayMs;
	private final JdbcTemplate jdbcTemplate;

	public AiReplyOutboxProcessor(AiReplyOutboxJdbcRepository outboxRepository,
			WhatsAppWebChannelJdbcRepository channelRepository, AestheticCenterService aestheticCenterService,
			AgentCoordinatorService agentCoordinatorService, ChannelDispatchService channelDispatchService,
			AiAgentProperties properties, JdbcTemplate jdbcTemplate,
			@Value("${app.ai.agents.outbox-batch-size:10}") int batchSize,
			@Value("${app.ai.agents.outbox-processing-timeout-ms:120000}") long processingTimeoutMs,
			@Value("${app.ai.agents.outbox-retry-base-delay-ms:30000}") long baseRetryDelayMs,
			@Value("${app.ai.agents.outbox-retry-max-delay-ms:900000}") long maxRetryDelayMs) {
		this.outboxRepository = outboxRepository;
		this.channelRepository = channelRepository;
		this.aestheticCenterService = aestheticCenterService;
		this.agentCoordinatorService = agentCoordinatorService;
		this.channelDispatchService = channelDispatchService;
		this.properties = properties;
		this.jdbcTemplate = jdbcTemplate;
		this.batchSize = batchSize;
		this.processingTimeoutMs = processingTimeoutMs;
		this.baseRetryDelayMs = baseRetryDelayMs;
		this.maxRetryDelayMs = maxRetryDelayMs;
	}

	@Scheduled(fixedDelayString = "${app.ai.agents.outbox-worker-interval-ms:5000}")
	public void processDueJobs() {
		String correlationId = AiTraceLogger.newTraceId("JOB-AI-OUTBOX");
		Instant pollStart = Instant.now();
		try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
			List<AiReplyOutboxJdbcRepository.AiReplyOutboxJob> jobs = outboxRepository.claimDueJobs(batchSize,
					processingTimeoutMs);
			if (jobs.isEmpty()) {
				AiTraceLogger.debug("AI_OUTBOX_WORKER_IDLE", correlationId, null, null, "AiReplyOutboxProcessor",
						"claimedJobs=0 batchSize=" + batchSize + " intervalMs="
								+ properties.getOutboxWorkerIntervalMs());
				return;
			}
			long claimedCount = jobs.size();
			AiTraceLogger.info("AI_OUTBOX_WORKER_CLAIMED", correlationId, null, null, "AiReplyOutboxProcessor",
					"claimedJobs=" + claimedCount + " batchSize=" + batchSize);

			int processed = 0;
			int failed = 0;
			for (AiReplyOutboxJdbcRepository.AiReplyOutboxJob job : jobs) {
				try {
					boolean completed = processJob(job);
					if (completed) {
						outboxRepository.markProcessed(job.id(), OffsetDateTime.now(ZoneOffset.UTC));
						processed++;
					} else {
						failed++;
					}
				} catch (RuntimeException exception) {
					failed++;
					OffsetDateTime nextAttemptAt = OffsetDateTime.now(ZoneOffset.UTC).plus(retryDelay(job.attempts()));
					outboxRepository.markFailedOrRetry(job.id(), job.attempts(), job.maxAttempts(),
							exception.getClass().getSimpleName(), exception.getMessage(), nextAttemptAt);
					AiTraceLogger.error("AI_OUTBOX_JOB_FAILED", job.traceId(), job.conversationId(),
							job.inboundMessageId(), "AiReplyOutboxProcessor",
							"attempt=" + job.attempts() + " maxAttempts=" + job.maxAttempts() + " nextAttemptAt="
									+ nextAttemptAt + " errorType=" + exception.getClass().getSimpleName(),
							exception);
				}
			}
			long pollDurationMs = Duration.between(pollStart, Instant.now()).toMillis();
			AiTraceLogger.info("AI_OUTBOX_WORKER_COMPLETED", correlationId, null, null, "AiReplyOutboxProcessor",
					"claimed=" + claimedCount + " processed=" + processed + " failed=" + failed + " durationMs="
							+ pollDurationMs);
		}
	}

	public AiOutboxStats getStats() {
		String sql = """
				select
				    count(*) filter (where status = 'PENDING') as pending,
				    count(*) filter (where status = 'PROCESSING') as processing,
				    count(*) filter (where status = 'FAILED') as failed,
				    min(created_at) filter (where status = 'PENDING') as oldest_pending
				from ai_reply_outbox
				where status in ('PENDING', 'PROCESSING', 'FAILED')
				""";
		return jdbcTemplate
				.query(sql,
						(rs, rowNum) -> new AiOutboxStats(rs.getLong("pending"), rs.getLong("processing"),
								rs.getLong("failed"), rs.getObject("oldest_pending", OffsetDateTime.class)))
				.stream().findFirst().orElse(new AiOutboxStats(0, 0, 0, null));
	}

	public record AiOutboxStats(long pending, long processing, long failed, OffsetDateTime oldestPendingCreatedAt) {
		public long oldestAgeSeconds() {
			if (oldestPendingCreatedAt == null)
				return 0;
			return Duration.between(oldestPendingCreatedAt, OffsetDateTime.now(ZoneOffset.UTC)).getSeconds();
		}
	}

	private boolean processJob(AiReplyOutboxJdbcRepository.AiReplyOutboxJob job) {
		if (!agentCoordinatorService.autoReplyEnabled()) {
			outboxRepository.markSkipped(job.id(), "AI_AUTO_REPLY_DISABLED", OffsetDateTime.now(ZoneOffset.UTC));
			AiTraceLogger.warn("AI_OUTBOX_JOB_SKIPPED", job.traceId(), job.conversationId(), job.inboundMessageId(),
					"AiReplyOutboxProcessor", "reason=AI_AUTO_REPLY_DISABLED");
			return false;
		}

		WhatsAppWebChannelJdbcRepository.ConversationRecord conversation = channelRepository
				.findConversationById(job.businessId(), job.conversationId()).orElseThrow(
						() -> new IllegalStateException("No se encontro la conversacion asociada a la cola de IA."));

		try {
			aestheticCenterService.analyzeInboundMessage(job.businessId(), job.customerId(), job.conversationId(),
					job.messageBody());
		} catch (RuntimeException exception) {
			AiTraceLogger.warn("AESTHETIC_ANALYSIS_SKIPPED", job.traceId(), job.conversationId(),
					job.inboundMessageId(), "AiReplyOutboxProcessor",
					"errorType=" + exception.getClass().getSimpleName()
							+ " functionalMessage=La clasificacion estetica no debe bloquear la respuesta IA.");
		}

		AgentConversationRequest request = new AgentConversationRequest(job.businessId(), job.channelAccountId(),
				job.conversationId(), job.customerId(), job.recipientPhone(), job.customerDisplayName(),
				job.messageBody(), OffsetDateTime.now(ZoneOffset.UTC), job.locationId(), job.locationName(),
				job.traceId(), false);

		agentCoordinatorService.route(request)
				.ifPresent(result -> sendAiResponse(job, conversation.assignedUserId(), result));
		return true;
	}

	private void sendAiResponse(AiReplyOutboxJdbcRepository.AiReplyOutboxJob job, UUID assignedUserId,
			AgentRoutingResult result) {
		String responseBody = result.responseToCustomer();
		if (responseBody == null || responseBody.isBlank()) {
			AiTraceLogger.warn("AI_OUTBOX_EMPTY_RESPONSE", job.traceId(), job.conversationId(), job.inboundMessageId(),
					"AiReplyOutboxProcessor", "agent=" + result.agentType() + " intent=" + result.primaryIntent());
			return;
		}

		AiTraceLogger.info("WHATSAPP_RESPONSE_SEND_STARTED", job.traceId(), job.conversationId(), null,
				"AiReplyOutboxProcessor", "phoneMasked=" + AiTraceLogger.maskPhone(job.recipientPhone())
						+ " messageLength=" + responseBody.length() + " responseType=AI_AUTO_REPLY_OUTBOX");

		UUID messageId = channelRepository.insertOutboundMessage(job.businessId(), job.conversationId(), assignedUserId,
				responseBody, OffsetDateTime.now(ZoneOffset.UTC));

		if (properties.safeModeEnabled()) {
			channelRepository.updateOutboundMessageAccepted(messageId, "safe-mode-simulated-" + UUID.randomUUID(),
					"SIMULATED", OffsetDateTime.now(ZoneOffset.UTC));
			channelRepository.updateConversationOutboundActivity(job.conversationId(), responseBody,
					OffsetDateTime.now(ZoneOffset.UTC));
			channelRepository.insertMessageDeliveryLog(job.businessId(), messageId, "SIMULATED", "safe-mode",
					Map.of("source", "AI_REPLY_OUTBOX", "agentType", result.agentType().name(), "primaryIntent",
							result.primaryIntent().name(), "outboxId", job.id().toString(), "safeMode", "true"),
					OffsetDateTime.now(ZoneOffset.UTC));
			AiTraceLogger.info("WHATSAPP_RESPONSE_SEND_RESULT", job.traceId(), job.conversationId(), messageId,
					"AiReplyOutboxProcessor",
					"sent=true adapterStatus=SIMULATED safeMode=true messageLength=" + responseBody.length());
			AiTraceLogger.info("AI_SAFE_MODE_RESPONSE", job.traceId(), job.conversationId(), messageId,
					"AiReplyOutboxProcessor", "safeModeResponse=" + LogSanitizer.responseSummary(responseBody));
			return;
		}

		try {
			ChannelDispatchResponse delivery = channelDispatchService.dispatch(new ChannelDispatchRequest(
					job.businessId(), MessageChannelType.WHATSAPP, job.recipientPhone(), responseBody));
			OffsetDateTime sentAt = delivery.acceptedAt() == null
					? OffsetDateTime.now(ZoneOffset.UTC)
					: OffsetDateTime.ofInstant(delivery.acceptedAt(), ZoneOffset.UTC);
			String deliveryStatus = normalizeDeliveryStatus(delivery.status());
			channelRepository.updateOutboundMessageAccepted(messageId, delivery.externalMessageId(), deliveryStatus,
					sentAt);
			channelRepository.updateConversationOutboundActivity(job.conversationId(), responseBody, sentAt);
			channelRepository.insertMessageDeliveryLog(job.businessId(), messageId, deliveryStatus,
					delivery.externalMessageId(),
					Map.of("source", "AI_REPLY_OUTBOX", "agentType", result.agentType().name(), "primaryIntent",
							result.primaryIntent().name(), "outboxId", job.id().toString()),
					sentAt);
			AiTraceLogger.info("WHATSAPP_RESPONSE_SEND_RESULT", job.traceId(), job.conversationId(), messageId,
					"AiReplyOutboxProcessor",
					"sent=true adapterStatus=" + deliveryStatus + " externalMessageIdMasked="
							+ com.asistentewhatsapp.shared.observability.LogSanitizer
									.maskExternalId(delivery.externalMessageId()));
		} catch (RuntimeException exception) {
			channelRepository.updateOutboundMessageFailed(messageId, "AI_REPLY_OUTBOX_DISPATCH_FAILED",
					OffsetDateTime.now(ZoneOffset.UTC));
			throw exception;
		}
	}

	private Duration retryDelay(int attempts) {
		long multiplier = Math.max(1L, 1L << Math.min(attempts - 1, 8));
		long delay = Math.min(baseRetryDelayMs * multiplier, maxRetryDelayMs);
		return Duration.ofMillis(delay);
	}

	private String normalizeDeliveryStatus(String status) {
		if (status == null || status.isBlank()) {
			return "FAILED";
		}
		return switch (status) {
			case "QUEUED", "SENT", "DELIVERED", "READ", "FAILED" -> status;
			case "PROVIDER_ACCEPTED", "ACCEPTED" -> "SENT";
			default -> "FAILED";
		};
	}
}
