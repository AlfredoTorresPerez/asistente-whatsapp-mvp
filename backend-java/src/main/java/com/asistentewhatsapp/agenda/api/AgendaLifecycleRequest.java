package com.asistentewhatsapp.agenda.api;

import jakarta.validation.constraints.Size;

public record AgendaLifecycleRequest(
		@Size(max = 2000, message = "El motivo no puede superar 2000 caracteres.") String reason,
		Boolean notifyCustomer) {
}
