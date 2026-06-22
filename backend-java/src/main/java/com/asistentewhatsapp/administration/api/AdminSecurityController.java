package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.AdminSecurityService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminSecurityController {

    private final AdminSecurityService adminSecurityService;

    public AdminSecurityController(AdminSecurityService adminSecurityService) {
        this.adminSecurityService = adminSecurityService;
    }

    @GetMapping("/api/v1/admin/security")
    public SecurityPolicyResponse getPolicy(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return adminSecurityService.getPolicy(authenticatedUser);
    }

    @PatchMapping(value = "/api/v1/admin/security", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SecurityPolicyResponse updatePolicy(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody SecurityPolicyRequest request) {
        return adminSecurityService.updatePolicy(authenticatedUser, request);
    }
}
