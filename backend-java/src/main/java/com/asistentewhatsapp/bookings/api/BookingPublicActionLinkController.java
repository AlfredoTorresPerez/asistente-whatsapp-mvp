package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingPublicActionService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class BookingPublicActionLinkController {

    private final BookingPublicActionService bookingPublicActionService;

    public BookingPublicActionLinkController(BookingPublicActionService bookingPublicActionService) {
        this.bookingPublicActionService = bookingPublicActionService;
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_RESCHEDULE')")
    @PostMapping(
            value = "/api/v1/bookings/{bookingId}/reschedule-link",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BookingPublicActionLinkResponse createRescheduleLink(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody CreateBookingRescheduleLinkRequest request) {
        return bookingPublicActionService.createRescheduleLink(authenticatedUser, bookingId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_CANCEL')")
    @PostMapping(
            value = "/api/v1/bookings/{bookingId}/cancellation-link",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BookingPublicActionLinkResponse createCancellationLink(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @RequestBody(required = false) CreateBookingCancellationLinkRequest request) {
        return bookingPublicActionService.createCancellationLink(authenticatedUser, bookingId, request);
    }
}
