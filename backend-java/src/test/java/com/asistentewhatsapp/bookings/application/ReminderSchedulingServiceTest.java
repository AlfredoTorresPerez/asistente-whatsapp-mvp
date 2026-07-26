package com.asistentewhatsapp.bookings.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReminderSchedulingServiceTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();

    private CompleteAgendaJdbcRepository agendaRepository;
    private ReminderSchedulingService service;

    @BeforeEach
    void setUp() {
        agendaRepository = mock(CompleteAgendaJdbcRepository.class);
        service = new ReminderSchedulingService(agendaRepository);
    }

    @Test
    void scheduleDefaultRemindersWithFutureStartsAtInsertsFourReminders() {
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        when(agendaRepository.findMaxReminderRevision(any(), any(), anyString(), anyString())).thenReturn(0);

        service.scheduleDefaultReminders(BUSINESS_ID, BOOKING_ID, startsAt);

        verify(agendaRepository).findMaxReminderRevision(BUSINESS_ID, BOOKING_ID, "TWENTY_FOUR_HOURS_BEFORE", "EMAIL");
        verify(agendaRepository).cancelReminderByBooking(BUSINESS_ID, BOOKING_ID);
        verify(agendaRepository, times(4)).insertReminderWithRevision(any(), any(), anyString(), anyString(), any(), anyInt());
    }

    @Test
    void scheduleDefaultRemindersUsesIncrementedRevision() {
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        when(agendaRepository.findMaxReminderRevision(any(), any(), anyString(), anyString())).thenReturn(3);

        service.scheduleDefaultReminders(BUSINESS_ID, BOOKING_ID, startsAt);

        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWENTY_FOUR_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(24), 4);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWENTY_FOUR_HOURS_BEFORE", "EMAIL", startsAt.minusHours(24), 4);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWO_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(2), 4);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWO_HOURS_BEFORE", "EMAIL", startsAt.minusHours(2), 4);
    }

    @Test
    void scheduleDefaultRemindersWithNullStartsAtSkips() {
        service.scheduleDefaultReminders(BUSINESS_ID, BOOKING_ID, null);

        verify(agendaRepository, never()).findMaxReminderRevision(any(), any(), anyString(), anyString());
        verify(agendaRepository, never()).cancelReminderByBooking(any(), any());
        verify(agendaRepository, never()).insertReminderWithRevision(any(), any(), anyString(), anyString(), any(), anyInt());
    }

    @Test
    void scheduleDefaultRemindersWhenBothScheduledAtInPastSkipsAllInserts() {
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        when(agendaRepository.findMaxReminderRevision(any(), any(), anyString(), anyString())).thenReturn(0);

        service.scheduleDefaultReminders(BUSINESS_ID, BOOKING_ID, startsAt);

        verify(agendaRepository).cancelReminderByBooking(BUSINESS_ID, BOOKING_ID);
        verify(agendaRepository, never()).insertReminderWithRevision(any(), any(), anyString(), anyString(), any(), anyInt());
    }

    @Test
    void scheduleDefaultRemindersWhen24hInPastBut2hFutureInsertsOnly2h() {
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(5);
        when(agendaRepository.findMaxReminderRevision(any(), any(), anyString(), anyString())).thenReturn(null);

        service.scheduleDefaultReminders(BUSINESS_ID, BOOKING_ID, startsAt);

        verify(agendaRepository).cancelReminderByBooking(BUSINESS_ID, BOOKING_ID);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWO_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(2), 0);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWO_HOURS_BEFORE", "EMAIL", startsAt.minusHours(2), 0);
        verify(agendaRepository, never()).insertReminderWithRevision(any(), any(), eq("TWENTY_FOUR_HOURS_BEFORE"), anyString(), any(), anyInt());
    }

    @Test
    void cancelRemindersDelegatesToRepository() {
        service.cancelReminders(BUSINESS_ID, BOOKING_ID);

        verify(agendaRepository).cancelReminderByBooking(BUSINESS_ID, BOOKING_ID);
    }

    @Test
    void scheduleDefaultRemindersWithFirstRevisionUsesZero() {
        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        when(agendaRepository.findMaxReminderRevision(any(), any(), anyString(), anyString())).thenReturn(null);

        service.scheduleDefaultReminders(BUSINESS_ID, BOOKING_ID, startsAt);

        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWENTY_FOUR_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(24), 0);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWENTY_FOUR_HOURS_BEFORE", "EMAIL", startsAt.minusHours(24), 0);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWO_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(2), 0);
        verify(agendaRepository).insertReminderWithRevision(BUSINESS_ID, BOOKING_ID, "TWO_HOURS_BEFORE", "EMAIL", startsAt.minusHours(2), 0);
    }
}
