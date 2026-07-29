package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.ChannelDispatchController;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChannelDispatchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ChannelDispatchControllerTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ObjectMapper objectMapper;

	private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@MockitoBean
	private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@MockitoBean
	private ChannelDispatchService channelDispatchService;

	private static final UUID BUSINESS_ID = UUID.randomUUID();

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() throws java.io.IOException {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		SecurityContextHolder.clearContext();

		doAnswer(invocation -> {
			jakarta.servlet.http.HttpServletResponse resp = invocation.getArgument(1);
			resp.setStatus(401);
			resp.setContentType("application/json");
			return null;
		}).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

		doAnswer(invocation -> {
			jakarta.servlet.http.HttpServletResponse resp = invocation.getArgument(1);
			resp.setStatus(403);
			resp.setContentType("application/json");
			return null;
		}).when(jwtAccessDeniedHandler).handle(any(), any(), any());
	}

	private void authenticateAsOwner() {
		AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), BUSINESS_ID, "Test Business", "Test", "User",
				"test@demo.cl", "America/Santiago", List.of("OWNER"), List.of("CHANNEL_MANAGE"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
	}

	@Test
	void shouldDispatchMessageWhenRequestIsValid() throws Exception {
		authenticateAsOwner();

		when(channelDispatchService.dispatch(any(ChannelDispatchRequest.class))).thenReturn(new ChannelDispatchResponse(
				MessageChannelType.WHATSAPP, "msg-123", "QUEUED", Instant.parse("2026-05-23T20:15:30Z")));

		ChannelDispatchRequest request = new ChannelDispatchRequest(BUSINESS_ID, MessageChannelType.WHATSAPP,
				"+56911112222", "Hola, este es un mensaje de prueba.");

		mockMvc.perform(post("/api/v1/channels/messages/dispatch").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isAccepted())
				.andExpect(jsonPath("$.channelType").value("WHATSAPP"))
				.andExpect(jsonPath("$.externalMessageId").value("msg-123"))
				.andExpect(jsonPath("$.status").value("QUEUED"));
	}

	@Test
	void shouldReturnValidationErrorsWhenRequestIsInvalid() throws Exception {
		authenticateAsOwner();

		String payload = """
				{
				  "businessId": null,
				  "channelType": "WHATSAPP",
				  "recipientPhone": "",
				  "body": ""
				}
				""";

		mockMvc.perform(
				post("/api/v1/channels/messages/dispatch").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.businessId").value("businessId es obligatorio"))
				.andExpect(jsonPath("$.fieldErrors.recipientPhone").value("recipientPhone es obligatorio"))
				.andExpect(jsonPath("$.fieldErrors.body").value("body es obligatorio"));
	}
}
