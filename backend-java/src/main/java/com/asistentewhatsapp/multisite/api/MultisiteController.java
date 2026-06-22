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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/multisite", produces = MediaType.APPLICATION_JSON_VALUE)
public class MultisiteController {

    private final MultisiteService multisiteService;

    public MultisiteController(MultisiteService multisiteService) {
        this.multisiteService = multisiteService;
    }

    @GetMapping("/summary")
    public List<MultisiteLocationSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return multisiteService.locationSummary(user);
    }

    @GetMapping("/catalog-availability")
    public List<MultisiteCatalogAvailabilityResponse> catalogAvailability(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID locationId) {
        return multisiteService.catalogAvailability(user, locationId);
    }

    @PutMapping(value = "/catalog-availability", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MultisiteCatalogAvailabilityResponse> upsertCatalogAvailability(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpsertCatalogAvailabilityRequest request) {
        return multisiteService.upsertCatalogAvailability(user, request);
    }

    @GetMapping("/professionals")
    public List<MultisiteProfessionalResponse> professionals(@AuthenticationPrincipal AuthenticatedUser user) {
        return multisiteService.professionals(user);
    }

    @GetMapping("/professional-schedules")
    public List<ProfessionalScheduleResponse> schedules(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID locationId) {
        return multisiteService.schedules(user, locationId);
    }

    @PostMapping(value = "/professional-schedules", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ProfessionalScheduleResponse> upsertSchedule(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpsertProfessionalScheduleRequest request) {
        return multisiteService.upsertSchedule(user, request);
    }

    @GetMapping("/user-access")
    public List<UserLocationAccessResponse> userAccess(@AuthenticationPrincipal AuthenticatedUser user) {
        return multisiteService.userAccess(user);
    }

    @PutMapping(value = "/user-access", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<UserLocationAccessResponse> upsertUserAccess(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpsertUserLocationAccessRequest request) {
        return multisiteService.upsertUserAccess(user, request);
    }

    @GetMapping("/channels")
    public List<MultisiteChannelResponse> channels(@AuthenticationPrincipal AuthenticatedUser user) {
        return multisiteService.channels(user);
    }

    @PutMapping(value = "/channels/{channelId}/location", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MultisiteChannelResponse> updateChannelLocation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID channelId,
            @RequestBody UpdateChannelLocationRequest request) {
        return multisiteService.updateChannelLocation(user, channelId, request);
    }
}
