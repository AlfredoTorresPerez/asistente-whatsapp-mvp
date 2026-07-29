package com.asistentewhatsapp.security.api;

import java.util.UUID;

public record UserProfileResponse(UUID id, String firstName, String lastName, String email, String phone,
		String timezone, String role, String businessName) {
}
