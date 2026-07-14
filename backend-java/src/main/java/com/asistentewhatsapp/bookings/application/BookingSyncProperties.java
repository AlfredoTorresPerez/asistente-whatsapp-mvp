package com.asistentewhatsapp.bookings.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking-sync")
public class BookingSyncProperties {

    private boolean phonePlaintextEnabled = false;
    private int eventMaxAttempts = 5;
    private int eventBatchSize = 10;
    private long eventProcessingTimeoutMs = 120000;
    private long eventWorkerIntervalMs = 10000;
    private long retryBaseDelayMs = 30000;
    private long retryMaxDelayMs = 900000;
    private String hmacSecret = "booking-sync-default-secret-change-in-prod";

    public boolean isPhonePlaintextEnabled() { return phonePlaintextEnabled; }
    public void setPhonePlaintextEnabled(boolean phonePlaintextEnabled) { this.phonePlaintextEnabled = phonePlaintextEnabled; }

    public int getEventMaxAttempts() { return eventMaxAttempts; }
    public void setEventMaxAttempts(int eventMaxAttempts) { this.eventMaxAttempts = eventMaxAttempts; }

    public int getEventBatchSize() { return eventBatchSize; }
    public void setEventBatchSize(int eventBatchSize) { this.eventBatchSize = eventBatchSize; }

    public long getEventProcessingTimeoutMs() { return eventProcessingTimeoutMs; }
    public void setEventProcessingTimeoutMs(long eventProcessingTimeoutMs) { this.eventProcessingTimeoutMs = eventProcessingTimeoutMs; }

    public long getEventWorkerIntervalMs() { return eventWorkerIntervalMs; }
    public void setEventWorkerIntervalMs(long eventWorkerIntervalMs) { this.eventWorkerIntervalMs = eventWorkerIntervalMs; }

    public long getRetryBaseDelayMs() { return retryBaseDelayMs; }
    public void setRetryBaseDelayMs(long retryBaseDelayMs) { this.retryBaseDelayMs = retryBaseDelayMs; }

    public long getRetryMaxDelayMs() { return retryMaxDelayMs; }
    public void setRetryMaxDelayMs(long retryMaxDelayMs) { this.retryMaxDelayMs = retryMaxDelayMs; }

    public String getHmacSecret() { return hmacSecret; }
    public void setHmacSecret(String hmacSecret) { this.hmacSecret = hmacSecret; }
}
