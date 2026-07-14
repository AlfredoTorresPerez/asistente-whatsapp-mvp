package com.asistentewhatsapp.reports.api;

import java.util.List;

public record ReportsSummaryResponse(
        List<ReportsKpiItem> kpis,
        List<ReportsChannelResponse> channelDistribution,
        List<ReportsConversationPerformancePoint> conversationPerformance,
        List<ReportsAppointmentDistributionPoint> appointmentDistribution,
        List<ReportsAppointmentPerformancePoint> appointmentPerformance,
        List<ReportsFunnelStageResponse> conversionFunnel,
        ReportsProspectsResponse prospects) {
}
