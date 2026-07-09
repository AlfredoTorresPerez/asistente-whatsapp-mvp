package com.asistentewhatsapp.landing.api;

import com.asistentewhatsapp.agenda.api.AgendaAvailabilityRequest;
import com.asistentewhatsapp.agenda.api.AgendaAvailabilityResponse;
import com.asistentewhatsapp.landing.application.PublicLandingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/public/landing", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicLandingController {

    private static final Logger log = LoggerFactory.getLogger(PublicLandingController.class);

    private final PublicLandingService publicLandingService;

    public PublicLandingController(PublicLandingService publicLandingService) {
        this.publicLandingService = publicLandingService;
    }

    @GetMapping
    public LandingPageResponse landing() {
        return publicLandingService.landing();
    }

    @GetMapping("/categories")
    public List<PublicCategoryResponse> categories() {
        return publicLandingService.categories();
    }

    @GetMapping("/categories/{categoryCode}/services")
    public List<LandingServiceItemResponse> servicesByCategory(@PathVariable String categoryCode) {
        return publicLandingService.servicesByCategory(categoryCode);
    }

    @GetMapping("/services/{serviceId}")
    public PublicServiceDetailResponse serviceDetail(@PathVariable UUID serviceId) {
        return publicLandingService.serviceDetail(serviceId);
    }

    @GetMapping("/services/{serviceId}/branches")
    public List<PublicServiceBranchResponse> serviceBranches(@PathVariable UUID serviceId) {
        return publicLandingService.serviceBranches(serviceId);
    }

    @PostMapping("/availability")
    public AgendaAvailabilityResponse availability(@Valid @RequestBody AgendaAvailabilityRequest request) {
        log.info(">>> availability request date={}, class={}", request.date(), request.date().getClass().getName());
        return publicLandingService.availability(request);
    }

    @GetMapping("/customer-info/{token}")
    public PublicCustomerInfoResponse customerInfo(@PathVariable String token) {
        return publicLandingService.getCustomerInfo(token);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createBooking(@Valid @RequestBody CreatePublicBookingRequest request) {
        log.info(">>> createBooking RAW startsAt={}, class={}", request.startsAt(), request.startsAt().getClass().getName());
        log.info(">>> createBooking startsAt year={} month={} day={} hour={} offset={}",
                request.startsAt().getYear(), request.startsAt().getMonthValue(), request.startsAt().getDayOfMonth(),
                request.startsAt().getHour(), request.startsAt().getOffset());
        UUID bookingId = publicLandingService.createBooking(request);
        return Map.of("bookingId", bookingId);
    }
}
