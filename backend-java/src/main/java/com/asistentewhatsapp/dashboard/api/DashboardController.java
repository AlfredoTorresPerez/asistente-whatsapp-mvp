package com.asistentewhatsapp.dashboard.api;

import com.asistentewhatsapp.dashboard.application.DashboardService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'DASHBOARD_VIEW')")
    @GetMapping("/api/v1/dashboard/summary")
    public DashboardSummaryResponse summary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) UUID ownerUserId) {
        return dashboardService.getSummary(authenticatedUser, from, to, ownerUserId);
    }
}
