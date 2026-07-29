package com.asistentewhatsapp.landing.api;

import java.util.UUID;

public record PublicCustomerInfoResponse(String customerName, String customerPhone, String customerEmail,
		UUID lastLocationId, String lastLocationName) {
}
