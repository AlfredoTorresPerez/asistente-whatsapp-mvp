package com.asistentewhatsapp.locations.api;

import com.asistentewhatsapp.locations.application.BusinessLocationService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class BusinessLocationController {

    private final BusinessLocationService businessLocationService;

    public BusinessLocationController(BusinessLocationService businessLocationService) {
        this.businessLocationService = businessLocationService;
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping({"/api/business-locations", "/api/v1/business-locations"})
    public List<BusinessLocationResponse> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return businessLocationService.list(authenticatedUser, activeOnly);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping({"/api/business-locations/{locationId}", "/api/v1/business-locations/{locationId}"})
    public BusinessLocationResponse detail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID locationId) {
        return businessLocationService.getDetail(authenticatedUser, locationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_MANAGE')")
    @PostMapping(
            value = {"/api/business-locations", "/api/v1/business-locations"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BusinessLocationResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertBusinessLocationRequest request) {
        return businessLocationService.create(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_MANAGE')")
    @PutMapping(
            value = {"/api/business-locations/{locationId}", "/api/v1/business-locations/{locationId}"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public BusinessLocationResponse update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID locationId,
            @Valid @RequestBody UpsertBusinessLocationRequest request) {
        return businessLocationService.update(authenticatedUser, locationId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_MANAGE')")
    @DeleteMapping({"/api/business-locations/{locationId}", "/api/v1/business-locations/{locationId}"})
    public BusinessLocationResponse deactivate(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID locationId) {
        return businessLocationService.deactivate(authenticatedUser, locationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LOCATIONS_VIEW')")
    @GetMapping({"/api/business-locations/{locationId}/commercial-qr", "/api/v1/business-locations/{locationId}/commercial-qr"})
    public Map<String, String> commercialQr(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID locationId) {
        return businessLocationService.commercialQr(authenticatedUser, locationId);
    }
}
