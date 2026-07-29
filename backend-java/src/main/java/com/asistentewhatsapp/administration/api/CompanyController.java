package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.CompanyService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class CompanyController {

	private final CompanyService companyService;

	public CompanyController(CompanyService companyService) {
		this.companyService = companyService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ADMIN_MANAGE')")
	@GetMapping({"/api/v1/company", "/api/businesses/current"})
	public CompanySettingsResponse current(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return companyService.getCurrent(authenticatedUser);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'USERS_MANAGE')")
	@PatchMapping({"/api/v1/company", "/api/businesses/current"})
	public CompanySettingsResponse update(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody CompanySettingsRequest request) {
		return companyService.updateCurrent(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'USERS_MANAGE')")
	@PutMapping("/api/businesses/current")
	public CompanySettingsResponse replace(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody CompanySettingsRequest request) {
		return companyService.updateCurrent(authenticatedUser, request);
	}
}
