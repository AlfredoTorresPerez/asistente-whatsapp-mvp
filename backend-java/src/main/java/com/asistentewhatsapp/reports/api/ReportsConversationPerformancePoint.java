package com.asistentewhatsapp.reports.api;

public record ReportsConversationPerformancePoint(String date, long received, long aiAnswered, long humanAnswered,
		long unanswered) {
}
