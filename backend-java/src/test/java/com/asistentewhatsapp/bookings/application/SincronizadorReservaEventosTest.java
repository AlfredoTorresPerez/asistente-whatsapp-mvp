package com.asistentewhatsapp.bookings.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.asistentewhatsapp.bookings.infrastructure.BookingSyncEventJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SincronizadorReservaEventosTest {

    private final BookingSyncEventJdbcRepository eventRepository = mock(BookingSyncEventJdbcRepository.class);
    private final BookingSyncProperties properties = new BookingSyncProperties();
    private final SincronizadorReservaEventos sync = new SincronizadorReservaEventos(eventRepository, properties);

    private final UUID businessId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);

    @Test
    void sincronizarReservaEnqueuesEventWithCorrectData() {
        sync.sincronizarReserva(
                businessId, bookingId, "56950954580", "Cliente",
                "Servicio", "Sucursal", "Profesional",
                startsAt, 30, "CONFIRMED", conversationId,
                "WHATSAPP_AI", "reservar", "trace-001");

        verify(eventRepository).enqueue(
                any(), eq(bookingId), eq(businessId), eq("RESERVA_CREADA"),
                eq(1), anyString(), anyString(),
                eq(properties.getEventMaxAttempts()), any(), eq("trace-001"));
    }

    @Test
    void sincronizarReservaEnqueuesEventWithNullConversationId() {
        sync.sincronizarReserva(
                businessId, bookingId, "56950954580", "Cliente",
                "Servicio", "Sucursal", "Profesional",
                startsAt, 30, "CONFIRMED", null,
                "PUBLIC_LINK", "reservar", "trace-002");

        verify(eventRepository).enqueue(
                any(), eq(bookingId), eq(businessId), eq("RESERVA_CREADA"),
                eq(1), anyString(), anyString(),
                anyInt(), any(), eq("trace-002"));
    }

    @Test
    void sincronizarReservaEnqueuesEventWithMaxAttemptsFromProperties() {
        properties.setEventMaxAttempts(10);

        sync.sincronizarReserva(
                businessId, bookingId, "56950954580", "Cliente",
                "Servicio", "Sucursal", "Profesional",
                startsAt, 30, "CONFIRMED", conversationId,
                "WHATSAPP_AI", "reservar", "trace-003");

        verify(eventRepository).enqueue(
                any(), eq(bookingId), eq(businessId), eq("RESERVA_CREADA"),
                eq(1), anyString(), anyString(),
                eq(10), any(), eq("trace-003"));
    }
}
