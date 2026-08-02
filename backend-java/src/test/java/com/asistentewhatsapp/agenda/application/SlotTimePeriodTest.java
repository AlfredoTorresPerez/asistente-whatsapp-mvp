package com.asistentewhatsapp.agenda.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class SlotTimePeriodTest {

	@Test
	void classifiesNineOClockAsMorning() {
		assertThat(SlotTimePeriod.of(LocalTime.of(9, 0))).isEqualTo(SlotTimePeriod.MORNING);
	}

	@Test
	void classifiesQuarterToNoonAsMorning() {
		assertThat(SlotTimePeriod.of(LocalTime.of(11, 45))).isEqualTo(SlotTimePeriod.MORNING);
	}

	@Test
	void classifiesNoonAsAfternoon() {
		assertThat(SlotTimePeriod.of(LocalTime.of(12, 0))).isEqualTo(SlotTimePeriod.AFTERNOON);
	}

	@Test
	void classifiesSixPmAsAfternoon() {
		assertThat(SlotTimePeriod.of(LocalTime.of(18, 0))).isEqualTo(SlotTimePeriod.AFTERNOON);
	}

	@Test
	void classifiesUsingOnlyTheStartTimeNotTheEndTime() {
		assertThat(SlotTimePeriod.of(LocalTime.of(11, 59))).isEqualTo(SlotTimePeriod.MORNING);
		assertThat(SlotTimePeriod.of(LocalTime.of(12, 0))).isEqualTo(SlotTimePeriod.AFTERNOON);
	}

	@Test
	void classifiesFromOffsetDateTimeStart() {
		assertThat(SlotTimePeriod.of(OffsetDateTime.parse("2026-08-05T09:00:00-04:00")))
				.isEqualTo(SlotTimePeriod.MORNING);
		assertThat(SlotTimePeriod.of(OffsetDateTime.parse("2026-08-05T12:00:00-04:00")))
				.isEqualTo(SlotTimePeriod.AFTERNOON);
	}

	@Test
	void noonConstantIsCentralizedAtTwelve() {
		assertThat(SlotTimePeriod.NOON).isEqualTo(LocalTime.NOON);
	}
}
