package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.domain.PolicySnapshot;
import com.asistentewhatsapp.bookings.infrastructure.BusinessPolicyJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BookingPolicyService {

	private static final Logger LOG = LoggerFactory.getLogger(BookingPolicyService.class);

	private final BusinessPolicyJdbcRepository repository;

	public BookingPolicyService(BusinessPolicyJdbcRepository repository) {
		this.repository = repository;
	}

	public PolicySnapshot freezePolicy(UUID businessId, UUID locationId, UUID bookingId) {
		OffsetDateTime now = OffsetDateTime.now();
		UUID versionId = repository.findActiveVersionId(businessId, now);
		if (versionId == null) {
			LOG.warn("POLICY_NO_ACTIVE_VERSION businessId={} at={}", businessId, now);
			return null;
		}
		PolicySnapshot snapshot = repository.buildSnapshot(businessId, locationId, versionId);
		repository.updateBookingPolicy(bookingId, versionId, snapshot);
		LOG.info("POLICY_FROZEN bookingId={} versionId={} cancellationWindow={}h rescheduleWindow={}h maxAdvance={}d",
				bookingId, versionId, snapshot.cancellationWindowHours(), snapshot.rescheduleWindowHours(),
				snapshot.maxAdvanceDays());
		return snapshot;
	}

	public void validateCancellation(UUID businessId, UUID locationId, OffsetDateTime startsAt) {
		OffsetDateTime now = OffsetDateTime.now();
		UUID versionId = repository.findActiveVersionId(businessId, now);
		if (versionId == null)
			return;
		PolicySnapshot snapshot = repository.buildSnapshot(businessId, locationId, versionId);
		if (snapshot.cancellationWindowHours() == null)
			return;
		long hoursUntilAppointment = java.time.Duration.between(now, startsAt).toHours();
		if (hoursUntilAppointment < snapshot.cancellationWindowHours()) {
			throw new ApiException(HttpStatus.CONFLICT, "CANCELLATION_WINDOW_CLOSED",
					"El plazo de cancelacion ha vencido. Debe realizarse con al menos "
							+ snapshot.cancellationWindowHours() + " horas de anticipacion.");
		}
	}

	public void validateReschedule(UUID businessId, UUID locationId, OffsetDateTime startsAt,
			int currentRescheduleCount) {
		OffsetDateTime now = OffsetDateTime.now();
		UUID versionId = repository.findActiveVersionId(businessId, now);
		if (versionId == null)
			return;
		PolicySnapshot snapshot = repository.buildSnapshot(businessId, locationId, versionId);
		if (snapshot.rescheduleMaxCount() != null && currentRescheduleCount >= snapshot.rescheduleMaxCount()) {
			throw new ApiException(HttpStatus.CONFLICT, "RESCHEDULE_MAX_COUNT_EXCEEDED",
					"Has alcanzado el maximo de reprogramaciones permitidas (" + snapshot.rescheduleMaxCount() + ").");
		}
		if (snapshot.rescheduleWindowHours() == null)
			return;
		long hoursUntilAppointment = java.time.Duration.between(now, startsAt).toHours();
		if (hoursUntilAppointment < snapshot.rescheduleWindowHours()) {
			throw new ApiException(HttpStatus.CONFLICT, "RESCHEDULE_WINDOW_CLOSED",
					"El plazo de reprogramacion ha vencido. Debe realizarse con al menos "
							+ snapshot.rescheduleWindowHours() + " horas de anticipacion.");
		}
	}

	public void validateMaxAdvance(UUID businessId, UUID locationId, OffsetDateTime startsAt) {
		OffsetDateTime now = OffsetDateTime.now();
		UUID versionId = repository.findActiveVersionId(businessId, now);
		if (versionId == null)
			return;
		PolicySnapshot snapshot = repository.buildSnapshot(businessId, locationId, versionId);
		if (snapshot.maxAdvanceDays() == null)
			return;
		long daysUntilAppointment = java.time.Duration.between(now, startsAt).toDays();
		if (daysUntilAppointment > snapshot.maxAdvanceDays()) {
			throw new ApiException(HttpStatus.CONFLICT, "MAX_ADVANCE_EXCEEDED",
					"La cita no puede agendarse con mas de " + snapshot.maxAdvanceDays() + " dias de anticipacion.");
		}
	}

	public int getSlotStepMinutes(UUID businessId, UUID locationId) {
		OffsetDateTime now = OffsetDateTime.now();
		UUID versionId = repository.findActiveVersionId(businessId, now);
		if (versionId == null)
			return 15;
		PolicySnapshot snapshot = repository.buildSnapshot(businessId, locationId, versionId);
		return normalizeSlotStepMinutes(snapshot.slotStepMinutes());
	}

	private int normalizeSlotStepMinutes(Integer slotStepMinutes) {
		if (slotStepMinutes == null || slotStepMinutes <= 0)
			return 15;
		return slotStepMinutes;
	}
}
