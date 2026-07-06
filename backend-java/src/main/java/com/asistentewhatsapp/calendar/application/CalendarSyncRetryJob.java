package com.asistentewhatsapp.calendar.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CalendarSyncRetryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarSyncRetryJob.class);

    private final CalendarSyncService calendarSyncService;

    public CalendarSyncRetryJob(CalendarSyncService calendarSyncService) {
        this.calendarSyncService = calendarSyncService;
    }

    @Scheduled(fixedDelayString = "${app.calendar.sync.retry-interval-ms:300000}")
    public void retryFailedSyncs() {
        try {
            calendarSyncService.retryFailedSyncs();
        } catch (Exception e) {
            LOGGER.warn("CALENDAR_SYNC_RETRY_JOB_FAILED reason={}", e.getMessage());
        }
    }
}
