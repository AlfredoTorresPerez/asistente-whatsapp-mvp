package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingPaymentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingPaymentWebhookController {

    private final BookingPaymentService bookingPaymentService;

    public BookingPaymentWebhookController(BookingPaymentService bookingPaymentService) {
        this.bookingPaymentService = bookingPaymentService;
    }

    @PostMapping(value = "/api/v1/integrations/booking-payments/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingPaymentWebhookResponse handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Booking-Payment-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Booking-Payment-Signature", required = false) String signature) {
        return bookingPaymentService.handleWebhook(rawBody, timestamp, signature);
    }
}
