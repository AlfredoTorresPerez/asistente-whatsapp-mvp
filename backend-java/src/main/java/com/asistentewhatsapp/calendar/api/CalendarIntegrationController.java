package com.asistentewhatsapp.calendar.api;

import com.asistentewhatsapp.calendar.application.CalendarIntegrationService;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.calendar.infrastructure.BookingCalendarSyncJdbcRepository.BookingCalendarSyncRecord;
import com.asistentewhatsapp.calendar.infrastructure.CalendarIntegrationJdbcRepository.CalendarIntegrationAccountRecord;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1")
public class CalendarIntegrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarIntegrationController.class);

    private final CalendarIntegrationService integrationService;
    private final CalendarSyncService calendarSyncService;
    private final AuditService auditService;

    public CalendarIntegrationController(
            CalendarIntegrationService integrationService,
            CalendarSyncService calendarSyncService,
            AuditService auditService) {
        this.integrationService = integrationService;
        this.calendarSyncService = calendarSyncService;
        this.auditService = auditService;
    }

    @GetMapping("/calendar-integrations/status")
    public ResponseEntity<List<CalendarIntegrationAccountRecord>> getStatus(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(integrationService.getAccounts(user.businessId()));
    }

    @GetMapping("/calendar-integrations/google/connect")
    public RedirectView connectGoogle(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "http://localhost:5173/calendario") String redirectUri) {
        String authUrl = integrationService.getAuthUrl(user.businessId(), "GOOGLE", redirectUri);
        LOGGER.info("CALENDAR_GOOGLE_CONNECT businessId={}", user.businessId());
        return new RedirectView(authUrl);
    }

    @GetMapping("/calendar-integrations/google/callback")
    public RedirectView googleCallback(
            @RequestParam String state,
            @RequestParam String code,
            @RequestParam(defaultValue = "http://localhost:5173/calendario") String redirectUri) {
        try {
            integrationService.handleOAuthCallback(state, code, redirectUri);
            LOGGER.info("CALENDAR_GOOGLE_CALLBACK_SUCCESS");
        } catch (Exception e) {
            LOGGER.warn("CALENDAR_GOOGLE_CALLBACK_FAILED reason={}", e.getMessage());
        }
        return new RedirectView("/calendario?calendar=connected");
    }

    @GetMapping("/calendar-integrations/outlook/connect")
    public RedirectView connectOutlook(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "http://localhost:5173/calendario") String redirectUri) {
        String authUrl = integrationService.getAuthUrl(user.businessId(), "OUTLOOK", redirectUri);
        LOGGER.info("CALENDAR_OUTLOOK_CONNECT businessId={}", user.businessId());
        return new RedirectView(authUrl);
    }

    @GetMapping("/calendar-integrations/outlook/callback")
    public RedirectView outlookCallback(
            @RequestParam String state,
            @RequestParam String code,
            @RequestParam(defaultValue = "http://localhost:5173/calendario") String redirectUri) {
        try {
            integrationService.handleOAuthCallback(state, code, redirectUri);
            LOGGER.info("CALENDAR_OUTLOOK_CALLBACK_SUCCESS");
        } catch (Exception e) {
            LOGGER.warn("CALENDAR_OUTLOOK_CALLBACK_FAILED reason={}", e.getMessage());
        }
        return new RedirectView("/calendario?calendar=connected");
    }

    @DeleteMapping("/calendar-integrations/{accountId}")
    public ResponseEntity<Map<String, String>> disconnect(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        integrationService.disconnect(accountId);
        auditService.record(user.businessId(), null, "CALENDAR_ACCOUNT_DISCONNECTED", "CALENDAR", null,
                "Cuenta de calendario desvinculada: " + accountId, null);
        return ResponseEntity.ok(Map.of("message", "Cuenta de calendario desvinculada."));
    }

    @PostMapping("/bookings/{bookingId}/calendar-sync/retry")
    public ResponseEntity<Map<String, String>> retrySync(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        calendarSyncService.retrySync(bookingId);
        return ResponseEntity.ok(Map.of("message", "Reintento de sincronizacion programado."));
    }

    @GetMapping("/bookings/{bookingId}/calendar-sync")
    public ResponseEntity<List<BookingCalendarSyncRecord>> getSyncStatus(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(calendarSyncService.getSyncStatus(bookingId));
    }
}
