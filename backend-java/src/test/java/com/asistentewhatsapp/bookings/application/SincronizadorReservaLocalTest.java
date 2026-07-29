package com.asistentewhatsapp.bookings.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.asistentewhatsapp.bookings.infrastructure.BookingSyncJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SincronizadorReservaLocalTest {

	private final BookingSyncJdbcRepository repository = mock(BookingSyncJdbcRepository.class);
	private final BookingSyncProperties properties = new BookingSyncProperties();
	private final BookingPhoneObfuscator phoneObfuscator = new BookingPhoneObfuscator(properties);
	private final SincronizadorReservaLocal sync = new SincronizadorReservaLocal(repository, phoneObfuscator);

	private final UUID businessId = UUID.randomUUID();
	private final UUID bookingId = UUID.randomUUID();
	private final UUID conversationId = UUID.randomUUID();
	private final OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);

	@BeforeEach
	void setUp() {
		properties.setPhonePlaintextEnabled(true);
	}

	@Test
	void sincronizarReservaUpsertsFactAndUpdatesBookingSyncStatus() {
		sync.sincronizarReserva(businessId, bookingId, "56950954580", "Cliente Test", "Limpieza facial", "Providencia",
				"María", startsAt, 60, "CONFIRMED", conversationId, "WHATSAPP_AI", "reservar", "trace-123");

		verify(repository).upsertBookingFact(eq(bookingId), eq(businessId), eq("56950954580"), eq("Cliente Test"),
				eq("56950954580"), eq("Limpieza facial"), eq("Providencia"), eq("María"), any(), any(), eq("CONFIRMED"),
				eq(conversationId), eq("WHATSAPP_AI"), eq("reservar"), eq(startsAt));

		verify(repository).updateBookingSyncStatus(eq(bookingId), eq(businessId), eq("SYNCED"));
	}

	@Test
	void sincronizarReservaObfuscatesPhoneWhenPlaintextDisabled() {
		properties.setPhonePlaintextEnabled(false);

		sync.sincronizarReserva(businessId, bookingId, "56950954580", "Cliente", "Servicio", "Sucursal", "Profesional",
				startsAt, 30, "CONFIRMED", conversationId, "PUBLIC_LINK", "reservar", "trace-456");

		verify(repository).upsertBookingFact(eq(bookingId), eq(businessId), any(), eq("Cliente"), any(), eq("Servicio"),
				eq("Sucursal"), eq("Profesional"), any(), any(), eq("CONFIRMED"), eq(conversationId), eq("PUBLIC_LINK"),
				eq("reservar"), eq(startsAt));
	}

	@Test
	void sincronizarReservaHandlesNullConversationId() {
		sync.sincronizarReserva(businessId, bookingId, "56950954580", "Cliente", "Servicio", "Sucursal", "Profesional",
				startsAt, 30, "CONFIRMED", null, "PUBLIC_LINK", "reservar", "trace-789");

		verify(repository).upsertBookingFact(eq(bookingId), eq(businessId), any(), eq("Cliente"), any(), eq("Servicio"),
				eq("Sucursal"), eq("Profesional"), any(), any(), eq("CONFIRMED"), eq(null), eq("PUBLIC_LINK"),
				eq("reservar"), eq(startsAt));
	}
}
