package com.asistentewhatsapp.multisite.api;

import com.asistentewhatsapp.multisite.application.MultisiteService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/multisite", produces = MediaType.APPLICATION_JSON_VALUE)
public class MultisiteController {

    private final MultisiteService multisiteService;

    public MultisiteController(MultisiteService multisiteService) {
        this.multisiteService = multisiteService;
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping("/summary")
    public List<MultisiteLocationSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return multisiteService.locationSummary(authenticatedUser);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping("/catalog-availability")
    public List<MultisiteCatalogAvailabilityResponse> catalogAvailability(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) UUID locationId) {
        return multisiteService.catalogAvailability(authenticatedUser, locationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @PutMapping(value = "/catalog-availability", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MultisiteCatalogAvailabilityResponse> upsertCatalogAvailability(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertCatalogAvailabilityRequest request) {
        return multisiteService.upsertCatalogAvailability(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping("/professionals")
    public List<MultisiteProfessionalResponse> professionals(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return multisiteService.professionals(authenticatedUser);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping("/professional-schedules")
    public List<ProfessionalScheduleResponse> schedules(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) UUID locationId) {
        return multisiteService.schedules(authenticatedUser, locationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @PostMapping(value = "/professional-schedules", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ProfessionalScheduleResponse> upsertSchedule(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertProfessionalScheduleRequest request) {
        return multisiteService.upsertSchedule(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping("/user-access")
    public List<UserLocationAccessResponse> userAccess(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return multisiteService.userAccess(authenticatedUser);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @PutMapping(value = "/user-access", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<UserLocationAccessResponse> upsertUserAccess(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertUserLocationAccessRequest request) {
        return multisiteService.upsertUserAccess(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping("/channels")
    public List<MultisiteChannelResponse> channels(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return multisiteService.channels(authenticatedUser);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @PutMapping(value = "/channels/{channelId}/location", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MultisiteChannelResponse> updateChannelLocation(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID channelId,
            @RequestBody UpdateChannelLocationRequest request) {
        return multisiteService.updateChannelLocation(authenticatedUser, channelId, request);
    }
}
