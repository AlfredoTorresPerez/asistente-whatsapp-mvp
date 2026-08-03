package com.asistentewhatsapp.leads.api;

import com.asistentewhatsapp.leads.application.LeadService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class LeadController {

	private final LeadService leadService;

	public LeadController(LeadService leadService) {
		this.leadService = leadService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LEAD_MANAGE')")
	@GetMapping({"/api/v1/leads", "/api/v1/prospects"})
	public PagedResponse<LeadSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String search, @RequestParam(required = false) String stage,
			@RequestParam(required = false) String status, @RequestParam(required = false) String origin,
			@RequestParam(required = false) String sourceType, @RequestParam(required = false) UUID ownerUserId,
			@RequestParam(required = false) UUID assignedUserId, @RequestParam(required = false) UUID responsibleUserId,
			@RequestParam(required = false) UUID locationId) {
		UUID resolvedResponsibleUserId = assignedUserId != null
				? assignedUserId
				: responsibleUserId != null ? responsibleUserId : ownerUserId;
		String resolvedStage = stage != null ? stage : status;
		String resolvedOrigin = origin != null ? origin : sourceType;

		return leadService.list(authenticatedUser, page, size, search, resolvedStage, resolvedOrigin,
				resolvedResponsibleUserId, locationId);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LEAD_MANAGE')")
	@PostMapping(value = {"/api/v1/leads", "/api/v1/prospects"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public LeadDetailResponse create(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody CreateLeadRequest request) {
		return leadService.create(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LEAD_MANAGE')")
	@GetMapping({"/api/v1/leads/{leadId}", "/api/v1/prospects/{leadId}"})
	public LeadDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID leadId) {
		return leadService.getDetail(authenticatedUser, leadId);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LEAD_MANAGE')")
	@PutMapping(value = {"/api/v1/leads/{leadId}",
			"/api/v1/prospects/{leadId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public LeadDetailResponse update(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID leadId, @Valid @RequestBody UpdateLeadRequest request) {
		return leadService.update(authenticatedUser, leadId, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LEAD_MANAGE')")
	@PostMapping(value = {"/api/v1/leads/{leadId}/notes",
			"/api/v1/prospects/{leadId}/notes"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public LeadNoteResponse addNote(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID leadId, @Valid @RequestBody AddLeadNoteRequest request) {
		return leadService.addNote(authenticatedUser, leadId, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LEAD_MANAGE')")
	@PatchMapping(value = {"/api/v1/leads/{leadId}/stage",
			"/api/v1/prospects/{leadId}/stage"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public LeadDetailResponse updateStage(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID leadId, @Valid @RequestBody UpdateLeadStageRequest request) {
		return leadService.updateStage(authenticatedUser, leadId, request);
	}
}
