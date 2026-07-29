package com.asistentewhatsapp.bookings.infrastructure;

import com.asistentewhatsapp.bookings.domain.BookingPolicyRecord;
import com.asistentewhatsapp.bookings.domain.PolicySnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BusinessPolicyJdbcRepository {

	private static final Logger LOG = LoggerFactory.getLogger(BusinessPolicyJdbcRepository.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public BusinessPolicyJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public UUID findActiveVersionId(UUID businessId, OffsetDateTime at) {
		List<UUID> result = jdbcTemplate.query("""
				select id from business_policy_version
				where business_id = :businessId
				  and effective_from <= :at
				  and (effective_until is null or effective_until > :at)
				order by version desc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("at", at),
				(rs, rowNum) -> rs.getObject("id", UUID.class));
		return result.isEmpty() ? null : result.getFirst();
	}

	public PolicySnapshot buildSnapshot(UUID businessId, UUID locationId, UUID versionId) {
		List<BookingPolicyRecord> policies = jdbcTemplate.query("""
				select bp.id, bp.version_id, bp.location_id,
				       bp.policy_type, bp.policy_key, bp.policy_value::text,
				       bp.priority, bp.active
				from business_policy bp
				where bp.version_id = :versionId
				  and bp.active = true
				  and (bp.location_id is null or bp.location_id = :locationId)
				order by bp.location_id nulls last, bp.priority asc
				""", new MapSqlParameterSource().addValue("versionId", versionId).addValue("locationId", locationId),
				policyRowMapper());

		Integer cancellationWindowHours = intFromPolicy(policies, "CANCELLATION", "window_hours");
		Integer rescheduleWindowHours = intFromPolicy(policies, "RESCHEDULE", "window_hours");
		Integer maxAdvanceDays = intFromPolicy(policies, "MAX_ADVANCE", "max_days");
		Integer minAdvanceMinutes = intFromPolicy(policies, "MIN_ADVANCE", "min_minutes");
		Integer gracePeriodMinutes = intFromPolicy(policies, "TOLERANCE", "grace_period_minutes");
		Integer autoExpireMinutes = intFromPolicy(policies, "TOLERANCE", "auto_expire_minutes");
		Integer toleranceMinutes = gracePeriodMinutes;
		Integer rescheduleMaxCount = intFromPolicy(policies, "RESCHEDULE", "max_count");

		Integer slotStepMinutes = intFromPolicy(policies, "SLOT_CONFIG", "slot_step_minutes");

		String penaltyType = strFromPolicy(policies, "PENALTY", "type");
		BigDecimal penaltyPercent = decFromPolicy(policies, "PENALTY", "percent");
		BigDecimal penaltyFixedAmount = decFromPolicy(policies, "PENALTY", "fixed_amount");
		String penaltyCurrency = strFromPolicy(policies, "PENALTY", "currency");

		return new PolicySnapshot(versionId, cancellationWindowHours, rescheduleWindowHours, maxAdvanceDays,
				minAdvanceMinutes, toleranceMinutes, gracePeriodMinutes, autoExpireMinutes, rescheduleMaxCount,
				penaltyType, penaltyPercent, penaltyFixedAmount, penaltyCurrency, slotStepMinutes);
	}

	public void updateBookingPolicy(UUID bookingId, UUID policyVersionId, PolicySnapshot snapshot) {
		String json;
		try {
			json = OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException e) {
			LOG.warn("POLICY_SNAPSHOT_SERIALIZE_FAILED bookingId={} reason={}", bookingId, e.getMessage());
			return;
		}
		jdbcTemplate.update("""
				update booking
				set policy_version_id = :policyVersionId,
				    policy_snapshot = :snapshot::jsonb
				where id = :bookingId
				""", new MapSqlParameterSource().addValue("policyVersionId", policyVersionId).addValue("snapshot", json)
				.addValue("bookingId", bookingId));
	}

	public boolean hasLocationOverride(UUID versionId, UUID locationId, String policyType) {
		Integer count = jdbcTemplate.queryForObject("""
				select count(*) from business_policy
				where version_id = :versionId
				  and location_id = :locationId
				  and policy_type = :policyType
				  and active = true
				""", new MapSqlParameterSource().addValue("versionId", versionId).addValue("locationId", locationId)
				.addValue("policyType", policyType), Integer.class);
		return count != null && count > 0;
	}

	private static Integer intFromPolicy(List<BookingPolicyRecord> policies, String type, String key) {
		return policies.stream().filter(p -> p.policyType().equals(type)).findFirst()
				.map(p -> extractInt(p.policyValue(), key)).orElse(null);
	}

	private static String strFromPolicy(List<BookingPolicyRecord> policies, String type, String key) {
		return policies.stream().filter(p -> p.policyType().equals(type)).findFirst()
				.map(p -> extractStr(p.policyValue(), key)).orElse(null);
	}

	private static BigDecimal decFromPolicy(List<BookingPolicyRecord> policies, String type, String key) {
		return policies.stream().filter(p -> p.policyType().equals(type)).findFirst()
				.map(p -> extractDec(p.policyValue(), key)).orElse(null);
	}

	private static Integer extractInt(String json, String key) {
		try {
			JsonNode node = OBJECT_MAPPER.readTree(json).get(key);
			return node != null && !node.isNull() ? node.asInt() : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String extractStr(String json, String key) {
		try {
			JsonNode node = OBJECT_MAPPER.readTree(json).get(key);
			return node != null && !node.isNull() ? node.asText() : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static BigDecimal extractDec(String json, String key) {
		try {
			JsonNode node = OBJECT_MAPPER.readTree(json).get(key);
			return node != null && !node.isNull() ? new BigDecimal(node.asText()) : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static RowMapper<BookingPolicyRecord> policyRowMapper() {
		return (rs, rowNum) -> new BookingPolicyRecord(rs.getObject("id", UUID.class),
				rs.getObject("version_id", UUID.class), rs.getObject("location_id", UUID.class),
				rs.getString("policy_type"), rs.getString("policy_key"), rs.getString("policy_value"),
				rs.getInt("priority"), rs.getBoolean("active"));
	}
}
