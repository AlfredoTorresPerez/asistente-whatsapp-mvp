package com.asistentewhatsapp.security.application;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CurrentUser implements UserDetails {

	private final UUID id;
	private final UUID businessId;
	private final String businessName;
	private final String firstName;
	private final String lastName;
	private final String email;
	private final String password;
	private final String timezone;
	private final List<GrantedAuthority> authorities;
	private final boolean enabled;

	public CurrentUser(UUID id, UUID businessId, String businessName, String firstName, String lastName, String email,
			String password, String timezone, List<GrantedAuthority> authorities, boolean enabled) {
		this.id = id;
		this.businessId = businessId;
		this.businessName = businessName;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.password = password;
		this.timezone = timezone;
		this.authorities = authorities;
		this.enabled = enabled;
	}

	public UUID id() {
		return id;
	}
	public UUID businessId() {
		return businessId;
	}
	public String businessName() {
		return businessName;
	}
	public String firstName() {
		return firstName;
	}
	public String lastName() {
		return lastName;
	}
	public String email() {
		return email;
	}
	public String timezone() {
		return timezone;
	}
	public String fullName() {
		return firstName + " " + lastName;
	}

	@Override
	public List<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return enabled;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
