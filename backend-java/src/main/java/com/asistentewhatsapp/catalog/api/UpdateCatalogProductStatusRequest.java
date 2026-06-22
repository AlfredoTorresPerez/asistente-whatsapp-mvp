package com.asistentewhatsapp.catalog.api;

import jakarta.validation.constraints.NotNull;

public record UpdateCatalogProductStatusRequest(@NotNull Boolean active) {
}
