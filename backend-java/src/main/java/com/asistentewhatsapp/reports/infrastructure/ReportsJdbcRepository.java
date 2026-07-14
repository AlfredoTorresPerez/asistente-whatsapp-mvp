package com.asistentewhatsapp.reports.infrastructure;

import com.asistentewhatsapp.reports.api.ReportsAppointmentDistributionPoint;
import com.asistentewhatsapp.reports.api.ReportsAppointmentPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsChannelResponse;
import com.asistentewhatsapp.reports.api.ReportsConversationPerformancePoint;
import com.asistentewhatsapp.reports.api.ReportsFunnelStageResponse;
import com.asistentewhatsapp.reports.api.ReportsKpiItem;
import com.asistentewhatsapp.reports.api.ReportsProspectRowResponse;
import com.asistentewhatsapp.reports.api.ReportsProspectsResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    public ReportsKpiItem buildConversationsKpi(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime previousFrom, OffsetDateTime previousTo) {
        long current = countConversations(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, from, to);
        long previous = countConversations(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, previousFrom, previousTo);
        Double variation = computeVariation(current, previous);
        return new ReportsKpiItem("Conversaciones", current, previous, variation,
                "Total de conversaciones creadas en el periodo.");
    }

    // --- KPI: Prospectos ---
    public ReportsKpiItem buildProspectsKpi(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime previousFrom, OffsetDateTime previousTo) {
        long current = countProspects(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, from, to);
        long previous = countProspects(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, previousFrom, previousTo);
        Double variation = computeVariation(current, previous);
        return new ReportsKpiItem("Prospectos", current, previous, variation,
                "Total de prospectos registrados en el periodo.");
    }

    // --- KPI: Citas creadas ---
    public ReportsKpiItem buildAppointmentsCreatedKpi(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime previousFrom, OffsetDateTime previousTo) {
        long current = countAppointments(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, from, to);
        long previous = countAppointments(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, previousFrom, previousTo);
        Double variation = computeVariation(current, previous);
        return new ReportsKpiItem("Citas creadas", current, previous, variation,
                "Total de citas creadas en el periodo (por fecha de creacion).");
    }

    // --- KPI: Citas confirmadas ---
    public ReportsKpiItem buildConfirmedAppointmentsKpi(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime previousFrom, OffsetDateTime previousTo) {
        long current = countConfirmedAppointments(businessId, locationId, professionalId, serviceId, ownerUserId, from, to);
        long previous = countConfirmedAppointments(businessId, locationId, professionalId, serviceId, ownerUserId, previousFrom, previousTo);
        Double variation = computeVariation(current, previous);
        return new ReportsKpiItem("Citas confirmadas", current, previous, variation,
                "Total de citas con estado CONFIRMADA en el periodo (por fecha de creacion).");
    }

    // --- KPI: Tasa de respuesta ---
    public ReportsKpiItem buildResponseRateKpi(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime previousFrom, OffsetDateTime previousTo) {
        double current = calculateResponseRate(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, from, to);
        double previous = calculateResponseRate(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, previousFrom, previousTo);
        Double variation;
        if (previous == 0) {
            variation = (current == 0) ? 0.0 : 100.0;
        } else {
            variation = current - previous;
        }
        return new ReportsKpiItem("Tasa de respuesta", Math.round(current), Math.round(previous), variation,
                "Porcentaje de conversaciones con al menos una respuesta.");
    }

    // --- KPI: Conversion a cita ---
    public ReportsKpiItem buildConversionRateKpi(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime previousFrom, OffsetDateTime previousTo) {
        double current = calculateConversionRate(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, from, to);
        double previous = calculateConversionRate(businessId, locationId, professionalId, serviceId, bookingStatus, ownerUserId, previousFrom, previousTo);
        Double variation;
        if (previous == 0) {
            variation = (current == 0) ? 0.0 : 100.0;
        } else {
            variation = current - previous;
        }
        return new ReportsKpiItem("Conversion a cita", Math.round(current), Math.round(previous), variation,
                "Porcentaje de prospectos que generaron al menos una cita.");
    }

    // --- Channel distribution ---
    public List<ReportsChannelResponse> loadChannelDistribution(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
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
        return channels.stream()
                .map(c -> new ReportsChannelResponse(c.channel(), c.count(), Math.round((double) c.count() * 1000 / total) / 10.0))
                .toList();
    }

    // --- Conversation performance (daily) with AI vs Human ---
    public List<ReportsConversationPerformancePoint> loadConversationPerformance(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
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

    // --- Appointment performance (daily by created_at) ---
    public List<ReportsAppointmentPerformancePoint> loadAppointmentPerformance(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource params = baseParams(businessId, from, to);
        StringBuilder filter = new StringBuilder();
        appendFilter(params, filter, "b.location_id", locationId);
        appendFilter(params, filter, "b.professional_id", professionalId);
        appendFilter(params, filter, "b.service_id", serviceId);
        appendFilter(params, filter, "b.assigned_user_id", ownerUserId);
        StringBuilder statusFilter = new StringBuilder();
        if (bookingStatus != null && !bookingStatus.isEmpty() && !bookingStatus.equals("TODAS")) {
            statusFilter.append(" and b.status = :bookingStatus");
            params.addValue("bookingStatus", bookingStatus);
        }

        String sql = """
                with days as (
                    select generate_series(cast(:from as date), cast(:to as date), interval '1 day')::date as day
                ),
                counts as (
                    select cast(b.created_at as date) as day,
                           count(*) filter (where b.status in ('SOLICITADA', 'PENDIENTE_CONFIRMACION', 'PENDIENTE_PAGO')) as solicitada,
                           count(*) filter (where b.status = 'CONFIRMADA') as confirmada,
                           count(*) filter (where b.status = 'ATENDIDA') as completada,
                           count(*) filter (where b.status in ('CANCELADA', 'CANCELADA_POR_CLIENTE')) as cancelada,
                           count(*) filter (where b.status = 'NO_ASISTE') as ausencia
                    from booking b
                    where b.business_id = :businessId
                      and b.created_at between :from and :to
                    """ + filter + statusFilter + """
                    group by cast(b.created_at as date)
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
    public List<ReportsAppointmentDistributionPoint> loadAppointmentDistribution(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource params = baseParams(businessId, from, to);
        StringBuilder filter = new StringBuilder();
        appendFilter(params, filter, "b.location_id", locationId);
        appendFilter(params, filter, "b.professional_id", professionalId);
        appendFilter(params, filter, "b.service_id", serviceId);
        appendFilter(params, filter, "b.assigned_user_id", ownerUserId);
        StringBuilder statusFilter = new StringBuilder();
        if (bookingStatus != null && !bookingStatus.isEmpty() && !bookingStatus.equals("TODAS")) {
            statusFilter.append(" and b.status = :bookingStatus");
            params.addValue("bookingStatus", bookingStatus);
        }

        String sql = """
                select b.status, count(*) as cnt
                from booking b
                where b.business_id = :businessId
                  and b.created_at between :from and :to
                """ + filter + statusFilter + """
                group by b.status
                order by cnt desc
                """;

        List<Map.Entry<String, Long>> raw = jdbcTemplate.query(sql, params, (rs, rn) ->
                Map.entry(rs.getString("status"), rs.getLong("cnt")));
        long total = raw.stream().mapToLong(Map.Entry::getValue).sum();
        if (total == 0) return List.of();

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
                case "ATENDIDA" -> "Atendida";
                case "NO_ASISTE" -> "No asiste";
                default -> e.getKey();
            };
            double pct = Math.round((double) e.getValue() * 1000 / total) / 10.0;
            return new ReportsAppointmentDistributionPoint(e.getKey(), label, e.getValue(), pct);
        }).toList();
    }

    // --- Conversion funnel ---
    public List<ReportsFunnelStageResponse> loadConversionFunnel(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
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
        if (bookingStatus != null && !bookingStatus.isEmpty() && !bookingStatus.equals("TODAS")) {
            bkStatusFilter.append(" and b.status = :bookingStatus");
            params.addValue("bookingStatus", bookingStatus);
        }

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
                    and b.created_at between :from and :to and b.status = 'ATENDIDA'
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

        if (result == null) return List.of();

        long conv = result.getOrDefault("conversations", 0L);
        long pros = result.getOrDefault("prospects", 0L);
        long wb = result.getOrDefault("with_booking", 0L);
        long conf = result.getOrDefault("confirmed", 0L);
        long comp = result.getOrDefault("completed", 0L);

        return List.of(
                stage("Conversaciones", conv, null, null),
                stage("Prospectos", pros, conv, conv),
                stage("Solicitudes de cita", wb, pros, conv),
                stage("Citas confirmadas", conf, wb, conv),
                stage("Citas completadas", comp, conf, conv)
        );
    }

    // --- Prospects detail (paginated) ---
    public ReportsProspectsResponse loadProspects(
            UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus,
            UUID ownerUserId, OffsetDateTime from, OffsetDateTime to,
            String search, int page, int size) {
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
        if (bookingStatus != null && !bookingStatus.isEmpty() && !bookingStatus.equals("TODAS")) {
            bkStatusFilter.append(" and b5.status = :bookingStatus");
            baseParams.addValue("bookingStatus", bookingStatus);
        }

        MapSqlParameterSource countParams = baseParams(businessId, from, to);
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
                          and b2.status not in ('ATENDIDA')) as next_appointment,
                       bl.name as location_name,
                       asv.name as service_interest,
                       case
                           when l.stage in ('WON') then 'Cerrado'
                           when l.stage in ('LOST') then 'Perdido'
                           when exists (select 1 from booking b3 where b3.lead_id = l.id and b3.business_id = l.business_id and b3.status = 'ATENDIDA') then 'Atendido'
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
                """ + filter + bkFilter + bkStatusFilter + """
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

    // --- Private helpers ---

    private long countConversations(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource params = baseParams(businessId, from, to);
        StringBuilder sql = new StringBuilder("select count(*) from conversation c where c.business_id = :businessId and c.created_at between :from and :to");
        appendFilter(params, sql, "c.location_id", locationId);
        appendOwner(sql, params, "c.assigned_user_id", ownerUserId);
        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count == null ? 0 : count;
    }

    private long countProspects(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource params = baseParams(businessId, from, to);
        StringBuilder sql = new StringBuilder("select count(*) from lead l where l.business_id = :businessId and l.active = true and l.created_at between :from and :to");
        appendFilter(params, sql, "l.location_id", locationId);
        appendOwner(sql, params, "l.assigned_user_id", ownerUserId);
        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count == null ? 0 : count;
    }

    private long countAppointments(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource params = baseParams(businessId, from, to);
        StringBuilder sql = new StringBuilder("select count(*) from booking b where b.business_id = :businessId and b.created_at between :from and :to");
        appendFilter(params, sql, "b.location_id", locationId);
        appendFilter(params, sql, "b.professional_id", professionalId);
        appendFilter(params, sql, "b.service_id", serviceId);
        appendOwner(sql, params, "b.assigned_user_id", ownerUserId);
        if (bookingStatus != null && !bookingStatus.isEmpty() && !bookingStatus.equals("TODAS")) {
            sql.append(" and b.status = :bookingStatus");
            params.addValue("bookingStatus", bookingStatus);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count == null ? 0 : count;
    }

    private long countConfirmedAppointments(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
        MapSqlParameterSource params = baseParams(businessId, from, to);
        StringBuilder sql = new StringBuilder("select count(*) from booking b where b.business_id = :businessId and b.created_at between :from and :to and b.status = 'CONFIRMADA'");
        appendFilter(params, sql, "b.location_id", locationId);
        appendFilter(params, sql, "b.professional_id", professionalId);
        appendFilter(params, sql, "b.service_id", serviceId);
        appendOwner(sql, params, "b.assigned_user_id", ownerUserId);
        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count == null ? 0 : count;
    }

    private double calculateResponseRate(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
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

    private double calculateConversionRate(UUID businessId, UUID locationId, UUID professionalId, UUID serviceId, String bookingStatus, UUID ownerUserId, OffsetDateTime from, OffsetDateTime to) {
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

    private Double computeVariation(long current, long previous) {
        if (previous == 0 && current == 0) return 0.0;
        if (previous == 0) return null;
        return Math.round((double) (current - previous) * 1000 / previous) / 10.0;
    }

    private ReportsFunnelStageResponse stage(String name, long count, Long previous, Long first) {
        Double fromPrev = (previous == null || previous == 0) ? null
                : Math.round((double) count * 1000 / previous) / 10.0;
        Double fromFirst = (first == null || first == 0) ? null
                : Math.round((double) count * 1000 / first) / 10.0;
        return new ReportsFunnelStageResponse(name, count, fromPrev, fromFirst);
    }

    // --- Parameter helpers ---

    private MapSqlParameterSource baseParams(UUID businessId, OffsetDateTime from, OffsetDateTime to) {
        return new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("from", from)
                .addValue("to", to);
    }

    private void appendOwner(StringBuilder sql, MapSqlParameterSource params, String column, UUID ownerUserId) {
        if (ownerUserId == null) return;
        sql.append(" and ").append(column).append(" = :ownerUserId");
        params.addValue("ownerUserId", ownerUserId);
    }

    private void appendOwnerRaw(MapSqlParameterSource params, StringBuilder sql, String column, UUID ownerUserId) {
        if (ownerUserId == null) return;
        sql.append(" and ").append(column).append(" = :ownerUserId");
        params.addValue("ownerUserId", ownerUserId);
    }

    private void appendFilter(MapSqlParameterSource params, StringBuilder sql, String column, UUID value) {
        if (value == null) return;
        String paramName = column.replace('.', '_');
        sql.append(" and ").append(column).append(" = :").append(paramName);
        params.addValue(paramName, value);
    }

    private MapSqlParameterSource copyParams(MapSqlParameterSource source, OffsetDateTime from, OffsetDateTime to, UUID businessId) {
        MapSqlParameterSource copy = new MapSqlParameterSource();
        copy.addValue("businessId", businessId);
        copy.addValue("from", from);
        copy.addValue("to", to);
        for (var entry : source.getValues().entrySet()) {
            if (!entry.getKey().equals("businessId") && !entry.getKey().equals("from") && !entry.getKey().equals("to")) {
                copy.addValue(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    // --- Row mappers ---

    private RowMapper<ReportsChannelResponse> channelRowMapper() {
        return (rs, rowNum) -> new ReportsChannelResponse(
                rs.getString("channel_type"), rs.getLong("cnt"), 0.0);
    }

    private RowMapper<ReportsConversationPerformancePoint> conversationPerformanceRowMapper() {
        return (rs, rowNum) -> new ReportsConversationPerformancePoint(
                rs.getString("label"),
                rs.getLong("received"),
                rs.getLong("ai_answered"),
                rs.getLong("human_answered"),
                rs.getLong("received") - rs.getLong("ai_answered") - rs.getLong("human_answered"));
    }

    private RowMapper<ReportsAppointmentPerformancePoint> appointmentPerformanceRowMapper() {
        return (rs, rowNum) -> new ReportsAppointmentPerformancePoint(
                rs.getString("label"),
                rs.getLong("solicitada"),
                rs.getLong("confirmada"),
                rs.getLong("completada"),
                rs.getLong("cancelada"),
                rs.getLong("ausencia"));
    }

    private RowMapper<ReportsProspectRowResponse> prospectRowMapper() {
        return (rs, rowNum) -> {
            String phone = rs.getString("phone");
            String masked = phone != null && phone.length() >= 4
                    ? phone.substring(0, phone.length() - 4).replaceAll(".", "*") + phone.substring(phone.length() - 4)
                    : phone;
            return new ReportsProspectRowResponse(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    masked,
                    rs.getObject("last_contact", OffsetDateTime.class),
                    rs.getString("stage"),
                    rs.getString("responsible"),
                    rs.getObject("next_appointment", OffsetDateTime.class),
                    rs.getString("location_name"),
                    rs.getString("service_interest"),
                    rs.getString("attention_status"));
        };
    }
}
