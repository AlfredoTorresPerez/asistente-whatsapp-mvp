package com.asistentewhatsapp.security.application;

import java.util.Comparator;

public final class SecurityMapper {

    private SecurityMapper() {
    }

    public static UserResponse toUserResponse(CurrentUser user) {
        String role = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .min(Comparator.naturalOrder())
                .orElse("READ_ONLY");
        return new UserResponse(
                user.id().toString(),
                user.firstName(),
                user.lastName(),
                user.fullName(),
                user.email(),
                role,
                user.businessId().toString(),
                user.businessName(),
                user.timezone());
    }
}
