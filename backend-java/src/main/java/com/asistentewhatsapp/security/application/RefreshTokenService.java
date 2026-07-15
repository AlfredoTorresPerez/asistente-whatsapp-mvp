package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.JwtProperties;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.domain.UserSessionEntity;
import com.asistentewhatsapp.security.infrastructure.UserSessionJdbcRepository;
import com.asistentewhatsapp.shared.exception.AuthenticationFailedException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserSessionJdbcRepository userSessionJdbcRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(UserSessionJdbcRepository userSessionJdbcRepository, JwtProperties jwtProperties) {
        this.userSessionJdbcRepository = userSessionJdbcRepository;
        this.jwtProperties = jwtProperties;
    }

    public record RefreshTokenResult(String rawToken, UserSessionEntity session) {}

    @Transactional
    public RefreshTokenResult createSession(AuthenticatedUser user, String deviceInfo, String ipAddress) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String rawToken = generateRawToken();
        String tokenHash = sha256(rawToken);

        UserSessionEntity session = new UserSessionEntity(
            UUID.randomUUID(),
            user.businessId(),
            user.userId(),
            tokenHash,
            deviceInfo,
            ipAddress,
            now, now,
            now.plusSeconds(jwtProperties.getRefreshTokenExpiresInSeconds()));

        userSessionJdbcRepository.insert(session);
        return new RefreshTokenResult(rawToken, session);
    }

    @Transactional
    public RefreshTokenResult rotate(AuthenticatedUser user, String currentRawToken, String deviceInfo, String ipAddress) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String currentHash = sha256(currentRawToken);
        UserSessionEntity session = userSessionJdbcRepository.findByRefreshTokenHash(currentHash)
            .orElseThrow(() -> new AuthenticationFailedException("Sesion invalida."));

        if (!session.isActive(now)) {
            throw new AuthenticationFailedException("Sesion expirada o revocada.");
        }

        if (!session.getUserId().equals(user.userId()) || !session.getBusinessId().equals(user.businessId())) {
            throw new AuthenticationFailedException("Sesion invalida.");
        }

        session.revoke(now, user.userId());
        userSessionJdbcRepository.revoke(session.getId(), now, user.userId());

        String newRawToken = generateRawToken();
        String newHash = sha256(newRawToken);

        UserSessionEntity newSession = new UserSessionEntity(
            UUID.randomUUID(),
            user.businessId(),
            user.userId(),
            newHash,
            deviceInfo != null ? deviceInfo : session.getDeviceInfo(),
            ipAddress != null ? ipAddress : session.getIpAddress(),
            now, now,
            now.plusSeconds(jwtProperties.getRefreshTokenExpiresInSeconds()));

        userSessionJdbcRepository.insert(newSession);
        return new RefreshTokenResult(newRawToken, newSession);
    }

    @Transactional
    public void revokeSession(UUID businessId, UUID userId, String rawToken) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String hash = sha256(rawToken);
        userSessionJdbcRepository.findByRefreshTokenHash(hash).ifPresent(session -> {
            if (session.getBusinessId().equals(businessId) && session.getUserId().equals(userId)) {
                userSessionJdbcRepository.revoke(session.getId(), now, userId);
            }
        });
    }

    @Transactional
    public void revokeAllSessions(UUID businessId, UUID userId, UUID revokedBy) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        userSessionJdbcRepository.revokeAllByUser(businessId, userId, now, revokedBy);
    }

    @Transactional(readOnly = true)
    public void validateSession(String rawToken) {
        String hash = sha256(rawToken);
        UserSessionEntity session = userSessionJdbcRepository.findByRefreshTokenHash(hash)
            .orElseThrow(() -> new AuthenticationFailedException("Sesion invalida."));
        if (!session.isActive(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new AuthenticationFailedException("Sesion expirada o revocada.");
        }
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar hash", e);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
