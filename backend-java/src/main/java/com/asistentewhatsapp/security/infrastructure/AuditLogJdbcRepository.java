package com.asistentewhatsapp.security.infrastructure;

import com.asistentewhatsapp.security.api.AuditLogResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogJdbcRepository {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public AuditLogJdbcRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void insert(UUID businessId, UUID actorUserId, String actionType, String entityType, UUID entityId,
			String summary, Map<String, Object> metadata, OffsetDateTime occurredAt) {
		jdbcTemplate.update("""
				insert into audit_log (
				    id,
				    business_id,
				    actor_user_id,
				    action_type,
				    entity_type,
				    entity_id,
				    summary,
				    metadata,
				    occurred_at
				) values (?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?)
				""", UUID.randomUUID(), businessId, actorUserId, actionType, entityType, entityId, summary,
				toJson(metadata), occurredAt);
	}

	public PagedResponse<AuditLogResponse> findByBusinessId(UUID businessId, int page, int size) {
		Long totalItems = jdbcTemplate.queryForObject("select count(*) from audit_log where business_id = ?",
				Long.class, businessId);
		long resolvedTotalItems = totalItems == null ? 0 : totalItems;
		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) resolvedTotalItems / size);
		List<AuditLogResponse> items = jdbcTemplate.query("""
				select id, actor_user_id, action_type, entity_type, entity_id, summary, metadata, occurred_at
				from audit_log
				where business_id = ?
				order by occurred_at desc
				limit ?
				offset ?
				""", new AuditLogRowMapper(), businessId, size, page * size);
		return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages);
	}

	private String toJson(Map<String, Object> metadata) {
		try {
			return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("No se pudo serializar metadata de auditoria.", exception);
		}
	}

	private Map<String, Object> readMetadata(String metadata) {
		try {
			if (metadata == null || metadata.isBlank()) {
				return Map.of();
			}
			return objectMapper.readValue(metadata, MAP_TYPE);
		} catch (JsonProcessingException exception) {
			return Map.of();
		}
	}

	private class AuditLogRowMapper implements RowMapper<AuditLogResponse> {

		@Override
		public AuditLogResponse mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			return new AuditLogResponse(resultSet.getObject("id", UUID.class),
					resultSet.getObject("actor_user_id", UUID.class), resultSet.getString("action_type"),
					resultSet.getString("entity_type"), resultSet.getObject("entity_id", UUID.class),
					resultSet.getString("summary"), readMetadata(resultSet.getString("metadata")),
					resultSet.getObject("occurred_at", OffsetDateTime.class));
		}
	}
}
