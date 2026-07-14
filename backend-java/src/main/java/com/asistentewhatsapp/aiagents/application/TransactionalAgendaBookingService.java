package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aesthetic.api.AestheticServiceResponse;
import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository;
import com.asistentewhatsapp.agenda.api.AgendaAvailabilityRequest;
import com.asistentewhatsapp.agenda.api.AgendaCancelRequest;
import com.asistentewhatsapp.agenda.api.AgendaCalendarItemResponse;
import com.asistentewhatsapp.agenda.api.AgendaRescheduleRequest;
import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.agenda.api.AgendaSlotResponse;
import com.asistentewhatsapp.agenda.api.CreateTemporaryAgendaBookingRequest;
import com.asistentewhatsapp.agenda.application.CompleteDigitalAgendaService;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.bookings.api.BookingConfirmationLinkResponse;
import com.asistentewhatsapp.bookings.api.BookingDetailResponse;
import com.asistentewhatsapp.bookings.api.CreateBookingConfirmationLinkRequest;
import com.asistentewhatsapp.bookings.application.BookingConfirmationService;
import com.asistentewhatsapp.bookings.application.BookingConfirmationProperties;
import com.asistentewhatsapp.bookings.application.BookingStateMachine;
import com.asistentewhatsapp.bookings.infrastructure.BookingActionLinkJdbcRepository;
import com.asistentewhatsapp.customerbookings.application.CustomerBookingService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.security.application.TokenHashService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalAgendaBookingService {

    private static final int MAX_SERVICE_SCAN = 200;
    private static final int MIN_EXPIRATION_MINUTES = 5;
    private static final int MAX_EXPIRATION_MINUTES = 1440;
    private static final java.util.Map<String, String> TEST_PHONE_MAP = java.util.Map.of(
            "224145803620505", "56950954580");
    private static final String DEMO_CUSTOMER_NAME = "Cliente WhatsApp";
    private static final String DEMO_CUSTOMER_PHONE = "+56900000000";
    private static final ZoneId DEFAULT_BUSINESS_ZONE = ZoneId.of("America/Santiago");
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b");
    private static final Pattern EXPLICIT_DATE_PATTERN = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
    private static final Pattern MONTH_NAME_DATE_PATTERN = Pattern.compile("\\b(\\d{1,2})(?:\\s+de)?\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)(?:\\s+de\\s+(\\d{2,4}))?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b(?:a\\s+las\\s+)?([01]?\\d|2[0-3])(?:(?::|\\.)([0-5]\\d)|\\s*(?:h|hrs?|horas?))?\\b", Pattern.CASE_INSENSITIVE);

    private final AestheticCenterJdbcRepository aestheticCenterJdbcRepository;
    private final BusinessLocationJdbcRepository businessLocationJdbcRepository;
    private final CompleteAgendaJdbcRepository completeAgendaJdbcRepository;
    private final CompleteDigitalAgendaService completeDigitalAgendaService;
    private final BookingConfirmationService bookingConfirmationService;
    private final BookingConfirmationProperties bookingConfirmationProperties;
    private final CustomerBookingService customerBookingService;
    private final BookingActionLinkJdbcRepository bookingActionLinkRepository;
    private final TokenHashService tokenHashService;
    private final String cancellationPublicBaseUrl;
    private final String reschedulePublicBaseUrl;
    private final String frontendPublicBaseUrl;

    public TransactionalAgendaBookingService(
            AestheticCenterJdbcRepository aestheticCenterJdbcRepository,
            BusinessLocationJdbcRepository businessLocationJdbcRepository,
            CompleteAgendaJdbcRepository completeAgendaJdbcRepository,
            CompleteDigitalAgendaService completeDigitalAgendaService,
            BookingConfirmationService bookingConfirmationService,
            BookingConfirmationProperties bookingConfirmationProperties,
            CustomerBookingService customerBookingService,
            BookingActionLinkJdbcRepository bookingActionLinkRepository,
            TokenHashService tokenHashService,
            @Value("${app.booking-cancellation.public-base-url}") String cancellationPublicBaseUrl,
            @Value("${app.booking-reschedule.public-base-url}") String reschedulePublicBaseUrl,
            @Value("${app.frontend.public-base-url:http://localhost:5173}") String frontendPublicBaseUrl) {
        this.aestheticCenterJdbcRepository = aestheticCenterJdbcRepository;
        this.businessLocationJdbcRepository = businessLocationJdbcRepository;
        this.completeAgendaJdbcRepository = completeAgendaJdbcRepository;
        this.completeDigitalAgendaService = completeDigitalAgendaService;
        this.bookingConfirmationService = bookingConfirmationService;
        this.bookingConfirmationProperties = bookingConfirmationProperties;
        this.customerBookingService = customerBookingService;
        this.bookingActionLinkRepository = bookingActionLinkRepository;
        this.tokenHashService = tokenHashService;
        this.cancellationPublicBaseUrl = cancellationPublicBaseUrl;
        this.reschedulePublicBaseUrl = reschedulePublicBaseUrl;
        this.frontendPublicBaseUrl = frontendPublicBaseUrl;
    }

    public String generateCancellationPublicLink(UUID businessId, UUID bookingId, int expirationMinutes) {
        String token = generatePublicToken();
        String publicUrl = buildPublicUrl(cancellationPublicBaseUrl, token);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(clampExpiration(expirationMinutes));
        bookingActionLinkRepository.insertCancellationLink(businessId, bookingId, tokenHashService.sha256(token),
                publicUrl, expiresAt, null, null, "WHATSAPP");
        return publicUrl;
    }

    public String generateReschedulePublicLink(UUID businessId, UUID bookingId,
            OffsetDateTime proposedStartsAt, OffsetDateTime proposedEndsAt,
            UUID locationId, UUID serviceId, UUID professionalId, UUID roomId, int expirationMinutes) {
        String token = generatePublicToken();
        String publicUrl = buildPublicUrl(reschedulePublicBaseUrl, token);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(clampExpiration(expirationMinutes));
        bookingActionLinkRepository.insertRescheduleLink(businessId, bookingId, tokenHashService.sha256(token),
                publicUrl, proposedStartsAt, proposedEndsAt, locationId, serviceId, professionalId,
                roomId, expiresAt, null, null, "WHATSAPP");
        return publicUrl;
    }

    private String generatePublicToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildPublicUrl(String baseUrl, String token) {
        return baseUrl.endsWith("/") ? baseUrl + token : baseUrl + "/" + token;
    }

    private int clampExpiration(int minutes) {
        return Math.max(MIN_EXPIRATION_MINUTES, Math.min(minutes, MAX_EXPIRATION_MINUTES));
    }

    public record BookingLinkResult(String url, boolean isKnownCustomer) {
    }

    public BookingLinkResult generateBookingLink(UUID businessId, String customerPhone) {
        return generateBookingLink(businessId, customerPhone, null, null);
    }

    public BookingLinkResult generateBookingLink(UUID businessId, String customerPhone, UUID conversationId, UUID customerId) {
        Optional<CompleteAgendaJdbcRepository.CustomerRecord> customerOpt =
                completeAgendaJdbcRepository.findCustomerByPhone(businessId, customerPhone);
        String correlationParams = buildCorrelationQuery(conversationId, customerId);
        if (customerOpt.isPresent()) {
            String phoneDigits = mappedPhoneDigits(customerPhone);
            String token = customerBookingService.generateToken(businessId, phoneDigits);
            String url = customerBookingService.buildBookingPublicUrl(token);
            if (!correlationParams.isEmpty()) {
                url += (url.contains("?") ? "&" : "?") + correlationParams;
            }
            return new BookingLinkResult(url, true);
        }
        String base = frontendPublicBaseUrl.endsWith("/") ? frontendPublicBaseUrl : frontendPublicBaseUrl + "/";
        String url = base + "reservar";
        if (!correlationParams.isEmpty()) {
            url += "?" + correlationParams;
        }
        return new BookingLinkResult(url, false);
    }

    private String buildCorrelationQuery(UUID conversationId, UUID customerId) {
        StringBuilder sb = new StringBuilder();
        if (conversationId != null) {
            sb.append("conversation_id=").append(conversationId);
        }
        if (customerId != null) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("customer_id=").append(customerId);
        }
        if (!sb.isEmpty()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("origin=whatsapp_ai");
        }
        return sb.toString();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<String> createTemporaryBookingLink(
            UUID businessId,
            UUID customerId,
            UUID conversationId,
            String customerName,
            String customerPhone,
            String message,
            String serviceText,
            String locationText,
            String dateText,
            String timeText,
            boolean sendWhatsApp,
            boolean dryRun,
            String traceId,
            UUID traceConversationId) {
        AiTraceLogger.info("TRANSACTIONAL_BOOKING_STARTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "serviceText=" + serviceText + " locationText=" + locationText
                        + " dateText=" + dateText + " timeText=" + timeText
                        + " dryRun=" + dryRun + " sendWhatsApp=" + sendWhatsApp);
        Optional<AestheticServiceResponse> service = resolveService(businessId, firstNonBlank(serviceText, message));
        Optional<BusinessLocationRecord> location = resolveLocation(businessId, firstNonBlank(locationText, message));
        Optional<LocalDate> date = resolveDate(dateText);
        Optional<LocalTime> time = resolveTime(timeText);

        AiTraceLogger.info("SERVICE_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "serviceText=" + serviceText
                        + " serviceId=" + service.map(AestheticServiceResponse::id).map(UUID::toString).orElse("")
                        + " serviceName=" + service.map(AestheticServiceResponse::name).orElse("")
                        + " durationMinutes=" + service.map(AestheticServiceResponse::durationMinutes).map(String::valueOf).orElse(""));
        AiTraceLogger.info("EFFECTIVE_LOCATION_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "locationText=" + locationText
                        + " locationId=" + location.map(BusinessLocationRecord::id).map(UUID::toString).orElse("")
                        + " locationName=" + location.map(BusinessLocationRecord::name).orElse(""));

        if (service.isEmpty() || location.isEmpty() || date.isEmpty() || time.isEmpty()) {
            AiTraceLogger.warn("BOOKING_REQUIRED_DATA_CHECK", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "hasService=" + service.isPresent()
                            + " hasLocation=" + location.isPresent()
                            + " hasDate=" + date.isPresent()
                            + " hasTime=" + time.isPresent()
                            + " nextAction=RETURN_TO_AGENT");
            return Optional.empty();
        }

        AuthenticatedUser systemUser = systemUser(businessId, location.get());
        AgendaAvailabilityResponse availability;
        try {
            AiTraceLogger.info("SERVICE_LOCATION_VALIDATED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "serviceId=" + service.get().id() + " locationId=" + location.get().id() + " available=pending reason=availability_check");
            AiTraceLogger.info("AVAILABILITY_CHECK_STARTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "serviceId=" + service.get().id()
                            + " locationId=" + location.get().id()
                            + " date=" + date.get()
                            + " time=" + time.get()
                            + " durationMinutes=" + service.get().durationMinutes());
            availability = completeDigitalAgendaService.availability(
                    systemUser,
                    new AgendaAvailabilityRequest(location.get().id(), service.get().id(), null, null, date.get(), null, 40));
        } catch (ApiException exception) {
            String functionalMessage = serviceLocationUnavailableResponse(service.get(), location.get(), exception);
            AiTraceLogger.warn("SERVICE_NOT_AVAILABLE_IN_LOCATION", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "serviceId=" + service.get().id() + " locationId=" + location.get().id()
                            + " functionalMessage=" + functionalMessage
                            + " technicalError=" + exception.getMessage());
            return Optional.of(functionalMessage);
        }

        Optional<AgendaSlotResponse> slot = resolveExactAvailableSlot(availability, time.get(), traceId, traceConversationId);
        long availableSlots = availability.slots().stream().filter(AgendaSlotResponse::available).count();
        AiTraceLogger.info("AVAILABILITY_CHECK_RESULT", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "available=" + slot.isPresent()
                        + " requestedTime=" + time.get()
                        + " totalSlots=" + availability.slots().size()
                        + " availableSlots=" + availableSlots
                        + " exactSlotReason=" + (slot.isPresent() ? "EXACT_SLOT_AVAILABLE" : exactSlotRejectionReason(availability, time.get())));

        if (slot.isEmpty()) {
            String response = noAvailabilityResponse(service.get(), location.get(), dateText, timeText, availability, time.get(), traceId, traceConversationId);
            return Optional.of(response);
        }

        if (dryRun) {
            AiTraceLogger.info("TEMPORARY_BOOKING_DRY_RUN", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "serviceId=" + service.get().id()
                            + " locationId=" + location.get().id()
                            + " date=" + date.get()
                            + " time=" + time.get()
                            + " nextAction=SEND_REAL_MESSAGE_TO_CREATE_BOOKING");
            return Optional.of(dryRunAvailabilityResponse(service.get(), location.get(), dateText, timeText, traceId, traceConversationId));
        }

        try {
            AiTraceLogger.info("TEMPORARY_BOOKING_CREATE_STARTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "serviceId=" + service.get().id()
                            + " locationId=" + location.get().id()
                            + " date=" + date.get()
                            + " time=" + time.get()
                            + " durationMinutes=" + service.get().durationMinutes()
                            + " expirationMinutes=" + expirationMinutes());
            BookingDetailResponse booking = completeDigitalAgendaService.createTemporaryBooking(
                    systemUser,
                    new CreateTemporaryAgendaBookingRequest(
                            location.get().id(),
                            service.get().id(),
                            slot.get().professionalId(),
                            slot.get().roomId(),
                            slot.get().startsAt(),
                            normalizedCustomerName(customerName),
                            normalizedCustomerPhone(customerPhone),
                            null,
                            customerId,
                            conversationId,
                            null,
                            "Reserva temporal creada automaticamente por IA de agenda WhatsApp.",
                            expirationMinutes(),
                            false,
                            false,
                            null));
            AiTraceLogger.info("TEMPORARY_BOOKING_CREATED", traceId, traceConversationId, booking.id(), "TransactionalAgendaBookingService",
                    "bookingId=" + booking.id()
                            + " status=" + booking.status()
                            + " serviceId=" + service.get().id()
                            + " locationId=" + location.get().id()
                            + " date=" + date.get()
                            + " time=" + time.get()
                            + " expirationMinutes=" + expirationMinutes());
            BookingConfirmationLinkResponse link = bookingConfirmationService.createConfirmationLink(
                    systemUser,
                    booking.id(),
                    new CreateBookingConfirmationLinkRequest(expirationMinutes(), sendWhatsApp));
            String token = extractToken(link.confirmationUrl());
            AiTraceLogger.info("CONFIRMATION_LINK_CREATED", traceId, traceConversationId, booking.id(), "TransactionalAgendaBookingService",
                    "bookingId=" + booking.id()
                            + " tokenMasked=" + AiTraceLogger.maskToken(token)
                            + " expirationMinutes=" + expirationMinutes()
                            + " urlCreated=true sendWhatsApp=" + sendWhatsApp);
            return Optional.of(successResponse(service.get(), location.get(), dateText, timeText, link, traceId, traceConversationId));
        } catch (RuntimeException exception) {
            AiTraceLogger.error("FLOW_ERROR", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "errorType=" + exception.getClass().getSimpleName()
                            + " functionalMessage=No fue posible crear la reserva temporal en este momento. Puedo revisar otro horario o derivarte con una persona del equipo.",
                    exception);
            return Optional.of("No fue posible crear la reserva temporal en este momento. Puedo revisar otro horario o derivarte con una persona del equipo.");
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String handleCancelBookingFromWhatsApp(
            UUID businessId,
            UUID customerId,
            UUID conversationId,
            String customerPhone,
            String message,
            Map<String, String> entities,
            String traceId,
            UUID traceConversationId) {
        try {
            String normalized = normalize(message);
            String pendingAction = value(entities, "accion_pendiente");
            if ("CANCEL_SELECT".equals(pendingAction)) {
                Optional<AgendaCalendarItemResponse> selected = selectPendingOption(businessId, customerId, conversationId, customerPhone, entities, normalized);
                if (selected.isEmpty()) {
                    return "No pude identificar la opcion. Responde con el numero de la reserva que deseas cancelar.";
                }
                clearPendingBookingAction(entities);
                return buildCancellationLinkResponse(businessId, customerPhone, selected.get());
            }

            if (isAffirmative(normalized) && entities.containsKey("booking_id_pendiente")) {
                Optional<AgendaCalendarItemResponse> pending = findPendingBooking(businessId, customerId, conversationId, customerPhone, entities);
                if (pending.isPresent()) {
                    clearPendingBookingAction(entities);
                    return buildCancellationLinkResponse(businessId, customerPhone, pending.get());
                }
            }

            List<AgendaCalendarItemResponse> candidates = findCandidateBookings(businessId, customerId, conversationId, customerPhone, message, entities, traceId, traceConversationId);
            if (candidates.isEmpty()) {
                clearPendingBookingAction(entities);
                return WhatsAppMessageFormatter.noActiveBookingsFound();
            }
            if (candidates.size() > 1) {
                clearPendingBookingAction(entities);
                String phoneDigits = mappedPhoneDigits(customerPhone);
                String token = customerBookingService.generateToken(businessId, phoneDigits);
                String url = customerBookingService.buildPublicUrl(token);
                List<WhatsAppMessageFormatter.CancellationCandidate> cancellationCandidates = candidates.stream()
                        .map(c -> new WhatsAppMessageFormatter.CancellationCandidate(
                                bookingTitle(c), bookingDate(c), bookingTime(c), locationName(c), url))
                        .toList();
                return WhatsAppMessageFormatter.multipleCancellationCandidatesSingleLink(cancellationCandidates, url, 60);
            }
            AgendaCalendarItemResponse candidate = candidates.getFirst();
            return buildCancellationLinkResponse(businessId, customerPhone, candidate);
        } catch (RuntimeException exception) {
            AiTraceLogger.error("WHATSAPP_CANCEL_FLOW_ERROR", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "functionalMessage=No pude completar la cancelacion en este momento.", exception);
            return "No pude completar la cancelacion en este momento. Puedo intentar nuevamente o derivarte con una persona del equipo.";
        }
    }

    private String buildCancellationLinkResponse(UUID businessId, String customerPhone, AgendaCalendarItemResponse booking) {
        try {
            String phoneDigits = mappedPhoneDigits(customerPhone);
            String token = customerBookingService.generateToken(businessId, phoneDigits);
            String url = customerBookingService.buildPublicUrl(token);
            return WhatsAppMessageFormatter.cancellationLinkGenerated(
                    bookingTitle(booking), bookingDate(booking), bookingTime(booking),
                    locationName(booking), url, 60);
        } catch (Exception e) {
            return "No pude generar el enlace de cancelacion. Puedo intentar nuevamente o derivarte con una persona del equipo.";
        }
    }

    private String buildRescheduleLinkResponse(UUID businessId, String customerPhone, AgendaCalendarItemResponse booking,
            LocalDate targetDate, LocalTime targetTime, Map<String, String> entities,
            String traceId, UUID traceConversationId) {
        try {
            String phoneDigits = mappedPhoneDigits(customerPhone);
            String token = customerBookingService.generateToken(businessId, phoneDigits);
            String url = customerBookingService.buildPublicUrl(token);
            return WhatsAppMessageFormatter.rescheduleLinkGenerated(
                    bookingTitle(booking), bookingDate(booking), bookingTime(booking),
                    locationName(booking), url, 60);
        } catch (Exception e) {
            return "No pude generar el enlace de reprogramacion. Puedo intentar nuevamente o derivarte con una persona del equipo.";
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String handleRescheduleBookingFromWhatsApp(
            UUID businessId,
            UUID customerId,
            UUID conversationId,
            String customerPhone,
            String message,
            Map<String, String> entities,
            String traceId,
            UUID traceConversationId) {
        try {
            String normalized = normalize(message);
            String pendingAction = value(entities, "accion_pendiente");
            Optional<AgendaCalendarItemResponse> selected = Optional.empty();

            if ("RESCHEDULE_SELECT".equals(pendingAction)) {
                selected = selectPendingOption(businessId, customerId, conversationId, customerPhone, entities, normalized);
                if (selected.isEmpty()) {
                    return "No pude identificar la opcion. Responde con el numero de la reserva que deseas reprogramar.";
                }
                entities.put("accion_pendiente", "RESCHEDULE_WAIT_NEW_DATE_TIME");
                entities.put("booking_id_pendiente", selected.get().bookingId().toString());
                return "Perfecto. ¿Para que dia y horario quieres reprogramar la reserva de " + bookingTitle(selected.get()) + "?";
            }

            if ("RESCHEDULE_WAIT_NEW_DATE_TIME".equals(pendingAction)) {
                selected = findPendingBooking(businessId, customerId, conversationId, customerPhone, entities);
                if (selected.isEmpty()) {
                    clearPendingBookingAction(entities);
                    return "No encontre la reserva que estabamos reprogramando. ¿Me puedes indicar servicio, fecha u hora de la cita original?";
                }
            }

            if ("RESCHEDULE_SELECT_ALTERNATIVE".equals(pendingAction)) {
                selected = findPendingBooking(businessId, customerId, conversationId, customerPhone, entities);
                if (selected.isEmpty()) {
                    clearPendingBookingAction(entities);
                    return "No encontre la reserva que estabamos reprogramando. ¿Me puedes indicar servicio, fecha u hora de la cita original?";
                }
                Optional<LocalDate> alternativeDate = resolveStoredAlternativeDate(entities);
                Optional<LocalTime> alternativeTime = resolveStoredAlternativeTime(message, entities, normalized);
                if (alternativeDate.isEmpty() || alternativeTime.isEmpty()) {
                    return "No pude identificar la opcion. Responde con el numero de una alternativa o con una hora de la lista.";
                }
                String response = buildRescheduleLinkResponse(businessId, customerPhone, selected.get(), alternativeDate.get(), alternativeTime.get(), entities, traceId, traceConversationId);
                clearPendingBookingAction(entities);
                return response;
            }

            if (selected.isEmpty()) {
                List<AgendaCalendarItemResponse> candidates = findCandidateBookings(businessId, customerId, conversationId, customerPhone, message, entities, traceId, traceConversationId);
                if (candidates.isEmpty()) {
                    clearPendingBookingAction(entities);
                    return "No encontre una reserva activa asociada a este numero. ¿Me puedes indicar la fecha, hora o servicio de la cita que quieres reprogramar?";
                }
                if (candidates.size() > 1) {
                    String phoneDigits = mappedPhoneDigits(customerPhone);
                    String token = customerBookingService.generateToken(businessId, phoneDigits);
                    String url = customerBookingService.buildPublicUrl(token);
                    List<WhatsAppMessageFormatter.RescheduleCandidate> rescheduleCandidates = candidates.stream()
                            .map(c -> new WhatsAppMessageFormatter.RescheduleCandidate(
                                    bookingTitle(c), bookingDate(c), bookingTime(c), locationName(c), url))
                            .toList();
                    return WhatsAppMessageFormatter.multipleRescheduleCandidatesSingleLink(rescheduleCandidates, url, 60);
                }
                selected = Optional.of(candidates.getFirst());
            }

            Optional<LocalDate> targetDate = resolveTargetDate(message, entities);
            Optional<LocalTime> targetTime = resolveTargetTime(message, entities);
            if (targetDate.isEmpty() || targetTime.isEmpty()) {
                entities.put("accion_pendiente", "RESCHEDULE_WAIT_NEW_DATE_TIME");
                entities.put("booking_id_pendiente", selected.get().bookingId().toString());
                if (targetDate.isEmpty() && targetTime.isEmpty()) {
                    return "Encontre tu reserva de " + bookingTitle(selected.get()) + " para " + bookingDateTime(selected.get())
                            + ". ¿Para que dia y horario te gustaria reprogramarla?";
                }
                if (targetDate.isEmpty()) {
                    return "Perfecto. ¿Para que dia quieres mover tu reserva?";
                }
                return "Perfecto. ¿A que hora prefieres asistir ese dia?";
            }

            String response = buildRescheduleLinkResponse(businessId, customerPhone, selected.get(), targetDate.get(), targetTime.get(), entities, traceId, traceConversationId);
            if (!"RESCHEDULE_SELECT_ALTERNATIVE".equals(value(entities, "accion_pendiente"))) {
                clearPendingBookingAction(entities);
            }
            return response;
        } catch (RuntimeException exception) {
            AiTraceLogger.error("WHATSAPP_RESCHEDULE_FLOW_ERROR", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "functionalMessage=No pude completar la reprogramacion en este momento.", exception);
            return "No pude completar la reprogramacion en este momento. Puedo buscarte otro horario o derivarte con una persona del equipo.";
        }
    }

    private String cancelBookingCandidate(UUID businessId, AgendaCalendarItemResponse candidate, String reason, String traceId, UUID traceConversationId) {
        if (isNotActionableStatus(candidate.status())) {
            return "Esa reserva no permite cancelacion porque su estado actual es " + candidate.status() + ".";
        }
        completeDigitalAgendaService.cancelByCustomer(businessId, candidate.bookingId(), reason);
        AiTraceLogger.info("WHATSAPP_BOOKING_CANCELLED", traceId, traceConversationId, candidate.bookingId(), "TransactionalAgendaBookingService",
                "bookingId=" + candidate.bookingId() + " source=WHATSAPP actor=CUSTOMER targetStatus=CANCELADA_POR_CLIENTE");
        return WhatsAppMessageFormatter.bookingCancelledSuccess(bookingTitle(candidate), bookingDate(candidate), bookingTime(candidate), locationName(candidate));
    }

    private String rescheduleBookingCandidate(UUID businessId, AgendaCalendarItemResponse candidate, LocalDate targetDate, LocalTime targetTime,
            Map<String, String> entities,
            String traceId, UUID traceConversationId) {
        if (isNotActionableStatus(candidate.status())) {
            return "Esa reserva no permite reprogramacion porque su estado actual es " + candidate.status() + ".";
        }
        if (candidate.locationId() == null || candidate.serviceId() == null) {
            return "No tengo suficientes datos tecnicos de la reserva para reprogramarla automaticamente. Te derivare con una persona del equipo.";
        }
        BusinessLocationRecord location = businessLocationFor(businessId, candidate);
        AuthenticatedUser systemUser = systemUser(businessId, location);
        AgendaAvailabilityResponse availability = completeDigitalAgendaService.availability(
                systemUser,
                new AgendaAvailabilityRequest(candidate.locationId(), candidate.serviceId(), candidate.professionalId(), candidate.roomId(), targetDate, null, 40));
        Optional<AgendaSlotResponse> slot = resolveExactAvailableSlot(availability, targetTime, traceId, traceConversationId);
        if (slot.isEmpty()) {
            List<String> alternatives = nearestAvailableSlots(availability, targetTime);
            if (alternatives.isEmpty()) {
                return "No encontre disponibilidad para " + formatDate(targetDate) + " a las " + formatTime(targetTime)
                        + ". Puedo revisar otro dia u horario.";
            }
            rememberRescheduleAlternatives(entities, candidate, targetDate, targetTime, alternatives);
            return "No encontre disponibilidad exacta para " + formatDate(targetDate) + " a las " + formatTime(targetTime)
                    + ". Tengo estas opciones disponibles:\n\n" + numberedTimes(alternatives) + "\n¿Cual prefieres?";
        }
        AgendaSlotResponse selectedSlot = slot.get();
        completeDigitalAgendaService.reschedule(
                systemUser,
                candidate.bookingId(),
                new AgendaRescheduleRequest(
                        selectedSlot.locationId(),
                        selectedSlot.serviceId(),
                        selectedSlot.professionalId(),
                        selectedSlot.roomId(),
                        selectedSlot.startsAt(),
                        "Reprogramacion solicitada por el cliente via WhatsApp."));
        AiTraceLogger.info("WHATSAPP_BOOKING_RESCHEDULED", traceId, traceConversationId, candidate.bookingId(), "TransactionalAgendaBookingService",
                "bookingId=" + candidate.bookingId() + " newStartsAt=" + selectedSlot.startsAt() + " source=WHATSAPP actor=CUSTOMER");
        return "Listo, tu reserva de " + bookingTitle(candidate) + " fue reprogramada para "
                + formatDate(targetDate) + " a las " + formatTime(targetTime) + ". Te esperamos.";
    }

    private List<AgendaCalendarItemResponse> findCandidateBookings(UUID businessId, UUID customerId, UUID conversationId, String customerPhone,
            String message, Map<String, String> entities, String traceId, UUID traceConversationId) {
        Optional<AestheticServiceResponse> service = resolveService(businessId, firstNonBlank(value(entities, "servicio_o_producto"), message));
        ResolvedLocation location = resolveEffectiveLocation(
                businessId,
                message,
                firstNonBlank(value(entities, "sede"), value(entities, "sede_id")),
                null,
                null,
                traceId,
                traceConversationId);
        ZoneId zone = location.location() == null ? DEFAULT_BUSINESS_ZONE : resolveBusinessLocationZone(location.location());
        Optional<LocalDate> date = resolveSearchDate(message, entities, zone);
        Optional<LocalTime> time = resolveSearchTime(message, entities);
        OffsetDateTime from = date.map(value -> value.atStartOfDay(zone).toOffsetDateTime()).orElse(null);
        OffsetDateTime to = from == null ? null : from.plusDays(1);
        UUID locationId = location.location() == null ? null : location.location().id();
        UUID serviceId = service.map(AestheticServiceResponse::id).orElse(null);
        String normalizedPhone = normalizedSearchPhone(firstNonBlank(value(entities, "telefono"), customerPhone));
        normalizedPhone = TEST_PHONE_MAP.getOrDefault(normalizedPhone, normalizedPhone);
        String customerName = firstNonBlank(value(entities, "cliente"), "");
        OffsetDateTime broadFrom = from == null ? null : from.minusHours(12);
        OffsetDateTime broadTo = to == null ? null : to.plusHours(12);

        AiTraceLogger.info("WHATSAPP_BOOKING_CANDIDATE_SEARCH_STARTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "fechaRelativaOriginal=" + firstNonBlank(value(entities, "fecha_relativa"), "")
                        + " fechaEntidad=" + firstNonBlank(value(entities, "fecha"), "")
                        + " fechaLocalResuelta=" + date.map(LocalDate::toString).orElse("")
                        + " dayStart=" + (from == null ? "" : from)
                        + " dayEnd=" + (to == null ? "" : to)
                        + " horaSolicitada=" + time.map(this::formatTime).orElse("")
                        + " timezone=" + zone
                        + " serviceId=" + serviceId
                        + " locationId=" + locationId
                        + " phoneLastDigits=" + lastDigits(normalizedPhone, 4)
                        + " customerName=" + customerName);

        List<AgendaCalendarItemResponse> merged = new ArrayList<>();
        appendCandidates(merged, searchCandidateLayer("STRICT_CONTEXT", businessId, customerId, conversationId, normalizedPhone,
                from, to, locationId, serviceId, time.orElse(null), zone, traceId, traceConversationId));

        if (merged.isEmpty() && (locationId != null || serviceId != null)) {
            appendCandidates(merged, searchCandidateLayer("CONTEXT_WITHOUT_WEAK_FILTERS", businessId, customerId, conversationId, normalizedPhone,
                    from, to, null, null, time.orElse(null), zone, traceId, traceConversationId));
        }

        if (merged.isEmpty() && normalizedPhone != null) {
            appendCandidates(merged, searchCandidateLayer("PHONE_ONLY_DAY_TIME", businessId, null, null, normalizedPhone,
                    from, to, null, null, time.orElse(null), zone, traceId, traceConversationId));
        }

        if (merged.isEmpty() && date.isPresent()) {
            appendCandidates(merged, searchOperationalLayer("OPERATIONAL_PHONE_OR_NAME_WITH_SERVICE_LOCATION", businessId,
                    broadFrom, broadTo, locationId, serviceId, normalizedPhone, customerName, date.orElse(null), time.orElse(null), zone, traceId, traceConversationId));
        }

        if (merged.isEmpty() && date.isPresent() && (locationId != null || serviceId != null)) {
            appendCandidates(merged, searchOperationalLayer("OPERATIONAL_UNLINKED_SERVICE_LOCATION", businessId,
                    broadFrom, broadTo, locationId, serviceId, null, null, date.orElse(null), time.orElse(null), zone, traceId, traceConversationId));
        }

        if (merged.isEmpty() && date.isPresent() && time.isPresent()) {
            appendCandidates(merged, searchCandidateLayer("DAY_TIME_WITHOUT_CUSTOMER_STRICT", businessId, customerId, conversationId, normalizedPhone,
                    broadFrom, broadTo, null, null, null, zone, traceId, traceConversationId));
        }

        if (merged.isEmpty() && date.isPresent() && time.isPresent()) {
            appendCandidates(merged, searchOperationalLayer("OPERATIONAL_UNLINKED_DAY_TIME", businessId,
                    broadFrom, broadTo, null, null, normalizedPhone, customerName, date.orElse(null), time.orElse(null), zone, traceId, traceConversationId));
        }

        if (merged.isEmpty()) {
            appendCandidates(merged, searchCandidateLayer("ACTIVE_CONTEXT_FALLBACK", businessId, customerId, conversationId, normalizedPhone,
                    null, null, null, null, null, zone, traceId, traceConversationId));
        }

        if (merged.isEmpty()) {
            appendCandidates(merged, searchOperationalLayer("ACTIVE_OPERATIONAL_FALLBACK", businessId,
                    null, null, null, null, normalizedPhone, customerName, null, null, zone, traceId, traceConversationId));
        }

        List<AgendaCalendarItemResponse> ranked = merged.stream()
                .sorted(Comparator.comparingInt(candidate -> -candidateScore(candidate, date.orElse(null), time.orElse(null), serviceId, locationId, zone)))
                .limit(10)
                .toList();
        AiTraceLogger.info("WHATSAPP_BOOKING_CANDIDATE_SEARCH_FINISHED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "cantidadReservasCandidatas=" + ranked.size()
                        + " bookingIds=" + ranked.stream().map(candidate -> candidate.bookingId().toString()).toList());
        return ranked;
    }

    private List<AgendaCalendarItemResponse> searchCandidateLayer(String layer, UUID businessId, UUID customerId, UUID conversationId, String normalizedPhone,
            OffsetDateTime from, OffsetDateTime to, UUID locationId, UUID serviceId, LocalTime requestedTime, ZoneId zone,
            String traceId, UUID traceConversationId) {
        List<AgendaCalendarItemResponse> raw = completeAgendaJdbcRepository.findActiveBookingsForCustomerContext(
                businessId,
                customerId,
                conversationId,
                normalizedPhone,
                from,
                to,
                locationId,
                serviceId);
        List<AgendaCalendarItemResponse> filtered = requestedTime == null
                ? raw
                : raw.stream()
                        .filter(candidate -> candidateLocalTime(candidate, zone).equals(requestedTime))
                        .toList();
        AiTraceLogger.info("WHATSAPP_BOOKING_CANDIDATE_SEARCH_LAYER", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "layer=" + layer
                        + " from=" + (from == null ? "" : from)
                        + " to=" + (to == null ? "" : to)
                        + " locationId=" + locationId
                        + " serviceId=" + serviceId
                        + " requestedTime=" + (requestedTime == null ? "" : formatTime(requestedTime))
                        + " raw=" + raw.size()
                        + " filtered=" + filtered.size());
        return filtered;
    }


    private List<AgendaCalendarItemResponse> searchOperationalLayer(String layer, UUID businessId,
            OffsetDateTime from, OffsetDateTime to, UUID locationId, UUID serviceId, String normalizedPhone, String customerName,
            LocalDate requestedDate, LocalTime requestedTime, ZoneId zone, String traceId, UUID traceConversationId) {
        List<AgendaCalendarItemResponse> raw = completeAgendaJdbcRepository.findActiveBookingsForOperationalLookup(
                businessId,
                from,
                to,
                locationId,
                serviceId,
                normalizedPhone,
                customerName);
        List<AgendaCalendarItemResponse> filtered = raw.stream()
                .filter(candidate -> requestedDate == null || candidateLocalDate(candidate, zone).equals(requestedDate))
                .filter(candidate -> requestedTime == null || candidateLocalTime(candidate, zone).equals(requestedTime))
                .toList();
        AiTraceLogger.info("WHATSAPP_BOOKING_CANDIDATE_SEARCH_OPERATIONAL_LAYER", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "layer=" + layer
                        + " from=" + (from == null ? "" : from)
                        + " to=" + (to == null ? "" : to)
                        + " locationId=" + locationId
                        + " serviceId=" + serviceId
                        + " phoneLastDigits=" + lastDigits(normalizedPhone, 4)
                        + " customerName=" + firstNonBlank(customerName, "")
                        + " requestedDate=" + (requestedDate == null ? "" : requestedDate)
                        + " requestedTime=" + (requestedTime == null ? "" : formatTime(requestedTime))
                        + " raw=" + raw.size()
                        + " filtered=" + filtered.size());
        return filtered;
    }

    private void appendCandidates(List<AgendaCalendarItemResponse> target, List<AgendaCalendarItemResponse> candidates) {
        Map<UUID, AgendaCalendarItemResponse> unique = new LinkedHashMap<>();
        for (AgendaCalendarItemResponse item : target) {
            unique.put(item.bookingId(), item);
        }
        for (AgendaCalendarItemResponse item : candidates) {
            unique.putIfAbsent(item.bookingId(), item);
        }
        target.clear();
        target.addAll(unique.values());
    }

    private int candidateScore(AgendaCalendarItemResponse candidate, LocalDate requestedDate, LocalTime requestedTime,
            UUID serviceId, UUID locationId, ZoneId zone) {
        int score = 0;
        if (requestedDate != null && candidate.startsAt().atZoneSameInstant(zone).toLocalDate().equals(requestedDate)) {
            score += 1000;
        }
        if (requestedTime != null && candidateLocalTime(candidate, zone).equals(requestedTime)) {
            score += 800;
        }
        if (serviceId != null && serviceId.equals(candidate.serviceId())) {
            score += 150;
        }
        if (locationId != null && locationId.equals(candidate.locationId())) {
            score += 150;
        }
        return score;
    }


    private LocalDate candidateLocalDate(AgendaCalendarItemResponse candidate, ZoneId zone) {
        return candidate.startsAt().atZoneSameInstant(zone == null ? DEFAULT_BUSINESS_ZONE : zone).toLocalDate();
    }

    private LocalTime candidateLocalTime(AgendaCalendarItemResponse candidate, ZoneId zone) {
        return candidate.startsAt().atZoneSameInstant(zone == null ? DEFAULT_BUSINESS_ZONE : zone).toLocalTime();
    }

    private Optional<LocalDate> resolveSearchDate(String message, Map<String, String> entities, ZoneId zone) {
        Optional<LocalDate> fromMessage = resolveDate(message, zone);
        if (fromMessage.isPresent()) {
            return fromMessage;
        }
        Optional<LocalDate> fromRelative = resolveDate(value(entities, "fecha_relativa"), zone);
        if (fromRelative.isPresent()) {
            return fromRelative;
        }
        return resolveDate(value(entities, "fecha"), zone);
    }

    private Optional<LocalTime> resolveSearchTime(String message, Map<String, String> entities) {
        Optional<LocalTime> fromEntity = resolveTime(value(entities, "hora"));
        if (fromEntity.isPresent()) {
            return fromEntity;
        }
        return resolveTime(message);
    }

    private Optional<AgendaCalendarItemResponse> findPendingBooking(UUID businessId, UUID customerId, UUID conversationId, String customerPhone,
            Map<String, String> entities) {
        UUID pendingId = parseUuid(value(entities, "booking_id_pendiente"));
        if (pendingId == null) {
            return Optional.empty();
        }
        return completeAgendaJdbcRepository.findActiveBookingsForCustomerContext(
                        businessId,
                        customerId,
                        conversationId,
                        normalizedSearchPhone(customerPhone),
                        null,
                        null,
                        null,
                        null)
                .stream()
                .filter(candidate -> candidate.bookingId().equals(pendingId))
                .findFirst();
    }

    private Optional<AgendaCalendarItemResponse> selectPendingOption(UUID businessId, UUID customerId, UUID conversationId, String customerPhone,
            Map<String, String> entities, String normalizedMessage) {
        Integer option = parseOption(normalizedMessage);
        if (option == null) {
            return Optional.empty();
        }
        UUID selectedId = parseUuid(value(entities, "reserva_opcion_" + option + "_id"));
        if (selectedId == null) {
            return Optional.empty();
        }
        return completeAgendaJdbcRepository.findActiveBookingsForCustomerContext(
                        businessId,
                        customerId,
                        conversationId,
                        normalizedSearchPhone(customerPhone),
                        null,
                        null,
                        null,
                        null)
                .stream()
                .filter(candidate -> candidate.bookingId().equals(selectedId))
                .findFirst();
    }

    private Optional<LocalDate> resolveTargetDate(String message, Map<String, String> entities) {
        Optional<LocalDate> fromTargetSegment = resolveDate(targetSegment(message), DEFAULT_BUSINESS_ZONE);
        return fromTargetSegment.isPresent() ? fromTargetSegment : resolveDate(message, DEFAULT_BUSINESS_ZONE);
    }

    private Optional<LocalTime> resolveTargetTime(String message, Map<String, String> entities) {
        Optional<LocalTime> fromTargetSegment = resolveTime(targetSegment(message));
        return fromTargetSegment.isPresent() ? fromTargetSegment : resolveTime(message);
    }

    private String targetSegment(String message) {
        String normalized = normalize(message);
        int para = normalized.lastIndexOf(" para ");
        if (para >= 0 && para + 6 < normalized.length()) {
            return normalized.substring(para + 6);
        }
        int mover = normalized.lastIndexOf(" mover ");
        if (mover >= 0 && mover + 7 < normalized.length()) {
            return normalized.substring(mover + 7);
        }
        int cambiar = normalized.lastIndexOf(" cambiar ");
        if (cambiar >= 0 && cambiar + 9 < normalized.length()) {
            return normalized.substring(cambiar + 9);
        }
        return "";
    }

    private boolean isNotActionableStatus(String status) {
        return BookingStateMachine.isClosed(status);
    }

    private BusinessLocationRecord businessLocationFor(UUID businessId, AgendaCalendarItemResponse candidate) {
        if (candidate.locationId() != null) {
            return businessLocationJdbcRepository.findActiveById(businessId, candidate.locationId());
        }
        return resolveLocation(businessId, candidate.locationName())
                .orElseGet(() -> businessLocationJdbcRepository.findSingleActive(businessId)
                        .orElseThrow(() -> new IllegalStateException("No existe sede activa para operar la reserva.")));
    }

    private ZoneId resolveBusinessLocationZone(BusinessLocationRecord location) {
        if (location == null || location.timezone() == null || location.timezone().isBlank()) {
            return DEFAULT_BUSINESS_ZONE;
        }
        try {
            return ZoneId.of(location.timezone().trim());
        } catch (RuntimeException exception) {
            return DEFAULT_BUSINESS_ZONE;
        }
    }

    private String bookingTitle(AgendaCalendarItemResponse candidate) {
        return firstNonBlank(firstNonBlank(candidate.serviceName(), candidate.subject()), "la reserva");
    }

    private String bookingDateTime(AgendaCalendarItemResponse candidate) {
        ZoneId zone = DEFAULT_BUSINESS_ZONE;
        return formatDate(candidateLocalDate(candidate, zone)) + " a las " + formatTime(candidateLocalTime(candidate, zone));
    }

    private String locationSuffix(AgendaCalendarItemResponse candidate) {
        return candidate.locationName() == null || candidate.locationName().isBlank() ? "" : " en " + candidate.locationName();
    }

    private String bookingDate(AgendaCalendarItemResponse candidate) {
        ZoneId zone = DEFAULT_BUSINESS_ZONE;
        return formatDate(candidateLocalDate(candidate, zone));
    }

    private String bookingTime(AgendaCalendarItemResponse candidate) {
        ZoneId zone = DEFAULT_BUSINESS_ZONE;
        return formatTime(candidateLocalTime(candidate, zone));
    }

    private String locationName(AgendaCalendarItemResponse candidate) {
        return candidate.locationName() != null && !candidate.locationName().isBlank() ? candidate.locationName() : null;
    }

    private String bookingOptionsMessage(String title, List<AgendaCalendarItemResponse> candidates) {
        StringBuilder builder = new StringBuilder(title).append("\n\n");
        int index = 1;
        for (AgendaCalendarItemResponse candidate : candidates.stream().limit(3).toList()) {
            builder.append(index++).append(". ")
                    .append(bookingTitle(candidate)).append(", ")
                    .append(bookingDateTime(candidate))
                    .append(locationSuffix(candidate))
                    .append(".\n");
        }
        return builder.append("\nResponde con el numero de la opcion.").toString();
    }

    private void putCandidateOptions(Map<String, String> entities, List<AgendaCalendarItemResponse> candidates) {
        for (int index = 1; index <= 3; index++) {
            entities.remove("reserva_opcion_" + index + "_id");
        }
        for (int index = 0; index < Math.min(3, candidates.size()); index++) {
            entities.put("reserva_opcion_" + (index + 1) + "_id", candidates.get(index).bookingId().toString());
        }
    }

    private void clearPendingBookingAction(Map<String, String> entities) {
        entities.remove("accion_pendiente");
        entities.remove("booking_id_pendiente");
        entities.remove("reprogramacion_intencion_original");
        entities.remove("reprogramacion_servicio");
        entities.remove("reprogramacion_sede");
        entities.remove("reprogramacion_fecha");
        entities.remove("reprogramacion_hora_solicitada");
        for (int index = 1; index <= 3; index++) {
            entities.remove("reserva_opcion_" + index + "_id");
            entities.remove("reprogramacion_opcion_" + index + "_hora");
        }
    }

    private void rememberRescheduleAlternatives(
            Map<String, String> entities,
            AgendaCalendarItemResponse candidate,
            LocalDate targetDate,
            LocalTime requestedTime,
            List<String> alternatives) {
        entities.put("accion_pendiente", "RESCHEDULE_SELECT_ALTERNATIVE");
        entities.put("booking_id_pendiente", candidate.bookingId().toString());
        entities.put("reprogramacion_intencion_original", "reprogramar_reserva");
        entities.put("reprogramacion_servicio", bookingTitle(candidate));
        entities.put("reprogramacion_sede", candidate.locationName() == null ? "" : candidate.locationName());
        entities.put("reprogramacion_fecha", targetDate.toString());
        entities.put("reprogramacion_hora_solicitada", formatTime(requestedTime));
        for (int index = 1; index <= 3; index++) {
            entities.remove("reprogramacion_opcion_" + index + "_hora");
        }
        List<String> distinctAlternatives = alternatives.stream().distinct().limit(3).toList();
        for (int index = 0; index < distinctAlternatives.size(); index++) {
            entities.put("reprogramacion_opcion_" + (index + 1) + "_hora", distinctAlternatives.get(index));
        }
    }

    private Optional<LocalDate> resolveStoredAlternativeDate(Map<String, String> entities) {
        String value = value(entities, "reprogramacion_fecha");
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<LocalTime> resolveStoredAlternativeTime(String message, Map<String, String> entities, String normalizedMessage) {
        Integer option = parseOption(normalizedMessage);
        if (option != null) {
            Optional<LocalTime> byOption = resolveTime(value(entities, "reprogramacion_opcion_" + option + "_hora"));
            if (byOption.isPresent()) {
                return byOption;
            }
        }
        Optional<LocalTime> requested = resolveTime(message);
        if (requested.isEmpty()) {
            return Optional.empty();
        }
        String formatted = formatTime(requested.get());
        for (int index = 1; index <= 3; index++) {
            if (formatted.equals(value(entities, "reprogramacion_opcion_" + index + "_hora"))) {
                return requested;
            }
        }
        return Optional.empty();
    }

    private String cancellationReason(String message) {
        String normalized = message == null ? "" : message.trim();
        if (normalized.isBlank()) {
            return "Cancelacion solicitada por el cliente via WhatsApp.";
        }
        return "Cancelacion solicitada por el cliente via WhatsApp. Mensaje: " + normalized;
    }

    private String numberedTimes(List<String> alternatives) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (String alternative : alternatives.stream().distinct().limit(3).toList()) {
            builder.append(index++).append(". ").append(alternative).append("\n");
        }
        return builder.toString();
    }

    private Integer parseOption(String normalizedMessage) {
        String value = normalizedMessage == null ? "" : normalizedMessage.trim();
        if (value.matches(".*\\b(primera|primer|uno)\\b.*")) {
            return 1;
        }
        if (value.matches(".*\\b(segunda|segundo|dos)\\b.*")) {
            return 2;
        }
        if (value.matches(".*\\b(tercera|tercer|tres)\\b.*")) {
            return 3;
        }
        Matcher matcher = Pattern.compile("^(?:.*\\b(?:opcion|opci[oó]n|reserva|reprograma|cancela|cancelar|la)\\b\\s*)?([1-3])\\s*$", Pattern.CASE_INSENSITIVE).matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isAffirmative(String normalizedMessage) {
        String value = normalizedMessage == null ? "" : normalizedMessage.trim();
        return value.equals("si") || value.equals("sí") || value.equals("ok") || value.equals("dale")
                || value.equals("correcto") || value.equals("confirmo") || value.contains("confirmo")
                || value.contains("si confirmo") || value.contains("si por favor");
    }

    private boolean isNegative(String normalizedMessage) {
        String value = normalizedMessage == null ? "" : normalizedMessage.trim();
        return value.equals("no") || value.equals("mejor no") || value.contains("no cancelar") || value.contains("no la canceles");
    }

    private boolean containsAny(String normalized, String... values) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        for (String value : values) {
            if (normalized.contains(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private String value(Map<String, String> entities, String key) {
        String value = entities.get(key);
        return value == null ? "" : value.trim();
    }

    private String formatDate(LocalDate value) {
        return String.format(Locale.ROOT, "%02d/%02d/%04d", value.getDayOfMonth(), value.getMonthValue(), value.getYear());
    }

    private int expirationMinutes() {
        int configured = bookingConfirmationProperties.getExpirationMinutes();
        if (configured < MIN_EXPIRATION_MINUTES) {
            return MIN_EXPIRATION_MINUTES;
        }
        if (configured > MAX_EXPIRATION_MINUTES) {
            return MAX_EXPIRATION_MINUTES;
        }
        return configured;
    }


    private Optional<AgendaSlotResponse> resolveExactAvailableSlot(
            AgendaAvailabilityResponse availability,
            LocalTime requestedTime,
            String traceId,
            UUID traceConversationId) {
        long availableSlots = availability.slots().stream().filter(AgendaSlotResponse::available).count();
        List<AgendaSlotResponse> exactCandidates = availability.slots().stream()
                .filter(candidate -> candidate.startsAt().toLocalTime().equals(requestedTime))
                .toList();
        Optional<AgendaSlotResponse> exactAvailable = exactCandidates.stream()
                .filter(AgendaSlotResponse::available)
                .findFirst();

        AiTraceLogger.info("EXACT_SLOT_VALIDATION_STARTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "requestedTime=" + requestedTime
                        + " totalSlots=" + availability.slots().size()
                        + " availableSlots=" + availableSlots
                        + " exactCandidates=" + exactCandidates.size()
                        + " firstAvailable=" + firstAvailableTime(availability)
                        + " lastAvailable=" + lastAvailableTime(availability));

        if (exactAvailable.isPresent()) {
            AgendaSlotResponse slot = exactAvailable.get();
            AiTraceLogger.info("EXACT_SLOT_VALIDATION_RESULT", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "available=true requestedTime=" + requestedTime
                            + " professionalId=" + slot.professionalId()
                            + " professionalName=" + safe(slot.professionalName())
                            + " roomId=" + slot.roomId()
                            + " roomName=" + safe(slot.roomName())
                            + " reason=EXACT_SLOT_AVAILABLE");
            return exactAvailable;
        }

        List<String> nearest = nearestAvailableSlots(availability, requestedTime);
        String reason = exactSlotRejectionReason(availability, requestedTime);
        AiTraceLogger.warn("EXACT_SLOT_REJECTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "available=false requestedTime=" + requestedTime
                        + " reason=" + reason
                        + " exactCandidates=" + exactCandidates.size()
                        + " nearestAvailable=" + nearest
                        + " diagnostic=No se crea reserva temporal porque no existe slot disponible exactamente a la hora solicitada.");
        return Optional.empty();
    }

    private String exactSlotRejectionReason(AgendaAvailabilityResponse availability, LocalTime requestedTime) {
        if (availability.slots().isEmpty()) {
            return "NO_SLOTS_RETURNED";
        }
        List<AgendaSlotResponse> available = availability.slots().stream()
                .filter(AgendaSlotResponse::available)
                .toList();
        if (available.isEmpty()) {
            return "NO_AVAILABLE_SLOTS_RETURNED";
        }
        boolean hasSameTime = availability.slots().stream()
                .anyMatch(slot -> slot.startsAt().toLocalTime().equals(requestedTime));
        if (hasSameTime) {
            return "REQUESTED_TIME_RETURNED_BUT_NOT_AVAILABLE";
        }
        LocalTime first = available.stream()
                .map(slot -> slot.startsAt().toLocalTime())
                .min(LocalTime::compareTo)
                .orElse(null);
        LocalTime last = available.stream()
                .map(slot -> slot.startsAt().toLocalTime())
                .max(LocalTime::compareTo)
                .orElse(null);
        if (first != null && requestedTime.isBefore(first)) {
            return "REQUESTED_TIME_BEFORE_FIRST_AVAILABLE_SLOT_OR_OUTSIDE_HOURS";
        }
        if (last != null && requestedTime.isAfter(last)) {
            return "REQUESTED_TIME_AFTER_LAST_AVAILABLE_SLOT_OR_OUTSIDE_HOURS";
        }
        return "REQUESTED_TIME_NOT_IN_AVAILABLE_SLOTS_POSSIBLE_CONFLICT_OR_BLOCK";
    }

    private String firstAvailableTime(AgendaAvailabilityResponse availability) {
        return availability.slots().stream()
                .filter(AgendaSlotResponse::available)
                .map(slot -> formatTime(slot.startsAt().toLocalTime()))
                .findFirst()
                .orElse("");
    }

    private String lastAvailableTime(AgendaAvailabilityResponse availability) {
        List<AgendaSlotResponse> available = availability.slots().stream()
                .filter(AgendaSlotResponse::available)
                .toList();
        if (available.isEmpty()) {
            return "";
        }
        return formatTime(available.get(available.size() - 1).startsAt().toLocalTime());
    }

    private List<String> nearestAvailableSlots(AgendaAvailabilityResponse availability, LocalTime requestedTime) {
        List<String> before = availability.slots().stream()
                .filter(AgendaSlotResponse::available)
                .map(slot -> slot.startsAt().toLocalTime())
                .filter(time -> time.isBefore(requestedTime))
                .sorted(Comparator.reverseOrder())
                .limit(2)
                .map(this::formatTime)
                .toList();
        List<String> after = availability.slots().stream()
                .filter(AgendaSlotResponse::available)
                .map(slot -> slot.startsAt().toLocalTime())
                .filter(time -> time.isAfter(requestedTime))
                .sorted()
                .limit(3)
                .map(this::formatTime)
                .toList();
        return java.util.stream.Stream.concat(before.stream(), after.stream())
                .distinct()
                .limit(5)
                .toList();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\n", " ").replace("\r", " ").trim();
    }

    public ResolvedLocation resolveEffectiveLocation(
            UUID businessId,
            String messageText,
            String extractedLocationText,
            UUID selectedLocationId,
            String selectedLocationName,
            String traceId,
            UUID traceConversationId) {
        /*
         * Prioridad funcional:
         * 1) Sucursal extraida por IA/reglas desde el mensaje del cliente.
         * 2) Texto completo del mensaje.
         * 3) Sucursal previamente asignada a la conversacion.
         *
         * Esto evita que una conversacion asociada a la sede principal
         * sobreescriba una sucursal explicita como "Providencia".
         */
        Optional<BusinessLocationRecord> fromEntity = resolveLocation(businessId, extractedLocationText);
        if (fromEntity.isPresent()) {
            AiTraceLogger.info("EFFECTIVE_LOCATION_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "locationId=" + fromEntity.get().id() + " locationName=" + fromEntity.get().name()
                            + " source=MESSAGE_TEXT entityLocation=" + extractedLocationText);
            return new ResolvedLocation(fromEntity.get(), "MESSAGE_TEXT");
        }

        Optional<BusinessLocationRecord> fromMessage = resolveLocation(businessId, messageText);
        if (fromMessage.isPresent()) {
            AiTraceLogger.info("EFFECTIVE_LOCATION_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "locationId=" + fromMessage.get().id() + " locationName=" + fromMessage.get().name()
                            + " source=MESSAGE_TEXT messageLocation=" + messageText);
            return new ResolvedLocation(fromMessage.get(), "MESSAGE_TEXT");
        }

        if (selectedLocationId != null) {
            Optional<BusinessLocationRecord> fromConversation = businessLocationJdbcRepository.findOptionalActiveById(businessId, selectedLocationId);
            if (fromConversation.isPresent()) {
                AiTraceLogger.info("EFFECTIVE_LOCATION_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                        "locationId=" + fromConversation.get().id() + " locationName=" + fromConversation.get().name()
                                + " source=CONVERSATION_SELECTED_LOCATION selectedLocationId=" + selectedLocationId);
                return new ResolvedLocation(fromConversation.get(), "CONVERSATION_SELECTED_LOCATION");
            }
        }

        Optional<BusinessLocationRecord> fromConversationName = resolveLocation(businessId, selectedLocationName);
        if (fromConversationName.isPresent()) {
            AiTraceLogger.info("EFFECTIVE_LOCATION_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "locationId=" + fromConversationName.get().id() + " locationName=" + fromConversationName.get().name()
                            + " source=CONVERSATION_SELECTED_LOCATION selectedLocationName=" + selectedLocationName);
            return new ResolvedLocation(fromConversationName.get(), "CONVERSATION_SELECTED_LOCATION");
        }

        Optional<BusinessLocationRecord> single = businessLocationJdbcRepository.findSingleActive(businessId);
        if (single.isPresent()) {
            AiTraceLogger.info("EFFECTIVE_LOCATION_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                    "locationId=" + single.get().id() + " locationName=" + single.get().name() + " source=BUSINESS_DEFAULT_LOCATION");
            return new ResolvedLocation(single.get(), "BUSINESS_DEFAULT_LOCATION");
        }
        AiTraceLogger.warn("EFFECTIVE_LOCATION_RESOLVED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "locationId= locationName= source=MISSING messageLocation=" + messageText
                        + " conversationLocation=" + selectedLocationName);
        return new ResolvedLocation(null, "MISSING");
    }

    public Optional<BusinessLocationRecord> resolveLocation(UUID businessId, String rawText) {
        List<BusinessLocationRecord> locations = businessLocationJdbcRepository.findActive(businessId);
        String normalized = normalize(rawText);
        if (normalized.isBlank()) {
            return locations.size() == 1 ? Optional.of(locations.getFirst()) : Optional.empty();
        }
        return locations.stream()
                .map(location -> new LocationMatch(location, scoreLocation(normalized, location)))
                .filter(match -> match.score() > 0)
                .max(Comparator.comparingInt(LocationMatch::score))
                .map(LocationMatch::location);
    }

    public Optional<AestheticServiceResponse> resolveService(UUID businessId, String rawText) {
        String normalized = normalize(resolveKnownAlias(rawText));
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        PagedResponse<AestheticServiceResponse> services = aestheticCenterJdbcRepository.findServices(
                businessId, 0, MAX_SERVICE_SCAN, null, null, true);
        return services.items().stream()
                .map(service -> new ServiceMatch(service, scoreService(normalized, service)))
                .filter(match -> match.score() > 0)
                .max(Comparator.comparingInt(ServiceMatch::score))
                .map(ServiceMatch::service);
    }

    private int scoreLocation(String normalizedText, BusinessLocationRecord location) {
        int best = 0;
        String name = normalize(location.name());
        String code = normalize(location.code());
        String commune = normalize(location.commune());
        String city = normalize(location.city());

        if (!name.isBlank() && normalizedText.contains(name)) {
            best = Math.max(best, 1000 + name.length());
        }
        if (!code.isBlank() && normalizedText.contains(code)) {
            best = Math.max(best, 950 + code.length());
        }
        if (normalizedText.contains("sucursal " + name) || normalizedText.contains("sede " + name) || normalizedText.contains("en " + name)) {
            best = Math.max(best, 1200 + name.length());
        }
        if (normalizedText.contains("provi") && (name.contains("providencia") || code.contains("providencia"))) {
            best = Math.max(best, 1150);
        }
        if (!commune.isBlank() && normalizedText.contains(commune)) {
            best = Math.max(best, 350 + commune.length());
        }
        if (!city.isBlank() && normalizedText.contains(city)) {
            best = Math.max(best, 150 + city.length());
        }
        return best;
    }

    private boolean containsNormalized(String normalizedText, String candidate) {
        String normalizedCandidate = normalize(candidate);
        return !normalizedCandidate.isBlank() && normalizedText.contains(normalizedCandidate);
    }

    private int scoreService(String normalizedText, AestheticServiceResponse service) {
        String normalizedName = normalize(service.name());
        String normalizedCode = normalize(service.code());
        if (!normalizedName.isBlank() && normalizedText.contains(normalizedName)) {
            return 1000 + normalizedName.length();
        }
        if (!normalizedCode.isBlank() && normalizedText.contains(normalizedCode)) {
            return 950 + normalizedCode.length();
        }
        List<String> serviceTokens = List.of(normalizedName.split(" ")).stream()
                .filter(token -> token.length() > 2)
                .filter(token -> !isServiceModifier(token))
                .distinct()
                .toList();
        if (!serviceTokens.isEmpty() && serviceTokens.stream().allMatch(token -> containsWholeToken(normalizedText, token))) {
            return 700 + serviceTokens.stream().mapToInt(String::length).sum();
        }
        return 0;
    }

    private boolean isServiceModifier(String token) {
        return switch (token) {
            case "profunda", "profundo", "basica", "basico", "suave", "completa", "completo", "estetica", "estetico", "facial" -> true;
            default -> false;
        };
    }

    private String resolveKnownAlias(String value) {
        String normalized = normalize(value);
        if (normalized.contains("limpieza de rostro")
                || normalized.contains("limpieza rostro")
                || normalized.contains("limpieza de cutis")
                || normalized.contains("higiene de rostro")) {
            return "Limpieza facial profunda " + value;
        }
        return value == null ? "" : value;
    }

    private Optional<LocalDate> resolveDate(String rawDate) {
        return resolveDate(rawDate, DEFAULT_BUSINESS_ZONE);
    }

    private Optional<LocalDate> resolveDate(String rawDate, ZoneId zone) {
        String normalized = normalize(rawDate);
        LocalDate today = LocalDate.now(zone == null ? DEFAULT_BUSINESS_ZONE : zone);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        Matcher iso = ISO_DATE_PATTERN.matcher(normalized);
        if (iso.find()) {
            int year = Integer.parseInt(iso.group(1));
            int month = Integer.parseInt(iso.group(2));
            int day = Integer.parseInt(iso.group(3));
            return Optional.of(LocalDate.of(year, month, day));
        }
        Matcher explicit = EXPLICIT_DATE_PATTERN.matcher(normalized);
        if (explicit.find()) {
            int day = Integer.parseInt(explicit.group(1));
            int month = Integer.parseInt(explicit.group(2));
            int year = explicit.group(3) == null ? today.getYear() : Integer.parseInt(explicit.group(3));
            if (year < 100) {
                year += 2000;
            }
            return Optional.of(LocalDate.of(year, month, day));
        }
        Matcher monthName = MONTH_NAME_DATE_PATTERN.matcher(normalized);
        if (monthName.find()) {
            int day = Integer.parseInt(monthName.group(1));
            int month = monthNumber(monthName.group(2));
            int year = monthName.group(3) == null ? today.getYear() : Integer.parseInt(monthName.group(3));
            if (year < 100) {
                year += 2000;
            }
            return Optional.of(LocalDate.of(year, month, day));
        }
        if (containsWholeToken(normalized, "hoy")) {
            return Optional.of(today);
        }
        if (normalized.contains("pasado manana")) {
            return Optional.of(today.plusDays(2));
        }
        if (containsWholeToken(normalized, "manana")) {
            return Optional.of(today.plusDays(1));
        }
        Optional<DayOfWeek> dayOfWeek = resolveDayOfWeek(normalized);
        if (dayOfWeek.isPresent()) {
            LocalDate resolved = today.with(TemporalAdjusters.nextOrSame(dayOfWeek.get()));
            return Optional.of(resolved.equals(today) ? today.plusWeeks(1) : resolved);
        }
        return Optional.empty();
    }


    private int monthNumber(String value) {
        return switch (normalize(value)) {
            case "enero" -> 1;
            case "febrero" -> 2;
            case "marzo" -> 3;
            case "abril" -> 4;
            case "mayo" -> 5;
            case "junio" -> 6;
            case "julio" -> 7;
            case "agosto" -> 8;
            case "septiembre", "setiembre" -> 9;
            case "octubre" -> 10;
            case "noviembre" -> 11;
            case "diciembre" -> 12;
            default -> throw new IllegalArgumentException("Mes no reconocido: " + value);
        };
    }

    private Optional<DayOfWeek> resolveDayOfWeek(String normalized) {
        if (containsWholeToken(normalized, "lunes")) return Optional.of(DayOfWeek.MONDAY);
        if (containsWholeToken(normalized, "martes")) return Optional.of(DayOfWeek.TUESDAY);
        if (containsWholeToken(normalized, "miercoles")) return Optional.of(DayOfWeek.WEDNESDAY);
        if (containsWholeToken(normalized, "jueves")) return Optional.of(DayOfWeek.THURSDAY);
        if (containsWholeToken(normalized, "viernes")) return Optional.of(DayOfWeek.FRIDAY);
        if (containsWholeToken(normalized, "sabado")) return Optional.of(DayOfWeek.SATURDAY);
        if (containsWholeToken(normalized, "domingo")) return Optional.of(DayOfWeek.SUNDAY);
        return Optional.empty();
    }

    private Optional<LocalTime> resolveTime(String rawTime) {
        String value = rawTime == null ? "" : rawTime.trim();
        String normalized = normalize(value);
        Matcher matcher = TIME_PATTERN.matcher(value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        if (hour >= 1 && hour <= 7 && (normalized.contains("tarde") || normalized.contains("noche"))) {
            hour += 12;
        }
        return Optional.of(LocalTime.of(hour, minute));
    }

    private String dryRunAvailabilityResponse(AestheticServiceResponse service, BusinessLocationRecord location, String dateText, String timeText, String traceId, UUID traceConversationId) {
        String response = WhatsAppMessageFormatter.bookingPreview(
                displayService(service.name()),
                location.name(),
                displayDate(dateText),
                displayTime(timeText));
        AiTraceLogger.info("WHATSAPP_MESSAGE_FORMATTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "type=BOOKING_PREVIEW containsLink=false messageLength=" + response.length());
        return response;
    }

    private String successResponse(AestheticServiceResponse service, BusinessLocationRecord location, String dateText, String timeText,
            BookingConfirmationLinkResponse link, String traceId, UUID traceConversationId) {
        String response = WhatsAppMessageFormatter.temporaryBookingCreated(
                displayService(service.name()),
                location.name(),
                displayDate(dateText),
                displayTime(timeText),
                link.confirmationUrl(),
                expirationMinutes());
        AiTraceLogger.info("WHATSAPP_MESSAGE_FORMATTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "type=TEMPORARY_BOOKING containsLink=" + response.contains("/reservas/confirmar/")
                        + " messageLength=" + response.length());
        return response;
    }

    private String noAvailabilityResponse(AestheticServiceResponse service, BusinessLocationRecord location, String dateText, String timeText,
            AgendaAvailabilityResponse availability, LocalTime requestedTime, String traceId, UUID traceConversationId) {
        List<String> alternatives = availability.slots().stream()
                .filter(AgendaSlotResponse::available)
                .filter(slot -> slot.startsAt().toLocalTime().isAfter(requestedTime))
                .map(slot -> displayDate(dateText) + " a las " + formatTime(slot.startsAt().toLocalTime()))
                .distinct()
                .limit(3)
                .toList();
        AiTraceLogger.info("AVAILABILITY_ALTERNATIVES_FOUND", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "requestedTime=" + requestedTime
                        + " alternatives=" + alternatives
                        + " duplicatesRemoved=true maxAlternatives=3");
        String response = WhatsAppMessageFormatter.noAvailability(
                displayService(service.name()),
                location.name(),
                displayDate(dateText),
                displayTime(timeText),
                alternatives);
        AiTraceLogger.info("WHATSAPP_MESSAGE_FORMATTED", traceId, traceConversationId, null, "TransactionalAgendaBookingService",
                "type=NO_AVAILABILITY containsLink=false messageLength=" + response.length());
        return response;
    }

    private String serviceLocationUnavailableResponse(AestheticServiceResponse service, BusinessLocationRecord location, ApiException exception) {
        return displayService(service.name()) + " no está configurado para " + location.name()
                + ". Puedo revisar otra sucursal disponible o ayudarte con otro servicio.";
    }

    private String extractToken(String confirmationUrl) {
        if (confirmationUrl == null || confirmationUrl.isBlank()) {
            return "";
        }
        int index = confirmationUrl.lastIndexOf('/');
        return index >= 0 && index + 1 < confirmationUrl.length() ? confirmationUrl.substring(index + 1) : confirmationUrl;
    }

    private AuthenticatedUser systemUser(UUID businessId, BusinessLocationRecord location) {
        return new AuthenticatedUser(
                null,
                businessId,
                "Centro Estetico Bella",
                "IA",
                "Negocio",
                "ia@local",
                location.timezone() == null || location.timezone().isBlank() ? "America/Santiago" : location.timezone(),
                List.of("ADMIN"),
                List.of());
    }

    private String normalizedCustomerName(String value) {
        return value == null || value.isBlank() ? DEMO_CUSTOMER_NAME : value.trim();
    }

    private String normalizedCustomerPhone(String value) {
        return value == null || value.isBlank() ? DEMO_CUSTOMER_PHONE : value.replace(" ", "").trim();
    }

    private String normalizedSearchPhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "").trim();
        return digits.isBlank() ? null : digits;
    }

    private String mappedPhoneDigits(String value) {
        String digits = normalizedSearchPhone(value);
        if (digits == null) {
            return "";
        }
        return TEST_PHONE_MAP.getOrDefault(digits, digits);
    }

    private String lastDigits(String value, int length) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() <= length) {
            return digits;
        }
        return digits.substring(digits.length() - length);
    }

    private String displayDate(String value) {
        return value == null || value.isBlank() ? "la fecha solicitada" : value.trim();
    }

    private String displayTime(String value) {
        return resolveTime(value).map(this::formatTime).orElse(value == null ? "" : value.trim());
    }

    private String formatTime(LocalTime value) {
        return String.format(Locale.ROOT, "%02d:%02d", value.getHour(), value.getMinute());
    }

    private String displayService(String value) {
        return value == null || value.isBlank() ? "el servicio" : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : (second == null ? "" : second);
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "").trim();
    }

    private boolean containsWholeToken(String normalizedText, String token) {
        if (normalizedText == null || normalizedText.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        for (String part : normalizedText.split(" ")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^a-z0-9:/ -]", " ").replaceAll("\\s+", " ").trim();
    }

    public record ResolvedLocation(BusinessLocationRecord location, String source) { }

    private record ServiceMatch(AestheticServiceResponse service, int score) { }

    private record LocationMatch(BusinessLocationRecord location, int score) { }
}
