package com.asistentewhatsapp.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class BusinessScopedEntity extends AuditableEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    public UUID getBusinessId() {
        return businessId;
    }

    protected void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }
}
