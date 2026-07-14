package com.asistentewhatsapp.bookings.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.bookings.domain.BookingSyncEventRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingSyncEventJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingSyncJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingSyncEventProcessorTest {

    private final BookingSyncEventJdbcRepository eventRepository = mock(BookingSyncEventJdbcRepository.class);
    private final BookingSyncJdbcRepository syncRepository = mock(BookingSyncJdbcRepository.class);
    private final BookingSyncProperties properties = new BookingSyncProperties();
    private final BookingSyncEventProcessor processor = new BookingSyncEventProcessor(eventRepository, syncRepository, properties);

    private final UUID eventId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        properties.setEventBatchSize(10);
        properties.setEventProcessingTimeoutMs(120000);
    }

    private BookingSyncEventRecord event(String status, int attempts, int maxAttempts) {
        return new BookingSyncEventRecord(
                eventId, bookingId, businessId, "RESERVA_CREADA", 1,
                "{}", "key-" + bookingId, status, attempts, maxAttempts,
                now, null, null, null, "trace-001", now, now);
    }

    @Test
    void processDueEventsDoesNothingWhenNoEventsClaimed() {
        when(eventRepository.claimDueEvents(10, 120000)).thenReturn(List.of());

        processor.processDueEvents();

        verify(syncRepository, never()).updateBookingFactStatus(any(), any());
    }

    @Test
    void processDueEventsMarksEventAsSyncedWhenAlreadySynced() {
        BookingSyncEventRecord ev = event("PROCESSING", 1, 5);
        when(eventRepository.claimDueEvents(10, 120000)).thenReturn(List.of(ev));
        when(syncRepository.findEventByIdempotencyKey("key-" + bookingId))
                .thenReturn(Optional.of(event("SYNCED", 1, 5)));

        processor.processDueEvents();

        verify(eventRepository).markSynced(eq(eventId), any());
        verify(syncRepository, never()).updateBookingFactStatus(any(), any());
    }

    @Test
    void processDueEventsUpdatesFactStatusAndMarksSynced() {
        BookingSyncEventRecord ev = event("PROCESSING", 1, 5);
        when(eventRepository.claimDueEvents(10, 120000)).thenReturn(List.of(ev));
        when(syncRepository.findEventByIdempotencyKey("key-" + bookingId))
                .thenReturn(Optional.empty());

        processor.processDueEvents();

        verify(syncRepository).updateBookingFactStatus(bookingId, "SYNCED");
        verify(eventRepository).markSynced(eq(eventId), any());
    }

    @Test
    void processDueEventsSkipsUnsupportedEventType() {
        BookingSyncEventRecord ev = new BookingSyncEventRecord(
                eventId, bookingId, businessId, "UNSUPPORTED_TYPE", 1,
                "{}", "key", "PROCESSING", 1, 5,
                now, null, null, null, "trace", now, now);
        when(eventRepository.claimDueEvents(10, 120000)).thenReturn(List.of(ev));

        processor.processDueEvents();

        verify(eventRepository).markSkipped(eventId, "UNSUPPORTED_EVENT_TYPE_UNSUPPORTED_TYPE");
        verify(syncRepository, never()).updateBookingFactStatus(any(), any());
    }

    @Test
    void processDueEventsMarksFailedWhenMaxAttemptsExceeded() {
        BookingSyncEventRecord ev = event("PROCESSING", 5, 5);
        when(eventRepository.claimDueEvents(10, 120000)).thenReturn(List.of(ev));
        when(syncRepository.findEventByIdempotencyKey(any())).thenThrow(new RuntimeException("DB error"));

        processor.processDueEvents();

        verify(eventRepository).markFailedOrRetry(
                eq(eventId), eq(5), eq(5),
                eq("RuntimeException"), eq("DB error"), any());
    }

    @Test
    void processDueEventsRetriesWhenAttemptsBelowMax() {
        BookingSyncEventRecord ev = event("PROCESSING", 1, 5);
        when(eventRepository.claimDueEvents(10, 120000)).thenReturn(List.of(ev));
        when(syncRepository.findEventByIdempotencyKey(any())).thenThrow(new RuntimeException("DB error"));

        processor.processDueEvents();

        verify(eventRepository).markFailedOrRetry(
                eq(eventId), eq(1), eq(5),
                eq("RuntimeException"), eq("DB error"), any());
    }

    @Test
    void processDueEventsHandlesMultipleEvents() {
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID bookingId2 = UUID.randomUUID();
        BookingSyncEventRecord ev1 = new BookingSyncEventRecord(
                eventId1, bookingId, businessId, "RESERVA_CREADA", 1,
                "{}", "k1", "PROCESSING", 1, 5,
                now, null, null, null, "trace", now, now);
        BookingSyncEventRecord ev2 = new BookingSyncEventRecord(
                eventId2, bookingId2, businessId, "RESERVA_CREADA", 1,
                "{}", "k2", "PROCESSING", 1, 5,
                now, null, null, null, "trace", now, now);
        when(eventRepository.claimDueEvents(10, 120000)).thenReturn(List.of(ev1, ev2));
        when(syncRepository.findEventByIdempotencyKey(any())).thenReturn(Optional.empty());

        processor.processDueEvents();

        verify(syncRepository).updateBookingFactStatus(bookingId, "SYNCED");
        verify(syncRepository).updateBookingFactStatus(bookingId2, "SYNCED");
        verify(eventRepository).markSynced(eq(eventId1), any());
        verify(eventRepository).markSynced(eq(eventId2), any());
    }
}
