package com.asistentewhatsapp.agenda.api;

import com.asistentewhatsapp.agenda.application.CompleteDigitalAgendaService;
import com.asistentewhatsapp.bookings.api.BookingDetailResponse;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api/v1/agenda", "/api/agenda"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class CompleteDigitalAgendaController {

    private final CompleteDigitalAgendaService completeDigitalAgendaService;

    public CompleteDigitalAgendaController(CompleteDigitalAgendaService completeDigitalAgendaService) {
        this.completeDigitalAgendaService = completeDigitalAgendaService;
    }

    @PostMapping(value = "/availability", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AgendaAvailabilityResponse availability(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AgendaAvailabilityRequest request) {
        return completeDigitalAgendaService.availability(authenticatedUser, request);
    }

    @PostMapping(value = "/temporary-bookings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDetailResponse createTemporaryBooking(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateTemporaryAgendaBookingRequest request) {
        return completeDigitalAgendaService.createTemporaryBooking(authenticatedUser, request);
    }


    @GetMapping("/filter-options")
    public AgendaFilterOptionsResponse filterOptions(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) UUID locationId) {
        return completeDigitalAgendaService.filterOptions(authenticatedUser, locationId);
    }

    @GetMapping("/calendar")
    public AgendaCalendarResponse calendar(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) String status) {
        return completeDigitalAgendaService.calendar(authenticatedUser, from, to, locationId, professionalId, roomId, serviceId, status);
    }

    @PatchMapping(value = "/bookings/{bookingId}/reschedule", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingDetailResponse reschedule(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody AgendaRescheduleRequest request) {
        return completeDigitalAgendaService.reschedule(authenticatedUser, bookingId, request);
    }

    @PatchMapping(value = "/bookings/{bookingId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingDetailResponse cancel(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody AgendaCancelRequest request) {
        return completeDigitalAgendaService.cancel(authenticatedUser, bookingId, request);
    }

    @PostMapping(value = "/blocks", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaBlockResponse createBlock(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AgendaBlockRequest request) {
        return completeDigitalAgendaService.createBlock(authenticatedUser, request);
    }
}
