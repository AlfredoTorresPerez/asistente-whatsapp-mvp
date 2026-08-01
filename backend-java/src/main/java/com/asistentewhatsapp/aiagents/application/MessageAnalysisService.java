package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository.DetectedIntentRecord;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository.MessageAnalysisRecord;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Registra el analisis canonico de cada mensaje procesado por el motor de IA:
 * ai_message_analysis (1 por turno) y su ranking de ai_detected_intent. El
 * detector_type refleja la fuente real de la deteccion (DATABASE o
 * JAVA_FALLBACK; AI_MODEL/HUMAN_VALIDATION quedan para fases futuras).
 */
@Service
public class MessageAnalysisService {

	private static final String DEFAULT_LANGUAGE = "es";
	private static final String DEFAULT_COUNTRY = "CL";

	private final AiAgentJdbcRepository repository;

	public MessageAnalysisService(AiAgentJdbcRepository repository) {
		this.repository = repository;
	}

	public UUID record(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> extractedEntities) {
		if (request == null || request.businessId() == null || intent == null) {
			return null;
		}
		String normalized = TextNormalizer.normalize(request.messageBody());
		String detectorType = "DATABASE".equals(intent.detectorSource()) ? "DATABASE" : "JAVA_FALLBACK";
		String primaryCode = mapToCatalogCode(intent.primaryIntent());
		if (primaryCode == null) {
			return null;
		}

		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("detector_type", detectorType);
		payload.put("intent_code", primaryCode);
		if (intent.secondaryIntent() != null) {
			payload.put("secondary_intent_code", mapToCatalogCode(intent.secondaryIntent()));
		}
		if (intent.handoffReason() != null) {
			payload.put("handoff_reason", intent.handoffReason());
		}
		if (extractedEntities != null) {
			extractedEntities.forEach((key, value) -> payload.put("entity_" + key, value));
		}

		UUID analysisId = repository
				.insertMessageAnalysis(new MessageAnalysisRecord(request.businessId(), request.conversationId(),
						request.customerId(), request.channelAccountId(), detectorType, DEFAULT_LANGUAGE,
						DEFAULT_COUNTRY, normalized, countTokens(normalized), ambiguity(intent.confidence()), payload));

		repository.insertDetectedIntent(analysisId, new DetectedIntentRecord(request.businessId(), primaryCode, 1, true,
				intent.confidence(), detectorType, null));
		if (intent.secondaryIntent() != null) {
			String secondaryCode = mapToCatalogCode(intent.secondaryIntent());
			if (secondaryCode != null) {
				repository.insertDetectedIntent(analysisId, new DetectedIntentRecord(request.businessId(),
						secondaryCode, 2, false, intent.confidence() * 0.8, detectorType, null));
			}
		}
		AiTraceLogger.info("MESSAGE_ANALYSIS_RECORDED", AiTraceLogger.traceId(request), request.conversationId(), null,
				"MessageAnalysisService", "analysisId=" + analysisId + " intent=" + primaryCode + " detector="
						+ detectorType + " confidence=" + intent.confidence());
		return analysisId;
	}

	private int countTokens(String normalized) {
		if (normalized == null || normalized.isBlank()) {
			return 0;
		}
		return normalized.trim().split("\\s+").length;
	}

	private BigDecimal ambiguity(double confidence) {
		return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, 1.0 - confidence)));
	}

	private static String mapToCatalogCode(AgentIntent intent) {
		if (intent == null) {
			return null;
		}
		return AGENT_INTENT_TO_CATALOG_CODE.get(intent);
	}

	private static final Map<AgentIntent, String> AGENT_INTENT_TO_CATALOG_CODE = Map.ofEntries(
			Map.entry(AgentIntent.BOOKING_REQUEST, "BOOKING_CREATE"),
			Map.entry(AgentIntent.BOOKING_CHANGE, "BOOKING_RESCHEDULE"),
			Map.entry(AgentIntent.BOOKING_CANCEL, "BOOKING_CANCEL"),
			Map.entry(AgentIntent.AVAILABILITY_QUERY, "BOOKING_AVAILABILITY"),
			Map.entry(AgentIntent.BOOKING_STATUS, "BOOKING_STATUS"),
			Map.entry(AgentIntent.SERVICE_INFORMATION, "SERVICE_INFORMATION"),
			Map.entry(AgentIntent.PRICE_REQUEST, "SERVICE_PRICE"),
			Map.entry(AgentIntent.BUSINESS_HOURS_QUERY, "BUSINESS_HOURS"),
			Map.entry(AgentIntent.LOCATION_QUERY, "BUSINESS_LOCATION"),
			Map.entry(AgentIntent.PAYMENT_INQUIRY, "PAYMENT_INFORMATION"), Map.entry(AgentIntent.GREETING, "GREETING"),
			Map.entry(AgentIntent.THANKS_OR_FAREWELL, "THANKS"), Map.entry(AgentIntent.HUMAN_REQUEST, "HUMAN_REQUEST"),
			Map.entry(AgentIntent.COMMERCIAL_INQUIRY, "COMMERCIAL_INQUIRY"),
			Map.entry(AgentIntent.SERVICE_RECOMMENDATION, "SERVICE_RECOMMENDATION"),
			Map.entry(AgentIntent.PROFESSIONAL_QUERY, "PROFESSIONAL_QUERY"),
			Map.entry(AgentIntent.QUOTE_REQUEST, "QUOTE_REQUEST"),
			Map.entry(AgentIntent.PAYMENT_PROBLEM, "PAYMENT_PROBLEM"),
			Map.entry(AgentIntent.SUPPORT_GENERAL, "SUPPORT_GENERAL"),
			Map.entry(AgentIntent.TECHNICAL_MESSAGE, "TECHNICAL_MESSAGE"),
			Map.entry(AgentIntent.KNOWLEDGE_QUERY, "KNOWLEDGE_QUERY"), Map.entry(AgentIntent.FOLLOW_UP, "FOLLOW_UP"),
			Map.entry(AgentIntent.COMPLAINT, "COMPLAINT"), Map.entry(AgentIntent.WAITLIST_QUERY, "WAITLIST_QUERY"),
			Map.entry(AgentIntent.COMMERCIAL_AND_BOOKING, "COMMERCIAL_INQUIRY"),
			Map.entry(AgentIntent.AMBIGUOUS, "UNKNOWN"));
}
