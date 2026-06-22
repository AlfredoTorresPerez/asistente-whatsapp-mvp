package com.asistentewhatsapp.administration.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SecurityPolicyResponse(
        UUID id,
        int sessionTimeoutMinutes,
        int passwordMinLength,
        boolean requireUppercase,
        boolean requireNumber,
        boolean requireSymbol,
        int maxFailedLoginAttempts,
        long activeUsers,
        long lockedUsers,
        long auditEventsLast7Days,
        OffsetDateTime updatedAt) {
}
