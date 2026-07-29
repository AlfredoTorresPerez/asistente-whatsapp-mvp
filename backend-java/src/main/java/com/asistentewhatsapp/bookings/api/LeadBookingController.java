package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class LeadBookingController {

	private final BookingService bookingService;

	public LeadBookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_CREATE')")
	@PostMapping(value = {"/api/v1/prospects/{leadId}/bookings", "/api/v1/prospects/{leadId}/appointments",
			"/api/v1/leads/{leadId}/bookings"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public BookingDetailResponse createFromLead(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID leadId, @Valid @RequestBody CreateBookingFromLeadRequest request) {
		return bookingService.createFromLead(authenticatedUser, leadId, request);
	}
}
