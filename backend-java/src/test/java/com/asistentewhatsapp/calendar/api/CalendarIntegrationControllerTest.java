package com.asistentewhatsapp.calendar.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asistentewhatsapp.calendar.application.CalendarIntegrationService;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.calendar.application.OAuthStateService;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.CalendarListEntry;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(controllers = CalendarIntegrationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("CalendarIntegrationController - API REST de integración de calendario")
class CalendarIntegrationControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private CalendarIntegrationService integrationService;

    @MockitoBean
    private CalendarSyncService calendarSyncService;

    @MockitoBean
    private OAuthStateService oAuthStateService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws java.io.IOException {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), BUSINESS_ID, "Test", "Test", "User",
                "test@demo.cl", "America/Santiago",
                List.of("OWNER"),
                List.of("CALENDAR_CONFIG_VIEW", "CALENDAR_CONFIG_MANAGE", "BOOKINGS_UPDATE"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("GET /api/v1/calendar-integrations/status retorna cuentas")
    void getStatusReturnsAccounts() throws Exception {
        authenticateAsOwner();
        when(integrationService.getStatus(any())).thenReturn(List.of(
                new CalendarAccountResponse(ACCOUNT_ID, "GOOGLE", "te***@demo.cl",
                        "primary", "Calendar", true,
                        OffsetDateTime.now(), null, false, null, "CONNECTED")));

        mockMvc.perform(get("/api/v1/calendar-integrations/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("GOOGLE"))
                .andExpect(jsonPath("$[0].authorizationStatus").value("CONNECTED"));
    }

    @Test
    @DisplayName("GET /api/v1/calendar-integrations/google/connect redirige a auth URL")
    void connectGoogleRedirectsToAuthUrl() throws Exception {
        authenticateAsOwner();
        when(integrationService.getAuthUrl(any(), eq("GOOGLE")))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test");

        mockMvc.perform(get("/api/v1/calendar-integrations/google/connect"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "https://accounts.google.com/o/oauth2/v2/auth?state=test"));
    }

    @Test
    @DisplayName("GET /api/v1/calendar-integrations/google/callback con error=access_denied redirige a denied")
    void googleCallbackWithAccessDeniedRedirectsToDenied() throws Exception {
        mockMvc.perform(get("/api/v1/calendar-integrations/google/callback")
                        .param("state", "s")
                        .param("code", "c")
                        .param("error", "access_denied"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "http://localhost:5173/configuration?calendar=denied"));
    }

    @Test
    @DisplayName("GET /api/v1/calendar-integrations/google/callback con state+code válido redirige a connected")
    void googleCallbackWithValidStateAndCodeRedirectsToConnected() throws Exception {
        when(integrationService.handleOAuthCallback(eq("valid-state"), eq("valid-code")))
                .thenReturn(new CalendarAccountResponse(ACCOUNT_ID, "GOOGLE", "te***@demo.cl",
                        "primary", "Calendar", true,
                        OffsetDateTime.now(), null, false, null, "CONNECTED"));

        mockMvc.perform(get("/api/v1/calendar-integrations/google/callback")
                        .param("state", "valid-state")
                        .param("code", "valid-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "http://localhost:5173/configuration?calendar=connected"));
    }

    @Test
    @DisplayName("GET /api/v1/calendar-integrations/google/callback con error de callback redirige a error")
    void googleCallbackWithErrorRedirectsToError() throws Exception {
        when(integrationService.handleOAuthCallback(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid state"));

        mockMvc.perform(get("/api/v1/calendar-integrations/google/callback")
                        .param("state", "bad")
                        .param("code", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "http://localhost:5173/configuration?calendar=error"));
    }

    @Test
    @DisplayName("DELETE /api/v1/calendar-integrations/{id} desconecta cuenta")
    void disconnectDeletesAccount() throws Exception {
        authenticateAsOwner();
        mockMvc.perform(delete("/api/v1/calendar-integrations/{accountId}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta de calendario desvinculada."));
        verify(integrationService).disconnect(eq(ACCOUNT_ID), any());
    }

    @Test
    @DisplayName("POST /api/v1/calendar-integrations/{id}/select-calendar selecciona calendario")
    void selectCalendarSelectsCalendar() throws Exception {
        authenticateAsOwner();
        mockMvc.perform(post("/api/v1/calendar-integrations/{accountId}/select-calendar", ACCOUNT_ID)
                        .param("calendarId", "cal-1")
                        .param("calendarSummary", "My Calendar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Calendario seleccionado correctamente."));
        verify(integrationService).selectCalendar(eq(ACCOUNT_ID), any(), eq("cal-1"), eq("My Calendar"));
    }

    @Test
    @DisplayName("GET /api/v1/calendar-integrations/{id}/calendars retorna lista")
    void listCalendarsReturnsList() throws Exception {
        authenticateAsOwner();
        when(integrationService.listCalendars(eq(ACCOUNT_ID), any()))
                .thenReturn(List.of(new CalendarListEntry("cal-1", "My Calendar", true, "owner")));

        mockMvc.perform(get("/api/v1/calendar-integrations/{accountId}/calendars", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cal-1"))
                .andExpect(jsonPath("$[0].summary").value("My Calendar"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id}/calendar-sync retorna estado de sync")
    void getSyncStatusReturnsStatus() throws Exception {
        authenticateAsOwner();
        mockMvc.perform(get("/api/v1/bookings/{bookingId}/calendar-sync", BOOKING_ID))
                .andExpect(status().isOk());
        verify(calendarSyncService).getSyncStatus(eq(BOOKING_ID), any());
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/calendar-sync/retry reintenta sync")
    void retrySyncRetriesSync() throws Exception {
        authenticateAsOwner();
        mockMvc.perform(post("/api/v1/bookings/{bookingId}/calendar-sync/retry", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reintento de sincronizacion programado."));
        verify(calendarSyncService).retrySync(eq(BOOKING_ID), any());
    }

    @Test
    @DisplayName("Callback de Google es público")
    void googleCallbackIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/calendar-integrations/google/callback")
                        .param("state", "s").param("code", "c"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Usuario no autenticado recibe 401")
    void unauthorizedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/calendar-integrations/status"))
                .andExpect(status().isUnauthorized());
    }
}
