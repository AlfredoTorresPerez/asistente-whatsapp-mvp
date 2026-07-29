package com.asistentewhatsapp.notifications.api;

import com.asistentewhatsapp.notifications.application.NotificationService;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@MockitoBean
	private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@MockitoBean
	private NotificationService notificationService;

	@Test
	@WithMockUser(roles = "OWNER")
	void shouldListNotifications() throws Exception {
		when(notificationService.list(nullable(AuthenticatedUser.class), eq(0), eq(20), eq(null), eq(null), eq(null)))
				.thenReturn(new PagedResponse<>(
						List.of(new NotificationResponse(UUID.fromString("69800000-0000-0000-0000-000000000001"),
								"NEW_MESSAGE", "UNREAD", "Nuevo mensaje de Sofia Rojas",
								"Sofia consulto por limpieza facial y quedo una conversacion abierta.", "CONVERSATION",
								UUID.fromString("64000000-0000-0000-0000-000000000001"),
								OffsetDateTime.parse("2026-05-23T18:16:00Z"), null)),
						0, 20, 1, 1));

		mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].title").value("Nuevo mensaje de Sofia Rojas"))
				.andExpect(jsonPath("$.totalItems").value(1));
	}

	@Test
	@WithMockUser(roles = "OWNER")
	void shouldMarkNotificationAsRead() throws Exception {
		UUID notificationId = UUID.fromString("69800000-0000-0000-0000-000000000001");
		when(notificationService.markAsRead(nullable(AuthenticatedUser.class), eq(notificationId))).thenReturn(
				new NotificationReadResponse(notificationId, "READ", OffsetDateTime.parse("2026-05-23T18:25:00Z")));

		mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", notificationId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("READ"))
				.andExpect(jsonPath("$.readAt").value("2026-05-23T18:25:00Z"));
	}

	@Test
	@WithMockUser(roles = "OWNER")
	void shouldMarkAllNotificationsAsRead() throws Exception {
		when(notificationService.markAllAsRead(nullable(AuthenticatedUser.class)))
				.thenReturn(new NotificationsReadAllResponse(3));

		mockMvc.perform(patch("/api/v1/notifications/read-all")).andExpect(status().isOk())
				.andExpect(jsonPath("$.updatedCount").value(3));
	}
}
