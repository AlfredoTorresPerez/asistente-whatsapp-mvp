package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.SecurityPolicyRequest;
import com.asistentewhatsapp.administration.api.SecurityPolicyResponse;
import com.asistentewhatsapp.administration.infrastructure.AdministrationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSecurityService {

	private final AdministrationJdbcRepository administrationJdbcRepository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;

	public AdminSecurityService(AdministrationJdbcRepository administrationJdbcRepository,
			AuditLogJdbcRepository auditLogJdbcRepository) {
		this.administrationJdbcRepository = administrationJdbcRepository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
	}

	@Transactional(readOnly = true)
	public SecurityPolicyResponse getPolicy(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		return administrationJdbcRepository.findSecurityPolicy(authenticatedUser.businessId());
	}

	@Transactional
	public SecurityPolicyResponse updatePolicy(AuthenticatedUser authenticatedUser, SecurityPolicyRequest request) {
		AdminAccessGuard.requireOwnerOrAdmin(authenticatedUser);
		SecurityPolicyResponse updated = administrationJdbcRepository
				.updateSecurityPolicy(authenticatedUser.businessId(), request);
		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(),
				"SECURITY_POLICY_UPDATED", "SECURITY_POLICY", updated.id(),
				"Politicas de seguridad actualizadas desde administracion.",
				Map.of("sessionTimeoutMinutes", request.sessionTimeoutMinutes(), "passwordMinLength",
						request.passwordMinLength(), "maxFailedLoginAttempts", request.maxFailedLoginAttempts()),
				OffsetDateTime.now(ZoneOffset.UTC));
		return updated;
	}
}
