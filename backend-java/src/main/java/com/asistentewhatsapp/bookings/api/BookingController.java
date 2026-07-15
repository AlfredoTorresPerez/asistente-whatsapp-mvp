package com.asistentewhatsapp.bookings.api;

import com.asistentewhatsapp.bookings.application.BookingService;
import com.asistentewhatsapp.bookings.application.BookingPaymentService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class BookingController {

    private final BookingService bookingService;
    private final BookingPaymentService bookingPaymentService;

    public BookingController(BookingService bookingService, BookingPaymentService bookingPaymentService) {
        this.bookingService = bookingService;
        this.bookingPaymentService = bookingPaymentService;
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AGENDA_VIEW')")
    @GetMapping({"/api/bookings", "/api/v1/bookings", "/api/v1/appointments"})
    public PagedResponse<BookingSummaryResponse> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) UUID responsibleUserId) {
        UUID resolvedResponsibleUserId = assignedUserId != null
                ? assignedUserId
                : responsibleUserId != null ? responsibleUserId : ownerUserId;
        return bookingService.list(
                authenticatedUser,
                page,
                size,
                from,
                to,
                search,
                status,
                resolvedResponsibleUserId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_CREATE')")
    @PostMapping(
            value = {"/api/bookings", "/api/v1/bookings", "/api/v1/appointments"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingDetailResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AGENDA_VIEW')")
    @GetMapping({"/api/bookings/{bookingId}", "/api/v1/bookings/{bookingId}", "/api/v1/appointments/{bookingId}"})
    public BookingDetailResponse detail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId) {
        return bookingService.getDetail(authenticatedUser, bookingId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_UPDATE')")
    @PutMapping(
            value = {"/api/bookings/{bookingId}", "/api/v1/bookings/{bookingId}", "/api/v1/appointments/{bookingId}"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingDetailResponse update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateBookingRequest request) {
        return bookingService.update(authenticatedUser, bookingId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_RESCHEDULE')")
    @PatchMapping(
            value = {
                "/api/bookings/{bookingId}/reschedule",
                "/api/v1/bookings/{bookingId}/reschedule",
                "/api/v1/appointments/{bookingId}/reschedule"
            },
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingDetailResponse reschedule(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody RescheduleBookingRequest request) {
        return bookingService.reschedule(authenticatedUser, bookingId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_CANCEL')")
    @PatchMapping(
            value = {
                "/api/bookings/{bookingId}/cancel",
                "/api/v1/bookings/{bookingId}/cancel",
                "/api/v1/appointments/{bookingId}/cancel"
            },
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingDetailResponse cancel(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody CancelBookingRequest request) {
        return bookingService.cancel(authenticatedUser, bookingId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_UPDATE')")
    @GetMapping({"/api/bookings/{bookingId}/payments", "/api/v1/bookings/{bookingId}/payments", "/api/v1/appointments/{bookingId}/payments"})
    public List<BookingPaymentResponse> payments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId) {
        return bookingPaymentService.listPayments(authenticatedUser, bookingId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_UPDATE')")
    @PostMapping(
            value = {
                "/api/bookings/{bookingId}/payment-link",
                "/api/v1/bookings/{bookingId}/payment-link",
                "/api/v1/appointments/{bookingId}/payment-link"
            },
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingPaymentResponse createPaymentLink(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody(required = false) CreateBookingPaymentLinkRequest request) {
        return bookingPaymentService.createCheckoutLink(authenticatedUser, bookingId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_UPDATE')")
    @PostMapping(
            value = {
                "/api/bookings/{bookingId}/payments/manual",
                "/api/v1/bookings/{bookingId}/payments/manual",
                "/api/v1/appointments/{bookingId}/payments/manual"
            },
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingPaymentResponse registerManualPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @Valid @RequestBody RegisterBookingManualPaymentRequest request) {
        return bookingPaymentService.registerManualPayment(authenticatedUser, bookingId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BOOKINGS_CANCEL')")
    @PatchMapping(
            value = {
                "/api/bookings/{bookingId}/payments/{paymentId}/refund",
                "/api/v1/bookings/{bookingId}/payments/{paymentId}/refund",
                "/api/v1/appointments/{bookingId}/payments/{paymentId}/refund"
            },
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookingPaymentResponse refundPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID bookingId,
            @PathVariable UUID paymentId,
            @RequestBody(required = false) RefundBookingPaymentRequest request) {
        return bookingPaymentService.refundPayment(authenticatedUser, bookingId, paymentId, request);
    }
}
