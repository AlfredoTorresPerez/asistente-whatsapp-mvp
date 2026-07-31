package com.asistentewhatsapp.administration.api;

public record AssignmentSummaryResponse(long totalServices, long coveredServices, long partialServices,
		long uncoveredServices) {
}
