package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.WhatsAppWebActionResponse;
import com.asistentewhatsapp.administration.api.WhatsAppWebStatusResponse;
import com.asistentewhatsapp.administration.api.WhatsAppWebTestMessageRequest;
import com.asistentewhatsapp.administration.api.WhatsAppWebTestMessageResponse;
import com.asistentewhatsapp.channels.domain.CanalWhatsApp;
import com.asistentewhatsapp.channels.domain.ChannelDelivery;
import com.asistentewhatsapp.channels.domain.OutboundMessage;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionAction;
import com.asistentewhatsapp.channels.domain.WhatsAppSessionStatus;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebChannelJdbcRepository;
import com.asistentewhatsapp.channels.infrastructure.whatsappweb.WhatsAppWebClientProperties;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppWebAdministrationService {

    private static final String WARNING_MESSAGE = "Canal WhatsApp desacoplado por ambiente: local puede usar WhatsApp Web experimental con QR; produccion debe usar Cloud API o modo dry-run configurado.";

    private final WhatsAppWebChannelJdbcRepository repository;
    private final AuditLogJdbcRepository auditLogJdbcRepository;
    private final List<CanalWhatsApp> canalesWhatsApp;
    private final WhatsAppWebClientProperties properties;

    public WhatsAppWebAdministrationService(
            WhatsAppWebChannelJdbcRepository repository,
            AuditLogJdbcRepository auditLogJdbcRepository,
            List<CanalWhatsApp> canalesWhatsApp,
            WhatsAppWebClientProperties properties) {
        this.repository = repository;
        this.auditLogJdbcRepository = auditLogJdbcRepository;
        this.canalesWhatsApp = canalesWhatsApp;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public WhatsAppWebStatusResponse getStatus(AuthenticatedUser authenticatedUser) {
        AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
        WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount = loadChannelAccount(authenticatedUser.businessId());

        try {
            WhatsAppSessionStatus response = resolveCanalWhatsApp().getStatus();
            return toStatusResponse(channelAccount, response, true);
        } catch (MessagingChannelUnavailableException exception) {
            return new WhatsAppWebStatusResponse(
                    channelAccount.status(),
                    resolveDisplayPhone(channelAccount.phoneNumber()),
                    channelAccount.lastQrCode(),
                    channelAccount.lastEventAt(),
                    false,
                    "EXPERIMENTAL",
                    WARNING_MESSAGE,
                    repository.findRecentEvents(authenticatedUser.businessId(), 6));
        }
    }

    @Transactional
    public WhatsAppWebActionResponse connect(AuthenticatedUser authenticatedUser) {
        AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
        WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount = loadChannelAccount(authenticatedUser.businessId());
        CanalWhatsApp canalWhatsApp = resolveCanalWhatsApp();
        WhatsAppSessionAction response = canalWhatsApp.connect();
        OffsetDateTime acceptedAt = response.acceptedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : response.acceptedAt();
        String displayPhone = resolveDisplayPhone(response.phoneNumber());
        repository.updateChannelAccount(
                channelAccount.id(),
                normalizeSessionStatus(response.connectionStatus()),
                displayPhone,
                response.qrCode(),
                acceptedAt);
        auditLogJdbcRepository.insert(
                authenticatedUser.businessId(),
                authenticatedUser.userId(),
                "WHATSAPP_WEB_CONNECT_REQUESTED",
                "CHANNEL_ACCOUNT",
                channelAccount.id(),
                "Se solicito conexion del adaptador WhatsApp Web experimental.",
                Map.of("sessionStatus", normalizeSessionStatus(response.connectionStatus())),
                acceptedAt);
        return new WhatsAppWebActionResponse(
                normalizeSessionStatus(response.connectionStatus()),
                displayPhone,
                response.qrCode(),
                acceptedAt,
                true,
                canalWhatsApp.provider().name());
    }

    @Transactional
    public WhatsAppWebActionResponse refreshQr(AuthenticatedUser authenticatedUser) {
        AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
        WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount = loadChannelAccount(authenticatedUser.businessId());
        CanalWhatsApp canalWhatsApp = resolveCanalWhatsApp();
        WhatsAppSessionAction response = canalWhatsApp.refreshQr();
        OffsetDateTime acceptedAt = response.acceptedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : response.acceptedAt();
        String displayPhone = resolveDisplayPhone(response.phoneNumber());
        repository.updateChannelAccount(
                channelAccount.id(),
                normalizeSessionStatus(response.connectionStatus()),
                displayPhone,
                response.qrCode(),
                acceptedAt);
        auditLogJdbcRepository.insert(
                authenticatedUser.businessId(),
                authenticatedUser.userId(),
                "WHATSAPP_WEB_QR_REFRESHED",
                "CHANNEL_ACCOUNT",
                channelAccount.id(),
                "Se solicito un nuevo QR del adaptador WhatsApp Web experimental.",
                Map.of("sessionStatus", normalizeSessionStatus(response.connectionStatus())),
                acceptedAt);
        return new WhatsAppWebActionResponse(
                normalizeSessionStatus(response.connectionStatus()),
                displayPhone,
                response.qrCode(),
                acceptedAt,
                true,
                canalWhatsApp.provider().name());
    }

    @Transactional
    public WhatsAppWebActionResponse disconnect(AuthenticatedUser authenticatedUser) {
        AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
        WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount = loadChannelAccount(authenticatedUser.businessId());
        CanalWhatsApp canalWhatsApp = resolveCanalWhatsApp();
        WhatsAppSessionAction response = canalWhatsApp.disconnect();
        OffsetDateTime acceptedAt = response.acceptedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : response.acceptedAt();
        String displayPhone = resolveDisplayPhone(response.phoneNumber());
        repository.updateChannelAccount(
                channelAccount.id(),
                normalizeSessionStatus(response.connectionStatus()),
                displayPhone,
                response.qrCode(),
                acceptedAt);
        auditLogJdbcRepository.insert(
                authenticatedUser.businessId(),
                authenticatedUser.userId(),
                "WHATSAPP_WEB_DISCONNECTED",
                "CHANNEL_ACCOUNT",
                channelAccount.id(),
                "Se desconecto la sesion experimental WhatsApp Web.",
                Map.of("sessionStatus", normalizeSessionStatus(response.connectionStatus())),
                acceptedAt);
        return new WhatsAppWebActionResponse(
                normalizeSessionStatus(response.connectionStatus()),
                displayPhone,
                response.qrCode(),
                acceptedAt,
                true,
                canalWhatsApp.provider().name());
    }

    @Transactional
    public WhatsAppWebTestMessageResponse sendTestMessage(
            AuthenticatedUser authenticatedUser,
            WhatsAppWebTestMessageRequest request) {
        AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
        WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount = loadChannelAccount(authenticatedUser.businessId());
        String normalizedPhone = normalizePhone(request.recipientPhone());
        WhatsAppWebChannelJdbcRepository.CustomerRecord customer = repository.findCustomerByPhone(
                        authenticatedUser.businessId(),
                        normalizedPhone)
                .orElseGet(() -> new WhatsAppWebChannelJdbcRepository.CustomerRecord(
                        repository.insertCustomer(authenticatedUser.businessId(), normalizedPhone, "Contacto de prueba"),
                        "Contacto de prueba",
                        normalizedPhone,
                        normalizedPhone));

        WhatsAppWebChannelJdbcRepository.ConversationRecord conversation = repository.findLatestConversation(
                        authenticatedUser.businessId(),
                        channelAccount.id(),
                        customer.id())
                .orElseGet(() -> new WhatsAppWebChannelJdbcRepository.ConversationRecord(
                        repository.insertConversation(
                                authenticatedUser.businessId(),
                                channelAccount.id(),
                                customer.id(),
                                authenticatedUser.userId(),
                                customer.displayName(),
                                normalizedPhone,
                                OffsetDateTime.now(ZoneOffset.UTC)),
                        authenticatedUser.userId(),
                        0,
                        null,
                        null));

        UUID messageId = repository.insertOutboundMessage(
                authenticatedUser.businessId(),
                conversation.id(),
                authenticatedUser.userId(),
                request.body().trim(),
                OffsetDateTime.now(ZoneOffset.UTC));

        try {
            ChannelDelivery channelDelivery = resolveCanalWhatsApp().send(new OutboundMessage(
                    authenticatedUser.businessId(),
                    normalizedPhone,
                    request.body().trim()));
            OffsetDateTime acceptedAt = OffsetDateTime.ofInstant(channelDelivery.acceptedAt(), ZoneOffset.UTC);
            repository.updateOutboundMessageAccepted(
                    messageId,
                    channelDelivery.externalMessageId(),
                    normalizeDeliveryStatus(channelDelivery.status()),
                    acceptedAt);
            repository.insertMessageDeliveryLog(
                    authenticatedUser.businessId(),
                    messageId,
                    normalizeDeliveryStatus(channelDelivery.status()),
                    channelDelivery.externalMessageId(),
                    Map.of(
                            "channelType", channelDelivery.channelType().name(),
                            "acceptedAt", acceptedAt),
                    acceptedAt);
            repository.updateConversationOutboundActivity(conversation.id(), request.body().trim(), acceptedAt);

            auditLogJdbcRepository.insert(
                    authenticatedUser.businessId(),
                    authenticatedUser.userId(),
                    "WHATSAPP_WEB_TEST_MESSAGE_SENT",
                    "MESSAGE",
                    messageId,
                    "Se envio un mensaje de prueba por el adaptador WhatsApp Web experimental.",
                    Map.of(
                            "recipientPhone", normalizedPhone,
                            "externalMessageId", channelDelivery.externalMessageId()),
                    acceptedAt);

            return new WhatsAppWebTestMessageResponse(
                    conversation.id(),
                    messageId,
                    channelDelivery.externalMessageId(),
                    normalizeDeliveryStatus(channelDelivery.status()),
                    acceptedAt);
        } catch (MessagingChannelUnavailableException exception) {
            OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC);
            repository.updateOutboundMessageFailed(messageId, "WHATSAPP_WEB_UNAVAILABLE", failedAt);
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "WHATSAPP_WEB_UNAVAILABLE",
                    "No fue posible enviar el mensaje de prueba al adaptador WhatsApp Web experimental.");
        }
    }

    private CanalWhatsApp resolveCanalWhatsApp() {
        return canalesWhatsApp.stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "WHATSAPP_CHANNEL_DISABLED",
                        "El puerto interno CanalWhatsApp no esta disponible para este ambiente."));
    }

    private WhatsAppWebChannelJdbcRepository.ChannelAccountRecord loadChannelAccount(UUID businessId) {
        return repository.findChannelAccountByBusinessId(businessId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "WHATSAPP_WEB_CHANNEL_NOT_FOUND",
                        "No se encontro la configuracion de canal WhatsApp Web para la empresa actual."));
    }

    private WhatsAppWebStatusResponse toStatusResponse(
            WhatsAppWebChannelJdbcRepository.ChannelAccountRecord channelAccount,
            WhatsAppSessionStatus response,
            boolean adapterReachable) {
        String sessionStatus = normalizeSessionStatus(response.connectionStatus());
        return new WhatsAppWebStatusResponse(
                sessionStatus,
                resolveDisplayPhone(response.phoneNumber()),
                response.qrCode(),
                response.lastEventAt(),
                adapterReachable,
                response.adapterMode() == null || response.adapterMode().isBlank() ? "EXPERIMENTAL" : response.adapterMode(),
                WARNING_MESSAGE,
                repository.findRecentEvents(channelAccount.businessId(), 6));
    }

    private String resolveDisplayPhone(String rawPhone) {
        String normalized = normalizeDisplayPhone(rawPhone);
        if (normalized != null) {
            return normalized;
        }

        return normalizeDisplayPhone(properties.defaultPhoneNumber());
    }

    private String normalizeDisplayPhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String digits = rawPhone.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String normalizeSessionStatus(String sessionStatus) {
        if (sessionStatus == null || sessionStatus.isBlank()) {
            return "ERROR";
        }
        return switch (sessionStatus) {
            case "CONNECTED", "DISCONNECTED", "QR_PENDING", "SYNCING", "ERROR" -> sessionStatus;
            case "QR_REQUIRED" -> "QR_PENDING";
            default -> "ERROR";
        };
    }

    private String normalizeDeliveryStatus(String deliveryStatus) {
        if (deliveryStatus == null || deliveryStatus.isBlank()) {
            return "QUEUED";
        }
        return switch (deliveryStatus) {
            case "PENDING", "QUEUED", "PROVIDER_ACCEPTED", "SENT", "DELIVERED", "READ", "FAILED", "SIMULATED", "DRY_RUN" -> deliveryStatus;
            default -> "QUEUED";
        };
    }

    private String normalizePhone(String rawPhone) {
        return rawPhone.trim().replace(" ", "");
    }
}
