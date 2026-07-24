package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.domain.BookingSyncEventRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingSyncEventJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingSyncEventJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean enqueue(UUID eventId, UUID bookingId, UUID businessId, String eventType,
            int eventVersion, String eventBody, String idempotencyKey, int maxAttempts,
            OffsetDateTime nextAttemptAt, String traceId) {
        try {
            jdbcTemplate.update("""
                    insert into booking_sync_event (
                        id, booking_id, business_id, event_type, event_version,
                        event_body, idempotency_key, status, attempts, max_attempts,
                        next_attempt_at, trace_id, created_at, updated_at
                    ) values (?, ?, ?, ?, ?, cast(? as jsonb), ?, 'PENDING', 0, ?, ?, ?, current_timestamp, current_timestamp)
                    """,
                    eventId, bookingId, businessId, eventType, eventVersion,
                    eventBody, idempotencyKey, maxAttempts, nextAttemptAt, traceId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public List<BookingSyncEventRecord> claimDueEvents(int limit, long processingTimeoutMs) {
        return jdbcTemplate.query("""
                with candidate as (
                    select id
                    from booking_sync_event
                    where (
                            status = 'PENDING'
                            and next_attempt_at <= current_timestamp
                          )
                       or (
                            status = 'PROCESSING'
                            and locked_at < current_timestamp - (? * interval '1 millisecond')
                          )
                    order by next_attempt_at asc, created_at asc
                    limit ?
                    for update skip locked
                )
                update booking_sync_event q
                set status = 'PROCESSING',
                    attempts = q.attempts + 1,
                    locked_at = current_timestamp,
                    updated_at = current_timestamp
                from candidate
                where q.id = candidate.id
                returning q.id, q.booking_id, q.business_id, q.event_type, q.event_version,
                          q.event_body, q.idempotency_key, q.status, q.attempts, q.max_attempts,
                          q.next_attempt_at, q.locked_at, q.last_error_code, q.last_error_message,
                          q.trace_id, q.created_at, q.updated_at
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
                processingTimeoutMs,
                limit);
    }

    public void markSynced(UUID eventId, OffsetDateTime syncedAt) {
        jdbcTemplate.update("""
                update booking_sync_event
                set status = 'SYNCED',
                    locked_at = null,
                    last_error_code = null,
                    last_error_message = null,
                    updated_at = current_timestamp
                where id = ?
                """, eventId);
    }

    public void markSkipped(UUID eventId, String reason) {
        jdbcTemplate.update("""
                update booking_sync_event
                set status = 'SKIPPED',
                    locked_at = null,
                    last_error_code = ?,
                    last_error_message = ?,
                    updated_at = current_timestamp
                where id = ?
                """, reason, reason, eventId);
    }

    public void markFailedOrRetry(UUID eventId, int attempts, int maxAttempts,
            String errorCode, String errorMessage, OffsetDateTime nextAttemptAt) {
        String nextStatus = attempts >= maxAttempts ? "FAILED" : "PENDING";
        jdbcTemplate.update("""
                update booking_sync_event
                set status = ?,
                    next_attempt_at = ?,
                    locked_at = null,
                    last_error_code = ?,
                    last_error_message = ?,
                    updated_at = current_timestamp
                where id = ?
                """, nextStatus, nextAttemptAt, errorCode, truncate(errorMessage, 4000), eventId);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
