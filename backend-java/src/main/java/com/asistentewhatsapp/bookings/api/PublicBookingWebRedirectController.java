package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingConfirmationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class PublicBookingWebRedirectController {

	private final BookingConfirmationProperties bookingConfirmationProperties;
	private final String reschedulePublicBaseUrl;
	private final String cancellationPublicBaseUrl;
	private final String paymentPublicBaseUrl;

	public PublicBookingWebRedirectController(BookingConfirmationProperties bookingConfirmationProperties,
			@Value("${app.booking-reschedule.public-base-url}") String reschedulePublicBaseUrl,
			@Value("${app.booking-cancellation.public-base-url}") String cancellationPublicBaseUrl,
			@Value("${app.booking-payment.checkout-public-base-url}") String paymentPublicBaseUrl) {
		this.bookingConfirmationProperties = bookingConfirmationProperties;
		this.reschedulePublicBaseUrl = sanitizeBaseUrl(reschedulePublicBaseUrl);
		this.cancellationPublicBaseUrl = sanitizeBaseUrl(cancellationPublicBaseUrl);
		this.paymentPublicBaseUrl = sanitizeBaseUrl(paymentPublicBaseUrl);
	}

	@GetMapping("/reservas/confirmar/{token}")
	public RedirectView confirmation(@PathVariable String token) {
		return redirect(bookingConfirmationProperties.getPublicBaseUrl(), token);
	}

	@GetMapping("/reservas/reprogramar/{token}")
	public RedirectView reschedule(@PathVariable String token) {
		return redirect(reschedulePublicBaseUrl, token);
	}

	@GetMapping("/reservas/cancelar/{token}")
	public RedirectView cancellation(@PathVariable String token) {
		return redirect(cancellationPublicBaseUrl, token);
	}

	@GetMapping("/reservas/pagar/{paymentId}")
	public RedirectView payment(@PathVariable String paymentId) {
		return redirect(paymentPublicBaseUrl, paymentId);
	}

	private RedirectView redirect(String baseUrl, String token) {
		String targetUrl = UriComponentsBuilder.fromUriString(baseUrl).pathSegment(token).build().toUriString();
		RedirectView redirectView = new RedirectView(targetUrl);
		redirectView.setContextRelative(false);
		return redirectView;
	}

	private String sanitizeBaseUrl(String value) {
		if (value == null || value.isBlank()) {
			return "http://localhost:5173";
		}
		return value.trim().replaceAll("/+$", "");
	}
}
