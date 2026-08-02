package com.asistentewhatsapp.shared.observability.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.aiagents.application.AiReplyOutboxProcessor;
import com.asistentewhatsapp.aiagents.application.AiReplyOutboxProcessor.AiOutboxStats;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class OutboxHealthIndicatorTest {

	private final AiReplyOutboxProcessor processor = mock(AiReplyOutboxProcessor.class);
	private final OutboxHealthIndicator indicator = new OutboxHealthIndicator(processor, 50, 15);

	@Test
	void reportsUpWhenPendingAndAgeAreWithinThresholds() {
		when(processor.getStats())
				.thenReturn(new AiOutboxStats(40, 5, 2, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)));

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("pendientes", 45L).containsEntry("fallidas", 2L);
	}

	@Test
	void reportsDownWhenPendingExceedsThreshold() {
		when(processor.getStats()).thenReturn(new AiOutboxStats(100, 0, 0, null));

		assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	void reportsDownWhenOldestPendingExceedsAgeThreshold() {
		when(processor.getStats())
				.thenReturn(new AiOutboxStats(1, 0, 0, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30)));

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("antiguedadMaximaMinutos", 30L);
	}

	@Test
	void reportsUpWhenOutboxIsEmpty() {
		when(processor.getStats()).thenReturn(new AiOutboxStats(0, 0, 0, null));

		assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
	}
}
