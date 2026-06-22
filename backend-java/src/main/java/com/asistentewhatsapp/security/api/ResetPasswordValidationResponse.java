package com.asistentewhatsapp.security.api;

import java.time.OffsetDateTime;

public record ResetPasswordValidationResponse(
        boolean valid,
        OffsetDateTime expiresAt) {
}

