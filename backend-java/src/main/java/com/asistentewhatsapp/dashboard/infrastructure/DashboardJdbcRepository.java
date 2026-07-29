package com.asistentewhatsapp.dashboard.infrastructure;

import com.asistentewhatsapp.dashboard.api.DashboardActivityResponse;
import com.asistentewhatsapp.dashboard.api.DashboardAppointmentResponse;
import com.asistentewhatsapp.dashboard.api.DashboardSeriesPointResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public DashboardJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public long countOpenConversations(UUID businessId, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from conversation
				where business_id = :businessId
				  and status in ('OPEN', 'PENDING')
				  and coalesce(last_message_at, created_at) between :from and :to
				""");
		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		appendOwnerFilter(sql, parameters, "assigned_user_id", ownerUserId);
		Long count = jdbcTemplate.queryForObject(sql.toString(), parameters, Long.class);
		return count == null ? 0 : count;
	}

	public long countNewProspects(UUID businessId, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from lead
				where business_id = :businessId
				  and created_at between :from and :to
				  and active = true
				""");
		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		appendOwnerFilter(sql, parameters, "assigned_user_id", ownerUserId);
		Long count = jdbcTemplate.queryForObject(sql.toString(), parameters, Long.class);
		return count == null ? 0 : count;
	}

	public long countOpenOrders(UUID businessId, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from order_request
				where business_id = :businessId
				  and status in ('DRAFT', 'CONFIRMED')
				  and created_at between :from and :to
				""");
		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		appendOwnerFilter(sql, parameters, "created_by_user_id", ownerUserId);
		Long count = jdbcTemplate.queryForObject(sql.toString(), parameters, Long.class);
		return count == null ? 0 : count;
	}

	public long countPendingAppointments(UUID businessId, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		StringBuilder sql = new StringBuilder("""
				select count(*)
				from booking
				where business_id = :businessId
				  and status in (
				      'SOLICITADA', 'PENDIENTE_CONFIRMACION', 'PENDIENTE_PAGO',
				      'CONFIRMADA', 'REPROGRAMACION_PENDIENTE', 'REPROGRAMADA'
				  )
				  and starts_at between :from and :to
				""");
		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		appendOwnerFilter(sql, parameters, "assigned_user_id", ownerUserId);
		Long count = jdbcTemplate.queryForObject(sql.toString(), parameters, Long.class);
		return count == null ? 0 : count;
	}

	public List<DashboardSeriesPointResponse> loadConversationSeries(UUID businessId, UUID ownerUserId,
			OffsetDateTime from, OffsetDateTime to) {
		String ownerCondition = ownerUserId == null ? "" : " and c.assigned_user_id = :ownerUserId";
		String sql = """
				with days as (
				    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
				)
				select to_char(days.day, 'YYYY-MM-DD') as label,
				       coalesce(count(c.id), 0) as value
				from days
				left join conversation c
				  on c.business_id = :businessId
				 and c.created_at >= :from
				 and c.created_at <= :to
				 and cast(c.created_at as date) = days.day
				""" + ownerCondition + """
				group by days.day
				order by days.day
				""";

		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		if (ownerUserId != null) {
			parameters.addValue("ownerUserId", ownerUserId);
		}

		return jdbcTemplate.query(sql, parameters, seriesPointRowMapper());
	}

	public List<DashboardSeriesPointResponse> loadOrderSeries(UUID businessId, UUID ownerUserId, OffsetDateTime from,
			OffsetDateTime to) {
		String ownerCondition = ownerUserId == null ? "" : " and o.created_by_user_id = :ownerUserId";
		String sql = """
				with days as (
				    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
				)
				select to_char(days.day, 'YYYY-MM-DD') as label,
				       coalesce(count(o.id), 0) as value
				from days
				left join order_request o
				  on o.business_id = :businessId
				 and o.created_at >= :from
				 and o.created_at <= :to
				 and cast(o.created_at as date) = days.day
				""" + ownerCondition + """
				group by days.day
				order by days.day
				""";

		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		if (ownerUserId != null) {
			parameters.addValue("ownerUserId", ownerUserId);
		}

		return jdbcTemplate.query(sql, parameters, seriesPointRowMapper());
	}

	public List<DashboardAppointmentResponse> loadTodayAppointments(UUID businessId, UUID ownerUserId,
			OffsetDateTime from, OffsetDateTime to) {
		StringBuilder sql = new StringBuilder("""
				select
				    b.id,
				    b.subject,
				    b.status,
				    c.display_name as customer_name,
				    b.starts_at,
				    b.duration_minutes,
				    b.location
				from booking b
				join customer c
				  on c.id = b.customer_id
				 and c.business_id = b.business_id
				where b.business_id = :businessId
				  and b.status in (
				      'SOLICITADA', 'PENDIENTE_CONFIRMACION', 'PENDIENTE_PAGO',
				      'CONFIRMADA', 'REPROGRAMACION_PENDIENTE', 'REPROGRAMADA'
				  )
				  and b.starts_at between :from and :to
				""");
		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		appendOwnerFilter(sql, parameters, "b.assigned_user_id", ownerUserId);
		sql.append(" order by b.starts_at asc limit 8");

		return jdbcTemplate.query(sql.toString(), parameters, appointmentRowMapper());
	}

	public List<DashboardActivityResponse> loadRecentActivity(UUID businessId, UUID ownerUserId, OffsetDateTime from,
			OffsetDateTime to) {
		String conversationOwner = ownerUserId == null ? "" : " and c.assigned_user_id = :ownerUserId";
		String leadOwner = ownerUserId == null ? "" : " and l.assigned_user_id = :ownerUserId";
		String bookingOwner = ownerUserId == null ? "" : " and b.assigned_user_id = :ownerUserId";
		String orderOwner = ownerUserId == null ? "" : " and o.created_by_user_id = :ownerUserId";

		String sql = """
				select entity_type, entity_id, title, body, status, occurred_at
				from (
				    select
				        'CONVERSATION' as entity_type,
				        c.id as entity_id,
				        ('Nuevo mensaje de ' || c.customer_name) as title,
				        left(m.body, 220) as body,
				        c.status as status,
				        coalesce(m.received_at, m.created_at) as occurred_at
				    from message m
				    join conversation c
				      on c.id = m.conversation_id
				     and c.business_id = m.business_id
				    where m.business_id = :businessId
				      and m.direction = 'INBOUND'
				      and coalesce(m.received_at, m.created_at) between :from and :to
				""" + conversationOwner + """
				    union all
				    select
				        'LEAD' as entity_type,
				        l.id as entity_id,
				        ('Prospecto actualizado: ' || l.first_name || ' ' || l.last_name) as title,
				        coalesce(l.notes, 'Prospecto registrado en el embudo comercial.') as body,
				        l.stage as status,
				        l.created_at as occurred_at
				    from lead l
				    where l.business_id = :businessId
				      and l.created_at between :from and :to
				""" + leadOwner + """
				    union all
				    select
				        'BOOKING' as entity_type,
				        b.id as entity_id,
				        ('Cita agendada: ' || b.subject) as title,
				        coalesce(b.notes, 'Hay una cita disponible en la agenda.') as body,
				        b.status as status,
				        b.created_at as occurred_at
				    from booking b
				    where b.business_id = :businessId
				      and b.created_at between :from and :to
				""" + bookingOwner + """
				    union all
				    select
				        'ORDER' as entity_type,
				        o.id as entity_id,
				        ('Pedido creado para ' || customer.display_name) as title,
				        coalesce(o.notes, 'Pedido registrado para seguimiento comercial.') as body,
				        o.payment_status as status,
				        o.created_at as occurred_at
				    from order_request o
				    join customer customer
				      on customer.id = o.customer_id
				     and customer.business_id = o.business_id
				    where o.business_id = :businessId
				      and o.created_at between :from and :to
				""" + orderOwner + """
				) activity
				order by occurred_at desc
				limit 8
				""";

		MapSqlParameterSource parameters = baseParameters(businessId, from, to);
		if (ownerUserId != null) {
			parameters.addValue("ownerUserId", ownerUserId);
		}

		return jdbcTemplate.query(sql, parameters, activityRowMapper());
	}

	private MapSqlParameterSource baseParameters(UUID businessId, OffsetDateTime from, OffsetDateTime to) {
		return new MapSqlParameterSource().addValue("businessId", businessId).addValue("from", from).addValue("to", to);
	}

	private void appendOwnerFilter(StringBuilder sql, MapSqlParameterSource parameters, String columnName,
			UUID ownerUserId) {
		if (ownerUserId == null) {
			return;
		}
		sql.append(" and ").append(columnName).append(" = :ownerUserId");
		parameters.addValue("ownerUserId", ownerUserId);
	}

	private RowMapper<DashboardSeriesPointResponse> seriesPointRowMapper() {
		return (resultSet, rowNum) -> new DashboardSeriesPointResponse(resultSet.getString("label"),
				resultSet.getLong("value"));
	}

	private RowMapper<DashboardAppointmentResponse> appointmentRowMapper() {
		return (resultSet, rowNum) -> new DashboardAppointmentResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("subject"), resultSet.getString("status"), resultSet.getString("customer_name"),
				resultSet.getObject("starts_at", OffsetDateTime.class), resultSet.getInt("duration_minutes"),
				resultSet.getString("location"));
	}

	private RowMapper<DashboardActivityResponse> activityRowMapper() {
		return (resultSet, rowNum) -> new DashboardActivityResponse(resultSet.getString("entity_type"),
				resultSet.getObject("entity_id", UUID.class), resultSet.getString("title"), resultSet.getString("body"),
				resultSet.getString("status"), resultSet.getObject("occurred_at", OffsetDateTime.class));
	}
}
