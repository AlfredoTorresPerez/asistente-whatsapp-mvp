package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.api.BookingDetailResponse;
import com.asistentewhatsapp.bookings.api.BookingSummaryResponse;
import com.asistentewhatsapp.bookings.api.CancelBookingRequest;
import com.asistentewhatsapp.bookings.api.CreateBookingFromConversationRequest;
import com.asistentewhatsapp.bookings.api.CreateBookingFromLeadRequest;
import com.asistentewhatsapp.bookings.api.CreateBookingRequest;
import com.asistentewhatsapp.bookings.api.RescheduleBookingRequest;
import com.asistentewhatsapp.bookings.api.UpdateBookingRequest;
import com.asistentewhatsapp.bookings.infrastructure.BookingJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingService.class);

    private final BookingJdbcRepository bookingJdbcRepository;
    private final BusinessLocationJdbcRepository businessLocationJdbcRepository;
    private final BookingConfirmationJdbcRepository bookingConfirmationJdbcRepository;
    private final CompleteAgendaJdbcRepository agendaRepository;
    private final AvailabilityService availabilityService;
    private final AuditService auditService;

    public BookingService(
            BookingJdbcRepository bookingJdbcRepository,
            BusinessLocationJdbcRepository businessLocationJdbcRepository,
            BookingConfirmationJdbcRepository bookingConfirmationJdbcRepository,
            CompleteAgendaJdbcRepository agendaRepository,
            AvailabilityService availabilityService,
            AuditService auditService) {
        this.bookingJdbcRepository = bookingJdbcRepository;
        this.businessLocationJdbcRepository = businessLocationJdbcRepository;
        this.bookingConfirmationJdbcRepository = bookingConfirmationJdbcRepository;
        this.agendaRepository = agendaRepository;
        this.availabilityService = availabilityService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingSummaryResponse> list(
            AuthenticatedUser authenticatedUser,
            int page,
            int size,
            OffsetDateTime from,
            OffsetDateTime to,
            String search,
            String status,
            UUID responsibleUserId) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), 500);
        DateRange dateRange = resolveDateRange(from, to);

        return bookingJdbcRepository.findBookings(
                authenticatedUser.businessId(),
                resolvedPage,
                resolvedSize,
                dateRange.from(),
                dateRange.to(),
                normalizeSearch(search),
                normalizeOptionalStatus(status),
                resolveResponsibleUserId(authenticatedUser, responsibleUserId));
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getDetail(AuthenticatedUser authenticatedUser, UUID bookingId) {
        return bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
    }

    @Transactional
    public BookingDetailResponse create(AuthenticatedUser authenticatedUser, CreateBookingRequest request) {
        String subject = normalizeRequiredValue(request.subject(), "subject", 160);
        String status = normalizeStatus(request.status(), "PENDIENTE_CONFIRMACION");
        BookingStateMachine.assertTransition(null, status, "crearse");
        OffsetDateTime startsAt = normalizeStartsAt(request.startsAt());
        int durationMinutes = normalizeDuration(request.durationMinutes());
        LocationResolution location = resolveLocation(
                authenticatedUser.businessId(),
                request.locationId(),
                normalizeOptionalText(request.location(), "location", 160),
                null,
                true);
        String notes = normalizeOptionalText(request.notes(), "notes", 2000);
        UUID assignedUserId = resolveResponsibleUserId(authenticatedUser, request.assignedUserId());

        BookingJdbcRepository.CustomerRecord customer = resolveCustomer(authenticatedUser, request);
        UUID professionalId = assignedUserId;
        UUID customerId = customer.id();
        ZoneId zone = resolveZoneId(authenticatedUser.businessId(), location.locationId());
        availabilityService.checkMinAdvanceNotice(authenticatedUser.businessId(), location.locationId(), professionalId, startsAt, zone);
        if (location.locationId() != null && professionalId != null) {
            OffsetDateTime endsAt = startsAt.plusMinutes(durationMinutes);
            availabilityService.checkProfessionalAbsence(authenticatedUser.businessId(), professionalId, startsAt, endsAt);
            availabilityService.checkProfessionalDailyCapacity(authenticatedUser.businessId(), professionalId, startsAt);
        }
        if (customerId != null) {
            OffsetDateTime endsAt = startsAt.plusMinutes(durationMinutes);
            availabilityService.checkCustomerDuplicateActiveBooking(authenticatedUser.businessId(), customerId, professionalId, startsAt, endsAt, null);
        }
        ensureSlotAvailable(authenticatedUser.businessId(), null, location.locationId(), startsAt, durationMinutes);
        UUID bookingId = bookingJdbcRepository.insertBooking(
                authenticatedUser.businessId(),
                customer.id(),
                null,
                null,
                assignedUserId,
                subject,
                status,
                startsAt,
                durationMinutes,
                location.locationId(),
                location.locationText(),
                notes,
                resolveCompletedAt(status, startsAt));
        auditService.record(authenticatedUser.businessId(), authenticatedUser.userId(),
                "BOOKING_CREATED", "BOOKING", bookingId,
                "Reserva creada manualmente.",
                AuditMetadata.of(
                        "status", status,
                        "customerId", customer.id(),
                        "customerPhone", customer.phone(),
                        "subject", subject,
                        "startsAt", startsAt,
                        "durationMinutes", durationMinutes,
                        "locationId", location.locationId(),
                        "assignedUserId", assignedUserId));
        return bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
    }

    @Transactional
    public BookingDetailResponse createFromConversation(
            AuthenticatedUser authenticatedUser,
            UUID conversationId,
            CreateBookingFromConversationRequest request) {
        BookingJdbcRepository.ConversationContextRecord conversation =
                bookingJdbcRepository.findConversationContext(authenticatedUser.businessId(), conversationId);
        UUID leadId = request.leadId() != null
                ? ensureLeadBelongsToBusiness(authenticatedUser, request.leadId()).id()
                : bookingJdbcRepository.findLeadIdByConversation(authenticatedUser.businessId(), conversationId).orElse(null);
        UUID assignedUserId = resolveResponsibleUserId(
                authenticatedUser,
                request.assignedUserId() != null ? request.assignedUserId() : conversation.assignedUserId());
        String status = normalizeStatus(request.status(), "PENDIENTE_CONFIRMACION");
        BookingStateMachine.assertTransition(null, status, "crearse");
        OffsetDateTime startsAt = normalizeStartsAt(request.startsAt());
        LocationResolution location = resolveLocation(
                authenticatedUser.businessId(),
                request.locationId(),
                normalizeOptionalText(request.location(), "location", 160),
                conversation.locationId(),
                true);

        int durationMinutes = normalizeDuration(request.durationMinutes());
        UUID professionalId = assignedUserId;
        UUID customerId = conversation.customerId();
        ZoneId zone = resolveZoneId(authenticatedUser.businessId(), location.locationId());
        availabilityService.checkMinAdvanceNotice(authenticatedUser.businessId(), location.locationId(), professionalId, startsAt, zone);
        if (location.locationId() != null && professionalId != null) {
            OffsetDateTime endsAt = startsAt.plusMinutes(durationMinutes);
            availabilityService.checkProfessionalAbsence(authenticatedUser.businessId(), professionalId, startsAt, endsAt);
            availabilityService.checkProfessionalDailyCapacity(authenticatedUser.businessId(), professionalId, startsAt);
        }
        if (customerId != null) {
            OffsetDateTime endsAt = startsAt.plusMinutes(durationMinutes);
            availabilityService.checkCustomerDuplicateActiveBooking(authenticatedUser.businessId(), customerId, professionalId, startsAt, endsAt, null);
        }
        ensureSlotAvailable(authenticatedUser.businessId(), null, location.locationId(), startsAt, durationMinutes);

        UUID bookingId = bookingJdbcRepository.insertBooking(
                authenticatedUser.businessId(),
                conversation.customerId(),
                leadId,
                conversationId,
                assignedUserId,
                normalizeRequiredValue(request.subject(), "subject", 160),
                status,
                startsAt,
                durationMinutes,
                location.locationId(),
                location.locationText(),
                normalizeOptionalText(request.notes(), "notes", 2000),
                resolveCompletedAt(status, startsAt));
        auditService.record(authenticatedUser.businessId(), authenticatedUser.userId(),
                "BOOKING_CREATED_FROM_CONVERSATION", "BOOKING", bookingId,
                "Reserva creada desde conversacion.",
                AuditMetadata.of(
                        "status", status,
                        "conversationId", conversationId,
                        "leadId", leadId,
                        "subject", request.subject(),
                        "startsAt", startsAt,
                        "durationMinutes", durationMinutes,
                        "locationId", location.locationId(),
                        "assignedUserId", assignedUserId));
        return bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
    }

    @Transactional
    public BookingDetailResponse createFromLead(
            AuthenticatedUser authenticatedUser,
            UUID leadId,
            CreateBookingFromLeadRequest request) {
        BookingJdbcRepository.LeadContextRecord lead =
                bookingJdbcRepository.findLeadContext(authenticatedUser.businessId(), leadId);
        UUID assignedUserId = resolveResponsibleUserId(
                authenticatedUser,
                request.assignedUserId() != null ? request.assignedUserId() : lead.assignedUserId());
        String status = normalizeStatus(request.status(), "PENDIENTE_CONFIRMACION");
        BookingStateMachine.assertTransition(null, status, "crearse");
        OffsetDateTime startsAt = normalizeStartsAt(request.startsAt());
        LocationResolution location = resolveLocation(
                authenticatedUser.businessId(),
                request.locationId(),
                normalizeOptionalText(request.location(), "location", 160),
                null,
                true);

        int durationMinutes = normalizeDuration(request.durationMinutes());
        UUID professionalId = assignedUserId;
        UUID customerId = lead.customerId();
        ZoneId zone = resolveZoneId(authenticatedUser.businessId(), location.locationId());
        availabilityService.checkMinAdvanceNotice(authenticatedUser.businessId(), location.locationId(), professionalId, startsAt, zone);
        if (location.locationId() != null && professionalId != null) {
            OffsetDateTime endsAt = startsAt.plusMinutes(durationMinutes);
            availabilityService.checkProfessionalAbsence(authenticatedUser.businessId(), professionalId, startsAt, endsAt);
            availabilityService.checkProfessionalDailyCapacity(authenticatedUser.businessId(), professionalId, startsAt);
        }
        if (customerId != null) {
            OffsetDateTime endsAt = startsAt.plusMinutes(durationMinutes);
            availabilityService.checkCustomerDuplicateActiveBooking(authenticatedUser.businessId(), customerId, professionalId, startsAt, endsAt, null);
        }
        ensureSlotAvailable(authenticatedUser.businessId(), null, location.locationId(), startsAt, durationMinutes);

        UUID bookingId = bookingJdbcRepository.insertBooking(
                authenticatedUser.businessId(),
                lead.customerId(),
                leadId,
                lead.conversationId(),
                assignedUserId,
                normalizeRequiredValue(request.subject(), "subject", 160),
                status,
                startsAt,
                durationMinutes,
                location.locationId(),
                location.locationText(),
                normalizeOptionalText(request.notes(), "notes", 2000),
                resolveCompletedAt(status, startsAt));
        auditService.record(authenticatedUser.businessId(), authenticatedUser.userId(),
                "BOOKING_CREATED_FROM_LEAD", "BOOKING", bookingId,
                "Reserva creada desde prospecto.",
                AuditMetadata.of(
                        "status", status,
                        "leadId", leadId,
                        "customerId", lead.customerId(),
                        "subject", request.subject(),
                        "startsAt", startsAt,
                        "durationMinutes", durationMinutes,
                        "locationId", location.locationId(),
                        "assignedUserId", assignedUserId));
        return bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
    }

    @Transactional
    public BookingDetailResponse update(
            AuthenticatedUser authenticatedUser,
            UUID bookingId,
            UpdateBookingRequest request) {
        BookingDetailResponse current = bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
        String status = normalizeStatus(request.status(), current.status());
        BookingStateMachine.assertTransition(current.status(), status, "actualizarse");
        OffsetDateTime startsAt = normalizeStartsAt(request.startsAt());
        LocationResolution location = resolveLocation(
                authenticatedUser.businessId(),
                request.locationId(),
                normalizeOptionalText(request.location(), "location", 160),
                current.locationId(),
                true);

        ensureSlotAvailable(authenticatedUser.businessId(), bookingId, location.locationId(), startsAt, normalizeDuration(request.durationMinutes()));

        bookingJdbcRepository.updateBooking(
                authenticatedUser.businessId(),
                bookingId,
                resolveResponsibleUserId(authenticatedUser, request.assignedUserId()),
                normalizeRequiredValue(request.subject(), "subject", 160),
                status,
                startsAt,
                normalizeDuration(request.durationMinutes()),
                location.locationId(),
                location.locationText(),
                normalizeOptionalText(request.notes(), "notes", 2000),
                resolveCompletedAt(status, startsAt));
        auditService.record(authenticatedUser.businessId(), authenticatedUser.userId(),
                "BOOKING_UPDATED", "BOOKING", bookingId,
                "Reserva actualizada.",
                AuditMetadata.of(
                        "previousStatus", current.status(),
                        "newStatus", status,
                        "subject", request.subject(),
                        "startsAt", startsAt,
                        "durationMinutes", request.durationMinutes(),
                        "locationId", location.locationId()));
        return bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
    }

    @Transactional
    public BookingDetailResponse reschedule(
            AuthenticatedUser authenticatedUser,
            UUID bookingId,
            RescheduleBookingRequest request) {
        BookingDetailResponse current = bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
        BookingStateMachine.assertTransition(current.status(), BookingStateMachine.RESCHEDULED, "reprogramarse");
        LocationResolution location = resolveLocation(
                authenticatedUser.businessId(),
                request.locationId(),
                normalizeOptionalText(request.location() != null ? request.location() : current.location(), "location", 160),
                current.locationId(),
                true);
        OffsetDateTime startsAt = normalizeStartsAt(request.startsAt());
        int durationMinutes = normalizeDuration(request.durationMinutes() != null ? request.durationMinutes() : current.durationMinutes());
        ensureSlotAvailable(authenticatedUser.businessId(), bookingId, location.locationId(), startsAt, durationMinutes);

        bookingJdbcRepository.rescheduleBooking(
                authenticatedUser.businessId(),
                bookingId,
                startsAt,
                durationMinutes,
                location.locationId(),
                location.locationText(),
                normalizeOptionalText(
                        request.notes() != null ? request.notes() : current.notes(),
                        "notes",
                        2000));
        auditService.record(authenticatedUser.businessId(), authenticatedUser.userId(),
                "BOOKING_RESCHEDULED", "BOOKING", bookingId,
                "Reserva reprogramada.",
                AuditMetadata.of(
                        "previousStatus", current.status(),
                        "newStatus", BookingStateMachine.RESCHEDULED,
                        "previousStartsAt", current.startsAt(),
                        "newStartsAt", startsAt,
                        "durationMinutes", durationMinutes,
                        "locationId", location.locationId()));
        return bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
    }

    @Transactional
    public BookingDetailResponse cancel(
            AuthenticatedUser authenticatedUser,
            UUID bookingId,
            CancelBookingRequest request) {
        BookingDetailResponse current = bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
        BookingStateMachine.assertTransition(current.status(), BookingStateMachine.CANCELLED, "cancelarse");
        String reason = normalizeRequiredValue(request.reason(), "reason", 2000);
        String notes = appendCancellationReason(current.notes(), reason);
        bookingJdbcRepository.cancelBooking(authenticatedUser.businessId(), bookingId, notes);
        auditService.record(authenticatedUser.businessId(), authenticatedUser.userId(),
                "BOOKING_CANCELLED", "BOOKING", bookingId,
                "Reserva cancelada: " + reason,
                AuditMetadata.of(
                        "previousStatus", current.status(),
                        "newStatus", BookingStateMachine.CANCELLED,
                        "reason", reason));
        return bookingJdbcRepository.findBookingDetail(authenticatedUser.businessId(), bookingId);
    }

    private LocationResolution resolveLocation(
            UUID businessId,
            UUID requestedLocationId,
            String requestedLocationText,
            UUID fallbackLocationId,
            boolean requiredWhenMultiple) {
        UUID candidateLocationId = requestedLocationId != null ? requestedLocationId : fallbackLocationId;
        if (candidateLocationId != null) {
            BusinessLocationRecord location = businessLocationJdbcRepository.findActiveById(businessId, candidateLocationId);
            return new LocationResolution(location.id(), requestedLocationText != null ? requestedLocationText : location.name());
        }

        long activeLocations = businessLocationJdbcRepository.countActive(businessId);
        if (activeLocations == 0) {
            return new LocationResolution(null, requestedLocationText);
        }
        if (activeLocations == 1) {
            BusinessLocationRecord location = businessLocationJdbcRepository.findDefaultActive(businessId)
                    .orElseThrow(() -> validationError("locationId", "No existe una sede activa para esta cita."));
            return new LocationResolution(location.id(), requestedLocationText != null ? requestedLocationText : location.name());
        }
        if (requiredWhenMultiple) {
            throw validationError("locationId", "Selecciona la sede para crear o actualizar la cita.");
        }
        return new LocationResolution(null, requestedLocationText);
    }

    private BookingJdbcRepository.CustomerRecord resolveCustomer(
            AuthenticatedUser authenticatedUser,
            CreateBookingRequest request) {
        if (request.customerId() != null) {
            return bookingJdbcRepository.findCustomerById(authenticatedUser.businessId(), request.customerId());
        }

        String customerName = normalizeRequiredValue(request.customerName(), "customerName", 160);
        NameParts nameParts = splitName(
                customerName,
                normalizeOptionalText(request.customerFirstName(), "customerFirstName", 80),
                normalizeOptionalText(request.customerLastName(), "customerLastName", 80));
        String phone = normalizePhone(request.customerPhone(), "customerPhone");
        String email = normalizeOptionalEmail(request.customerEmail(), "customerEmail");

        BookingJdbcRepository.CustomerRecord existing = bookingJdbcRepository
                .findCustomerByNormalizedPhone(authenticatedUser.businessId(), phone)
                .orElse(null);
        if (existing != null) {
            bookingJdbcRepository.updateCustomer(
                    authenticatedUser.businessId(),
                    existing.id(),
                    nameParts.firstName(),
                    nameParts.lastName(),
                    customerName,
                    phone,
                    email);
            return bookingJdbcRepository.findCustomerById(authenticatedUser.businessId(), existing.id());
        }

        UUID customerId = bookingJdbcRepository.insertCustomer(
                authenticatedUser.businessId(),
                nameParts.firstName(),
                nameParts.lastName(),
                customerName,
                phone,
                email);
        return bookingJdbcRepository.findCustomerById(authenticatedUser.businessId(), customerId);
    }

    private BookingJdbcRepository.LeadContextRecord ensureLeadBelongsToBusiness(
            AuthenticatedUser authenticatedUser,
            UUID leadId) {
        return bookingJdbcRepository.findLeadContext(authenticatedUser.businessId(), leadId);
    }

    private UUID resolveResponsibleUserId(AuthenticatedUser authenticatedUser, UUID responsibleUserId) {
        if (responsibleUserId == null) {
            return null;
        }
        return bookingJdbcRepository.findUserId(authenticatedUser.businessId(), responsibleUserId)
                .orElseThrow(() -> validationError("assignedUserId", "El responsable indicado no existe."));
    }

    private DateRange resolveDateRange(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime resolvedFrom = from != null
                ? from
                : now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime resolvedTo = to != null
                ? to
                : resolvedFrom.plusMonths(1).minusNanos(1);
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw validationError("from", "El rango de fechas es invalido.");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String normalized = search.trim();
        if (normalized.length() > 80) {
            throw validationError("search", "La busqueda no puede superar los 80 caracteres.");
        }
        return normalized;
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return normalizeStatus(status, BookingStateMachine.CONFIRMED);
    }

    private String normalizeStatus(String status, String defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }
        return switch (status.trim().toUpperCase()) {
            case "REQUESTED", "SOLICITADA" -> BookingStateMachine.REQUESTED;
            case "PENDIENTE_CONFIRMACION", "TEMPORARY", "TEMPORAL" -> BookingStateMachine.PENDING_CONFIRMATION;
            case "PENDING_PAYMENT", "PENDIENTE_PAGO" -> BookingStateMachine.PENDING_PAYMENT;
            case "CONFIRMED", "CONFIRMADA", "SCHEDULED" -> BookingStateMachine.CONFIRMED;
            case "RESCHEDULE_PENDING", "REPROGRAMACION_PENDIENTE" -> BookingStateMachine.RESCHEDULE_PENDING;
            case "RESCHEDULED", "REPROGRAMADA" -> BookingStateMachine.RESCHEDULED;
            case "CANCELLED", "CANCELED", "CANCELADA" -> BookingStateMachine.CANCELLED;
            case "COMPLETED", "ATTENDED", "ATENDIDA" -> BookingStateMachine.ATTENDED;
            case "NO_SHOW", "NO_ASISTE" -> BookingStateMachine.NO_SHOW;
            case "EXPIRED", "RELEASED", "LIBERADA", "EXPIRADA" -> BookingStateMachine.EXPIRED;
            case "PENDING", "PENDIENTE" -> BookingStateMachine.REQUESTED;
            default -> throw validationError("status", "El estado indicado no es valido.");
        };
    }

    private OffsetDateTime normalizeStartsAt(OffsetDateTime startsAt) {
        if (startsAt == null) {
            throw validationError("startsAt", "La fecha de inicio es obligatoria.");
        }
        if (startsAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw validationError("startsAt", "No puedes agendar una fecha u hora que ya paso.");
        }
        return startsAt;
    }

    private int normalizeDuration(Integer durationMinutes) {
        int resolved = durationMinutes == null ? 60 : durationMinutes;
        if (resolved < 15 || resolved > 720) {
            throw validationError("durationMinutes", "La duracion debe estar entre 15 y 720 minutos.");
        }
        return resolved;
    }

    private String normalizeRequiredValue(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw validationError(field, "Este campo es obligatorio.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validationError(field, "El valor supera el largo maximo permitido.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validationError(field, "El valor supera el largo maximo permitido.");
        }
        return normalized;
    }

    private String normalizePhone(String value, String field) {
        String normalized = normalizeRequiredValue(value, field, 30).replace(" ", "");
        if (normalized.length() < 8) {
            throw validationError(field, "El telefono debe tener al menos 8 caracteres.");
        }
        return normalized;
    }

    private String normalizeOptionalEmail(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 255) {
            throw validationError(field, "El correo supera el largo maximo permitido.");
        }
        return normalized;
    }

    private OffsetDateTime resolveCompletedAt(String status, OffsetDateTime startsAt) {
        return BookingStateMachine.ATTENDED.equals(BookingStateMachine.canonical(status)) ? startsAt : null;
    }

    private String appendCancellationReason(String currentNotes, String reason) {
        if (currentNotes == null || currentNotes.isBlank()) {
            return "Cancelacion: " + reason;
        }
        return currentNotes + "\n\nCancelacion: " + reason;
    }

    private NameParts splitName(String displayName, String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return new NameParts(firstName, lastName);
        }
        String[] parts = displayName.trim().split("\\s+", 2);
        if (parts.length == 1) {
            return new NameParts(parts[0], parts[0]);
        }
        return new NameParts(
                firstName != null ? firstName : parts[0],
                lastName != null ? lastName : parts[1]);
    }


    private ZoneId resolveZoneId(UUID businessId, UUID locationId) {
        if (locationId == null) {
            return ZoneId.of("America/Santiago");
        }
        BusinessLocationRecord location = businessLocationJdbcRepository.findActiveById(businessId, locationId);
        return location.timezone() != null ? ZoneId.of(location.timezone()) : ZoneId.of("America/Santiago");
    }

    private void ensureSlotAvailable(UUID businessId, UUID bookingId, UUID locationId, OffsetDateTime startsAt, int durationMinutes) {
        if (locationId == null) {
            return;
        }
        OffsetDateTime endsAt = startsAt.plusMinutes(durationMinutes);
        if (bookingConfirmationJdbcRepository.hasOverlappingActiveBooking(
                businessId, bookingId, locationId, startsAt, endsAt)) {
            LOGGER.warn("[diagnostico] Slot ocupado: businessId={} locationId={} startsAt={} endsAt={}",
                    businessId, locationId, startsAt, endsAt);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "BOOKING_SLOT_NOT_AVAILABLE",
                    "El horario ya esta ocupado para esta sucursal.",
                    Map.of("startsAt", "Selecciona otro horario disponible."));
        }
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "La solicitud contiene datos invalidos.",
                Map.of(field, message));
    }

    private record NameParts(String firstName, String lastName) {
    }

    private record LocationResolution(UUID locationId, String locationText) {
    }

    private record DateRange(OffsetDateTime from, OffsetDateTime to) {
    }
}
