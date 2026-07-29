package com.asistentewhatsapp.reports.api;

import com.asistentewhatsapp.reports.application.ReportsService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ReportsController {

	private final ReportsService reportsService;

	public ReportsController(ReportsService reportsService) {
		this.reportsService = reportsService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'REPORTS_VIEW')")
	@GetMapping("/api/v1/reports/summary")
	public ReportsSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) UUID locationId, @RequestParam(required = false) UUID professionalId,
			@RequestParam(required = false) UUID serviceId, @RequestParam(required = false) String bookingStatus,
			@RequestParam(required = false) UUID ownerUserId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return reportsService.getSummary(authenticatedUser, from, to, locationId, professionalId, serviceId,
				bookingStatus, ownerUserId, page, size);
	}
}
