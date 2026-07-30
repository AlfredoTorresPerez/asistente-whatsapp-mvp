package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignmentRequest(@NotNull UUID serviceId, UUID professionalId, UUID roomId) {
}
