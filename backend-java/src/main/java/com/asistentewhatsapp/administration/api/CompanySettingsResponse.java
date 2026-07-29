package com.asistentewhatsapp.administration.api;

import java.util.UUID;

public record CompanySettingsResponse(UUID id, String companyName, String businessName, String timezone,
		String currency, String contactEmail, String supportPhone, String address) {
}
