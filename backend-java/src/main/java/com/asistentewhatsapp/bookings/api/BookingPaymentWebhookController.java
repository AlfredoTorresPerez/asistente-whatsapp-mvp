package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingPaymentService;
import com.asistentewhatsapp.bookings.application.BookingPaymentProperties;
import com.asistentewhatsapp.bookings.infrastructure.MercadoPagoPaymentProvider;
import com.asistentewhatsapp.bookings.infrastructure.SimulatedPaymentProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
public class BookingPaymentWebhookController {

    private final BookingPaymentService bookingPaymentService;
    private final BookingPaymentProperties properties;

    public BookingPaymentWebhookController(
            BookingPaymentService bookingPaymentService,
            BookingPaymentProperties properties) {
        this.bookingPaymentService = bookingPaymentService;
        this.properties = properties;
    }

    @PostMapping(value = "/api/v1/integrations/booking-payments/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingPaymentWebhookResponse handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Booking-Payment-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Booking-Payment-Signature", required = false) String signature,
            @RequestHeader(value = "x-signature", required = false) String mpSignature,
            @RequestHeader(value = "x-request-id", required = false) String mpRequestId) {

        String provider = detectProvider(mpSignature, mpRequestId);
        Map<String, String> headers = new LinkedHashMap<>();
        if (MercadoPagoPaymentProvider.NAME.equals(provider)) {
            if (mpSignature != null) headers.put("x-signature", mpSignature);
            if (mpRequestId != null) headers.put("x-request-id", mpRequestId);
        } else {
            if (timestamp != null) headers.put("X-Booking-Payment-Timestamp", timestamp);
            if (signature != null) headers.put("X-Booking-Payment-Signature", signature);
        }
        return bookingPaymentService.handleWebhook(rawBody, provider, headers);
    }

    private String detectProvider(String mpSignature, String mpRequestId) {
        if (mpSignature != null || mpRequestId != null) {
            return MercadoPagoPaymentProvider.NAME;
        }
        return properties.getProvider() != null ? properties.getProvider() : SimulatedPaymentProvider.NAME;
    }
}
