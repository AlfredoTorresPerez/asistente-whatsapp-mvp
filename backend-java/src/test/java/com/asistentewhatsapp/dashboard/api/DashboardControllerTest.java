package com.asistentewhatsapp.dashboard.api;

import com.asistentewhatsapp.dashboard.application.DashboardService;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
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

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    @WithMockUser(roles = "OWNER")
    void shouldReturnDashboardSummary() throws Exception {
        when(dashboardService.getSummary(nullable(AuthenticatedUser.class), isNull(), isNull(), isNull()))
                .thenReturn(new DashboardSummaryResponse(
                        new DashboardKpisResponse(2, 3, 1, 4),
                        List.of(new DashboardSeriesPointResponse("2026-05-23", 2)),
                        List.of(new DashboardSeriesPointResponse("2026-05-23", 1)),
                        List.of(new DashboardAppointmentResponse(
                                UUID.fromString("68000000-0000-0000-0000-000000000001"),
                                "Evaluacion facial inicial",
                                "SCHEDULED",
                                "Sofia Rojas",
                                OffsetDateTime.parse("2026-05-27T14:00:00Z"),
                                45,
                                "Sucursal Providencia")),
                        List.of(new DashboardActivityResponse(
                                "CONVERSATION",
                                UUID.fromString("64000000-0000-0000-0000-000000000001"),
                                "Nuevo mensaje de Sofia Rojas",
                                "Quiero saber el precio",
                                "OPEN",
                                OffsetDateTime.parse("2026-05-23T18:10:00Z")))));

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.openConversations").value(2))
                .andExpect(jsonPath("$.todayAppointments[0].customerName").value("Sofia Rojas"))
                .andExpect(jsonPath("$.recentActivity[0].entityType").value("CONVERSATION"));
    }
}
