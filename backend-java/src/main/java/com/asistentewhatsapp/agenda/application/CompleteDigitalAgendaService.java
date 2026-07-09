package com.asistentewhatsapp.agenda.application;

import com.asistentewhatsapp.agenda.api.AgendaAvailabilityRequest;
import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.agenda.api.AgendaBlockRequest;
import com.asistentewhatsapp.agenda.api.AgendaBlockResponse;
import com.asistentewhatsapp.agenda.api.AgendaCalendarResponse;
import com.asistentewhatsapp.agenda.api.AgendaCancelRequest;
import com.asistentewhatsapp.agenda.api.AgendaFilterOptionsResponse;
import com.asistentewhatsapp.agenda.api.AgendaRescheduleRequest;
import com.asistentewhatsapp.agenda.api.AgendaSlotResponse;
import com.asistentewhatsapp.agenda.api.CreateTemporaryAgendaBookingRequest;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.LocationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ProfessionalRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.RoomRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ServiceRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.TimeWindowRecord;
import com.asistentewhatsapp.bookings.api.BookingConfirmationLinkResponse;
import com.asistentewhatsapp.bookings.api.BookingDetailResponse;
import com.asistentewhatsapp.bookings.api.CreateBookingConfirmationLinkRequest;
import com.asistentewhatsapp.bookings.application.BookingStateMachine;
import com.asistentewhatsapp.bookings.application.BookingConfirmationService;
import com.asistentewhatsapp.bookings.application.AvailabilityService;
import com.asistentewhatsapp.bookings.infrastructure.BookingJdbcRepository;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompleteDigitalAgendaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteDigitalAgendaService.class);
    private static final int DEFAULT_EXPIRATION_MINUTES = 60;
    private static final int SLOT_STEP_MINUTES = 15;

    private final CompleteAgendaJdbcRepository repository;
    private final BookingJdbcRepository bookingJdbcRepository;
    private final BookingConfirmationService bookingConfirmationService;
    private final CalendarSyncService calendarSyncService;
    private final AuditService auditService;
    private final ChannelDispatchService channelDispatchService;
    private final AvailabilityService availabilityService;

    public CompleteDigitalAgendaService(
            CompleteAgendaJdbcRepository repository,
            BookingJdbcRepository bookingJdbcRepository,
            BookingConfirmationService bookingConfirmationService,
            CalendarSyncService calendarSyncService,
            AuditService auditService,
            ChannelDispatchService channelDispatchService,
            AvailabilityService availabilityService) {
        this.repository = repository;
        this.bookingJdbcRepository = bookingJdbcRepository;
        this.bookingConfirmationService = bookingConfirmationService;
        this.calendarSyncService = calendarSyncService;
        this.auditService = auditService;
        this.channelDispatchService = channelDispatchService;
        this.availabilityService = availabilityService;
    }

    @Transactional(readOnly = true)
    public AgendaAvailabilityResponse availability(AuthenticatedUser user, AgendaAvailabilityRequest request) {
        LocationRecord location = repository.findLocation(user.businessId(), request.locationId());
        ServiceRecord service = repository.findService(user.businessId(), request.locationId(), request.serviceId());
        int dayOfWeek = request.date().getDayOfWeek().getValue();

        if (repository.isHoliday(user.businessId(), request.locationId(), request.date())) {
            return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), request.date(),
                    service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), List.of());
        }

        List<TimeWindowRecord> businessHours = repository.findBusinessHours(user.businessId(), request.locationId(), dayOfWeek);
        if (businessHours.isEmpty()) {
            return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), request.date(),
                    service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), List.of());
        }

        List<ProfessionalRecord> professionals = repository.findProfessionalCandidates(
                user.businessId(), request.locationId(), request.serviceId(), request.professionalId());
        if (professionals.isEmpty()) {
            return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), request.date(),
                    service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), List.of());
        }

        List<RoomRecord> rooms = service.requiresRoom()
                ? repository.findRoomCandidates(user.businessId(), request.locationId(), request.serviceId(), request.roomId())
                : List.of(new RoomRecord(null, null));
        if (rooms.isEmpty()) {
            return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), request.date(),
                    service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), List.of());
        }

        int limit = normalizeLimit(request.maxSlots());
        List<AgendaSlotResponse> slots = new ArrayList<>();
        for (ProfessionalRecord professional : professionals) {
            List<TimeWindowRecord> professionalHours = repository.findProfessionalHours(
                    user.businessId(), request.locationId(), professional.id(), dayOfWeek);
            if (professionalHours.isEmpty()) {
                continue;
            }
            for (RoomRecord room : rooms) {
                collectSlots(user.businessId(), location, service, professional, room, request, businessHours, professionalHours, slots, limit);
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

    @Transactional
    public BookingDetailResponse createTemporaryBooking(AuthenticatedUser user, CreateTemporaryAgendaBookingRequest request) {
        LocationRecord location = repository.findLocation(user.businessId(), request.locationId());
        ServiceRecord service = repository.findService(user.businessId(), request.locationId(), request.serviceId());
        UUID professionalId = resolveRequiredProfessional(user, request, service);
        UUID roomId = resolveRoom(user, request, service);
        OffsetDateTime startsAt = normalizeStartsAt(request.startsAt());
        assertSlotBookable(user.businessId(), location.id(), service.id(), professionalId, roomId, startsAt, null);
        OffsetDateTime effectiveStart = startsAt.minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());

        CompleteAgendaJdbcRepository.CustomerRecord customer = repository.findOrCreateCustomer(
                user.businessId(), request.customerId(), normalizeCustomerName(request.customerName()), normalizePhone(request.customerPhone()), request.customerEmail());
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(resolveExpirationMinutes(request.expirationMinutes()));
        UUID bookingId = repository.insertTemporaryBooking(
                user.businessId(), customer.id(), request.conversationId(), request.leadId(), user.userId(),
                service.name(), location.id(), service.id(), professionalId, roomId, startsAt, endsAt,
                service.durationMinutes(), expiresAt, service.requiresDeposit(), service.depositAmount(), request.notes());
        auditService.record(user.businessId(), user.userId(), "AGENDA_TEMPORARY_BOOKING_CREATED", "BOOKING", bookingId,
                "Reserva temporal creada por agenda digital completa.",
                AuditMetadata.of(
                        "source", historySource(user),
                        "newStatus", BookingStateMachine.PENDING_CONFIRMATION,
                        "locationId", location.id(),
                        "serviceId", service.id(),
                        "professionalId", professionalId,
                        "roomId", roomId,
                        "startsAt", startsAt,
                        "endsAt", endsAt,
                        "temporaryExpiresAt", expiresAt,
                        "requiresDeposit", service.requiresDeposit(),
                        "depositAmount", service.depositAmount()));

        if (Boolean.TRUE.equals(request.generateConfirmationLink())) {
            bookingConfirmationService.createConfirmationLink(
                    user,
                    bookingId,
                    new CreateBookingConfirmationLinkRequest(resolveExpirationMinutes(request.expirationMinutes()), request.sendWhatsApp()));
        }
        return bookingJdbcRepository.findBookingDetail(user.businessId(), bookingId);
    }

    @Transactional(readOnly = true)
    public AgendaFilterOptionsResponse filterOptions(AuthenticatedUser user, UUID locationId) {
        return new AgendaFilterOptionsResponse(
                repository.findServiceFilterOptions(user.businessId(), locationId),
                repository.findProfessionalFilterOptions(user.businessId(), locationId),
                repository.findRoomFilterOptions(user.businessId(), locationId));
    }

    @Transactional(readOnly = true)
    public AgendaCalendarResponse calendar(AuthenticatedUser user, OffsetDateTime from, OffsetDateTime to, UUID locationId,
            UUID professionalId, UUID roomId, UUID serviceId, String status) {
        OffsetDateTime resolvedFrom = from != null ? from : OffsetDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime resolvedTo = to != null ? to : resolvedFrom.plusDays(7);
        if (!resolvedTo.isAfter(resolvedFrom)) {
            throw validationError("to", "El rango final debe ser posterior al inicio.");
        }
        return new AgendaCalendarResponse(resolvedFrom, resolvedTo,
                repository.findCalendar(user.businessId(), resolvedFrom, resolvedTo, locationId, professionalId, roomId, serviceId, normalizeStatus(status)));
    }

    @Transactional
    public BookingDetailResponse reschedule(AuthenticatedUser user, UUID bookingId, AgendaRescheduleRequest request) {
        BookingDetailResponse currentBooking = bookingJdbcRepository.findBookingDetail(user.businessId(), bookingId);
        BookingStateMachine.assertTransition(currentBooking.status(), BookingStateMachine.RESCHEDULED, "reprogramarse");
        LocationRecord location = repository.findLocation(user.businessId(), request.locationId());
        ServiceRecord service = repository.findService(user.businessId(), request.locationId(), request.serviceId());
        UUID professionalId = request.professionalId();
        UUID roomId = service.requiresRoom() ? request.roomId() : null;
        if (professionalId == null) {
            throw validationError("professionalId", "Selecciona el profesional para reprogramar.");
        }
        if (service.requiresRoom() && roomId == null) {
            throw validationError("roomId", "Selecciona la cabina para reprogramar.");
        }
        OffsetDateTime startsAt = normalizeStartsAt(request.startsAt());
        assertSlotBookable(user.businessId(), location.id(), service.id(), professionalId, roomId, startsAt, bookingId);
        OffsetDateTime effectiveStart = startsAt.minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
        repository.updateBookingSchedule(user.businessId(), bookingId, user.userId(), location.id(), service.id(), professionalId, roomId,
                startsAt, endsAt, service.durationMinutes(), request.reason(), historySource(user));
        scheduleDefaultReminders(user.businessId(), bookingId, startsAt);
        auditService.record(user.businessId(), user.userId(), "AGENDA_BOOKING_RESCHEDULED", "BOOKING", bookingId,
                "Reserva reprogramada desde agenda digital completa.",
                AuditMetadata.of(
                        "source", historySource(user),
                        "previousStatus", BookingStateMachine.canonical(currentBooking.status()),
                        "newStatus", BookingStateMachine.RESCHEDULED,
                        "locationId", location.id(),
                        "serviceId", service.id(),
                        "professionalId", professionalId,
                        "roomId", roomId,
                        "startsAt", startsAt,
                        "endsAt", endsAt,
                        "reason", request.reason()));
        try { calendarSyncService.syncRescheduled(bookingId, user.businessId()); } catch (Exception ignored) {}
        return bookingJdbcRepository.findBookingDetail(user.businessId(), bookingId);
    }

    @Transactional
    public BookingDetailResponse cancel(AuthenticatedUser user, UUID bookingId, AgendaCancelRequest request) {
        BookingDetailResponse currentBooking = bookingJdbcRepository.findBookingDetail(user.businessId(), bookingId);
        BookingStateMachine.assertTransition(currentBooking.status(), BookingStateMachine.CANCELLED, "cancelarse");
        repository.cancelBooking(user.businessId(), bookingId, user.userId(), request.reason(), historySource(user));
        auditService.record(user.businessId(), user.userId(), "AGENDA_BOOKING_CANCELLED", "BOOKING", bookingId,
                "Reserva cancelada desde agenda digital completa.",
                AuditMetadata.of(
                        "source", historySource(user),
                        "previousStatus", BookingStateMachine.canonical(currentBooking.status()),
                        "newStatus", BookingStateMachine.CANCELLED,
                        "reason", request.reason()));
        try { calendarSyncService.syncCancelled(bookingId, user.businessId()); } catch (Exception ignored) {}
        return bookingJdbcRepository.findBookingDetail(user.businessId(), bookingId);
    }

    public void cancelByCustomer(UUID businessId, UUID bookingId, String reason) {
        BookingDetailResponse currentBooking = bookingJdbcRepository.findBookingDetail(businessId, bookingId);
        BookingStateMachine.assertTransition(currentBooking.status(), BookingStateMachine.CANCELLED_BY_CUSTOMER, "cancelarse");
        repository.cancelBookingByCustomer(businessId, bookingId, reason, "WHATSAPP");
    }

    @Transactional
    public AgendaBlockResponse createBlock(AuthenticatedUser user, AgendaBlockRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw validationError("endsAt", "El termino del bloqueo debe ser posterior al inicio.");
        }
        AgendaBlockResponse response = repository.insertBlock(
                user.businessId(), user.userId(), request.locationId(), request.professionalId(), request.roomId(),
                request.startsAt(), request.endsAt(), request.reason());
        auditService.record(user.businessId(), user.userId(), "AGENDA_BLOCK_CREATED", "AGENDA_BLOCK", response.id(),
                "Bloqueo manual creado en agenda digital completa.");
        return response;
    }

    @Scheduled(fixedDelayString = "${app.complete-agenda.expiration-scan-ms:60000}")
    @Transactional
    public void expireTemporaryBookings() {
        repository.releaseExpiredTemporaryBookings(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void collectSlots(UUID businessId, LocationRecord location, ServiceRecord service, ProfessionalRecord professional, RoomRecord room,
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
                LocalTime cursor = applyPreference(start, end, request.preference());
                while (!cursor.plusMinutes(service.durationMinutes()).isAfter(end) && slots.size() < limit) {
                    OffsetDateTime startsAt = request.date().atTime(cursor).atZone(locationZone).toOffsetDateTime();
                    OffsetDateTime effectiveStart = startsAt.minusMinutes(service.preparationMinutes());
                    OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
                    boolean available = !repository.hasConflict(businessId, null, location.id(), professional.id(), room.id(), effectiveStart, endsAt)
                            && !repository.hasBlock(businessId, location.id(), professional.id(), room.id(), effectiveStart, endsAt)
                            && startsAt.isAfter(nowAtLocation);
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

    private UUID resolveRequiredProfessional(AuthenticatedUser user, CreateTemporaryAgendaBookingRequest request, ServiceRecord service) {
        if (request.professionalId() != null) {
            return request.professionalId();
        }
        List<ProfessionalRecord> candidates = repository.findProfessionalCandidates(
                user.businessId(), request.locationId(), service.id(), null);
        if (candidates.isEmpty()) {
            throw validationError("professionalId", "No hay profesionales disponibles para este servicio y sucursal.");
        }
        return candidates.getFirst().id();
    }

    private UUID resolveRoom(AuthenticatedUser user, CreateTemporaryAgendaBookingRequest request, ServiceRecord service) {
        if (!service.requiresRoom()) {
            return null;
        }
        if (request.roomId() != null) {
            return request.roomId();
        }
        List<RoomRecord> rooms = repository.findRoomCandidates(user.businessId(), request.locationId(), service.id(), null);
        if (rooms.isEmpty()) {
            throw validationError("roomId", "No hay cabinas disponibles para este servicio y sucursal.");
        }
        return rooms.getFirst().id();
    }

    public void assertSlotBookable(
            UUID businessId,
            UUID locationId,
            UUID serviceId,
            UUID professionalId,
            UUID roomId,
            OffsetDateTime startsAt,
            UUID excludeBookingId) {
        if (startsAt == null) {
            throw validationError("startsAt", "La fecha y hora son obligatorias.");
        }
        if (startsAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw validationError("startsAt", "La fecha y hora deben ser futuras.");
        }

        LocationRecord location = repository.findLocation(businessId, locationId);
        ServiceRecord service = repository.findService(businessId, locationId, serviceId);
        ZoneId zone = resolveLocationZone(location);
        OffsetDateTime effectiveStart = startsAt.minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());

        LocalDate slotDate = startsAt.atZoneSameInstant(zone).toLocalDate();
        if (repository.isHoliday(businessId, locationId, slotDate)) {
            throw conflict("AGENDA_HOLIDAY", "La fecha seleccionada es feriado.",
                    Map.of("startsAt", "Selecciona otra fecha."));
        }

        int dayOfWeek = startsAt.getDayOfWeek().getValue();
        List<TimeWindowRecord> businessHours = repository.findBusinessHours(businessId, locationId, dayOfWeek);
        if (businessHours.isEmpty()) {
            throw conflict("AGENDA_OUTSIDE_BUSINESS_HOURS", "El negocio no atiende el dia seleccionado.",
                    Map.of("startsAt", "Selecciona un dia habil."));
        }
        if (businessHours.stream().noneMatch(w -> fitsInWindow(effectiveStart, endsAt, w, zone))) {
            throw conflict("AGENDA_OUTSIDE_BUSINESS_HOURS", "El horario esta fuera del horario de atencion del negocio.",
                    Map.of("startsAt", "Selecciona un horario dentro del horario de atencion."));
        }

        List<TimeWindowRecord> professionalHours = repository.findProfessionalHours(businessId, locationId, professionalId, dayOfWeek);
        if (professionalHours.isEmpty()) {
            throw conflict("AGENDA_PROFESSIONAL_NOT_AVAILABLE", "El profesional no atiende el dia seleccionado.",
                    Map.of("professionalId", "Selecciona otro profesional o fecha."));
        }
        if (professionalHours.stream().noneMatch(w -> fitsInWindow(effectiveStart, endsAt, w, zone))) {
            throw conflict("AGENDA_PROFESSIONAL_NOT_AVAILABLE", "El profesional no esta disponible en el horario seleccionado.",
                    Map.of("startsAt", "Selecciona un horario dentro del horario del profesional."));
        }

        List<ProfessionalRecord> candidates = repository.findProfessionalCandidates(businessId, locationId, serviceId, professionalId);
        if (candidates.isEmpty()) {
            throw conflict("AGENDA_PROFESSIONAL_NOT_CANDIDATE", "El profesional no realiza este servicio en esta sucursal.",
                    Map.of("professionalId", "Selecciona otro profesional."));
        }

        availabilityService.checkProfessionalAbsence(businessId, professionalId, effectiveStart, endsAt);
        availabilityService.checkProfessionalDailyCapacity(businessId, professionalId, startsAt);

        if (service.requiresRoom()) {
            if (roomId == null) {
                throw validationError("roomId", "El servicio requiere una cabina.");
            }
            List<RoomRecord> rooms = repository.findRoomCandidates(businessId, locationId, serviceId, roomId);
            if (rooms.isEmpty()) {
                throw conflict("AGENDA_ROOM_NOT_CANDIDATE", "La cabina no esta disponible para este servicio.",
                        Map.of("roomId", "Selecciona otra cabina."));
            }
        }

        if (repository.hasConflict(businessId, excludeBookingId, locationId, professionalId, roomId, effectiveStart, endsAt)) {
            throw conflict("AGENDA_SLOT_NOT_AVAILABLE", "El horario ya esta ocupado.",
                    Map.of("startsAt", "Selecciona otro horario disponible."));
        }

        if (repository.hasBlock(businessId, locationId, professionalId, roomId, effectiveStart, endsAt)) {
            throw conflict("AGENDA_SLOT_BLOCKED", "El horario esta bloqueado.",
                    Map.of("startsAt", "Selecciona otro horario disponible."));
        }
    }

    private boolean fitsInWindow(OffsetDateTime slotStart, OffsetDateTime slotEnd, TimeWindowRecord window, ZoneId zone) {
        LocalTime startLocal = slotStart.atZoneSameInstant(zone).toLocalTime();
        LocalTime endLocal = slotEnd.atZoneSameInstant(zone).toLocalTime();
        return !startLocal.isBefore(window.startTime()) && !endLocal.isAfter(window.endTime());
    }

    private ApiException conflict(String code, String message, Map<String, String> fieldErrors) {
        LOGGER.warn("[diagnostico] Conflicto al crear reserva: code={} message={} fieldErrors={}", code, message, fieldErrors);
        return new ApiException(HttpStatus.CONFLICT, code, message, fieldErrors);
    }

    private String historySource(AuthenticatedUser user) {
        return user.userId() == null ? "WHATSAPP" : "ADMIN";
    }

    private void scheduleDefaultReminders(UUID businessId, UUID bookingId, OffsetDateTime startsAt) {
        repository.cancelPendingReminders(businessId, bookingId);
        repository.insertReminder(businessId, bookingId, "CONFIRMATION", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));
        repository.insertReminder(businessId, bookingId, "TWENTY_FOUR_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(24));
        repository.insertReminder(businessId, bookingId, "TWENTY_FOUR_HOURS_BEFORE", "EMAIL", startsAt.minusHours(24));
        repository.insertReminder(businessId, bookingId, "TWO_HOURS_BEFORE", "WHATSAPP", startsAt.minusHours(2));
        repository.insertReminder(businessId, bookingId, "TWO_HOURS_BEFORE", "EMAIL", startsAt.minusHours(2));
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

    private int normalizeLimit(Integer maxSlots) {
        if (maxSlots == null) {
            return 12;
        }
        return Math.min(Math.max(maxSlots, 1), 40);
    }

    private OffsetDateTime normalizeStartsAt(OffsetDateTime startsAt) {
        if (startsAt == null) {
            throw validationError("startsAt", "La fecha y hora son obligatorias.");
        }
        if (startsAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw validationError("startsAt", "La fecha y hora deben ser futuras.");
        }
        return startsAt;
    }

    private String normalizeCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank() || customerName.trim().length() > 160) {
            throw validationError("customerName", "Ingresa un nombre de cliente valido.");
        }
        return customerName.trim();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw validationError("customerPhone", "Ingresa un telefono valido.");
        }
        String normalized = phone.replace(" ", "").trim();
        if (normalized.length() < 8 || normalized.length() > 30) {
            throw validationError("customerPhone", "El telefono debe tener entre 8 y 30 caracteres.");
        }
        return normalized;
    }

    private int resolveExpirationMinutes(Integer expirationMinutes) {
        if (expirationMinutes == null) {
            return DEFAULT_EXPIRATION_MINUTES;
        }
        if (expirationMinutes < 5 || expirationMinutes > 1440) {
            throw validationError("expirationMinutes", "La expiracion debe estar entre 5 y 1440 minutos.");
        }
        return expirationMinutes;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private LocalTime max(LocalTime left, LocalTime right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalTime min(LocalTime left, LocalTime right) {
        return left.isBefore(right) ? left : right;
    }

    private LocalTime applyPreference(LocalTime start, LocalTime end, String preference) {
        if (preference == null || preference.isBlank()) {
            return start;
        }
        String normalized = preference.trim().toUpperCase();
        if ("AFTERNOON".equals(normalized) || "TARDE".equals(normalized)) {
            LocalTime afternoon = LocalTime.of(14, 0);
            return afternoon.isAfter(start) && afternoon.isBefore(end) ? afternoon : start;
        }
        if ("MORNING".equals(normalized) || "MANANA".equals(normalized) || "MAÑANA".equals(normalized)) {
            return start;
        }
        return start;
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "AGENDA_VALIDATION_ERROR", "La solicitud de agenda contiene datos invalidos.",
                Map.of(field, message));
    }
}
