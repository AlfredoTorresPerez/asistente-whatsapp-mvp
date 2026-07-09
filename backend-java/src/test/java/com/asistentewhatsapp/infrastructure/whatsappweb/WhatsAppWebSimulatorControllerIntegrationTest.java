package com.asistentewhatsapp.infrastructure.whatsappweb;

import static org.junit.jupiter.api.Assertions.*;

import com.asistentewhatsapp.channels.infrastructure.whatsappweb.DemoIncomingMessageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WhatsAppWebSimulatorControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointShouldRespond() {
        var response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void simulatorEndpointShouldAcceptMessage() {
        var request = new DemoIncomingMessageRequest(
                null,
                "+56900000001",
                "Hola, quiero agendar una hora",
                null);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/test/whatsapp-inbound", request, String.class);
        // Should return 200 ACCEPTED when a channel account exists (seeded by V4)
        assertTrue(response.getStatusCode() == HttpStatus.OK
                || response.getStatusCode() == HttpStatus.ACCEPTED);
        assertTrue(response.getBody() != null && response.getBody().contains("ACCEPTED"));
    }
}
