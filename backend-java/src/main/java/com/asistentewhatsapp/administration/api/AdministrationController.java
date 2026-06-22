package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.AdminSummaryService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AdministrationController {

    private final AdminSummaryService adminSummaryService;

    public AdministrationController(AdminSummaryService adminSummaryService) {
        this.adminSummaryService = adminSummaryService;
    }

    @GetMapping("/api/v1/admin/summary")
    public AdminSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return adminSummaryService.getSummary(authenticatedUser);
    }
}
