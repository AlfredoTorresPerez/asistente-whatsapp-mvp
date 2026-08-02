package com.asistentewhatsapp.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ObservabilityClientErrorsControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ObservabilityClientErrorsController controller = new ObservabilityClientErrorsController("");
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void rejectsRequestWithoutOriginOrReferer() throws Exception {
		mockMvc.perform(post("/api/v1/observability/client-errors").contentType(MediaType.APPLICATION_JSON)
				.content(validBody("Fallo de frontend"))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value("SOURCE_NOT_ALLOWED"));
	}

	@Test
	void rejectsBlankMessage() throws Exception {
		mockMvc.perform(post("/api/v1/observability/client-errors").header("Origin", "http://localhost:5173")
				.contentType(MediaType.APPLICATION_JSON).content(validBody("   "))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value("INVALID_PAYLOAD"));
	}

	@Test
	void acceptsValidReport() throws Exception {
		mockMvc.perform(post("/api/v1/observability/client-errors").header("Origin", "http://localhost:5173")
				.contentType(MediaType.APPLICATION_JSON).content(validBody("Fallo de frontend")))
				.andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("ACCEPTED"));
	}

	@Test
	void acceptsValidReportViaReferer() throws Exception {
		mockMvc.perform(post("/api/v1/observability/client-errors").header("Referer", "http://localhost:5173/login")
				.contentType(MediaType.APPLICATION_JSON).content(validBody("Fallo de frontend")))
				.andExpect(status().isAccepted());
	}

	@Test
	void rateLimitsPerClient() throws Exception {
		String body = validBody("Fallo de frontend");
		for (int i = 0; i < 20; i++) {
			mockMvc.perform(post("/api/v1/observability/client-errors").header("Origin", "http://localhost:5173")
					.header("X-Forwarded-For", "203.0.113.10").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isAccepted());
		}

		mockMvc.perform(post("/api/v1/observability/client-errors").header("Origin", "http://localhost:5173")
				.header("X-Forwarded-For", "203.0.113.10").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.status").value("RATE_LIMITED"));
	}

	@Test
	void rateLimitBucketIsPerClient() throws Exception {
		String body = validBody("Fallo de frontend");
		for (int i = 0; i < 20; i++) {
			mockMvc.perform(post("/api/v1/observability/client-errors").header("Origin", "http://localhost:5173")
					.header("X-Forwarded-For", "203.0.113.10").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isAccepted());
		}

		mockMvc.perform(post("/api/v1/observability/client-errors").header("Origin", "http://localhost:5173")
				.header("X-Forwarded-For", "203.0.113.99").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isAccepted());
	}

	private String validBody(String message) {
		return "{\"message\":\"" + message + "\",\"stack\":\"stack traza\",\"url\":\"http://localhost:5173/app\","
				+ "\"component\":\"MiComponente\",\"errorType\":\"TypeError\"}";
	}
}
