package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.ProfessionalService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfessionalController {

	private final ProfessionalService professionalService;

	public ProfessionalController(ProfessionalService professionalService) {
		this.professionalService = professionalService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'PROFESSIONAL_VIEW')")
	@GetMapping(value = "/api/v1/admin/professionals", produces = MediaType.APPLICATION_JSON_VALUE)
	public PagedResponse<ProfessionalResponse> listProfessionals(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active) {
		return professionalService.listProfessionals(authenticatedUser, page, size, search, active);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'PROFESSIONAL_VIEW')")
	@GetMapping(value = "/api/v1/admin/professionals/{professionalId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ProfessionalResponse getProfessional(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID professionalId) {
		return professionalService.getProfessional(authenticatedUser, professionalId);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'PROFESSIONAL_MANAGE')")
	@PostMapping(value = "/api/v1/admin/professionals", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ProfessionalResponse createProfessional(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody ProfessionalRequest request) {
		return professionalService.createProfessional(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'PROFESSIONAL_MANAGE')")
	@PatchMapping(value = "/api/v1/admin/professionals/{professionalId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ProfessionalResponse updateProfessional(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID professionalId, @Valid @RequestBody ProfessionalRequest request) {
		return professionalService.updateProfessional(authenticatedUser, professionalId, request);
	}
}
