package com.asistentewhatsapp.shared.api;

import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.sql.Connection;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final DataSource dataSource;
    private final String environment;
    private final String appName;
    private final boolean aiAutoReplyEnabled;
    private final boolean calendarGoogleEnabled;
    private final boolean calendarSyncEnabled;
    private final boolean emailEnabled;
    private final String paymentProvider;

    public SystemController(
            DataSource dataSource,
            @Value("${app.environment:local}") String environment,
            @Value("${spring.application.name:backend-java}") String appName,
            @Value("${app.ai.agents.auto-reply-enabled:false}") boolean aiAutoReplyEnabled,
            @Value("${app.calendar.google.enabled:false}") boolean calendarGoogleEnabled,
            @Value("${app.calendar.sync.enabled:true}") boolean calendarSyncEnabled,
            @Value("${app.email.enabled:false}") boolean emailEnabled,
            @Value("${app.booking-payment.provider:SIMULATED}") String paymentProvider) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.appName = appName;
        this.aiAutoReplyEnabled = aiAutoReplyEnabled;
        this.calendarGoogleEnabled = calendarGoogleEnabled;
        this.calendarSyncEnabled = calendarSyncEnabled;
        this.emailEnabled = emailEnabled;
        this.paymentProvider = paymentProvider;
    }

    @PreAuthorize("hasPermission(#user.businessId(), 'SECURITY_AUDIT_VIEW')")
    @GetMapping("/status")
    public SystemStatusResponse status(@AuthenticationPrincipal AuthenticatedUser user) {
        String backendStatus = "UP";
        String dbStatus = checkDatabase();

        return new SystemStatusResponse(
                backendStatus,
                dbStatus,
                appName,
                environment,
                Instant.now(),
                aiAutoReplyEnabled,
                calendarGoogleEnabled,
                calendarSyncEnabled,
                emailEnabled,
                paymentProvider,
                "No disponible");
    }

    private String checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return "UP";
            }
            return "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    public record SystemStatusResponse(
            String backendStatus,
            String databaseStatus,
            String appName,
            String environment,
            Instant serverTimestamp,
            boolean aiAutoReplyEnabled,
            boolean calendarIntegrationEnabled,
            boolean calendarSyncEnabled,
            boolean emailEnabled,
            String paymentProvider,
            String recentErrors) {
    }
}
