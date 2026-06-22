package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.bookings.api.BookingPublicActionLinkResponse;
import com.asistentewhatsapp.bookings.api.CreateBookingCancellationLinkRequest;
import com.asistentewhatsapp.bookings.api.CreateBookingRescheduleLinkRequest;
import com.asistentewhatsapp.bookings.api.PublicBookingCancellationRequest;
import com.asistentewhatsapp.bookings.api.PublicBookingCancellationResponse;
import com.asistentewhatsapp.bookings.api.PublicBookingRescheduleResponse;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository.ActionBookingRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository.CancellationLinkRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository.RescheduleLinkRecord;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.security.application.TokenHashService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingPublicActionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BookingActionLinkJdbcRepository repository;
    private final CompleteAgendaJdbcRepository agendaRepository;
    private final TokenHashService tokenHashService;
    private final AuditService auditService;
    private final ChannelDispatchService channelDispatchService;
    private final BookingEmailService bookingEmailService;
    private final String reschedulePublicBaseUrl;
    private final String cancellationPublicBaseUrl;

    public BookingPublicActionService(
            BookingActionLinkJdbcRepository repository,
            CompleteAgendaJdbcRepository agendaRepository,
            TokenHashService tokenHashService,
            AuditService auditService,
            ChannelDispatchService channelDispatchService,
            BookingEmailService bookingEmailService,
            @Value("${app.booking-reschedule.public-base-url}") String reschedulePublicBaseUrl,
            @Value("${app.booking-cancellation.public-base-url}") String cancellationPublicBaseUrl) {
        this.repository = repository;
        this.agendaRepository = agendaRepository;
        this.tokenHashService = tokenHashService;
        this.auditService = auditService;
        this.channelDispatchService = channelDispatchService;
        this.bookingEmailService = bookingEmailService;
        this.reschedulePublicBaseUrl = reschedulePublicBaseUrl;
        this.cancellationPublicBaseUrl = cancellationPublicBaseUrl;
    }

    @Transactional
    public BookingPublicActionLinkResponse createRescheduleLink(AuthenticatedUser user, UUID bookingId,
            CreateBookingRescheduleLinkRequest request) {
        ActionBookingRecord booking = repository.findBooking(user.businessId(), bookingId);
        ensureCanChange(booking.bookingStatus(), "reprogramarse");
        if (booking.rescheduleCount() >= 3) {
            throw validationError("rescheduleCount", "La cita alcanzo el maximo de reprogramaciones permitido.");
        }

        CompleteAgendaJdbcRepository.LocationRecord location = agendaRepository.findLocation(user.businessId(), request.locationId());
        int durationMinutes = booking.durationMinutes();
        if (request.serviceId() != null) {
            CompleteAgendaJdbcRepository.ServiceRecord service = agendaRepository.findService(user.businessId(), request.locationId(), request.serviceId());
            durationMinutes = service.durationMinutes();
        }
        OffsetDateTime proposedStartsAt = normalizeFutureStartsAt(request.startsAt());
        OffsetDateTime proposedEndsAt = proposedStartsAt.plusMinutes(durationMinutes);
        ensureAgendaSlotAvailable(user.businessId(), bookingId, location.id(), request.professionalId(), request.roomId(),
                proposedStartsAt, proposedEndsAt);

        repository.invalidateActiveRescheduleLinks(user.businessId(), bookingId);
        String token = generateToken();
        String publicUrl = buildPublicUrl(reschedulePublicBaseUrl, token);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(resolveExpirationMinutes(request.expirationMinutes()));
        UUID linkId = repository.insertRescheduleLink(user.businessId(), bookingId, tokenHashService.sha256(token), publicUrl,
                proposedStartsAt, proposedEndsAt, location.id(), request.serviceId(), request.professionalId(), request.roomId(),
                expiresAt, normalizeOptionalText(request.reason(), 2000), user.userId(), "ADMIN");
        auditService.record(user.businessId(), user.userId(), "BOOKING_RESCHEDULE_LINK_CREATED", "BOOKING", bookingId,
                "Se genero enlace publico de reprogramacion.",
                AuditMetadata.of(
                        "linkId", linkId,
                        "previousStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "targetStatus", BookingStateMachine.RESCHEDULED,
                        "linkExpiresAt", expiresAt,
                        "proposedStartsAt", proposedStartsAt,
                        "proposedEndsAt", proposedEndsAt,
                        "locationId", location.id(),
                        "serviceId", request.serviceId(),
                        "professionalId", request.professionalId(),
                        "roomId", request.roomId()));

        OffsetDateTime whatsappSentAt = null;
        if (!Boolean.FALSE.equals(request.sendWhatsApp())) {
            whatsappSentAt = sendWhatsApp(booking, buildRescheduleMessage(booking, proposedStartsAt, location.name(), publicUrl, expiresAt),
                    user.userId(), "BOOKING_RESCHEDULE_WHATSAPP_SENT");
        }
        OffsetDateTime emailSentAt = null;
        if (!Boolean.FALSE.equals(request.sendEmail())) {
            emailSentAt = sendEmail(booking, "BOOKING_RESCHEDULE_PENDING",
                    "Confirma la reprogramacion de tu reserva",
                    "Tenemos una nueva propuesta de horario para tu cita. Confirma o rechaza el cambio desde el enlace.",
                    location.name(), publicUrl);
            auditService.record(user.businessId(), user.userId(), "BOOKING_RESCHEDULE_EMAIL_SENT", "BOOKING", bookingId,
                    "Enlace de reprogramacion enviado por correo.");
        }
        return new BookingPublicActionLinkResponse(linkId, bookingId, "RESCHEDULE", "ACTIVE", publicUrl, expiresAt, whatsappSentAt, emailSentAt);
    }

    @Transactional
    public BookingPublicActionLinkResponse createCancellationLink(AuthenticatedUser user, UUID bookingId,
            CreateBookingCancellationLinkRequest request) {
        ActionBookingRecord booking = repository.findBooking(user.businessId(), bookingId);
        ensureCanChange(booking.bookingStatus(), "cancelarse");
        repository.invalidateActiveCancellationLinks(user.businessId(), bookingId);
        String token = generateToken();
        String publicUrl = buildPublicUrl(cancellationPublicBaseUrl, token);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(resolveExpirationMinutes(request == null ? null : request.expirationMinutes()));
        String reason = request == null ? null : normalizeOptionalText(request.reason(), 2000);
        UUID linkId = repository.insertCancellationLink(user.businessId(), bookingId, tokenHashService.sha256(token), publicUrl,
                expiresAt, reason, user.userId(), "ADMIN");
        auditService.record(user.businessId(), user.userId(), "BOOKING_CANCELLATION_LINK_CREATED", "BOOKING", bookingId,
                "Se genero enlace publico de cancelacion.",
                AuditMetadata.of(
                        "linkId", linkId,
                        "previousStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "targetStatus", BookingStateMachine.CANCELLED,
                        "linkExpiresAt", expiresAt,
                        "reason", request.reason()));

        OffsetDateTime whatsappSentAt = null;
        if (request == null || !Boolean.FALSE.equals(request.sendWhatsApp())) {
            whatsappSentAt = sendWhatsApp(booking, buildCancellationMessage(booking, publicUrl, expiresAt),
                    user.userId(), "BOOKING_CANCELLATION_WHATSAPP_SENT");
        }
        OffsetDateTime emailSentAt = null;
        if (request == null || !Boolean.FALSE.equals(request.sendEmail())) {
            emailSentAt = sendEmail(booking, "BOOKING_CANCELLATION_PENDING",
                    "Confirma la cancelacion de tu reserva",
                    "Recibimos una solicitud de cancelacion. Confirma desde el enlace si quieres liberar el cupo.",
                    booking.locationName(), publicUrl);
            auditService.record(user.businessId(), user.userId(), "BOOKING_CANCELLATION_EMAIL_SENT", "BOOKING", bookingId,
                    "Enlace de cancelacion enviado por correo.");
        }
        return new BookingPublicActionLinkResponse(linkId, bookingId, "CANCELLATION", "ACTIVE", publicUrl, expiresAt, whatsappSentAt, emailSentAt);
    }

    @Transactional
    public PublicBookingRescheduleResponse previewReschedule(String rawToken) {
        RescheduleLinkRecord link = repository.findRescheduleByTokenHash(tokenHashService.sha256(normalizeToken(rawToken)), false);
        if (isExpired(link.expiresAt(), link.linkStatus())) {
            repository.markExpiredReschedule(link.linkId());
            link = repository.findRescheduleByTokenHash(tokenHashService.sha256(normalizeToken(rawToken)), false);
        }
        return toRescheduleResponse(link);
    }

    @Transactional
    public PublicBookingRescheduleResponse confirmReschedule(String rawToken) {
        String tokenHash = tokenHashService.sha256(normalizeToken(rawToken));
        RescheduleLinkRecord link = repository.findRescheduleByTokenHash(tokenHash, true);
        if ("USED".equals(link.linkStatus()) || BookingStateMachine.RESCHEDULED.equals(BookingStateMachine.canonical(link.bookingStatus()))) {
            return toRescheduleResponse(link);
        }
        if (isExpired(link.expiresAt(), link.linkStatus())) {
            repository.markExpiredReschedule(link.linkId());
            return toRescheduleResponse(repository.findRescheduleByTokenHash(tokenHash, false));
        }
        ActionBookingRecord booking = repository.findBookingForUpdate(link.businessId(), link.bookingId());
        ensureCanChange(booking.bookingStatus(), "reprogramarse");
        ensureAgendaSlotAvailable(link.businessId(), link.bookingId(), link.proposedLocationId(), link.proposedProfessionalId(),
                link.proposedRoomId(), link.proposedStartsAt(), link.proposedEndsAt());
        agendaRepository.updateBookingSchedule(link.businessId(), link.bookingId(), null, link.proposedLocationId(),
                link.proposedServiceId(), link.proposedProfessionalId(), link.proposedRoomId(), link.proposedStartsAt(),
                link.proposedEndsAt(), (int) java.time.Duration.between(link.proposedStartsAt(), link.proposedEndsAt()).toMinutes(),
                link.reason() == null ? "Reprogramacion confirmada por enlace publico." : link.reason());
        repository.markRescheduleUsed(link.linkId());
        scheduleConfirmedBookingReminders(link.businessId(), link.bookingId(), link.proposedStartsAt());
        auditService.record(link.businessId(), null, "BOOKING_RESCHEDULE_CONFIRMED", "BOOKING", link.bookingId(),
                "Cliente confirmo reprogramacion desde enlace publico.",
                AuditMetadata.of(
                        "source", "PUBLIC_LINK",
                        "linkId", link.linkId(),
                        "previousStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "newStatus", BookingStateMachine.RESCHEDULED,
                        "proposedStartsAt", link.proposedStartsAt(),
                        "proposedEndsAt", link.proposedEndsAt(),
                        "locationId", link.proposedLocationId(),
                        "serviceId", link.proposedServiceId(),
                        "professionalId", link.proposedProfessionalId(),
                        "roomId", link.proposedRoomId()));
        sendEmail(booking, "BOOKING_RESCHEDULE_CONFIRMED", "Tu reserva fue reprogramada",
                "Tu cita fue reprogramada correctamente.", link.proposedLocationName(), link.publicUrl());
        return toRescheduleResponse(repository.findRescheduleByTokenHash(tokenHash, false));
    }

    @Transactional
    public PublicBookingRescheduleResponse rejectReschedule(String rawToken) {
        String tokenHash = tokenHashService.sha256(normalizeToken(rawToken));
        RescheduleLinkRecord link = repository.findRescheduleByTokenHash(tokenHash, true);
        if ("ACTIVE".equals(link.linkStatus())) {
            repository.markRescheduleRejected(link.linkId());
            auditService.record(link.businessId(), null, "BOOKING_RESCHEDULE_REJECTED", "BOOKING", link.bookingId(),
                    "Cliente rechazo reprogramacion desde enlace publico.",
                    AuditMetadata.of(
                            "source", "PUBLIC_LINK",
                            "linkId", link.linkId(),
                            "linkStatus", link.linkStatus()));
        }
        return toRescheduleResponse(repository.findRescheduleByTokenHash(tokenHash, false));
    }

    @Transactional
    public PublicBookingCancellationResponse previewCancellation(String rawToken) {
        CancellationLinkRecord link = repository.findCancellationByTokenHash(tokenHashService.sha256(normalizeToken(rawToken)), false);
        if (isExpired(link.expiresAt(), link.linkStatus())) {
            repository.markExpiredCancellation(link.linkId());
            link = repository.findCancellationByTokenHash(tokenHashService.sha256(normalizeToken(rawToken)), false);
        }
        return toCancellationResponse(link);
    }

    @Transactional
    public PublicBookingCancellationResponse confirmCancellation(String rawToken, PublicBookingCancellationRequest request) {
        String tokenHash = tokenHashService.sha256(normalizeToken(rawToken));
        CancellationLinkRecord link = repository.findCancellationByTokenHash(tokenHash, true);
        if ("USED".equals(link.linkStatus()) || BookingStateMachine.CANCELLED.equals(BookingStateMachine.canonical(link.bookingStatus()))) {
            return toCancellationResponse(link);
        }
        if (isExpired(link.expiresAt(), link.linkStatus())) {
            repository.markExpiredCancellation(link.linkId());
            return toCancellationResponse(repository.findCancellationByTokenHash(tokenHash, false));
        }
        ActionBookingRecord booking = repository.findBookingForUpdate(link.businessId(), link.bookingId());
        ensureCanChange(booking.bookingStatus(), "cancelarse");
        String reason = request != null && request.reason() != null && !request.reason().isBlank()
                ? normalizeOptionalText(request.reason(), 2000)
                : link.cancellationReason();
        agendaRepository.cancelBooking(link.businessId(), link.bookingId(), null,
                reason == null ? "Cancelacion confirmada por enlace publico." : reason);
        repository.markCancellationUsed(link.linkId(), reason);
        auditService.record(link.businessId(), null, "BOOKING_CANCELLED", "BOOKING", link.bookingId(),
                "Cliente cancelo reserva desde enlace publico.",
                AuditMetadata.of(
                        "source", "PUBLIC_LINK",
                        "linkId", link.linkId(),
                        "previousStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "newStatus", BookingStateMachine.CANCELLED,
                        "reason", reason));
        sendEmail(booking, "BOOKING_CANCELLATION_CONFIRMED", "Tu reserva fue cancelada",
                "Tu cita fue cancelada y el cupo quedo liberado.", link.locationName(), link.publicUrl());
        return toCancellationResponse(repository.findCancellationByTokenHash(tokenHash, false));
    }

    private void scheduleConfirmedBookingReminders(UUID businessId, UUID bookingId, OffsetDateTime startsAt) {
        agendaRepository.cancelPendingReminders(businessId, bookingId);
        agendaRepository.insertReminder(businessId, bookingId, "TWENTY_FOUR_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(24));
        agendaRepository.insertReminder(businessId, bookingId, "TWENTY_FOUR_HOURS_BEFORE", "EMAIL", startsAt.minusHours(24));
        agendaRepository.insertReminder(businessId, bookingId, "TWO_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(2));
        agendaRepository.insertReminder(businessId, bookingId, "TWO_HOURS_BEFORE", "EMAIL", startsAt.minusHours(2));
        auditService.record(businessId, null, "BOOKING_REMINDER_SCHEDULED", "BOOKING", bookingId,
                "Recordatorios WhatsApp y correo recalculados para la cita confirmada.");
    }

    private OffsetDateTime sendWhatsApp(ActionBookingRecord booking, String body, UUID actorUserId, String auditEvent) {
        try {
            channelDispatchService.dispatch(new ChannelDispatchRequest(booking.businessId(), MessageChannelType.WHATSAPP, booking.customerPhone(), body));
            OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
            auditService.record(booking.businessId(), actorUserId, auditEvent, "BOOKING", booking.bookingId(),
                    "Mensaje operativo enviado por WhatsApp.");
            return sentAt;
        } catch (RuntimeException exception) {
            auditService.record(booking.businessId(), actorUserId, auditEvent + "_FAILED", "BOOKING", booking.bookingId(),
                    "Fallo envio WhatsApp: " + safeMessage(exception));
            return null;
        }
    }

    private OffsetDateTime sendEmail(ActionBookingRecord booking, String templateKey, String subject, String actionText,
            String locationName, String publicUrl) {
        String body = bookingEmailService.buildAppointmentEmailBody(
                booking.customerName(),
                actionText,
                booking.serviceName() == null ? booking.subject() : booking.serviceName(),
                formatDateTime(booking.startsAt()),
                locationName,
                booking.professionalName(),
                booking.roomName(),
                publicUrl,
                "Si no solicitaste este cambio, responde a nuestro WhatsApp para revisar tu reserva.");
        return bookingEmailService.sendBookingEmail(booking.businessId(), booking.bookingId(), booking.customerEmail(),
                templateKey, subject, body);
    }

    private String buildRescheduleMessage(ActionBookingRecord booking, OffsetDateTime proposedStartsAt, String locationName,
            String publicUrl, OffsetDateTime expiresAt) {
        return "Solicitud de reprogramacion\n\n"
                + "Cliente: " + booking.customerName() + "\n"
                + "Servicio: " + valueOrFallback(booking.serviceName(), booking.subject()) + "\n"
                + "Nueva fecha: " + formatDateTime(proposedStartsAt) + "\n"
                + "Sucursal: " + valueOrFallback(locationName, "Por confirmar") + "\n"
                + "Confirma o rechaza aqui: " + publicUrl + "\n"
                + "Enlace vigente hasta: " + formatDateTime(expiresAt);
    }

    private String buildCancellationMessage(ActionBookingRecord booking, String publicUrl, OffsetDateTime expiresAt) {
        return "Solicitud de cancelacion\n\n"
                + "Cliente: " + booking.customerName() + "\n"
                + "Servicio: " + valueOrFallback(booking.serviceName(), booking.subject()) + "\n"
                + "Fecha: " + formatDateTime(booking.startsAt()) + "\n"
                + "Confirma la cancelacion aqui: " + publicUrl + "\n"
                + "Enlace vigente hasta: " + formatDateTime(expiresAt);
    }

    private void ensureAgendaSlotAvailable(UUID businessId, UUID bookingId, UUID locationId, UUID professionalId, UUID roomId,
            OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (agendaRepository.hasConflict(businessId, bookingId, locationId, professionalId, roomId, startsAt, endsAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_SLOT_NOT_AVAILABLE",
                    "El horario ya esta ocupado.", Map.of("startsAt", "Selecciona otro horario disponible."));
        }
        if (agendaRepository.hasBlock(businessId, locationId, professionalId, roomId, startsAt, endsAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_SLOT_BLOCKED",
                    "El horario esta bloqueado.", Map.of("startsAt", "Selecciona otro horario disponible."));
        }
    }

    private PublicBookingRescheduleResponse toRescheduleResponse(RescheduleLinkRecord link) {
        return new PublicBookingRescheduleResponse(
                link.bookingId(),
                normalizeStatusForApi(link.bookingStatus()),
                link.linkStatus(),
                link.subject(),
                link.proposedServiceName() == null ? link.currentServiceName() : link.proposedServiceName(),
                link.currentLocationName(),
                link.proposedLocationName(),
                link.currentProfessionalName(),
                link.proposedProfessionalName(),
                link.currentRoomName(),
                link.proposedRoomName(),
                link.currentStartsAt(),
                link.proposedStartsAt(),
                link.proposedEndsAt(),
                link.customerName(),
                maskPhone(link.customerPhone()),
                link.expiresAt(),
                link.usedAt(),
                link.reason());
    }

    private PublicBookingCancellationResponse toCancellationResponse(CancellationLinkRecord link) {
        return new PublicBookingCancellationResponse(
                link.bookingId(),
                normalizeStatusForApi(link.bookingStatus()),
                link.linkStatus(),
                link.subject(),
                link.serviceName(),
                link.locationName(),
                link.professionalName(),
                link.roomName(),
                link.startsAt(),
                link.endsAt(),
                link.customerName(),
                maskPhone(link.customerPhone()),
                link.expiresAt(),
                link.usedAt(),
                link.cancellationReason());
    }

    private void ensureCanChange(String status, String action) {
        BookingStateMachine.assertCanChange(status, action);
    }

    private boolean isExpired(OffsetDateTime expiresAt, String status) {
        return expiresAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC)) || "EXPIRED".equals(status) || "CANCELLED".equals(status);
    }

    private OffsetDateTime normalizeFutureStartsAt(OffsetDateTime startsAt) {
        if (startsAt == null || !startsAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw validationError("startsAt", "La nueva fecha debe ser futura.");
        }
        return startsAt;
    }

    private int resolveExpirationMinutes(Integer expirationMinutes) {
        int resolved = expirationMinutes == null ? 720 : expirationMinutes;
        if (resolved < 5 || resolved > 1440) {
            throw validationError("expirationMinutes", "La expiracion debe estar entre 5 y 1440 minutos.");
        }
        return resolved;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validationError("reason", "El texto supera el largo maximo permitido.");
        }
        return normalized;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildPublicUrl(String baseUrl, String token) {
        return baseUrl.endsWith("/") ? baseUrl + token : baseUrl + "/" + token;
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank() || token.length() > 256) {
            throw validationError("token", "El token publico no es valido.");
        }
        return token.trim();
    }

    private String normalizeStatusForApi(String status) {
        return switch (status) {
            case "RESCHEDULED" -> "REPROGRAMADA";
            case "CANCELLED" -> "CANCELADA";
            case "CONFIRMED" -> "CONFIRMADA";
            case "REQUESTED" -> "SOLICITADA";
            case "COMPLETED", "ATTENDED" -> "ATENDIDA";
            case "NO_SHOW" -> "NO_ASISTE";
            case "EXPIRED", "RELEASED" -> "EXPIRADA";
            default -> status;
        };
    }

    private String maskPhone(String phone) {
        return phone == null || phone.length() <= 4 ? "****" : "****" + phone.substring(phone.length() - 4);
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "Sin fecha" : value.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "sin detalle" : exception.getMessage();
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BOOKING_PUBLIC_ACTION_VALIDATION_ERROR",
                "La solicitud contiene datos invalidos.", Map.of(field, message));
    }
}
