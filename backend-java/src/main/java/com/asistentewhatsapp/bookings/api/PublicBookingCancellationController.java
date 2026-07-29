package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingPublicActionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/booking-cancellations/{token}")
public class PublicBookingCancellationController {

	private final BookingPublicActionService bookingPublicActionService;

	public PublicBookingCancellationController(BookingPublicActionService bookingPublicActionService) {
		this.bookingPublicActionService = bookingPublicActionService;
	}

	@GetMapping
	public PublicBookingCancellationResponse preview(@PathVariable String token) {
		return bookingPublicActionService.previewCancellation(token);
	}

	@PostMapping("/confirm")
	public PublicBookingCancellationResponse confirm(@PathVariable String token,
			@RequestBody(required = false) PublicBookingCancellationRequest request) {
		return bookingPublicActionService.confirmCancellation(token, request);
	}

	@GetMapping("/confirm")
	public PublicBookingCancellationResponse confirmViaGetFallback(@PathVariable String token) {
		return bookingPublicActionService.confirmCancellation(token, null);
	}
}
