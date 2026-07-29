package com.asistentewhatsapp.security.api;

import com.asistentewhatsapp.security.application.AuditLogService;
import com.asistentewhatsapp.security.application.UserProfileService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.api.StatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class SecurityController {

	private final UserProfileService userProfileService;
	private final AuditLogService auditLogService;

	public SecurityController(UserProfileService userProfileService, AuditLogService auditLogService) {
		this.userProfileService = userProfileService;
		this.auditLogService = auditLogService;
	}

	@PostMapping({"/api/v1/users/me/change-password", "/api/security/change-password"})
	public StatusResponse changePassword(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody ChangePasswordRequest request) {
		return userProfileService.changePassword(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'SECURITY_AUDIT_VIEW')")
	@GetMapping({"/api/v1/security/audit-log", "/api/security/audit-log"})
	public PagedResponse<AuditLogResponse> auditLog(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return auditLogService.list(authenticatedUser, page, size);
	}
}
