package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.domain.SincronizadorReservaMotorReglas;
import com.asistentewhatsapp.bookings.infrastructure.BookingSyncEventJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SincronizadorReservaEventos implements SincronizadorReservaMotorReglas {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorReservaEventos.class);

    private final BookingSyncEventJdbcRepository eventRepository;
    private final BookingSyncProperties properties;

    public SincronizadorReservaEventos(BookingSyncEventJdbcRepository eventRepository, BookingSyncProperties properties) {
        this.eventRepository = eventRepository;
        this.properties = properties;
    }

    @Override
    public void sincronizarReserva(UUID businessId, UUID bookingId,
            String customerPhone, String customerName, String serviceName,
            String locationName, String professionalName, OffsetDateTime startsAt,
            int durationMinutes, String bookingStatus, UUID conversationId,
            String channelOrigin, String originIntent, String traceId) {
        String eventBody = buildEventBody(bookingId, businessId, customerPhone, customerName,
                serviceName, locationName, professionalName, startsAt, durationMinutes,
                bookingStatus, conversationId, channelOrigin, originIntent);
        String idempotencyKey = "RESERVA_CREADA_" + bookingId + "_" + (startsAt != null ? startsAt.toEpochSecond() : "");
        OffsetDateTime nextAttemptAt = OffsetDateTime.now(ZoneOffset.UTC);

        boolean enqueued = eventRepository.enqueue(
                UUID.randomUUID(),
                bookingId,
                businessId,
                "RESERVA_CREADA",
                1,
                eventBody,
                idempotencyKey,
                properties.getEventMaxAttempts(),
                nextAttemptAt,
                traceId);

        if (enqueued) {
            log.info("BOOKING_SYNC_EVENT_ENQUEUED bookingId={} eventType=RESERVA_CREADA idempotencyKey={}",
                    bookingId, idempotencyKey);
        } else {
            log.warn("BOOKING_SYNC_EVENT_DUPLICATE bookingId={} idempotencyKey={}", bookingId, idempotencyKey);
        }
    }

    private String buildEventBody(UUID bookingId, UUID businessId, String customerPhone,
            String customerName, String serviceName, String locationName,
            String professionalName, OffsetDateTime startsAt, int durationMinutes,
            String bookingStatus, UUID conversationId, String channelOrigin,
            String originIntent) {
        return String.format("""
                {"bookingId":"%s","businessId":"%s","customerName":"%s","serviceName":"%s","locationName":"%s","professionalName":"%s","startsAt":"%s","durationMinutes":%d,"bookingStatus":"%s","conversationId":"%s","channelOrigin":"%s","originIntent":"%s"}
                """,
                safeJson(bookingId.toString()),
                safeJson(businessId.toString()),
                safeJson(customerName),
                safeJson(serviceName),
                safeJson(locationName),
                safeJson(professionalName),
                startsAt != null ? startsAt.toString() : "",
                durationMinutes,
                safeJson(bookingStatus),
                conversationId != null ? conversationId.toString() : "",
                safeJson(channelOrigin),
                safeJson(originIntent));
    }

    private String safeJson(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
