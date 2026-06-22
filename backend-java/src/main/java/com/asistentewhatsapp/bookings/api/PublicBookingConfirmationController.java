package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.bookings.application.BookingConfirmationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/booking-confirmations/{token}")
public class PublicBookingConfirmationController {

    private final BookingConfirmationService bookingConfirmationService;

    public PublicBookingConfirmationController(BookingConfirmationService bookingConfirmationService) {
        this.bookingConfirmationService = bookingConfirmationService;
    }

    @GetMapping
    public PublicBookingConfirmationResponse preview(@PathVariable String token) {
        return bookingConfirmationService.preview(token);
    }

    @PostMapping("/confirm")
    public PublicBookingConfirmationResponse confirm(@PathVariable String token) {
        return bookingConfirmationService.confirm(token);
    }

    @GetMapping("/confirm")
    public PublicBookingConfirmationResponse confirmViaPublicGetFallback(@PathVariable String token) {
        return bookingConfirmationService.confirm(token);
    }

    @GetMapping("/availability")
    public AgendaAvailabilityResponse availability(
            @PathVariable String token,
            @RequestParam LocalDate date,
            @RequestParam(required = false) Integer maxSlots) {
        return bookingConfirmationService.publicAvailability(token, date, maxSlots);
    }

    @PostMapping("/reschedule")
    public PublicBookingConfirmationResponse reschedule(
            @PathVariable String token,
            @Valid @RequestBody PublicBookingRescheduleRequest request) {
        return bookingConfirmationService.rescheduleFromConfirmation(token, request);
    }

    @PostMapping("/cancel")
    public PublicBookingConfirmationResponse cancel(
            @PathVariable String token,
            @RequestBody PublicBookingCancellationRequest request) {
        return bookingConfirmationService.cancelFromConfirmation(token, request);
    }
}

