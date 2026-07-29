package com.asistentewhatsapp.security.infrastructure;

import com.asistentewhatsapp.security.domain.UserSessionEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserSessionJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public UserSessionJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private final RowMapper<UserSessionEntity> rowMapper = (rs, rowNum) -> {
		UserSessionEntity e = new UserSessionEntity();
		e.setId(rs.getObject("id", UUID.class));
		e.setBusinessId(rs.getObject("business_id", UUID.class));
		e.setUserId(rs.getObject("user_id", UUID.class));
		e.setRefreshTokenHash(rs.getString("refresh_token_hash"));
		e.setDeviceInfo(rs.getString("device_info"));
		e.setIpAddress(rs.getString("ip_address"));
		e.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
		e.setLastUsedAt(rs.getObject("last_used_at", OffsetDateTime.class));
		e.setExpiresAt(rs.getObject("expires_at", OffsetDateTime.class));
		e.setRevokedAt(rs.getObject("revoked_at", OffsetDateTime.class));
		e.setRevokedBy(rs.getObject("revoked_by", UUID.class));
		return e;
	};

	public void insert(UserSessionEntity session) {
		jdbcTemplate.update("""
				insert into user_session (id, business_id, user_id, refresh_token_hash,
				    device_info, ip_address, created_at, last_used_at, expires_at)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""", session.getId(), session.getBusinessId(), session.getUserId(), session.getRefreshTokenHash(),
				session.getDeviceInfo(), session.getIpAddress(), session.getCreatedAt(), session.getLastUsedAt(),
				session.getExpiresAt());
	}

	public Optional<UserSessionEntity> findByRefreshTokenHash(String hash) {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject("""
					select * from user_session where refresh_token_hash = ?
					""", rowMapper, hash));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	public List<UserSessionEntity> findActiveByUser(UUID businessId, UUID userId, OffsetDateTime now) {
		return jdbcTemplate.query("""
				select * from user_session
				where business_id = ? and user_id = ? and revoked_at is null and expires_at > ?
				order by created_at desc
				""", rowMapper, businessId, userId, now);
	}

	public List<UserSessionEntity> findAllByUser(UUID businessId, UUID userId) {
		return jdbcTemplate.query("""
				select * from user_session
				where business_id = ? and user_id = ?
				order by created_at desc
				""", rowMapper, businessId, userId);
	}

	public void updateLastUsed(UUID id, OffsetDateTime lastUsedAt) {
		jdbcTemplate.update("update user_session set last_used_at = ? where id = ?", lastUsedAt, id);
	}

	public void revoke(UUID id, OffsetDateTime revokedAt, UUID revokedBy) {
		jdbcTemplate.update("update user_session set revoked_at = ?, revoked_by = ? where id = ?", revokedAt, revokedBy,
				id);
	}

	public void revokeAllByUser(UUID businessId, UUID userId, OffsetDateTime revokedAt, UUID revokedBy) {
		jdbcTemplate.update("""
				update user_session set revoked_at = ?, revoked_by = ?
				where business_id = ? and user_id = ? and revoked_at is null
				""", revokedAt, revokedBy, businessId, userId);
	}

	public void revokeAllByBusiness(UUID businessId, OffsetDateTime revokedAt, UUID revokedBy) {
		jdbcTemplate.update("""
				update user_session set revoked_at = ?, revoked_by = ?
				where business_id = ? and revoked_at is null
				""", revokedAt, revokedBy, businessId);
	}

	public void deleteExpired(OffsetDateTime now) {
		jdbcTemplate.update("delete from user_session where expires_at < ?", now);
	}

	public int countActiveByUser(UUID businessId, UUID userId, OffsetDateTime now) {
		Integer count = jdbcTemplate.queryForObject("""
				select count(*) from user_session
				where business_id = ? and user_id = ? and revoked_at is null and expires_at > ?
				""", Integer.class, businessId, userId, now);
		return count != null ? count : 0;
	}
}
