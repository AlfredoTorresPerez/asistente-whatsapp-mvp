package com.asistentewhatsapp.calendar.api;

import com.asistentewhatsapp.calendar.application.CalendarIntegrationService;
import com.asistentewhatsapp.calendar.application.CalendarSyncService;
import com.asistentewhatsapp.calendar.application.OAuthStateService;
import com.asistentewhatsapp.calendar.infrastructure.GoogleCalendarHttpClient.GoogleCalendarApiException;
import com.asistentewhatsapp.calendar.provider.CalendarProvider.CalendarListEntry;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1")
public class CalendarIntegrationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(CalendarIntegrationController.class);

	private final CalendarIntegrationService integrationService;
	private final CalendarSyncService calendarSyncService;
	private final AuditService auditService;
	private final OAuthStateService oAuthStateService;
	private final String frontendBaseUrl;

	public CalendarIntegrationController(CalendarIntegrationService integrationService,
			CalendarSyncService calendarSyncService, AuditService auditService, OAuthStateService oAuthStateService,
			@Value("${app.frontend.public-base-url:http://localhost:5173}") String frontendBaseUrl) {
		this.integrationService = integrationService;
		this.calendarSyncService = calendarSyncService;
		this.auditService = auditService;
		this.oAuthStateService = oAuthStateService;
		this.frontendBaseUrl = frontendBaseUrl;
	}

	@PreAuthorize("hasPermission(#user.businessId(), 'CALENDAR_CONFIG_VIEW')")
	@GetMapping("/calendar-integrations/status")
	public ResponseEntity<List<CalendarAccountResponse>> getStatus(@AuthenticationPrincipal AuthenticatedUser user) {
		return ResponseEntity.ok(integrationService.getStatus(user.businessId()));
	}

	@PreAuthorize("hasPermission(#user.businessId(), 'CALENDAR_CONFIG_MANAGE')")
	@GetMapping("/calendar-integrations/google/connect")
	public RedirectView connectGoogle(@AuthenticationPrincipal AuthenticatedUser user) {
		String authUrl = integrationService.getAuthUrl(user.businessId(), "GOOGLE");
		LOGGER.info("CALENDAR_GOOGLE_CONNECT businessId={}", user.businessId());
		return new RedirectView(authUrl);
	}

	@GetMapping("/calendar-integrations/google/callback")
	public RedirectView googleCallback(@RequestParam String state, @RequestParam String code,
			@RequestParam(required = false) String error) {
		if ("access_denied".equals(error)) {
			LOGGER.info("CALENDAR_GOOGLE_CALLBACK_DENIED");
			return new RedirectView(frontendBaseUrl + "/configuration?calendar=denied");
		}
		try {
			integrationService.handleOAuthCallback(state, code);
			LOGGER.info("CALENDAR_GOOGLE_CALLBACK_SUCCESS");
			return new RedirectView(frontendBaseUrl + "/configuration?calendar=connected");
		} catch (IllegalArgumentException e) {
			LOGGER.warn("CALENDAR_GOOGLE_CALLBACK_INVALID_STATE reason={}", e.getMessage());
			return new RedirectView(frontendBaseUrl + "/configuration?calendar=error");
		} catch (GoogleCalendarApiException e) {
			LOGGER.warn("CALENDAR_GOOGLE_CALLBACK_API_ERROR statusCode={} reason={}", e.getStatusCode(),
					e.getMessage());
			return new RedirectView(frontendBaseUrl + "/configuration?calendar=error");
		} catch (Exception e) {
			LOGGER.error("CALENDAR_GOOGLE_CALLBACK_FAILED reason={}", e.getMessage());
			return new RedirectView(frontendBaseUrl + "/configuration?calendar=error");
		}
	}

	@PreAuthorize("hasPermission(#user.businessId(), 'CALENDAR_CONFIG_MANAGE')")
	@DeleteMapping("/calendar-integrations/{accountId}")
	public ResponseEntity<Map<String, String>> disconnect(@PathVariable UUID accountId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		integrationService.disconnect(accountId, user.businessId());
		return ResponseEntity.ok(Map.of("message", "Cuenta de calendario desvinculada."));
	}

	@PreAuthorize("hasPermission(#user.businessId(), 'CALENDAR_CONFIG_MANAGE')")
	@PostMapping("/calendar-integrations/{accountId}/select-calendar")
	public ResponseEntity<Map<String, String>> selectCalendar(@PathVariable UUID accountId,
			@RequestParam String calendarId, @RequestParam String calendarSummary,
			@AuthenticationPrincipal AuthenticatedUser user) {
		integrationService.selectCalendar(accountId, user.businessId(), calendarId, calendarSummary);
		return ResponseEntity.ok(Map.of("message", "Calendario seleccionado correctamente."));
	}

	@PreAuthorize("hasPermission(#user.businessId(), 'CALENDAR_CONFIG_VIEW')")
	@GetMapping("/calendar-integrations/{accountId}/calendars")
	public ResponseEntity<List<CalendarListEntry>> listCalendars(@PathVariable UUID accountId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		List<CalendarListEntry> calendars = integrationService.listCalendars(accountId, user.businessId());
		return ResponseEntity.ok(calendars);
	}

	@PreAuthorize("hasPermission(#user.businessId(), 'BOOKINGS_UPDATE')")
	@GetMapping("/bookings/{bookingId}/calendar-sync")
	public ResponseEntity<List<BookingSyncStatusResponse>> getSyncStatus(@PathVariable UUID bookingId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		var records = calendarSyncService.getSyncStatus(bookingId, user.businessId());
		var response = records.stream()
				.map(r -> new BookingSyncStatusResponse(r.id(), r.bookingId(), r.provider(), r.externalEventId(),
						r.syncStatus(), r.syncAction(), r.errorMessage(), r.retryCount(), r.lastSyncAttemptAt(),
						r.lastSuccessfulSyncAt()))
				.toList();
		return ResponseEntity.ok(response);
	}

	@PreAuthorize("hasPermission(#user.businessId(), 'CALENDAR_CONFIG_MANAGE')")
	@PostMapping("/bookings/{bookingId}/calendar-sync/retry")
	public ResponseEntity<Map<String, String>> retrySync(@PathVariable UUID bookingId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		calendarSyncService.retrySync(bookingId, user.businessId());
		return ResponseEntity.ok(Map.of("message", "Reintento de sincronizacion programado."));
	}
}
