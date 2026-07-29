package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SecurityPolicyRequest(
		@Min(value = 5, message = "La sesion debe durar al menos 5 minutos.") @Max(value = 1440, message = "La sesion no puede superar 1440 minutos.") int sessionTimeoutMinutes,

		@Min(value = 8, message = "La contrasena debe tener al menos 8 caracteres.") @Max(value = 72, message = "La contrasena no puede superar 72 caracteres.") int passwordMinLength,

		boolean requireUppercase, boolean requireNumber, boolean requireSymbol,

		@Min(value = 3, message = "El bloqueo requiere al menos 3 intentos.") @Max(value = 20, message = "El bloqueo no puede superar 20 intentos.") int maxFailedLoginAttempts) {
}
