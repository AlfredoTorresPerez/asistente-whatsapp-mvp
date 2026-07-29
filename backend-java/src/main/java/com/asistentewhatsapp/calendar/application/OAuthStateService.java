package com.asistentewhatsapp.calendar.application;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthStateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OAuthStateService.class);
	private static final int STATE_BYTES = 32;
	private static final int EXPIRY_MINUTES = 10;

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final SecureRandom secureRandom;

	public OAuthStateService(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.secureRandom = new SecureRandom();
	}

	@Transactional
	public String generateState(UUID businessId, String provider) {
		byte[] rawBytes = new byte[STATE_BYTES];
		secureRandom.nextBytes(rawBytes);
		String rawState = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
		String stateHash = sha256Hex(rawState);

		OffsetDateTime now = OffsetDateTime.now();
		jdbcTemplate.update(
				"""
						insert into oauth_state (id, business_id, provider, state_hash, redirect_uri, created_at, expires_at, consumed)
						values (:id, :businessId, :provider, :stateHash, :redirectUri, :createdAt, :expiresAt, false)
						""",
				new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("businessId", businessId)
						.addValue("provider", provider).addValue("stateHash", stateHash).addValue("redirectUri", null)
						.addValue("createdAt", now).addValue("expiresAt", now.plusMinutes(EXPIRY_MINUTES)));

		LOGGER.debug("OAUTH_STATE_GENERATED businessId={} provider={}", businessId, provider);
		return rawState;
	}

	@Transactional
	public OAuthStateInfo consumeAndValidate(String state, UUID expectedBusinessId, String expectedProvider) {
		if (state == null || state.isBlank()) {
			throw new IllegalArgumentException("OAuth state parameter is missing");
		}
		String stateHash = sha256Hex(state);

		var rows = jdbcTemplate.query("""
				select business_id, provider, redirect_uri, consumed, expires_at
				from oauth_state where state_hash = :stateHash
				""", new MapSqlParameterSource("stateHash", stateHash),
				(rs, rowNum) -> new OAuthStateRow(rs.getObject("business_id", UUID.class), rs.getString("provider"),
						rs.getString("redirect_uri"), rs.getBoolean("consumed"),
						rs.getObject("expires_at", OffsetDateTime.class)));

		if (rows.isEmpty()) {
			throw new IllegalArgumentException("OAuth state not found or already used");
		}

		OAuthStateRow row = rows.getFirst();

		if (row.consumed()) {
			throw new IllegalArgumentException("OAuth state has already been consumed");
		}

		if (row.expiresAt().isBefore(OffsetDateTime.now())) {
			throw new IllegalArgumentException("OAuth state has expired");
		}

		if (expectedBusinessId != null && !row.businessId().equals(expectedBusinessId)) {
			throw new IllegalArgumentException("OAuth state business_id mismatch");
		}

		if (expectedProvider != null && !row.provider().equals(expectedProvider)) {
			throw new IllegalArgumentException("OAuth state provider mismatch");
		}

		int updated = jdbcTemplate.update(
				"update oauth_state set consumed = true where state_hash = :stateHash and consumed = false",
				new MapSqlParameterSource("stateHash", stateHash));

		if (updated == 0) {
			throw new IllegalArgumentException("OAuth state could not be marked as consumed (concurrent request)");
		}

		LOGGER.info("OAUTH_STATE_CONSUMED businessId={} provider={}", row.businessId(), row.provider());
		return new OAuthStateInfo(row.businessId(), row.provider(), row.redirectUri());
	}

	@Transactional
	public void cleanupExpired() {
		int deleted = jdbcTemplate.update("delete from oauth_state where expires_at < :now",
				new MapSqlParameterSource("now", OffsetDateTime.now()));
		if (deleted > 0) {
			LOGGER.debug("OAUTH_STATE_CLEANUP deleted={}", deleted);
		}
	}

	private String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new RuntimeException("Failed to compute SHA-256 hash", e);
		}
	}

	private record OAuthStateRow(UUID businessId, String provider, String redirectUri, boolean consumed,
			OffsetDateTime expiresAt) {
	}

	public record OAuthStateInfo(UUID businessId, String provider, String redirectUri) {
	}
}
