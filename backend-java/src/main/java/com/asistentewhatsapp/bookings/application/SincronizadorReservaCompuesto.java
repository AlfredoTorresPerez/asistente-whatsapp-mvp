package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.domain.SincronizadorReservaMotorReglas;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SincronizadorReservaCompuesto implements SincronizadorReservaMotorReglas {

	private static final Logger log = LoggerFactory.getLogger(SincronizadorReservaCompuesto.class);

	private final SincronizadorReservaLocal local;
	private final SincronizadorReservaEventos eventos;

	public SincronizadorReservaCompuesto(SincronizadorReservaLocal local, SincronizadorReservaEventos eventos) {
		this.local = local;
		this.eventos = eventos;
	}

	@Override
	public void sincronizarReserva(UUID businessId, UUID bookingId, String customerPhone, String customerName,
			String serviceName, String locationName, String professionalName, OffsetDateTime startsAt,
			int durationMinutes, String bookingStatus, UUID conversationId, String channelOrigin, String originIntent,
			String traceId) {
		local.sincronizarReserva(businessId, bookingId, customerPhone, customerName, serviceName, locationName,
				professionalName, startsAt, durationMinutes, bookingStatus, conversationId, channelOrigin, originIntent,
				traceId);

		eventos.sincronizarReserva(businessId, bookingId, customerPhone, customerName, serviceName, locationName,
				professionalName, startsAt, durationMinutes, bookingStatus, conversationId, channelOrigin, originIntent,
				traceId);

		log.info("BOOKING_SYNC_COMPOUND_COMPLETED bookingId={}", bookingId);
	}
}
