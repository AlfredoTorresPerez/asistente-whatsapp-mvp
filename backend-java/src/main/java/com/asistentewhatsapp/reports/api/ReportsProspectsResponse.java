package com.asistentewhatsapp.reports.api;

import java.util.List;

public record ReportsProspectsResponse(
        List<ReportsProspectRowResponse> items,
        long total,
        int page,
        int size) {
}
