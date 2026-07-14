package com.asistentewhatsapp.customerbookings.application;

import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.agenda.api.AgendaCalendarItemResponse;
import com.asistentewhatsapp.agenda.api.AgendaSlotResponse;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.LocationRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ProfessionalRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.RoomRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.ServiceRecord;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.TimeWindowRecord;
import com.asistentewhatsapp.aesthetic.api.AestheticServiceResponse;
import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository;
import com.asistentewhatsapp.bookings.application.BookingStateMachine;
import com.asistentewhatsapp.customerbookings.api.CustomerBookingItemResponse;
import com.asistentewhatsapp.customerbookings.api.CustomerBookingReschedulePreviewResponse;
import com.asistentewhatsapp.customerbookings.api.CustomerBookingReschedulePreviewResponse.LocationOption;
import com.asistentewhatsapp.customerbookings.api.CustomerBookingReschedulePreviewResponse.ServiceOption;
import com.asistentewhatsapp.customerbookings.api.CustomerBookingRescheduleRequest;
import com.asistentewhatsapp.customerbookings.infrastructure.CustomerBookingTokenJdbcRepository;
import com.asistentewhatsapp.landing.api.PublicCustomerInfoResponse;
import com.asistentewhatsapp.customerbookings.infrastructure.CustomerBookingTokenJdbcRepository.TokenRecord;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.security.application.TokenHashService;
import com.asistentewhatsapp.shared.exception.ApiException;
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
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerBookingService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SLOT_STEP_MINUTES = 15;

    private final CustomerBookingTokenJdbcRepository tokenRepository;
    private final CompleteAgendaJdbcRepository agendaRepository;
    private final BusinessLocationJdbcRepository locationRepository;
    private final AestheticCenterJdbcRepository aestheticRepository;
    private final TokenHashService tokenHashService;
    private final String frontendPublicBaseUrl;

    public CustomerBookingService(
            CustomerBookingTokenJdbcRepository tokenRepository,
            CompleteAgendaJdbcRepository agendaRepository,
            BusinessLocationJdbcRepository locationRepository,
            AestheticCenterJdbcRepository aestheticRepository,
            TokenHashService tokenHashService,
            @Value("${app.frontend.public-base-url:http://localhost:5173}") String frontendPublicBaseUrl) {
        this.tokenRepository = tokenRepository;
        this.agendaRepository = agendaRepository;
        this.locationRepository = locationRepository;
        this.aestheticRepository = aestheticRepository;
        this.tokenHashService = tokenHashService;
        this.frontendPublicBaseUrl = frontendPublicBaseUrl;
    }

    @Transactional
    public String generateToken(UUID businessId, String phoneDigits) {
        String raw = generateRawToken();
        String hash = tokenHashService.sha256(raw);
        UUID id = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(60);
        tokenRepository.insert(id, businessId, hash, phoneDigits, expiresAt);
        return raw;
    }

    public String buildPublicUrl(String rawToken) {
        String base = frontendPublicBaseUrl.endsWith("/") ? frontendPublicBaseUrl : frontendPublicBaseUrl + "/";
        return base + "reservas/mis-reservas/" + rawToken;
    }

    public String buildBookingPublicUrl(String rawToken) {
        String base = frontendPublicBaseUrl.endsWith("/") ? frontendPublicBaseUrl : frontendPublicBaseUrl + "/";
        return base + "reservar?token=" + rawToken;
    }

    public PublicCustomerInfoResponse getCustomerInfoByToken(String rawToken) {
        TokenRecord token = resolveToken(rawToken);
        UUID businessId = token.businessId();
        String phoneDigits = token.phoneDigits();
        Optional<CompleteAgendaJdbcRepository.CustomerRecord> customerOpt = agendaRepository.findCustomerByPhone(businessId, phoneDigits);
        if (customerOpt.isEmpty()) {
            return new PublicCustomerInfoResponse(null, null, null, null, null);
        }
        CompleteAgendaJdbcRepository.CustomerRecord customer = customerOpt.get();
        Optional<UUID> lastLocationId = agendaRepository.findLastCustomerLocationId(businessId, phoneDigits);
        String lastLocationName = null;
        if (lastLocationId.isPresent()) {
            BusinessLocationRecord loc = locationRepository.findOptionalById(businessId, lastLocationId.get()).orElse(null);
            lastLocationName = loc != null ? loc.name() : null;
        }
        return new PublicCustomerInfoResponse(
                customer.displayName(),
                customer.phone(),
                customer.email(),
                lastLocationId.orElse(null),
                lastLocationName);
    }

    public List<AgendaCalendarItemResponse> findActiveBookingsByToken(String rawToken) {
        TokenRecord token = resolveToken(rawToken);
        return agendaRepository.findActiveBookingsByPhone(
                token.businessId(), token.phoneDigits());
    }

    public AgendaCalendarItemResponse findActiveBookingById(String rawToken, UUID bookingId) {
        List<AgendaCalendarItemResponse> bookings = findActiveBookingsByToken(rawToken);
        return bookings.stream()
                .filter(b -> b.bookingId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND",
                        "No se encontro la reserva indicada.",
                        Map.of("bookingId", "Reserva no encontrada o ya no esta activa.")));
    }

    @Transactional
    public void cancelBooking(String rawToken, UUID bookingId, String reason) {
        TokenRecord token = resolveToken(rawToken);
        AgendaCalendarItemResponse booking = findActiveBookingById(rawToken, bookingId);
        if (BookingStateMachine.isClosed(booking.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_ALREADY_CLOSED",
                    "La reserva ya no esta activa.",
                    Map.of("status", booking.status()));
        }
        agendaRepository.cancelBookingByCustomer(token.businessId(), bookingId,
                reason == null || reason.isBlank()
                        ? "Cancelacion solicitada por el cliente desde enlace publico." : reason,
                "PUBLIC_LINK");
    }

    @Transactional(readOnly = true)
    public CustomerBookingReschedulePreviewResponse previewReschedule(String rawToken, UUID bookingId) {
        TokenRecord token = resolveToken(rawToken);
        AgendaCalendarItemResponse booking = findActiveBookingById(rawToken, bookingId);
        UUID businessId = token.businessId();

        List<AestheticServiceResponse> svc = aestheticRepository.findServices(businessId, 0, 1000, null, null, true).items();
        List<BusinessLocationRecord> locs = locationRepository.findActive(businessId);

        List<ServiceOption> services = svc.stream()
                .map(s -> new ServiceOption(s.id(), s.name(), s.categoryName(),
                        s.durationMinutes() != null ? s.durationMinutes() : 0,
                        "REQUIRED".equalsIgnoreCase(s.professionalRequired())))
                .toList();
        List<LocationOption> locations = locs.stream()
                .map(l -> new LocationOption(l.id(), l.name(), l.address(), l.commune()))
                .toList();

        return new CustomerBookingReschedulePreviewResponse(toItemResponse(booking), services, locations);
    }

    @Transactional(readOnly = true)
    public AgendaAvailabilityResponse getAvailability(String rawToken, UUID bookingId,
            UUID serviceId, UUID locationId, LocalDate date) {
        resolveToken(rawToken);
        resolveBooking(rawToken, bookingId);
        return computeAvailability(resolveToken(rawToken).businessId(), bookingId, locationId, serviceId, date, null);
    }

    @Transactional
    public AgendaCalendarItemResponse rescheduleBooking(String rawToken, UUID bookingId, CustomerBookingRescheduleRequest request) {
        TokenRecord token = resolveToken(rawToken);
        AgendaCalendarItemResponse booking = findActiveBookingById(rawToken, bookingId);
        UUID businessId = token.businessId();

        if (BookingStateMachine.isClosed(booking.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_ALREADY_CLOSED",
                    "La reserva ya no esta activa.", Map.of("status", booking.status()));
        }

        LocationRecord location = agendaRepository.findLocation(businessId, request.locationId());
        ServiceRecord service = agendaRepository.findService(businessId, request.locationId(), request.serviceId());
        UUID professionalId = request.professionalId();
        UUID roomId = service.requiresRoom() ? request.roomId() : null;

        if (professionalId == null) {
            List<ProfessionalRecord> candidates = agendaRepository.findProfessionalCandidates(
                    businessId, request.locationId(), request.serviceId(), null);
            if (candidates.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "NO_PROFESSIONAL_AVAILABLE",
                        "No hay profesionales disponibles para el servicio y sucursal seleccionados.", Map.of());
            }
            professionalId = candidates.getFirst().id();
        }
        if (service.requiresRoom() && roomId == null) {
            List<RoomRecord> candidates = agendaRepository.findRoomCandidates(
                    businessId, request.locationId(), request.serviceId(), null);
            if (candidates.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "NO_ROOM_AVAILABLE",
                        "No hay cabinas disponibles para el servicio y sucursal seleccionados.", Map.of());
            }
            roomId = candidates.getFirst().id();
        }

        OffsetDateTime startsAt = request.startsAt();
        assertSlotBookable(businessId, bookingId, request.locationId(), service, professionalId, roomId, startsAt);

        String reason = request.reason() == null || request.reason().isBlank()
                ? "Reprogramacion solicitada por el cliente desde enlace publico."
                : request.reason();
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
        agendaRepository.updateBookingSchedule(businessId, bookingId, null,
                request.locationId(), request.serviceId(), professionalId, roomId,
                startsAt, endsAt, service.durationMinutes(), reason, "PUBLIC_LINK");
        return findActiveBookingById(rawToken, bookingId);
    }

    private void assertSlotBookable(UUID businessId, UUID bookingId, UUID locationId,
            ServiceRecord service, UUID professionalId, UUID roomId, OffsetDateTime startsAt) {
        OffsetDateTime effectiveStart = startsAt.minusMinutes(service.preparationMinutes());
        OffsetDateTime endsAt = startsAt.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
        if (agendaRepository.hasConflict(businessId, bookingId, locationId, professionalId, roomId, effectiveStart, endsAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE",
                    "El horario seleccionado ya no esta disponible. Elige otro horario.", Map.of());
        }
        if (agendaRepository.hasBlock(businessId, locationId, professionalId, roomId, effectiveStart, endsAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_BLOCKED",
                    "El horario seleccionado esta bloqueado. Elige otro horario.", Map.of());
        }
    }

    private AgendaAvailabilityResponse computeAvailability(UUID businessId, UUID excludeBookingId, UUID locationId, UUID serviceId,
            LocalDate date, Integer maxSlots) {
        LocationRecord location = agendaRepository.findLocation(businessId, locationId);
        ServiceRecord service = agendaRepository.findService(businessId, locationId, serviceId);
        int dayOfWeek = date.getDayOfWeek().getValue();
        ZoneId locationZone = resolveLocationZone(location);
        OffsetDateTime nowAtLocation = OffsetDateTime.now(locationZone);

        if (agendaRepository.isHoliday(businessId, locationId, date)) {
            return emptyAvailability(location, service, date);
        }

        List<TimeWindowRecord> businessHours = agendaRepository.findBusinessHours(businessId, locationId, dayOfWeek);
        if (businessHours.isEmpty()) return emptyAvailability(location, service, date);

        List<ProfessionalRecord> professionals = agendaRepository.findProfessionalCandidates(businessId, locationId, serviceId, null);
        if (professionals.isEmpty()) return emptyAvailability(location, service, date);

        List<RoomRecord> rooms = service.requiresRoom()
                ? agendaRepository.findRoomCandidates(businessId, locationId, serviceId, null)
                : List.of(new RoomRecord(null, null));
        if (rooms.isEmpty()) return emptyAvailability(location, service, date);

        int limit = normalizeLimit(maxSlots);
        List<AgendaSlotResponse> slots = new ArrayList<>();

        for (ProfessionalRecord professional : professionals) {
            List<TimeWindowRecord> professionalHours = agendaRepository.findProfessionalHours(
                    businessId, locationId, professional.id(), dayOfWeek);
            if (professionalHours.isEmpty()) continue;

            for (RoomRecord room : rooms) {
                for (TimeWindowRecord bw : businessHours) {
                    for (TimeWindowRecord pw : professionalHours) {
                        LocalTime start = max(bw.startTime(), pw.startTime());
                        LocalTime end = min(bw.endTime(), pw.endTime());
                        if (!end.isAfter(start)) continue;
                        LocalTime cursor = start;
                        while (!cursor.plusMinutes(service.durationMinutes()).isAfter(end) && slots.size() < limit) {
                            OffsetDateTime slotStart = date.atTime(cursor).atZone(locationZone).toOffsetDateTime();
                            OffsetDateTime effStart = slotStart.minusMinutes(service.preparationMinutes());
                            OffsetDateTime slotEnd = slotStart.plusMinutes(service.durationMinutes()).plusMinutes(service.cleanupMinutes());
                            boolean available = !agendaRepository.hasConflict(businessId, excludeBookingId, locationId,
                                    professional.id(), room.id(), effStart, slotEnd)
                                    && !agendaRepository.hasBlock(businessId, locationId, professional.id(),
                                            room.id(), effStart, slotEnd)
                                    && slotStart.isAfter(nowAtLocation);
                            if (available) {
                                slots.add(new AgendaSlotResponse(slotStart, slotEnd, locationId, location.name(),
                                        serviceId, service.name(), service.durationMinutes(),
                                        professional.id(), professional.name(), room.id(), room.name(),
                                        true, "Disponible"));
                            }
                            cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
                        }
                        if (slots.size() >= limit) break;
                    }
                    if (slots.size() >= limit) break;
                }
                if (slots.size() >= limit) break;
            }
            if (slots.size() >= limit) break;
        }
        slots.sort(Comparator.comparing(AgendaSlotResponse::startsAt));
        if (slots.size() > limit) slots = slots.subList(0, limit);
        return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), date,
                service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), slots);
    }

    private void resolveBooking(String rawToken, UUID bookingId) {
        boolean exists = findActiveBookingsByToken(rawToken).stream()
                .anyMatch(b -> b.bookingId().equals(bookingId));
        if (!exists) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND",
                    "No se encontro la reserva indicada.", Map.of("bookingId", "No encontrada."));
        }
    }

    private TokenRecord resolveToken(String rawToken) {
        String hash = tokenHashService.sha256(normalizeToken(rawToken));
        return tokenRepository.findValidByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOKEN_INVALIDO",
                        "El enlace no es valido o expiro. Solicita uno nuevo por WhatsApp.",
                        Map.of("token", "Invalido o vencido.")));
    }

    private AgendaAvailabilityResponse emptyAvailability(LocationRecord location, ServiceRecord service, LocalDate date) {
        return new AgendaAvailabilityResponse(location.id(), location.name(), service.id(), service.name(), date,
                service.durationMinutes(), service.requiresRoom(), service.requiresDeposit(), List.of());
    }

    private CustomerBookingItemResponse toItemResponse(AgendaCalendarItemResponse item) {
        return new CustomerBookingItemResponse(
                item.bookingId(),
                item.locationId(),
                item.serviceId(),
                item.professionalId(),
                item.roomId(),
                item.serviceName() != null ? item.serviceName() : item.subject(),
                item.locationName(),
                item.professionalName(),
                item.startsAt(),
                item.endsAt(),
                item.durationMinutes(),
                item.status(),
                item.customerName(),
                maskPhone(item.customerPhone()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                .replace('-', 'A')
                .replace('_', 'Z');
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank() || token.length() > 256) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO",
                    "El enlace no es valido.", Map.of("token", "Invalido."));
        }
        return token.trim();
    }

    private ZoneId resolveLocationZone(LocationRecord location) {
        String timezone = location.timezone();
        if (timezone == null || timezone.isBlank()) return ZoneId.of("America/Santiago");
        try { return ZoneId.of(timezone.trim()); }
        catch (DateTimeException e) { return ZoneId.of("America/Santiago"); }
    }

    private LocalTime max(LocalTime a, LocalTime b) { return a.isAfter(b) ? a : b; }
    private LocalTime min(LocalTime a, LocalTime b) { return a.isBefore(b) ? a : b; }
    private int normalizeLimit(Integer maxSlots) { return maxSlots == null ? 12 : Math.min(Math.max(maxSlots, 1), 40); }
}
