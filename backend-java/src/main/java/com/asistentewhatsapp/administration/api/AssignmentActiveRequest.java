package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.NotNull;

public record AssignmentActiveRequest(@NotNull Boolean active) {
}
