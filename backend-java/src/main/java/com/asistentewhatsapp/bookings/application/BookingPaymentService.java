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
import com.asistentewhatsapp.bookings.infrastructure.MercadoPagoPaymentProvider;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingPaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingPaymentService.class);

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
    public BookingPaymentWebhookResponse handleWebhook(String rawBody, String provider, Map<String, String> headers) {
        String detectedProvider = provider != null ? provider : properties.getProvider();
        if (detectedProvider == null || detectedProvider.isBlank()) {
            detectedProvider = "SIMULATED";
        }
        BookingPaymentProvider paymentProvider = providerRegistry.getProvider(detectedProvider);

        // Validate provider-specific signature
        if (MercadoPagoPaymentProvider.NAME.equals(detectedProvider)) {
            validateMercadoPagoSignature(rawBody, headers);
        } else if (properties.isWebhookSignatureEnabled()) {
            String timestamp = headers.get("X-Booking-Payment-Timestamp");
            String signature = headers.get("X-Booking-Payment-Signature");
            validateSignature(rawBody, timestamp, signature);
        }

        if (paymentProvider.supportsWebhook()) {
            Optional<BookingPaymentProvider.PaymentNotification> notification = paymentProvider.parseWebhook(rawBody, headers);
            if (notification.isPresent()) {
                return processProviderNotification(notification.get(), rawBody, detectedProvider);
            }
        }
        BookingPaymentWebhookRequest request = parseWebhookBody(rawBody);
        return processStandardWebhook(request, rawBody);
    }

    private void validateMercadoPagoSignature(String rawBody, Map<String, String> headers) {
        if (!properties.isWebhookSignatureEnabled()) return;
        String webhookSecret = properties.getMercadopago() != null ? properties.getMercadopago().getWebhookSecret() : "";
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_SIGNATURE_REQUIRED",
                    "Falta el secreto del webhook de Mercado Pago.",
                    Map.of("signature", "Firma requerida."));
        }
        String signature = headers.get("x-signature");
        String requestId = headers.get("x-request-id");
        if (signature == null || requestId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_SIGNATURE_REQUIRED",
                    "Faltan cabeceras de firma de Mercado Pago (x-signature, x-request-id).",
                    Map.of("signature", "Firma requerida."));
        }
        if (!verifyMercadoPagoSignature(rawBody, signature, requestId, webhookSecret)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_SIGNATURE_INVALID",
                    "La firma del webhook de Mercado Pago no es valida.",
                    Map.of("signature", "Firma invalida."));
        }
    }

    private boolean verifyMercadoPagoSignature(String payload, String signatureHeader, String requestId, String secret) {
        try {
            String[] parts = signatureHeader.split(",");
            String ts = null;
            String v1 = null;
            for (String part : parts) {
                if (part.startsWith("ts=")) ts = part.substring(3);
                else if (part.startsWith("v1=")) v1 = part.substring(3);
            }
            if (ts == null || v1 == null) return false;

            long timestamp = Long.parseLong(ts);
            long now = System.currentTimeMillis() / 1000;
            if (Math.abs(now - timestamp) > properties.getWebhookToleranceSeconds()) {
                return false;
            }

            String manifest = payload + "|" + requestId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(hash);
            return MessageDigest.isEqual(v1.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.warn("Mercado Pago signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private BookingPaymentWebhookResponse processProviderNotification(
            BookingPaymentProvider.PaymentNotification notification, String rawBody, String providerName) {
        String rawPayloadJson = rawBody == null || rawBody.isBlank() ? "{}" : rawBody;
        return doProcessPayment(
                null, null,
                notification.providerPaymentId(),
                notification.providerPreferenceId(),
                notification.providerExternalReference(),
                notification.idempotencyKey(),
                notification.amount(),
                notification.currency(),
                notification.status(),
                notification.statusDetail(),
                notification.rawStatus(),
                notification.paymentMethodId(),
                notification.installments(),
                notification.payerEmail(),
                notification.occurredAt(),
                notification.metadata(),
                providerName,
                rawPayloadJson);
    }

    private BookingPaymentWebhookResponse processStandardWebhook(BookingPaymentWebhookRequest request, String rawBody) {
        String rawPayloadJson = rawBody == null || rawBody.isBlank() ? "{}" : rawBody;
        String provider = normalizeProvider(request.provider());
        return doProcessPayment(
                request.businessId(),
                request.bookingId(),
                request.providerPaymentId(),
                null,
                null,
                request.idempotencyKey(),
                request.amount(),
                request.currency(),
                request.status(),
                null,
                null,
                null,
                null,
                null,
                request.occurredAt(),
                request.metadata() == null ? Map.of() : request.metadata(),
                provider,
                rawPayloadJson);
    }

    private record IntegratePaymentResult(BookingPaymentRecord payment, boolean duplicate, String bookingStatus) {}

    private BookingPaymentWebhookResponse doProcessPayment(
            UUID businessId, UUID bookingId,
            String providerPaymentId, String providerPreferenceId, String providerExternalReference,
            String idempotencyKey, BigDecimal amount, String currency,
            String status, String statusDetail, String rawStatus,
            String paymentMethodId, Integer installments, String payerEmail,
            OffsetDateTime occurredAt, Map<String, Object> metadata,
            String provider, String rawPayloadJson) {
        if (providerPaymentId == null && providerExternalReference == null && idempotencyKey == null) {
            throw validationError("idempotencyKey",
                    "El webhook debe incluir providerPaymentId, providerExternalReference o idempotencyKey.");
        }
        if (amount == null || amount.signum() < 0) {
            throw validationError("amount", "El monto del pago no puede ser negativo.");
        }
        String resolvedCurrency = currency == null || currency.isBlank() ? "CLP"
                : currency.trim().toUpperCase(Locale.ROOT);
        if (resolvedCurrency.length() != 3) {
            throw validationError("currency", "La moneda debe usar codigo ISO de 3 letras.");
        }
        String resolvedStatus = normalizeStatus(status);
        OffsetDateTime resolvedOccurredAt = occurredAt == null
                ? OffsetDateTime.now(ZoneOffset.UTC) : occurredAt;

        // resolver businessId/bookingId desde externalReference si no vienen
        if (businessId == null && bookingId == null && providerExternalReference != null) {
            BookingPaymentRecord resolved = repository.findByProviderExternalReference(
                    provider, providerExternalReference);
            if (resolved != null) {
                businessId = resolved.businessId();
                bookingId = bookingId != null ? bookingId : resolved.bookingId();
            }
        }
        // resolver bookingId desde providerPaymentId si aun no
        if (businessId == null && bookingId == null && providerPaymentId != null) {
            BookingPaymentRecord resolved = repository.findByProviderPaymentId(
                    provider, providerPaymentId);
            if (resolved != null) {
                businessId = resolved.businessId();
                bookingId = bookingId != null ? bookingId : resolved.bookingId();
            }
        }
        // resolver bookingId desde preferenceId
        if (businessId == null && bookingId == null && providerPreferenceId != null) {
            BookingPaymentRecord resolved = repository.findByProviderPreferenceId(
                    provider, providerPreferenceId);
            if (resolved != null) {
                businessId = resolved.businessId();
                bookingId = bookingId != null ? bookingId : resolved.bookingId();
            }
        }
        if (businessId == null || bookingId == null) {
            throw validationError("providerPaymentId",
                    "No se pudo resolver la reserva desde el webhook. Falta referencia externa o idempotency.");
        }

        IntegratePaymentResult result = integratePayment(
                businessId, bookingId, provider, providerPaymentId,
                providerPreferenceId, providerExternalReference,
                idempotencyKey, amount, resolvedCurrency, resolvedStatus,
                statusDetail, rawStatus, paymentMethodId, installments, payerEmail,
                resolvedOccurredAt, metadata, rawPayloadJson);
        return new BookingPaymentWebhookResponse(
                result.payment().id(),
                result.payment().bookingId(),
                result.payment().status(),
                result.bookingStatus(),
                result.duplicate(),
                BookingStateMachine.CONFIRMED.equals(result.bookingStatus()));
    }

    private IntegratePaymentResult integratePayment(
            UUID businessId, UUID bookingId, String provider,
            String providerPaymentId, String providerPreferenceId,
            String providerExternalReference, String idempotencyKey,
            BigDecimal amount, String currency, String status,
            String statusDetail, String rawStatus,
            String paymentMethodId, Integer installments, String payerEmail,
            OffsetDateTime occurredAt, Map<String, Object> metadata, String rawPayloadJson) {
        String metadataJson = toJson(metadata);
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(businessId, bookingId);
        Optional<BookingPaymentRecord> existing = repository.findExisting(
                businessId, provider, providerPaymentId, providerExternalReference, idempotencyKey);
        if (existing.isPresent() && existing.get().status().equals(status)) {
            return new IntegratePaymentResult(existing.get(), true, booking.bookingStatus());
        }
        if (existing.isPresent() && isTerminal(existing.get().status())) {
            return new IntegratePaymentResult(existing.get(), true, booking.bookingStatus());
        }
        BookingPaymentRecord payment;
        if (existing.isPresent()) {
            payment = repository.updatePaymentProviderStatus(
                    existing.get().id(), status, rawPayloadJson, metadataJson,
                    occurredAt, statusDetail, rawStatus, paymentMethodId, installments, payerEmail);
        } else {
            if (providerPaymentId == null && idempotencyKey == null) {
                idempotencyKey = "webhook:" + provider + ":" + UUID.randomUUID();
            }
            payment = repository.insertPayment(
                    businessId, bookingId, provider,
                    providerPaymentId, providerPreferenceId, providerExternalReference,
                    idempotencyKey, amount, currency, status,
                    rawPayloadJson, metadataJson, occurredAt,
                    statusDetail, rawStatus, paymentMethodId, installments, payerEmail,
                    "DEPOSIT");
        }
        repository.recalculateBookingPaymentStatus(businessId, bookingId);
        BookingPaymentBookingRecord updatedBooking = repository.findBookingForUpdate(businessId, bookingId);
        recordPaymentAudit(existing.isPresent() ? existing.get() : payment, updatedBooking, existing.isPresent());
        if ("APPROVED".equals(status) && isPaidOrOverpaid(updatedBooking)) {
            transitionToConfirmed(businessId, bookingId, updatedBooking);
        }
        if ("APPROVED".equals(status)) {
            sendPostPaymentNotifications(businessId, payment);
        }
        return new IntegratePaymentResult(payment, false, updatedBooking.bookingStatus());
    }

    private boolean isPaidOrOverpaid(BookingPaymentBookingRecord booking) {
        if (!booking.requiresDeposit()) return true;
        return repository.hasApprovedRequiredDeposit(booking.businessId(), booking.bookingId());
    }

    private void transitionToConfirmed(UUID businessId, UUID bookingId, BookingPaymentBookingRecord booking) {
        if (BookingStateMachine.isClosed(booking.bookingStatus())) {
            LOGGER.warn("BOOKING_AUTO_CONFIRM_SKIPPED bookingId={} status={} reason=cancelled/expired",
                    bookingId, booking.bookingStatus());
            auditService.record(businessId, null, "BOOKING_AUTO_CONFIRM_SKIPPED_CLOSED",
                    "BOOKING", bookingId,
                    "Pago aprobado pero reserva esta cerrada. No se confirma automaticamente.",
                    AuditMetadata.of("bookingStatus", booking.bookingStatus(),
                            "paymentStatus", booking.paymentStatus()));
            return;
        }
        BookingStateMachine.assertTransition(booking.bookingStatus(),
                BookingStateMachine.CONFIRMED, "confirmarse por pago");
        repository.updateBookingStatus(businessId, bookingId,
                BookingStateMachine.CONFIRMED, "PAGO_APROBADO",
                "Reserva confirmada automaticamente por pago aprobado.");
        auditService.record(businessId, null, "BOOKING_AUTO_CONFIRMED_BY_PAYMENT",
                "BOOKING", bookingId,
                "Reserva confirmada automaticamente tras pago aprobado.",
                AuditMetadata.of("previousBookingStatus",
                        BookingStateMachine.canonical(booking.bookingStatus()),
                        "paymentStatus", booking.paymentStatus()));
        try {
            calendarSyncService.syncConfirmed(bookingId, businessId);
        } catch (Exception e) {
            LOGGER.warn("CALENDAR_SYNC_CONFIRMED_FAILED bookingId={}", bookingId, e);
        }
    }

    private void sendPostPaymentNotifications(UUID businessId, BookingPaymentRecord payment) {
        boolean sendWhatsApp = properties.isDispatchPostPaymentWhatsApp();
        boolean sendEmail = properties.isDispatchPostPaymentEmail();
        if (!sendWhatsApp && !sendEmail) return;
        BookingPaymentNotificationRecord booking;
        try {
            booking = repository.findNotificationContext(businessId, payment.bookingId());
        } catch (RuntimeException e) {
            return;
        }
        if (sendEmail) sendPostPaymentEmail(booking, payment);
        if (sendWhatsApp) sendPostPaymentWhatsApp(booking, payment);
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
        bookingEmailService.sendBookingEmail(booking.businessId(), booking.bookingId(),
                booking.customerEmail(),
                "BOOKING_PAYMENT_RECEIVED", "Pago recibido - Reserva confirmada", body);
        auditService.record(booking.businessId(), null, "BOOKING_POST_PAYMENT_EMAIL_SENT",
                "BOOKING", booking.bookingId(),
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
                payment.currency(), payment.amount());
        try {
            channelDispatchService.dispatch(new ChannelDispatchRequest(
                    booking.businessId(), MessageChannelType.WHATSAPP,
                    booking.customerPhone(), body));
            auditService.record(booking.businessId(), null, "BOOKING_POST_PAYMENT_WHATSAPP_SENT",
                    "BOOKING", booking.bookingId(),
                    "WhatsApp de confirmacion de pago enviado.",
                    AuditMetadata.of("paymentId", payment.id()));
        } catch (RuntimeException e) {
            auditService.record(booking.businessId(), null, "BOOKING_POST_PAYMENT_WHATSAPP_FAILED",
                    "BOOKING", booking.bookingId(),
                    "Fallo envio WhatsApp post-pago: " + safeMessage(e),
                    AuditMetadata.of("paymentId", payment.id()));
        }
    }

    @Transactional(readOnly = true)
    public boolean hasApprovedRequiredDeposit(UUID businessId, UUID bookingId) {
        return repository.hasApprovedRequiredDeposit(businessId, bookingId);
    }

    @Transactional
    public List<BookingPaymentResponse> listPayments(AuthenticatedUser user, UUID bookingId) {
        repository.findBookingForUpdate(user.businessId(), bookingId);
        return repository.findPayments(user.businessId(), bookingId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public BookingPaymentResponse createCheckoutLink(
            AuthenticatedUser user, UUID bookingId, CreateBookingPaymentLinkRequest request) {
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(user.businessId(), bookingId);
        if (!booking.requiresDeposit()) {
            throw validationError("requiresDeposit",
                    "La reserva no requiere abono. Usa pago total o pago manual en su lugar.");
        }
        if (BookingStateMachine.isClosed(booking.bookingStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_CLOSED_FOR_PAYMENT_LINK",
                    "No se puede crear un enlace de pago para una reserva cerrada.",
                    Map.of("bookingStatus", BookingStateMachine.canonical(booking.bookingStatus())));
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Optional<BookingPaymentRecord> activeCheckout = repository.findActiveCheckout(
                user.businessId(), bookingId, now);
        if (activeCheckout.isPresent()) {
            sendPaymentLinkNotifications(user, activeCheckout.get(), request);
            return toResponse(activeCheckout.get());
        }
        String paymentPurpose = resolvePaymentPurpose(request);
        BigDecimal amount = resolveCheckoutAmount(request, booking, paymentPurpose);
        if (amount == null || amount.signum() <= 0) {
            throw validationError("amount", "El monto del enlace de pago debe ser mayor a cero.");
        }
        String providerName = normalizeProvider(
                request == null ? null : request.provider(), properties.getProvider());
        String currency = normalizeCurrency(request == null ? null : request.currency());
        int expirationMinutes = request != null && request.expirationMinutes() != null
                ? Math.min(Math.max(request.expirationMinutes(), 5), 1440)
                : Math.min(Math.max(properties.getCheckoutExpirationMinutes(), 5), 1440);

        // Generar paymentId ANTES de llamar al provider
        UUID paymentId = UUID.randomUUID();
        String externalRef = paymentId.toString();
        String idempotencyKey = "checkout:" + paymentId;

        BookingPaymentProvider provider = providerRegistry.getProvider(providerName);
        String description = "Reserva #" + bookingId.toString().substring(0, 8);
        String returnUrl = properties.getCheckoutPublicBaseUrl() + "/" + bookingId;
        String notificationUrl = resolveNotificationUrl();

        BookingPaymentProvider.CreateCheckoutResult checkout = provider.createCheckout(
                user.businessId(), bookingId, paymentId, amount, currency, description,
                returnUrl, notificationUrl, expirationMinutes);
        OffsetDateTime expiresAt = checkout.expiresAt() != null
                ? checkout.expiresAt()
                : now.plusMinutes(expirationMinutes);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "PROVIDER_CHECKOUT_LINK");
        metadata.put("provider", providerName);
        metadata.put("actorUserId", user.userId());
        metadata.put("bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()));
        metadata.put("bookingPaymentStatus", booking.paymentStatus());
        metadata.put("paymentPurpose", paymentPurpose);
        if (checkout.metadata() != null) metadata.putAll(checkout.metadata());
        if (request != null && request.metadata() != null) metadata.putAll(request.metadata());

        BookingPaymentRecord payment = repository.insertPaymentFromCheckout(
                user.businessId(), bookingId, providerName, provider.providerName(),
                checkout.providerPaymentId(),
                checkout.providerPreferenceId(),
                checkout.providerExternalReference(),
                idempotencyKey, amount, currency, paymentPurpose,
                checkout.checkoutUrl(), expiresAt,
                toJson(metadata), paymentId);

        if (checkout.providerPaymentId() != null && !checkout.providerPaymentId().isBlank()) {
            repository.updatePaymentProviderId(paymentId, checkout.providerPaymentId());
        }
        if (checkout.providerPreferenceId() != null && !checkout.providerPreferenceId().isBlank()) {
            repository.updatePaymentPreferenceId(paymentId, checkout.providerPreferenceId());
        }

        // Transicionar reserva a PENDIENTE_PAGO
        if (booking.bookingStatus() != null
                && !BookingStateMachine.PENDING_PAYMENT.equals(
                        BookingStateMachine.canonical(booking.bookingStatus()))) {
            BookingStateMachine.assertTransition(booking.bookingStatus(),
                    BookingStateMachine.PENDING_PAYMENT, "generar link de pago");
            repository.updateBookingStatus(user.businessId(), bookingId,
                    BookingStateMachine.PENDING_PAYMENT, "PAGO_LINK_CREADO",
                    "Reserva pasa a pendiente de pago por generacion de link.");
        }
        auditService.record(user.businessId(), user.userId(), "BOOKING_PAYMENT_LINK_CREATED",
                "BOOKING", bookingId,
                "Se genero enlace de pago para reserva via " + providerName + ".",
                AuditMetadata.of("paymentId", payment.id(), "provider", providerName,
                        "amount", payment.amount(), "currency", payment.currency(),
                        "paymentPurpose", paymentPurpose,
                        "checkoutExpiresAt", payment.checkoutExpiresAt(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "providerPreferenceId", checkout.providerPreferenceId(),
                        "providerExternalReference", checkout.providerExternalReference()));
        sendPaymentLinkNotifications(user, payment, request);
        return toResponse(payment);
    }

    private String resolvePaymentPurpose(CreateBookingPaymentLinkRequest request) {
        if (request == null) return "DEPOSIT";
        if (request.metadata() != null && request.metadata().containsKey("paymentPurpose")) {
            String purpose = String.valueOf(request.metadata().get("paymentPurpose")).trim().toUpperCase(Locale.ROOT);
            if ("FULL".equals(purpose) || "DEPOSIT".equals(purpose) || "MANUAL".equals(purpose)) {
                return purpose;
            }
        }
        return "DEPOSIT";
    }

    private BigDecimal resolveCheckoutAmount(
            CreateBookingPaymentLinkRequest request,
            BookingPaymentBookingRecord booking,
            String paymentPurpose) {
        if (request != null && request.amount() != null) {
            return request.amount();
        }
        if ("FULL".equals(paymentPurpose)) {
            BigDecimal servicePrice = repository.findServicePrice(
                    booking.businessId(), booking.bookingId());
            if (servicePrice != null && servicePrice.signum() > 0) return servicePrice;
            throw validationError("amount",
                    "No hay precio de servicio disponible. Ingresa un monto explicito para pago total.");
        }
        return booking.depositAmount();
    }

    private String resolveNotificationUrl() {
        if (properties.getWebhookPublicUrl() != null && !properties.getWebhookPublicUrl().isBlank()) {
            return properties.getWebhookPublicUrl();
        }
        String baseUrl = properties.getCheckoutPublicBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.replace("/reservas/pagar", "/api/v1/integrations/booking-payments/webhook");
        }
        return "";
    }

    @Transactional
    public BookingPaymentResponse registerManualPayment(
            AuthenticatedUser user, UUID bookingId, RegisterBookingManualPaymentRequest request) {
        if (request == null) throw validationError("payment", "El pago manual es obligatorio.");
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
        Optional<BookingPaymentRecord> existing = repository.findExisting(
                user.businessId(), provider, providerPaymentId, null, idempotencyKey);
        if (existing.isPresent()) return toResponse(existing.get());
        String status = normalizeStatus(request.status() == null || request.status().isBlank()
                ? "APPROVED" : request.status());
        OffsetDateTime occurredAt = request.occurredAt() == null
                ? OffsetDateTime.now(ZoneOffset.UTC) : request.occurredAt();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "INTERNAL_MANUAL_PAYMENT");
        metadata.put("actorUserId", user.userId());
        metadata.put("bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()));
        metadata.put("notes", request.notes());
        if (request.metadata() != null) metadata.putAll(request.metadata());
        BookingPaymentRecord payment = repository.insertManualPayment(
                user.businessId(), bookingId, provider, providerPaymentId, idempotencyKey,
                amount, normalizeCurrency(request.currency()), status,
                "{}", toJson(metadata), occurredAt);
        repository.recalculateBookingPaymentStatus(user.businessId(), bookingId);
        BookingPaymentBookingRecord updatedBooking = repository.findBookingForUpdate(
                user.businessId(), bookingId);
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
            BookingPaymentRecord updated = repository.updatePaymentProviderStatus(
                    payment.id(), "EXPIRED", "{}",
                    toJson(AuditMetadata.of("source", "BOOKING_PAYMENT_EXPIRATION_SCHEDULER",
                            "checkoutExpiresAt", payment.checkoutExpiresAt())),
                    now, null, null, null, null, null);
            repository.recalculateBookingPaymentStatus(updated.businessId(), updated.bookingId());
            auditService.record(updated.businessId(), null, "BOOKING_PAYMENT_LINK_EXPIRED",
                    "BOOKING", updated.bookingId(),
                    "Link de pago de reserva expirado automaticamente.",
                    AuditMetadata.of("paymentId", updated.id(), "provider", updated.provider(),
                            "amount", updated.amount(), "currency", updated.currency(),
                            "checkoutExpiresAt", updated.checkoutExpiresAt()));
        }
    }

    @Transactional
    public BookingPaymentResponse refundPayment(
            AuthenticatedUser user, UUID bookingId, UUID paymentId,
            RefundBookingPaymentRequest request) {
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(
                user.businessId(), bookingId);
        BookingPaymentRecord current = repository.findByIdForBooking(
                user.businessId(), bookingId, paymentId);
        if (!"APPROVED".equals(current.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_PAYMENT_NOT_REFUNDABLE",
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
            if (refundResult.metadata() != null) metadata.putAll(refundResult.metadata());
            BookingPaymentRecord payment = repository.updatePaymentProviderStatus(
                    current.id(), "REFUNDED", "{}", toJson(metadata),
                    OffsetDateTime.now(ZoneOffset.UTC), null, null, null, null, null);
            repository.recalculateBookingPaymentStatus(user.businessId(), bookingId);
            auditService.record(user.businessId(), user.userId(), "BOOKING_PAYMENT_REFUNDED",
                    "BOOKING", bookingId,
                    "Reembolso procesado via " + provider.providerName() + ".",
                    AuditMetadata.of("paymentId", payment.id(), "provider", current.provider(),
                            "amount", current.amount(), "providerRefundId",
                            refundResult.providerRefundId()));
            return toResponse(payment);
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "BOOKING_PAYMENT_REFUND_FAILED",
                    "Error al procesar reembolso: " + safeMessage(e),
                    Map.of("error", safeMessage(e)));
        }
    }

    private void recordPaymentAudit(
            BookingPaymentRecord payment, BookingPaymentBookingRecord booking, boolean update) {
        String eventName = switch (payment.status()) {
            case "APPROVED" -> "BOOKING_PAYMENT_APPROVED";
            case "REJECTED" -> "BOOKING_PAYMENT_REJECTED";
            case "EXPIRED" -> "BOOKING_PAYMENT_EXPIRED";
            case "REFUNDED" -> "BOOKING_PAYMENT_REFUNDED";
            default -> "BOOKING_PAYMENT_PENDING";
        };
        auditService.record(payment.businessId(), null, eventName, "BOOKING", payment.bookingId(),
                "Webhook de pago de reserva procesado.",
                AuditMetadata.of("paymentId", payment.id(), "provider", payment.provider(),
                        "providerPaymentId", payment.providerPaymentId(),
                        "idempotencyKey", payment.idempotencyKey(),
                        "amount", payment.amount(), "currency", payment.currency(),
                        "paymentStatus", payment.status(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "bookingPaymentStatus", booking.paymentStatus(),
                        "requiresDeposit", booking.requiresDeposit(),
                        "depositAmount", booking.depositAmount(),
                        "updatedExistingPayment", update));
    }

    @Transactional(readOnly = true)
    public BookingPaymentResponse getPaymentStatus(UUID paymentId) {
        return toResponse(repository.findById(paymentId));
    }

    @Transactional(readOnly = true)
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
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_PAYMENT_ALREADY_PROCESSED",
                    "El pago ya fue procesado (estado: " + payment.status()
                    + "). No se puede simular.",
                    Map.of("paymentStatus", payment.status()));
        }
        if (!"SIMULATED".equalsIgnoreCase(payment.provider())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BOOKING_PAYMENT_NOT_SIMULATED",
                    "Solo se puede simular pagos del proveedor SIMULATED.",
                    Map.of("provider", payment.provider()));
        }
        String normalizedAction = action != null ? action.trim().toUpperCase(Locale.ROOT) : "";
        String targetStatus = switch (normalizedAction) {
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "BOOKING_PAYMENT_INVALID_ACTION",
                    "Accion de simulacion no valida. Use APPROVED o REJECTED.",
                    Map.of("action", normalizedAction));
        };
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("source", "PUBLIC_SIMULATION");
        metadata.put("simulatedAction", normalizedAction);
        metadata.put("simulated", true);
        BookingPaymentRecord updated = repository.updatePaymentProviderStatus(
                paymentId, targetStatus, "{}", toJson(metadata), now,
                null, null, null, null, null);
        repository.recalculateBookingPaymentStatus(payment.businessId(), payment.bookingId());
        BookingPaymentBookingRecord booking = repository.findBookingForUpdate(
                payment.businessId(), payment.bookingId());
        if ("APPROVED".equals(targetStatus) && isPaidOrOverpaid(booking)) {
            transitionToConfirmed(payment.businessId(), payment.bookingId(), booking);
        }
        if ("APPROVED".equals(targetStatus)) {
            sendPostPaymentNotifications(payment.businessId(), updated);
        }
        return toResponse(updated);
    }

    public BookingPaymentRecord findByExternalReference(String provider, String externalRef) {
        return repository.findByProviderExternalReference(provider, externalRef);
    }

    public BookingPaymentRecord findByProviderPreferenceId(String provider, String preferenceId) {
        return repository.findByProviderPreferenceId(provider, preferenceId);
    }

    // ---- Private helpers ----

    private void validateSignature(String rawBody, String timestampHeader, String signatureHeader) {
        if (!properties.isWebhookSignatureEnabled()) return;
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
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "PAYMENT_WEBHOOK_TIMESTAMP_INVALID",
                    "El timestamp del webhook de pago es invalido.",
                    Map.of("timestamp", "Timestamp invalido."));
        }
        long skewSeconds = Math.abs(
                OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond() - timestamp);
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
            mac.init(new SecretKeySpec(
                    properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + bytesToHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular firma HMAC de pago.", e);
        }
    }

    private BookingPaymentWebhookRequest parseWebhookBody(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, BookingPaymentWebhookRequest.class);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "INVALID_PAYMENT_WEBHOOK_BODY",
                    "El cuerpo del webhook de pago no es valido.",
                    Map.of("body", "Cuerpo no procesable."));
        }
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(sanitizeMetadata(metadata));
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "INVALID_PAYMENT_METADATA",
                    "La metadata del pago no es valida.",
                    Map.of("metadata", "Metadata no serializable."));
        }
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return Map.of();
        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) sanitized.put(key, sanitizeMetadataValue(value));
        });
        return sanitized;
    }

    private Object sanitizeMetadataValue(Object value) {
        if (value instanceof OffsetDateTime dt) return dt.toString();
        if (value instanceof UUID uuid) return uuid.toString();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                if (k != null && v != null) sanitized.put(String.valueOf(k), sanitizeMetadataValue(v));
            });
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new java.util.ArrayList<>();
            iterable.forEach(item -> { if (item != null) sanitized.add(sanitizeMetadataValue(item)); });
            return sanitized;
        }
        return value;
    }

    private void sendPaymentLinkNotifications(
            AuthenticatedUser user, BookingPaymentRecord payment,
            CreateBookingPaymentLinkRequest request) {
        boolean sendWhatsApp = request != null && request.sendWhatsApp() != null
                ? request.sendWhatsApp() : properties.isDispatchWhatsApp();
        boolean sendEmail = request != null && request.sendEmail() != null
                ? request.sendEmail() : properties.isDispatchEmail();
        if (!sendWhatsApp && !sendEmail) return;
        BookingPaymentNotificationRecord booking = repository.findNotificationContext(
                user.businessId(), payment.bookingId());
        if (sendEmail) sendPaymentEmail(booking, payment);
        if (sendWhatsApp) sendPaymentWhatsApp(user, booking, payment);
    }

    private void sendPaymentEmail(BookingPaymentNotificationRecord booking, BookingPaymentRecord payment) {
        String body = bookingEmailService.buildAppointmentEmailBody(
                booking.customerName(),
                "Tu reserva requiere pago para quedar lista.",
                booking.serviceName() == null ? booking.subject() : booking.serviceName(),
                formatDateTime(booking.startsAt()),
                booking.locationName() == null ? booking.location() : booking.locationName(),
                booking.professionalName(),
                booking.roomName(),
                payment.checkoutUrl(),
                "Monto: " + payment.currency() + " " + payment.amount()
                        + ". Enlace vigente hasta: " + formatDateTime(payment.checkoutExpiresAt()) + ".");
        bookingEmailService.sendBookingEmail(booking.businessId(), booking.bookingId(),
                booking.customerEmail(),
                "BOOKING_PAYMENT_LINK", "Pago de reserva pendiente", body);
        auditService.record(booking.businessId(), null, "BOOKING_PAYMENT_EMAIL_SENT",
                "BOOKING", booking.bookingId(),
                "Correo de link de pago generado.",
                AuditMetadata.of("paymentId", payment.id(),
                        "checkoutExpiresAt", payment.checkoutExpiresAt()));
    }

    private void sendPaymentWhatsApp(
            AuthenticatedUser user, BookingPaymentNotificationRecord booking,
            BookingPaymentRecord payment) {
        String body = """
                Pago de reserva pendiente

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
        try {
            channelDispatchService.dispatch(new ChannelDispatchRequest(
                    booking.businessId(), MessageChannelType.WHATSAPP,
                    booking.customerPhone(), body));
            UUID userId = user != null ? user.userId() : null;
            auditService.record(booking.businessId(), userId, "BOOKING_PAYMENT_WHATSAPP_SENT",
                    "BOOKING", booking.bookingId(),
                    "Link de pago enviado por WhatsApp.",
                    AuditMetadata.of("paymentId", payment.id(), "checkoutUrl", payment.checkoutUrl()));
        } catch (RuntimeException e) {
            auditService.record(booking.businessId(), null, "BOOKING_PAYMENT_WHATSAPP_SEND_FAILED",
                    "BOOKING", booking.bookingId(),
                    "Fallo envio WhatsApp de link de pago: " + safeMessage(e),
                    AuditMetadata.of("paymentId", payment.id()));
        }
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "Por confirmar" : value.toLocalDate() + " " + value.toLocalTime();
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeMessage(RuntimeException e) {
        return e.getMessage() == null ? "sin detalle" : e.getMessage();
    }

    private void recordManualPaymentAudit(
            AuthenticatedUser user, BookingPaymentRecord payment,
            BookingPaymentBookingRecord booking) {
        String eventName = switch (payment.status()) {
            case "APPROVED" -> "BOOKING_PAYMENT_APPROVED_MANUAL";
            case "REJECTED" -> "BOOKING_PAYMENT_REJECTED_MANUAL";
            case "EXPIRED" -> "BOOKING_PAYMENT_EXPIRED_MANUAL";
            case "REFUNDED" -> "BOOKING_PAYMENT_REFUNDED_MANUAL";
            default -> "BOOKING_PAYMENT_PENDING_MANUAL";
        };
        auditService.record(user.businessId(), user.userId(), eventName, "BOOKING", payment.bookingId(),
                "Pago manual de reserva registrado.",
                AuditMetadata.of("paymentId", payment.id(), "provider", payment.provider(),
                        "providerPaymentId", payment.providerPaymentId(),
                        "idempotencyKey", payment.idempotencyKey(),
                        "amount", payment.amount(), "currency", payment.currency(),
                        "paymentStatus", payment.status(),
                        "bookingStatus", BookingStateMachine.canonical(booking.bookingStatus()),
                        "bookingPaymentStatus", booking.paymentStatus()));
    }

    private BookingPaymentResponse toResponse(BookingPaymentRecord payment) {
        return new BookingPaymentResponse(
                payment.id(), payment.bookingId(), payment.provider(),
                payment.providerPaymentId(),
                payment.idempotencyKey(),
                payment.amount(), payment.currency(), payment.status(),
                payment.checkoutUrl(), payment.checkoutExpiresAt(), payment.manual(),
                payment.approvedAt(), payment.rejectedAt(), payment.expiredAt(),
                payment.refundedAt(), payment.createdAt());
    }

    private String normalizeProvider(String provider) {
        return normalizeProvider(provider, null);
    }

    private String normalizeProvider(String provider, String defaultProvider) {
        if ((provider == null || provider.isBlank()) && defaultProvider != null)
            return defaultProvider;
        if (provider == null || provider.isBlank())
            throw validationError("provider", "El proveedor de pago es obligatorio.");
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 60)
            throw validationError("provider", "El proveedor de pago supera el largo permitido.");
        return normalized;
    }

    private String normalizeCurrency(String currency) {
        String resolved = currency == null || currency.isBlank() ? "CLP"
                : currency.trim().toUpperCase(Locale.ROOT);
        if (resolved.length() != 3)
            throw validationError("currency", "La moneda debe usar codigo ISO de 3 letras.");
        return resolved;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank())
            throw validationError("status", "El estado del pago es obligatorio.");
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
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private boolean isTerminal(String status) {
        return "APPROVED".equals(status) || "REJECTED".equals(status)
                || "EXPIRED".equals(status) || "REFUNDED".equals(status);
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message, Map.of(field, message));
    }

    private static final class MessageDigestSupport {
        private MessageDigestSupport() {}
        private static boolean constantTimeEquals(String expected, String actual) {
            if (expected == null || actual == null) return false;
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8));
        }
    }
}