package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.JwtProperties;
import com.asistentewhatsapp.security.api.AuthUserResponse;
import com.asistentewhatsapp.security.api.ForgotPasswordRequest;
import com.asistentewhatsapp.security.api.ForgotPasswordResponse;
import com.asistentewhatsapp.security.api.LoginRequest;
import com.asistentewhatsapp.security.api.LoginResponse;
import com.asistentewhatsapp.security.api.ResetPasswordRequest;
import com.asistentewhatsapp.security.api.ResetPasswordValidationResponse;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.domain.BusinessEntity;
import com.asistentewhatsapp.security.domain.PasswordResetTokenEntity;
import com.asistentewhatsapp.security.domain.SecurityPolicyEntity;
import com.asistentewhatsapp.security.domain.UserAccountEntity;
import com.asistentewhatsapp.security.domain.UserAccountStatus;
import com.asistentewhatsapp.security.domain.UserSessionEntity;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.BusinessRepository;
import com.asistentewhatsapp.security.infrastructure.PasswordResetTokenRepository;
import com.asistentewhatsapp.security.infrastructure.SecurityPolicyRepository;
import com.asistentewhatsapp.security.infrastructure.UserAccountRepository;
import com.asistentewhatsapp.security.infrastructure.UserPermissionJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.UserRoleJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.UserSessionJdbcRepository;
import com.asistentewhatsapp.shared.api.StatusResponse;
import com.asistentewhatsapp.shared.email.TransactionalEmailService;
import com.asistentewhatsapp.shared.email.TransactionalEmailService.DeliveryStatus;
import com.asistentewhatsapp.shared.exception.AuthenticationFailedException;
import com.asistentewhatsapp.shared.exception.ApiException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PASSWORD_RECOVERY_ACCEPTED_MESSAGE =
            "Si el correo existe, enviaremos instrucciones para restablecer la contrasena.";

    private final UserAccountRepository userAccountRepository;
    private final SecurityPolicyRepository securityPolicyRepository;
    private final BusinessRepository businessRepository;
    private final UserRoleJdbcRepository userRoleJdbcRepository;
    private final UserPermissionJdbcRepository userPermissionJdbcRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogJdbcRepository auditLogJdbcRepository;
    private final UserSessionJdbcRepository userSessionJdbcRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordPolicyService passwordPolicyService;
    private final SecurityUserMapper securityUserMapper;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final TransactionalEmailService transactionalEmailService;
    private final String frontendPublicBaseUrl;

    public AuthService(
            UserAccountRepository userAccountRepository,
            SecurityPolicyRepository securityPolicyRepository,
            BusinessRepository businessRepository,
            UserRoleJdbcRepository userRoleJdbcRepository,
            UserPermissionJdbcRepository userPermissionJdbcRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AuditLogJdbcRepository auditLogJdbcRepository,
            UserSessionJdbcRepository userSessionJdbcRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordPolicyService passwordPolicyService,
            SecurityUserMapper securityUserMapper,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService,
            TransactionalEmailService transactionalEmailService,
            @Value("${app.frontend.public-base-url:http://localhost:5173}") String frontendPublicBaseUrl) {
        this.userAccountRepository = userAccountRepository;
        this.securityPolicyRepository = securityPolicyRepository;
        this.businessRepository = businessRepository;
        this.userRoleJdbcRepository = userRoleJdbcRepository;
        this.userPermissionJdbcRepository = userPermissionJdbcRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.auditLogJdbcRepository = auditLogJdbcRepository;
        this.userSessionJdbcRepository = userSessionJdbcRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordPolicyService = passwordPolicyService;
        this.securityUserMapper = securityUserMapper;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.transactionalEmailService = transactionalEmailService;
        this.frontendPublicBaseUrl = frontendPublicBaseUrl;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public LoginResponse login(LoginRequest request, String clientIpAddress) {
        Optional<UserAccountEntity> optionalUser = userAccountRepository.findByEmailIgnoreCase(request.email().trim());

        if (optionalUser.isEmpty()) {
            recordAnonymousFailedLogin(request.email(), clientIpAddress);
            throw new AuthenticationFailedException("Correo o contrasena incorrectos.");
        }

        UserAccountEntity userAccount = optionalUser.get();
        SecurityPolicyEntity securityPolicy = findSecurityPolicy(userAccount.getBusinessId());

        if (userAccount.getStatus() == UserAccountStatus.INACTIVE) {
            throw new AuthenticationFailedException("La cuenta se encuentra inactiva.");
        }

        if (userAccount.getStatus() == UserAccountStatus.LOCKED) {
            throw new AuthenticationFailedException("La cuenta se encuentra bloqueada.");
        }

        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            userAccount.incrementFailedLoginAttempts();
            if (userAccount.getFailedLoginAttempts() >= securityPolicy.getMaxFailedLoginAttempts()) {
                userAccount.setStatus(UserAccountStatus.LOCKED);
            }
            userAccountRepository.save(userAccount);
            auditLogJdbcRepository.insert(
                    userAccount.getBusinessId(),
                    userAccount.getId(),
                    "LOGIN_FAILED",
                    "USER_ACCOUNT",
                    userAccount.getId(),
                    "Intento fallido de inicio de sesion.",
                    Map.of("email", userAccount.getEmail(), "ipAddress", clientIpAddress),
                    OffsetDateTime.now(ZoneOffset.UTC));
            throw new AuthenticationFailedException("Correo o contrasena incorrectos.");
        }

        userAccount.setFailedLoginAttempts(0);
        userAccount.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        userAccountRepository.save(userAccount);

        AuthenticatedUser authenticatedUser = buildAuthenticatedUser(userAccount);
        auditLogJdbcRepository.insert(
                userAccount.getBusinessId(),
                userAccount.getId(),
                "LOGIN_SUCCEEDED",
                "USER_ACCOUNT",
                userAccount.getId(),
                "Inicio de sesion exitoso.",
                Map.of("email", userAccount.getEmail(), "ipAddress", clientIpAddress),
                OffsetDateTime.now(ZoneOffset.UTC));

        RefreshTokenService.RefreshTokenResult refreshResult = refreshTokenService.createSession(
                authenticatedUser, null, clientIpAddress);

        AuthUserResponse authUserResponse = securityUserMapper.toAuthUserResponse(authenticatedUser);
        return new LoginResponse(
                jwtService.createToken(authenticatedUser),
                refreshResult.rawToken(),
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds(),
                jwtProperties.getRefreshTokenExpiresInSeconds(),
                authUserResponse);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(AuthenticatedUser authenticatedUser) {
        UserAccountEntity userAccount = loadScopedUser(authenticatedUser);
        AuthenticatedUser hydratedUser = buildAuthenticatedUser(userAccount);
        return securityUserMapper.toAuthUserResponse(hydratedUser);
    }

    @Transactional
    public StatusResponse logout(AuthenticatedUser authenticatedUser, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeSession(
                authenticatedUser.businessId(), authenticatedUser.userId(), refreshToken);
        } else {
            refreshTokenService.revokeAllSessions(
                authenticatedUser.businessId(), authenticatedUser.userId(), authenticatedUser.userId());
        }
        auditLogJdbcRepository.insert(
                authenticatedUser.businessId(),
                authenticatedUser.userId(),
                "LOGOUT",
                "USER_ACCOUNT",
                authenticatedUser.userId(),
                "Cierre de sesion solicitado.",
                Map.of("email", authenticatedUser.email()),
                OffsetDateTime.now(ZoneOffset.UTC));
        return new StatusResponse("LOGGED_OUT");
    }

    @Transactional
    public LoginResponse refresh(String refreshToken, String deviceInfo, String clientIpAddress) {
        refreshTokenService.validateSession(refreshToken);

        String hash = refreshTokenService.sha256(refreshToken);
        UserSessionEntity session = userSessionJdbcRepository.findByRefreshTokenHash(hash)
            .orElseThrow(() -> new AuthenticationFailedException("Sesion invalida."));

        if (!session.isActive(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new AuthenticationFailedException("Sesion expirada o revocada.");
        }

        UserAccountEntity userAccount = userAccountRepository.findScopedById(
                session.getBusinessId(), session.getUserId())
            .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado."));

        if (userAccount.getStatus() != UserAccountStatus.ACTIVE) {
            throw new AuthenticationFailedException("Cuenta inactiva o bloqueada.");
        }

        AuthenticatedUser authenticatedUser = buildAuthenticatedUser(userAccount);

        var rotated = refreshTokenService.rotate(authenticatedUser, refreshToken, deviceInfo, clientIpAddress);

        userSessionJdbcRepository.updateLastUsed(rotated.session().getId(), OffsetDateTime.now(ZoneOffset.UTC));

        auditLogJdbcRepository.insert(
                userAccount.getBusinessId(), userAccount.getId(),
                "TOKEN_REFRESHED", "USER_ACCOUNT", userAccount.getId(),
                "Token de acceso renovado.",
                Map.of("ipAddress", clientIpAddress),
                OffsetDateTime.now(ZoneOffset.UTC));

        AuthUserResponse authUserResponse = securityUserMapper.toAuthUserResponse(authenticatedUser);
        return new LoginResponse(
                jwtService.createToken(authenticatedUser),
                rotated.rawToken(),
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds(),
                jwtProperties.getRefreshTokenExpiresInSeconds(),
                authUserResponse);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Optional<UserAccountEntity> optionalUser = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        String maskedEmail = maskEmail(normalizedEmail);

        LOGGER.info("PASSWORD_RESET_EMAIL_REQUESTED emailMasked={}", maskedEmail);

        if (optionalUser.isEmpty()) {
            recordAnonymousPasswordRecoveryRequest(maskedEmail, now);
            return acceptedForgotPasswordResponse();
        }

        UserAccountEntity userAccount = optionalUser.get();
        if (userAccount.getStatus() != UserAccountStatus.ACTIVE) {
            auditLogJdbcRepository.insert(
                    userAccount.getBusinessId(),
                    userAccount.getId(),
                    "PASSWORD_RECOVERY_REQUESTED",
                    "USER_ACCOUNT",
                    userAccount.getId(),
                    "Solicitud de recuperacion de contrasena.",
                    Map.of(
                            "emailMasked", maskEmail(userAccount.getEmail()),
                            "requestedAt", now.toString(),
                            "status", "ACCEPTED",
                            "accountStatus", userAccount.getStatus().name(),
                            "delivery", "SKIPPED_NON_ACTIVE_USER"),
                    now);
            return acceptedForgotPasswordResponse();
        }

        SecurityPolicyEntity securityPolicy = findSecurityPolicy(userAccount.getBusinessId());
        passwordResetTokenRepository.findAllByUserIdAndConsumedAtIsNullAndExpiresAtAfter(
                        userAccount.getId(),
                        now)
                .forEach(token -> token.markConsumed(now));

        String rawToken = generateResetToken();
        OffsetDateTime expiresAt = now.plusMinutes(jwtProperties.getResetTokenExpiresInMinutes());
        PasswordResetTokenEntity passwordResetToken = PasswordResetTokenEntity.create(
                UUID.randomUUID(),
                userAccount.getBusinessId(),
                userAccount.getId(),
                hashToken(rawToken),
                expiresAt);
        passwordResetTokenRepository.save(passwordResetToken);

        auditLogJdbcRepository.insert(
                userAccount.getBusinessId(),
                userAccount.getId(),
                "PASSWORD_RECOVERY_REQUESTED",
                "USER_ACCOUNT",
                userAccount.getId(),
                "Solicitud de recuperacion de contrasena.",
                Map.of(
                        "emailMasked", maskEmail(userAccount.getEmail()),
                        "requestedAt", now.toString(),
                        "status", "ACCEPTED",
                        "passwordMinLength", securityPolicy.getPasswordMinLength(),
                        "tokenId", passwordResetToken.getId()),
                now);

        LOGGER.info("PASSWORD_RESET_TOKEN_CREATED userId={} expiresAt={}", userAccount.getId(), expiresAt);

        String resetUrl = buildResetUrl(rawToken);
        try {
            DeliveryStatus deliveryStatus = transactionalEmailService.sendPasswordResetEmail(
                    userAccount.getEmail(),
                    displayName(userAccount),
                    resetUrl,
                    expiresAt.toInstant(),
                    resolveZoneId(userAccount.getTimezone()));
            boolean sent = DeliveryStatus.SENT.equals(deliveryStatus);
            auditLogJdbcRepository.insert(
                    userAccount.getBusinessId(),
                    userAccount.getId(),
                    sent ? "PASSWORD_RECOVERY_EMAIL_SENT" : "PASSWORD_RECOVERY_EMAIL_SIMULATED",
                    "USER_ACCOUNT",
                    userAccount.getId(),
                    sent
                            ? "Correo de recuperacion enviado por SMTP."
                            : "Correo de recuperacion simulado o no enviado por configuracion local.",
                    Map.of(
                            "emailMasked", maskEmail(userAccount.getEmail()),
                            "provider", sent ? "smtp" : "simulation",
                            "expiresAt", expiresAt.toString()),
                    OffsetDateTime.now(ZoneOffset.UTC));
        } catch (Exception exception) {
            LOGGER.warn(
                    "PASSWORD_RESET_EMAIL_FAILED userId={} reason={}",
                    userAccount.getId(),
                    exception.getClass().getSimpleName());
            auditLogJdbcRepository.insert(
                    userAccount.getBusinessId(),
                    userAccount.getId(),
                    "PASSWORD_RECOVERY_EMAIL_FAILED",
                    "USER_ACCOUNT",
                    userAccount.getId(),
                    "No se pudo enviar el correo de recuperacion.",
                    Map.of(
                            "emailMasked", maskEmail(userAccount.getEmail()),
                            "reason", exception.getClass().getSimpleName()),
                    OffsetDateTime.now(ZoneOffset.UTC));
        }

        return acceptedForgotPasswordResponse();
    }

    @Transactional(readOnly = true)
    public ResetPasswordValidationResponse validateResetPasswordToken(String token) {
        Optional<PasswordResetTokenEntity> optionalToken = passwordResetTokenRepository.findByTokenHash(hashToken(token));

        if (optionalToken.isEmpty()) {
            return new ResetPasswordValidationResponse(false, null);
        }

        PasswordResetTokenEntity passwordResetToken = optionalToken.get();
        boolean valid = passwordResetToken.isAvailable(OffsetDateTime.now(ZoneOffset.UTC));
        return new ResetPasswordValidationResponse(valid, passwordResetToken.getExpiresAt());
    }

    @Transactional
    public StatusResponse resetPassword(ResetPasswordRequest request) {
        ensurePasswordConfirmationMatches(request.newPassword(), request.confirmPassword());

        PasswordResetTokenEntity passwordResetToken = passwordResetTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new ResourceNotFoundException("El token de recuperacion no existe."));

        if (!passwordResetToken.isAvailable(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RESET_TOKEN_INVALID",
                    "El token de recuperacion es invalido o ya expiro.");
        }

        UserAccountEntity userAccount = userAccountRepository.findScopedById(
                        passwordResetToken.getBusinessId(),
                        passwordResetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro el usuario asociado al token."));
        SecurityPolicyEntity securityPolicy = findSecurityPolicy(userAccount.getBusinessId());
        passwordPolicyService.validateNewPassword(request.newPassword(), securityPolicy);

        userAccount.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccount.setFailedLoginAttempts(0);
        if (userAccount.getStatus() == UserAccountStatus.LOCKED) {
            userAccount.setStatus(UserAccountStatus.ACTIVE);
        }
        passwordResetToken.markConsumed(OffsetDateTime.now(ZoneOffset.UTC));
        userAccountRepository.save(userAccount);
        passwordResetTokenRepository.save(passwordResetToken);

        auditLogJdbcRepository.insert(
                userAccount.getBusinessId(),
                userAccount.getId(),
                "PASSWORD_RESET_COMPLETED",
                "USER_ACCOUNT",
                userAccount.getId(),
                "Contrasena actualizada desde flujo de recuperacion.",
                Map.of("email", userAccount.getEmail()),
                OffsetDateTime.now(ZoneOffset.UTC));
        return new StatusResponse("PASSWORD_UPDATED");
    }

    private AuthenticatedUser buildAuthenticatedUser(UserAccountEntity userAccount) {
        List<String> roles = userRoleJdbcRepository.findRoleCodesByUserId(userAccount.getId());
        List<String> permissions = userPermissionJdbcRepository.findPermissionCodesByUserId(userAccount.getId());
        String businessName = businessRepository.findById(userAccount.getBusinessId())
                .map(BusinessEntity::getBusinessName)
                .orElse("Centro Estetico Bella");
        return securityUserMapper.toAuthenticatedUser(userAccount, businessName, roles, permissions);
    }

    private UserAccountEntity loadScopedUser(AuthenticatedUser authenticatedUser) {
        return userAccountRepository.findScopedById(authenticatedUser.businessId(), authenticatedUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro el usuario autenticado."));
    }

    private SecurityPolicyEntity findSecurityPolicy(UUID businessId) {
        return securityPolicyRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro la politica de seguridad del negocio."));
    }

    private void recordAnonymousFailedLogin(String email, String clientIpAddress) {
        businessRepository.findByActiveTrueOrderByCreatedAtAsc().stream().findFirst().ifPresent(
                business -> auditLogJdbcRepository.insert(
                        business.getId(),
                        null,
                        "LOGIN_FAILED",
                        "USER_ACCOUNT",
                        null,
                        "Intento fallido de inicio de sesion.",
                        Map.of("email", email, "ipAddress", clientIpAddress),
                        OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private void recordAnonymousPasswordRecoveryRequest(String maskedEmail, OffsetDateTime now) {
        businessRepository.findByActiveTrueOrderByCreatedAtAsc().stream().findFirst().ifPresent(
                business -> auditLogJdbcRepository.insert(
                        business.getId(),
                        null,
                        "PASSWORD_RECOVERY_REQUESTED",
                        "USER_ACCOUNT",
                        null,
                        "Solicitud de recuperacion de contrasena.",
                        Map.of(
                                "emailMasked", maskedEmail,
                                "requestedAt", now.toString(),
                                "status", "ACCEPTED",
                                "userMatched", false),
                        now));
    }

    private ForgotPasswordResponse acceptedForgotPasswordResponse() {
        return new ForgotPasswordResponse("ACCEPTED", PASSWORD_RECOVERY_ACCEPTED_MESSAGE);
    }

    private void ensurePasswordConfirmationMatches(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La solicitud contiene datos invalidos.",
                    Map.of("confirmPassword", "La confirmacion debe coincidir con la nueva contrasena."));
        }
    }

    private String generateResetToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo generar el hash del token.", exception);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        return localPart.substring(0, Math.min(2, localPart.length())) + "***" + domainPart;
    }

    private String buildResetUrl(String rawToken) {
        String baseUrl = frontendPublicBaseUrl == null || frontendPublicBaseUrl.isBlank()
                ? "http://localhost:5173"
                : frontendPublicBaseUrl.trim();
        return UriComponentsBuilder.fromUriString(baseUrl)
                .replacePath(normalizedBasePath(baseUrl) + "/reset-password")
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }

    private String normalizedBasePath(String baseUrl) {
        String path = UriComponentsBuilder.fromUriString(baseUrl).build().getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private String displayName(UserAccountEntity userAccount) {
        String fullName = (userAccount.getFirstName() + " " + userAccount.getLastName()).trim();
        return fullName.isBlank() ? "usuario" : fullName;
    }

    private ZoneId resolveZoneId(String timezone) {
        try {
            return timezone == null || timezone.isBlank() ? ZoneId.of("UTC") : ZoneId.of(timezone);
        } catch (Exception exception) {
            return ZoneId.of("UTC");
        }
    }
}
