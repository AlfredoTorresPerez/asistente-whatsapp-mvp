package com.asistentewhatsapp.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_account")
public class UserAccount {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "phone")
	private String phone;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "timezone", nullable = false)
	private String timezone;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new LinkedHashSet<>();

	public UUID getId() {
		return id;
	}

	public Business getBusiness() {
		return business;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getTimezone() {
		return timezone;
	}

	public String getStatus() {
		return status;
	}

	public OffsetDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public String displayName() {
		return firstName + " " + lastName;
	}

	public void registerSuccessfulLogin(OffsetDateTime when) {
		this.lastLoginAt = when;
		this.failedLoginAttempts = 0;
	}

	public void registerFailedLogin() {
		this.failedLoginAttempts = failedLoginAttempts + 1;
	}

	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}
