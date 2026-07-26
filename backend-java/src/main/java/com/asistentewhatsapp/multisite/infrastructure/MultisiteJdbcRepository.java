package com.asistentewhatsapp.multisite.infrastructure;

import com.asistentewhatsapp.multisite.api.MultisiteCatalogAvailabilityResponse;
import com.asistentewhatsapp.multisite.api.MultisiteChannelResponse;
import com.asistentewhatsapp.multisite.api.MultisiteLocationSummaryResponse;
import com.asistentewhatsapp.multisite.api.MultisiteProfessionalResponse;
import com.asistentewhatsapp.multisite.api.ProfessionalLocationAssignmentResponse;
import com.asistentewhatsapp.multisite.api.ProfessionalScheduleResponse;
import com.asistentewhatsapp.multisite.api.UpdateChannelLocationRequest;
import com.asistentewhatsapp.multisite.api.UpsertCatalogAvailabilityRequest;
import com.asistentewhatsapp.multisite.api.UpsertProfessionalScheduleRequest;
import com.asistentewhatsapp.multisite.api.UpsertUserLocationAccessRequest;
import com.asistentewhatsapp.multisite.api.UserLocationAccessResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MultisiteJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MultisiteJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MultisiteLocationSummaryResponse> locationSummary(UUID businessId) {
        return jdbcTemplate.query(
                """
                        select
                            bl.id as location_id,
                            bl.code as location_code,
                            bl.name as location_name,
                            bl.active,
                            count(distinct c.id) as conversations,
                            count(distinct l.id) as leads,
                            count(distinct b.id) as bookings,
                            count(distinct o.id) as orders,
                            count(distinct pls.product_service_id) as products_with_stock,
                            count(distinct apl.professional_id) as professionals
                        from business_location bl
                        left join conversation c on c.business_id = bl.business_id and c.location_id = bl.id
                        left join lead l on l.business_id = bl.business_id and l.location_id = bl.id
                        left join booking b on b.business_id = bl.business_id and b.location_id = bl.id
                        left join order_request o on o.business_id = bl.business_id and o.location_id = bl.id
                        left join product_location_stock pls on pls.business_id = bl.business_id and pls.location_id = bl.id and pls.active = true
                        left join aesthetic_professional_location apl on apl.business_id = bl.business_id and apl.location_id = bl.id and apl.active = true
                        where bl.business_id = :businessId
                        group by bl.id, bl.code, bl.name, bl.active
                        order by bl.active desc, bl.name asc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId),
                (rs, rowNum) -> new MultisiteLocationSummaryResponse(
                        rs.getObject("location_id", UUID.class),
                        rs.getString("location_code"),
                        rs.getString("location_name"),
                        rs.getBoolean("active"),
                        rs.getLong("conversations"),
                        rs.getLong("leads"),
                        rs.getLong("bookings"),
                        rs.getLong("orders"),
                        rs.getLong("products_with_stock"),
                        rs.getLong("professionals")));
    }

    public List<MultisiteCatalogAvailabilityResponse> catalogAvailability(UUID businessId, UUID locationId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("locationId", locationId);
        return jdbcTemplate.query(
                """
                        select
                            ps.id as item_id,
                            ps.type,
                            ps.name,
                            ps.sku,
                            ps.price as base_price,
                            bl.id as location_id,
                            bl.name as location_name,
                            coalesce(psl.active, false) as available,
                            psl.price_override,
                            psl.duration_override_minutes,
                            psl.stock_enabled,
                            pls.stock_quantity,
                            pls.stock_minimum
                        from product_service ps
                        join business_location bl on bl.business_id = ps.business_id
                        left join product_service_location psl
                          on psl.business_id = ps.business_id
                         and psl.product_service_id = ps.id
                         and psl.location_id = bl.id
                        left join product_location_stock pls
                          on pls.business_id = ps.business_id
                         and pls.product_service_id = ps.id
                         and pls.location_id = bl.id
                        where ps.business_id = :businessId
                          and ps.type = 'SERVICE'
                          and (cast(:locationId as uuid) is null or bl.id = :locationId)
                        order by bl.name asc, ps.name asc
                        """,
                parameters,
                (rs, rowNum) -> new MultisiteCatalogAvailabilityResponse(
                        rs.getObject("item_id", UUID.class),
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getBigDecimal("base_price"),
                        rs.getObject("location_id", UUID.class),
                        rs.getString("location_name"),
                        rs.getBoolean("available"),
                        rs.getBigDecimal("price_override"),
                        (Integer) rs.getObject("duration_override_minutes"),
                        (Boolean) rs.getObject("stock_enabled"),
                        (Integer) rs.getObject("stock_quantity"),
                        (Integer) rs.getObject("stock_minimum")));
    }

    public void upsertCatalogAvailability(UUID businessId, UpsertCatalogAvailabilityRequest request) {
        assertLocationBelongsToBusiness(businessId, request.locationId());
        assertProductServiceBelongsToBusiness(businessId, request.productServiceId());
        jdbcTemplate.update(
                """
                        insert into product_service_location (
                            id, business_id, product_service_id, location_id, active, price_override,
                            duration_override_minutes, stock_enabled
                        ) values (
                            gen_random_uuid(), :businessId, :productServiceId, :locationId, :active, :priceOverride,
                            :durationOverrideMinutes, :stockEnabled
                        )
                        on conflict (business_id, product_service_id, location_id) do update
                        set active = excluded.active,
                            price_override = excluded.price_override,
                            duration_override_minutes = excluded.duration_override_minutes,
                            stock_enabled = excluded.stock_enabled,
                            updated_at = current_timestamp
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("productServiceId", request.productServiceId())
                        .addValue("locationId", request.locationId())
                        .addValue("active", request.active() == null || request.active())
                        .addValue("priceOverride", request.priceOverride())
                        .addValue("durationOverrideMinutes", request.durationOverrideMinutes())
                        .addValue("stockEnabled", request.stockEnabled() != null && request.stockEnabled()));
        if (request.stockQuantity() != null || request.stockMinimum() != null) {
            jdbcTemplate.update(
                    """
                            insert into product_location_stock (
                                id, business_id, product_service_id, location_id, stock_quantity, stock_minimum, active
                            ) values (
                                gen_random_uuid(), :businessId, :productServiceId, :locationId, :stockQuantity, :stockMinimum, true
                            )
                            on conflict (business_id, product_service_id, location_id) do update
                            set stock_quantity = excluded.stock_quantity,
                                stock_minimum = excluded.stock_minimum,
                                active = true,
                                updated_at = current_timestamp
                            """,
                    new MapSqlParameterSource()
                            .addValue("businessId", businessId)
                            .addValue("productServiceId", request.productServiceId())
                            .addValue("locationId", request.locationId())
                            .addValue("stockQuantity", request.stockQuantity() == null ? 0 : request.stockQuantity())
                            .addValue("stockMinimum", request.stockMinimum() == null ? 0 : request.stockMinimum()));
        }
    }

    public List<MultisiteProfessionalResponse> professionals(UUID businessId) {
        List<ProfessionalFlatRow> rows = jdbcTemplate.query(
                """
                        select
                            ap.id as professional_id,
                            ap.full_name,
                            ap.specialty,
                            ap.active as professional_active,
                            bl.id as location_id,
                            bl.name as location_name,
                            coalesce(apl.active, false) as location_active
                        from aesthetic_professional ap
                        left join aesthetic_professional_location apl
                          on apl.business_id = ap.business_id
                         and apl.professional_id = ap.id
                        left join business_location bl
                          on bl.business_id = ap.business_id
                         and bl.id = apl.location_id
                        where ap.business_id = :businessId
                        order by ap.full_name asc, bl.name asc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId),
                (rs, rowNum) -> new ProfessionalFlatRow(
                        rs.getObject("professional_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getString("specialty"),
                        rs.getBoolean("professional_active"),
                        rs.getObject("location_id", UUID.class),
                        rs.getString("location_name"),
                        rs.getBoolean("location_active")));
        Map<UUID, MultisiteProfessionalBuilder> grouped = new LinkedHashMap<>();
        for (ProfessionalFlatRow row : rows) {
            grouped.computeIfAbsent(row.professionalId(), key -> new MultisiteProfessionalBuilder(row))
                    .add(row);
        }
        return grouped.values().stream().map(MultisiteProfessionalBuilder::build).toList();
    }

    public List<ProfessionalScheduleResponse> professionalSchedules(UUID businessId, UUID locationId) {
        return jdbcTemplate.query(
                """
                        select
                            aph.id,
                            aph.professional_id,
                            ap.full_name as professional_name,
                            aph.location_id,
                            bl.name as location_name,
                            aph.day_of_week,
                            aph.start_time,
                            aph.end_time,
                            aph.active
                        from agenda_professional_hours aph
                        join aesthetic_professional ap on ap.id = aph.professional_id and ap.business_id = aph.business_id
                        join business_location bl on bl.id = aph.location_id and bl.business_id = aph.business_id
                        where aph.business_id = :businessId
                          and (cast(:locationId as uuid) is null or aph.location_id = :locationId)
                        order by bl.name asc, ap.full_name asc, aph.day_of_week asc, aph.start_time asc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId),
                (rs, rowNum) -> new ProfessionalScheduleResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("professional_id", UUID.class),
                        rs.getString("professional_name"),
                        rs.getObject("location_id", UUID.class),
                        rs.getString("location_name"),
                        rs.getInt("day_of_week"),
                        rs.getObject("start_time", Time.class).toLocalTime(),
                        rs.getObject("end_time", Time.class).toLocalTime(),
                        rs.getBoolean("active")));
    }

    public UUID upsertProfessionalSchedule(UUID businessId, UpsertProfessionalScheduleRequest request) {
        assertLocationBelongsToBusiness(businessId, request.locationId());
        assertProfessionalBelongsToBusiness(businessId, request.professionalId());
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into aesthetic_professional_location (id, business_id, professional_id, location_id, active)
                        values (gen_random_uuid(), :businessId, :professionalId, :locationId, true)
                        on conflict (business_id, professional_id, location_id) do update
                        set active = true, updated_at = current_timestamp
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("professionalId", request.professionalId())
                        .addValue("locationId", request.locationId()));
        jdbcTemplate.update(
                """
                        insert into agenda_professional_hours (
                            id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active
                        ) values (
                            :id, :businessId, :professionalId, :locationId, :dayOfWeek, :startTime, :endTime, :active
                        )
                        on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do update
                        set active = excluded.active,
                            updated_at = current_timestamp
                        """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("businessId", businessId)
                        .addValue("professionalId", request.professionalId())
                        .addValue("locationId", request.locationId())
                        .addValue("dayOfWeek", request.dayOfWeek())
                        .addValue("startTime", Time.valueOf(request.startTime()))
                        .addValue("endTime", Time.valueOf(request.endTime()))
                        .addValue("active", request.active() == null || request.active()));
        return id;
    }

    public List<UserLocationAccessResponse> userLocationAccess(UUID businessId) {
        return jdbcTemplate.query(
                """
                        select
                            ua.id as user_id,
                            concat(ua.first_name, ' ', ua.last_name) as user_name,
                            ua.email,
                            bl.id as location_id,
                            bl.name as location_name,
                            coalesce(ula.role_scope, 'VIEWER') as role_scope,
                            coalesce(ula.can_view_conversations, false) as can_view_conversations,
                            coalesce(ula.can_manage_bookings, false) as can_manage_bookings,
                            coalesce(ula.can_manage_orders, false) as can_manage_orders,
                            coalesce(ula.can_manage_catalog, false) as can_manage_catalog,
                            coalesce(ula.can_view_reports, false) as can_view_reports,
                            coalesce(ula.active, false) as active
                        from user_account ua
                        cross join business_location bl
                        left join user_location_access ula
                          on ula.business_id = ua.business_id
                         and ula.user_id = ua.id
                         and ula.location_id = bl.id
                        where ua.business_id = :businessId
                        order by ua.email asc, bl.name asc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId),
                (rs, rowNum) -> new UserLocationAccessResponse(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("user_name"),
                        rs.getString("email"),
                        rs.getObject("location_id", UUID.class),
                        rs.getString("location_name"),
                        rs.getString("role_scope"),
                        rs.getBoolean("can_view_conversations"),
                        rs.getBoolean("can_manage_bookings"),
                        rs.getBoolean("can_manage_orders"),
                        rs.getBoolean("can_manage_catalog"),
                        rs.getBoolean("can_view_reports"),
                        rs.getBoolean("active")));
    }

    public void upsertUserLocationAccess(UUID businessId, UpsertUserLocationAccessRequest request) {
        assertLocationBelongsToBusiness(businessId, request.locationId());
        assertUserBelongsToBusiness(businessId, request.userId());
        jdbcTemplate.update(
                """
                        insert into user_location_access (
                            id, business_id, user_id, location_id, role_scope,
                            can_view_conversations, can_manage_bookings, can_manage_orders, can_manage_catalog, can_view_reports, active
                        ) values (
                            gen_random_uuid(), :businessId, :userId, :locationId, :roleScope,
                            :canViewConversations, :canManageBookings, :canManageOrders, :canManageCatalog, :canViewReports, :active
                        )
                        on conflict (business_id, user_id, location_id) do update
                        set role_scope = excluded.role_scope,
                            can_view_conversations = excluded.can_view_conversations,
                            can_manage_bookings = excluded.can_manage_bookings,
                            can_manage_orders = excluded.can_manage_orders,
                            can_manage_catalog = excluded.can_manage_catalog,
                            can_view_reports = excluded.can_view_reports,
                            active = excluded.active,
                            updated_at = current_timestamp
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("userId", request.userId())
                        .addValue("locationId", request.locationId())
                        .addValue("roleScope", request.roleScope() == null ? "OPERATOR" : request.roleScope())
                        .addValue("canViewConversations", request.canViewConversations() == null || request.canViewConversations())
                        .addValue("canManageBookings", request.canManageBookings() == null || request.canManageBookings())
                        .addValue("canManageOrders", request.canManageOrders() == null || request.canManageOrders())
                        .addValue("canManageCatalog", request.canManageCatalog() != null && request.canManageCatalog())
                        .addValue("canViewReports", request.canViewReports() == null || request.canViewReports())
                        .addValue("active", request.active() == null || request.active()));
    }

    public List<MultisiteChannelResponse> channels(UUID businessId) {
        return jdbcTemplate.query(
                """
                        select
                            ca.id,
                            ca.channel_type,
                            ca.provider_name,
                            ca.status,
                            ca.phone_number,
                            ca.location_id,
                            bl.name as location_name,
                            ca.routing_mode,
                            ca.active
                        from channel_account ca
                        left join business_location bl on bl.id = ca.location_id and bl.business_id = ca.business_id
                        where ca.business_id = :businessId
                        order by ca.location_id nulls first, ca.channel_type asc
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId),
                (rs, rowNum) -> new MultisiteChannelResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("channel_type"),
                        rs.getString("provider_name"),
                        rs.getString("status"),
                        rs.getString("phone_number"),
                        rs.getObject("location_id", UUID.class),
                        rs.getString("location_name"),
                        rs.getString("routing_mode"),
                        rs.getBoolean("active")));
    }

    public void updateChannelLocation(UUID businessId, UUID channelId, UpdateChannelLocationRequest request) {
        if (request.locationId() != null) {
            assertLocationBelongsToBusiness(businessId, request.locationId());
        }
        int updated = jdbcTemplate.update(
                """
                        update channel_account
                        set location_id = :locationId,
                            routing_mode = :routingMode,
                            updated_at = current_timestamp
                        where business_id = :businessId
                          and id = :channelId
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("channelId", channelId)
                        .addValue("locationId", request.locationId())
                        .addValue("routingMode", request.routingMode() == null ? (request.locationId() == null ? "CENTRALIZED" : "LOCATION_SPECIFIC") : request.routingMode()));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro el canal solicitado.");
        }
    }

    private void assertLocationBelongsToBusiness(UUID businessId, UUID locationId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from business_location where business_id = :businessId and id = :locationId",
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("locationId", locationId),
                Long.class);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("La sede no pertenece al negocio autenticado.");
        }
    }

    private void assertProductServiceBelongsToBusiness(UUID businessId, UUID productServiceId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from product_service where business_id = :businessId and id = :productServiceId",
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("productServiceId", productServiceId),
                Long.class);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("El producto o servicio no pertenece al negocio autenticado.");
        }
    }

    private void assertProfessionalBelongsToBusiness(UUID businessId, UUID professionalId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from aesthetic_professional where business_id = :businessId and id = :professionalId",
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("professionalId", professionalId),
                Long.class);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("El profesional no pertenece al negocio autenticado.");
        }
    }

    private void assertUserBelongsToBusiness(UUID businessId, UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from user_account where business_id = :businessId and id = :userId",
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("userId", userId),
                Long.class);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("El usuario no pertenece al negocio autenticado.");
        }
    }

    private record ProfessionalFlatRow(
            UUID professionalId,
            String fullName,
            String specialty,
            boolean active,
            UUID locationId,
            String locationName,
            boolean locationActive) {
    }

    private static final class MultisiteProfessionalBuilder {
        private final UUID id;
        private final String fullName;
        private final String specialty;
        private final boolean active;
        private final List<ProfessionalLocationAssignmentResponse> locations = new ArrayList<>();

        private MultisiteProfessionalBuilder(ProfessionalFlatRow row) {
            this.id = row.professionalId();
            this.fullName = row.fullName();
            this.specialty = row.specialty();
            this.active = row.active();
        }

        private MultisiteProfessionalBuilder add(ProfessionalFlatRow row) {
            if (row.locationId() != null) {
                locations.add(new ProfessionalLocationAssignmentResponse(row.locationId(), row.locationName(), row.locationActive()));
            }
            return this;
        }

        private MultisiteProfessionalResponse build() {
            return new MultisiteProfessionalResponse(id, fullName, specialty, active, locations);
        }
    }
}
