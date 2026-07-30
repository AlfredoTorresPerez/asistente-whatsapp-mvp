package com.asistentewhatsapp.businessai.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PromptTemplateResponse(UUID id, UUID businessId, String codigo, String nombre, String descripcion,
		String modulo, String tipo, String contenido, int prioridad, boolean activo, int version,
		OffsetDateTime fechaCreacion, OffsetDateTime fechaActualizacion) {
}
