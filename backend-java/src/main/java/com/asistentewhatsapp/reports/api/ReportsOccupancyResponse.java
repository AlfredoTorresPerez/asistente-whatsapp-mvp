package com.asistentewhatsapp.reports.api;

import java.util.UUID;

public record ReportsOccupancyResponse(UUID id, String name, long availableMinutes, long reservedMinutes,
		Double occupancyPercent) {
}
