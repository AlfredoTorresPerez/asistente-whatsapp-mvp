package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.DueReminderRecord;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.application.AuditService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingReminderService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CompleteAgendaJdbcRepository agendaRepository;
    private final ChannelDispatchService channelDispatchService;
    private final BookingEmailService bookingEmailService;
    private final AuditService auditService;

    public BookingReminderService(
            CompleteAgendaJdbcRepository agendaRepository,
            ChannelDispatchService channelDispatchService,
            BookingEmailService bookingEmailService,
            AuditService auditService) {
        this.agendaRepository = agendaRepository;
        this.channelDispatchService = channelDispatchService;
        this.bookingEmailService = bookingEmailService;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${app.booking-reminders.worker-interval-ms:60000}")
    @Transactional
    public void sendDueReminders() {
        for (DueReminderRecord reminder : agendaRepository.findDueReminders(OffsetDateTime.now(ZoneOffset.UTC), 25)) {
            String bookingStatus = BookingStateMachine.canonical(reminder.bookingStatus());
            if (!BookingStateMachine.CONFIRMED.equals(bookingStatus) && !BookingStateMachine.RESCHEDULED.equals(bookingStatus)) {
                agendaRepository.markReminderSkipped(reminder.id(), "La cita no esta confirmada.");
                continue;
            }
            try {
                if ("EMAIL".equals(reminder.channelType())) {
                    sendEmailReminder(reminder);
                } else {
                    sendWhatsAppReminder(reminder);
                }
                OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
                agendaRepository.markReminderSent(reminder.id(), sentAt);
                auditService.record(reminder.businessId(), null, "BOOKING_REMINDER_SENT", "BOOKING", reminder.bookingId(),
                        "Recordatorio enviado por " + reminder.channelType() + ".");
            } catch (RuntimeException exception) {
                String message = exception.getMessage() == null ? "Error de envio" : exception.getMessage();
                agendaRepository.markReminderFailed(reminder.id(), message);
                auditService.record(reminder.businessId(), null, "BOOKING_REMINDER_FAILED", "BOOKING", reminder.bookingId(),
                        "Fallo recordatorio por " + reminder.channelType() + ": " + message);
            }
        }
    }

    private void sendWhatsAppReminder(DueReminderRecord reminder) {
        channelDispatchService.dispatch(new ChannelDispatchRequest(
                reminder.businessId(),
                MessageChannelType.WHATSAPP,
                reminder.customerPhone(),
                buildReminderBody(reminder)));
    }

    private void sendEmailReminder(DueReminderRecord reminder) {
        String body = bookingEmailService.buildAppointmentEmailBody(
                reminder.customerName(),
                buildReminderBody(reminder),
                reminder.serviceName() == null ? reminder.subject() : reminder.serviceName(),
                formatDateTime(reminder.startsAt()),
                reminder.locationName(),
                reminder.professionalName(),
                reminder.roomName(),
                "Responde a nuestro WhatsApp si necesitas reprogramar o cancelar.",
                "Gracias por preferirnos.");
        bookingEmailService.sendBookingEmail(reminder.businessId(), reminder.bookingId(), reminder.customerEmail(),
                "BOOKING_REMINDER", "Recordatorio de tu cita", body);
    }

    private String buildReminderBody(DueReminderRecord reminder) {
        return "Recordatorio de cita\n\n"
                + "Hola " + valueOrFallback(reminder.customerName(), "cliente") + ".\n"
                + "Servicio: " + valueOrFallback(reminder.serviceName(), reminder.subject()) + "\n"
                + "Fecha: " + formatDateTime(reminder.startsAt()) + "\n"
                + "Sucursal: " + valueOrFallback(reminder.locationName(), "Por confirmar") + "\n"
                + "Profesional: " + valueOrFallback(reminder.professionalName(), "Por asignar") + "\n"
                + "Cabina: " + valueOrFallback(reminder.roomName(), "No requerida") + "\n\n"
                + "Si necesitas reprogramar o cancelar, responde este WhatsApp para enviarte un enlace publico seguro.";
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "Sin fecha" : value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
