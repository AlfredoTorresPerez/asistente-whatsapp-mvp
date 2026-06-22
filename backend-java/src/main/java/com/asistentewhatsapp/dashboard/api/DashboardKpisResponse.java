package com.asistentewhatsapp.dashboard.api;

public record DashboardKpisResponse(
        long openConversations,
        long newProspects,
        long openOrders,
        long pendingAppointments) {
}
