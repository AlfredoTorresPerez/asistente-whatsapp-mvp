package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        String status,
        String timezone,
        OffsetDateTime lastLoginAt,
        int failedLoginAttempts,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
