package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.domain.BookingSyncEventRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingSyncEventJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingSyncJdbcRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BookingSyncEventProcessor {

	private static final Logger log = LoggerFactory.getLogger(BookingSyncEventProcessor.class);

	private final BookingSyncEventJdbcRepository eventRepository;
	private final BookingSyncJdbcRepository syncRepository;
	private final BookingSyncProperties properties;

	public BookingSyncEventProcessor(BookingSyncEventJdbcRepository eventRepository,
			BookingSyncJdbcRepository syncRepository, BookingSyncProperties properties) {
		this.eventRepository = eventRepository;
		this.syncRepository = syncRepository;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${app.booking-sync.event-worker-interval-ms:10000}")
	public void processDueEvents() {
		List<BookingSyncEventRecord> events = eventRepository.claimDueEvents(properties.getEventBatchSize(),
				properties.getEventProcessingTimeoutMs());

		if (events.isEmpty()) {
			log.debug("BOOKING_SYNC_WORKER_IDLE batchSize={}", properties.getEventBatchSize());
			return;
		}

		log.info("BOOKING_SYNC_WORKER_CLAIMED events={} batchSize={}", events.size(), properties.getEventBatchSize());
		events.forEach(this::processEventSafely);
	}

	private void processEventSafely(BookingSyncEventRecord event) {
		try {
			processEvent(event);
			eventRepository.markSynced(event.id(), OffsetDateTime.now(ZoneOffset.UTC));
		} catch (RuntimeException e) {
			OffsetDateTime nextAttemptAt = OffsetDateTime.now(ZoneOffset.UTC).plus(retryDelay(event.attempts()));
			eventRepository.markFailedOrRetry(event.id(), event.attempts(), event.maxAttempts(),
					e.getClass().getSimpleName(), e.getMessage(), nextAttemptAt);
			log.error("BOOKING_SYNC_EVENT_FAILED eventId={} bookingId={} attempt={}/{} nextAttemptAt={} error={}",
					event.id(), event.bookingId(), event.attempts(), event.maxAttempts(), nextAttemptAt, e.getMessage(),
					e);
		}
	}

	private void processEvent(BookingSyncEventRecord event) {
		if (!"RESERVA_CREADA".equals(event.eventType())) {
			eventRepository.markSkipped(event.id(), "UNSUPPORTED_EVENT_TYPE_" + event.eventType());
			log.warn("BOOKING_SYNC_EVENT_SKIPPED_UNSUPPORTED_TYPE eventId={} type={}", event.id(), event.eventType());
			return;
		}

		String idempotencyKey = event.idempotencyKey();
		var existing = syncRepository.findEventByIdempotencyKey(idempotencyKey);
		if (existing.isPresent() && "SYNCED".equals(existing.get().status())) {
			log.info("BOOKING_SYNC_EVENT_ALREADY_SYNCED eventId={} idempotencyKey={}", event.id(), idempotencyKey);
			return;
		}

		syncRepository.updateBookingFactStatus(event.bookingId(), "SYNCED");
		log.info("BOOKING_SYNC_EVENT_PROCESSED eventId={} bookingId={} type={}", event.id(), event.bookingId(),
				event.eventType());
	}

	private Duration retryDelay(int attempts) {
		long multiplier = Math.max(1L, 1L << Math.min(attempts - 1, 8));
		long delay = Math.min(properties.getRetryBaseDelayMs() * multiplier, properties.getRetryMaxDelayMs());
		return Duration.ofMillis(delay);
	}
}
