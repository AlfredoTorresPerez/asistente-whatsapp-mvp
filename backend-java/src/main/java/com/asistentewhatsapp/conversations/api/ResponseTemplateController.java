package com.asistentewhatsapp.conversations.api;

import com.asistentewhatsapp.conversations.application.ResponseTemplateService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ResponseTemplateController {

    private final ResponseTemplateService responseTemplateService;

    public ResponseTemplateController(ResponseTemplateService responseTemplateService) {
        this.responseTemplateService = responseTemplateService;
    }

    @GetMapping("/api/v1/templates")
    public List<ResponseTemplateResponse> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) Boolean active) {
        return responseTemplateService.list(authenticatedUser, active);
    }

    @PostMapping(value = "/api/v1/templates", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseTemplateResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateResponseTemplateRequest request) {
        return responseTemplateService.create(authenticatedUser, request);
    }

    @PutMapping(value = "/api/v1/templates/{templateId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseTemplateResponse update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateResponseTemplateRequest request) {
        return responseTemplateService.update(authenticatedUser, templateId, request);
    }

    @PatchMapping(value = "/api/v1/templates/{templateId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseTemplateResponse updateStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateStatusRequest request) {
        return responseTemplateService.updateStatus(authenticatedUser, templateId, request);
    }
}
