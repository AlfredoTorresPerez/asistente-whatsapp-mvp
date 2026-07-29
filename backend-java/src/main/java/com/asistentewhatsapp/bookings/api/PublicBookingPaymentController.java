package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingPaymentService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/booking-payments/{paymentId}")
public class PublicBookingPaymentController {

	private final BookingPaymentService bookingPaymentService;

	public PublicBookingPaymentController(BookingPaymentService bookingPaymentService) {
		this.bookingPaymentService = bookingPaymentService;
	}

	@GetMapping
	public BookingPaymentResponse getPaymentStatus(@PathVariable UUID paymentId) {
		return bookingPaymentService.getPaymentStatus(paymentId);
	}

	@GetMapping("/detail")
	public PublicBookingPaymentDetailResponse getPaymentDetail(@PathVariable UUID paymentId) {
		return bookingPaymentService.getPublicPaymentDetail(paymentId);
	}

	@PostMapping(value = "/simulate", consumes = MediaType.APPLICATION_JSON_VALUE)
	public BookingPaymentResponse simulatePayment(@PathVariable UUID paymentId, @RequestBody Map<String, String> body) {
		String action = body != null ? body.get("action") : null;
		return bookingPaymentService.simulatePayment(paymentId, action);
	}
}
