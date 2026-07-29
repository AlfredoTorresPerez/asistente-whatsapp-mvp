package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.CompanyService;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.shared.exception.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompanyController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAccessDeniedHandler.class,
		JwtAuthenticationEntryPoint.class})
class AdminAuthorizationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private CompanyService companyService;

	@Test
	@WithMockUser(roles = "AGENT")
	void agentCannotModifyCompanySettings() throws Exception {
		mockMvc.perform(patch("/api/v1/company").contentType(MediaType.APPLICATION_JSON).content(validCompanyPayload()))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "AGENT")
	void agentCannotAccessSensitiveAdministrationRoutes() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/admin/security").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/security/audit-log")).andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/whatsapp-web/connect")).andExpect(status().isForbidden());

		mockMvc.perform(
				post("/api/channels/whatsapp-web/test-message").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "SUPERVISOR")
	void supervisorCanReadButCannotModifyCompanySettings() throws Exception {
		when(companyService.getCurrent(any())).thenReturn(companyResponse());

		mockMvc.perform(get("/api/v1/company")).andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/company").contentType(MediaType.APPLICATION_JSON).content(validCompanyPayload()))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanModifyCompanySettings() throws Exception {
		when(companyService.updateCurrent(any(), any())).thenReturn(companyResponse());

		mockMvc.perform(patch("/api/v1/company").contentType(MediaType.APPLICATION_JSON).content(validCompanyPayload()))
				.andExpect(status().isOk());
	}

	private String validCompanyPayload() {
		return """
				{
				  "companyName": "Centro Demo SpA",
				  "businessName": "Centro Demo",
				  "timezone": "America/Santiago",
				  "currency": "CLP",
				  "contactEmail": "admin@example.com",
				  "supportPhone": "+56911112222",
				  "address": "Santiago"
				}
				""";
	}

	private CompanySettingsResponse companyResponse() {
		return new CompanySettingsResponse(UUID.randomUUID(), "Centro Demo SpA", "Centro Demo", "America/Santiago",
				"CLP", "admin@example.com", "+56911112222", "Santiago");
	}
}
