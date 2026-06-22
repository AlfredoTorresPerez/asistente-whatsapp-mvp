package com.asistentewhatsapp.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "summary", nullable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected AuditLog() {
    }

    public AuditLog(UUID id, UUID businessId, UUID actorUserId, String actionType, String entityType, UUID entityId,
                    String summary, String metadata, OffsetDateTime occurredAt) {
        this.id = id;
        this.businessId = businessId;
        this.actorUserId = actorUserId;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary;
        this.metadata = metadata;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public UUID getActorUserId() { return actorUserId; }
    public String getActionType() { return actionType; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getSummary() { return summary; }
    public String getMetadata() { return metadata; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
