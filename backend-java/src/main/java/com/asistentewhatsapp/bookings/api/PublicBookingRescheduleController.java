package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingPublicActionService;
import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/booking-reschedules/{token}")
public class PublicBookingRescheduleController {

    private final BookingPublicActionService bookingPublicActionService;

    public PublicBookingRescheduleController(BookingPublicActionService bookingPublicActionService) {
        this.bookingPublicActionService = bookingPublicActionService;
    }

    @GetMapping
    public PublicBookingRescheduleResponse preview(@PathVariable String token) {
        return bookingPublicActionService.previewReschedule(token);
    }

    @PostMapping("/confirm")
    public PublicBookingRescheduleResponse confirm(@PathVariable String token) {
        return bookingPublicActionService.confirmReschedule(token);
    }

    @GetMapping("/confirm")
    public PublicBookingRescheduleResponse confirmViaGetFallback(@PathVariable String token) {
        return bookingPublicActionService.confirmReschedule(token);
    }

    @PostMapping("/reject")
    public PublicBookingRescheduleResponse reject(@PathVariable String token) {
        return bookingPublicActionService.rejectReschedule(token);
    }

    @GetMapping("/reject")
    public PublicBookingRescheduleResponse rejectViaGetFallback(@PathVariable String token) {
        return bookingPublicActionService.rejectReschedule(token);
    }

    @GetMapping("/{bookingId}/availability")
    public AgendaAvailabilityResponse availability(
            @PathVariable String token,
            @PathVariable UUID bookingId,
            @RequestParam UUID serviceId,
            @RequestParam UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingPublicActionService.getRescheduleAvailability(token, bookingId, serviceId, locationId, date);
    }

    @PostMapping("/{bookingId}/reschedule")
    public com.asistentewhatsapp.customerbookings.api.CustomerBookingItemResponse rescheduleBooking(
            @PathVariable String token,
            @PathVariable UUID bookingId,
            @Valid @RequestBody PublicBookingRescheduleRequest request) {
        return bookingPublicActionService.rescheduleBooking(token, bookingId, request);
    }
}
