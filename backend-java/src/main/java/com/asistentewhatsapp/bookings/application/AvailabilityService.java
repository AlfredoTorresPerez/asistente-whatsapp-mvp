package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AvailabilityService.class);

	private final CompleteAgendaJdbcRepository repository;

	public AvailabilityService(CompleteAgendaJdbcRepository repository) {
		this.repository = repository;
	}

	public void checkProfessionalAbsence(UUID businessId, UUID professionalId, OffsetDateTime startsAt,
			OffsetDateTime endsAt) {
		if (repository.hasActiveAbsence(businessId, professionalId, startsAt, endsAt)) {
			LOGGER.warn("[diagnostico] Ausencia activa: businessId={} professionalId={} startsAt={}", businessId,
					professionalId, startsAt);
			throw conflict("PROFESSIONAL_ABSENCE",
					"El profesional no esta disponible por ausencia en la fecha seleccionada.",
					Map.of("startsAt", "El profesional tiene ausencia registrada en este horario."));
		}
	}

	public void checkProfessionalDailyCapacity(UUID businessId, UUID professionalId, OffsetDateTime startsAt) {
		Integer maxDaily = repository.findProfessionalMaxDailyBookings(businessId, professionalId);
		if (maxDaily == null) {
			return;
		}
		int currentCount = repository.countProfessionalBookingsOnDate(businessId, professionalId, startsAt);
		if (currentCount >= maxDaily) {
			LOGGER.warn("[diagnostico] Capacidad diaria excedida: professionalId={} date={} current={} max={}",
					professionalId, startsAt, currentCount, maxDaily);
			throw conflict("PROFESSIONAL_DAILY_CAPACITY_EXCEEDED",
					"El profesional ha alcanzado su cupo maximo de reservas para el dia seleccionado.",
					Map.of("startsAt", "Selecciona otra fecha o profesional."));
		}
	}

	public void checkCustomerDuplicateActiveBooking(UUID businessId, UUID customerId, UUID professionalId,
			OffsetDateTime startsAt, OffsetDateTime endsAt, UUID excludeBookingId) {
		int count = repository.countCustomerActiveOverlappingBookingsExcluding(businessId, customerId, professionalId,
				startsAt, endsAt, excludeBookingId);
		if (count > 0) {
			LOGGER.warn("[diagnostico] Duplicado de cliente: customerId={} professionalId={} startsAt={}", customerId,
					professionalId, startsAt);
			throw conflict("CUSTOMER_DUPLICATE_BOOKING", "El cliente ya tiene una reserva activa en el mismo horario.",
					Map.of("startsAt", "El cliente ya tiene una reserva en este horario."));
		}
	}

	public void checkMinAdvanceNotice(UUID businessId, UUID locationId, UUID professionalId, OffsetDateTime startsAt,
			ZoneId zone) {
		OffsetDateTime now = OffsetDateTime.now(zone);
		long minutesAhead = Duration.between(now, startsAt).toMinutes();
		if (minutesAhead < 0) {
			throw conflict("BOOKING_IN_PAST", "La fecha y hora seleccionadas ya pasaron.",
					Map.of("startsAt", "Selecciona una fecha y hora futura."));
		}
		int dayOfWeek = startsAt.getDayOfWeek().getValue();
		int minAdvance = repository.findMinAdvanceNoticeMinutes(businessId, locationId, dayOfWeek, professionalId);
		if (minutesAhead < minAdvance) {
			LOGGER.warn("[diagnostico] Anticipacion insuficiente: businessId={} minutesAhead={} minRequired={}",
					businessId, minutesAhead, minAdvance);
			throw conflict("INSUFFICIENT_ADVANCE_NOTICE",
					"La reserva debe hacerse con al menos " + minAdvance + " minutos de anticipacion.",
					Map.of("startsAt", "Selecciona un horario con mayor anticipacion."));
		}
	}

	private ApiException conflict(String code, String message, Map<String, String> fieldErrors) {
		LOGGER.warn("[diagnostico] Conflicto de disponibilidad: code={} message={} fieldErrors={}", code, message,
				fieldErrors);
		return new ApiException(HttpStatus.CONFLICT, code, message, fieldErrors);
	}
}
