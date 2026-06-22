package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.JwtProperties;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(AuthenticatedUser authenticatedUser) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpiresInSeconds());

        return Jwts.builder()
                .subject(authenticatedUser.userId().toString())
                .claim("businessId", authenticatedUser.businessId().toString())
                .claim("businessName", authenticatedUser.businessName())
                .claim("firstName", authenticatedUser.firstName())
                .claim("lastName", authenticatedUser.lastName())
                .claim("email", authenticatedUser.email())
                .claim("timezone", authenticatedUser.timezone())
                .claim("roles", authenticatedUser.roles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<String> roles = claims.get("roles", List.class);
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("businessId", String.class)),
                claims.get("businessName", String.class),
                claims.get("firstName", String.class),
                claims.get("lastName", String.class),
                claims.get("email", String.class),
                claims.get("timezone", String.class),
                roles == null ? List.of() : roles);
    }

    public long getAccessTokenExpiresInSeconds() {
        return jwtProperties.getAccessTokenExpiresInSeconds();
    }
}
