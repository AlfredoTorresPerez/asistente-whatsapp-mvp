package com.asistentewhatsapp.calendar.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingCalendarSyncJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookingCalendarSyncJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BookingCalendarSyncRecord> findByBooking(UUID bookingId) {
        return jdbcTemplate.query(
                "select * from booking_calendar_sync where booking_id = :bookingId order by provider",
                new MapSqlParameterSource("bookingId", bookingId),
                recordRowMapper());
    }

    public List<BookingCalendarSyncRecord> findByBookingAndBusiness(UUID bookingId, UUID businessId) {
        return jdbcTemplate.query(
                "select * from booking_calendar_sync where booking_id = :bookingId and business_id = :businessId order by provider",
                new MapSqlParameterSource()
                        .addValue("bookingId", bookingId)
                        .addValue("businessId", businessId),
                recordRowMapper());
    }

    public List<BookingCalendarSyncRecord> findByBookingAndProvider(UUID bookingId, String provider) {
        return jdbcTemplate.query(
                "select * from booking_calendar_sync where booking_id = :bookingId and provider = :provider",
                new MapSqlParameterSource()
                        .addValue("bookingId", bookingId)
                        .addValue("provider", provider),
                recordRowMapper());
    }

    public Optional<BookingCalendarSyncRecord> findByBookingAndProviderAndBusiness(UUID bookingId, String provider, UUID businessId) {
        return jdbcTemplate.query(
                "select * from booking_calendar_sync where booking_id = :bookingId and provider = :provider and business_id = :businessId",
                new MapSqlParameterSource()
                        .addValue("bookingId", bookingId)
                        .addValue("provider", provider)
                        .addValue("businessId", businessId),
                recordRowMapper()).stream().findFirst();
    }

    public Optional<BookingCalendarSyncRecord> findByBookingAndProviderOptional(UUID bookingId, String provider) {
        return findByBookingAndProvider(bookingId, provider).stream().findFirst();
    }

    public List<BookingCalendarSyncRecord> findFailedSyncs(int maxRetries, OffsetDateTime before) {
        return jdbcTemplate.query(
                """
                select * from booking_calendar_sync
                where sync_status = 'FAILED'
                  and retry_count < :maxRetries
                  and (last_sync_attempt_at is null or last_sync_attempt_at < :before)
                order by retry_count asc, last_sync_attempt_at asc nulls first
                limit 50
                """,
                new MapSqlParameterSource()
                        .addValue("maxRetries", maxRetries)
                        .addValue("before", before),
                recordRowMapper());
    }

    public List<BookingCalendarSyncRecord> findByBusinessAndStatusAndRetries(UUID businessId, String status,
            int maxRetries, OffsetDateTime before, int limit) {
        return jdbcTemplate.query(
                """
                select * from booking_calendar_sync
                where business_id = :businessId
                  and sync_status = :status
                  and retry_count < :maxRetries
                  and (last_sync_attempt_at is null or last_sync_attempt_at < :before)
                order by retry_count asc, last_sync_attempt_at asc nulls first
                limit :limit
                """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("status", status)
                        .addValue("maxRetries", maxRetries)
                        .addValue("before", before)
                        .addValue("limit", limit),
                recordRowMapper());
    }

    public List<BookingCalendarSyncRecord> findPendingSyncs(int limit) {
        return jdbcTemplate.query(
                """
                select * from booking_calendar_sync
                where sync_status = 'PENDING'
                order by created_at asc
                limit :limit
                """,
                new MapSqlParameterSource("limit", limit),
                recordRowMapper());
    }

    public void insert(BookingCalendarSyncRecord record) {
        jdbcTemplate.update(
                """
                insert into booking_calendar_sync (id, booking_id, business_id, provider,
                    external_event_id, sync_status, sync_action, error_message,
                    retry_count, last_sync_attempt_at, last_successful_sync_at, created_at, updated_at)
                values (:id, :bookingId, :businessId, :provider,
                    :externalEventId, :syncStatus, :syncAction, :errorMessage,
                    :retryCount, :lastSyncAttemptAt, :lastSuccessfulSyncAt, :createdAt, :updatedAt)
                on conflict (booking_id, provider) do update set
                    external_event_id = coalesce(excluded.external_event_id, booking_calendar_sync.external_event_id),
                    sync_status = excluded.sync_status,
                    sync_action = excluded.sync_action,
                    error_message = excluded.error_message,
                    retry_count = excluded.retry_count,
                    last_sync_attempt_at = excluded.last_sync_attempt_at,
                    last_successful_sync_at = excluded.last_successful_sync_at,
                    updated_at = excluded.updated_at
                """,
            new MapSqlParameterSource()
                    .addValue("id", record.id())
                    .addValue("bookingId", record.bookingId())
                    .addValue("businessId", record.businessId())
                    .addValue("provider", record.provider())
                    .addValue("externalEventId", record.externalEventId())
                    .addValue("syncStatus", record.syncStatus())
                    .addValue("syncAction", record.syncAction())
                    .addValue("errorMessage", record.errorMessage())
                    .addValue("retryCount", record.retryCount())
                    .addValue("lastSyncAttemptAt", record.lastSyncAttemptAt())
                    .addValue("lastSuccessfulSyncAt", record.lastSuccessfulSyncAt())
                    .addValue("createdAt", record.createdAt())
                    .addValue("updatedAt", record.updatedAt()));
    }

    public void updateStatus(UUID syncId, String syncStatus, String externalEventId, String errorMessage) {
        jdbcTemplate.update(
                """
                update booking_calendar_sync set
                    sync_status = :syncStatus,
                    external_event_id = coalesce(:externalEventId, external_event_id),
                    error_message = :errorMessage,
                    last_sync_attempt_at = :now,
                    updated_at = :now
                where id = :id
                """,
            new MapSqlParameterSource()
                    .addValue("id", syncId)
                    .addValue("syncStatus", syncStatus)
                    .addValue("externalEventId", externalEventId)
                    .addValue("errorMessage", errorMessage)
                    .addValue("now", OffsetDateTime.now()));
    }

    public void updateSyncSuccess(UUID syncId, String externalEventId) {
        jdbcTemplate.update(
                """
                update booking_calendar_sync set
                    sync_status = 'SYNCED',
                    external_event_id = coalesce(:externalEventId, external_event_id),
                    error_message = null,
                    retry_count = 0,
                    last_sync_attempt_at = :now,
                    last_successful_sync_at = :now,
                    updated_at = :now
                where id = :id
                """,
            new MapSqlParameterSource()
                    .addValue("id", syncId)
                    .addValue("externalEventId", externalEventId)
                    .addValue("now", OffsetDateTime.now()));
    }

    public void updateSyncFailed(UUID syncId, String errorMessage) {
        jdbcTemplate.update(
                """
                update booking_calendar_sync set
                    sync_status = 'FAILED',
                    error_message = :errorMessage,
                    retry_count = retry_count + 1,
                    last_sync_attempt_at = :now,
                    updated_at = :now
                where id = :id
                """,
            new MapSqlParameterSource()
                    .addValue("id", syncId)
                    .addValue("errorMessage", errorMessage != null && errorMessage.length() > 500
                            ? errorMessage.substring(0, 500) : errorMessage)
                    .addValue("now", OffsetDateTime.now()));
    }

    private RowMapper<BookingCalendarSyncRecord> recordRowMapper() {
        return (rs, rowNum) -> new BookingCalendarSyncRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("booking_id", UUID.class),
                rs.getObject("business_id", UUID.class),
                rs.getString("provider"),
                rs.getString("external_event_id"),
                rs.getString("sync_status"),
                rs.getString("sync_action"),
                rs.getString("error_message"),
                rs.getInt("retry_count"),
                rs.getObject("last_sync_attempt_at", OffsetDateTime.class),
                rs.getObject("last_successful_sync_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    public record BookingCalendarSyncRecord(
            UUID id,
            UUID bookingId,
            UUID businessId,
            String provider,
            String externalEventId,
            String syncStatus,
            String syncAction,
            String errorMessage,
            int retryCount,
            OffsetDateTime lastSyncAttemptAt,
            OffsetDateTime lastSuccessfulSyncAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {}
}
