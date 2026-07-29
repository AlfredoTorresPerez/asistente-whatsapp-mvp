package com.asistentewhatsapp.security.domain;

import com.asistentewhatsapp.shared.persistence.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetTokenEntity extends BusinessScopedEntity {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "token_hash", nullable = false)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private OffsetDateTime expiresAt;

	@Column(name = "consumed_at")
	private OffsetDateTime consumedAt;

	public static PasswordResetTokenEntity create(UUID id, UUID businessId, UUID userId, String tokenHash,
			OffsetDateTime expiresAt) {
		PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
		entity.id = id;
		entity.setBusinessId(businessId);
		entity.userId = userId;
		entity.tokenHash = tokenHash;
		entity.expiresAt = expiresAt;
		return entity;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public OffsetDateTime getConsumedAt() {
		return consumedAt;
	}

	public boolean isAvailable(OffsetDateTime referenceTime) {
		return consumedAt == null && expiresAt.isAfter(referenceTime);
	}

	public void markConsumed(OffsetDateTime consumedAt) {
		this.consumedAt = consumedAt;
	}
}
