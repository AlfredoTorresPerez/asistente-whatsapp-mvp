package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.bookings.api.BookingPaymentWebhookRequest;
import com.asistentewhatsapp.bookings.api.BookingPaymentWebhookResponse;
import com.asistentewhatsapp.bookings.api.BookingPaymentResponse;
import com.asistentewhatsapp.bookings.api.PublicBookingPaymentDetailResponse;
import com.asistentewhatsapp.bookings.api.CreateBookingPaymentLinkRequest;
import com.asistentewhatsapp.bookings.api.RefundBookingPaymentRequest;
import com.asistentewhatsapp.bookings.api.RegisterBookingManualPaymentRequest;
import com.asistentewhatsapp.bookings.domain.BookingPaymentProvider;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentBookingRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentNotificationRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.PublicBookingPaymentDetailRecord;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingPaymentService {

    private final BookingPaymentJdbcRepository repository;
    private final BookingPaymentProperties properties;
    private final BookingPaymentProviderRegistry providerRegistry;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ChannelDispatchService channelDispatchService;
    private final BookingEmailService bookingEmailService;
    private final CalendarSyncService calendarSyncService;

    public BookingPaymentService(
            BookingPaymentJdbcRepository repository,
            BookingPaymentProperties properties,
            BookingPaymentProviderRegistry providerRegistry,
            AuditService auditService,
            ObjectMapper objectMapper,
            ChannelDispatchService channelDispatchService,
            BookingEmailService bookingEmailService,
            CalendarSyncService calendarSyncService) {
        this.repository = repository;
        this.properties = properties;
        this.providerRegistry = providerRegistry;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.channelDispatchService = channelDispatchService;
        this.bookingEmailService = bookingEmailService;
        this.calendarSyncService = calendarSyncService;
    }

    @Transactional
    public BookingPaymentWebhookResponse handleWebhook(String rawBody, String timestampHeader, String signatureHeader) {
        validateSignature(rawBody, timestampHeader, signatureHeader);
        BookingPaymentProvider provider = providerRegistry.getDefaultProvider();
        if (provider.supportsWebhook()) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("X-Booking-Payment-Timestamp", timestampHeader);
            headers.put("X-Booking-Payment-Signature", signatureHeader);
            Optional<BookingPaymentProvider.PaymentNotification> notification = provider.parseWebhook(rawBody, headers);
            if (notification.isPresent()) {
                return processProviderNotification(notification.get(), rawBody, provider.providerName());
            }
        }
        BookingPaymentWebhookRequest request = parseWebhookBody(rawBody);
        return processStandardWebhook(request, rawBody);
    }

    private BookingPaymentWebhookResponse processProviderNotification(BookingPaymentProvider.PaymentNotification notification, String rawBody, String providerName) {
        String rawPayloadJson = rawBody == null || rawBody.isBlank() ? "{}" : rawBody;
        return doProcessPayment(null, null, notification.providerPaymentId(), notification.idempotencyKey(), notification.amount(),
                notification.currency(), notification.status(), notification.occurredAt(), notification.metadata(),
                providerName, rawPayloadJson);
    }

    private BookingPaymentWebhookResponse processStandardWebhook(BookingPaymentWebhookRequest request, String rawBody) {
        String rawPayloadJson = rawBody == null || rawBody.isBlank() ? "{}" : rawBody;
        String provider = normalizeProvider(request.provider());
        return doProcessPayment(request.businessId(), request.bookingId(), request.providerPaymentId(), request.idempotencyKey(), request.amount(),
                request.currency(), request.status(), request.occurredAt(),
                request.metadata() == null ? Map.of() : request.metadata(),
                provider, rawPayloadJson);
    }

    private record IntegratePaymentResult(BookingPaymentRecord payment, boolean duplicate, String bookingStatus) {}

    private BookingPaymentWebhookResponse doProcessPayment(
            UUID businessId, UUID bookingId, String providerPaymentId, String idempotencyKey, BigDecimal amount, String currency,
            String status, OffsetDateTime occurredAt, Map<String, Object> metadata,
            String provider, String rawPayloadJson) {
        if (providerPaymentId == null && idempotencyKey == null) {
            throw validationError("idempotencyKey", "El webhook debe incluir providerPaymentId o idempotencyKey.");
        }
        if (amount == null || amount.signum() < 0) {
            throw validationError("amount", "El monto del pago no puede ser negativo.");
        }
        String resolvedCurrency = currency == null || currency.isBlank() ? "CLP" : currency.trim().toUpperCase(Locale.ROOT);
        if (resolvedCurrency.length() != 3) {
            throw validationError("currency", "La moneda debe usar codigo ISO de 3 letras.");
        }
        String resolvedStatus = normalizeStatus(status);
        OffsetDateTime resolvedOccurredAt = occurredAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : occurredAt;
        NormalizedPayment normalized = new NormalizedPayment(businessId, bookingId, provider,
                providerPaymentId, idempotencyKey, amount, resolvedCurrency, resolvedStatus, resolvedOccurredAt, metadata);

        IntegratePaymentResult result = integratePayment(normalized, rawPayloadJson);
        return new BookingPaymentWebhookResponse(
                result.payment().id(),
                result.payment().bookingId(),
                null,
                result.bookingStatus(),
                result.duplicate(),
                false);
    }

    private IntegratePaymentResult integratePayment(NormalizedPayment normalized, String rawPayloadJson) {
        String metadataJson = toJson(normalized.metadata());
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(normalized.businessId(), normalized.bookingId());
        Optional<BookingPaymentRecord> existing = repository.findExisting(
                normalized.businessId(), normalized.provider(),
                normalized.providerPaymentId(), normalized.idempotencyKey());
        if (existing.isPresent() && existing.get().status().equals(normalized.status())) {
            return new IntegratePaymentResult(existing.get(), true, booking.bookingStatus());
        }
        if (existing.isPresent() && isTerminal(existing.get().status())) {
            return new IntegratePaymentResult(existing.get(), true, booking.bookingStatus());
        }
        BookingPaymentRecord payment;
        if (existing.isPresent()) {
            payment = repository.updatePaymentStatus(existing.get().id(), normalized.status(), rawPayloadJson, metadataJson, normalized.occurredAt());
        } else {
            payment = repository.insertPayment(
                    normalized.businessId(), normalized.bookingId(), normalized.provider(),
                    normalized.providerPaymentId(), normalized.idempotencyKey(),
                    normalized.amount(), normalized.currency(), normalized.status(),
                    rawPayloadJson, metadataJson, normalized.occurredAt());
        }
        repository.recalculateBookingPaymentStatus(normalized.businessId(), normalized.bookingId());
        BookingPaymentBookingRecord updatedBooking = repository.findBookingForUpdate(normalized.businessId(), normalized.bookingId());
        recordPaymentAudit(payment, updatedBooking, existing.isPresent());
        if ("APPROVED".equals(normalized.status()) && isPaidOrOverpaid(updatedBooking)) {
            transitionToConfirmed(normalized.businessId(), normalized.bookingId(), updatedBooking);
        }
        if ("APPROVED".equals(normalized.status())) {
            sendPostPaymentNotifications(normalized.businessId(), payment);
        }
        return new IntegratePaymentResult(payment, false, updatedBooking.bookingStatus());
    }

    private boolean isPaidOrOverpaid(BookingPaymentBookingRecord booking) {
        if (!booking.requiresDeposit()) {
            return true;
        }
        return repository.hasApprovedRequiredDeposit(booking.businessId(), booking.bookingId());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingPaymentService.class);

    private void transitionToConfirmed(UUID businessId, UUID bookingId, BookingPaymentBookingRecord booking) {
        BookingStateMachine.assertTransition(booking.bookingStatus(), BookingStateMachine.CONFIRMED, "confirmarse por pago");
        repository.updateBookingStatus(businessId, bookingId, BookingStateMachine.CONFIRMED, "PAGO_APROBADO",
                "Reserva confirmada automaticamente por pago aprobado.");
        auditService.record(businessId, null, "BOOKING_AUTO_CONFIRMED_BY_PAYMENT", "BOOKING", bookingId,
                "Reserva confirmada automaticamente tras pago aprobado.",
                AuditMetadata.of("previousBookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "paymentStatus", booking.paymentStatus()));
        try { calendarSyncService.syncConfirmed(bookingId, businessId); }
        catch (Exception e) { LOGGER.warn("CALENDAR_SYNC_CONFIRMED_FAILED bookingId={}", bookingId, e); }
    }

    private void sendPostPaymentNotifications(UUID businessId, BookingPaymentRecord payment) {
        boolean sendWhatsApp = properties.isDispatchPostPaymentWhatsApp();
        boolean sendEmail = properties.isDispatchPostPaymentEmail();
        if (!sendWhatsApp && !sendEmail) {
            return;
        }
        BookingPaymentNotificationRecord booking;
        try {
            booking = repository.findNotificationContext(businessId, payment.bookingId());
        } catch (RuntimeException exception) {
            return;
        }
        if (sendEmail) {
            sendPostPaymentEmail(booking, payment);
        }
        if (sendWhatsApp) {
            sendPostPaymentWhatsApp(booking, payment);
        }
    }

    private void sendPostPaymentEmail(BookingPaymentNotificationRecord booking, BookingPaymentRecord payment) {
        String body = bookingEmailService.buildAppointmentEmailBody(
                booking.customerName(),
                "Pago recibido. Tu reserva esta confirmada.",
                booking.serviceName() == null ? booking.subject() : booking.serviceName(),
                formatDateTime(booking.startsAt()),
                booking.locationName() == null ? booking.location() : booking.locationName(),
                booking.professionalName(),
                booking.roomName(),
                null,
                "Monto pagado: " + payment.currency() + " " + payment.amount() + ".");
        bookingEmailService.sendBookingEmail(booking.businessId(), booking.bookingId(), booking.customerEmail(),
                "BOOKING_PAYMENT_RECEIVED", "Pago recibido - Reserva confirmada", body);
        auditService.record(booking.businessId(), null, "BOOKING_POST_PAYMENT_EMAIL_SENT", "BOOKING", booking.bookingId(),
                "Correo de confirmacion de pago enviado.",
                AuditMetadata.of("paymentId", payment.id(), "amount", payment.amount()));
    }

    private void sendPostPaymentWhatsApp(BookingPaymentNotificationRecord booking, BookingPaymentRecord payment) {
        String body = """
                Pago recibido - Reserva confirmada

                Cliente: %s
                Servicio: %s
                Fecha: %s
                Monto pagado: %s %s
                """.formatted(
                valueOrFallback(booking.customerName(), "Cliente"),
                valueOrFallback(booking.serviceName(), booking.subject()),
                formatDateTime(booking.startsAt()),
                payment.currency(),
                payment.amount());
        try {
            channelDispatchService.dispatch(new ChannelDispatchRequest(
                    booking.businessId(), MessageChannelType.WHATSAPP, booking.customerPhone(), body));
            auditService.record(booking.businessId(), null, "BOOKING_POST_PAYMENT_WHATSAPP_SENT", "BOOKING", booking.bookingId(),
                    "WhatsApp de confirmacion de pago enviado.",
                    AuditMetadata.of("paymentId", payment.id()));
        } catch (RuntimeException exception) {
            auditService.record(booking.businessId(), null, "BOOKING_POST_PAYMENT_WHATSAPP_FAILED", "BOOKING", booking.bookingId(),
                    "Fallo envio WhatsApp post-pago: " + safeMessage(exception),
                    AuditMetadata.of("paymentId", payment.id()));
        }
    }

    public boolean hasApprovedRequiredDeposit(UUID businessId, UUID bookingId) {
        return repository.hasApprovedRequiredDeposit(businessId, bookingId);
    }

    @Transactional
    public List<BookingPaymentResponse> listPayments(AuthenticatedUser user, UUID bookingId) {
        repository.findBookingForUpdate(user.businessId(), bookingId);
        return repository.findPayments(user.businessId(), bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingPaymentResponse createCheckoutLink(
            AuthenticatedUser user,
            UUID bookingId,
            CreateBookingPaymentLinkRequest request) {
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(user.businessId(), bookingId);
        if (!booking.requiresDeposit()) {
            throw validationError("requiresDeposit", "La reserva no requiere abono.");
        }
        if (BookingStateMachine.isClosed(booking.bookingStatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "BOOKING_CLOSED_FOR_PAYMENT_LINK",
                    "No se puede crear un enlace de pago para una reserva cerrada.",
                    Map.of("bookingStatus", BookingStateMachine.canonical(booking.bookingStatus())));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Optional<BookingPaymentRecord> activeCheckout = repository.findActiveCheckout(user.businessId(), bookingId, now);
        if (activeCheckout.isPresent()) {
            sendPaymentLinkNotifications(user, activeCheckout.get(), request);
            return toResponse(activeCheckout.get());
        }

        BigDecimal amount = request != null && request.amount() != null ? request.amount() : booking.depositAmount();
        if (amount == null || amount.signum() <= 0) {
            throw validationError("amount", "El monto del enlace de pago debe ser mayor a cero.");
        }
        String providerName = normalizeProvider(request == null ? null : request.provider(), properties.getProvider());
        String currency = normalizeCurrency(request == null ? null : request.currency());
        int expirationMinutes = request != null && request.expirationMinutes() != null
                ? Math.min(Math.max(request.expirationMinutes(), 5), 1440)
                : Math.min(Math.max(properties.getCheckoutExpirationMinutes(), 5), 1440);
        String idempotencyKey = "checkout:" + bookingId + ":" + now.toEpochSecond();

        BookingPaymentProvider provider = providerRegistry.getProvider(providerName);
        String description = "Reserva #" + bookingId.toString().substring(0, 8);
        String returnUrl = properties.getCheckoutPublicBaseUrl() + "/" + bookingId;
        String notificationUrl = "";
        String baseUrl = properties.getCheckoutPublicBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            notificationUrl = baseUrl.replace("/reservas/pagar", "/api/v1/integrations/booking-payments/webhook");
        }

        BookingPaymentProvider.CreateCheckoutResult checkout = provider.createCheckout(
                user.businessId(), bookingId, null, amount, currency, description, returnUrl, notificationUrl, expirationMinutes);
        OffsetDateTime expiresAt = checkout.expiresAt() != null ? checkout.expiresAt() : now.plusMinutes(expirationMinutes);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "PROVIDER_CHECKOUT_LINK");
        metadata.put("provider", providerName);
        metadata.put("actorUserId", user.userId());
        metadata.put("bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()));
        metadata.put("bookingPaymentStatus", booking.paymentStatus());
        metadata.putAll(checkout.metadata());
        if (request != null && request.metadata() != null) {
            metadata.putAll(request.metadata());
        }

        UUID paymentId = UUID.randomUUID();
        BookingPaymentRecord payment = insertPaymentFromCheckout(
                user.businessId(), bookingId, providerName, provider.providerName(),
                checkout.providerPaymentId(), idempotencyKey, amount, currency,
                checkout.checkoutUrl(), expiresAt, toJson(metadata), paymentId);

        if (checkout.providerPaymentId() != null && !checkout.providerPaymentId().isBlank()) {
            repository.updatePaymentProviderId(paymentId, checkout.providerPaymentId());
        }

        if (booking.bookingStatus() != null
                && !BookingStateMachine.PENDING_PAYMENT.equals(BookingStateMachine.canonical(booking.bookingStatus()))) {
            BookingStateMachine.assertTransition(booking.bookingStatus(), BookingStateMachine.PENDING_PAYMENT, "generar link de pago");
            repository.updateBookingStatus(user.businessId(), bookingId, BookingStateMachine.PENDING_PAYMENT, "PAGO_LINK_CREADO",
                    "Reserva pasa a pendiente de pago por generacion de link.");
        }

        auditService.record(user.businessId(), user.userId(), "BOOKING_PAYMENT_LINK_CREATED", "BOOKING", bookingId,
                "Se genero enlace de pago para reserva via " + providerName + ".",
                AuditMetadata.of(
                        "paymentId", payment.id(),
                        "provider", providerName,
                        "amount", payment.amount(),
                        "currency", payment.currency(),
                        "checkoutExpiresAt", payment.checkoutExpiresAt(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus())));
        sendPaymentLinkNotifications(user, payment, request);
        return toResponse(payment);
    }

    private BookingPaymentRecord insertPaymentFromCheckout(
            UUID businessId, UUID bookingId, String provider, String providerName,
            String providerPaymentId, String idempotencyKey, BigDecimal amount, String currency,
            String checkoutUrl, OffsetDateTime expiresAt, String metadata, UUID paymentId) {
        repository.insertPaymentFromCheckout(businessId, bookingId, provider, providerPaymentId,
                idempotencyKey, amount, currency, checkoutUrl, expiresAt, metadata, paymentId);
        return repository.findById(paymentId);
    }

    @Transactional
    public BookingPaymentResponse registerManualPayment(
            AuthenticatedUser user,
            UUID bookingId,
            RegisterBookingManualPaymentRequest request) {
        if (request == null) {
            throw validationError("payment", "El pago manual es obligatorio.");
        }
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(user.businessId(), bookingId);
        BigDecimal amount = request.amount() != null ? request.amount() : booking.depositAmount();
        if (amount == null || amount.signum() <= 0) {
            throw validationError("amount", "El monto del pago debe ser mayor a cero.");
        }
        String provider = normalizeProvider(request.provider(), "MANUAL");
        String providerPaymentId = normalizeOptional(request.providerPaymentId(), 160);
        String idempotencyKey = normalizeOptional(request.idempotencyKey(), 160);
        if (providerPaymentId == null && idempotencyKey == null) {
            idempotencyKey = "manual:" + bookingId + ":" + UUID.randomUUID();
        }
        Optional<BookingPaymentRecord> existing = repository.findExisting(user.businessId(), provider, providerPaymentId, idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        String status = normalizeStatus(request.status() == null || request.status().isBlank() ? "APPROVED" : request.status());
        OffsetDateTime occurredAt = request.occurredAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : request.occurredAt();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "INTERNAL_MANUAL_PAYMENT");
        metadata.put("actorUserId", user.userId());
        metadata.put("bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()));
        metadata.put("notes", request.notes());
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        BookingPaymentRecord payment = repository.insertManualPayment(
                user.businessId(), bookingId, provider, providerPaymentId, idempotencyKey,
                amount, normalizeCurrency(request.currency()), status, "{}", toJson(metadata), occurredAt);
        repository.recalculateBookingPaymentStatus(user.businessId(), bookingId);
        BookingPaymentBookingRecord updatedBooking = repository.findBookingForUpdate(user.businessId(), bookingId);
        recordManualPaymentAudit(user, payment, updatedBooking);
        if ("APPROVED".equals(status) && isPaidOrOverpaid(updatedBooking)) {
            transitionToConfirmed(user.businessId(), bookingId, updatedBooking);
        }
        if ("APPROVED".equals(status)) {
            sendPostPaymentNotifications(user.businessId(), payment);
        }
        return toResponse(payment);
    }

    @Scheduled(fixedDelayString = "${app.booking-payment.expiration-scan-ms:60000}")
    @Transactional
    public void expireDuePaymentLinks() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int limit = Math.min(Math.max(properties.getExpirationBatchSize(), 1), 500);
        List<BookingPaymentRecord> expired = repository.findExpiredPendingCheckouts(now, limit);
        for (BookingPaymentRecord payment : expired) {
            BookingPaymentRecord updated = repository.updatePaymentStatus(
                    payment.id(), "EXPIRED", "{}",
                    toJson(AuditMetadata.of("source", "BOOKING_PAYMENT_EXPIRATION_SCHEDULER",
                            "checkoutExpiresAt", payment.checkoutExpiresAt())), now);
            repository.recalculateBookingPaymentStatus(updated.businessId(), updated.bookingId());
            auditService.record(updated.businessId(), null, "BOOKING_PAYMENT_LINK_EXPIRED", "BOOKING", updated.bookingId(),
                    "Link de pago de reserva expirado automaticamente.",
                    AuditMetadata.of("paymentId", updated.id(), "provider", updated.provider(),
                            "amount", updated.amount(), "currency", updated.currency(),
                            "checkoutExpiresAt", updated.checkoutExpiresAt()));
        }
    }

    @Transactional
    public BookingPaymentResponse refundPayment(
            AuthenticatedUser user,
            UUID bookingId,
            UUID paymentId,
            RefundBookingPaymentRequest request) {
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(user.businessId(), bookingId);
        BookingPaymentRecord current = repository.findByIdForBooking(user.businessId(), bookingId, paymentId);
        if (!"APPROVED".equals(current.status())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "BOOKING_PAYMENT_NOT_REFUNDABLE",
                    "Solo se puede reembolsar un pago aprobado.",
                    Map.of("paymentStatus", current.status()));
        }
        BookingPaymentProvider provider = providerRegistry.getProvider(current.provider());
        try {
            BookingPaymentProvider.RefundResult refundResult = provider.refund(
                    user.businessId(), bookingId, paymentId, current.providerPaymentId(),
                    current.amount(), request == null ? null : request.reason());
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", "PROVIDER_REFUND");
            metadata.put("actorUserId", user.userId());
            metadata.put("reason", request == null ? null : request.reason());
            metadata.put("previousStatus", current.status());
            metadata.put("providerRefundId", refundResult.providerRefundId());
            metadata.putAll(refundResult.metadata());
            BookingPaymentRecord payment = repository.updatePaymentStatus(
                    current.id(), "REFUNDED", "{}", toJson(metadata), OffsetDateTime.now(ZoneOffset.UTC));
            repository.recalculateBookingPaymentStatus(user.businessId(), bookingId);
            auditService.record(user.businessId(), user.userId(), "BOOKING_PAYMENT_REFUNDED", "BOOKING", bookingId,
                    "Reembolso procesado via " + provider.providerName() + ".",
                    AuditMetadata.of("paymentId", payment.id(), "provider", current.provider(),
                            "amount", current.amount(), "providerRefundId", refundResult.providerRefundId()));
            return toResponse(payment);
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "BOOKING_PAYMENT_REFUND_FAILED",
                    "Error al procesar reembolso: " + safeMessage(exception),
                    Map.of("error", safeMessage(exception)));
        }
    }

    private void recordPaymentAudit(BookingPaymentRecord payment, BookingPaymentBookingRecord booking, boolean update) {
        String eventName = switch (payment.status()) {
            case "APPROVED" -> "BOOKING_PAYMENT_APPROVED";
            case "REJECTED" -> "BOOKING_PAYMENT_REJECTED";
            case "EXPIRED" -> "BOOKING_PAYMENT_EXPIRED";
            case "REFUNDED" -> "BOOKING_PAYMENT_REFUNDED";
            default -> "BOOKING_PAYMENT_PENDING";
        };
        auditService.record(payment.businessId(), null, eventName, "BOOKING", payment.bookingId(),
                "Webhook de pago de reserva procesado.",
                AuditMetadata.of(
                        "paymentId", payment.id(), "provider", payment.provider(),
                        "providerPaymentId", payment.providerPaymentId(), "idempotencyKey", payment.idempotencyKey(),
                        "amount", payment.amount(), "currency", payment.currency(), "paymentStatus", payment.status(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "bookingPaymentStatus", booking.paymentStatus(),
                        "requiresDeposit", booking.requiresDeposit(),
                        "depositAmount", booking.depositAmount(),
                        "updatedExistingPayment", update));
    }

    public BookingPaymentResponse getPaymentStatus(UUID paymentId) {
        BookingPaymentRecord payment = repository.findById(paymentId);
        return toResponse(payment);
    }

    public PublicBookingPaymentDetailResponse getPublicPaymentDetail(UUID paymentId) {
        PublicBookingPaymentDetailRecord detail = repository.findPaymentDetail(paymentId);
        return new PublicBookingPaymentDetailResponse(
                detail.id(), detail.bookingId(), detail.provider(), detail.providerPaymentId(),
                detail.amount(), detail.currency(), detail.status(),
                detail.checkoutUrl(), detail.checkoutExpiresAt(), detail.manual(),
                detail.approvedAt(), detail.rejectedAt(), detail.expiredAt(), detail.refundedAt(),
                detail.createdAt(),
                detail.bookingStatus(), detail.bookingPaymentStatus(),
                detail.subject(), detail.serviceName(), detail.professionalName(), detail.roomName(),
                detail.startsAt(), detail.durationMinutes(), detail.locationName(), detail.customerName());
    }

    @Transactional
    public BookingPaymentResponse simulatePayment(UUID paymentId, String action) {
        BookingPaymentRecord payment = repository.findById(paymentId);
        if (!"PENDING".equals(payment.status())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "BOOKING_PAYMENT_ALREADY_PROCESSED",
                    "El pago ya fue procesado (estado: " + payment.status() + "). No se puede simular.",
                    Map.of("paymentStatus", payment.status()));
        }
        if (!"SIMULATED".equalsIgnoreCase(payment.provider())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "BOOKING_PAYMENT_NOT_SIMULATED",
                    "Solo se puede simular pagos del proveedor SIMULATED.",
                    Map.of("provider", payment.provider()));
        }
        String normalizedAction = action != null ? action.trim().toUpperCase(java.util.Locale.ROOT) : "";
        String targetStatus = switch (normalizedAction) {
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "BOOKING_PAYMENT_INVALID_ACTION",
                    "Accion de simulacion no valida. Use APPROVED o REJECTED.",
                    Map.of("action", normalizedAction));
        };

        var metadata = new java.util.LinkedHashMap<String, Object>();
        metadata.put("source", "PUBLIC_SIMULATION");
        metadata.put("simulatedAction", normalizedAction);
        metadata.put("simulated", true);

        String rawPayloadJson = "{}";
        String metadataJson = toJson(metadata);

        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);

        BookingPaymentRecord updated = repository.updatePaymentStatus(
                paymentId, targetStatus, rawPayloadJson, metadataJson, now);
        repository.recalculateBookingPaymentStatus(payment.businessId(), payment.bookingId());
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(payment.businessId(), payment.bookingId());
        if ("APPROVED".equals(targetStatus) && isPaidOrOverpaid(booking)) {
            transitionToConfirmed(payment.businessId(), payment.bookingId(), booking);
        }
        if ("APPROVED".equals(targetStatus)) {
            sendPostPaymentNotifications(payment.businessId(), updated);
        }
        return toResponse(updated);
    }

    private void validateSignature(String rawBody, String timestampHeader, String signatureHeader) {
        if (!properties.isWebhookSignatureEnabled()) {
            return;
        }
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()
                || timestampHeader == null || signatureHeader == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_SIGNATURE_REQUIRED",
                    "Faltan credenciales de firma del webhook de pago.",
                    Map.of("signature", "Firma requerida."));
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_TIMESTAMP_INVALID",
                    "El timestamp del webhook de pago es invalido.",
                    Map.of("timestamp", "Timestamp invalido."));
        }
        long skewSeconds = Math.abs(OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond() - timestamp);
        if (skewSeconds > properties.getWebhookToleranceSeconds()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_TIMESTAMP_EXPIRED",
                    "El timestamp del webhook de pago esta fuera de tolerancia.",
                    Map.of("timestamp", "Timestamp fuera de tolerancia."));
        }
        String expected = hmac(timestampHeader + "." + rawBody);
        if (!MessageDigestSupport.constantTimeEquals(expected, signatureHeader)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_SIGNATURE_INVALID",
                    "La firma del webhook de pago no es valida.",
                    Map.of("signature", "Firma invalida."));
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo calcular firma HMAC de pago.", exception);
        }
    }

    private BookingPaymentWebhookRequest parseWebhookBody(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, BookingPaymentWebhookRequest.class);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "INVALID_PAYMENT_WEBHOOK_BODY",
                    "El cuerpo del webhook de pago no es valido.",
                    Map.of("body", "Cuerpo no procesable."));
        }
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(sanitizeMetadata(metadata));
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "INVALID_PAYMENT_METADATA",
                    "La metadata del pago no es valida.",
                    Map.of("metadata", "Metadata no serializable."));
        }
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, sanitizeMetadataValue(value));
            }
        });
        return sanitized;
    }

    private Object sanitizeMetadataValue(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((nestedKey, nestedValue) -> {
                if (nestedKey != null && nestedValue != null) {
                    sanitized.put(String.valueOf(nestedKey), sanitizeMetadataValue(nestedValue));
                }
            });
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new java.util.ArrayList<>();
            iterable.forEach(item -> {
                if (item != null) {
                    sanitized.add(sanitizeMetadataValue(item));
                }
            });
            return sanitized;
        }
        return value;
    }

    private void sendPaymentLinkNotifications(AuthenticatedUser user, BookingPaymentRecord payment, CreateBookingPaymentLinkRequest request) {
        boolean sendWhatsApp = request != null && request.sendWhatsApp() != null ? request.sendWhatsApp() : properties.isDispatchWhatsApp();
        boolean sendEmail = request != null && request.sendEmail() != null ? request.sendEmail() : properties.isDispatchEmail();
        if (!sendWhatsApp && !sendEmail) {
            return;
        }
        BookingPaymentNotificationRecord booking = repository.findNotificationContext(user.businessId(), payment.bookingId());
        if (sendEmail) {
            sendPaymentEmail(booking, payment);
        }
        if (sendWhatsApp) {
            sendPaymentWhatsApp(user, booking, payment);
        }
    }

    private void sendPaymentEmail(BookingPaymentNotificationRecord booking, BookingPaymentRecord payment) {
        String body = bookingEmailService.buildAppointmentEmailBody(
                booking.customerName(),
                "Tu reserva requiere abono para quedar lista.",
                booking.serviceName() == null ? booking.subject() : booking.serviceName(),
                formatDateTime(booking.startsAt()),
                booking.locationName() == null ? booking.location() : booking.locationName(),
                booking.professionalName(),
                booking.roomName(),
                payment.checkoutUrl(),
                "Monto: " + payment.currency() + " " + payment.amount()
                        + ". Enlace vigente hasta: " + formatDateTime(payment.checkoutExpiresAt()) + ".");
        bookingEmailService.sendBookingEmail(booking.businessId(), booking.bookingId(), booking.customerEmail(),
                "BOOKING_PAYMENT_LINK", "Abono de reserva pendiente", body);
        auditService.record(booking.businessId(), null, "BOOKING_PAYMENT_EMAIL_SENT", "BOOKING", booking.bookingId(),
                "Correo de link de pago generado.",
                AuditMetadata.of("paymentId", payment.id(), "checkoutExpiresAt", payment.checkoutExpiresAt()));
    }

    private void sendPaymentWhatsApp(AuthenticatedUser user, BookingPaymentNotificationRecord booking, BookingPaymentRecord payment) {
        String body = buildPaymentWhatsAppMessage(booking, payment);
        try {
            channelDispatchService.dispatch(new ChannelDispatchRequest(
                    booking.businessId(), MessageChannelType.WHATSAPP, booking.customerPhone(), body));
            auditService.record(booking.businessId(), user.userId(), "BOOKING_PAYMENT_WHATSAPP_SENT", "BOOKING", booking.bookingId(),
                    "Link de pago enviado por WhatsApp.",
                    AuditMetadata.of("paymentId", payment.id(), "checkoutUrl", payment.checkoutUrl()));
        } catch (RuntimeException exception) {
            auditService.record(booking.businessId(), user.userId(), "BOOKING_PAYMENT_WHATSAPP_SEND_FAILED", "BOOKING", booking.bookingId(),
                    "Fallo envio WhatsApp de link de pago: " + safeMessage(exception),
                    AuditMetadata.of("paymentId", payment.id()));
        }
    }

    private String buildPaymentWhatsAppMessage(BookingPaymentNotificationRecord booking, BookingPaymentRecord payment) {
        return """
                Abono de reserva pendiente

                Cliente: %s
                Servicio: %s
                Fecha: %s
                Monto: %s %s
                Paga aqui: %s
                Vigente hasta: %s
                """.formatted(
                valueOrFallback(booking.customerName(), "Cliente"),
                valueOrFallback(booking.serviceName(), booking.subject()),
                formatDateTime(booking.startsAt()),
                payment.currency(), payment.amount(),
                payment.checkoutUrl(),
                formatDateTime(payment.checkoutExpiresAt()));
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "Por confirmar" : value.toLocalDate() + " " + value.toLocalTime();
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "sin detalle" : exception.getMessage();
    }

    private void recordManualPaymentAudit(AuthenticatedUser user, BookingPaymentRecord payment, BookingPaymentBookingRecord booking) {
        String eventName = switch (payment.status()) {
            case "APPROVED" -> "BOOKING_PAYMENT_APPROVED_MANUAL";
            case "REJECTED" -> "BOOKING_PAYMENT_REJECTED_MANUAL";
            case "EXPIRED" -> "BOOKING_PAYMENT_EXPIRED_MANUAL";
            case "REFUNDED" -> "BOOKING_PAYMENT_REFUNDED_MANUAL";
            default -> "BOOKING_PAYMENT_PENDING_MANUAL";
        };
        auditService.record(user.businessId(), user.userId(), eventName, "BOOKING", payment.bookingId(),
                "Pago manual de reserva registrado.",
                AuditMetadata.of(
                        "paymentId", payment.id(), "provider", payment.provider(),
                        "providerPaymentId", payment.providerPaymentId(), "idempotencyKey", payment.idempotencyKey(),
                        "amount", payment.amount(), "currency", payment.currency(), "paymentStatus", payment.status(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "bookingPaymentStatus", booking.paymentStatus()));
    }

    private BookingPaymentResponse toResponse(BookingPaymentRecord payment) {
        return new BookingPaymentResponse(
                payment.id(), payment.bookingId(), payment.provider(), payment.providerPaymentId(),
                payment.idempotencyKey(), payment.amount(), payment.currency(), payment.status(),
                payment.checkoutUrl(), payment.checkoutExpiresAt(), payment.manual(),
                payment.approvedAt(), payment.rejectedAt(), payment.expiredAt(), payment.refundedAt(),
                payment.createdAt());
    }

    private String normalizeProvider(String provider) {
        return normalizeProvider(provider, null);
    }

    private String normalizeProvider(String provider, String defaultProvider) {
        if ((provider == null || provider.isBlank()) && defaultProvider != null) {
            return defaultProvider;
        }
        if (provider == null || provider.isBlank()) {
            throw validationError("provider", "El proveedor de pago es obligatorio.");
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 60) {
            throw validationError("provider", "El proveedor de pago supera el largo permitido.");
        }
        return normalized;
    }

    private String normalizeCurrency(String currency) {
        String resolved = currency == null || currency.isBlank() ? "CLP" : currency.trim().toUpperCase(Locale.ROOT);
        if (resolved.length() != 3) {
            throw validationError("currency", "La moneda debe usar codigo ISO de 3 letras.");
        }
        return resolved;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw validationError("status", "El estado del pago es obligatorio.");
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "APPROVED", "PAID", "SUCCEEDED", "SUCCESS", "COMPLETED" -> "APPROVED";
            case "REJECTED", "FAILED", "DECLINED", "CANCELED", "CANCELLED" -> "REJECTED";
            case "EXPIRED" -> "EXPIRED";
            case "REFUNDED" -> "REFUNDED";
            case "PENDING", "CREATED", "PROCESSING" -> "PENDING";
            default -> throw validationError("status", "El estado del pago no es soportado.");
        };
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private boolean isTerminal(String status) {
        return "APPROVED".equals(status) || "REJECTED".equals(status) || "EXPIRED".equals(status) || "REFUNDED".equals(status);
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, Map.of(field, message));
    }

    private record NormalizedPayment(
            UUID businessId, UUID bookingId, String provider, String providerPaymentId,
            String idempotencyKey, BigDecimal amount, String currency, String status,
            OffsetDateTime occurredAt, Map<String, Object> metadata) {
    }

    private static final class MessageDigestSupport {
        private MessageDigestSupport() {
        }

        private static boolean constantTimeEquals(String expected, String actual) {
            if (expected == null || actual == null) {
                return false;
            }
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8));
        }
    }
}
