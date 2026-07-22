package com.asistentewhatsapp.cloudapi.onboarding;

import com.asistentewhatsapp.shared.exception.ApiException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MetaOnboardingRepository {

    private final JdbcTemplate jdbcTemplate;

    public MetaOnboardingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static String maskString(String value) {
        if (value == null || value.length() < 8) return value;
        return value.substring(0, 4) + "\u2022\u2022\u2022\u2022" + value.substring(value.length() - 4);
    }

    public Optional<ChannelAccountRecord> findCentralizedChannel() {
        return jdbcTemplate.query(
                """
                        select ca.id, ca.business_id, ca.provider_name, ca.channel_type,
                               ca.status, ca.phone_number, ca.display_phone_number,
                               ca.normalized_phone_number, ca.phone_number_id,
                               ca.provider_account_id, ca.registration_status,
                               ca.operational_status, ca.webhook_status, ca.credential_status,
                               ca.active, ca.encrypted_access_token,
                               ca.encrypted_verify_token, ca.token_expires_at,
                               ca.location_id, ca.routing_mode, ca.adapter_mode,
                               ca.last_error_code, ca.last_error, ca.last_health_check_at, ca.version,
                               ca.created_at, ca.updated_at,
                               null as location_name, null as location_code
                        from channel_account ca
                        where ca.routing_mode = 'CENTRALIZED'
                          and ca.provider_name = 'META_CLOUD_API'
                          and ca.channel_type = 'WHATSAPP'
                          and ca.active = true
                        order by ca.updated_at desc
                        """,
                new ChannelAccountRowMapper())
                .stream()
                .findFirst();
    }

    public Optional<ChannelAccountRecord> findCloudApiChannel(UUID businessId) {
        return jdbcTemplate.query(
                """
                        select ca.id, ca.business_id, ca.provider_name, ca.channel_type,
                               ca.status, ca.phone_number, ca.display_phone_number,
                               ca.normalized_phone_number, ca.phone_number_id,
                               ca.provider_account_id, ca.registration_status,
                               ca.operational_status, ca.webhook_status, ca.credential_status,
                               ca.active, ca.encrypted_access_token,
                               ca.encrypted_verify_token, ca.token_expires_at,
                               ca.location_id, ca.routing_mode, ca.adapter_mode,
                               ca.last_error_code, ca.last_error, ca.last_health_check_at, ca.version,
                               ca.created_at, ca.updated_at,
                               null as location_name, null as location_code
                        from channel_account ca
                        where ca.business_id = ?
                          and ca.provider_name = 'META_CLOUD_API'
                          and ca.channel_type = 'WHATSAPP'
                          and ca.active = true
                        order by ca.updated_at desc
                        """,
                new ChannelAccountRowMapper(),
                businessId)
                .stream()
                .findFirst();
    }

    public ChannelAccountRecord findOrCreateCentralChannel(UUID businessId) {
        return findCloudApiChannel(businessId)
                .orElseGet(() -> {
                    UUID newId = UUID.randomUUID();
                    jdbcTemplate.update(
                            """
                                    insert into channel_account (
                                        id, business_id, provider_name, channel_type, session_key, status,
                                        phone_number, display_phone_number, normalized_phone_number,
                                        location_id, routing_mode, adapter_mode,
                                        active, registration_status, operational_status, webhook_status, credential_status,
                                        created_at, updated_at
                                    ) values (
                                        ?, ?, 'META_CLOUD_API', 'WHATSAPP',
                                        'META_CLOUD_API_' || ?::text, 'DISCONNECTED',
                                        null, null, null,
                                        null, 'CENTRALIZED', 'META_CLOUD_API_CLOUD_API',
                                        true, 'NOT_CONFIGURED', 'INACTIVE', 'NOT_CONFIGURED', 'NOT_CONFIGURED',
                                        current_timestamp, current_timestamp
                                    )
                                    on conflict (business_id) where provider_name = 'META_CLOUD_API' and routing_mode = 'CENTRALIZED' and active = true do nothing
                                    """,
                            newId, businessId, newId);
                    return findCloudApiChannel(businessId).orElseThrow(() ->
                            new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CHANNEL_CREATE_FAILED",
                                    "No se pudo crear el canal central META_CLOUD_API."));
                });
    }

    public void validatePhoneNumberIdNotUsed(String phoneNumberId, UUID excludeBusinessId) {
        if (phoneNumberId == null || phoneNumberId.isBlank()) return;
        Optional<UUID> existing = jdbcTemplate.query(
                """
                        select business_id from channel_account
                        where phone_number_id = ?
                          and provider_name = 'META_CLOUD_API'
                          and channel_type = 'WHATSAPP'
                          and active = true
                          and business_id <> ?
                        limit 1
                        """,
                (rs, rn) -> rs.getObject("business_id", UUID.class),
                phoneNumberId, excludeBusinessId)
                .stream().findFirst();
        if (existing.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_NUMBER_ID_ALREADY_IN_USE",
                    "El Phone Number ID " + maskString(phoneNumberId) + " ya está asociado a otra empresa.");
        }
    }

    public void updateAfterOnboarding(
            UUID channelId,
            String phoneNumberId,
            String businessAccountId,
            String encryptedAccessToken,
            OffsetDateTime tokenExpiresAt) {
        int updated = jdbcTemplate.update(
                """
                        update channel_account
                        set phone_number_id = ?,
                            provider_account_id = ?,
                            encrypted_access_token = ?,
                            token_expires_at = ?,
                            routing_mode = 'CENTRALIZED',
                            location_id = null,
                            registration_status = 'REGISTERED',
                            operational_status = 'CONNECTED',
                            credential_status = 'CONFIGURED',
                            active = true,
                            version = version + 1,
                            updated_at = current_timestamp
                        where id = ?
                          and provider_name = 'META_CLOUD_API'
                        """,
                phoneNumberId,
                businessAccountId,
                encryptedAccessToken,
                tokenExpiresAt,
                channelId);
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND",
                    "No se encontro el canal META_CLOUD_API para actualizar.");
        }
    }

    public void updatePhoneMetadata(UUID channelId, String displayPhoneNumber, String normalizedPhoneNumber, String verifiedName) {
        jdbcTemplate.update(
                """
                        update channel_account
                        set display_phone_number = ?,
                            normalized_phone_number = ?,
                            verified_name = ?,
                            version = version + 1,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                displayPhoneNumber,
                normalizedPhoneNumber,
                verifiedName,
                channelId);
    }

    public void updateWebhookStatus(UUID channelId, String webhookStatus) {
        jdbcTemplate.update(
                """
                        update channel_account
                        set webhook_status = ?,
                            version = version + 1,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                webhookStatus,
                channelId);
    }

    public void updateHealthCheck(UUID channelId) {
        jdbcTemplate.update(
                """
                        update channel_account
                        set last_health_check_at = current_timestamp,
                            version = version + 1,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                channelId);
    }

    public void updateOperationalStatus(UUID channelId, String operationalStatus, String lastErrorCode) {
        jdbcTemplate.update(
                """
                        update channel_account
                        set operational_status = ?,
                            last_error_code = ?,
                            version = version + 1,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                operationalStatus,
                lastErrorCode,
                channelId);
    }

    public void disconnectChannel(UUID channelId) {
        jdbcTemplate.update(
                """
                        update channel_account
                        set operational_status = 'DISCONNECTED',
                            credential_status = 'NOT_CONFIGURED',
                            encrypted_access_token = null,
                            encrypted_verify_token = null,
                            phone_number_id = null,
                            provider_account_id = null,
                            token_expires_at = null,
                            active = false,
                            version = version + 1,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                channelId);
    }

    public record ChannelAccountRecord(
            UUID id,
            UUID businessId,
            String providerName,
            String channelType,
            String status,
            String phoneNumber,
            String displayPhoneNumber,
            String normalizedPhoneNumber,
            String phoneNumberId,
            String providerAccountId,
            String registrationStatus,
            String operationalStatus,
            String webhookStatus,
            String credentialStatus,
            boolean active,
            String encryptedAccessToken,
            String encryptedVerifyToken,
            OffsetDateTime tokenExpiresAt,
            UUID locationId,
            String routingMode,
            String adapterMode,
            String lastErrorCode,
            String lastError,
            OffsetDateTime lastHealthCheckAt,
            int version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String locationName,
            String locationCode) {
    }

    private static class ChannelAccountRowMapper implements RowMapper<ChannelAccountRecord> {
        @Override
        public ChannelAccountRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ChannelAccountRecord(
                    rs.getObject("id", UUID.class),
                    rs.getObject("business_id", UUID.class),
                    rs.getString("provider_name"),
                    rs.getString("channel_type"),
                    rs.getString("status"),
                    rs.getString("phone_number"),
                    rs.getString("display_phone_number"),
                    rs.getString("normalized_phone_number"),
                    rs.getString("phone_number_id"),
                    rs.getString("provider_account_id"),
                    rs.getString("registration_status"),
                    rs.getString("operational_status"),
                    rs.getString("webhook_status"),
                    rs.getString("credential_status"),
                    rs.getBoolean("active"),
                    rs.getString("encrypted_access_token"),
                    rs.getString("encrypted_verify_token"),
                    rs.getObject("token_expires_at", OffsetDateTime.class),
                    rs.getObject("location_id", UUID.class),
                    rs.getString("routing_mode"),
                    rs.getString("adapter_mode"),
                    rs.getString("last_error_code"),
                    rs.getString("last_error"),
                    rs.getObject("last_health_check_at", OffsetDateTime.class),
                    rs.getInt("version"),
                    rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("updated_at", OffsetDateTime.class),
                    rs.getString("location_name"),
                    rs.getString("location_code"));
        }
    }
}
