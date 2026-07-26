package com.asistentewhatsapp.bookings.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking-reminders")
public class BookingReminderProperties {

    private boolean enabled = true;
    private int workerIntervalMs = 60000;
    private int batchSize = 50;
    private int processingTimeoutMinutes = 15;
    private int hoursBefore = 24;
    private int minimumRemainingHours = 2;
    private int maxAttempts = 4;
    private String defaultTimezone = "America/Santiago";

    private RetryDelay retryDelay = new RetryDelay();

    public static class RetryDelay {
        private int attempt1 = 0;
        private int attempt2 = 5;
        private int attempt3 = 15;
        private int attempt4 = 60;

        public int getAttempt1() { return attempt1; }
        public void setAttempt1(int attempt1) { this.attempt1 = attempt1; }
        public int getAttempt2() { return attempt2; }
        public void setAttempt2(int attempt2) { this.attempt2 = attempt2; }
        public int getAttempt3() { return attempt3; }
        public void setAttempt3(int attempt3) { this.attempt3 = attempt3; }
        public int getAttempt4() { return attempt4; }
        public void setAttempt4(int attempt4) { this.attempt4 = attempt4; }

        public int getDelayMinutesForAttempt(int attempt) {
            return switch (attempt) {
                case 1 -> attempt1;
                case 2 -> attempt2;
                case 3 -> attempt3;
                case 4 -> attempt4;
                default -> 60;
            };
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getWorkerIntervalMs() { return workerIntervalMs; }
    public void setWorkerIntervalMs(int workerIntervalMs) { this.workerIntervalMs = workerIntervalMs; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getProcessingTimeoutMinutes() { return processingTimeoutMinutes; }
    public void setProcessingTimeoutMinutes(int processingTimeoutMinutes) { this.processingTimeoutMinutes = processingTimeoutMinutes; }
    public int getHoursBefore() { return hoursBefore; }
    public void setHoursBefore(int hoursBefore) { this.hoursBefore = hoursBefore; }
    public int getMinimumRemainingHours() { return minimumRemainingHours; }
    public void setMinimumRemainingHours(int minimumRemainingHours) { this.minimumRemainingHours = minimumRemainingHours; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public String getDefaultTimezone() { return defaultTimezone; }
    public void setDefaultTimezone(String defaultTimezone) { this.defaultTimezone = defaultTimezone; }
    public RetryDelay getRetryDelay() { return retryDelay; }
    public void setRetryDelay(RetryDelay retryDelay) { this.retryDelay = retryDelay; }
}
