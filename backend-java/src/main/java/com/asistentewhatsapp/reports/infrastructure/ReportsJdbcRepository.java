package com.asistentewhatsapp.reports.infrastructure;

import com.asistentewhatsapp.bookings.application.BookingStateMachine;
import com.asistentewhatsapp.reports.api.ReportsAppointmentDistributionPoint;
import com.asistentewhatsapp.reports.api.ReportsAppointmentPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsChannelResponse;
import com.asistentewhatsapp.reports.api.ReportsConversationPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsFunnelStageResponse;
import com.asistentewhatsapp.reports.api.ReportsKpiItem;
import com.asistentewhatsapp.reports.api.ReportsOccupancyResponse;
import com.asistentewhatsapp.reports.api.ReportsProspectRowResponse;
import com.asistentewhatsapp.reports.api.ReportsProspectsResponse;
import com.asistentewhatsapp.reports.api.ReportsServiceDemandResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportsJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public ReportsJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// --- KPI: Conversaciones ---
	public ReportsKpiItem buildConversationsKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = countConversations(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				from, to);
		long previous = countConversations(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, previousFrom, previousTo);
		Double variation = computeVariation(current, previous);
		return new ReportsKpiItem("Conversaciones", current, previous, variation, "COUNT", false,
				"Total de conversaciones creadas en el periodo.");
	}

	// --- KPI: Prospectos ---
	public ReportsKpiItem buildProspectsKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = countProspects(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				from, to);
		long previous = countProspects(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				previousFrom, previousTo);
		Double variation = computeVariation(current, previous);
		return new ReportsKpiItem("Prospectos", current, previous, variation, "COUNT", false,
				"Total de prospectos registrados en el periodo.");
	}

	// --- KPI: Citas creadas ---
	public ReportsKpiItem buildAppointmentsCreatedKpi(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
			OffsetDateTime previousFrom, OffsetDateTime previousTo) {
		long current = countAppointments(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				from, to);
		long previous = countAppointments(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				previousFrom, previousTo);
		Double variation = computeVariation(current, previous);
		return new ReportsKpiItem("Citas creadas", current, previous, variation, "COUNT", false,
				"Total de citas creadas en el periodo (por fecha de creacion).");
	}

	// --- KPI: Citas confirmadas ---
	public ReportsKpiItem buildConfirmedAppointmentsKpi(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
			OffsetDateTime previousFrom, OffsetDateTime previousTo) {
		long current = countConfirmedAppointments(businessId, locationId, professionalId, serviceId, ownerUserId, from,
				to);
		long previous = countConfirmedAppointments(businessId, locationId, professionalId, serviceId, ownerUserId,
				previousFrom, previousTo);
		Double variation = computeVariation(current, previous);
		return new ReportsKpiItem("Citas confirmadas", current, previous, variation, "COUNT", false,
				"Total de citas actualmente confirmadas con fecha de atencion en el periodo.");
	}

	// --- KPI: Tasa de respuesta ---
	public ReportsKpiItem buildResponseRateKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		double current = calculateResponseRate(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, from, to);
		double previous = calculateResponseRate(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, previousFrom, previousTo);
		Double variation = previous == 0 && current > 0 ? null : current - previous;
		return new ReportsKpiItem("Tasa de respuesta", Math.round(current), Math.round(previous), variation, "PERCENT",
				false, "Porcentaje de conversaciones con al menos una respuesta.");
	}

	// --- KPI: Conversion a cita ---
	public ReportsKpiItem buildConversionRateKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		double current = calculateConversionRate(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, from, to);
		double previous = calculateConversionRate(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, previousFrom, previousTo);
		Double variation = previous == 0 && current > 0 ? null : current - previous;
		return new ReportsKpiItem("Conversion a cita", Math.round(current), Math.round(previous), variation, "PERCENT",
				false, "Porcentaje de prospectos que generaron al menos una cita.");
	}

	// --- Channel distribution ---
	public List<ReportsChannelResponse> loadChannelDistribution(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(params, filter, "c.location_id", locationId);
		appendFilter(params, filter, "c.assigned_user_id", ownerUserId);

		String sql = """
				select c.channel_type, count(*) as cnt
				from conversation c
				where c.business_id = :businessId
				  and c.created_at between :from and :to
				""" + filter + """
				group by c.channel_type
				order by cnt desc
				""";

		List<ReportsChannelResponse> channels = jdbcTemplate.query(sql, params, channelRowMapper());
		long total = channels.stream().mapToLong(ReportsChannelResponse::count).sum();
		if (total == 0) {
			return List.of(new ReportsChannelResponse("WHATSAPP", 0, 100.0));
		}
		return channels.stream().map(c -> new ReportsChannelResponse(c.channel(), c.count(),
				Math.round((double) c.count() * 1000 / total) / 10.0)).toList();
	}

	// --- Conversation performance (daily) with AI vs Human ---
	public List<ReportsConversationPerformancePoint> loadConversationPerformance(UUID businessId, UUID locationId,
			UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from,
			OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(params, filter, "c.location_id", locationId);
		appendFilter(params, filter, "c.assigned_user_id", ownerUserId);

		String sql = """
				with days as (
				    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
				),
				received as (
				    select cast(m.created_at as date) as day, count(distinct m.conversation_id) as cnt
				    from message m
				    join conversation c on c.id = m.conversation_id and c.business_id = m.business_id
				    where m.business_id = :businessId
				      and m.direction = 'INBOUND'
				      and m.created_at between :from and :to
				    """ + filter + """
				    group by cast(m.created_at as date)
				),
				ai_answered as (
				    select cast(m.created_at as date) as day, count(distinct m.conversation_id) as cnt
				    from message m
				    join conversation c on c.id = m.conversation_id and c.business_id = m.business_id
				    where m.business_id = :businessId
				      and m.direction = 'OUTBOUND'
				      and m.sent_by_user_id is null
				      and m.created_at between :from and :to
				      and exists (
				          select 1 from message m2
				          where m2.conversation_id = m.conversation_id
				            and m2.business_id = m.business_id
				            and m2.direction = 'INBOUND'
				            and m2.created_at < m.created_at
				      )
				    """ + filter + """
				    group by cast(m.created_at as date)
				),
				human_answered as (
				    select cast(m.created_at as date) as day, count(distinct m.conversation_id) as cnt
				    from message m
				    join conversation c on c.id = m.conversation_id and c.business_id = m.business_id
				    where m.business_id = :businessId
				      and m.direction = 'OUTBOUND'
				      and m.sent_by_user_id is not null
				      and m.created_at between :from and :to
				      and exists (
				          select 1 from message m2
				          where m2.conversation_id = m.conversation_id
				            and m2.business_id = m.business_id
				            and m2.direction = 'INBOUND'
				            and m2.created_at < m.created_at
				      )
				    """ + filter + """
				    group by cast(m.created_at as date)
				)
				select to_char(days.day, 'YYYY-MM-DD') as label,
				       coalesce(r.cnt, 0) as received,
				       coalesce(ai.cnt, 0) as ai_answered,
				       coalesce(hu.cnt, 0) as human_answered
				from days
				left join received r on r.day = days.day
				left join ai_answered ai on ai.day = days.day
				left join human_answered hu on hu.day = days.day
				order by days.day
				""";

		return jdbcTemplate.query(sql, params, conversationPerformanceRowMapper());
	}

	// --- Appointment performance (daily by appointment time) ---
	public List<ReportsAppointmentPerformancePoint> loadAppointmentPerformance(UUID businessId, UUID locationId,
			UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from,
			OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(params, filter, "b.location_id", locationId);
		appendFilter(params, filter, "b.professional_id", professionalId);
		appendFilter(params, filter, "b.service_id", serviceId);
		appendFilter(params, filter, "b.assigned_user_id", ownerUserId);
		StringBuilder statusFilter = new StringBuilder();
		appendBookingStatusFilter(statusFilter, params, "b.status", bookingStatus);

		String sql = """
				with days as (
				    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
				),
				counts as (
				    select cast(b.starts_at as date) as day,
				           count(*) filter (where b.status in ('SOLICITADA', 'PENDIENTE_CONFIRMACION', 'PENDIENTE_PAGO')) as solicitada,
				           count(*) filter (where b.status = 'CONFIRMADA') as confirmada,
				                   count(*) filter (where b.status in ('ATENDIDA', 'COMPLETADA')) as completada,
				           count(*) filter (where b.status in ('CANCELADA', 'CANCELADA_POR_CLIENTE')) as cancelada,
				           count(*) filter (where b.status = 'NO_ASISTE') as ausencia
				    from booking b
				    where b.business_id = :businessId
				      and b.starts_at between :from and :to
				    """
				+ filter + statusFilter + """
						    group by cast(b.starts_at as date)
						)
						select to_char(days.day, 'YYYY-MM-DD') as label,
						       coalesce(c.solicitada, 0) as solicitada,
						       coalesce(c.confirmada, 0) as confirmada,
						       coalesce(c.completada, 0) as completada,
						       coalesce(c.cancelada, 0) as cancelada,
						       coalesce(c.ausencia, 0) as ausencia
						from days
						left join counts c on c.day = days.day
						order by days.day
						""";

		return jdbcTemplate.query(sql, params, appointmentPerformanceRowMapper());
	}

	// --- Appointment status distribution (pie chart) ---
	public List<ReportsAppointmentDistributionPoint> loadAppointmentDistribution(UUID businessId, UUID locationId,
			UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from,
			OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(params, filter, "b.location_id", locationId);
		appendFilter(params, filter, "b.professional_id", professionalId);
		appendFilter(params, filter, "b.service_id", serviceId);
		appendFilter(params, filter, "b.assigned_user_id", ownerUserId);
		StringBuilder statusFilter = new StringBuilder();
		appendBookingStatusFilter(statusFilter, params, "b.status", bookingStatus);

		String sql = """
				select b.status, count(*) as cnt
				from booking b
				where b.business_id = :businessId
				  and b.starts_at between :from and :to
				""" + filter + statusFilter + """
				group by b.status
				order by cnt desc
				""";

		List<Map.Entry<String, Long>> raw = jdbcTemplate.query(sql, params,
				(rs, rn) -> Map.entry(rs.getString("status"), rs.getLong("cnt")));
		long total = raw.stream().mapToLong(Map.Entry::getValue).sum();
		if (total == 0)
			return List.of();

		return raw.stream().map(e -> {
			String label = switch (e.getKey()) {
				case "SOLICITADA" -> "Solicitada";
				case "PENDIENTE_CONFIRMACION" -> "Pendiente confirmacion";
				case "PENDIENTE_PAGO" -> "Pendiente pago";
				case "CONFIRMADA" -> "Confirmada";
				case "REPROGRAMACION_PENDIENTE" -> "Reprogramacion pendiente";
				case "REPROGRAMADA" -> "Reprogramada";
				case "CANCELADA" -> "Cancelada";
				case "CANCELADA_POR_CLIENTE" -> "Cancelada por cliente";
				case "EXPIRADA" -> "Expirada";
				case "ATENDIDA", "COMPLETADA" -> "Completada";
				case "NO_ASISTE" -> "Inasistencia";
				default -> e.getKey();
			};
			double pct = Math.round((double) e.getValue() * 1000 / total) / 10.0;
			return new ReportsAppointmentDistributionPoint(e.getKey(), label, e.getValue(), pct);
		}).toList();
	}

	// --- Conversion funnel ---
	public List<ReportsFunnelStageResponse> loadConversionFunnel(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder convFilter = new StringBuilder();
		appendFilter(params, convFilter, "c.location_id", locationId);
		appendFilter(params, convFilter, "c.assigned_user_id", ownerUserId);

		StringBuilder leadFilter = new StringBuilder();
		appendFilter(params, leadFilter, "l.location_id", locationId);
		appendFilter(params, leadFilter, "l.assigned_user_id", ownerUserId);

		StringBuilder bkFilter = new StringBuilder();
		appendFilter(params, bkFilter, "b.location_id", locationId);
		appendFilter(params, bkFilter, "b.professional_id", professionalId);
		appendFilter(params, bkFilter, "b.service_id", serviceId);
		appendFilter(params, bkFilter, "b.assigned_user_id", ownerUserId);
		StringBuilder bkStatusFilter = new StringBuilder();
		appendBookingStatusFilter(bkStatusFilter, params, "b.status", bookingStatus);

		String sql = """
				with conversations as (
				    select count(*) as cnt from conversation c
				    where c.business_id = :businessId and c.created_at between :from and :to
				""" + convFilter + """
				),
				prospects as (
				    select count(*) as cnt from lead l
				    where l.business_id = :businessId and l.active = true and l.created_at between :from and :to
				""" + leadFilter + """
				),
				with_booking as (
				    select count(distinct l.id) as cnt from lead l
				    join booking b on b.lead_id = l.id and b.business_id = l.business_id
				    where l.business_id = :businessId and l.active = true
				      and (l.created_at between :from and :to)
				""" + leadFilter + """
				    and b.created_at between :from and :to
				""" + bkFilter + bkStatusFilter + """
				),
				confirmed as (
				    select count(distinct l.id) as cnt from lead l
				    join booking b on b.lead_id = l.id and b.business_id = l.business_id
				    where l.business_id = :businessId and l.active = true
				      and (l.created_at between :from and :to)
				""" + leadFilter + """
				    and b.created_at between :from and :to and b.status = 'CONFIRMADA'
				""" + bkFilter + """
				),
				completed as (
				    select count(distinct l.id) as cnt from lead l
				    join booking b on b.lead_id = l.id and b.business_id = l.business_id
				    where l.business_id = :businessId and l.active = true
				      and (l.created_at between :from and :to)
				""" + leadFilter + """
				    and b.created_at between :from and :to and b.status in ('ATENDIDA', 'COMPLETADA')
				""" + bkFilter + """
				)
				select
				    (select cnt from conversations) as conversations,
				    (select cnt from prospects) as prospects,
				    (select cnt from with_booking) as with_booking,
				    (select cnt from confirmed) as confirmed,
				    (select cnt from completed) as completed
				""";

		Map<String, Long> result = jdbcTemplate.queryForObject(sql, params, (rs, rn) -> {
			Map<String, Long> m = new HashMap<>();
			m.put("conversations", rs.getLong("conversations"));
			m.put("prospects", rs.getLong("prospects"));
			m.put("with_booking", rs.getLong("with_booking"));
			m.put("confirmed", rs.getLong("confirmed"));
			m.put("completed", rs.getLong("completed"));
			return m;
		});

		if (result == null)
			return List.of();

		long conv = result.getOrDefault("conversations", 0L);
		long pros = result.getOrDefault("prospects", 0L);
		long wb = result.getOrDefault("with_booking", 0L);
		long conf = result.getOrDefault("confirmed", 0L);
		long comp = result.getOrDefault("completed", 0L);

		return List.of(stage("Conversaciones", conv, null, null), stage("Prospectos", pros, conv, conv),
				stage("Solicitudes de cita", wb, pros, conv), stage("Citas confirmadas", conf, wb, conv),
				stage("Citas completadas", comp, conf, conv));
	}

	// --- Prospects detail (paginated) ---
	public ReportsProspectsResponse loadProspects(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, String search, int page,
			int size) {
		MapSqlParameterSource baseParams = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(baseParams, filter, "l.location_id", locationId);
		appendFilter(baseParams, filter, "l.assigned_user_id", ownerUserId);
		StringBuilder bkFilter = new StringBuilder();
		if (professionalId != null) {
			bkFilter.append(" and b5.professional_id = :professionalId");
			baseParams.addValue("professionalId", professionalId);
		}
		if (serviceId != null) {
			bkFilter.append(" and b5.service_id = :serviceId");
			baseParams.addValue("serviceId", serviceId);
		}
		StringBuilder bkStatusFilter = new StringBuilder();
		appendBookingStatusFilter(bkStatusFilter, baseParams, "b5.status", bookingStatus);

		MapSqlParameterSource countParams = copyParams(baseParams, from, to, businessId);
		StringBuilder countFilter = new StringBuilder(filter.toString());
		if (ownerUserId != null) {
			countFilter.append(" and l.assigned_user_id = :ownerUserId");
		}
		if (locationId != null) {
			countFilter.append(" and l.location_id = :locationId");
		}
		String countSql = "select count(*) from lead l where l.business_id = :businessId and l.active = true and l.created_at between :from and :to"
				+ countFilter;
		Long total = jdbcTemplate.queryForObject(countSql, countParams, Long.class);

		String dataSql = """
				select l.id,
				       l.first_name || ' ' || l.last_name as name,
				       l.phone,
				       l.stage,
				       l.created_at as last_contact,
				       concat(u.first_name, ' ', u.last_name) as responsible,
				       (select min(b2.starts_at) from booking b2
				        where b2.lead_id = l.id and b2.business_id = l.business_id
				          and b2.starts_at > current_timestamp
				          and b2.status not in ('CANCELADA', 'CANCELADA_POR_CLIENTE', 'EXPIRADA', 'NO_ASISTE')
				          and b2.status not in ('ATENDIDA', 'COMPLETADA')) as next_appointment,
				       bl.name as location_name,
				       asv.name as service_interest,
				       case
				           when l.stage in ('WON') then 'Cerrado'
				           when l.stage in ('LOST') then 'Perdido'
				           when exists (select 1 from booking b3 where b3.lead_id = l.id and b3.business_id = l.business_id and b3.status in ('ATENDIDA', 'COMPLETADA')) then 'Cerrado'
				           when exists (select 1 from booking b4 where b4.lead_id = l.id and b4.business_id = l.business_id and b4.status in ('CONFIRMADA', 'PENDIENTE_CONFIRMACION')) then 'Con cita'
				           else 'Pendiente'
				       end as attention_status
				from lead l
				left join user_account u on u.id = l.assigned_user_id
				left join booking b5 on b5.lead_id = l.id and b5.business_id = l.business_id
				left join business_location bl on bl.id = coalesce(b5.location_id, l.location_id)
				left join aesthetic_service asv on asv.id = b5.service_id
				where l.business_id = :businessId
				  and l.active = true
				  and l.created_at between :from and :to
				"""
				+ filter + bkFilter + bkStatusFilter
				+ """
						group by l.id, l.first_name, l.last_name, l.phone, l.stage, l.created_at, u.first_name, u.last_name, bl.name, asv.name
						order by l.created_at desc
						limit :limit offset :offset
						""";

		MapSqlParameterSource dataParams = copyParams(baseParams, from, to, businessId);
		dataParams.addValue("limit", size);
		dataParams.addValue("offset", (long) page * size);

		List<ReportsProspectRowResponse> items = jdbcTemplate.query(dataSql, dataParams, prospectRowMapper());
		return new ReportsProspectsResponse(items, total == null ? 0 : total, page, size);
	}

	public ReportsKpiItem buildAvailableHoursKpi(UUID businessId, UUID locationId, UUID professionalId,
			OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom, OffsetDateTime previousTo) {
		long current = Math.round(countAvailableMinutes(businessId, locationId, professionalId, from, to) / 60.0);
		long previous = Math
				.round(countAvailableMinutes(businessId, locationId, professionalId, previousFrom, previousTo) / 60.0);
		return new ReportsKpiItem("Horas disponibles", current, previous, computeVariation(current, previous), "HOURS",
				false, "Horas configuradas de profesionales activos para el periodo seleccionado.");
	}

	public ReportsKpiItem buildReservedHoursKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = Math.round(countReservedMinutes(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, from, to) / 60.0);
		long previous = Math.round(countReservedMinutes(businessId, locationId, professionalId, serviceId,
				bookingStatus, ownerUserId, previousFrom, previousTo) / 60.0);
		return new ReportsKpiItem("Horas reservadas", current, previous, computeVariation(current, previous), "HOURS",
				false, "Horas ocupadas por citas no canceladas ni expiradas.");
	}

	public ReportsKpiItem buildCancellationsKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = countBookingsByStatuses(businessId, locationId, professionalId, serviceId, null, ownerUserId,
				from, to, List.of("CANCELADA", "CANCELADA_POR_CLIENTE"));
		long previous = countBookingsByStatuses(businessId, locationId, professionalId, serviceId, null, ownerUserId,
				previousFrom, previousTo, List.of("CANCELADA", "CANCELADA_POR_CLIENTE"));
		return new ReportsKpiItem("Cancelaciones", current, previous, computeVariation(current, previous), "COUNT",
				true, "Citas canceladas con fecha de atencion dentro del periodo.");
	}

	public ReportsKpiItem buildReschedulesKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = countStatusTransitions(businessId, locationId, professionalId, serviceId, ownerUserId, from, to,
				List.of("REPROGRAMADA", "REPROGRAMACION_PENDIENTE"));
		long previous = countStatusTransitions(businessId, locationId, professionalId, serviceId, ownerUserId,
				previousFrom, previousTo, List.of("REPROGRAMADA", "REPROGRAMACION_PENDIENTE"));
		return new ReportsKpiItem("Reprogramaciones", current, previous, computeVariation(current, previous), "COUNT",
				true, "Cambios de estado asociados a reprogramaciones durante el periodo.");
	}

	public ReportsKpiItem buildNoShowsKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = countBookingsByStatuses(businessId, locationId, professionalId, serviceId, null, ownerUserId,
				from, to, List.of("NO_ASISTE"));
		long previous = countBookingsByStatuses(businessId, locationId, professionalId, serviceId, null, ownerUserId,
				previousFrom, previousTo, List.of("NO_ASISTE"));
		return new ReportsKpiItem("Inasistencias", current, previous, computeVariation(current, previous), "COUNT",
				true, "Citas marcadas como inasistencia en el periodo.");
	}

	public ReportsKpiItem buildConfirmationsKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = countStatusTransitions(businessId, locationId, professionalId, serviceId, ownerUserId, from, to,
				List.of("CONFIRMADA"));
		long previous = countStatusTransitions(businessId, locationId, professionalId, serviceId, ownerUserId,
				previousFrom, previousTo, List.of("CONFIRMADA"));
		return new ReportsKpiItem("Confirmaciones", current, previous, computeVariation(current, previous), "COUNT",
				false, "Confirmaciones registradas durante el periodo.");
	}

	public ReportsKpiItem buildAverageResponseMinutesKpi(UUID businessId, UUID locationId, UUID ownerUserId,
			OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom, OffsetDateTime previousTo) {
		long current = Math.round(averageResponseMinutes(businessId, locationId, ownerUserId, from, to));
		long previous = Math
				.round(averageResponseMinutes(businessId, locationId, ownerUserId, previousFrom, previousTo));
		return new ReportsKpiItem("Tiempo de respuesta", current, previous, computeVariation(current, previous),
				"MINUTES", true, "Minutos promedio hasta la primera respuesta posterior a un mensaje recibido.");
	}

	public ReportsKpiItem buildConversationToBookingKpi(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = Math.round(
				conversationToBookingRate(businessId, locationId, professionalId, serviceId, ownerUserId, from, to));
		long previous = Math.round(conversationToBookingRate(businessId, locationId, professionalId, serviceId,
				ownerUserId, previousFrom, previousTo));
		Double variation = previous == 0 && current > 0 ? null : (double) (current - previous);
		return new ReportsKpiItem("Conversacion a cita", current, previous, variation, "PERCENT", false,
				"Porcentaje de conversaciones del periodo que tienen una cita relacionada.");
	}

	public ReportsKpiItem buildLeadToBookingKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = Math
				.round(leadToBookingRate(businessId, locationId, professionalId, serviceId, ownerUserId, from, to));
		long previous = Math.round(leadToBookingRate(businessId, locationId, professionalId, serviceId, ownerUserId,
				previousFrom, previousTo));
		Double variation = previous == 0 && current > 0 ? null : (double) (current - previous);
		return new ReportsKpiItem("Prospecto a cita", current, previous, variation, "PERCENT", false,
				"Porcentaje de prospectos del periodo que tienen una cita relacionada.");
	}

	public ReportsKpiItem buildEstimatedRevenueKpi(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
			OffsetDateTime previousFrom, OffsetDateTime previousTo) {
		long current = sumEstimatedRevenue(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, from, to);
		long previous = sumEstimatedRevenue(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, previousFrom, previousTo);
		return new ReportsKpiItem("Ingresos estimados", current, previous, computeVariation(current, previous),
				"CURRENCY", false, "Suma del precio base vigente del servicio en citas no canceladas.");
	}

	public ReportsKpiItem buildDepositsKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = sumApprovedPayments(businessId, locationId, professionalId, serviceId, ownerUserId, from, to);
		long previous = sumApprovedPayments(businessId, locationId, professionalId, serviceId, ownerUserId,
				previousFrom, previousTo);
		return new ReportsKpiItem("Abonos", current, previous, computeVariation(current, previous), "CURRENCY", false,
				"Pagos aprobados relacionados con citas del periodo.");
	}

	public ReportsKpiItem buildPendingBalancesKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = sumPendingBalances(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				from, to);
		long previous = sumPendingBalances(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, previousFrom, previousTo);
		return new ReportsKpiItem("Saldos pendientes", current, previous, computeVariation(current, previous),
				"CURRENCY", true, "Saldo estimado pendiente: precio base menos pagos aprobados.");
	}

	public ReportsKpiItem buildNewCustomersKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = countNewCustomers(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				from, to);
		long previous = countNewCustomers(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId,
				previousFrom, previousTo);
		return new ReportsKpiItem("Clientes nuevos", current, previous, computeVariation(current, previous), "COUNT",
				false, "Clientes creados o con primera cita dentro del periodo.");
	}

	public ReportsKpiItem buildRecurringCustomersKpi(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
			OffsetDateTime previousFrom, OffsetDateTime previousTo) {
		long current = countRecurringCustomers(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, from, to);
		long previous = countRecurringCustomers(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, previousFrom, previousTo);
		return new ReportsKpiItem("Clientes recurrentes", current, previous, computeVariation(current, previous),
				"COUNT", false, "Clientes con dos o mas citas no canceladas en el periodo.");
	}

	public ReportsKpiItem buildRetentionKpi(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, OffsetDateTime previousFrom,
			OffsetDateTime previousTo) {
		long current = Math.round(
				retentionRate(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, from, to));
		long previous = Math.round(retentionRate(businessId, locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, previousFrom, previousTo));
		Double variation = previous == 0 && current > 0 ? null : (double) (current - previous);
		return new ReportsKpiItem("Retencion", current, previous, variation, "PERCENT", false,
				"Porcentaje de clientes del periodo que ya tenian una cita anterior.");
	}

	public List<ReportsOccupancyResponse> loadOccupancyByProfessional(UUID businessId, UUID locationId,
			UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from,
			OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder availableFilter = new StringBuilder();
		appendFilter(params, availableFilter, "aph.location_id", locationId);
		appendFilter(params, availableFilter, "aph.professional_id", professionalId);
		StringBuilder reservedFilter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				with days as (
				    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
				),
				available as (
				    select aph.professional_id as id,
				           round(sum(extract(epoch from (aph.end_time - aph.start_time)) / 60))::bigint as minutes
				    from days
				    join agenda_professional_hours aph on aph.business_id = :businessId
				     and aph.active = true
				     and aph.day_of_week = extract(isodow from days.day)
				    join aesthetic_professional ap on ap.id = aph.professional_id and ap.business_id = aph.business_id and ap.active = true
				    where true
				"""
				+ availableFilter
				+ """
						    group by aph.professional_id
						),
						reserved as (
						    select b.professional_id as id, coalesce(sum(b.duration_minutes), 0)::bigint as minutes
						    from booking b
						    where b.business_id = :businessId and b.professional_id is not null and b.starts_at between :from and :to
						"""
				+ reservedFilter + """
						    group by b.professional_id
						)
						select ap.id, ap.full_name as name, coalesce(a.minutes, 0) as available_minutes,
						       coalesce(r.minutes, 0) as reserved_minutes
						from aesthetic_professional ap
						left join available a on a.id = ap.id
						left join reserved r on r.id = ap.id
						where ap.business_id = :businessId and ap.active = true
						""" + (professionalId == null ? "" : " and ap.id = :aph_professional_id") + """
						order by reserved_minutes desc, name
						limit 10
						""";
		return jdbcTemplate.query(sql, params, occupancyRowMapper());
	}

	public List<ReportsOccupancyResponse> loadOccupancyByRoom(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder availableFilter = new StringBuilder();
		appendFilter(params, availableFilter, "abh.location_id", locationId);
		StringBuilder roomFilter = new StringBuilder();
		appendFilter(params, roomFilter, "ar.location_id", locationId);
		StringBuilder reservedFilter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				with days as (
				    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
				),
				available as (
				    select ar.id,
				           round(sum(extract(epoch from (abh.end_time - abh.start_time)) / 60))::bigint as minutes
				    from days
				    join agenda_business_hours abh on abh.business_id = :businessId
				     and abh.active = true
				     and abh.day_of_week = extract(isodow from days.day)
				    join agenda_room ar on ar.business_id = abh.business_id and ar.location_id = abh.location_id and ar.active = true
				    where true
				"""
				+ availableFilter
				+ """
						    group by ar.id
						),
						reserved as (
						    select b.room_id as id, coalesce(sum(b.duration_minutes), 0)::bigint as minutes
						    from booking b
						    where b.business_id = :businessId and b.room_id is not null and b.starts_at between :from and :to
						"""
				+ reservedFilter + """
						    group by b.room_id
						)
						select ar.id, ar.name, coalesce(a.minutes, 0) as available_minutes,
						       coalesce(r.minutes, 0) as reserved_minutes
						from agenda_room ar
						left join available a on a.id = ar.id
						left join reserved r on r.id = ar.id
						where ar.business_id = :businessId and ar.active = true
						""" + roomFilter + """
						order by reserved_minutes desc, name
						limit 10
						""";
		return jdbcTemplate.query(sql, params, occupancyRowMapper());
	}

	public List<ReportsOccupancyResponse> loadOccupancyByLocation(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder availableFilter = new StringBuilder();
		appendFilter(params, availableFilter, "aph.location_id", locationId);
		appendFilter(params, availableFilter, "aph.professional_id", professionalId);
		StringBuilder locationFilter = new StringBuilder();
		appendFilter(params, locationFilter, "bl.id", locationId);
		StringBuilder reservedFilter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				with days as (
				    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
				),
				available as (
				    select aph.location_id as id,
				           round(sum(extract(epoch from (aph.end_time - aph.start_time)) / 60))::bigint as minutes
				    from days
				    join agenda_professional_hours aph on aph.business_id = :businessId
				     and aph.active = true
				     and aph.day_of_week = extract(isodow from days.day)
				    join aesthetic_professional ap on ap.id = aph.professional_id and ap.business_id = aph.business_id and ap.active = true
				    where true
				"""
				+ availableFilter
				+ """
						    group by aph.location_id
						),
						reserved as (
						    select b.location_id as id, coalesce(sum(b.duration_minutes), 0)::bigint as minutes
						    from booking b
						    where b.business_id = :businessId and b.location_id is not null and b.starts_at between :from and :to
						"""
				+ reservedFilter + """
						    group by b.location_id
						)
						select bl.id, bl.name, coalesce(a.minutes, 0) as available_minutes,
						       coalesce(r.minutes, 0) as reserved_minutes
						from business_location bl
						left join available a on a.id = bl.id
						left join reserved r on r.id = bl.id
						where bl.business_id = :businessId and bl.active = true
						""" + locationFilter + """
						order by reserved_minutes desc, name
						""";
		return jdbcTemplate.query(sql, params, occupancyRowMapper());
	}

	public List<ReportsServiceDemandResponse> loadTopServices(UUID businessId, UUID locationId, UUID professionalId,
			UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				select s.id, s.name, count(b.id) as bookings, coalesce(round(sum(s.price_base)), 0)::bigint as estimated_revenue
				from booking b
				join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				where b.business_id = :businessId
				  and b.starts_at between :from and :to
				"""
				+ filter + """
						group by s.id, s.name
						order by bookings desc, estimated_revenue desc, s.name
						limit 10
						""";
		return jdbcTemplate.query(sql, params,
				(rs, rn) -> new ReportsServiceDemandResponse(rs.getObject("id", UUID.class), rs.getString("name"),
						rs.getLong("bookings"), rs.getLong("estimated_revenue")));
	}

	// --- Private helpers ---

	private long countConversations(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder sql = new StringBuilder(
				"select count(*) from conversation c where c.business_id = :businessId and c.created_at between :from and :to");
		appendFilter(params, sql, "c.location_id", locationId);
		appendOwner(sql, params, "c.assigned_user_id", ownerUserId);
		Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
		return count == null ? 0 : count;
	}

	private long countProspects(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder sql = new StringBuilder(
				"select count(*) from lead l where l.business_id = :businessId and l.active = true and l.created_at between :from and :to");
		appendFilter(params, sql, "l.location_id", locationId);
		appendOwner(sql, params, "l.assigned_user_id", ownerUserId);
		Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
		return count == null ? 0 : count;
	}

	private long countAppointments(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder sql = new StringBuilder(
				"select count(*) from booking b where b.business_id = :businessId and b.created_at between :from and :to");
		appendFilter(params, sql, "b.location_id", locationId);
		appendFilter(params, sql, "b.professional_id", professionalId);
		appendFilter(params, sql, "b.service_id", serviceId);
		appendOwner(sql, params, "b.assigned_user_id", ownerUserId);
		appendBookingStatusFilter(sql, params, "b.status", bookingStatus);
		Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
		return count == null ? 0 : count;
	}

	private long countConfirmedAppointments(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder sql = new StringBuilder(
				"select count(*) from booking b where b.business_id = :businessId and b.starts_at between :from and :to and b.status = 'CONFIRMADA'");
		appendFilter(params, sql, "b.location_id", locationId);
		appendFilter(params, sql, "b.professional_id", professionalId);
		appendFilter(params, sql, "b.service_id", serviceId);
		appendOwner(sql, params, "b.assigned_user_id", ownerUserId);
		Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
		return count == null ? 0 : count;
	}

	private double calculateResponseRate(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(params, filter, "c.location_id", locationId);
		appendOwnerRaw(params, filter, "c.assigned_user_id", ownerUserId);

		String sql = """
				select case when count(*) = 0 then 0.0
				else round(count(*) filter (where has_response) * 100.0 / count(*), 1) end
				from (
				    select c.id,
				           exists (select 1 from message m
				                   where m.conversation_id = c.id
				                     and m.business_id = c.business_id
				                     and m.direction = 'OUTBOUND'
				                     and m.created_at >= c.created_at) as has_response
				    from conversation c
				    where c.business_id = :businessId
				      and c.created_at between :from and :to
				""" + filter + """
				) sub
				""";
		Double rate = jdbcTemplate.queryForObject(sql, params, Double.class);
		return rate == null ? 0.0 : rate;
	}

	private double calculateConversionRate(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder sql = new StringBuilder("""
				select case when count(*) = 0 then 0.0
				else round(count(*) filter (where has_booking) * 100.0 / count(*), 1) end
				from (
				    select l.id,
				           exists (select 1 from booking b
				                   where b.lead_id = l.id
				                     and b.business_id = l.business_id
				                     and b.created_at between :from and :to) as has_booking
				    from lead l
				    where l.business_id = :businessId
				      and l.active = true
				      and l.created_at between :from and :to
				""");
		appendFilter(params, sql, "l.location_id", locationId);
		appendOwner(sql, params, "l.assigned_user_id", ownerUserId);
		sql.append(") sub");
		Double rate = jdbcTemplate.queryForObject(sql.toString(), params, Double.class);
		return rate == null ? 0.0 : rate;
	}

	private long countAvailableMinutes(UUID businessId, UUID locationId, UUID professionalId, OffsetDateTime from,
			OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(params, filter, "aph.location_id", locationId);
		appendFilter(params, filter, "aph.professional_id", professionalId);
		String sql = """
				select coalesce(round(sum(extract(epoch from (aph.end_time - aph.start_time)) / 60)), 0)::bigint
				from generate_series(cast(:from as date), cast(:to as date), interval '1 day') days(day)
				join agenda_professional_hours aph on aph.business_id = :businessId
				 and aph.active = true
				 and aph.day_of_week = extract(isodow from days.day)
				join aesthetic_professional ap on ap.id = aph.professional_id and ap.business_id = aph.business_id and ap.active = true
				where true
				"""
				+ filter;
		return queryLong(sql, params);
	}

	private long countReservedMinutes(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				select coalesce(sum(b.duration_minutes), 0)::bigint
				from booking b
				where b.business_id = :businessId
				  and b.starts_at between :from and :to
				""" + filter;
		return queryLong(sql, params);
	}

	private long countBookingsByStatuses(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, List<String> statuses) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		params.addValue("statuses", statuses);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, false);
		String sql = """
				select count(*)
				from booking b
				where b.business_id = :businessId
				  and b.starts_at between :from and :to
				  and b.status in (:statuses)
				""" + filter;
		return queryLong(sql, params);
	}

	private long countStatusTransitions(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to, List<String> statuses) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		params.addValue("statuses", statuses);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, null, ownerUserId,
				false);
		String sql = """
				select count(*)
				from booking_status_history h
				join booking b on b.id = h.booking_id and b.business_id = h.business_id
				where h.business_id = :businessId
				  and h.created_at between :from and :to
				  and h.new_status in (:statuses)
				""" + filter;
		return queryLong(sql, params);
	}

	private double averageResponseMinutes(UUID businessId, UUID locationId, UUID ownerUserId, OffsetDateTime from,
			OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = new StringBuilder();
		appendFilter(params, filter, "c.location_id", locationId);
		appendOwnerRaw(params, filter, "c.assigned_user_id", ownerUserId);
		String sql = """
				with inbound as (
				    select c.id as conversation_id, min(m.created_at) as first_inbound_at
				    from conversation c
				    join message m on m.conversation_id = c.id and m.business_id = c.business_id
				    where c.business_id = :businessId
				      and c.created_at between :from and :to
				      and m.direction = 'INBOUND'
				""" + filter + """
				    group by c.id
				),
				first_response as (
				    select i.conversation_id, min(m.created_at) as first_response_at
				    from inbound i
				    join message m on m.conversation_id = i.conversation_id
				     and m.business_id = :businessId
				     and m.direction = 'OUTBOUND'
				     and m.created_at >= i.first_inbound_at
				    group by i.conversation_id
				)
				select coalesce(round(extract(epoch from avg(first_response_at - first_inbound_at)) / 60), 0)
				from inbound i
				join first_response r on r.conversation_id = i.conversation_id
				""";
		return queryDouble(sql, params);
	}

	private double conversationToBookingRate(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder convFilter = new StringBuilder();
		appendFilter(params, convFilter, "c.location_id", locationId);
		appendOwnerRaw(params, convFilter, "c.assigned_user_id", ownerUserId);
		StringBuilder bkFilter = bookingFilters(params, "b", locationId, professionalId, serviceId, null, ownerUserId,
				true);
		String sql = """
				with conversations as (
				    select c.id
				    from conversation c
				    where c.business_id = :businessId and c.created_at between :from and :to
				""" + convFilter + """
				),
				booked as (
				    select distinct b.conversation_id
				    from booking b
				    where b.business_id = :businessId
				      and b.conversation_id is not null
				      and b.starts_at between :from and :to
				""" + bkFilter + """
				)
				select case when count(c.id) = 0 then 0.0
				            else round(count(b.conversation_id) * 100.0 / count(c.id), 1) end
				from conversations c
				left join booked b on b.conversation_id = c.id
				""";
		return queryDouble(sql, params);
	}

	private double leadToBookingRate(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder leadFilter = new StringBuilder();
		appendFilter(params, leadFilter, "l.location_id", locationId);
		appendOwnerRaw(params, leadFilter, "l.assigned_user_id", ownerUserId);
		StringBuilder bkFilter = bookingFilters(params, "b", locationId, professionalId, serviceId, null, ownerUserId,
				true);
		String sql = """
				with leads as (
				    select l.id
				    from lead l
				    where l.business_id = :businessId and l.active = true and l.created_at between :from and :to
				""" + leadFilter + """
				),
				booked as (
				    select distinct b.lead_id
				    from booking b
				    where b.business_id = :businessId
				      and b.lead_id is not null
				      and b.starts_at between :from and :to
				""" + bkFilter + """
				)
				select case when count(l.id) = 0 then 0.0
				            else round(count(b.lead_id) * 100.0 / count(l.id), 1) end
				from leads l
				left join booked b on b.lead_id = l.id
				""";
		return queryDouble(sql, params);
	}

	private long sumEstimatedRevenue(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				select coalesce(round(sum(s.price_base)), 0)::bigint
				from booking b
				join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				where b.business_id = :businessId
				  and b.starts_at between :from and :to
				""" + filter;
		return queryLong(sql, params);
	}

	private long sumApprovedPayments(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, null, ownerUserId,
				false);
		String sql = """
				select coalesce(round(sum(bp.amount)), 0)::bigint
				from booking_payment bp
				join booking b on b.id = bp.booking_id and b.business_id = bp.business_id
				where bp.business_id = :businessId
				  and bp.status = 'APPROVED'
				  and b.starts_at between :from and :to
				""" + filter;
		return queryLong(sql, params);
	}

	private long sumPendingBalances(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				with approved as (
				    select booking_id, coalesce(sum(amount), 0) as paid
				    from booking_payment
				    where business_id = :businessId and status = 'APPROVED'
				    group by booking_id
				)
				select coalesce(round(sum(greatest(s.price_base - coalesce(a.paid, 0), 0))), 0)::bigint
				from booking b
				join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
				left join approved a on a.booking_id = b.id
				where b.business_id = :businessId
				  and b.starts_at between :from and :to
				""" + filter;
		return queryLong(sql, params);
	}

	private long countNewCustomers(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, false);
		String sql = """
				select count(distinct c.id)
				from customer c
				left join booking b on b.customer_id = c.id and b.business_id = c.business_id and b.starts_at between :from and :to
				where c.business_id = :businessId
				  and c.active = true
				  and (c.created_at between :from and :to or b.id is not null)
				  and not exists (
				      select 1 from booking previous
				      where previous.business_id = c.business_id
				        and previous.customer_id = c.id
				        and previous.starts_at < :from
				  )
				"""
				+ filter;
		return queryLong(sql, params);
	}

	private long countRecurringCustomers(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				select count(*)
				from (
				    select b.customer_id
				    from booking b
				    where b.business_id = :businessId
				      and b.starts_at between :from and :to
				""" + filter + """
				    group by b.customer_id
				    having count(*) >= 2
				) recurring
				""";
		return queryLong(sql, params);
	}

	private double retentionRate(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId,
			String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
		MapSqlParameterSource params = baseParams(businessId, from, to);
		StringBuilder filter = bookingFilters(params, "b", locationId, professionalId, serviceId, bookingStatus,
				ownerUserId, true);
		String sql = """
				with period_customers as (
				    select distinct b.customer_id
				    from booking b
				    where b.business_id = :businessId
				      and b.starts_at between :from and :to
				""" + filter + """
				)
				select case when count(pc.customer_id) = 0 then 0.0
				            else round(count(pc.customer_id) filter (
				                where exists (
				                    select 1 from booking previous
				                    where previous.business_id = :businessId
				                      and previous.customer_id = pc.customer_id
				                      and previous.starts_at < :from
				                      and previous.status not in ('CANCELADA', 'CANCELADA_POR_CLIENTE', 'EXPIRADA')
				                )
				            ) * 100.0 / count(pc.customer_id), 1) end
				from period_customers pc
				""";
		return queryDouble(sql, params);
	}

	private Double computeVariation(long current, long previous) {
		if (previous == 0 && current == 0)
			return 0.0;
		if (previous == 0)
			return null;
		return Math.round((double) (current - previous) * 1000 / previous) / 10.0;
	}

	private ReportsFunnelStageResponse stage(String name, long count, Long previous, Long first) {
		Double fromPrev = (previous == null || previous == 0)
				? null
				: Math.round((double) count * 1000 / previous) / 10.0;
		Double fromFirst = (first == null || first == 0) ? null : Math.round((double) count * 1000 / first) / 10.0;
		return new ReportsFunnelStageResponse(name, count, fromPrev, fromFirst);
	}

	// --- Parameter helpers ---

	private MapSqlParameterSource baseParams(UUID businessId, OffsetDateTime from, OffsetDateTime to) {
		return new MapSqlParameterSource().addValue("businessId", businessId).addValue("from", from).addValue("to", to);
	}

	private void appendOwner(StringBuilder sql, MapSqlParameterSource params, String column, UUID ownerUserId) {
		if (ownerUserId == null)
			return;
		sql.append(" and ").append(column).append(" = :ownerUserId");
		params.addValue("ownerUserId", ownerUserId);
	}

	private void appendOwnerRaw(MapSqlParameterSource params, StringBuilder sql, String column, UUID ownerUserId) {
		if (ownerUserId == null)
			return;
		sql.append(" and ").append(column).append(" = :ownerUserId");
		params.addValue("ownerUserId", ownerUserId);
	}

	private void appendBookingStatusFilter(StringBuilder sql, MapSqlParameterSource params, String column,
			String bookingStatus) {
		if (bookingStatus == null || bookingStatus.isBlank() || "TODAS".equalsIgnoreCase(bookingStatus)) {
			return;
		}
		String canonical = BookingStateMachine.canonical(bookingStatus);
		if (BookingStateMachine.COMPLETED.equals(canonical)) {
			sql.append(" and ").append(column).append(" in (:bookingStatuses)");
			params.addValue("bookingStatuses", List.of("ATENDIDA", BookingStateMachine.COMPLETED));
			return;
		}
		if (BookingStateMachine.IN_SERVICE.equals(canonical)) {
			sql.append(" and ").append(column).append(" in (:bookingStatuses)");
			params.addValue("bookingStatuses", List.of("IN_PROGRESS", BookingStateMachine.IN_SERVICE));
			return;
		}
		sql.append(" and ").append(column).append(" = :bookingStatus");
		params.addValue("bookingStatus", canonical);
	}

	private StringBuilder bookingFilters(MapSqlParameterSource params, String alias, UUID locationId,
			UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, boolean excludeInactive) {
		StringBuilder sql = new StringBuilder();
		appendFilter(params, sql, alias + ".location_id", locationId);
		appendFilter(params, sql, alias + ".professional_id", professionalId);
		appendFilter(params, sql, alias + ".service_id", serviceId);
		appendOwner(sql, params, alias + ".assigned_user_id", ownerUserId);
		appendBookingStatusFilter(sql, params, alias + ".status", bookingStatus);
		if (excludeInactive) {
			sql.append(" and ").append(alias)
					.append(".status not in ('CANCELADA', 'CANCELADA_POR_CLIENTE', 'EXPIRADA')");
		}
		return sql;
	}

	private void appendFilter(MapSqlParameterSource params, StringBuilder sql, String column, UUID value) {
		if (value == null)
			return;
		String paramName = column.replace('.', '_');
		sql.append(" and ").append(column).append(" = :").append(paramName);
		params.addValue(paramName, value);
	}

	private MapSqlParameterSource copyParams(MapSqlParameterSource source, OffsetDateTime from, OffsetDateTime to,
			UUID businessId) {
		MapSqlParameterSource copy = new MapSqlParameterSource();
		copy.addValue("businessId", businessId);
		copy.addValue("from", from);
		copy.addValue("to", to);
		for (var entry : source.getValues().entrySet()) {
			if (!entry.getKey().equals("businessId") && !entry.getKey().equals("from")
					&& !entry.getKey().equals("to")) {
				copy.addValue(entry.getKey(), entry.getValue());
			}
		}
		return copy;
	}

	private long queryLong(String sql, MapSqlParameterSource params) {
		Number value = jdbcTemplate.queryForObject(sql, params, Number.class);
		return value == null ? 0 : value.longValue();
	}

	private double queryDouble(String sql, MapSqlParameterSource params) {
		Number value = jdbcTemplate.queryForObject(sql, params, Number.class);
		if (value == null) {
			return 0.0;
		}
		if (value instanceof BigDecimal decimal) {
			return decimal.doubleValue();
		}
		return value.doubleValue();
	}

	// --- Row mappers ---

	private RowMapper<ReportsChannelResponse> channelRowMapper() {
		return (rs, rowNum) -> new ReportsChannelResponse(rs.getString("channel_type"), rs.getLong("cnt"), 0.0);
	}

	private RowMapper<ReportsConversationPerformancePoint> conversationPerformanceRowMapper() {
		return (rs, rowNum) -> new ReportsConversationPerformancePoint(rs.getString("label"), rs.getLong("received"),
				rs.getLong("ai_answered"), rs.getLong("human_answered"),
				rs.getLong("received") - rs.getLong("ai_answered") - rs.getLong("human_answered"));
	}

	private RowMapper<ReportsAppointmentPerformancePoint> appointmentPerformanceRowMapper() {
		return (rs, rowNum) -> new ReportsAppointmentPerformancePoint(rs.getString("label"), rs.getLong("solicitada"),
				rs.getLong("confirmada"), rs.getLong("completada"), rs.getLong("cancelada"), rs.getLong("ausencia"));
	}

	private RowMapper<ReportsOccupancyResponse> occupancyRowMapper() {
		return (rs, rowNum) -> {
			long available = rs.getLong("available_minutes");
			long reserved = rs.getLong("reserved_minutes");
			Double occupancy = available == 0 ? null : Math.round((double) reserved * 1000 / available) / 10.0;
			return new ReportsOccupancyResponse(rs.getObject("id", UUID.class), rs.getString("name"), available,
					reserved, occupancy);
		};
	}

	private RowMapper<ReportsProspectRowResponse> prospectRowMapper() {
		return (rs, rowNum) -> {
			String phone = rs.getString("phone");
			String masked = phone != null && phone.length() >= 4
					? phone.substring(0, phone.length() - 4).replaceAll(".", "*") + phone.substring(phone.length() - 4)
					: phone;
			return new ReportsProspectRowResponse(rs.getObject("id", UUID.class), rs.getString("name"), masked,
					rs.getObject("last_contact", OffsetDateTime.class), rs.getString("stage"),
					rs.getString("responsible"), rs.getObject("next_appointment", OffsetDateTime.class),
					rs.getString("location_name"), rs.getString("service_interest"), rs.getString("attention_status"));
		};
	}
}
