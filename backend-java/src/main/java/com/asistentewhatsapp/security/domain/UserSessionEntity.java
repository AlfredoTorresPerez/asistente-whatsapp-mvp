package com.asistentewhatsapp.security.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserSessionEntity {
	private UUID id;
	private UUID businessId;
	private UUID userId;
	private String refreshTokenHash;
	private String deviceInfo;
	private String ipAddress;
	private OffsetDateTime createdAt;
	private OffsetDateTime lastUsedAt;
	private OffsetDateTime expiresAt;
	private OffsetDateTime revokedAt;
	private UUID revokedBy;

	public UserSessionEntity() {
	}

	public UserSessionEntity(UUID id, UUID businessId, UUID userId, String refreshTokenHash, String deviceInfo,
			String ipAddress, OffsetDateTime createdAt, OffsetDateTime lastUsedAt, OffsetDateTime expiresAt) {
		this.id = id;
		this.businessId = businessId;
		this.userId = userId;
		this.refreshTokenHash = refreshTokenHash;
		this.deviceInfo = deviceInfo;
		this.ipAddress = ipAddress;
		this.createdAt = createdAt;
		this.lastUsedAt = lastUsedAt;
		this.expiresAt = expiresAt;
	}

	public boolean isActive(OffsetDateTime now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void markUsed(OffsetDateTime now) {
		this.lastUsedAt = now;
	}

	public void revoke(OffsetDateTime now, UUID revokedBy) {
		this.revokedAt = now;
		this.revokedBy = revokedBy;
	}

	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public UUID getBusinessId() {
		return businessId;
	}
	public void setBusinessId(UUID businessId) {
		this.businessId = businessId;
	}
	public UUID getUserId() {
		return userId;
	}
	public void setUserId(UUID userId) {
		this.userId = userId;
	}
	public String getRefreshTokenHash() {
		return refreshTokenHash;
	}
	public void setRefreshTokenHash(String refreshTokenHash) {
		this.refreshTokenHash = refreshTokenHash;
	}
	public String getDeviceInfo() {
		return deviceInfo;
	}
	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public OffsetDateTime getLastUsedAt() {
		return lastUsedAt;
	}
	public void setLastUsedAt(OffsetDateTime lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}
	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}
	public void setExpiresAt(OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}
	public OffsetDateTime getRevokedAt() {
		return revokedAt;
	}
	public void setRevokedAt(OffsetDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}
	public UUID getRevokedBy() {
		return revokedBy;
	}
	public void setRevokedBy(UUID revokedBy) {
		this.revokedBy = revokedBy;
	}
}
