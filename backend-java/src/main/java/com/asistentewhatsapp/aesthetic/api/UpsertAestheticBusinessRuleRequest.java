package com.asistentewhatsapp.aesthetic.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertAestheticBusinessRuleRequest(
		@Size(max = 80, message = "code no debe superar 80 caracteres") String code,
		@NotBlank(message = "name es obligatorio") @Size(max = 160, message = "name no debe superar 160 caracteres") String name,
		@NotBlank(message = "ruleType es obligatorio") @Size(max = 60, message = "ruleType no debe superar 60 caracteres") String ruleType,
		@NotBlank(message = "description es obligatoria") @Size(max = 4000, message = "description no debe superar 4000 caracteres") String description,
		@Min(value = 1, message = "priority debe ser mayor o igual a 1") @Max(value = 999, message = "priority no debe superar 999") Integer priority,
		Boolean active, @Size(max = 8000, message = "rulePayload no debe superar 8000 caracteres") String rulePayload) {
}
