package com.asistentewhatsapp.multisite.api;

import java.util.UUID;

public record ProfessionalLocationAssignmentResponse(UUID locationId, String locationName, boolean active) {
}
