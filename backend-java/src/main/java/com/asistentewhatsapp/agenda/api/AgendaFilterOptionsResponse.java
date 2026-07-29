package com.asistentewhatsapp.agenda.api;

import java.util.List;

public record AgendaFilterOptionsResponse(List<AgendaFilterOptionResponse> services,
		List<AgendaFilterOptionResponse> professionals, List<AgendaFilterOptionResponse> rooms) {
}
