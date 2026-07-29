package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.domain.SincronizadorReservaMotorReglas;
import com.asistentewhatsapp.bookings.infrastructure.BookingSyncJdbcRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SincronizadorReservaLocal implements SincronizadorReservaMotorReglas {

	private static final Logger log = LoggerFactory.getLogger(SincronizadorReservaLocal.class);

	private final BookingSyncJdbcRepository repository;
	private final BookingPhoneObfuscator phoneObfuscator;

	public SincronizadorReservaLocal(BookingSyncJdbcRepository repository, BookingPhoneObfuscator phoneObfuscator) {
		this.repository = repository;
		this.phoneObfuscator = phoneObfuscator;
	}

	@Override
	public void sincronizarReserva(UUID businessId, UUID bookingId, String customerPhone, String customerName,
			String serviceName, String locationName, String professionalName, OffsetDateTime startsAt,
			int durationMinutes, String bookingStatus, UUID conversationId, String channelOrigin, String originIntent,
			String traceId) {
		String obfuscatedPhone = phoneObfuscator.obfuscate(customerPhone);
		String managementId = phoneObfuscator.toManagementId(customerPhone);
		OffsetDateTime bookingDate = startsAt != null
				? startsAt.toLocalDate().atStartOfDay(startsAt.getOffset()).toOffsetDateTime()
				: null;
		OffsetDateTime bookingTime = startsAt != null
				? startsAt.toLocalTime().atDate(startsAt.toLocalDate()).atOffset(startsAt.getOffset())
				: null;

		repository.upsertBookingFact(bookingId, businessId, obfuscatedPhone, customerName, managementId, serviceName,
				locationName, professionalName, bookingDate, bookingTime, bookingStatus, conversationId, channelOrigin,
				originIntent, startsAt);

		repository.updateBookingSyncStatus(bookingId, businessId, "SYNCED");

		log.info("BOOKING_FACT_SYNCED bookingId={} businessId={} phoneObfuscated={} service={} status={}", bookingId,
				businessId, !obfuscatedPhone.equals(customerPhone), serviceName, bookingStatus);
	}
}
