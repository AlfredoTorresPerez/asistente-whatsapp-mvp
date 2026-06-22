package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.api.BookingPaymentWebhookRequest;
import com.asistentewhatsapp.bookings.api.BookingPaymentWebhookResponse;
import com.asistentewhatsapp.bookings.api.BookingPaymentResponse;
import com.asistentewhatsapp.bookings.api.CreateBookingPaymentLinkRequest;
import com.asistentewhatsapp.bookings.api.RefundBookingPaymentRequest;
import com.asistentewhatsapp.bookings.api.RegisterBookingManualPaymentRequest;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentBookingRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentNotificationRecord;
import com.asistentewhatsapp.bookings.infrastructure.BookingPaymentJdbcRepository.BookingPaymentRecord;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.application.AuditMetadata;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingPaymentService {

    private final BookingPaymentJdbcRepository repository;
    private final BookingPaymentProperties properties;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ChannelDispatchService channelDispatchService;
    private final BookingEmailService bookingEmailService;

    public BookingPaymentService(
            BookingPaymentJdbcRepository repository,
            BookingPaymentProperties properties,
            AuditService auditService,
            ObjectMapper objectMapper,
            ChannelDispatchService channelDispatchService,
            BookingEmailService bookingEmailService) {
        this.repository = repository;
        this.properties = properties;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.channelDispatchService = channelDispatchService;
        this.bookingEmailService = bookingEmailService;
    }

    @Transactional
    public BookingPaymentWebhookResponse handleWebhook(String rawBody, String timestampHeader, String signatureHeader) {
        validateSignature(rawBody, timestampHeader, signatureHeader);
        BookingPaymentWebhookRequest request = parse(rawBody);
        NormalizedPayment normalized = normalize(request);
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(normalized.businessId(), normalized.bookingId());
        Optional<BookingPaymentRecord> existing = repository.findExisting(
                normalized.businessId(),
                normalized.provider(),
                normalized.providerPaymentId(),
                normalized.idempotencyKey());
        if (existing.isPresent() && existing.get().status().equals(normalized.status())) {
            return new BookingPaymentWebhookResponse(
                    existing.get().id(),
                    existing.get().bookingId(),
                    booking.paymentStatus(),
                    BookingStateMachine.canonical(booking.bookingStatus()),
                    true,
                    false);
        }

        String rawPayloadJson = rawBody == null || rawBody.isBlank() ? "{}" : rawBody;
        String metadataJson = toJson(normalized.metadata());
        BookingPaymentRecord payment = existing
                .map(current -> updateExistingPayment(current, normalized, rawPayloadJson, metadataJson))
                .orElseGet(() -> repository.insertPayment(
                        normalized.businessId(),
                        normalized.bookingId(),
                        normalized.provider(),
                        normalized.providerPaymentId(),
                        normalized.idempotencyKey(),
                        normalized.amount(),
                        normalized.currency(),
                        normalized.status(),
                        rawPayloadJson,
                        metadataJson,
                        normalized.occurredAt()));

        repository.recalculateBookingPaymentStatus(normalized.businessId(), normalized.bookingId());
        BookingPaymentBookingRecord updatedBooking = repository.findBookingForUpdate(normalized.businessId(), normalized.bookingId());
        recordPaymentAudit(payment, updatedBooking, existing.isPresent());
        return new BookingPaymentWebhookResponse(
                payment.id(),
                payment.bookingId(),
                updatedBooking.paymentStatus(),
                BookingStateMachine.canonical(updatedBooking.bookingStatus()),
                false,
                false);
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
        String provider = normalizeProvider(request == null ? null : request.provider(), "SIMULATED");
        String currency = normalizeCurrency(request == null ? null : request.currency());
        int expirationMinutes = request != null && request.expirationMinutes() != null
                ? Math.min(Math.max(request.expirationMinutes(), 5), 1440)
                : Math.min(Math.max(properties.getCheckoutExpirationMinutes(), 5), 1440);
        OffsetDateTime expiresAt = now.plusMinutes(expirationMinutes);
        String idempotencyKey = "checkout:" + bookingId + ":" + now.toEpochSecond();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "INTERNAL_CHECKOUT_LINK");
        metadata.put("actorUserId", user.userId());
        metadata.put("bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()));
        metadata.put("bookingPaymentStatus", booking.paymentStatus());
        if (request != null && request.metadata() != null) {
            metadata.putAll(request.metadata());
        }

        BookingPaymentRecord payment = repository.insertCheckoutPayment(
                user.businessId(),
                bookingId,
                provider,
                idempotencyKey,
                amount,
                currency,
                resolveCheckoutUrlTemplate(provider),
                expiresAt,
                toJson(metadata));
        auditService.record(user.businessId(), user.userId(), "BOOKING_PAYMENT_LINK_CREATED", "BOOKING", bookingId,
                "Se genero enlace de pago para reserva.",
                AuditMetadata.of(
                        "paymentId", payment.id(),
                        "provider", payment.provider(),
                        "amount", payment.amount(),
                        "currency", payment.currency(),
                        "checkoutExpiresAt", payment.checkoutExpiresAt(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "bookingPaymentStatus", booking.paymentStatus()));
        sendPaymentLinkNotifications(user, payment, request);
        return toResponse(payment);
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
                user.businessId(),
                bookingId,
                provider,
                providerPaymentId,
                idempotencyKey,
                amount,
                normalizeCurrency(request.currency()),
                status,
                "{}",
                toJson(metadata),
                occurredAt);
        repository.recalculateBookingPaymentStatus(user.businessId(), bookingId);
        BookingPaymentBookingRecord updatedBooking = repository.findBookingForUpdate(user.businessId(), bookingId);
        recordManualPaymentAudit(user, payment, updatedBooking);
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
                    payment.id(),
                    "EXPIRED",
                    "{}",
                    toJson(AuditMetadata.of(
                            "source", "BOOKING_PAYMENT_EXPIRATION_SCHEDULER",
                            "checkoutExpiresAt", payment.checkoutExpiresAt())),
                    now);
            repository.recalculateBookingPaymentStatus(updated.businessId(), updated.bookingId());
            auditService.record(updated.businessId(), null, "BOOKING_PAYMENT_LINK_EXPIRED", "BOOKING", updated.bookingId(),
                    "Link de pago de reserva expirado automaticamente.",
                    AuditMetadata.of(
                            "paymentId", updated.id(),
                            "provider", updated.provider(),
                            "amount", updated.amount(),
                            "currency", updated.currency(),
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
        Map<String, Object> metadata = AuditMetadata.of(
                "source", "INTERNAL_REFUND",
                "actorUserId", user.userId(),
                "reason", request == null ? null : request.reason(),
                "previousStatus", current.status(),
                "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()));
        BookingPaymentRecord payment = repository.updatePaymentStatus(
                current.id(),
                "REFUNDED",
                "{}",
                toJson(metadata),
                OffsetDateTime.now(ZoneOffset.UTC));
        repository.recalculateBookingPaymentStatus(user.businessId(), bookingId);
        auditService.record(user.businessId(), user.userId(), "BOOKING_PAYMENT_REFUNDED", "BOOKING", bookingId,
                "Se registro reembolso de pago de reserva.",
                AuditMetadata.of(
                        "paymentId", payment.id(),
                        "provider", payment.provider(),
                        "amount", payment.amount(),
                        "currency", payment.currency(),
                        "reason", request == null ? null : request.reason()));
        return toResponse(payment);
    }

    private BookingPaymentRecord updateExistingPayment(
            BookingPaymentRecord current,
            NormalizedPayment normalized,
            String rawPayloadJson,
            String metadataJson) {
        if (isTerminal(current.status())) {
            return current;
        }
        return repository.updatePaymentStatus(current.id(), normalized.status(), rawPayloadJson, metadataJson, normalized.occurredAt());
    }

    private void recordPaymentAudit(BookingPaymentRecord payment, BookingPaymentBookingRecord booking, boolean update) {
        String eventName = switch (payment.status()) {
            case "APPROVED" -> "BOOKING_PAYMENT_APPROVED";
            case "REJECTED" -> "BOOKING_PAYMENT_REJECTED";
            case "EXPIRED" -> "BOOKING_PAYMENT_EXPIRED";
            case "REFUNDED" -> "BOOKING_PAYMENT_REFUNDED";
            default -> "BOOKING_PAYMENT_PENDING";
        };
        boolean bookingClosed = BookingStateMachine.isClosed(booking.bookingStatus());
        auditService.record(payment.businessId(), null, eventName, "BOOKING", payment.bookingId(),
                "Webhook de pago de reserva procesado.",
                AuditMetadata.of(
                        "paymentId", payment.id(),
                        "provider", payment.provider(),
                        "providerPaymentId", payment.providerPaymentId(),
                        "idempotencyKey", payment.idempotencyKey(),
                        "amount", payment.amount(),
                        "currency", payment.currency(),
                        "paymentStatus", payment.status(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "bookingPaymentStatus", booking.paymentStatus(),
                        "requiresDeposit", booking.requiresDeposit(),
                        "depositAmount", booking.depositAmount(),
                        "lateOrClosedBooking", bookingClosed,
                        "updatedExistingPayment", update));
    }

    private BookingPaymentWebhookRequest parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, BookingPaymentWebhookRequest.class);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "INVALID_PAYMENT_WEBHOOK_PAYLOAD",
                    "El payload del webhook de pago no es valido.",
                    Map.of("payload", "JSON invalido."));
        }
    }

    private NormalizedPayment normalize(BookingPaymentWebhookRequest request) {
        if (request == null || request.businessId() == null || request.bookingId() == null) {
            throw validationError("bookingId", "El pago debe indicar negocio y reserva.");
        }
        String provider = normalizeProvider(request.provider());
        String providerPaymentId = normalizeOptional(request.providerPaymentId(), 160);
        String idempotencyKey = normalizeOptional(request.idempotencyKey(), 160);
        if (providerPaymentId == null && idempotencyKey == null) {
            throw validationError("idempotencyKey", "El webhook debe incluir providerPaymentId o idempotencyKey.");
        }
        BigDecimal amount = request.amount();
        if (amount == null || amount.signum() < 0) {
            throw validationError("amount", "El monto del pago no puede ser negativo.");
        }
        String currency = request.currency() == null || request.currency().isBlank()
                ? "CLP"
                : request.currency().trim().toUpperCase(Locale.ROOT);
        if (currency.length() != 3) {
            throw validationError("currency", "La moneda debe usar codigo ISO de 3 letras.");
        }
        return new NormalizedPayment(
                request.businessId(),
                request.bookingId(),
                provider,
                providerPaymentId,
                idempotencyKey,
                amount,
                currency,
                normalizeStatus(request.status()),
                request.occurredAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : request.occurredAt(),
                request.metadata() == null ? Map.of() : request.metadata());
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
        String resolved = currency == null || currency.isBlank()
                ? "CLP"
                : currency.trim().toUpperCase(Locale.ROOT);
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

    private String resolveCheckoutUrlTemplate(String provider) {
        if (!"SIMULATED".equals(provider)
                && properties.getExternalCheckoutUrlTemplate() != null
                && !properties.getExternalCheckoutUrlTemplate().isBlank()) {
            return properties.getExternalCheckoutUrlTemplate().trim();
        }
        String value = properties.getCheckoutPublicBaseUrl();
        return (value == null || value.isBlank() ? "http://localhost:5173/reservas/pagar" : value.trim()).replaceAll("/+$", "");
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
                "Tu reserva requiere abono para quedar lista para confirmacion.",
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
                "Correo de link de pago generado o simulado.",
                AuditMetadata.of("paymentId", payment.id(), "checkoutExpiresAt", payment.checkoutExpiresAt()));
    }

    private void sendPaymentWhatsApp(AuthenticatedUser user, BookingPaymentNotificationRecord booking, BookingPaymentRecord payment) {
        String body = buildPaymentWhatsAppMessage(booking, payment);
        try {
            channelDispatchService.dispatch(new ChannelDispatchRequest(
                    booking.businessId(),
                    MessageChannelType.WHATSAPP,
                    booking.customerPhone(),
                    body));
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
                payment.currency(),
                payment.amount(),
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
                        "paymentId", payment.id(),
                        "provider", payment.provider(),
                        "providerPaymentId", payment.providerPaymentId(),
                        "idempotencyKey", payment.idempotencyKey(),
                        "amount", payment.amount(),
                        "currency", payment.currency(),
                        "paymentStatus", payment.status(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "bookingPaymentStatus", booking.paymentStatus(),
                        "lateOrClosedBooking", BookingStateMachine.isClosed(booking.bookingStatus())));
    }

    private BookingPaymentResponse toResponse(BookingPaymentRecord payment) {
        return new BookingPaymentResponse(
                payment.id(),
                payment.bookingId(),
                payment.provider(),
                payment.providerPaymentId(),
                payment.idempotencyKey(),
                payment.amount(),
                payment.currency(),
                payment.status(),
                payment.checkoutUrl(),
                payment.checkoutExpiresAt(),
                payment.manual(),
                payment.approvedAt(),
                payment.rejectedAt(),
                payment.expiredAt(),
                payment.refundedAt(),
                payment.createdAt());
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, Map.of(field, message));
    }

    private record NormalizedPayment(
            UUID businessId,
            UUID bookingId,
            String provider,
            String providerPaymentId,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String status,
            OffsetDateTime occurredAt,
            Map<String, Object> metadata) {
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
