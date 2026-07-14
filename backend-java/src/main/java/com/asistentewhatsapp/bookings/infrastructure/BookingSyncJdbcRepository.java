package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.domain.BookingSyncEventRecord;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingSyncJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingSyncJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertBookingFact(UUID bookingId, UUID businessId, String customerPhone,
            String customerName, String customerManagementId, String serviceName,
            String locationName, String professionalName, OffsetDateTime bookingDate,
            OffsetDateTime bookingTime, String bookingStatus, UUID conversationId,
            String channelOrigin, String originIntent, OffsetDateTime bookingCreatedAt) {
        jdbcTemplate.update("""
                insert into ia_hecho_reserva (
                    booking_id, business_id, customer_phone, customer_name,
                    customer_management_id, service_name, location_name, professional_name,
                    booking_date, booking_time, booking_status, conversation_id,
                    channel_origin, origin_intent, tiene_reserva_activa, booking_created_at,
                    sync_status, synced_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?, 'SYNCED', current_timestamp, current_timestamp, current_timestamp)
                on conflict (booking_id)
                do update set
                    customer_phone = excluded.customer_phone,
                    customer_name = excluded.customer_name,
                    customer_management_id = excluded.customer_management_id,
                    service_name = excluded.service_name,
                    location_name = excluded.location_name,
                    professional_name = excluded.professional_name,
                    booking_date = excluded.booking_date,
                    booking_time = excluded.booking_time,
                    booking_status = excluded.booking_status,
                    conversation_id = excluded.conversation_id,
                    channel_origin = excluded.channel_origin,
                    origin_intent = excluded.origin_intent,
                    tiene_reserva_activa = true,
                    sync_status = 'SYNCED',
                    synced_at = current_timestamp,
                    updated_at = current_timestamp
                """,
                bookingId, businessId, customerPhone, customerName,
                customerManagementId, serviceName, locationName, professionalName,
                bookingDate, bookingTime, bookingStatus, conversationId,
                channelOrigin, originIntent, bookingCreatedAt);
    }

    public void updateBookingSyncStatus(UUID bookingId, UUID businessId, String syncStatus) {
        jdbcTemplate.update("""
                update booking
                set sync_status = ?,
                    version = version + 1,
                    updated_at = current_timestamp
                where id = ? and business_id = ?
                """, syncStatus, bookingId, businessId);
    }

    public Optional<BookingSyncEventRecord> findEventByIdempotencyKey(String idempotencyKey) {
        var results = jdbcTemplate.query("""
                select id, booking_id, business_id, event_type, event_version,
                       event_body, idempotency_key, status, attempts, max_attempts,
                       next_attempt_at, locked_at, last_error_code, last_error_message,
                       trace_id, created_at, updated_at
                from booking_sync_event
                where idempotency_key = ?
                limit 1
                """,
                (rs, rowNum) -> new BookingSyncEventRecord(
                        rs.getObject("id", UUID.class),
                        rs.getObject("booking_id", UUID.class),
                        rs.getObject("business_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getInt("event_version"),
                        rs.getString("event_body"),
                        rs.getString("idempotency_key"),
                        rs.getString("status"),
                        rs.getInt("attempts"),
                        rs.getInt("max_attempts"),
                        rs.getObject("next_attempt_at", OffsetDateTime.class),
                        rs.getObject("locked_at", OffsetDateTime.class),
                        rs.getString("last_error_code"),
                        rs.getString("last_error_message"),
                        rs.getString("trace_id"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                idempotencyKey);
        return results.stream().findFirst();
    }

    public void updateBookingFactStatus(UUID bookingId, String syncStatus) {
        jdbcTemplate.update("""
                update ia_hecho_reserva
                set sync_status = ?, synced_at = case when ? = 'SYNCED' then current_timestamp else synced_at end,
                    updated_at = current_timestamp
                where booking_id = ?
                """, syncStatus, syncStatus, bookingId);
    }
}
