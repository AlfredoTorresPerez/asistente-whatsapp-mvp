package com.asistentewhatsapp.reports.api;

public record ReportsFunnelStageResponse(
        String name,
        long count,
        Double conversionFromPrevious,
        Double conversionFromFirst) {
}
