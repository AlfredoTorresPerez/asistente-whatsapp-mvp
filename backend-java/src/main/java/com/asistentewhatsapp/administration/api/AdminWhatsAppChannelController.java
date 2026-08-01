package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.WhatsAppChannelAdministrationService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminWhatsAppChannelController {

	private final WhatsAppChannelAdministrationService whatsAppChannelAdministrationService;

	public AdminWhatsAppChannelController(WhatsAppChannelAdministrationService whatsAppChannelAdministrationService) {
		this.whatsAppChannelAdministrationService = whatsAppChannelAdministrationService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_VIEW')")
	@GetMapping({"/api/v1/whatsapp-channel/status", "/api/channels/whatsapp-channel/status"})
	public WhatsAppChannelStatusResponse status(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppChannelAdministrationService.getStatus(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping({"/api/v1/whatsapp-channel/connect", "/api/channels/whatsapp-channel/connect"})
	public WhatsAppChannelActionResponse connect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppChannelAdministrationService.connect(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping({"/api/v1/whatsapp-channel/disconnect", "/api/channels/whatsapp-channel/disconnect"})
	public WhatsAppChannelActionResponse disconnect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppChannelAdministrationService.disconnect(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping({"/api/v1/whatsapp-channel/test-message", "/api/channels/whatsapp-channel/test-message"})
	public WhatsAppChannelTestMessageResponse sendTestMessage(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody WhatsAppChannelTestMessageRequest request) {
		return whatsAppChannelAdministrationService.sendTestMessage(authenticatedUser, request);
	}
}
