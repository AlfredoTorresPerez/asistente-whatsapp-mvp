package com.asistentewhatsapp.security.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(
        UUID userId,
        UUID businessId,
        String businessName,
        String firstName,
        String lastName,
        String email,
        String timezone,
        List<String> roles) {

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    public String displayName() {
        return firstName + " " + lastName;
    }

    public String primaryRole() {
        return roles.isEmpty() ? "AGENT" : roles.getFirst();
    }
}

