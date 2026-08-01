package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.infrastructure.CanonicalEntityJdbcRepository;
import com.asistentewhatsapp.aiagents.infrastructure.CanonicalEntityJdbcRepository.DetectedEntityRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Registra las entidades resueltas de cada mensaje en ai_detected_entity,
 * vinculadas al analisis (ai_message_analysis) y a la entidad canonica
 * (ai_canonical_entity). El orden de resolucion es: (1) la resolucion aplicada
 * por la extraccion (DATABASE/ALIAS con canonical_entity_id y matched_alias via
 * claves auxiliares), (2) coincidencia exacta del valor contra canonical_name
 * (DATABASE), (3) match por alias con scoring (ALIAS) y (4) PATTERN sin
 * canonical.
 */
@Service
public class DetectedEntityService {

	private static final List<String> TRACKED_ENTITY_KEYS = List.of("servicio_o_producto", "sede", "profesional",
			"fecha", "fecha_relativa", "hora", "tramo_horario", "preferencia_horaria", "nombre", "telefono");
	private static final BigDecimal DEFAULT_CONFIDENCE = BigDecimal.valueOf(0.85);

	private final CanonicalEntityJdbcRepository canonicalEntityJdbcRepository;
	private final CanonicalEntityService canonicalEntityService;

	public DetectedEntityService(CanonicalEntityJdbcRepository canonicalEntityJdbcRepository,
			CanonicalEntityService canonicalEntityService) {
		this.canonicalEntityJdbcRepository = canonicalEntityJdbcRepository;
		this.canonicalEntityService = canonicalEntityService;
	}

	public void record(UUID messageAnalysisId, AgentConversationRequest request, Map<String, String> entities) {
		if (messageAnalysisId == null || request == null || request.businessId() == null || entities == null
				|| entities.isEmpty()) {
			return;
		}
		for (String key : TRACKED_ENTITY_KEYS) {
			String value = entities.get(key);
			if (value == null || value.isBlank()) {
				continue;
			}
			recordEntity(request, messageAnalysisId, key, value, entities);
		}
	}

	private void recordEntity(AgentConversationRequest request, UUID analysisId, String entityKey, String value,
			Map<String, String> entities) {
		Resolution resolution = resolve(request, entityKey, value, entities);
		DetectedEntityRecord entity = new DetectedEntityRecord(request.businessId(), resolution.canonicalEntityId(),
				resolution.entityType(), entityKey, value, resolution.method(), resolution.matchedAlias(),
				resolution.confidence(), null, null);
		canonicalEntityJdbcRepository.insertDetectedEntity(analysisId, entity);
	}

	private Resolution resolve(AgentConversationRequest request, String entityKey, String value,
			Map<String, String> entities) {
		String canonicalIdAux = entities.get(entityKey + "_canonical_id");
		if (canonicalIdAux != null && !canonicalIdAux.isBlank()) {
			String resolution = entities.get(entityKey + "_resolution");
			String matchedAlias = entities.get(entityKey + "_matched_alias");
			String confidenceAux = entities.get(entityKey + "_confidence");
			return new Resolution(UUID.fromString(canonicalIdAux), mapEntityType(entityKey),
					"ALIAS".equals(resolution) ? "ALIAS" : "DATABASE", matchedAlias,
					confidenceAux == null ? DEFAULT_CONFIDENCE : new BigDecimal(confidenceAux));
		}
		UUID canonicalId = canonicalEntityJdbcRepository.findIdByCanonicalName(request.businessId(), value)
				.orElse(null);
		if (canonicalId != null) {
			return new Resolution(canonicalId, mapEntityType(entityKey), "DATABASE", null, DEFAULT_CONFIDENCE);
		}
		return canonicalEntityService.resolveValueByAlias(request.businessId(), value)
				.map(match -> new Resolution(match.canonicalEntityId(), mapEntityType(entityKey), "ALIAS",
						match.matchedAlias(), match.confidence()))
				.orElse(new Resolution(null, mapEntityType(entityKey), "PATTERN", null, DEFAULT_CONFIDENCE));
	}

	private static String mapEntityType(String entityKey) {
		return switch (entityKey) {
			case "servicio_o_producto" -> "SERVICE";
			case "sede" -> "LOCATION";
			case "profesional" -> "PROFESSIONAL";
			case "fecha", "fecha_relativa" -> "RELATIVE_DATE";
			case "hora", "tramo_horario" -> "TIME";
			case "preferencia_horaria" -> "PREFERENCE";
			case "nombre" -> "PERSON";
			case "telefono" -> "CONTACT";
			default -> "OTHER";
		};
	}

	private record Resolution(UUID canonicalEntityId, String entityType, String method, String matchedAlias,
			BigDecimal confidence) {
	}
}
