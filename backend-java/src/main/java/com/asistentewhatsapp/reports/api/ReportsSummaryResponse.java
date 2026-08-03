package com.asistentewhatsapp.reports.api;

import java.util.List;

public record ReportsSummaryResponse(ReportsPeriodResponse period, List<ReportsKpiItem> kpis,
		List<ReportsKpiItem> operationalKpis, List<ReportsOccupancyResponse> occupancyByProfessional,
		List<ReportsOccupancyResponse> occupancyByRoom, List<ReportsOccupancyResponse> occupancyByLocation,
		List<ReportsServiceDemandResponse> topServices, List<ReportsChannelResponse> channelDistribution,
		List<ReportsConversationPerformancePoint> conversationPerformance,
		List<ReportsAppointmentDistributionPoint> appointmentDistribution,
		List<ReportsAppointmentPerformancePoint> appointmentPerformance,
		List<ReportsFunnelStageResponse> conversionFunnel, ReportsProspectsResponse prospects) {
}
