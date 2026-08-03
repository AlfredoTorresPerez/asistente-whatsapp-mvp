package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.AdminRoleResponse;
import com.asistentewhatsapp.administration.api.AdminUserRequest;
import com.asistentewhatsapp.administration.api.AdminUserResponse;
import com.asistentewhatsapp.administration.infrastructure.AdministrationJdbcRepository;
import com.asistentewhatsapp.security.api.ForgotPasswordRequest;
import com.asistentewhatsapp.security.application.AuthService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.UserSessionJdbcRepository;
import com.asistentewhatsapp.shared.api.StatusResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

	private final AdministrationJdbcRepository administrationJdbcRepository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;
	private final AuthService authService;
	private final UserSessionJdbcRepository userSessionJdbcRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminUserService(AdministrationJdbcRepository administrationJdbcRepository,
			AuditLogJdbcRepository auditLogJdbcRepository, AuthService authService,
			UserSessionJdbcRepository userSessionJdbcRepository, PasswordEncoder passwordEncoder) {
		this.administrationJdbcRepository = administrationJdbcRepository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
		this.authService = authService;
		this.userSessionJdbcRepository = userSessionJdbcRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public PagedResponse<AdminUserResponse> listUsers(AuthenticatedUser authenticatedUser, int page, int size,
			String search, String role, String status) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		return administrationJdbcRepository.findAdminUsers(authenticatedUser.businessId(), Math.max(page, 0),
				Math.min(Math.max(size, 1), 100), normalize(search), normalize(role), normalize(status));
	}

	@Transactional(readOnly = true)
	public AdminUserResponse getUser(AuthenticatedUser authenticatedUser, UUID userId) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		return administrationJdbcRepository.findAdminUser(authenticatedUser.businessId(), userId);
	}

	@Transactional(readOnly = true)
	public List<AdminRoleResponse> listRoles(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		return administrationJdbcRepository.findAdminRoles();
	}

	@Transactional
	public AdminUserResponse createUser(AuthenticatedUser authenticatedUser, AdminUserRequest request) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		String role = normalizeRole(request.role());
		String status = normalizeStatus(request.status());
		String initialPassword = generateInternalInitialPassword();

		AdminUserResponse created = administrationJdbcRepository.insertAdminUser(authenticatedUser.businessId(),
				request, role, status, normalizeTimezone(request.timezone(), authenticatedUser.timezone()),
				passwordEncoder.encode(initialPassword));
		if ("ACTIVE".equals(created.status())) {
			authService.forgotPassword(new ForgotPasswordRequest(created.email()));
		}
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(), "ADMIN_USER_CREATED",
				"USER_ACCOUNT", created.id(), "Usuario administrativo creado.", Map.of("email", created.email(), "role",
						created.role(), "status", created.status(), "accessInvitation", "REQUESTED"),
				OffsetDateTime.now(ZoneOffset.UTC));
		return created;
	}

	@Transactional
	public AdminUserResponse updateUser(AuthenticatedUser authenticatedUser, UUID userId, AdminUserRequest request) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		String role = normalizeRole(request.role());
		String status = normalizeStatus(request.status());
		AdminUserResponse updated = administrationJdbcRepository.updateAdminUser(authenticatedUser.businessId(), userId,
				request, role, status, normalizeTimezone(request.timezone(), authenticatedUser.timezone()));
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(), "ADMIN_USER_UPDATED",
				"USER_ACCOUNT", updated.id(), "Usuario administrativo actualizado.",
				Map.of("email", updated.email(), "role", updated.role(), "status", updated.status()),
				OffsetDateTime.now(ZoneOffset.UTC));
		return updated;
	}

	@Transactional
	public StatusResponse deactivateUser(AuthenticatedUser authenticatedUser, UUID userId) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		if (authenticatedUser.userId().equals(userId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
					"No puedes desactivar tu propio usuario desde esta pantalla.");
		}
		AdminUserResponse current = administrationJdbcRepository.findAdminUser(authenticatedUser.businessId(), userId);
		administrationJdbcRepository.updateAdminUser(authenticatedUser.businessId(), userId,
				new AdminUserRequest(current.firstName(), current.lastName(), current.email(), current.phone(),
						current.role(), "INACTIVE", current.timezone(), null),
				current.role(), "INACTIVE", current.timezone());
		userSessionJdbcRepository.revokeAllByUser(authenticatedUser.businessId(), userId,
				OffsetDateTime.now(ZoneOffset.UTC), authenticatedUser.userId());
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(),
				"ADMIN_USER_DEACTIVATED", "USER_ACCOUNT", userId, "Usuario administrativo desactivado.",
				Map.of("email", current.email()), OffsetDateTime.now(ZoneOffset.UTC));
		return new StatusResponse("USER_DEACTIVATED");
	}

	@Transactional
	public StatusResponse revokeSessions(AuthenticatedUser authenticatedUser, UUID userId) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		AdminUserResponse current = administrationJdbcRepository.findAdminUser(authenticatedUser.businessId(), userId);
		userSessionJdbcRepository.revokeAllByUser(authenticatedUser.businessId(), userId,
				OffsetDateTime.now(ZoneOffset.UTC), authenticatedUser.userId());
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(),
				"ADMIN_USER_SESSIONS_REVOKED", "USER_ACCOUNT", userId, "Sesiones de usuario revocadas.",
				Map.of("email", current.email()), OffsetDateTime.now(ZoneOffset.UTC));
		return new StatusResponse("USER_SESSIONS_REVOKED");
	}

	@Transactional
	public StatusResponse resetAccess(AuthenticatedUser authenticatedUser, UUID userId) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		AdminUserResponse current = administrationJdbcRepository.findAdminUser(authenticatedUser.businessId(), userId);
		authService.forgotPassword(new ForgotPasswordRequest(current.email()));
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(),
				"ADMIN_USER_ACCESS_RESET_REQUESTED", "USER_ACCOUNT", userId,
				"Restablecimiento de acceso solicitado por administracion.", Map.of("email", current.email()),
				OffsetDateTime.now(ZoneOffset.UTC));
		return new StatusResponse("USER_ACCESS_RESET_REQUESTED");
	}

	private String generateInternalInitialPassword() {
		return UUID.randomUUID() + "Aa1!";
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String normalizeRole(String role) {
		String normalizedRole = normalize(role);
		if (normalizedRole == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El rol es obligatorio.");
		}
		return normalizedRole.toUpperCase();
	}

	private String normalizeStatus(String status) {
		String normalizedStatus = normalize(status);
		if (normalizedStatus == null) {
			return "ACTIVE";
		}
		return normalizedStatus.toUpperCase();
	}

	private String normalizeTimezone(String timezone, String fallbackTimezone) {
		String normalizedTimezone = normalize(timezone);
		return normalizedTimezone == null ? fallbackTimezone : normalizedTimezone;
	}
}
