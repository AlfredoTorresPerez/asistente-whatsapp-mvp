package com.asistentewhatsapp.bookings.domain;

import java.util.UUID;

public record BookingPolicyRecord(UUID id, UUID versionId, UUID locationId, String policyType, String policyKey,
		String policyValue, int priority, boolean active) {
}
