package com.asistentewhatsapp.security.application;

public record UserResponse(
        String id,
        String firstName,
        String lastName,
        String name,
        String email,
        String role,
        String businessId,
        String businessName,
        String timezone) {
}
