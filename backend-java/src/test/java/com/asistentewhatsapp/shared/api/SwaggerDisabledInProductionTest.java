package com.asistentewhatsapp.shared.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerDisabledInProductionTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void swaggerUiShouldNotBeAccessibleWhenDisabled() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().is(greaterThanOrEqualTo(400)));
	}

	@Test
	void apiDocsShouldNotBeAccessibleWhenDisabled() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().is(greaterThanOrEqualTo(400)));
	}
}
