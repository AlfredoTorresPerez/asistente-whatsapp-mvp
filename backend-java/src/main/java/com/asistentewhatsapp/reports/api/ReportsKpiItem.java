package com.asistentewhatsapp.reports.api;

public record ReportsKpiItem(
        String label,
        long currentValue,
        long previousValue,
        Double variationPercent,
        String help) {
}
