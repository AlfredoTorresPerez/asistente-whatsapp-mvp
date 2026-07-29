package com.asistentewhatsapp.security.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(UUID userId, UUID businessId, String businessName, String firstName, String lastName,
		String email, String timezone, List<String> roles, List<String> permissions) {

	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
	}

	public String displayName() {
		return firstName + " " + lastName;
	}

	public String primaryRole() {
		return roles.isEmpty() ? "AGENT" : roles.getFirst();
	}

	public boolean hasPermission(String permission) {
		return permissions != null && permissions.contains(permission);
	}

	public boolean hasAnyPermission(String... permissions) {
		if (this.permissions == null)
			return false;
		for (String p : permissions) {
			if (this.permissions.contains(p))
				return true;
		}
		return false;
	}

	public boolean hasRole(String role) {
		return roles != null && roles.contains(role);
	}
}
