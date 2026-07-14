package com.asistentewhatsapp.bookings.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingSyncEventRecord(
        UUID id,
        UUID bookingId,
        UUID businessId,
        String eventType,
        int eventVersion,
        String eventBody,
        String idempotencyKey,
        String status,
        int attempts,
        int maxAttempts,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime lockedAt,
        String lastErrorCode,
        String lastErrorMessage,
        String traceId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public enum Status {
        PENDING, PROCESSING, SYNCED, FAILED, SKIPPED;

        public static Status fromString(String value) {
            for (Status s : values()) {
                if (s.name().equals(value)) return s;
            }
            throw new IllegalArgumentException("Unknown status: " + value);
        }
    }

    public enum EventType {
        RESERVA_CREADA, RESERVA_CANCELADA, RESERVA_REPROGRAMADA;

        public static EventType fromString(String value) {
            for (EventType e : values()) {
                if (e.name().equals(value)) return e;
            }
            throw new IllegalArgumentException("Unknown event type: " + value);
        }
    }
}
