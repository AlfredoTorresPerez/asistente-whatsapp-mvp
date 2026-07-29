package com.asistentewhatsapp.bookings.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record PolicySnapshot(UUID policyVersionId, Integer cancellationWindowHours, Integer rescheduleWindowHours,
		Integer maxAdvanceDays, Integer minAdvanceMinutes, Integer toleranceMinutes, Integer gracePeriodMinutes,
		Integer autoExpireMinutes, Integer rescheduleMaxCount, String penaltyType, BigDecimal penaltyPercent,
		BigDecimal penaltyFixedAmount, String penaltyCurrency, Integer slotStepMinutes) {
}
