package com.asistentewhatsapp.reports.api;

public record ReportsAppointmentPerformancePoint(
        String date,
        long solicitada,
        long confirmada,
        long completada,
        long cancelada,
        long ausencia) {
}
