package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingPaymentService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
