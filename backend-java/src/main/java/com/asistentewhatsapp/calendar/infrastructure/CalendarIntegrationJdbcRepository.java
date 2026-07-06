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
                "select * from calendar_integration_account where business_id = :businessId and active = true",
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
        List<CalendarIntegrationAccountRecord> results = jdbcTemplate.query(
                "select * from calendar_integration_account where id = :id",
                new MapSqlParameterSource("id", id),
                recordRowMapper());
        return results.stream().findFirst();
    }

    public Optional<CalendarIntegrationAccountRecord> findByBusinessAndProvider(UUID businessId, String provider) {
        List<CalendarIntegrationAccountRecord> results = jdbcTemplate.query(
                "select * from calendar_integration_account where business_id = :businessId and provider = :provider",
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("provider", provider),
                recordRowMapper());
        return results.stream().findFirst();
    }

    public void save(CalendarIntegrationAccountRecord record) {
        jdbcTemplate.update(
                """
                insert into calendar_integration_account (id, business_id, provider, email,
                    access_token_encrypted, refresh_token_encrypted, token_expires_at,
                    calendar_id, calendar_summary, active, connected_at, created_at, updated_at)
                values (:id, :businessId, :provider, :email,
                    :accessTokenEncrypted, :refreshTokenEncrypted, :tokenExpiresAt,
                    :calendarId, :calendarSummary, :active, :connectedAt, :createdAt, :updatedAt)
                on conflict (business_id, provider) do update set
                    email = excluded.email,
                    access_token_encrypted = excluded.access_token_encrypted,
                    refresh_token_encrypted = excluded.refresh_token_encrypted,
                    token_expires_at = excluded.token_expires_at,
                    calendar_id = excluded.calendar_id,
                    calendar_summary = excluded.calendar_summary,
                    active = excluded.active,
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
                    .addValue("updatedAt", record.updatedAt()));
    }

    public void deactivate(UUID id) {
        jdbcTemplate.update(
                "update calendar_integration_account set active = false, updated_at = :now where id = :id",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("now", OffsetDateTime.now()));
    }

    private RowMapper<CalendarIntegrationAccountRecord> recordRowMapper() {
        return (rs, rowNum) -> new CalendarIntegrationAccountRecord(
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
                rs.getObject("updated_at", OffsetDateTime.class));
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
            OffsetDateTime updatedAt) {}
}
