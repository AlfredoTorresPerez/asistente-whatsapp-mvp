package com.asistentewhatsapp.customerbookings.api;

import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.agenda.api.AgendaCalendarItemResponse;
import com.asistentewhatsapp.customerbookings.application.CustomerBookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/public/customer-bookings", produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomerBookingsController {

	private final CustomerBookingService customerBookingService;

	public CustomerBookingsController(CustomerBookingService customerBookingService) {
		this.customerBookingService = customerBookingService;
	}

	@GetMapping("/{token}")
	public List<CustomerBookingItemResponse> listBookings(@PathVariable String token) {
		return customerBookingService.findActiveBookingsByToken(token).stream().map(this::toItemResponse).toList();
	}

	@GetMapping("/{token}/{bookingId}")
	public CustomerBookingItemResponse bookingDetail(@PathVariable String token, @PathVariable UUID bookingId) {
		return toItemResponse(customerBookingService.findActiveBookingById(token, bookingId));
	}

	@PostMapping("/{token}/{bookingId}/cancel")
	public Map<String, Object> cancelBooking(@PathVariable String token, @PathVariable UUID bookingId,
			@RequestBody(required = false) Map<String, String> body) {
		String reason = body == null ? null : body.get("reason");
		customerBookingService.cancelBooking(token, bookingId, reason);
		return Map.of("status", "CANCELADA_POR_CLIENTE", "bookingId", bookingId.toString());
	}

	@GetMapping("/{token}/{bookingId}/reschedule")
	public CustomerBookingReschedulePreviewResponse previewReschedule(@PathVariable String token,
			@PathVariable UUID bookingId) {
		return customerBookingService.previewReschedule(token, bookingId);
	}

	@GetMapping("/{token}/{bookingId}/reschedule/availability")
	public AgendaAvailabilityResponse availability(@PathVariable String token, @PathVariable UUID bookingId,
			@RequestParam UUID serviceId, @RequestParam UUID locationId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return customerBookingService.getAvailability(token, bookingId, serviceId, locationId, date);
	}

	@PostMapping("/{token}/{bookingId}/reschedule")
	public CustomerBookingItemResponse rescheduleBooking(@PathVariable String token, @PathVariable UUID bookingId,
			@Valid @RequestBody CustomerBookingRescheduleRequest request) {
		AgendaCalendarItemResponse result = customerBookingService.rescheduleBooking(token, bookingId, request);
		return toItemResponse(result);
	}

	private CustomerBookingItemResponse toItemResponse(AgendaCalendarItemResponse item) {
		return new CustomerBookingItemResponse(item.bookingId(), item.locationId(), item.serviceId(),
				item.professionalId(), item.roomId(), item.serviceName() != null ? item.serviceName() : item.subject(),
				item.locationName(), item.professionalName(), item.startsAt(), item.endsAt(), item.durationMinutes(),
				item.status(), item.customerName(), maskPhone(item.customerPhone()));
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.length() <= 4)
			return "****";
		return "****" + phone.substring(phone.length() - 4);
	}
}
