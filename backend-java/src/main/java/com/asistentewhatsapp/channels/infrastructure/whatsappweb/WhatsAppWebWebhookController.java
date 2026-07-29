package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import com.asistentewhatsapp.shared.api.StatusResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class WhatsAppWebWebhookController {

	private final WhatsAppWebWebhookService whatsAppWebWebhookService;

	public WhatsAppWebWebhookController(WhatsAppWebWebhookService whatsAppWebWebhookService) {
		this.whatsAppWebWebhookService = whatsAppWebWebhookService;
	}

	@PostMapping({"/api/v1/integrations/whatsapp-web/webhook", "/api/webhooks/whatsapp-web/messages"})
	public StatusResponse receive(@RequestHeader("X-WhatsApp-Web-Timestamp") String timestamp,
			@RequestHeader("X-WhatsApp-Web-Signature") String signature,
			@RequestHeader("X-WhatsApp-Web-Delivery-Id") String deliveryId, @RequestBody String rawBody) {
		return whatsAppWebWebhookService.handleWebhook(rawBody, timestamp, signature, deliveryId);
	}
}
