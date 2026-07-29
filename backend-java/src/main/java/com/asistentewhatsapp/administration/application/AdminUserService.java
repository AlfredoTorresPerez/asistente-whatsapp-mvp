package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.AdminRoleResponse;
import com.asistentewhatsapp.administration.api.AdminUserRequest;
import com.asistentewhatsapp.administration.api.AdminUserResponse;
import com.asistentewhatsapp.administration.infrastructure.AdministrationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
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
	private final PasswordEncoder passwordEncoder;

	public AdminUserService(AdministrationJdbcRepository administrationJdbcRepository,
			AuditLogJdbcRepository auditLogJdbcRepository, PasswordEncoder passwordEncoder) {
		this.administrationJdbcRepository = administrationJdbcRepository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
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
		String temporaryPassword = normalize(request.temporaryPassword());
		if (temporaryPassword == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
					"La contrasena temporal es obligatoria para crear usuarios.",
					Map.of("temporaryPassword", "Define una contrasena temporal unica para este usuario."));
		}
		if (temporaryPassword.length() < 8 || temporaryPassword.length() > 72) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
					"La contrasena temporal debe tener entre 8 y 72 caracteres.",
					Map.of("temporaryPassword", "Usa entre 8 y 72 caracteres."));
		}

		AdminUserResponse created = administrationJdbcRepository.insertAdminUser(authenticatedUser.businessId(),
				request, role, status, normalizeTimezone(request.timezone(), authenticatedUser.timezone()),
				passwordEncoder.encode(temporaryPassword));
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(), "ADMIN_USER_CREATED",
				"USER_ACCOUNT", created.id(), "Usuario administrativo creado.",
				Map.of("email", created.email(), "role", created.role(), "status", created.status()),
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
