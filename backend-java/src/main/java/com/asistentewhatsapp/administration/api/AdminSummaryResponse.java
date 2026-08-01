package com.asistentewhatsapp.administration.api;

import java.util.UUID;

public record AdminSummaryResponse(CompanySummary company, UsersSummary users, WhatsAppChannelSummary whatsapp,
		SecuritySummary security) {

	public record CompanySummary(UUID id, String companyName) {
	}

	public record UsersSummary(long total, long active) {
	}

	public record WhatsAppChannelSummary(String status) {
	}

	public record SecuritySummary(int sessionTimeoutMinutes) {
	}
}
