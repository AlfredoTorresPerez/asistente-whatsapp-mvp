package com.asistentewhatsapp.security.api;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String role,
        UUID businessId,
        String businessName,
        String timezone) {
}

