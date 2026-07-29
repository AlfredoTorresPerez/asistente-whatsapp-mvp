package com.asistentewhatsapp.dashboard.api;

import java.util.List;

public record DashboardSummaryResponse(DashboardKpisResponse kpis,
		List<DashboardSeriesPointResponse> conversationSeries, List<DashboardSeriesPointResponse> orderSeries,
		List<DashboardAppointmentResponse> todayAppointments, List<DashboardActivityResponse> recentActivity) {
}
