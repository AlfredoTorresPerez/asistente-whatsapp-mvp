package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvailabilityServiceTest {

	private static final UUID BUSINESS_ID = UUID.randomUUID();
	private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	private CompleteAgendaJdbcRepository repository;
	private AvailabilityService availabilityService;

	@BeforeEach
	void setUp() {
		repository = mock(CompleteAgendaJdbcRepository.class);
		availabilityService = new AvailabilityService(repository);
	}

	@Test
	void acceptsNoDuplicateWhenExcludingOwnBooking() {
		OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withHour(10).withMinute(0);
		OffsetDateTime endsAt = startsAt.plusHours(1);
		UUID excludeBookingId = UUID.randomUUID();

		when(repository.countCustomerActiveOverlappingBookingsExcluding(eq(BUSINESS_ID), eq(CUSTOMER_ID),
				eq(PROFESSIONAL_ID), eq(startsAt), eq(endsAt), eq(excludeBookingId))).thenReturn(0);

		assertThatCode(() -> availabilityService.checkCustomerDuplicateActiveBooking(BUSINESS_ID, CUSTOMER_ID,
				PROFESSIONAL_ID, startsAt, endsAt, excludeBookingId)).doesNotThrowAnyException();
	}

	@Test
	void rejectsDuplicateWhenExcludingDifferentBooking() {
		OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withHour(10).withMinute(0);
		OffsetDateTime endsAt = startsAt.plusHours(1);
		UUID excludeBookingId = UUID.randomUUID();

		when(repository.countCustomerActiveOverlappingBookingsExcluding(eq(BUSINESS_ID), eq(CUSTOMER_ID),
				eq(PROFESSIONAL_ID), eq(startsAt), eq(endsAt), eq(excludeBookingId))).thenReturn(1);

		assertThatThrownBy(() -> availabilityService.checkCustomerDuplicateActiveBooking(BUSINESS_ID, CUSTOMER_ID,
				PROFESSIONAL_ID, startsAt, endsAt, excludeBookingId)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("CUSTOMER_DUPLICATE_BOOKING"));
	}

	@Test
	void acceptsNewBookingForCustomerWithoutOverlap() {
		OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withHour(10).withMinute(0);
		OffsetDateTime endsAt = startsAt.plusHours(1);

		when(repository.countCustomerActiveOverlappingBookingsExcluding(eq(BUSINESS_ID), eq(CUSTOMER_ID),
				eq(PROFESSIONAL_ID), eq(startsAt), eq(endsAt), eq(null))).thenReturn(0);

		assertThatCode(() -> availabilityService.checkCustomerDuplicateActiveBooking(BUSINESS_ID, CUSTOMER_ID,
				PROFESSIONAL_ID, startsAt, endsAt, null)).doesNotThrowAnyException();
	}

	@Test
	void rejectsDuplicateBookingForSameCustomer() {
		OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withHour(10).withMinute(0);
		OffsetDateTime endsAt = startsAt.plusHours(1);

		when(repository.countCustomerActiveOverlappingBookingsExcluding(eq(BUSINESS_ID), eq(CUSTOMER_ID),
				eq(PROFESSIONAL_ID), eq(startsAt), eq(endsAt), eq(null))).thenReturn(1);

		assertThatThrownBy(() -> availabilityService.checkCustomerDuplicateActiveBooking(BUSINESS_ID, CUSTOMER_ID,
				PROFESSIONAL_ID, startsAt, endsAt, null)).isInstanceOf(ApiException.class)
				.matches(e -> ((ApiException) e).getCode().equals("CUSTOMER_DUPLICATE_BOOKING"));
	}

	@Test
	void skipsDuplicateCheckWhenCustomerIdIsNull() {
		OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withHour(10).withMinute(0);
		OffsetDateTime endsAt = startsAt.plusHours(1);

		assertThatCode(() -> availabilityService.checkCustomerDuplicateActiveBooking(BUSINESS_ID, null, PROFESSIONAL_ID,
				startsAt, endsAt, null)).doesNotThrowAnyException();
	}
}
