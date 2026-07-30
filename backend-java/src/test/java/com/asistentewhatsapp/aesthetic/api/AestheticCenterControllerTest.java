package com.asistentewhatsapp.aesthetic.api;

import com.asistentewhatsapp.aesthetic.application.AestheticCenterService;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AestheticCenterController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AestheticCenterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@MockitoBean
	private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@MockitoBean
	private AestheticCenterService aestheticCenterService;

	@Test
	@WithMockUser(roles = "OWNER")
	void shouldListAestheticServices() throws Exception {
		when(aestheticCenterService.listServices(nullable(AuthenticatedUser.class), eq(0), eq(10), isNull(), isNull(),
				isNull()))
				.thenReturn(new PagedResponse<>(List.of(new AestheticServiceResponse(
						UUID.fromString("73000000-0000-0000-0000-000000000001"), "FAC-LIMPIEZA",
						"Limpieza facial profunda", "Limpieza facial con higienizacion.", "FACIAL",
						"Tratamientos faciales", 60, BigDecimal.valueOf(34990), "Cosmetologa facial", "Mascarilla",
						"Heridas abiertas", "Lunes a sabado", "Pedir datos minimos", "12 horas", "Usar protector solar",
						false, false, true, OffsetDateTime.parse("2026-05-29T10:00:00Z"),
						OffsetDateTime.parse("2026-05-29T10:00:00Z"), Collections.emptyList(),
						Collections.emptyList())), 0, 50, 1, 1));

		mockMvc.perform(get("/api/v1/esthetic/services")).andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].name").value("Limpieza facial profunda"))
				.andExpect(jsonPath("$.items[0].categoryName").value("Tratamientos faciales"))
				.andExpect(jsonPath("$.totalItems").value(1));
	}

	@Test
	@WithMockUser(roles = "OWNER")
	void shouldAnalyzeIntent() throws Exception {
		IntentAnalysisRequest request = new IntentAnalysisRequest(null, null,
				"Estoy embarazada, puedo hacerme drenaje linfatico?");
		when(aestheticCenterService.analyzeIntent(nullable(AuthenticatedUser.class), any(IntentAnalysisRequest.class)))
				.thenReturn(new IntentAnalysisResponse("derivar_atencion_humana", BigDecimal.valueOf(0.95),
						new IntentEntitiesResponse("Drenaje linfatico", null, null, null, null, null), true, true,
						"Consulta sensible: requiere evaluacion profesional.",
						"Por seguridad, esta consulta debe revisarla una profesional del centro.",
						"gpt-5.4-mini:fallback-rules"));

		mockMvc.perform(post("/api/v1/esthetic/intent/analyze").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.intencion").value("derivar_atencion_humana"))
				.andExpect(jsonPath("$.requiereDerivacionHumana").value(true))
				.andExpect(jsonPath("$.entidades.servicio").value("Drenaje linfatico"));
	}
}
