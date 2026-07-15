package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.JwtProperties;
import com.asistentewhatsapp.security.api.ForgotPasswordRequest;
import com.asistentewhatsapp.security.domain.PasswordResetTokenEntity;
import com.asistentewhatsapp.security.domain.SecurityPolicyEntity;
import com.asistentewhatsapp.security.domain.UserAccountEntity;
import com.asistentewhatsapp.security.domain.UserAccountStatus;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.BusinessRepository;
import com.asistentewhatsapp.security.infrastructure.PasswordResetTokenRepository;
import com.asistentewhatsapp.security.infrastructure.SecurityPolicyRepository;
import com.asistentewhatsapp.security.infrastructure.UserAccountRepository;
import com.asistentewhatsapp.security.infrastructure.UserPermissionJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.UserRoleJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.UserSessionJdbcRepository;
import com.asistentewhatsapp.security.application.RefreshTokenService;
import com.asistentewhatsapp.shared.email.TransactionalEmailService;
import com.asistentewhatsapp.shared.email.TransactionalEmailService.DeliveryStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordRecoveryTest {

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final SecurityPolicyRepository securityPolicyRepository = mock(SecurityPolicyRepository.class);
    private final BusinessRepository businessRepository = mock(BusinessRepository.class);
    private final UserRoleJdbcRepository userRoleJdbcRepository = mock(UserRoleJdbcRepository.class);
    private final UserPermissionJdbcRepository userPermissionJdbcRepository = mock(UserPermissionJdbcRepository.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final AuditLogJdbcRepository auditLogJdbcRepository = mock(AuditLogJdbcRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
    private final SecurityUserMapper securityUserMapper = new SecurityUserMapper();
    private final JwtProperties jwtProperties = new JwtProperties();
    private final TransactionalEmailService transactionalEmailService = mock(TransactionalEmailService.class);
    private final UserSessionJdbcRepository userSessionJdbcRepository = mock(UserSessionJdbcRepository.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

    @Test
    void forgotPasswordStoresHashAndDoesNotAuditRawToken() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAccountEntity user = userAccount(businessId, userId);
        SecurityPolicyEntity policy = securityPolicy(businessId);
        jwtProperties.setResetTokenExpiresInMinutes(30);
        AuthService authService = new AuthService(
                userAccountRepository,
                securityPolicyRepository,
                businessRepository,
                userRoleJdbcRepository,
                userPermissionJdbcRepository,
                passwordResetTokenRepository,
                auditLogJdbcRepository,
                userSessionJdbcRepository,
                passwordEncoder,
                jwtService,
                passwordPolicyService,
                securityUserMapper,
                jwtProperties,
                refreshTokenService,
                transactionalEmailService,
                "http://localhost:5173");

        when(userAccountRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(securityPolicyRepository.findByBusinessId(businessId)).thenReturn(Optional.of(policy));
        when(passwordResetTokenRepository.findAllByUserIdAndConsumedAtIsNullAndExpiresAtAfter(eq(userId), any()))
                .thenReturn(List.of());
        when(transactionalEmailService.sendPasswordResetEmail(eq("admin@example.com"), eq("Admin Demo"), any(), any(), any()))
                .thenReturn(DeliveryStatus.SIMULATED);

        authService.forgotPassword(new ForgotPasswordRequest("admin@example.com"));

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogJdbcRepository).insert(
                eq(businessId),
                eq(userId),
                eq("PASSWORD_RECOVERY_REQUESTED"),
                eq("USER_ACCOUNT"),
                eq(userId),
                any(),
                metadataCaptor.capture(),
                any(OffsetDateTime.class));

        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata).doesNotContainKey("resetTokenPreview");
        assertThat(metadata.values()).allSatisfy(value -> assertThat(String.valueOf(value).length()).isNotEqualTo(48));
        assertThat(metadata).containsEntry("status", "ACCEPTED");
        verify(transactionalEmailService).sendPasswordResetEmail(
                eq("admin@example.com"),
                eq("Admin Demo"),
                org.mockito.ArgumentMatchers.contains("/reset-password?token="),
                any(),
                any());
    }

    private UserAccountEntity userAccount(UUID businessId, UUID userId) {
        UserAccountEntity user = new UserAccountEntity();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "businessId", businessId);
        ReflectionTestUtils.setField(user, "email", "admin@example.com");
        ReflectionTestUtils.setField(user, "firstName", "Admin");
        ReflectionTestUtils.setField(user, "lastName", "Demo");
        user.setPasswordHash("hash");
        user.setStatus(UserAccountStatus.ACTIVE);
        user.setTimezone("America/Santiago");
        return user;
    }

    private SecurityPolicyEntity securityPolicy(UUID businessId) {
        SecurityPolicyEntity policy = new SecurityPolicyEntity();
        ReflectionTestUtils.setField(policy, "businessId", businessId);
        ReflectionTestUtils.setField(policy, "passwordMinLength", 12);
        ReflectionTestUtils.setField(policy, "maxFailedLoginAttempts", 5);
        return policy;
    }
}
