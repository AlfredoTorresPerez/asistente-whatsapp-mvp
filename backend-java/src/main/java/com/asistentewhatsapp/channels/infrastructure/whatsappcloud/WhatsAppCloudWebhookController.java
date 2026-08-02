package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.asistentewhatsapp.shared.api.StatusResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.observability.BusinessMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(prefix = "app.channels.whatsapp-cloud-api", name = "enabled", havingValue = "true")
public class WhatsAppCloudWebhookController {

	private static final int MAX_BODY_SIZE = 1024 * 100;

	private final WhatsAppCloudWebhookValidator validator;
	private final WhatsAppCloudWebhookParser parser;
	private final WhatsAppCloudApiMetrics metrics;
	private final BusinessMetrics businessMetrics;

	public WhatsAppCloudWebhookController(WhatsAppCloudWebhookValidator validator, WhatsAppCloudWebhookParser parser,
			WhatsAppCloudApiMetrics metrics, BusinessMetrics businessMetrics) {
		this.validator = validator;
		this.parser = parser;
		this.metrics = metrics;
		this.businessMetrics = businessMetrics;
	}

	@GetMapping(value = "/api/v1/integrations/whatsapp-cloud/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") String mode,
			@RequestParam("hub.verify_token") String verifyToken, @RequestParam("hub.challenge") String challenge) {
		metrics.incrementWebhookReceived();
		try {
			String response = validator.validateAndExtractChallenge(mode, verifyToken, challenge);
			metrics.incrementWebhookAccepted();
			return ResponseEntity.ok(response);
		} catch (ApiException exception) {
			metrics.incrementWebhookRejected();
			return ResponseEntity.status(exception.getStatus().value()).body("403");
		}
	}

	@PostMapping("/api/v1/integrations/whatsapp-cloud/webhook")
	public ResponseEntity<StatusResponse> receiveWebhook(@RequestBody String rawBody,
			@RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
			@RequestHeader(value = "X-Hub-Signature", required = false) String legacySignature) {
		metrics.incrementWebhookReceived();
		businessMetrics.incrementWhatsappWebhooksRecibidos();

		Timer.Sample sample = Timer.start();

		if (rawBody == null || rawBody.length() > MAX_BODY_SIZE) {
			metrics.incrementWebhookRejected();
			return ResponseEntity.badRequest().body(new StatusResponse("PAYLOAD_TOO_LARGE"));
		}

		String effectiveSignature = signature != null ? signature : legacySignature;

		try {
			validator.validateSignature(effectiveSignature, rawBody);
		} catch (ApiException exception) {
			metrics.incrementWebhookRejected();
			businessMetrics.incrementWhatsappWebhooksFirmaInvalida();
			HttpStatus status = exception.getStatus() != null ? exception.getStatus() : HttpStatus.UNAUTHORIZED;
			return ResponseEntity.status(status).body(new StatusResponse("REJECTED"));
		}

		metrics.incrementWebhookAccepted();

		try {
			parser.parseAndProcess(rawBody);
			sample.stop(metrics.getWebhookProcessingTimer());
			return ResponseEntity.ok(new StatusResponse("ACCEPTED"));
		} catch (ApiException exception) {
			metrics.incrementWebhookRejected();
			HttpStatus status = exception.getStatus();
			if (status == HttpStatus.BAD_REQUEST) {
				return ResponseEntity.badRequest().body(new StatusResponse("INVALID_PAYLOAD"));
			}
			return ResponseEntity.ok(new StatusResponse("ACCEPTED"));
		} catch (Exception exception) {
			metrics.incrementWebhookRejected();
			return ResponseEntity.ok(new StatusResponse("ACCEPTED"));
		}
	}
}
