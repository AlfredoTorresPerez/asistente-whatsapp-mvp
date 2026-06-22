package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ConversationBookingController {

    private final BookingService bookingService;

    public ConversationBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping(
            value = {
                "/api/v1/conversations/{conversationId}/bookings",
                "/api/v1/conversations/{conversationId}/appointments"
            },
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingDetailResponse createFromConversation(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody CreateBookingFromConversationRequest request) {
        return bookingService.createFromConversation(authenticatedUser, conversationId, request);
    }
}
