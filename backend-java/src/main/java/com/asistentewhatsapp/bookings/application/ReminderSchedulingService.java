package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReminderSchedulingService {

    private static final Logger LOG = LoggerFactory.getLogger(ReminderSchedulingService.class);

    private final CompleteAgendaJdbcRepository agendaRepository;

    public ReminderSchedulingService(CompleteAgendaJdbcRepository agendaRepository) {
        this.agendaRepository = agendaRepository;
    }

    public void scheduleDefaultReminders(UUID businessId, UUID bookingId, OffsetDateTime startsAt) {
        if (skipped(startsAt)) return;
        int revision = nextRevision(businessId, bookingId);
        cancelReminders(businessId, bookingId);
        schedule24h(businessId, bookingId, startsAt, revision);
        schedule2h(businessId, bookingId, startsAt, revision);
        LOG.info("REMINDER_SCHEDULED businessId={} bookingId={} revision={}", businessId, bookingId, revision);
    }

    public void cancelReminders(UUID businessId, UUID bookingId) {
        agendaRepository.cancelReminderByBooking(businessId, bookingId);
    }

    private boolean skipped(OffsetDateTime startsAt) {
        if (startsAt == null) {
            LOG.warn("REMINDER_SCHEDULER_SKIPPED null startsAt");
            return true;
        }
        return false;
    }

    private int nextRevision(UUID businessId, UUID bookingId) {
        Integer max = agendaRepository.findMaxReminderRevision(businessId, bookingId, "TWENTY_FOUR_HOURS_BEFORE", "EMAIL");
        return max != null ? max + 1 : 0;
    }

    private void schedule24h(UUID businessId, UUID bookingId, OffsetDateTime startsAt, int revision) {
        OffsetDateTime scheduledAt = startsAt.minusHours(24);
        if (scheduledAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            LOG.warn("REMINDER_SCHEDULER_24H_SKIPPED startsAt={} scheduledAt={} is in the past", startsAt, scheduledAt);
            return;
        }
        agendaRepository.insertReminderWithRevision(businessId, bookingId, "TWENTY_FOUR_HOURS_BEFORE", "WHATSAPP", scheduledAt, revision);
        agendaRepository.insertReminderWithRevision(businessId, bookingId, "TWENTY_FOUR_HOURS_BEFORE", "EMAIL", scheduledAt, revision);
    }

    private void schedule2h(UUID businessId, UUID bookingId, OffsetDateTime startsAt, int revision) {
        OffsetDateTime scheduledAt = startsAt.minusHours(2);
        if (scheduledAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            LOG.warn("REMINDER_SCHEDULER_2H_SKIPPED startsAt={} scheduledAt={} is in the past", startsAt, scheduledAt);
            return;
        }
        agendaRepository.insertReminderWithRevision(businessId, bookingId, "TWO_HOURS_BEFORE", "WHATSAPP", scheduledAt, revision);
        agendaRepository.insertReminderWithRevision(businessId, bookingId, "TWO_HOURS_BEFORE", "EMAIL", scheduledAt, revision);
    }
}
