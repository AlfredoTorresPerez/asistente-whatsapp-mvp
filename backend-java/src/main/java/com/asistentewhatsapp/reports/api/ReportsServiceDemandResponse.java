package com.asistentewhatsapp.reports.api;

import java.util.UUID;

public record ReportsServiceDemandResponse(UUID serviceId, String serviceName, long bookings, long estimatedRevenue) {
}
