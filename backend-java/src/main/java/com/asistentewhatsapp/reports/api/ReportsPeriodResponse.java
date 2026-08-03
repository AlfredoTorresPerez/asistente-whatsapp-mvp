package com.asistentewhatsapp.reports.api;

import java.time.LocalDate;

public record ReportsPeriodResponse(LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo,
		String timezone) {
}
