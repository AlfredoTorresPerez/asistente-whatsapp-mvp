package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.WhatsAppWebAdministrationService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class WhatsAppWebController {

    private final WhatsAppWebAdministrationService whatsAppWebAdministrationService;

    public WhatsAppWebController(WhatsAppWebAdministrationService whatsAppWebAdministrationService) {
        this.whatsAppWebAdministrationService = whatsAppWebAdministrationService;
    }

    @GetMapping({"/api/v1/whatsapp-web/status", "/api/channels/whatsapp-web/status"})
    public WhatsAppWebStatusResponse status(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return whatsAppWebAdministrationService.getStatus(authenticatedUser);
    }

    @GetMapping({"/api/v1/whatsapp-web/qr", "/api/channels/whatsapp-web/qr"})
    public WhatsAppWebQrResponse qr(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return whatsAppWebAdministrationService.getQr(authenticatedUser);
    }

    @PostMapping({"/api/v1/whatsapp-web/connect", "/api/channels/whatsapp-web/connect"})
    public WhatsAppWebActionResponse connect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return whatsAppWebAdministrationService.connect(authenticatedUser);
    }

    @PostMapping({"/api/v1/whatsapp-web/refresh-qr", "/api/channels/whatsapp-web/refresh-qr"})
    public WhatsAppWebActionResponse refreshQr(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return whatsAppWebAdministrationService.refreshQr(authenticatedUser);
    }

    @PostMapping({"/api/v1/whatsapp-web/disconnect", "/api/channels/whatsapp-web/disconnect"})
    public WhatsAppWebActionResponse disconnect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return whatsAppWebAdministrationService.disconnect(authenticatedUser);
    }

    @PostMapping({"/api/v1/whatsapp-web/test-message", "/api/channels/whatsapp-web/test-message"})
    public WhatsAppWebTestMessageResponse sendTestMessage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody WhatsAppWebTestMessageRequest request) {
        return whatsAppWebAdministrationService.sendTestMessage(authenticatedUser, request);
    }
}
