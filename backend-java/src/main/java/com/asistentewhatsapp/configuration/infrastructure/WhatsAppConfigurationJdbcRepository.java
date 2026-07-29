package com.asistentewhatsapp.configuration.infrastructure;

import com.asistentewhatsapp.configuration.api.WhatsAppConfigurationPreferencesRequest;
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
public class WhatsAppConfigurationJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public WhatsAppConfigurationJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<BusinessRecord> findBusiness(UUID businessId) {
		return jdbcTemplate.query("""
				select id, business_name, company_name
				from business
				where id = ?
				""", new BusinessRowMapper(), businessId).stream().findFirst();
	}

	public Optional<ChannelAccountDetailRecord> findChannelAccount(UUID businessId) {
		return jdbcTemplate.query("""
				select id,
				       business_id,
				       provider_name,
				       session_key,
				       status,
				       phone_number,
				       last_qr_code,
				       last_event_at,
				       connected_at,
				       disconnected_at,
				       active,
				       updated_at
				from channel_account
				where business_id = ?
				  and channel_type = 'WHATSAPP'
				order by created_at asc
				limit 1
				""", new ChannelAccountDetailRowMapper(), businessId).stream().findFirst();
	}

	public PreferencesRecord findOrCreatePreferences(UUID businessId) {
		jdbcTemplate.update("""
				insert into whatsapp_configuration_preferences (
				    business_id,
				    new_message_notifications,
				    auto_reassignment,
				    agent_signature,
				    out_of_hours_message
				) values (?, true, true, true, false)
				on conflict (business_id) do nothing
				""", businessId);

		return jdbcTemplate.queryForObject("""
				select business_id,
				       new_message_notifications,
				       auto_reassignment,
				       agent_signature,
				       out_of_hours_message
				from whatsapp_configuration_preferences
				where business_id = ?
				""", new PreferencesRowMapper(), businessId);
	}

	public PreferencesRecord updatePreferences(UUID businessId, WhatsAppConfigurationPreferencesRequest request) {
		jdbcTemplate.update("""
				insert into whatsapp_configuration_preferences (
				    business_id,
				    new_message_notifications,
				    auto_reassignment,
				    agent_signature,
				    out_of_hours_message
				) values (?, ?, ?, ?, ?)
				on conflict (business_id) do update
				set new_message_notifications = excluded.new_message_notifications,
				    auto_reassignment = excluded.auto_reassignment,
				    agent_signature = excluded.agent_signature,
				    out_of_hours_message = excluded.out_of_hours_message,
				    updated_at = current_timestamp
				""", businessId, request.newMessageNotifications(), request.autoReassignment(),
				request.agentSignature(), request.outOfHoursMessage());
		return findOrCreatePreferences(businessId);
	}

	public List<SessionEventRecord> findRecentSessionEvents(UUID businessId, int limit) {
		return jdbcTemplate.query("""
				select delivery_id, event_type, processing_status, received_at
				from channel_event_log
				where business_id = ?
				order by received_at desc
				limit ?
				""", new SessionEventRowMapper(), businessId, limit);
	}

	public record BusinessRecord(UUID id, String businessName, String companyName) {
	}

	public record ChannelAccountDetailRecord(UUID id, UUID businessId, String providerName, String sessionKey,
			String status, String phoneNumber, String lastQrCode, OffsetDateTime lastEventAt,
			OffsetDateTime connectedAt, OffsetDateTime disconnectedAt, boolean active, OffsetDateTime updatedAt) {
	}

	public record PreferencesRecord(UUID businessId, boolean newMessageNotifications, boolean autoReassignment,
			boolean agentSignature, boolean outOfHoursMessage) {
	}

	public record SessionEventRecord(String deliveryId, String eventType, String processingStatus,
			OffsetDateTime receivedAt) {
	}

	private static class BusinessRowMapper implements RowMapper<BusinessRecord> {

		@Override
		public BusinessRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			return new BusinessRecord(resultSet.getObject("id", UUID.class), resultSet.getString("business_name"),
					resultSet.getString("company_name"));
		}
	}

	private static class ChannelAccountDetailRowMapper implements RowMapper<ChannelAccountDetailRecord> {

		@Override
		public ChannelAccountDetailRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			return new ChannelAccountDetailRecord(resultSet.getObject("id", UUID.class),
					resultSet.getObject("business_id", UUID.class), resultSet.getString("provider_name"),
					resultSet.getString("session_key"), resultSet.getString("status"),
					resultSet.getString("phone_number"), resultSet.getString("last_qr_code"),
					resultSet.getObject("last_event_at", OffsetDateTime.class),
					resultSet.getObject("connected_at", OffsetDateTime.class),
					resultSet.getObject("disconnected_at", OffsetDateTime.class), resultSet.getBoolean("active"),
					resultSet.getObject("updated_at", OffsetDateTime.class));
		}
	}

	private static class PreferencesRowMapper implements RowMapper<PreferencesRecord> {

		@Override
		public PreferencesRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			return new PreferencesRecord(resultSet.getObject("business_id", UUID.class),
					resultSet.getBoolean("new_message_notifications"), resultSet.getBoolean("auto_reassignment"),
					resultSet.getBoolean("agent_signature"), resultSet.getBoolean("out_of_hours_message"));
		}
	}

	private static class SessionEventRowMapper implements RowMapper<SessionEventRecord> {

		@Override
		public SessionEventRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			return new SessionEventRecord(resultSet.getString("delivery_id"), resultSet.getString("event_type"),
					resultSet.getString("processing_status"), resultSet.getObject("received_at", OffsetDateTime.class));
		}
	}
}
