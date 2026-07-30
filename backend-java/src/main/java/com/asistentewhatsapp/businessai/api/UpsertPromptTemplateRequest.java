package com.asistentewhatsapp.businessai.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertPromptTemplateRequest(@NotBlank @Size(max = 120) String codigo,

		@NotBlank @Size(max = 180) String nombre,

		@NotBlank @Size(max = 500) String descripcion,

		@NotBlank @Size(max = 80) String modulo,

		@NotBlank @Size(max = 60) String tipo,

		@NotBlank String contenido,

		int prioridad) {
}
