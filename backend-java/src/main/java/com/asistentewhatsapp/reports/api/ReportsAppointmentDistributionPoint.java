package com.asistentewhatsapp.reports.api;

public record ReportsAppointmentDistributionPoint(
        String status,
        String label,
        long count,
        double percentage) {
}
