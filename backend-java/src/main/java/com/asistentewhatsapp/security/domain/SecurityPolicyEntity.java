package com.asistentewhatsapp.security.domain;

import com.asistentewhatsapp.shared.persistence.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "security_policy")
public class SecurityPolicyEntity extends BusinessScopedEntity {

	@Id
	private UUID id;

	@Column(name = "session_timeout_minutes", nullable = false)
	private int sessionTimeoutMinutes;

	@Column(name = "password_min_length", nullable = false)
	private int passwordMinLength;

	@Column(name = "require_uppercase", nullable = false)
	private boolean requireUppercase;

	@Column(name = "require_number", nullable = false)
	private boolean requireNumber;

	@Column(name = "require_symbol", nullable = false)
	private boolean requireSymbol;

	@Column(name = "max_failed_login_attempts", nullable = false)
	private int maxFailedLoginAttempts;

	public UUID getId() {
		return id;
	}

	public int getSessionTimeoutMinutes() {
		return sessionTimeoutMinutes;
	}

	public int getPasswordMinLength() {
		return passwordMinLength;
	}

	public boolean isRequireUppercase() {
		return requireUppercase;
	}

	public boolean isRequireNumber() {
		return requireNumber;
	}

	public boolean isRequireSymbol() {
		return requireSymbol;
	}

	public int getMaxFailedLoginAttempts() {
		return maxFailedLoginAttempts;
	}
}
