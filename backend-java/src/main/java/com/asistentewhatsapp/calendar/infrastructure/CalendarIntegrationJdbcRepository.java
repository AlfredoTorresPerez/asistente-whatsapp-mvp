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
public class CalendarIntegrationJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CalendarIntegrationJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CalendarIntegrationAccountRecord> findActiveByBusiness(UUID businessId) {
        return jdbcTemplate.query(
                "select * from calendar_integration_account where business_id = :businessId and active = true order by provider",
                new MapSqlParameterSource("businessId", businessId),
                recordRowMapper());
    }

    public List<CalendarIntegrationAccountRecord> findActiveByBusinessAndProvider(UUID businessId, String provider) {
        return jdbcTemplate.query(
                "select * from calendar_integration_account where business_id = :businessId and provider = :provider and active = true",
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("provider", provider),
                recordRowMapper());
    }

    public Optional<CalendarIntegrationAccountRecord> findById(UUID id) {
        return jdbcTemplate.query(
                "select * from calendar_integration_account where id = :id",
                new MapSqlParameterSource("id", id),
                recordRowMapper()).stream().findFirst();
    }

    public Optional<CalendarIntegrationAccountRecord> findByIdAndBusiness(UUID id, UUID businessId) {
        return jdbcTemplate.query(
                "select * from calendar_integration_account where id = :id and business_id = :businessId",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("businessId", businessId),
                recordRowMapper()).stream().findFirst();
    }

    public Optional<CalendarIntegrationAccountRecord> findByBusinessAndProviderActive(UUID businessId, String provider) {
        return jdbcTemplate.query(
                "select * from calendar_integration_account where business_id = :businessId and provider = :provider and active = true",
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("provider", provider),
                recordRowMapper()).stream().findFirst();
    }

    public Optional<CalendarIntegrationAccountRecord> findByBusinessAndProvider(UUID businessId, String provider) {
        return jdbcTemplate.query(
                "select * from calendar_integration_account where business_id = :businessId and provider = :provider",
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("provider", provider),
                recordRowMapper()).stream().findFirst();
    }

    public void save(CalendarIntegrationAccountRecord record) {
        jdbcTemplate.update(
                """
                insert into calendar_integration_account (id, business_id, provider, email,
                    access_token_encrypted, refresh_token_encrypted, token_expires_at,
                    calendar_id, calendar_summary, active, connected_at, created_at, updated_at,
                    revoked_at, last_sync_at, requires_reconnect)
                values (:id, :businessId, :provider, :email,
                    :accessTokenEncrypted, :refreshTokenEncrypted, :tokenExpiresAt,
                    :calendarId, :calendarSummary, :active, :connectedAt, :createdAt, :updatedAt,
                    :revokedAt, :lastSyncAt, :requiresReconnect)
                on conflict (business_id, provider) do update set
                    email = coalesce(excluded.email, calendar_integration_account.email),
                    access_token_encrypted = coalesce(excluded.access_token_encrypted, calendar_integration_account.access_token_encrypted),
                    refresh_token_encrypted = coalesce(excluded.refresh_token_encrypted, calendar_integration_account.refresh_token_encrypted),
                    token_expires_at = excluded.token_expires_at,
                    calendar_id = coalesce(excluded.calendar_id, calendar_integration_account.calendar_id),
                    calendar_summary = coalesce(excluded.calendar_summary, calendar_integration_account.calendar_summary),
                    active = coalesce(excluded.active, calendar_integration_account.active),
                    revoked_at = coalesce(excluded.revoked_at, calendar_integration_account.revoked_at),
                    last_sync_at = coalesce(excluded.last_sync_at, calendar_integration_account.last_sync_at),
                    requires_reconnect = coalesce(excluded.requires_reconnect, calendar_integration_account.requires_reconnect),
                    updated_at = excluded.updated_at
                """,
            new MapSqlParameterSource()
                    .addValue("id", record.id())
                    .addValue("businessId", record.businessId())
                    .addValue("provider", record.provider())
                    .addValue("email", record.email())
                    .addValue("accessTokenEncrypted", record.accessTokenEncrypted())
                    .addValue("refreshTokenEncrypted", record.refreshTokenEncrypted())
                    .addValue("tokenExpiresAt", record.tokenExpiresAt())
                    .addValue("calendarId", record.calendarId())
                    .addValue("calendarSummary", record.calendarSummary())
                    .addValue("active", record.active())
                    .addValue("connectedAt", record.connectedAt())
                    .addValue("createdAt", record.createdAt())
                    .addValue("updatedAt", record.updatedAt())
                    .addValue("revokedAt", record.revokedAt())
                    .addValue("lastSyncAt", record.lastSyncAt())
                    .addValue("requiresReconnect", record.requiresReconnect()));
    }

    public void updateCalendarId(UUID id, UUID businessId, String calendarId, String calendarSummary) {
        jdbcTemplate.update(
                """
                update calendar_integration_account set
                    calendar_id = :calendarId,
                    calendar_summary = :calendarSummary,
                    updated_at = :now
                where id = :id and business_id = :businessId
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("businessId", businessId)
                        .addValue("calendarId", calendarId)
                        .addValue("calendarSummary", calendarSummary)
                        .addValue("now", OffsetDateTime.now()));
    }

    public void updateTokens(UUID id, UUID businessId, String accessTokenEncrypted, String refreshTokenEncrypted,
            OffsetDateTime tokenExpiresAt) {
        jdbcTemplate.update(
                """
                update calendar_integration_account set
                    access_token_encrypted = :accessTokenEncrypted,
                    refresh_token_encrypted = :refreshTokenEncrypted,
                    token_expires_at = :tokenExpiresAt,
                    updated_at = :now
                where id = :id and business_id = :businessId
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("businessId", businessId)
                        .addValue("accessTokenEncrypted", accessTokenEncrypted)
                        .addValue("refreshTokenEncrypted", refreshTokenEncrypted)
                        .addValue("tokenExpiresAt", tokenExpiresAt)
                        .addValue("now", OffsetDateTime.now()));
    }

    public void updateLastSyncAt(UUID id, UUID businessId, OffsetDateTime lastSyncAt) {
        jdbcTemplate.update(
                """
                update calendar_integration_account set
                    last_sync_at = :lastSyncAt,
                    updated_at = :now
                where id = :id and business_id = :businessId
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("businessId", businessId)
                        .addValue("lastSyncAt", lastSyncAt)
                        .addValue("now", OffsetDateTime.now()));
    }

    public void updateRequiresReconnect(UUID id, UUID businessId, boolean requiresReconnect) {
        jdbcTemplate.update(
                """
                update calendar_integration_account set
                    requires_reconnect = :requiresReconnect,
                    updated_at = :now
                where id = :id and business_id = :businessId
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("businessId", businessId)
                        .addValue("requiresReconnect", requiresReconnect)
                        .addValue("now", OffsetDateTime.now()));
    }

    public void revokeAccount(UUID id, UUID businessId) {
        jdbcTemplate.update(
                """
                update calendar_integration_account set
                    active = false,
                    access_token_encrypted = null,
                    refresh_token_encrypted = null,
                    token_expires_at = null,
                    revoked_at = :now,
                    updated_at = :now
                where id = :id and business_id = :businessId
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("businessId", businessId)
                        .addValue("now", OffsetDateTime.now()));
    }

    public List<CalendarIntegrationAccountRecord> findAccountsNeedingSync(OffsetDateTime before, int limit) {
        return jdbcTemplate.query(
                """
                select * from calendar_integration_account
                where active = true
                  and (last_sync_at is null or last_sync_at < :before)
                  and requires_reconnect = false
                order by last_sync_at asc nulls first
                limit :limit
                """,
                new MapSqlParameterSource()
                        .addValue("before", before)
                        .addValue("limit", limit),
                recordRowMapper());
    }

    private RowMapper<CalendarIntegrationAccountRecord> recordRowMapper() {
        return (rs, rowNum) -> {
            boolean requiresReconnect = false;
            try {
                requiresReconnect = rs.getBoolean("requires_reconnect");
            } catch (Exception ignored) {}
            return new CalendarIntegrationAccountRecord(
                    rs.getObject("id", UUID.class),
                    rs.getObject("business_id", UUID.class),
                    rs.getString("provider"),
                    rs.getString("email"),
                    rs.getString("access_token_encrypted"),
                    rs.getString("refresh_token_encrypted"),
                    rs.getObject("token_expires_at", OffsetDateTime.class),
                    rs.getString("calendar_id"),
                    rs.getString("calendar_summary"),
                    rs.getBoolean("active"),
                    rs.getObject("connected_at", OffsetDateTime.class),
                    rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("updated_at", OffsetDateTime.class),
                    rs.getObject("revoked_at", OffsetDateTime.class),
                    rs.getObject("last_sync_at", OffsetDateTime.class),
                    requiresReconnect);
        };
    }

    public record CalendarIntegrationAccountRecord(
            UUID id,
            UUID businessId,
            String provider,
            String email,
            String accessTokenEncrypted,
            String refreshTokenEncrypted,
            OffsetDateTime tokenExpiresAt,
            String calendarId,
            String calendarSummary,
            boolean active,
            OffsetDateTime connectedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime revokedAt,
            OffsetDateTime lastSyncAt,
            boolean requiresReconnect) {}
}
