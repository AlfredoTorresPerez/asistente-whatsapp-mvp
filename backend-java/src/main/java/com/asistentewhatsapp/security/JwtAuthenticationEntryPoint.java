package com.asistentewhatsapp.security;

import com.asistentewhatsapp.shared.api.ApiErrorResponse;
import com.asistentewhatsapp.shared.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(),
				new ApiErrorResponse(Instant.now(), HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
						"Debes iniciar sesion para continuar.", request.getRequestURI(),
						CorrelationIdFilter.currentCorrelationId(), Map.of()));
	}
}
