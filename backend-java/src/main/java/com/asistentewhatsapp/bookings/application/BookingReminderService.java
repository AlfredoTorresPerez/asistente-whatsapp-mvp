package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.DueReminderRecord;
import com.asistentewhatsapp.bookings.application.BookingReminderProperties.RetryDelay;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.shared.email.EmailTemplateRenderer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingReminderService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingReminderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String INSTANCE_ID = UUID.randomUUID().toString().substring(0, 8);

    private final CompleteAgendaJdbcRepository agendaRepository;
    private final ChannelDispatchService channelDispatchService;
    private final BookingEmailService bookingEmailService;
    private final AuditService auditService;
    private final EmailTemplateRenderer templateRenderer;
    private final BookingReminderProperties properties;

    public BookingReminderService(
            CompleteAgendaJdbcRepository agendaRepository,
            ChannelDispatchService channelDispatchService,
            BookingEmailService bookingEmailService,
            AuditService auditService,
            EmailTemplateRenderer templateRenderer,
            BookingReminderProperties properties) {
        this.agendaRepository = agendaRepository;
        this.channelDispatchService = channelDispatchService;
        this.bookingEmailService = bookingEmailService;
        this.auditService = auditService;
        this.templateRenderer = templateRenderer;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.booking-reminders.worker-interval-ms:60000}")
    public void processDueReminders() {
        if (!properties.isEnabled()) {
            return;
        }
        recoverStaleProcessingRecords();
        List<DueReminderRecord> claimed = agendaRepository.claimDueReminders(
                OffsetDateTime.now(ZoneOffset.UTC), properties.getBatchSize(), INSTANCE_ID);
        for (DueReminderRecord reminder : claimed) {
            processReminder(reminder);
        }
    }

    private void recoverStaleProcessingRecords() {
        try {
            OffsetDateTime timeoutThreshold = OffsetDateTime.now(ZoneOffset.UTC)
                    .minusMinutes(properties.getProcessingTimeoutMinutes());
            int recovered = agendaRepository.recoverStaleProcessingReminders(timeoutThreshold, INSTANCE_ID);
            if (recovered > 0) {
                LOG.warn("BOOKING_REMINDER_RECOVERY recovered={} instance={}", recovered, INSTANCE_ID);
            }
        } catch (RuntimeException e) {
            LOG.warn("BOOKING_REMINDER_RECOVERY_FAILED reason={}", e.getMessage());
        }
    }

    private void processReminder(DueReminderRecord reminder) {
        String bookingStatus = BookingStateMachine.canonical(reminder.bookingStatus());
        if (!BookingStateMachine.CONFIRMED.equals(bookingStatus)
                && !BookingStateMachine.RESCHEDULED.equals(bookingStatus)) {
            agendaRepository.markReminderSkipped(reminder.id(),
                    "La cita no esta confirmada o reprogramada: " + reminder.bookingStatus());
            auditService.record(reminder.businessId(), null, "BOOKING_REMINDER_SKIPPED",
                    "BOOKING", reminder.bookingId(),
                    "Recordatorio saltado: cita " + reminder.bookingStatus());
            return;
        }
        try {
            OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
            if ("EMAIL".equals(reminder.channelType())) {
                sendEmailReminder(reminder);
            } else {
                sendWhatsAppReminder(reminder);
            }
            agendaRepository.markReminderSentWithProvider(reminder.id(), sentAt, null);
            auditService.record(reminder.businessId(), null, "BOOKING_REMINDER_SENT",
                    "BOOKING", reminder.bookingId(),
                    "Recordatorio enviado por " + reminder.channelType() + ".");
        } catch (PermanentFailureException e) {
            agendaRepository.markReminderFailed(reminder.id(), e.getMessage());
            auditService.record(reminder.businessId(), null, "BOOKING_REMINDER_FAILED",
                    "BOOKING", reminder.bookingId(),
                    "Fallo permanente: " + e.getMessage());
        } catch (RuntimeException e) {
            handleRetry(reminder, e);
        }
    }

    private void handleRetry(DueReminderRecord reminder, RuntimeException exception) {
        int maxAttempts = properties.getMaxAttempts();
        RetryDelay retryDelay = properties.getRetryDelay();

        String message = exception.getMessage() != null ? exception.getMessage() : "Error de envio";
        int currentAttempt = getCurrentAttempt(reminder.id());
        String errorCode = classifyError(exception);

        if (currentAttempt >= maxAttempts) {
            agendaRepository.markReminderFailed(reminder.id(), message);
            auditService.record(reminder.businessId(), null, "BOOKING_REMINDER_FAILED",
                    "BOOKING", reminder.bookingId(),
                    "Fallo tras " + currentAttempt + " intentos: " + message);
            LOG.warn("BOOKING_REMINDER_MAX_RETRIES reminderId={} bookingId={} attempts={} errorCode={}",
                    reminder.id(), reminder.bookingId(), currentAttempt, errorCode);
            return;
        }

        int delayMinutes = retryDelay.getDelayMinutesForAttempt(currentAttempt + 1);
        OffsetDateTime nextAttempt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(delayMinutes);
        agendaRepository.markReminderRetry(reminder.id(), nextAttempt, errorCode, message);
        auditService.record(reminder.businessId(), null, "BOOKING_REMINDER_RETRY",
                "BOOKING", reminder.bookingId(),
                "Reintento " + (currentAttempt + 1) + "/" + maxAttempts
                        + " en " + delayMinutes + "min: " + message);
        LOG.warn("BOOKING_REMINDER_RETRY reminderId={} bookingId={} attempt={}/{} delay={}min errorCode={}",
                reminder.id(), reminder.bookingId(), currentAttempt + 1, maxAttempts, delayMinutes, errorCode);
    }

    private int getCurrentAttempt(UUID reminderId) {
        return 0;
    }

    private String classifyError(Throwable exception) {
        if (exception == null) return "UNKNOWN";
        String msg = exception.getMessage();
        if (msg == null) return "UNKNOWN";
        String msgLower = msg.toLowerCase();
        if (msgLower.contains("timeout") || msgLower.contains("timed out")) return "TIMEOUT";
        if (msgLower.contains("connection refused") || msgLower.contains("connect refused")) return "CONNECTION_REFUSED";
        if (msgLower.contains("mail server") || msgLower.contains("smtp")) return "SMTP_ERROR";
        if (msgLower.contains("quota") || msgLower.contains("rate limit")) return "RATE_LIMIT";
        if (msgLower.contains("unreachable") || msgLower.contains("network")) return "NETWORK_ERROR";
        return "TEMPORARY_ERROR";
    }

    private void sendWhatsAppReminder(DueReminderRecord reminder) {
        channelDispatchService.dispatch(new ChannelDispatchRequest(
                reminder.businessId(),
                MessageChannelType.WHATSAPP,
                reminder.customerPhone(),
                buildWhatsAppBody(reminder)));
    }

    private void sendEmailReminder(DueReminderRecord reminder) {
        String htmlBody = build24hReminderHtml(reminder);
        String textBody = buildFallbackText(reminder);
        bookingEmailService.sendBookingEmail(
                reminder.businessId(), reminder.bookingId(), reminder.customerEmail(),
                "BOOKING_REMINDER_24H", buildSubject(reminder), htmlBody);
    }

    private String buildSubject(DueReminderRecord reminder) {
        String businessName = reminder.locationName() != null
                ? reminder.locationName()
                : "nuestra clinica";
        return "Recordatorio: tu cita es manana en " + businessName;
    }

    private String build24hReminderHtml(DueReminderRecord reminder) {
        try {
            AppointmentReminderEmailDTO dto = buildDto(reminder);
            return templateRenderer.render("appointment-reminder-24h", dto);
        } catch (Exception e) {
            LOG.warn("BOOKING_REMINDER_24H_HTML_RENDER_FAILED bookingId={} reason={}",
                    reminder.bookingId(), e.getMessage());
            return buildFallbackText(reminder);
        }
    }

    private AppointmentReminderEmailDTO buildDto(DueReminderRecord reminder) {
        AppointmentReminderEmailDTO dto = new AppointmentReminderEmailDTO();
        dto.setBusinessName(reminder.locationName() != null ? reminder.locationName() : "Centro estetico");
        dto.setCustomerName(reminder.customerName() != null ? reminder.customerName() : "cliente");
        dto.setServiceName(reminder.serviceName() != null ? reminder.serviceName() : "Servicio agendado");
        dto.setAppointmentDate(formatDate(reminder.startsAt()));
        dto.setAppointmentTime(formatTime(reminder.startsAt()));
        dto.setProfessionalName(reminder.professionalName());
        dto.setBranchName(reminder.locationName());
        dto.setAddress(null);
        dto.setPhone(null);
        dto.setEmail(reminder.customerEmail());
        dto.setHelpUrl(null);
        dto.setMinimumNoticeHours(properties.getMinimumRemainingHours());
        return dto;
    }

    private String formatDate(OffsetDateTime value) {
        if (value == null) return "Por confirmar";
        return value.toLocalDate().format(DATE_FORMATTER);
    }

    private String formatTime(OffsetDateTime value) {
        if (value == null) return "Por confirmar";
        return value.toLocalTime().format(TIME_FORMATTER);
    }

    private String buildWhatsAppBody(DueReminderRecord reminder) {
        return "Recordatorio de cita\n\n"
                + "Hola " + valueOrFallback(reminder.customerName(), "cliente") + ".\n"
                + "Servicio: " + valueOrFallback(reminder.serviceName(), reminder.subject()) + "\n"
                + "Fecha: " + formatDateTime(reminder.startsAt()) + "\n"
                + "Sucursal: " + valueOrFallback(reminder.locationName(), "Por confirmar") + "\n"
                + "Profesional: " + valueOrFallback(reminder.professionalName(), "Por asignar");
    }

    private String buildFallbackText(DueReminderRecord reminder) {
        return "Recordatorio: tu cita es manana\n\n"
                + "Hola " + valueOrFallback(reminder.customerName(), "cliente") + ",\n\n"
                + "Te recordamos que manana tienes una atencion agendada.\n\n"
                + "Servicio: " + valueOrFallback(reminder.serviceName(), reminder.subject()) + "\n"
                + "Fecha: " + formatDateTime(reminder.startsAt()) + "\n"
                + "Sucursal: " + valueOrFallback(reminder.locationName(), "Por confirmar") + "\n"
                + "Profesional: " + valueOrFallback(reminder.professionalName(), "Por asignar") + "\n\n"
                + "Si necesitas reprogramar o cancelar, contactanos por WhatsApp.";
    }

    private String formatDateTime(OffsetDateTime value) {
        if (value == null) return "Sin fecha";
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(value);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static class PermanentFailureException extends RuntimeException {
        public PermanentFailureException(String message) {
            super(message);
        }
    }
}
