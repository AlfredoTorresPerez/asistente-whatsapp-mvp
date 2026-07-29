package com.asistentewhatsapp.notifications.infrastructure;

import com.asistentewhatsapp.notifications.api.NotificationReadResponse;
import com.asistentewhatsapp.notifications.api.NotificationResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public NotificationJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public PagedResponse<NotificationResponse> findByUser(UUID businessId, UUID userId, int page, int size,
			String search, String status, String type) {
		QueryParts queryParts = buildQuery(businessId, userId, search, status, type);
		Long totalItems = jdbcTemplate.queryForObject("select count(*) " + queryParts.fromAndWhere(),
				queryParts.parameters(), Long.class);
		long resolvedTotalItems = totalItems == null ? 0 : totalItems;
		int totalPages = resolvedTotalItems == 0 ? 0 : (int) Math.ceil((double) resolvedTotalItems / size);

		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);

		List<NotificationResponse> items = jdbcTemplate.query("""
				select
				    id,
				    type,
				    status,
				    title,
				    body,
				    related_entity_type,
				    related_entity_id,
				    created_at,
				    read_at
				""" + queryParts.fromAndWhere() + """
				order by created_at desc
				limit :limit
				offset :offset
				""", parameters, notificationRowMapper());

		return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages);
	}

	public NotificationReadResponse markAsRead(UUID businessId, UUID userId, UUID notificationId) {
		OffsetDateTime readAt = OffsetDateTime.now(ZoneOffset.UTC);
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("userId", userId).addValue("notificationId", notificationId).addValue("readAt", readAt);

		jdbcTemplate.update("""
				update notification
				set status = 'READ',
				    read_at = coalesce(read_at, :readAt),
				    updated_at = :readAt
				where id = :notificationId
				  and business_id = :businessId
				  and user_id = :userId
				""", parameters);

		List<NotificationReadResponse> items = jdbcTemplate.query("""
				select id, status, read_at
				from notification
				where id = :notificationId
				  and business_id = :businessId
				  and user_id = :userId
				""", parameters, readResponseRowMapper());

		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la notificacion solicitada.");
		}

		return items.getFirst();
	}

	public long markAllAsRead(UUID businessId, UUID userId) {
		OffsetDateTime readAt = OffsetDateTime.now(ZoneOffset.UTC);
		return jdbcTemplate.update("""
				update notification
				set status = 'READ',
				    read_at = coalesce(read_at, :readAt),
				    updated_at = :readAt
				where business_id = :businessId
				  and user_id = :userId
				  and status = 'UNREAD'
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("userId", userId)
				.addValue("readAt", readAt));
	}

	private QueryParts buildQuery(UUID businessId, UUID userId, String search, String status, String type) {
		StringBuilder sql = new StringBuilder("""
				from notification
				where business_id = :businessId
				  and user_id = :userId
				""");
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("userId", userId);

		if (search != null) {
			sql.append(" and (title ilike :search or body ilike :search) ");
			parameters.addValue("search", "%" + search + "%");
		}

		if (status != null) {
			sql.append(" and status = :status ");
			parameters.addValue("status", status);
		}

		if (type != null) {
			sql.append(" and type = :type ");
			parameters.addValue("type", type);
		}

		return new QueryParts(sql.toString(), parameters);
	}

	private RowMapper<NotificationResponse> notificationRowMapper() {
		return (resultSet, rowNum) -> new NotificationResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("type"), resultSet.getString("status"), resultSet.getString("title"),
				resultSet.getString("body"), resultSet.getString("related_entity_type"),
				resultSet.getObject("related_entity_id", UUID.class),
				resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("read_at", OffsetDateTime.class));
	}

	private RowMapper<NotificationReadResponse> readResponseRowMapper() {
		return (resultSet, rowNum) -> new NotificationReadResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("status"), resultSet.getObject("read_at", OffsetDateTime.class));
	}

	private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
	}
}
