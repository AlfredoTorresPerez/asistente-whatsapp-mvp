package com.asistentewhatsapp.bookings.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.asistentewhatsapp.bookings.infrastructure.BookingSyncEventJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingSyncJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SincronizadorReservaCompuestoTest {

	private final BookingSyncJdbcRepository repository = mock(BookingSyncJdbcRepository.class);
	private final BookingSyncEventJdbcRepository eventRepository = mock(BookingSyncEventJdbcRepository.class);
	private final BookingSyncProperties properties = new BookingSyncProperties();
	private final BookingPhoneObfuscator phoneObfuscator = new BookingPhoneObfuscator(properties);
	private final SincronizadorReservaLocal local = new SincronizadorReservaLocal(repository, phoneObfuscator);
	private final SincronizadorReservaEventos eventos = new SincronizadorReservaEventos(eventRepository, properties);
	private final SincronizadorReservaCompuesto compuesto = new SincronizadorReservaCompuesto(local, eventos);

	private final UUID businessId = UUID.randomUUID();
	private final UUID bookingId = UUID.randomUUID();
	private final UUID conversationId = UUID.randomUUID();
	private final OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);

	@BeforeEach
	void setUp() {
		properties.setPhonePlaintextEnabled(true);
	}

	@Test
	void compuestoCallsLocalAndEventos() {
		compuesto.sincronizarReserva(businessId, bookingId, "56950954580", "Cliente", "Servicio", "Sucursal",
				"Profesional", startsAt, 30, "CONFIRMED", conversationId, "WHATSAPP_AI", "reservar", "trace-c-001");

		verify(repository).upsertBookingFact(eq(bookingId), eq(businessId), any(), eq("Cliente"), any(), eq("Servicio"),
				eq("Sucursal"), eq("Profesional"), any(), any(), eq("CONFIRMED"), eq(conversationId), eq("WHATSAPP_AI"),
				eq("reservar"), eq(startsAt));

		verify(eventRepository).enqueue(any(), eq(bookingId), eq(businessId), eq("RESERVA_CREADA"), eq(1), anyString(),
				anyString(), anyInt(), any(), eq("trace-c-001"));

		verify(repository).updateBookingSyncStatus(bookingId, businessId, "SYNCED");
	}
}
