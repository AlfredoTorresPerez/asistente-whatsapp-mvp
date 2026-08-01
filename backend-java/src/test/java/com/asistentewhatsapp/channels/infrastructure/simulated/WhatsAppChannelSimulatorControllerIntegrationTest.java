package com.asistentewhatsapp.channels.infrastructure.simulated;

import static org.junit.jupiter.api.Assertions.*;

import com.asistentewhatsapp.channels.infrastructure.simulated.WhatsAppChannelSimulatorController.SimulatedIncomingMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WhatsAppChannelSimulatorControllerIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private String jwtToken;

	@BeforeEach
	void setUp() {
		var loginBody = new java.util.LinkedHashMap<String, String>();
		loginBody.put("email", "admin@demo.cl");
		loginBody.put("password", "Cambiar123!");
		var loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginBody, String.class);
		if (loginResponse.getStatusCode() == HttpStatus.OK && loginResponse.getBody() != null) {
			try {
				var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
				var root = mapper.readTree(loginResponse.getBody());
				jwtToken = root.path("token").asText();
				if (jwtToken == null || jwtToken.isBlank()) {
					jwtToken = root.path("accessToken").asText();
				}
			} catch (Exception e) {
				jwtToken = null;
			}
		}
	}

	@Test
	void healthEndpointShouldRespond() {
		var response = restTemplate.getForEntity("/actuator/health", String.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@Test
	void simulatorEndpointShouldAcceptMessage() {
		var request = new SimulatedIncomingMessageRequest(null, "+56900000001", "Hola, quiero agendar una hora", null,
				null, null);
		var headers = new org.springframework.http.HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		if (jwtToken != null) {
			headers.setBearerAuth(jwtToken);
		}
		var entity = new org.springframework.http.HttpEntity<>(request, headers);
		ResponseEntity<String> response = restTemplate.exchange("/api/v1/test/whatsapp-inbound",
				org.springframework.http.HttpMethod.POST, entity, String.class);
		assertTrue(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.ACCEPTED,
				"Esperado 200/202 pero obtuvo " + response.getStatusCode() + " body=" + response.getBody());
		assertTrue(response.getBody() != null && response.getBody().contains("ACCEPTED"),
				"Body no contiene ACCEPTED: " + response.getBody());
	}
}
