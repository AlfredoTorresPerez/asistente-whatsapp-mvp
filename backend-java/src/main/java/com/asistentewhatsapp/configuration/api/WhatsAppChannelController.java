package com.asistentewhatsapp.configuration.api;

import com.asistentewhatsapp.configuration.application.WhatsAppChannelService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/whatsapp/channel", produces = MediaType.APPLICATION_JSON_VALUE)
public class WhatsAppChannelController {

	private final WhatsAppChannelService whatsAppChannelService;

	public WhatsAppChannelController(WhatsAppChannelService whatsAppChannelService) {
		this.whatsAppChannelService = whatsAppChannelService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_VIEW')")
	@GetMapping
	public WhatsAppChannelResponse getChannel(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppChannelService.getChannel(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PutMapping
	public WhatsAppChannelResponse updateChannel(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody WhatsAppChannelUpdateRequest request) {
		return whatsAppChannelService.updateChannel(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/validate")
	@ResponseStatus(HttpStatus.OK)
	public WhatsAppChannelValidateResponse validateConfiguration(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppChannelService.validateConfiguration(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/test-message")
	public WhatsAppChannelTestMessageResponse sendTestMessage(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody WhatsAppChannelTestMessageRequest request) {
		return whatsAppChannelService.sendTestMessage(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/activate")
	public WhatsAppChannelResponse activateChannel(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppChannelService.activateChannel(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/deactivate")
	public WhatsAppChannelResponse deactivateChannel(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return whatsAppChannelService.deactivateChannel(authenticatedUser);
	}
}
