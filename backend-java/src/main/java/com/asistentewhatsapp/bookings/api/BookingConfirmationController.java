package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingConfirmationService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/confirmation-link")
public class BookingConfirmationController {

	private final BookingConfirmationService bookingConfirmationService;

	public BookingConfirmationController(BookingConfirmationService bookingConfirmationService) {
		this.bookingConfirmationService = bookingConfirmationService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_UPDATE')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingConfirmationLinkResponse create(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID bookingId,
			@Valid @RequestBody(required = false) CreateBookingConfirmationLinkRequest request) {
		return bookingConfirmationService.createConfirmationLink(authenticatedUser, bookingId, request);
	}
}
