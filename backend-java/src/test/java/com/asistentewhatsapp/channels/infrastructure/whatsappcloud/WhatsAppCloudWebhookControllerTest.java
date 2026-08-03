package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asistentewhatsapp.shared.observability.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WhatsAppCloudWebhookControllerTest {

	private MockMvc mockMvc;
	private WhatsAppCloudWebhookValidator validator;
	private WhatsAppCloudWebhookParser parser;
	private WhatsAppCloudApiMetrics metrics;

	@BeforeEach
	void setUp() {
		validator = mock(WhatsAppCloudWebhookValidator.class);
		parser = mock(WhatsAppCloudWebhookParser.class);
		metrics = mock(WhatsAppCloudApiMetrics.class);
		WhatsAppCloudWebhookController controller = new WhatsAppCloudWebhookController(validator, parser, metrics,
				new BusinessMetrics(new SimpleMeterRegistry()));
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void verificationGetValidReturnsChallenge() throws Exception {
		WhatsAppCloudApiProperties props = new WhatsAppCloudApiProperties(true, null, null, null, null, "test_verify",
				null, false, null, null, null, null, null, false, 5, 15, List.of());
		WhatsAppCloudWebhookValidator localValidator = new WhatsAppCloudWebhookValidator(props);
		WhatsAppCloudWebhookParser localParser = mock(WhatsAppCloudWebhookParser.class);
		WhatsAppCloudApiMetrics localMetrics = mock(WhatsAppCloudApiMetrics.class);
		WhatsAppCloudWebhookController localController = new WhatsAppCloudWebhookController(localValidator, localParser,
				localMetrics, new BusinessMetrics(new SimpleMeterRegistry()));

		MockMvc localMvc = MockMvcBuilders.standaloneSetup(localController).build();

		localMvc.perform(get("/api/v1/integrations/whatsapp-cloud/webhook").param("hub.mode", "subscribe")
				.param("hub.verify_token", "test_verify").param("hub.challenge", "challenge_value_123"))
				.andExpect(status().isOk()).andExpect(content().string("challenge_value_123"));
	}

	@Test
	void verificationGetInvalidTokenReturns403() throws Exception {
		doThrow(new com.asistentewhatsapp.shared.exception.ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
				"WEBHOOK_VERIFICATION_FAILED", "Invalid token")).when(validator)
				.validateAndExtractChallenge("subscribe", "wrong_token", "challenge");

		mockMvc.perform(get("/api/v1/integrations/whatsapp-cloud/webhook").param("hub.mode", "subscribe")
				.param("hub.verify_token", "wrong_token").param("hub.challenge", "challenge"))
				.andExpect(status().isForbidden());
	}

	@Test
	void verificationGetInvalidModeReturns403() throws Exception {
		doThrow(new com.asistentewhatsapp.shared.exception.ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
				"WEBHOOK_VERIFICATION_FAILED", "Invalid mode")).when(validator)
				.validateAndExtractChallenge("unsubscribe", "test_verify", "challenge");

		mockMvc.perform(get("/api/v1/integrations/whatsapp-cloud/webhook").param("hub.mode", "unsubscribe")
				.param("hub.verify_token", "test_verify").param("hub.challenge", "challenge"))
				.andExpect(status().isForbidden());
	}

	@Test
	void validSignatureReturnsAccepted() throws Exception {
		doNothing().when(validator).validateSignature(anyString(), anyString());
		doNothing().when(metrics).incrementWebhookReceived();
		doNothing().when(metrics).incrementWebhookAccepted();

		mockMvc.perform(post("/api/v1/integrations/whatsapp-cloud/webhook")
				.header("X-Hub-Signature-256", "sha256=valid").contentType(MediaType.APPLICATION_JSON)
				.content("{\"object\":\"whatsapp_business_account\",\"entry\":[]}")).andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"ACCEPTED\"}"));
	}

	@Test
	void invalidSignatureReturnsUnauthorized() throws Exception {
		doThrow(new com.asistentewhatsapp.shared.exception.ApiException(
				org.springframework.http.HttpStatus.UNAUTHORIZED, "WEBHOOK_SIGNATURE_INVALID", "Invalid signature"))
				.when(validator).validateSignature(anyString(), anyString());

		mockMvc.perform(
				post("/api/v1/integrations/whatsapp-cloud/webhook").header("X-Hub-Signature-256", "sha256=invalid")
						.contentType(MediaType.APPLICATION_JSON).content("{\"data\":\"test\"}"))
				.andExpect(status().isUnauthorized()).andExpect(content().json("{\"status\":\"REJECTED\"}"));
	}

	@Test
	void missingSignatureReturnsUnauthorizedWhenRequired() throws Exception {
		doThrow(new com.asistentewhatsapp.shared.exception.ApiException(
				org.springframework.http.HttpStatus.UNAUTHORIZED, "WEBHOOK_SIGNATURE_MISSING", "Missing signature"))
				.when(validator).validateSignature(null, "body");

		mockMvc.perform(post("/api/v1/integrations/whatsapp-cloud/webhook").contentType(MediaType.APPLICATION_JSON)
				.content("body")).andExpect(status().isUnauthorized());
	}

	@Test
	void largePayloadReturnsBadRequest() throws Exception {
		StringBuilder large = new StringBuilder(1024 * 100 + 1);
		large.append("x".repeat(1024 * 100 + 1));

		mockMvc.perform(post("/api/v1/integrations/whatsapp-cloud/webhook").contentType(MediaType.APPLICATION_JSON)
				.content(large.toString())).andExpect(status().isBadRequest());
	}

	@Test
	void parserExceptionReturnsAccepted() throws Exception {
		doNothing().when(validator).validateSignature(anyString(), anyString());
		doNothing().when(metrics).incrementWebhookReceived();
		doNothing().when(metrics).incrementWebhookAccepted();
		doThrow(new RuntimeException("Unexpected")).when(parser).parseAndProcess(anyString());

		mockMvc.perform(post("/api/v1/integrations/whatsapp-cloud/webhook")
				.header("X-Hub-Signature-256", "sha256=valid").contentType(MediaType.APPLICATION_JSON)
				.content("{\"object\":\"whatsapp_business_account\",\"entry\":[]}")).andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"ACCEPTED\"}"));
	}
}
