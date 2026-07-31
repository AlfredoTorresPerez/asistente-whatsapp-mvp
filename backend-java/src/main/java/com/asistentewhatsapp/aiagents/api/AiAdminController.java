package com.asistentewhatsapp.aiagents.api;

import com.asistentewhatsapp.aiagents.application.AgentCoordinatorService;
import com.asistentewhatsapp.aiagents.application.AiReplyOutboxProcessor;
import com.asistentewhatsapp.aiagents.application.AgentConversationRequest;
import com.asistentewhatsapp.aiagents.application.AgentRoutingResult;
import com.asistentewhatsapp.aiagents.application.AiTraceLogger;
import com.asistentewhatsapp.administration.application.AdminAccessGuard;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/ai", produces = MediaType.APPLICATION_JSON_VALUE)
public class AiAdminController {

	private final AiReplyOutboxProcessor outboxProcessor;
	private final AgentCoordinatorService agentCoordinatorService;

	public AiAdminController(AiReplyOutboxProcessor outboxProcessor, AgentCoordinatorService agentCoordinatorService) {
		this.outboxProcessor = outboxProcessor;
		this.agentCoordinatorService = agentCoordinatorService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BUSINESS_AI_AUDIT_VIEW')")
	@GetMapping("/outbox/stats")
	public AiReplyOutboxProcessor.AiOutboxStats getOutboxStats(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		return outboxProcessor.getStats();
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BUSINESS_AI_TEST')")
	@PostMapping("/preview")
	public AiPreviewResponse preview(@RequestBody AiPreviewRequest request,
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);

		AgentConversationRequest agentRequest = new AgentConversationRequest(authenticatedUser.businessId(),
				request.channelAccountId(),
				request.conversationId() != null ? request.conversationId() : UUID.randomUUID(),
				request.customerId() != null ? request.customerId() : UUID.randomUUID(), request.customerPhone(),
				request.customerName(), request.message(), java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC),
				request.selectedLocationId(), request.selectedLocationName(), AiTraceLogger.newTraceId("AI-PREVIEW"),
				true, null);

		var result = agentCoordinatorService.preview(agentRequest);
		if (result.isEmpty()) {
			return new AiPreviewResponse(null, "AI_PREVIEW_SKIPPED", "IA deshabilitada o mensaje no accionable");
		}
		AgentRoutingResult routing = result.get();
		return new AiPreviewResponse(routing, "OK", "Preview generado sin persistir");
	}

	public record AiPreviewRequest(String message, UUID conversationId, UUID channelAccountId, UUID customerId,
			String customerPhone, String customerName, UUID selectedLocationId, String selectedLocationName) {
	}

	public record AiPreviewResponse(AgentRoutingResult result, String status, String message) {
	}
}