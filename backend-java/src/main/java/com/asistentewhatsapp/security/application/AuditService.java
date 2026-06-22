package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.domain.AuditLog;
import com.asistentewhatsapp.security.infrastructure.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final UUID SYSTEM_BUSINESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(UUID businessId, UUID actorUserId, String actionType, String entityType, UUID entityId, String summary) {
        record(businessId, actorUserId, actionType, entityType, entityId, summary, Map.of());
    }

    @Transactional
    public void record(UUID businessId, UUID actorUserId, String actionType, String entityType, UUID entityId, String summary,
            Map<String, Object> metadata) {
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(),
                businessId != null ? businessId : SYSTEM_BUSINESS_ID,
                actorUserId,
                actionType,
                entityType,
                entityId,
                summary,
                toJson(metadata),
                OffsetDateTime.now()));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> recent(CurrentUser user, int size) {
        return auditLogRepository.findByBusinessIdOrderByOccurredAtDesc(user.businessId(), PageRequest.of(0, Math.min(size, 100)))
                .stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActionType(),
                        log.getEntityType(),
                        log.getSummary(),
                        log.getOccurredAt()))
                .toList();
    }

    public record AuditLogResponse(UUID id, String actionType, String entityType, String summary, OffsetDateTime occurredAt) {}

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar metadata de auditoria.", exception);
        }
    }
}
