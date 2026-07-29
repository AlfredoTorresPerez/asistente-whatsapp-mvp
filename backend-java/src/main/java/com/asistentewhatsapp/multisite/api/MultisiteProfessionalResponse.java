package com.asistentewhatsapp.multisite.api;

import java.util.List;
import java.util.UUID;

public record MultisiteProfessionalResponse(UUID professionalId, String fullName, String specialty, boolean active,
		List<ProfessionalLocationAssignmentResponse> locations) {
}
