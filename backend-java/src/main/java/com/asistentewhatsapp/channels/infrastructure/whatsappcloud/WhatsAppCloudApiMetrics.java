package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
public class WhatsAppCloudApiMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter webhookReceivedTotal;
    private final Counter webhookRejectedTotal;
    private final Counter messagesReceivedTotal;
    private final Counter messagesSentTotal;
    private final Counter messageStatusTotal;
    private final Counter apiErrorsTotal;
    private final Timer webhookProcessingTimer;
    private final Timer apiCallTimer;

    private final ConcurrentMap<String, Counter> messageTypeCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> statusCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    public WhatsAppCloudApiMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.webhookReceivedTotal = Counter.builder("whatsapp_cloud_webhook_received_total")
                .description("Total de webhooks recibidos de Meta Cloud API")
                .register(meterRegistry);
        this.webhookRejectedTotal = Counter.builder("whatsapp_cloud_webhook_rejected_total")
                .description("Total de webhooks rechazados de Meta Cloud API")
                .register(meterRegistry);
        this.messagesReceivedTotal = Counter.builder("whatsapp_cloud_messages_received_total")
                .description("Total de mensajes entrantes procesados de Cloud API")
                .register(meterRegistry);
        this.messagesSentTotal = Counter.builder("whatsapp_cloud_messages_sent_total")
                .description("Total de mensajes salientes enviados via Cloud API")
                .register(meterRegistry);
        this.messageStatusTotal = Counter.builder("whatsapp_cloud_message_status_total")
                .description("Total de actualizaciones de estado de mensajes Cloud API")
                .register(meterRegistry);
        this.apiErrorsTotal = Counter.builder("whatsapp_cloud_api_errors_total")
                .description("Total de errores de API de Meta Cloud API")
                .register(meterRegistry);
        this.webhookProcessingTimer = Timer.builder("whatsapp_cloud_webhook_processing_seconds")
                .description("Tiempo de procesamiento de webhooks Cloud API")
                .register(meterRegistry);
        this.apiCallTimer = Timer.builder("whatsapp_cloud_api_call_seconds")
                .description("Latencia de llamadas a la API de Meta Cloud API")
                .register(meterRegistry);
    }

    public void incrementWebhookReceived() {
        webhookReceivedTotal.increment();
    }

    public void incrementWebhookRejected() {
        webhookRejectedTotal.increment();
    }

    public void incrementWebhookAccepted() {
    }

    public void incrementMessagesReceived() {
        messagesReceivedTotal.increment();
    }

    public void incrementMessagesSent() {
        messagesSentTotal.increment();
    }

    public void incrementMessageStatus(String status) {
        messageStatusTotal.increment();
        statusCounters.computeIfAbsent("status_" + status,
                key -> Counter.builder("whatsapp_cloud_message_status_total")
                        .tag("status", status)
                        .register(meterRegistry))
                .increment();
    }

    public void incrementApiErrors(String errorType) {
        apiErrorsTotal.increment();
        errorCounters.computeIfAbsent("error_" + errorType,
                key -> Counter.builder("whatsapp_cloud_api_errors_total")
                        .tag("error_type", errorType)
                        .register(meterRegistry))
                .increment();
    }

    public void incrementMessagesByType(String messageType) {
        messageTypeCounters.computeIfAbsent("type_" + messageType,
                key -> Counter.builder("whatsapp_cloud_messages_received_total")
                        .tag("message_type", messageType)
                        .register(meterRegistry))
                .increment();
    }

    public Timer getWebhookProcessingTimer() {
        return webhookProcessingTimer;
    }

    public Timer getApiCallTimer() {
        return apiCallTimer;
    }

    public void recordApiCall(long durationMillis, boolean success) {
        apiCallTimer.record(durationMillis, TimeUnit.MILLISECONDS);
        if (!success) {
            apiErrorsTotal.increment();
        }
    }
}
