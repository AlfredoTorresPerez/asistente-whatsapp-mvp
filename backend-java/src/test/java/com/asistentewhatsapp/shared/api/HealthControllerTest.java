package com.asistentewhatsapp.shared.api;

import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@MockitoBean
	private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Test
	void shouldReturnHealthStatusWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.service").value("backend-java"));
	}
}
