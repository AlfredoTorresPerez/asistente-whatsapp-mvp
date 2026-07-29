package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.api.ChangePasswordRequest;
import com.asistentewhatsapp.security.api.UpdateProfileRequest;
import com.asistentewhatsapp.security.api.UserProfileResponse;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.domain.BusinessEntity;
import com.asistentewhatsapp.security.domain.SecurityPolicyEntity;
import com.asistentewhatsapp.security.domain.UserAccountEntity;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.BusinessRepository;
import com.asistentewhatsapp.security.infrastructure.SecurityPolicyRepository;
import com.asistentewhatsapp.security.infrastructure.UserAccountRepository;
import com.asistentewhatsapp.security.infrastructure.UserRoleJdbcRepository;
import com.asistentewhatsapp.shared.api.StatusResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.exception.AuthenticationFailedException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

	private final UserAccountRepository userAccountRepository;
	private final SecurityPolicyRepository securityPolicyRepository;
	private final BusinessRepository businessRepository;
	private final UserRoleJdbcRepository userRoleJdbcRepository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicyService passwordPolicyService;
	private final SecurityUserMapper securityUserMapper;

	public UserProfileService(UserAccountRepository userAccountRepository,
			SecurityPolicyRepository securityPolicyRepository, BusinessRepository businessRepository,
			UserRoleJdbcRepository userRoleJdbcRepository, AuditLogJdbcRepository auditLogJdbcRepository,
			PasswordEncoder passwordEncoder, PasswordPolicyService passwordPolicyService,
			SecurityUserMapper securityUserMapper) {
		this.userAccountRepository = userAccountRepository;
		this.securityPolicyRepository = securityPolicyRepository;
		this.businessRepository = businessRepository;
		this.userRoleJdbcRepository = userRoleJdbcRepository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicyService = passwordPolicyService;
		this.securityUserMapper = securityUserMapper;
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getCurrentProfile(AuthenticatedUser authenticatedUser) {
		UserAccountEntity userAccount = loadCurrentUser(authenticatedUser);
		return toUserProfileResponse(userAccount);
	}

	@Transactional
	public UserProfileResponse updateCurrentProfile(AuthenticatedUser authenticatedUser, UpdateProfileRequest request) {
		UserAccountEntity userAccount = loadCurrentUser(authenticatedUser);
		userAccount.setFirstName(request.firstName().trim());
		userAccount.setLastName(request.lastName().trim());
		userAccount.setPhone(blankToNull(request.phone()));
		userAccount.setTimezone(request.timezone().trim());
		userAccountRepository.save(userAccount);
		return toUserProfileResponse(userAccount);
	}

	@Transactional
	public StatusResponse changePassword(AuthenticatedUser authenticatedUser, ChangePasswordRequest request) {
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos invalidos.",
					Map.of("confirmPassword", "La confirmacion debe coincidir con la nueva contrasena."));
		}

		UserAccountEntity userAccount = loadCurrentUser(authenticatedUser);

		if (!passwordEncoder.matches(request.currentPassword(), userAccount.getPasswordHash())) {
			throw new AuthenticationFailedException("La contrasena actual es incorrecta.",
					Map.of("currentPassword", "La contrasena actual es incorrecta."));
		}

		SecurityPolicyEntity securityPolicy = securityPolicyRepository.findByBusinessId(authenticatedUser.businessId())
				.orElseThrow(
						() -> new ResourceNotFoundException("No se encontro la politica de seguridad del negocio."));
		passwordPolicyService.validateNewPassword(request.newPassword(), securityPolicy);

		userAccount.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userAccountRepository.save(userAccount);

		auditLogJdbcRepository.insert(authenticatedUser.businessId(), authenticatedUser.userId(), "PASSWORD_CHANGED",
				"USER_ACCOUNT", authenticatedUser.userId(), "Cambio de contrasena realizado por el usuario.",
				Map.of("email", authenticatedUser.email()), OffsetDateTime.now(ZoneOffset.UTC));

		return new StatusResponse("PASSWORD_CHANGED");
	}

	private UserAccountEntity loadCurrentUser(AuthenticatedUser authenticatedUser) {
		return userAccountRepository.findScopedById(authenticatedUser.businessId(), authenticatedUser.userId())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontro el usuario autenticado."));
	}

	private UserProfileResponse toUserProfileResponse(UserAccountEntity userAccount) {
		String businessName = businessRepository.findById(userAccount.getBusinessId())
				.map(BusinessEntity::getBusinessName).orElse("Centro Estetico Bella");
		List<String> roles = userRoleJdbcRepository.findRoleCodesByUserId(userAccount.getId());
		return securityUserMapper.toUserProfileResponse(userAccount, businessName, roles);
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
