package com.asistentewhatsapp.configuration.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WhatsAppChannelJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public WhatsAppChannelJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public String findBusinessName(UUID businessId) {
		return jdbcTemplate.query("select coalesce(business_name, company_name) from business where id = ?",
				(ResultSet rs) -> {
					if (rs.next())
						return rs.getString(1);
					return "Empresa";
				}, businessId);
	}

	public Optional<ChannelRecord> findChannel(UUID businessId) {
		return jdbcTemplate.query("""
				select ca.id,
				       ca.business_id,
				       b.business_name,
				       b.company_name,
				       ca.provider_name,
				       ca.session_key,
				       ca.status,
				       ca.phone_number,
				       ca.display_phone_number,
				       ca.normalized_phone_number,
				       ca.provider_account_id,
				       ca.phone_number_id,
				       ca.registration_status,
				       ca.operational_status,
				       ca.webhook_status,
				       ca.credential_status,
				       ca.active,
				       ca.last_health_check_at,
				       ca.last_message_received_at,
				       ca.last_message_sent_at,
				       ca.last_error_code,
				       ca.last_error,
				       ca.last_event_at,
				       ca.connected_at,
				       ca.disconnected_at,
				       ca.last_qr_code,
				       ca.last_qr_at,
				       ca.token_expires_at,
				       ca.version,
				       ca.created_at,
				       ca.updated_at
				from channel_account ca
				join business b on b.id = ca.business_id
				where ca.business_id = ?
				  and ca.channel_type = 'WHATSAPP'
				order by ca.created_at asc
				limit 1
				""", new ChannelRowMapper(), businessId).stream().findFirst();
	}

	public List<EventRecord> findRecentEvents(UUID businessId, int limit) {
		return jdbcTemplate.query("""
				select delivery_id, event_type, processing_status, received_at
				from channel_event_log
				where business_id = ?
				order by received_at desc
				limit ?
				""", new EventRowMapper(), businessId, limit);
	}

	public void updateChannelConfig(UUID channelId, String displayPhoneNumber, String normalizedPhoneNumber,
			String phoneNumberId, String businessAccountId, String graphApiVersion, String webhookCallbackUrl) {
		jdbcTemplate.update(
				"""
						update channel_account
						set display_phone_number = coalesce(?, display_phone_number),
						    normalized_phone_number = coalesce(?, normalized_phone_number),
						    phone_number_id = coalesce(?, phone_number_id),
						    provider_account_id = coalesce(?, provider_account_id),
						    adapter_mode = coalesce(?, adapter_mode),
						    webhook_status = case when ? is not null and ? != '' then 'PENDING_VALIDATION' else webhook_status end,
						    version = version + 1,
						    updated_at = current_timestamp
						where id = ?
						""",
				displayPhoneNumber, normalizedPhoneNumber, phoneNumberId, businessAccountId, graphApiVersion,
				webhookCallbackUrl, webhookCallbackUrl, channelId);
	}

	public void updateActive(UUID businessId, boolean active) {
		jdbcTemplate.update("""
				update channel_account
				set active = ?,
				    operational_status = case when ? then 'CONNECTED' else 'DISCONNECTED' end,
				    version = version + 1,
				    updated_at = current_timestamp
				where business_id = ?
				  and channel_type = 'WHATSAPP'
				  and provider_name = 'META_CLOUD_API'
				""", active, active, businessId);
	}

	public void updateHealthCheck(UUID businessId, OffsetDateTime now) {
		jdbcTemplate.update("""
				update channel_account
				set last_health_check_at = ?,
				    version = version + 1,
				    updated_at = current_timestamp
				where business_id = ?
				  and channel_type = 'WHATSAPP'
				""", now, businessId);
	}

	public record ChannelRecord(UUID id, UUID businessId, String businessName, String companyName, String providerName,
			String sessionKey, String sessionStatus, String phoneNumber, String displayPhoneNumber,
			String normalizedPhoneNumber, String providerAccountId, String phoneNumberId, String registrationStatus,
			String operationalStatus, String webhookStatus, String credentialStatus, boolean active,
			OffsetDateTime lastHealthCheckAt, OffsetDateTime lastMessageReceivedAt, OffsetDateTime lastMessageSentAt,
			String lastErrorCode, String lastErrorMessage, OffsetDateTime lastEventAt, OffsetDateTime connectedAt,
			OffsetDateTime disconnectedAt, String lastQrCode, OffsetDateTime lastQrAt, OffsetDateTime tokenExpiresAt,
			int version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

		public String graphApiVersion() {
			return "v23.0";
		}

		public String webhookCallbackUrl() {
			return null;
		}
	}

	public record EventRecord(String deliveryId, String eventType, String processingStatus, OffsetDateTime receivedAt) {
	}

	private static class ChannelRowMapper implements RowMapper<ChannelRecord> {
		@Override
		public ChannelRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new ChannelRecord(rs.getObject("id", UUID.class), rs.getObject("business_id", UUID.class),
					rs.getString("business_name"), rs.getString("company_name"), rs.getString("provider_name"),
					rs.getString("session_key"), rs.getString("status"), rs.getString("phone_number"),
					rs.getString("display_phone_number"), rs.getString("normalized_phone_number"),
					rs.getString("provider_account_id"), rs.getString("phone_number_id"),
					rs.getString("registration_status"), rs.getString("operational_status"),
					rs.getString("webhook_status"), rs.getString("credential_status"), rs.getBoolean("active"),
					rs.getObject("last_health_check_at", OffsetDateTime.class),
					rs.getObject("last_message_received_at", OffsetDateTime.class),
					rs.getObject("last_message_sent_at", OffsetDateTime.class), rs.getString("last_error_code"),
					rs.getString("last_error"), rs.getObject("last_event_at", OffsetDateTime.class),
					rs.getObject("connected_at", OffsetDateTime.class),
					rs.getObject("disconnected_at", OffsetDateTime.class), rs.getString("last_qr_code"),
					rs.getObject("last_qr_at", OffsetDateTime.class),
					rs.getObject("token_expires_at", OffsetDateTime.class), rs.getInt("version"),
					rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
		}
	}

	private static class EventRowMapper implements RowMapper<EventRecord> {
		@Override
		public EventRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new EventRecord(rs.getString("delivery_id"), rs.getString("event_type"),
					rs.getString("processing_status"), rs.getObject("received_at", OffsetDateTime.class));
		}
	}
}
