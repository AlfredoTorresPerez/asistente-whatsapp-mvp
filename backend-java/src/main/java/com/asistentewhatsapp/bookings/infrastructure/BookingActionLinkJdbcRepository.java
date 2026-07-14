package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingActionLinkJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookingActionLinkJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ActionBookingRecord findBooking(UUID businessId, UUID bookingId) {
        List<ActionBookingRecord> items = queryBooking(
                "where b.business_id = :businessId and b.id = :bookingId",
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la cita solicitada.");
        }
        return items.getFirst();
    }

    public ActionBookingRecord findBookingForUpdate(UUID businessId, UUID bookingId) {
        List<ActionBookingRecord> items = queryBooking(
                "where b.business_id = :businessId and b.id = :bookingId for update of b",
                new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId));
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la cita solicitada.");
        }
        return items.getFirst();
    }

    public void invalidateActiveRescheduleLinks(UUID businessId, UUID bookingId) {
        jdbcTemplate.update(
                """
                        update booking_reschedule_link
                        set status = 'CANCELLED', updated_at = current_timestamp
                        where business_id = :businessId
                          and booking_id = :bookingId
                          and status = 'ACTIVE'
                        """,
                params(businessId, bookingId));
    }

    public void invalidateActiveCancellationLinks(UUID businessId, UUID bookingId) {
        jdbcTemplate.update(
                """
                        update booking_cancellation_link
                        set status = 'CANCELLED', updated_at = current_timestamp
                        where business_id = :businessId
                          and booking_id = :bookingId
                          and status = 'ACTIVE'
                        """,
                params(businessId, bookingId));
    }

    public UUID insertRescheduleLink(UUID businessId, UUID bookingId, String tokenHash, String publicUrl,
            OffsetDateTime proposedStartsAt, OffsetDateTime proposedEndsAt, UUID locationId, UUID serviceId,
            UUID professionalId, UUID roomId, OffsetDateTime expiresAt, String reason, UUID actorUserId,
            String createdByChannel) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "update booking_reschedule_link set status = 'EXPIRED', expires_at = now() where business_id = :businessId and booking_id = :bookingId",
                Map.of("businessId", businessId, "bookingId", bookingId));
        jdbcTemplate.update(
                """
                        insert into booking_reschedule_link (
                            id, business_id, booking_id, token_hash, reschedule_url, proposed_starts_at,
                            proposed_ends_at, proposed_location_id, proposed_service_id, proposed_professional_id,
                            proposed_room_id, expires_at, reason, created_by_user_id, created_by_channel
                        ) values (
                            :id, :businessId, :bookingId, :tokenHash, :publicUrl, :proposedStartsAt,
                            :proposedEndsAt, :locationId, :serviceId, :professionalId,
                            :roomId, :expiresAt, :reason, :actorUserId, :createdByChannel
                        )
                        """,
                params(businessId, bookingId)
                        .addValue("id", id)
                        .addValue("tokenHash", tokenHash)
                        .addValue("publicUrl", publicUrl)
                        .addValue("proposedStartsAt", proposedStartsAt)
                        .addValue("proposedEndsAt", proposedEndsAt)
                        .addValue("locationId", locationId)
                        .addValue("serviceId", serviceId)
                        .addValue("professionalId", professionalId)
                        .addValue("roomId", roomId)
                        .addValue("expiresAt", expiresAt)
                        .addValue("reason", reason)
                        .addValue("actorUserId", actorUserId)
                        .addValue("createdByChannel", createdByChannel));
        return id;
    }

    public UUID insertCancellationLink(UUID businessId, UUID bookingId, String tokenHash, String publicUrl,
            OffsetDateTime expiresAt, String reason, UUID actorUserId, String createdByChannel) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "update booking_cancellation_link set status = 'EXPIRED', expires_at = now() where business_id = :businessId and booking_id = :bookingId",
                Map.of("businessId", businessId, "bookingId", bookingId));
        jdbcTemplate.update(
                """
                        insert into booking_cancellation_link (
                            id, business_id, booking_id, token_hash, cancellation_url, expires_at,
                            cancellation_reason, created_by_user_id, created_by_channel
                        ) values (
                            :id, :businessId, :bookingId, :tokenHash, :publicUrl, :expiresAt,
                            :reason, :actorUserId, :createdByChannel
                        )
                        """,
                params(businessId, bookingId)
                        .addValue("id", id)
                        .addValue("tokenHash", tokenHash)
                        .addValue("publicUrl", publicUrl)
                        .addValue("expiresAt", expiresAt)
                        .addValue("reason", reason)
                        .addValue("actorUserId", actorUserId)
                        .addValue("createdByChannel", createdByChannel));
        jdbcTemplate.update(
                """
                        update booking
                        set cancellation_requested_at = coalesce(cancellation_requested_at, current_timestamp)
                        where business_id = :businessId and id = :bookingId
                        """,
                params(businessId, bookingId));
        return id;
    }

    public RescheduleLinkRecord findRescheduleByTokenHash(String tokenHash, boolean forUpdate) {
        String lock = forUpdate ? " for update of l, b" : "";
        List<RescheduleLinkRecord> items = jdbcTemplate.query(rescheduleSql("where l.token_hash = :tokenHash" + lock),
                new MapSqlParameterSource().addValue("tokenHash", tokenHash),
                rescheduleMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("El enlace de reprogramacion no existe o fue invalidado.");
        }
        return items.getFirst();
    }

    public CancellationLinkRecord findCancellationByTokenHash(String tokenHash, boolean forUpdate) {
        String lock = forUpdate ? " for update of l, b" : "";
        List<CancellationLinkRecord> items = jdbcTemplate.query(cancellationSql("where l.token_hash = :tokenHash" + lock),
                new MapSqlParameterSource().addValue("tokenHash", tokenHash),
                cancellationMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("El enlace de cancelacion no existe o fue invalidado.");
        }
        return items.getFirst();
    }

    public void markRescheduleUsed(UUID linkId) {
        jdbcTemplate.update(
                "update booking_reschedule_link set status = 'USED', used_at = coalesce(used_at, current_timestamp), updated_at = current_timestamp where id = :linkId and status = 'ACTIVE'",
                new MapSqlParameterSource().addValue("linkId", linkId));
    }

    public void markRescheduleRejected(UUID linkId) {
        jdbcTemplate.update(
                "update booking_reschedule_link set status = 'REJECTED', rejected_at = coalesce(rejected_at, current_timestamp), updated_at = current_timestamp where id = :linkId and status = 'ACTIVE'",
                new MapSqlParameterSource().addValue("linkId", linkId));
    }

    public void markCancellationUsed(UUID linkId, String reason) {
        jdbcTemplate.update(
                """
                        update booking_cancellation_link
                        set status = 'USED',
                            cancellation_reason = coalesce(:reason, cancellation_reason),
                            used_at = coalesce(used_at, current_timestamp),
                            updated_at = current_timestamp
                        where id = :linkId and status = 'ACTIVE'
                        """,
                new MapSqlParameterSource().addValue("linkId", linkId).addValue("reason", reason));
    }

    public void markExpiredReschedule(UUID linkId) {
        jdbcTemplate.update(
                "update booking_reschedule_link set status = 'EXPIRED', updated_at = current_timestamp where id = :linkId and status = 'ACTIVE'",
                new MapSqlParameterSource().addValue("linkId", linkId));
    }

    public void markExpiredCancellation(UUID linkId) {
        jdbcTemplate.update(
                "update booking_cancellation_link set status = 'EXPIRED', updated_at = current_timestamp where id = :linkId and status = 'ACTIVE'",
                new MapSqlParameterSource().addValue("linkId", linkId));
    }

    private List<ActionBookingRecord> queryBooking(String whereClause, MapSqlParameterSource params) {
        return jdbcTemplate.query(
                """
                        select
                            b.id as booking_id, b.business_id, b.subject, b.status as booking_status,
                            b.starts_at, coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
                            b.duration_minutes, b.location_id, coalesce(bl.name, b.location) as location_name,
                            b.service_id, s.name as service_name, b.professional_id, p.full_name as professional_name,
                            b.room_id, r.name as room_name, b.conversation_id, b.reschedule_count,
                            c.display_name as customer_name, c.phone as customer_phone, c.email as customer_email
                        from booking b
                        join customer c on c.id = b.customer_id and c.business_id = b.business_id
                        left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
                        left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
                        left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
                        left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
                        """ + whereClause,
                params,
                bookingMapper());
    }

    private String rescheduleSql(String whereClause) {
        return """
                select
                    l.id as link_id, l.business_id, l.booking_id, l.status as link_status, l.reschedule_url,
                    l.proposed_starts_at, l.proposed_ends_at, l.proposed_location_id, l.proposed_service_id,
                    l.proposed_professional_id, l.proposed_room_id, l.expires_at, l.used_at, l.reason,
                    b.subject, b.status as booking_status, b.starts_at as current_starts_at,
                    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as current_ends_at,
                    coalesce(current_location.name, b.location) as current_location_name,
                    current_service.name as current_service_name,
                    proposed_service.name as proposed_service_name,
                    current_professional.full_name as current_professional_name,
                    proposed_professional.full_name as proposed_professional_name,
                    current_room.name as current_room_name,
                    proposed_room.name as proposed_room_name,
                    proposed_location.name as proposed_location_name,
                    c.display_name as customer_name, c.phone as customer_phone, c.email as customer_email
                from booking_reschedule_link l
                join booking b on b.id = l.booking_id and b.business_id = l.business_id
                join customer c on c.id = b.customer_id and c.business_id = b.business_id
                left join business_location current_location on current_location.id = b.location_id and current_location.business_id = b.business_id
                left join business_location proposed_location on proposed_location.id = l.proposed_location_id and proposed_location.business_id = l.business_id
                left join aesthetic_service current_service on current_service.id = b.service_id and current_service.business_id = b.business_id
                left join aesthetic_service proposed_service on proposed_service.id = l.proposed_service_id and proposed_service.business_id = l.business_id
                left join aesthetic_professional current_professional on current_professional.id = b.professional_id and current_professional.business_id = b.business_id
                left join aesthetic_professional proposed_professional on proposed_professional.id = l.proposed_professional_id and proposed_professional.business_id = l.business_id
                left join agenda_room current_room on current_room.id = b.room_id and current_room.business_id = b.business_id
                left join agenda_room proposed_room on proposed_room.id = l.proposed_room_id and proposed_room.business_id = l.business_id
                """ + whereClause;
    }

    private String cancellationSql(String whereClause) {
        return """
                select
                    l.id as link_id, l.business_id, l.booking_id, l.status as link_status, l.cancellation_url,
                    l.expires_at, l.used_at, l.cancellation_reason,
                    b.subject, b.status as booking_status, b.starts_at,
                    coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) as ends_at,
                    coalesce(bl.name, b.location) as location_name, s.name as service_name,
                    p.full_name as professional_name, r.name as room_name,
                    c.display_name as customer_name, c.phone as customer_phone, c.email as customer_email
                from booking_cancellation_link l
                join booking b on b.id = l.booking_id and b.business_id = l.business_id
                join customer c on c.id = b.customer_id and c.business_id = b.business_id
                left join business_location bl on bl.id = b.location_id and bl.business_id = b.business_id
                left join aesthetic_service s on s.id = b.service_id and s.business_id = b.business_id
                left join aesthetic_professional p on p.id = b.professional_id and p.business_id = b.business_id
                left join agenda_room r on r.id = b.room_id and r.business_id = b.business_id
                """ + whereClause;
    }

    private MapSqlParameterSource params(UUID businessId, UUID bookingId) {
        return new MapSqlParameterSource().addValue("businessId", businessId).addValue("bookingId", bookingId);
    }

    private RowMapper<ActionBookingRecord> bookingMapper() {
        return (rs, rowNum) -> new ActionBookingRecord(
                rs.getObject("booking_id", UUID.class),
                rs.getObject("business_id", UUID.class),
                rs.getString("subject"),
                rs.getString("booking_status"),
                rs.getObject("starts_at", OffsetDateTime.class),
                rs.getObject("ends_at", OffsetDateTime.class),
                rs.getInt("duration_minutes"),
                rs.getObject("location_id", UUID.class),
                rs.getString("location_name"),
                rs.getObject("service_id", UUID.class),
                rs.getString("service_name"),
                rs.getObject("professional_id", UUID.class),
                rs.getString("professional_name"),
                rs.getObject("room_id", UUID.class),
                rs.getString("room_name"),
                rs.getObject("conversation_id", UUID.class),
                rs.getInt("reschedule_count"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("customer_email"));
    }

    private RowMapper<RescheduleLinkRecord> rescheduleMapper() {
        return (rs, rowNum) -> new RescheduleLinkRecord(
                rs.getObject("link_id", UUID.class),
                rs.getObject("business_id", UUID.class),
                rs.getObject("booking_id", UUID.class),
                rs.getString("link_status"),
                rs.getString("reschedule_url"),
                rs.getObject("proposed_starts_at", OffsetDateTime.class),
                rs.getObject("proposed_ends_at", OffsetDateTime.class),
                rs.getObject("proposed_location_id", UUID.class),
                rs.getObject("proposed_service_id", UUID.class),
                rs.getObject("proposed_professional_id", UUID.class),
                rs.getObject("proposed_room_id", UUID.class),
                rs.getObject("expires_at", OffsetDateTime.class),
                rs.getObject("used_at", OffsetDateTime.class),
                rs.getString("reason"),
                rs.getString("subject"),
                rs.getString("booking_status"),
                rs.getObject("current_starts_at", OffsetDateTime.class),
                rs.getObject("current_ends_at", OffsetDateTime.class),
                rs.getString("current_location_name"),
                rs.getString("current_service_name"),
                rs.getString("proposed_service_name"),
                rs.getString("current_professional_name"),
                rs.getString("proposed_professional_name"),
                rs.getString("current_room_name"),
                rs.getString("proposed_room_name"),
                rs.getString("proposed_location_name"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("customer_email"));
    }

    private RowMapper<CancellationLinkRecord> cancellationMapper() {
        return (rs, rowNum) -> new CancellationLinkRecord(
                rs.getObject("link_id", UUID.class),
                rs.getObject("business_id", UUID.class),
                rs.getObject("booking_id", UUID.class),
                rs.getString("link_status"),
                rs.getString("cancellation_url"),
                rs.getObject("expires_at", OffsetDateTime.class),
                rs.getObject("used_at", OffsetDateTime.class),
                rs.getString("cancellation_reason"),
                rs.getString("subject"),
                rs.getString("booking_status"),
                rs.getObject("starts_at", OffsetDateTime.class),
                rs.getObject("ends_at", OffsetDateTime.class),
                rs.getString("location_name"),
                rs.getString("service_name"),
                rs.getString("professional_name"),
                rs.getString("room_name"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("customer_email"));
    }

    public record ActionBookingRecord(UUID bookingId, UUID businessId, String subject, String bookingStatus,
            OffsetDateTime startsAt, OffsetDateTime endsAt, int durationMinutes, UUID locationId, String locationName,
            UUID serviceId, String serviceName, UUID professionalId, String professionalName, UUID roomId, String roomName,
            UUID conversationId, int rescheduleCount, String customerName, String customerPhone, String customerEmail) {
    }

    public record RescheduleLinkRecord(UUID linkId, UUID businessId, UUID bookingId, String linkStatus, String publicUrl,
            OffsetDateTime proposedStartsAt, OffsetDateTime proposedEndsAt, UUID proposedLocationId, UUID proposedServiceId,
            UUID proposedProfessionalId, UUID proposedRoomId, OffsetDateTime expiresAt, OffsetDateTime usedAt, String reason,
            String subject, String bookingStatus, OffsetDateTime currentStartsAt, OffsetDateTime currentEndsAt,
            String currentLocationName, String currentServiceName, String proposedServiceName, String currentProfessionalName,
            String proposedProfessionalName, String currentRoomName, String proposedRoomName, String proposedLocationName,
            String customerName, String customerPhone, String customerEmail) {
    }

    public record CancellationLinkRecord(UUID linkId, UUID businessId, UUID bookingId, String linkStatus, String publicUrl,
            OffsetDateTime expiresAt, OffsetDateTime usedAt, String cancellationReason, String subject, String bookingStatus,
            OffsetDateTime startsAt, OffsetDateTime endsAt, String locationName, String serviceName, String professionalName,
            String roomName, String customerName, String customerPhone, String customerEmail) {
    }
}
