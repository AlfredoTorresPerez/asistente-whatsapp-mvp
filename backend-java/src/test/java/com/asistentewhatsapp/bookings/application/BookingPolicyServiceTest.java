package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.bookings.domain.PolicySnapshot;
import com.asistentewhatsapp.bookings.infrastructure.BusinessPolicyJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingPolicyServiceTest {

	@Test
	void freezePolicyReturnsSnapshotAndUpdatesBooking() {
		Fixture f = new Fixture();
		UUID businessId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, 24, 12, 60, 60, 15, 15, 30, 3, "PERCENT", null, null,
				"CLP", 15);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		PolicySnapshot result = f.service.freezePolicy(businessId, locationId, bookingId);

		assertThat(result).isEqualTo(snapshot);
		verify(f.repository).updateBookingPolicy(bookingId, versionId, snapshot);
	}

	@Test
	void freezePolicyReturnsNullWhenNoActiveVersion() {
		Fixture f = new Fixture();
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(null);

		PolicySnapshot result = f.service.freezePolicy(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(result).isNull();
	}

	@Test
	void validateCancellationThrowsWhenWindowClosed() {
		Fixture f = new Fixture();
		UUID businessId = UUID.randomUUID();
		UUID locationId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, 24, null, null, null, null, null, null, null, null,
				null, null, null, null);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		OffsetDateTime startsAt = OffsetDateTime.now().plusHours(12);
		assertThatThrownBy(() -> f.service.validateCancellation(businessId, locationId, startsAt))
				.isInstanceOf(ApiException.class).extracting("code").isEqualTo("CANCELLATION_WINDOW_CLOSED");
	}

	@Test
	void validateCancellationPassesWhenWindowOpen() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, 24, null, null, null, null, null, null, null, null,
				null, null, null, null);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		OffsetDateTime startsAt = OffsetDateTime.now().plusHours(48);
		f.service.validateCancellation(UUID.randomUUID(), UUID.randomUUID(), startsAt);
	}

	@Test
	void validateRescheduleThrowsWhenMaxCountExceeded() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, null, null, null, null, null, null, null, 2, null, null,
				null, null, null);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		assertThatThrownBy(
				() -> f.service.validateReschedule(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), 2))
				.isInstanceOf(ApiException.class).extracting("code").isEqualTo("RESCHEDULE_MAX_COUNT_EXCEEDED");
	}

	@Test
	void validateRescheduleThrowsWhenWindowClosed() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, null, 24, null, null, null, null, null, null, null,
				null, null, null, null);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		OffsetDateTime startsAt = OffsetDateTime.now().plusHours(12);
		assertThatThrownBy(() -> f.service.validateReschedule(UUID.randomUUID(), UUID.randomUUID(), startsAt, 0))
				.isInstanceOf(ApiException.class).extracting("code").isEqualTo("RESCHEDULE_WINDOW_CLOSED");
	}

	@Test
	void validateReschedulePassesWhenCountAndWindowOk() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, null, 24, null, null, null, null, null, 3, null, null,
				null, null, null);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		OffsetDateTime startsAt = OffsetDateTime.now().plusHours(48);
		f.service.validateReschedule(UUID.randomUUID(), UUID.randomUUID(), startsAt, 1);
	}

	@Test
	void validateMaxAdvanceThrowsWhenExceeded() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, null, null, 30, null, null, null, null, null, null,
				null, null, null, null);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		OffsetDateTime startsAt = OffsetDateTime.now().plusDays(45);
		assertThatThrownBy(() -> f.service.validateMaxAdvance(UUID.randomUUID(), UUID.randomUUID(), startsAt))
				.isInstanceOf(ApiException.class).extracting("code").isEqualTo("MAX_ADVANCE_EXCEEDED");
	}

	@Test
	void validateMaxAdvancePassesWhenWithinLimit() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, null, null, 60, null, null, null, null, null, null,
				null, null, null, null);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		OffsetDateTime startsAt = OffsetDateTime.now().plusDays(30);
		f.service.validateMaxAdvance(UUID.randomUUID(), UUID.randomUUID(), startsAt);
	}

	@Test
	void validateMethodsPassWhenNoActiveVersion() {
		Fixture f = new Fixture();
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(null);

		f.service.validateCancellation(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now());
		f.service.validateReschedule(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), 5);
		f.service.validateMaxAdvance(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now());
	}

	@Test
	void getSlotStepMinutesReturnsConfiguredValue() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, null, null, null, null, null, null, null, null, null,
				null, null, null, 10);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		int result = f.service.getSlotStepMinutes(UUID.randomUUID(), UUID.randomUUID());

		assertThat(result).isEqualTo(10);
	}

	@Test
	void getSlotStepMinutesDefaultsWhenNoActiveVersion() {
		Fixture f = new Fixture();
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(null);

		int result = f.service.getSlotStepMinutes(UUID.randomUUID(), UUID.randomUUID());

		assertThat(result).isEqualTo(15);
	}

	@Test
	void getSlotStepMinutesDefaultsWhenConfiguredValueIsInvalid() {
		Fixture f = new Fixture();
		UUID versionId = UUID.randomUUID();
		PolicySnapshot snapshot = new PolicySnapshot(versionId, null, null, null, null, null, null, null, null, null,
				null, null, null, 0);
		when(f.repository.findActiveVersionId(any(), any())).thenReturn(versionId);
		when(f.repository.buildSnapshot(any(), any(), any())).thenReturn(snapshot);

		int result = f.service.getSlotStepMinutes(UUID.randomUUID(), UUID.randomUUID());

		assertThat(result).isEqualTo(15);
	}

	private static final class Fixture {
		private final BusinessPolicyJdbcRepository repository = mock(BusinessPolicyJdbcRepository.class);
		private final BookingPolicyService service = new BookingPolicyService(repository);
	}
}
