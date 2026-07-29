package com.asistentewhatsapp.calendar.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asistentewhatsapp.calendar.application.CalendarIntegrationService;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.calendar.application.OAuthStateService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("SecurityCalendarPermissions - Permisos de seguridad en endpoints de calendario")
class SecurityCalendarPermissionsTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CalendarIntegrationService integrationService;

	@MockitoBean
	private CalendarSyncService calendarSyncService;

	@MockitoBean
	private OAuthStateService oAuthStateService;

	private static final UUID ACCOUNT_ID = UUID.randomUUID();
	private static final UUID BOOKING_ID = UUID.randomUUID();

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Usuario sin role OWNER/ADMIN/SUPERVISOR obtiene 403 en status (URL-level)")
	@WithMockUser(authorities = {"ROLE_AGENT"})
	void withoutOwnerRoleGets403OnStatus() throws Exception {
		mockMvc.perform(get("/api/v1/calendar-integrations/status")).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Usuario con ROLE_OWNER puede acceder a status (hasPermission pasa con el contexto real)")
	void withCalendarConfigViewCanAccessStatus() throws Exception {
		AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), "Test", "Test", "User",
				"test@demo.cl", "America/Santiago", List.of("OWNER"), List.of("CALENDAR_CONFIG_VIEW"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
		when(integrationService.getStatus(any())).thenReturn(java.util.List.of());
		mockMvc.perform(get("/api/v1/calendar-integrations/status")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Usuario con ROLE_AGENT obtiene 403 en disconnect")
	@WithMockUser(authorities = {"ROLE_AGENT"})
	void withoutCalendarConfigManageGets403OnDisconnect() throws Exception {
		mockMvc.perform(delete("/api/v1/calendar-integrations/{id}", ACCOUNT_ID)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Usuario con ROLE_AGENT obtiene 403 en select-calendar")
	@WithMockUser(authorities = {"ROLE_AGENT"})
	void withoutCalendarConfigManageGets403OnSelectCalendar() throws Exception {
		mockMvc.perform(post("/api/v1/calendar-integrations/{id}/select-calendar", ACCOUNT_ID).param("calendarId", "c1")
				.param("calendarSummary", "Cal")).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Usuario con ROLE_AGENT obtiene 403 en retry-sync")
	@WithMockUser(authorities = {"ROLE_AGENT"})
	void withoutCalendarConfigManageGets403OnRetrySync() throws Exception {
		mockMvc.perform(post("/api/v1/bookings/{id}/calendar-sync/retry", BOOKING_ID))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Usuario con ROLE_AGENT obtiene 403 en list-calendars")
	@WithMockUser(authorities = {"ROLE_AGENT"})
	void withoutCalendarConfigViewGets403OnListCalendars() throws Exception {
		mockMvc.perform(get("/api/v1/calendar-integrations/{id}/calendars", ACCOUNT_ID))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Callback de Google es accesible sin autenticación")
	void googleCallbackIsPublic() throws Exception {
		mockMvc.perform(get("/api/v1/calendar-integrations/google/callback").param("state", "s").param("code", "c"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	@DisplayName("Usuario con ROLE_AGENT obtiene 403 en sync-status")
	@WithMockUser(authorities = {"ROLE_AGENT"})
	void withoutBookingsUpdateGets403OnSyncStatus() throws Exception {
		mockMvc.perform(get("/api/v1/bookings/{id}/calendar-sync", BOOKING_ID)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Usuario no autenticado obtiene 401")
	void unauthenticatedGets401() throws Exception {
		mockMvc.perform(get("/api/v1/calendar-integrations/status")).andExpect(status().isUnauthorized());
	}
}
