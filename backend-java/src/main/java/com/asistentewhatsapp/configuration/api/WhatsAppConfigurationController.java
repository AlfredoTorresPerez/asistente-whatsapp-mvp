package com.asistentewhatsapp.configuration.api;

import com.asistentewhatsapp.configuration.application.WhatsAppConfigurationService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/configuration/whatsapp", produces = MediaType.APPLICATION_JSON_VALUE)
public class WhatsAppConfigurationController {

	private final WhatsAppConfigurationService whatsAppConfigurationService;

	public WhatsAppConfigurationController(WhatsAppConfigurationService whatsAppConfigurationService) {
		this.whatsAppConfigurationService = whatsAppConfigurationService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_VIEW')")
	@GetMapping
	public WhatsAppConfigurationResponse getConfiguration(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppConfigurationService.getConfiguration(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PatchMapping(path = "/preferences")
	public WhatsAppConfigurationResponse updatePreferences(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody WhatsAppConfigurationPreferencesRequest request) {
		return whatsAppConfigurationService.updatePreferences(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping(path = "/connect")
	public WhatsAppConfigurationResponse connect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppConfigurationService.connect(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping(path = "/disconnect")
	public WhatsAppConfigurationResponse disconnect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppConfigurationService.disconnect(authenticatedUser);
	}
}
