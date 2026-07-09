package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.LocationRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ProfessionalRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.RoomRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ServiceRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.TimeWindowRecord;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.DateTimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BookingValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingValidationService.class);

    private static final Pattern CHILE_PHONE_PATTERN = Pattern.compile("^569\\d{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final int MAX_ADVANCE_DAYS = 60;

    private final CompleteAgendaJdbcRepository agendaRepository;
    private final AvailabilityService availabilityService;

    public BookingValidationService(CompleteAgendaJdbcRepository agendaRepository, AvailabilityService availabilityService) {
        this.agendaRepository = agendaRepository;
        this.availabilityService = availabilityService;
    }

    public ValidationContext validateAll(UUID businessId, ValidateBookingRequest request) {
        ValidationContext ctx = new ValidationContext(businessId, request);
        return validateAll(ctx);
    }

    public ValidationContext validateAll(ValidationContext ctx) {
        validateCustomer(ctx);
        if (ctx.hasErrors()) return ctx;

        validateService(ctx);
        if (ctx.hasErrors()) return ctx;

        validateBranch(ctx);
        if (ctx.hasErrors()) return ctx;

        validateProfessional(ctx);
        if (ctx.hasErrors()) return ctx;

        validateDateTime(ctx);
        if (ctx.hasErrors()) return ctx;

        validateOverlap(ctx);
        if (ctx.hasErrors()) return ctx;

        validateAvailability(ctx);
        return ctx;
    }

    public void validateCustomer(ValidationContext ctx) {
        CreateBookingCustomerData customer = ctx.request.customer();
        if (customer.name() == null || customer.name().isBlank()) {
            ctx.addError("customerName", "El nombre del cliente es obligatorio.");
            return;
        }
        if (customer.name().trim().length() > 160) {
            ctx.addError("customerName", "El nombre no puede superar los 160 caracteres.");
        }
        if (customer.phone() == null || customer.phone().isBlank()) {
            ctx.addError("customerPhone", "El telefono es obligatorio.");
            return;
        }
        String phone = customer.phone().replaceAll("[^0-9]", "");
        if (!CHILE_PHONE_PATTERN.matcher(phone).matches()) {
            ctx.addError("customerPhone", "El telefono debe tener formato chileno valido (569XXXXXXXX).");
        }
        if (customer.email() != null && !customer.email().isBlank()) {
            if (!EMAIL_PATTERN.matcher(customer.email().trim()).matches()) {
                ctx.addError("customerEmail", "El correo electronico no tiene un formato valido.");
            }
        }
        if (agendaRepository.isCustomerBlocked(ctx.businessId, customer.phone())) {
            ctx.addError("customerPhone", "El cliente se encuentra bloqueado. No es posible realizar reservas.");
        }
        if (agendaRepository.hasExcessiveNoShows(ctx.businessId, customer.phone())) {
            ctx.addError("customerPhone", "El cliente tiene exceso de inasistencias. No es posible reservar.");
        }
    }

    public void validateService(ValidationContext ctx) {
        ServiceRecord service = ctx.service();
        if (!service.active()) {
            ctx.addError("serviceId", "El servicio seleccionado no esta activo.");
        }
        if (!agendaRepository.isServiceCategoryActive(ctx.businessId, service.id())) {
            ctx.addError("serviceId", "La categoria del servicio no esta activa.");
        }
        if (service.durationMinutes() <= 0) {
            ctx.addError("serviceId", "El servicio debe tener una duracion mayor a cero.");
        }
        if (service.priceBase() != null && service.priceBase().signum() < 0) {
            ctx.addError("serviceId", "El precio del servicio no es valido.");
        }
        if (service.requiresPriorEvaluation()) {
            ctx.addError("serviceId", "El servicio requiere evaluacion previa. Agendala antes de continuar.");
        }
        if (service.requiresInformedConsent()) {
            ctx.setRequiresConsent(true);
        }
    }

    public void validateBranch(ValidationContext ctx) {
        LocationRecord location = ctx.location();
        if (location == null) {
            ctx.addError("locationId", "La sucursal no existe o no esta activa.");
            return;
        }
        if (!agendaRepository.isLocationServingService(ctx.businessId, location.id(), ctx.request.serviceId())) {
            ctx.addError("locationId", "La sucursal no atiende el servicio seleccionado.");
        }
        LocalDate slotDate = ctx.startsAt().atZoneSameInstant(ctx.zone()).toLocalDate();
        if (agendaRepository.isHoliday(ctx.businessId, location.id(), slotDate)) {
            ctx.addError("startsAt", "La sucursal esta cerrada por feriado en la fecha seleccionada.");
        }
        if (agendaRepository.isLocationClosedForMaintenance(ctx.businessId, location.id(), ctx.startsAt())) {
            ctx.addError("startsAt", "La sucursal esta cerrada por mantenimiento o bloqueo administrativo.");
        }
    }

    public void validateProfessional(ValidationContext ctx) {
        if (ctx.professionalId() == null) {
            ctx.addError("professionalId", "Se requiere un profesional para el servicio.");
            return;
        }
        if (!agendaRepository.isProfessionalActive(ctx.businessId, ctx.professionalId())) {
            ctx.addError("professionalId", "El profesional no esta activo.");
        }
        List<ProfessionalRecord> candidates = agendaRepository.findProfessionalCandidates(
                ctx.businessId, ctx.location().id(), ctx.request.serviceId(), ctx.professionalId());
        if (candidates.isEmpty()) {
            ctx.addError("professionalId", "El profesional no realiza este servicio en esta sucursal.");
            return;
        }
        int dayOfWeek = ctx.startsAt().getDayOfWeek().getValue();
        List<TimeWindowRecord> professionalHours = agendaRepository.findProfessionalHours(
                ctx.businessId, ctx.location().id(), ctx.professionalId(), dayOfWeek);
        if (professionalHours.isEmpty()) {
            ctx.addError("professionalId", "El profesional no atiende el dia seleccionado.");
        }
    }

    public void validateDateTime(ValidationContext ctx) {
        if (ctx.startsAt() == null) {
            ctx.addError("startsAt", "La fecha y hora son obligatorias.");
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(ctx.zone());
        if (!ctx.startsAt().isAfter(now)) {
            ctx.addError("startsAt", "No puedes agendar una hora que ya paso.");
        }
        long minutesAhead = java.time.Duration.between(now, ctx.startsAt()).toMinutes();
        int minMinutesAhead = ctx.request.minMinutesAhead() > 0 ? ctx.request.minMinutesAhead() : 60;
        if (minutesAhead < minMinutesAhead) {
            ctx.addError("startsAt", "La reserva debe hacerse con al menos " + minMinutesAhead + " minutos de anticipacion.");
        }
        long maxMinutesAhead = (long) MAX_ADVANCE_DAYS * 24 * 60;
        if (minutesAhead > maxMinutesAhead) {
            ctx.addError("startsAt", "La reserva no puede hacerse con mas de " + MAX_ADVANCE_DAYS + " dias de anticipacion.");
        }
        ServiceRecord service = ctx.service();
        OffsetDateTime effectiveStart = ctx.startsAt().minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = ctx.startsAt().plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
        int dayOfWeek = ctx.startsAt().getDayOfWeek().getValue();
        List<TimeWindowRecord> businessHours = agendaRepository.findBusinessHours(ctx.businessId, ctx.location().id(), dayOfWeek);
        if (!businessHours.isEmpty()) {
            boolean fits = businessHours.stream().anyMatch(w -> fitsInWindow(effectiveStart, endsAt, w, ctx.zone()));
            if (!fits) {
                ctx.addError("startsAt", "El horario seleccionado esta fuera del horario de atencion.");
            }
        }
        if (ctx.professionalId() != null) {
            List<TimeWindowRecord> profHours = agendaRepository.findProfessionalHours(
                    ctx.businessId, ctx.location().id(), ctx.professionalId(), dayOfWeek);
            if (!profHours.isEmpty()) {
                boolean fits = profHours.stream().anyMatch(w -> fitsInWindow(effectiveStart, endsAt, w, ctx.zone()));
                if (!fits) {
                    ctx.addError("startsAt", "El horario esta fuera del horario laboral del profesional.");
                }
            }
        }
    }

    public void validateOverlap(ValidationContext ctx) {
        ServiceRecord service = ctx.service();
        OffsetDateTime effectiveStart = ctx.startsAt().minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = ctx.startsAt().plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
        if (agendaRepository.hasConflict(ctx.businessId, ctx.request.excludeBookingId(), ctx.location().id(),
                ctx.professionalId(), ctx.roomId(), effectiveStart, endsAt)) {
            ctx.addError("startsAt", "El horario seleccionado ya esta ocupado.");
        }
        if (agendaRepository.hasBlock(ctx.businessId, ctx.location().id(),
                ctx.professionalId(), ctx.roomId(), effectiveStart, endsAt)) {
            ctx.addError("startsAt", "El horario seleccionado esta bloqueado.");
        }
    }

    public void validateAvailability(ValidationContext ctx) {
        ServiceRecord service = ctx.service();
        if (service == null) return;
        OffsetDateTime effectiveStart = ctx.startsAt().minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = ctx.startsAt().plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());

        try {
            availabilityService.checkProfessionalAbsence(ctx.businessId(), ctx.professionalId(), effectiveStart, endsAt);
        } catch (ApiException e) {
            ctx.addError("professionalId", e.getMessage());
        }

        try {
            availabilityService.checkProfessionalDailyCapacity(ctx.businessId(), ctx.professionalId(), ctx.startsAt());
        } catch (ApiException e) {
            ctx.addError("startsAt", e.getMessage());
        }
    }

    public void throwIfErrors(ValidationContext ctx) {
        if (ctx.hasErrors()) {
            LOGGER.warn("[diagnostico] Error de validacion al crear reserva: errors={}", ctx.errors());
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_VALIDATION_ERROR",
                    "La solicitud contiene errores de validacion.", ctx.errors());
        }
    }

    private boolean fitsInWindow(OffsetDateTime slotStart, OffsetDateTime slotEnd, TimeWindowRecord window, ZoneId zone) {
        LocalTime startLocal = slotStart.atZoneSameInstant(zone).toLocalTime();
        LocalTime endLocal = slotEnd.atZoneSameInstant(zone).toLocalTime();
        return !startLocal.isBefore(window.startTime()) && !endLocal.isAfter(window.endTime());
    }

    public static ZoneId resolveLocationZone(LocationRecord location) {
        String timezone = location != null ? location.timezone() : null;
        if (timezone == null || timezone.isBlank()) return ZoneId.of("America/Santiago");
        try { return ZoneId.of(timezone.trim()); }
        catch (DateTimeException e) { return ZoneId.of("America/Santiago"); }
    }

    public static class ValidationContext {
        private final UUID businessId;
        private final ValidateBookingRequest request;
        private final Map<String, String> errors = new java.util.LinkedHashMap<>();
        private LocationRecord location;
        private ServiceRecord service;
        private ZoneId zone;
        private OffsetDateTime startsAt;
        private UUID professionalId;
        private UUID roomId;
        private boolean requiresConsent;

        public ValidationContext(UUID businessId, ValidateBookingRequest request) {
            this.businessId = businessId;
            this.request = request;
        }

        public void addError(String field, String message) {
            errors.putIfAbsent(field, message);
        }

        public boolean hasErrors() { return !errors.isEmpty(); }
        public Map<String, String> errors() { return errors; }
        public UUID businessId() { return businessId; }
        public ValidateBookingRequest request() { return request; }

        public LocationRecord location() { return location; }
        public void setLocation(LocationRecord location) { this.location = location; }

        public ServiceRecord service() { return service; }
        public void setService(ServiceRecord service) { this.service = service; }

        public ZoneId zone() { return zone; }
        public void setZone(ZoneId zone) { this.zone = zone; }

        public OffsetDateTime startsAt() { return startsAt; }
        public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }

        public UUID professionalId() { return professionalId; }
        public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }

        public UUID roomId() { return roomId; }
        public void setRoomId(UUID roomId) { this.roomId = roomId; }

        public boolean requiresConsent() { return requiresConsent; }
        public void setRequiresConsent(boolean requiresConsent) { this.requiresConsent = requiresConsent; }
    }

    public record CreateBookingCustomerData(String name, String phone, String email) {
    }

    public record ValidateBookingRequest(
            UUID serviceId,
            UUID locationId,
            UUID professionalId,
            UUID roomId,
            OffsetDateTime startsAt,
            CreateBookingCustomerData customer,
            UUID excludeBookingId,
            int minMinutesAhead) {

        public ValidateBookingRequest {
            if (customer == null) throw new IllegalArgumentException("customer es obligatorio");
        }

        public ValidateBookingRequest(
                UUID serviceId, UUID locationId, UUID professionalId, UUID roomId,
                OffsetDateTime startsAt, CreateBookingCustomerData customer) {
            this(serviceId, locationId, professionalId, roomId, startsAt, customer, null, 60);
        }
    }
}
