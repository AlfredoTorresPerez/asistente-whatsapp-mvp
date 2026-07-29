package com.asistentewhatsapp.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

	@Id
	private UUID id;

	@Column(name = "business_id", nullable = false)
	private UUID businessId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private OffsetDateTime expiresAt;

	@Column(name = "consumed_at")
	private OffsetDateTime consumedAt;

	protected PasswordResetToken() {
	}

	public PasswordResetToken(UUID id, UUID businessId, UserAccount user, String tokenHash, OffsetDateTime expiresAt) {
		this.id = id;
		this.businessId = businessId;
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getBusinessId() {
		return businessId;
	}

	public UserAccount getUser() {
		return user;
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

	public boolean isUsable(OffsetDateTime now) {
		return consumedAt == null && expiresAt.isAfter(now);
	}

	public void consume(OffsetDateTime when) {
		this.consumedAt = when;
	}
}
