package com.asistentewhatsapp.customerbookings.infrastructure;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerBookingTokenJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public CustomerBookingTokenJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void insert(UUID id, UUID businessId, String tokenHash, String phoneDigits, OffsetDateTime expiresAt) {
		jdbcTemplate.update("""
				insert into customer_bookings_token (id, business_id, token_hash, phone_digits, expires_at)
				values (:id, :businessId, :tokenHash, :phoneDigits, :expiresAt)
				""",
				new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
						.addValue("tokenHash", tokenHash).addValue("phoneDigits", phoneDigits)
						.addValue("expiresAt", expiresAt));
	}

	public Optional<TokenRecord> findValidByTokenHash(String tokenHash) {
		return jdbcTemplate.query("""
				select business_id, phone_digits from customer_bookings_token
				where token_hash = :tokenHash
				  and expires_at > :now
				limit 1
				""", new MapSqlParameterSource().addValue("tokenHash", tokenHash).addValue("now", OffsetDateTime.now()),
				(rs, rowNum) -> new TokenRecord(UUID.fromString(rs.getString("business_id")),
						rs.getString("phone_digits")))
				.stream().findFirst();
	}

	public record TokenRecord(UUID businessId, String phoneDigits) {
	}
}
