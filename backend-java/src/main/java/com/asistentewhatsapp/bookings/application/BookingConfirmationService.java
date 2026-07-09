package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.api.AgendaAvailabilityRequest;
import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.agenda.api.AgendaSlotResponse;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.LocationRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ProfessionalRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.RoomRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ServiceRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.TimeWindowRecord;
import com.asistentewhatsapp.aiagents.application.AiTraceLogger;
import com.asistentewhatsapp.aiagents.application.WhatsAppMessageFormatter;
import com.asistentewhatsapp.bookings.api.BookingConfirmationLinkResponse;
import com.asistentewhatsapp.bookings.api.CreateBookingConfirmationLinkRequest;
import com.asistentewhatsapp.bookings.api.PublicBookingCancellationRequest;
import com.asistentewhatsapp.bookings.api.PublicBookingConfirmationResponse;
import com.asistentewhatsapp.bookings.api.PublicBookingConfirmationRescheduleRequest;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationBookingRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.ConfirmationLinkRecord;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebChannelJdbcRepository;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.security.application.TokenHashService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BookingConfirmationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingConfirmationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String STATUS_PENDING_CONFIRMATION = BookingStateMachine.PENDING_CONFIRMATION;
    private static final String STATUS_CONFIRMED = BookingStateMachine.CONFIRMED;
    private static final String STATUS_EXPIRED = BookingStateMachine.EXPIRED;
    private static final String LINK_STATUS_CONFIRMED = "CONFIRMED";
    private static final int SLOT_STEP_MINUTES = 15;

    private final BookingConfirmationJdbcRepository repository;
    private final BookingConfirmationProperties properties;
    private final TokenHashService tokenHashService;
    private final AuditService auditService;
    private final ChannelDispatchService channelDispatchService;
    private final CompleteAgendaJdbcRepository completeAgendaJdbcRepository;
    private final WhatsAppWebChannelJdbcRepository whatsAppWebChannelJdbcRepository;
    private final BookingEmailService bookingEmailService;
    private final BookingPaymentService bookingPaymentService;
    private final CalendarSyncService calendarSyncService;
    private final BookingConfirmationNotificationsService notificationsService;
    private final TransactionTemplate transactionTemplate;

    public BookingConfirmationService(
            BookingConfirmationJdbcRepository repository,
            BookingConfirmationProperties properties,
            TokenHashService tokenHashService,
            AuditService auditService,
            ChannelDispatchService channelDispatchService,
            CompleteAgendaJdbcRepository completeAgendaJdbcRepository,
            WhatsAppWebChannelJdbcRepository whatsAppWebChannelJdbcRepository,
            BookingEmailService bookingEmailService,
            BookingPaymentService bookingPaymentService,
            CalendarSyncService calendarSyncService,
            BookingConfirmationNotificationsService notificationsService,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.properties = properties;
        this.tokenHashService = tokenHashService;
        this.auditService = auditService;
        this.channelDispatchService = channelDispatchService;
        this.completeAgendaJdbcRepository = completeAgendaJdbcRepository;
        this.whatsAppWebChannelJdbcRepository = whatsAppWebChannelJdbcRepository;
        this.bookingEmailService = bookingEmailService;
        this.bookingPaymentService = bookingPaymentService;
        this.calendarSyncService = calendarSyncService;
        this.notificationsService = notificationsService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public BookingConfirmationLinkResponse createConfirmationLink(AuthenticatedUser user, UUID bookingId,
            CreateBookingConfirmationLinkRequest request) {
        ConfirmationBookingRecord booking = repository.findBooking(user.businessId(), bookingId);
        validateBookingCanReceiveConfirmation(booking);
        ensureAvailability(booking);

        int expirationMinutes = request != null && request.expirationMinutes() != null
                ? request.expirationMinutes()
                : properties.getExpirationMinutes();
        boolean sendWhatsApp = request != null && request.sendWhatsApp() != null
                ? request.sendWhatsApp()
                : properties.isDispatchWhatsApp();

        repository.invalidateActiveLinks(user.businessId(), bookingId);
        String token = generateToken();
        String confirmationUrl = buildConfirmationUrl(token);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expirationMinutes);
        UUID linkId = repository.insertLink(user.businessId(), bookingId, tokenHashService.sha256(token), confirmationUrl, expiresAt);
        String targetStatus = booking.requiresDeposit() ? BookingStateMachine.PENDING_PAYMENT : STATUS_PENDING_CONFIRMATION;
        BookingStateMachine.assertTransition(booking.bookingStatus(), targetStatus, "quedar pendiente de confirmacion");
        repository.updateBookingStatus(user.businessId(), bookingId, targetStatus);
        if (!targetStatus.equals(BookingStateMachine.canonical(booking.bookingStatus()))) {
            completeAgendaJdbcRepository.insertStatusHistory(user.businessId(), bookingId, booking.bookingStatus(), targetStatus,
                    "Reserva marcada como pendiente de confirmacion al generar enlace publico.", user.userId(), "WHATSAPP");
        }

        OffsetDateTime sentAt = null;
        if (sendWhatsApp) {
            sentAt = dispatchWhatsApp(booking, linkId, confirmationUrl, expiresAt, user.userId());
        }
        sendPendingConfirmationEmail(booking, confirmationUrl, expiresAt);

        auditService.record(user.businessId(), user.userId(), "BOOKING_CONFIRMATION_LINK_CREATED", "BOOKING", bookingId,
                "Se genero enlace de confirmacion para reserva temporal por WhatsApp.",
                AuditMetadata.of(
                        "linkId", linkId,
                        "previousStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "newStatus", targetStatus,
                        "requiresDeposit", booking.requiresDeposit(),
                        "depositAmount", booking.depositAmount(),
                        "paymentStatus", booking.paymentStatus(),
                        "linkExpiresAt", expiresAt,
                        "sentAt", sentAt,
                        "sendWhatsApp", sendWhatsApp));
        return new BookingConfirmationLinkResponse(linkId, bookingId, sentAt != null ? "SENT" : "GENERATED", confirmationUrl, expiresAt, sentAt);
    }

    @Transactional
    public PublicBookingConfirmationResponse preview(String rawToken) {
        String tokenHash = tokenHashService.sha256(normalizeToken(rawToken));
        ConfirmationLinkRecord link = repository.findByTokenHash(tokenHash);
        if (isExpired(link)) {
            expireAndRelease(link);
            return toPublicResponse(repository.findByTokenHash(tokenHash));
        }
        repository.markOpened(link.linkId());
        return toPublicResponse(repository.findByTokenHash(tokenHash));
    }

    public PublicBookingConfirmationResponse confirm(String rawToken) {
        String tokenHash = tokenHashService.sha256(normalizeToken(rawToken));
        ConfirmationLinkRecord link = repository.findByTokenHash(tokenHash);
        if (LINK_STATUS_CONFIRMED.equals(link.linkStatus())
                || BookingStateMachine.CONFIRMED.equals(BookingStateMachine.canonical(link.bookingStatus()))) {
            return toPublicResponse(link);
        }
        if (isExpired(link) || BookingStateMachine.EXPIRED.equals(BookingStateMachine.canonical(link.bookingStatus()))) {
            expireAndRelease(link);
            return toPublicResponse(repository.findByTokenHash(tokenHash));
        }
        validateBookingStillConfirmable(link);
        ensurePaymentAllowsConfirmation(link);
        BookingStateMachine.assertTransition(link.bookingStatus(), STATUS_CONFIRMED, "confirmarse");
        ensureAvailability(link);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                ConfirmationLinkRecord locked = repository.findByTokenHashForUpdate(tokenHash);
                repository.markConfirmed(locked.linkId());
                repository.updateBookingStatus(locked.businessId(), locked.bookingId(), STATUS_CONFIRMED);
                completeAgendaJdbcRepository.insertStatusHistory(locked.businessId(), locked.bookingId(), locked.bookingStatus(), STATUS_CONFIRMED,
                        "Reserva confirmada desde enlace publico.", null, "PUBLIC_LINK");
            }
        });
        safelyRun(() -> notificationsService.scheduleReminders(link.businessId(), link.bookingId(), link.startsAt()),
                "REMINDER_SCHEDULING_FAILED", link.bookingId());
        if (properties.isDispatchWhatsApp()) {
            safelyRun(() -> notificationsService.sendConfirmationWhatsApp(link),
                    "CONFIRMED_WHATSAPP_DISPATCH_FAILED", link.bookingId());
        }
        safelyRun(() -> notificationsService.sendConfirmationEmail(link),
                "CONFIRMED_EMAIL_SEND_FAILED", link.bookingId());
        safelyRun(() -> notificationsService.auditRecord(
                link.businessId(), link.bookingId(), link.linkId(),
                link.bookingStatus(), link.linkStatus(),
                link.requiresDeposit(), link.depositAmount(), link.paymentStatus(), link.startsAt()),
                "CONFIRMED_AUDIT_FAILED", link.bookingId());
        safelyRun(() -> notificationsService.syncCalendar(link.bookingId(), link.businessId()),
                "CALENDAR_SYNC_CONFIRMED_FAILED", link.bookingId());
        return toPublicResponse(repository.findByTokenHash(tokenHash));
    }



    @Transactional(readOnly = true)
    public AgendaAvailabilityResponse publicAvailability(String rawToken, LocalDate date, Integer maxSlots) {
        ConfirmationLinkRecord link = repository.findByTokenHash(tokenHashService.sha256(normalizeToken(rawToken)));
        ensurePublicActionAllowed(link, "reprogramarse");
        if (date == null) {
            throw validationError("date", "La fecha es obligatoria.");
        }
        if (link.locationId() == null || link.serviceId() == null) {
            throw validationError("booking", "La reserva no tiene sucursal o servicio configurado para reprogramar.");
        }
        AgendaAvailabilityRequest request = new AgendaAvailabilityRequest(
                link.locationId(),
                link.serviceId(),
                null,
                null,
                date,
                null,
                maxSlots == null ? 12 : maxSlots);
        return buildPublicAvailability(link.businessId(), request);
    }

    @Transactional
    public PublicBookingConfirmationResponse rescheduleFromConfirmation(String rawToken, PublicBookingConfirmationRescheduleRequest request) {
        String tokenHash = tokenHashService.sha256(normalizeToken(rawToken));
        ConfirmationLinkRecord link = repository.findByTokenHashForUpdate(tokenHash);
        ensurePublicActionAllowed(link, "reprogramarse");
        if (request == null || request.startsAt() == null) {
            throw validationError("startsAt", "Selecciona una nueva fecha y hora.");
        }
        if (link.locationId() == null || link.serviceId() == null) {
            throw validationError("booking", "La reserva no tiene sucursal o servicio configurado para reprogramar.");
        }

        ServiceRecord service = completeAgendaJdbcRepository.findService(link.businessId(), link.locationId(), link.serviceId());
        UUID professionalId = request.professionalId() != null ? request.professionalId() : link.professionalId();
        UUID roomId = request.roomId() != null ? request.roomId() : link.roomId();
        if (professionalId == null) {
            throw validationError("professionalId", "Selecciona un profesional disponible.");
        }
        if (service.requiresRoom() && roomId == null) {
            throw validationError("roomId", "Selecciona una cabina disponible.");
        }

        OffsetDateTime startsAt = normalizeFutureStartsAt(request.startsAt());
        OffsetDateTime effectiveStart = startsAt.minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
        ensureAgendaSlotAvailable(link.businessId(), link.bookingId(), link.locationId(), professionalId, roomId, effectiveStart, endsAt);
        BookingStateMachine.assertTransition(link.bookingStatus(), BookingStateMachine.RESCHEDULED, "reprogramarse");
        String reason = request.reason() == null || request.reason().isBlank()
                ? "Reprogramacion solicitada por cliente desde enlace publico de confirmacion."
                : normalizeOptionalText(request.reason(), 1000);
        completeAgendaJdbcRepository.updateBookingSchedule(link.businessId(), link.bookingId(), null, link.locationId(), link.serviceId(),
                professionalId, roomId, startsAt, endsAt, service.durationMinutes(), reason);
        scheduleConfirmedBookingReminders(repository.findByTokenHash(tokenHash));
        auditService.record(link.businessId(), null, "BOOKING_RESCHEDULED_BY_CUSTOMER_LINK", "BOOKING", link.bookingId(),
                "El cliente reprogramo la reserva desde enlace publico de confirmacion.",
                AuditMetadata.of(
                        "source", "PUBLIC_LINK",
                        "linkId", link.linkId(),
                        "previousStatus", BookingStateMachine.canonical(link.bookingStatus()),
                        "newStatus", BookingStateMachine.RESCHEDULED,
                        "locationId", link.locationId(),
                        "serviceId", link.serviceId(),
                        "professionalId", professionalId,
                        "roomId", roomId,
                        "startsAt", startsAt,
                        "endsAt", endsAt,
                        "reason", reason));
        try { calendarSyncService.syncRescheduled(link.bookingId(), link.businessId()); }
        catch (Exception e) { LOGGER.warn("CALENDAR_SYNC_RESCHEDULED_FAILED bookingId={}", link.bookingId(), e); }
        return toPublicResponse(repository.findByTokenHash(tokenHash));
    }

    @Transactional
    public PublicBookingConfirmationResponse cancelFromConfirmation(String rawToken, PublicBookingCancellationRequest request) {
        String tokenHash = tokenHashService.sha256(normalizeToken(rawToken));
        ConfirmationLinkRecord link = repository.findByTokenHashForUpdate(tokenHash);
        ensurePublicActionAllowed(link, "cancelarse");
        String reason = request == null ? null : normalizeOptionalText(request.reason(), 1000);
        if (reason == null || reason.length() < 5) {
            throw validationError("reason", "Ingresa un motivo de cancelacion de al menos 5 caracteres.");
        }
        BookingStateMachine.assertTransition(link.bookingStatus(), BookingStateMachine.CANCELLED, "cancelarse");
        completeAgendaJdbcRepository.cancelBooking(link.businessId(), link.bookingId(), null, reason);
        auditService.record(link.businessId(), null, "BOOKING_CANCELLED_BY_CUSTOMER_LINK", "BOOKING", link.bookingId(),
                "El cliente cancelo la reserva desde enlace publico de confirmacion.",
                AuditMetadata.of(
                        "source", "PUBLIC_LINK",
                        "linkId", link.linkId(),
                        "previousStatus", BookingStateMachine.canonical(link.bookingStatus()),
                        "newStatus", BookingStateMachine.CANCELLED,
                        "reason", reason));
        try { calendarSyncService.syncCancelled(link.bookingId(), link.businessId()); }
        catch (Exception e) { LOGGER.warn("CALENDAR_SYNC_CANCELLED_FAILED bookingId={}", link.bookingId(), e); }
        return toPublicResponse(repository.findByTokenHash(tokenHash));
    }

    @Scheduled(fixedDelayString = "${app.booking-confirmation.expiration-scan-ms:60000}")
    @Transactional
    public void expireDueLinks() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        repository.expireDueLinks(now);
        repository.expireBookingsWithExpiredLinks(now);
    }

    private void validateBookingCanReceiveConfirmation(ConfirmationBookingRecord booking) {
        BookingStateMachine.assertCanReceiveConfirmationLink(booking.bookingStatus());
        if (booking.locationId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "BOOKING_LOCATION_REQUIRED",
                    "La reserva debe tener sucursal antes de enviar enlace de confirmacion.",
                    Map.of("locationId", "Selecciona una sucursal."));
        }
    }

    private void validateBookingStillConfirmable(ConfirmationLinkRecord link) {
        BookingStateMachine.assertTransition(link.bookingStatus(), STATUS_CONFIRMED, "confirmarse");
    }

    private void ensurePaymentAllowsConfirmation(ConfirmationLinkRecord link) {
        if (!link.requiresDeposit()) {
            return;
        }
        if (bookingPaymentService.hasApprovedRequiredDeposit(link.businessId(), link.bookingId())) {
            return;
        }
        throw new ApiException(HttpStatus.CONFLICT,
                "BOOKING_PAYMENT_REQUIRED",
                "La reserva requiere abono aprobado antes de confirmarse.",
                Map.of("paymentStatus", link.paymentStatus() == null ? "PENDING" : link.paymentStatus()));
    }

    private void ensureAvailability(ConfirmationBookingRecord booking) {
        if (repository.hasOverlappingActiveBooking(booking.businessId(), booking.bookingId(), booking.locationId(),
                booking.startsAt(), booking.startsAt().plusMinutes(booking.durationMinutes()))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "BOOKING_SLOT_NOT_AVAILABLE",
                    "El horario ya esta ocupado para esta sucursal.",
                    Map.of("startsAt", "Selecciona otro horario."));
        }
    }

    private void ensureAvailability(ConfirmationLinkRecord link) {
        if (repository.hasOverlappingActiveBooking(link.businessId(), link.bookingId(), link.locationId(),
                link.startsAt(), link.startsAt().plusMinutes(link.durationMinutes()))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "BOOKING_SLOT_NOT_AVAILABLE",
                    "El horario ya fue tomado por otra reserva.",
                    Map.of("startsAt", "Solicita un nuevo horario por WhatsApp."));
        }
    }

    private boolean isExpired(ConfirmationLinkRecord link) {
        return link.expiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))
                || "EXPIRED".equals(link.linkStatus())
                || "INVALIDATED".equals(link.linkStatus());
    }

    private void expireAndRelease(ConfirmationLinkRecord link) {
        if ("INVALIDATED".equals(link.linkStatus())) {
            return;
        }
        repository.markExpired(link.linkId());
        if (isPendingConfirmationStatus(link.bookingStatus())) {
            repository.updateBookingStatus(link.businessId(), link.bookingId(), STATUS_EXPIRED);
            completeAgendaJdbcRepository.insertStatusHistory(link.businessId(), link.bookingId(), link.bookingStatus(), STATUS_EXPIRED,
                    "Reserva expirada por vencimiento de enlace de confirmacion.", null, "PUBLIC_LINK");
            completeAgendaJdbcRepository.cancelPendingReminders(link.businessId(), link.bookingId());
            auditService.record(link.businessId(), null, "BOOKING_CONFIRMATION_LINK_EXPIRED", "BOOKING", link.bookingId(),
                    "El enlace expiro y el cupo fue liberado.",
                    AuditMetadata.of(
                            "source", "PUBLIC_LINK",
                            "linkId", link.linkId(),
                            "previousStatus", BookingStateMachine.canonical(link.bookingStatus()),
                            "newStatus", BookingStateMachine.EXPIRED,
                            "linkStatus", link.linkStatus(),
                            "linkExpiresAt", link.expiresAt()));
        }
    }



    private AgendaAvailabilityResponse buildPublicAvailability(UUID businessId, AgendaAvailabilityRequest request) {
        LocationRecord location = completeAgendaJdbcRepository.findLocation(businessId, request.locationId());
        ServiceRecord service = completeAgendaJdbcRepository.findService(businessId, request.locationId(), request.serviceId());
        int dayOfWeek = request.date().getDayOfWeek().getValue();
        if (completeAgendaJdbcRepository.isHoliday(businessId, request.locationId(), request.date())) {
            return emptyAvailability(location, service, request.date());
        }
        List<TimeWindowRecord> businessHours = completeAgendaJdbcRepository.findBusinessHours(businessId, request.locationId(), dayOfWeek);
        if (businessHours.isEmpty()) {
            return emptyAvailability(location, service, request.date());
        }
        List<ProfessionalRecord> professionals = completeAgendaJdbcRepository.findProfessionalCandidates(
                businessId, request.locationId(), request.serviceId(), request.professionalId());
        if (professionals.isEmpty()) {
            return emptyAvailability(location, service, request.date());
        }
        List<RoomRecord> rooms = service.requiresRoom()
                ? completeAgendaJdbcRepository.findRoomCandidates(businessId, request.locationId(), request.serviceId(), request.roomId())
                : List.of(new RoomRecord(null, null));
        if (rooms.isEmpty()) {
            return emptyAvailability(location, service, request.date());
        }
        int limit = normalizeLimit(request.maxSlots());
        List<AgendaSlotResponse> slots = new ArrayList<>();
        for (ProfessionalRecord professional : professionals) {
            List<TimeWindowRecord> professionalHours = completeAgendaJdbcRepository.findProfessionalHours(
                    businessId, request.locationId(), professional.id(), dayOfWeek);
            if (professionalHours.isEmpty()) {
                continue;
            }
            for (RoomRecord room : rooms) {
                collectPublicSlots(businessId, location, service, professional, room, request, businessHours, professionalHours, slots, limit);
                if (slots.size() >= limit) {
                    break;
                }
            }
            if (slots.size() >= limit) {
                break;
            }
        }
        slots.sort(Comparator.comparing(AgendaSlotResponse::startsAt));
        if (slots.size() > limit) {
            slots = slots.subList(0, limit);
        }
        return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), request.date(),
                service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), slots);
    }

    private AgendaAvailabilityResponse emptyAvailability(LocationRecord location, ServiceRecord service, LocalDate date) {
        return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), date,
                service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), List.of());
    }

    private void collectPublicSlots(UUID businessId, LocationRecord location, ServiceRecord service, ProfessionalRecord professional, RoomRecord room,
            AgendaAvailabilityRequest request, List<TimeWindowRecord> businessHours, List<TimeWindowRecord> professionalHours,
            List<AgendaSlotResponse> slots, int limit) {
        ZoneId locationZone = resolveLocationZone(location);
        OffsetDateTime nowAtLocation = OffsetDateTime.now(locationZone);
        for (TimeWindowRecord businessWindow : businessHours) {
            for (TimeWindowRecord professionalWindow : professionalHours) {
                LocalTime start = max(businessWindow.startTime(), professionalWindow.startTime());
                LocalTime end = min(businessWindow.endTime(), professionalWindow.endTime());
                if (!end.isAfter(start)) {
                    continue;
                }
                LocalTime cursor = start;
                while (!cursor.plusMinutes(service.durationMinutes()).isAfter(end) && slots.size() < limit) {
                    OffsetDateTime startsAt = request.date().atTime(cursor).atZone(locationZone).toOffsetDateTime();
                    OffsetDateTime effectiveStart = startsAt.minusMinutes(service.preparationMinutes());
                    OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
                    boolean available = !completeAgendaJdbcRepository.hasConflict(businessId, null, location.id(), professional.id(), room.id(), effectiveStart, endsAt)
                            && !completeAgendaJdbcRepository.hasBlock(businessId, location.id(), professional.id(), room.id(), effectiveStart, endsAt)
                            && startsAt.isAfter(nowAtLocation.plusMinutes(properties.getMinMinutesAhead()));
                    if (available) {
                        slots.add(new AgendaSlotResponse(startsAt, startsAt.plusMinutes(service.durationMinutes()), location.id(), location.name(),
                                service.id(), service.name(), service.durationMinutes(), professional.id(), professional.name(), room.id(), room.name(),
                                true, "Disponible"));
                    }
                    cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
                }
            }
        }
    }

    private void ensurePublicActionAllowed(ConfirmationLinkRecord link, String action) {
        if (isExpired(link)) {
            throw new ApiException(HttpStatus.CONFLICT, "PUBLIC_LINK_EXPIRED",
                    "El enlace publico ya expiro y la reserva no puede " + action + ".", Map.of("expiresAt", link.expiresAt().toString()));
        }
        validateBookingCanChange(link.bookingStatus(), action);
        if (!link.startsAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(properties.getMinMinutesAhead()))) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_CHANGE_WINDOW_CLOSED",
                    "La reserva no puede " + action + " con menos de " + properties.getMinMinutesAhead() + " minutos de anticipacion.", Map.of("startsAt", link.startsAt().toString()));
        }
    }

    private void validateBookingCanChange(String status, String action) {
        BookingStateMachine.assertCanChange(status, action);
    }

    private void ensureAgendaSlotAvailable(UUID businessId, UUID bookingId, UUID locationId, UUID professionalId, UUID roomId,
            OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (completeAgendaJdbcRepository.hasConflict(businessId, bookingId, locationId, professionalId, roomId, startsAt, endsAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "AGENDA_SLOT_NOT_AVAILABLE", "El horario ya esta ocupado.",
                    Map.of("startsAt", "Selecciona otro horario disponible."));
        }
        if (completeAgendaJdbcRepository.hasBlock(businessId, locationId, professionalId, roomId, startsAt, endsAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "AGENDA_SLOT_BLOCKED", "El horario esta bloqueado.",
                    Map.of("startsAt", "Selecciona otro horario disponible."));
        }
    }

    private OffsetDateTime normalizeFutureStartsAt(OffsetDateTime startsAt) {
        if (startsAt == null || !startsAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(properties.getMinMinutesAhead()))) {
            throw validationError("startsAt", "La nueva fecha debe tener al menos " + properties.getMinMinutesAhead() + " minutos de anticipacion.");
        }
        return startsAt;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private int normalizeLimit(Integer maxSlots) {
        if (maxSlots == null) {
            return 12;
        }
        return Math.min(Math.max(maxSlots, 1), 40);
    }

    private ZoneId resolveLocationZone(LocationRecord location) {
        String timezone = location.timezone();
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("America/Santiago");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            return ZoneId.of("America/Santiago");
        }
    }

    private LocalTime max(LocalTime first, LocalTime second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalTime min(LocalTime first, LocalTime second) {
        return first.isBefore(second) ? first : second;
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, Map.of(field, message));
    }

    private boolean isPendingConfirmationStatus(String status) {
        String canonical = BookingStateMachine.canonical(status);
        return BookingStateMachine.PENDING_CONFIRMATION.equals(canonical)
                || BookingStateMachine.REQUESTED.equals(canonical)
                || BookingStateMachine.PENDING_PAYMENT.equals(canonical);
    }

    private PublicBookingConfirmationResponse toPublicResponse(ConfirmationLinkRecord link) {
        return new PublicBookingConfirmationResponse(
                link.bookingId(),
                link.bookingStatus(),
                link.linkStatus(),
                link.subject(),
                link.serviceName(),
                link.professionalName(),
                link.roomName(),
                link.startsAt(),
                link.durationMinutes(),
                link.locationId(),
                link.location(),
                link.locationName(),
                link.customerName(),
                maskPhone(link.customerPhone()),
                link.requiresDeposit(),
                link.depositAmount(),
                link.paymentStatus(),
                link.expiresAt(),
                link.confirmedAt(),
                properties.getMinMinutesAhead());
    }

    private void scheduleConfirmedBookingReminders(ConfirmationLinkRecord link) {
        completeAgendaJdbcRepository.cancelPendingReminders(link.businessId(), link.bookingId());
        completeAgendaJdbcRepository.insertReminder(link.businessId(), link.bookingId(), "TWENTY_FOUR_HOURS_BEFORE", "WHATSAPP", link.startsAt().minusHours(24));
        completeAgendaJdbcRepository.insertReminder(link.businessId(), link.bookingId(), "TWENTY_FOUR_HOURS_BEFORE", "EMAIL", link.startsAt().minusHours(24));
        completeAgendaJdbcRepository.insertReminder(link.businessId(), link.bookingId(), "TWO_HOURS_BEFORE", "WHATSAPP", link.startsAt().minusHours(2));
        completeAgendaJdbcRepository.insertReminder(link.businessId(), link.bookingId(), "TWO_HOURS_BEFORE", "EMAIL", link.startsAt().minusHours(2));
        auditService.record(link.businessId(), null, "BOOKING_REMINDER_SCHEDULED", "BOOKING", link.bookingId(),
                "Recordatorios automaticos WhatsApp y correo programados.");
    }

    private void sendPendingConfirmationEmail(ConfirmationBookingRecord booking, String confirmationUrl, OffsetDateTime expiresAt) {
        String body = bookingEmailService.buildAppointmentEmailBody(
                booking.customerName(),
                "Tu reserva quedo pendiente de confirmacion. Usa el boton o enlace para mantener el cupo.",
                booking.serviceName() == null ? booking.subject() : booking.serviceName(),
                booking.startsAt().toLocalDate() + " " + booking.startsAt().toLocalTime(),
                booking.locationName() == null ? booking.location() : booking.locationName(),
                booking.professionalName(),
                booking.roomName(),
                confirmationUrl,
                "El enlace vence el " + expiresAt.toLocalDate() + " a las " + expiresAt.toLocalTime() + ".");
        bookingEmailService.sendBookingEmail(booking.businessId(), booking.bookingId(), booking.customerEmail(),
                "BOOKING_CONFIRMATION_PENDING", "Confirma tu reserva", body);
        auditService.record(booking.businessId(), null, "BOOKING_CONFIRMATION_EMAIL_SENT", "BOOKING", booking.bookingId(),
                "Correo de confirmacion pendiente generado o simulado.");
    }

    private void sendConfirmedEmail(ConfirmationLinkRecord link) {
        String body = bookingEmailService.buildAppointmentEmailBody(
                link.customerName(),
                "Tu reserva fue confirmada correctamente.",
                link.serviceName() == null ? link.subject() : link.serviceName(),
                link.startsAt().toLocalDate() + " " + link.startsAt().toLocalTime(),
                link.locationName() == null ? link.location() : link.locationName(),
                link.professionalName(),
                link.roomName(),
                link.confirmationUrl(),
                "Te esperamos en la fecha y hora indicada.");
        bookingEmailService.sendBookingEmail(link.businessId(), link.bookingId(), link.customerEmail(),
                "BOOKING_CONFIRMED", "Tu reserva esta confirmada", body);
    }

    private OffsetDateTime dispatchWhatsApp(ConfirmationBookingRecord booking, UUID linkId, String confirmationUrl, OffsetDateTime expiresAt,
            UUID actorUserId) {
        String body = buildWhatsAppConfirmationMessage(booking, confirmationUrl, expiresAt);
        UUID messageId = null;
        OffsetDateTime attemptAt = OffsetDateTime.now(ZoneOffset.UTC);
        auditService.record(booking.businessId(), actorUserId, "BOOKING_CONFIRMATION_WHATSAPP_SEND_ATTEMPT", "BOOKING", booking.bookingId(),
                "Intento de envio de enlace de confirmacion por WhatsApp.");
        if (booking.conversationId() != null) {
            messageId = whatsAppWebChannelJdbcRepository.insertOutboundMessage(
                    booking.businessId(), booking.conversationId(), actorUserId, body, attemptAt);
        }
        try {
            ChannelDispatchResponse response = channelDispatchService.dispatch(
                    new ChannelDispatchRequest(booking.businessId(), MessageChannelType.WHATSAPP, booking.customerPhone(), body));
            OffsetDateTime sentAt = OffsetDateTime.now(ZoneOffset.UTC);
            repository.markSent(linkId, sentAt);
            if (messageId != null) {
                whatsAppWebChannelJdbcRepository.updateOutboundMessageAccepted(messageId, response.externalMessageId(), "SENT", sentAt);
                whatsAppWebChannelJdbcRepository.insertMessageDeliveryLog(
                        booking.businessId(), messageId, "SENT", response.externalMessageId(),
                        Map.of("status", response.status() == null ? "SENT" : response.status(), "confirmationUrl", confirmationUrl), sentAt);
            }
            auditService.record(booking.businessId(), actorUserId, "BOOKING_CONFIRMATION_WHATSAPP_SENT", "BOOKING", booking.bookingId(),
                    "Enlace de confirmacion enviado por WhatsApp.");
            return sentAt;
        } catch (RuntimeException exception) {
            OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC);
            if (messageId != null) {
                whatsAppWebChannelJdbcRepository.updateOutboundMessageFailed(messageId, "WHATSAPP_CONFIRMATION_LINK_SEND_FAILED", failedAt);
                whatsAppWebChannelJdbcRepository.insertMessageDeliveryLog(
                        booking.businessId(), messageId, "FAILED", null,
                        Map.of("error", exception.getMessage() == null ? "Error de envio" : exception.getMessage()), failedAt);
            }
            auditService.record(booking.businessId(), actorUserId, "BOOKING_CONFIRMATION_WHATSAPP_SEND_FAILED", "BOOKING", booking.bookingId(),
                    "Fallo el envio del enlace de confirmacion por WhatsApp: "
                            + (exception.getMessage() == null ? "sin detalle" : exception.getMessage()));
            return null;
        }
    }

    private String buildWhatsAppConfirmationMessage(ConfirmationBookingRecord booking, String confirmationUrl, OffsetDateTime expiresAt) {
        String locationName = booking.locationName() != null ? booking.locationName() : booking.location();
        String serviceName = booking.serviceName() != null ? booking.serviceName() : booking.subject();
        int minutes = Math.max(1, properties.getExpirationMinutes());
        String body = WhatsAppMessageFormatter.temporaryBookingCreated(
                serviceName,
                locationName == null ? "por confirmar" : locationName,
                String.valueOf(booking.startsAt().toLocalDate()),
                booking.startsAt().toLocalTime().toString(),
                confirmationUrl,
                minutes);
        AiTraceLogger.info("WHATSAPP_MESSAGE_FORMATTED", null, null, booking.bookingId(), "BookingConfirmationService",
                "type=TEMPORARY_BOOKING containsLink=" + body.contains("/reservas/confirmar/")
                        + " messageLength=" + body.length());
        return body;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildConfirmationUrl(String token) {
        String baseUrl = properties.getPublicBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl + token : baseUrl + "/" + token;
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank() || token.length() > 256) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "INVALID_CONFIRMATION_TOKEN",
                    "El token de confirmacion no es valido.",
                    Map.of("token", "Token ausente o invalido."));
        }
        return token.trim();
    }

    private void safelyRun(Runnable task, String warnCode, UUID bookingId) {
        try {
            task.run();
        } catch (Exception e) {
            LOGGER.warn("{} bookingId={}", warnCode, bookingId, e);
        }
    }

    private String maskPhone(String phone) {
        return phone == null || phone.length() <= 4 ? "****" : "****" + phone.substring(phone.length() - 4);
    }
}
