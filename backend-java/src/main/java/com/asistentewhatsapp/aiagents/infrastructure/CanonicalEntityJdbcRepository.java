package com.asistentewhatsapp.aiagents.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CanonicalEntityJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public CanonicalEntityJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<CanonicalEntityRecord> findActive(UUID businessId) {
		if (businessId == null) {
			return List.of();
		}
		return jdbcTemplate.query("""
				select id,
				       entity_type,
				       reference_type,
				       reference_id,
				       canonical_name,
				       display_name,
				       priority
				from ai_canonical_entity
				where (business_id = ? or business_id is null)
				  and active = true
				order by priority desc, length(canonical_name) desc
				""",
				(rs, rowNum) -> new CanonicalEntityRecord(rs.getObject("id", UUID.class), rs.getString("entity_type"),
						rs.getString("reference_type"), rs.getObject("reference_id", UUID.class),
						rs.getString("canonical_name"), rs.getString("display_name"), rs.getInt("priority")),
				businessId);
	}

	public Optional<UUID> findIdByCanonicalName(UUID businessId, String canonicalName) {
		if (businessId == null || canonicalName == null || canonicalName.isBlank()) {
			return Optional.empty();
		}
		return jdbcTemplate.query("""
				select id
				from ai_canonical_entity
				where (business_id = ? or business_id is null)
				  and canonical_name = ?
				  and active = true
				order by case when business_id = ? then 0 else 1 end
				limit 1
				""", (rs, rowNum) -> rs.getObject("id", UUID.class), businessId,
				canonicalName.toLowerCase(java.util.Locale.ROOT), businessId).stream().findFirst();
	}

	public void insertDetectedEntity(UUID messageAnalysisId, DetectedEntityRecord entity) {
		jdbcTemplate.update("""
				insert into ai_detected_entity (
				    id,
				    message_analysis_id,
				    business_id,
				    canonical_entity_id,
				    entity_type,
				    entity_key,
				    entity_value,
				    resolution_method,
				    matched_alias,
				    confidence,
				    reference_type,
				    reference_id
				) values (
				    gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
				)
				""", messageAnalysisId, entity.businessId(), entity.canonicalEntityId(), entity.entityType(),
				entity.entityKey(), entity.entityValue(), entity.resolutionMethod(), entity.matchedAlias(),
				entity.confidence(), entity.referenceType(), entity.referenceId());
	}

	public List<CanonicalAliasRecord> findActiveAliases(UUID businessId) {
		if (businessId == null) {
			return List.of();
		}
		return jdbcTemplate.query("""
				select c.id,
				       c.canonical_name,
				       c.display_name,
				       c.entity_type,
				       a.alias,
				       a.alias_type,
				       a.confidence_base,
				       c.reference_type,
				       c.reference_id
				from ai_entity_alias a
				join ai_canonical_entity c on c.id = a.canonical_entity_id
				where (c.business_id = ? or c.business_id is null)
				  and c.active = true
				  and (a.valid_from is null or a.valid_from <= now())
				  and (a.valid_until is null or a.valid_until >= now())
				order by c.priority desc, length(a.alias) desc
				""",
				(rs, rowNum) -> new CanonicalAliasRecord(rs.getObject("id", UUID.class), rs.getString("canonical_name"),
						rs.getString("display_name"), rs.getString("entity_type"), rs.getString("alias"),
						rs.getString("alias_type"), rs.getBigDecimal("confidence_base"), rs.getString("reference_type"),
						rs.getObject("reference_id", UUID.class)),
				businessId);
	}

	public record CanonicalEntityRecord(UUID id, String entityType, String referenceType, UUID referenceId,
			String canonicalName, String displayName, Integer priority) {
	}

	public record CanonicalAliasRecord(UUID canonicalEntityId, String canonicalName, String displayName,
			String entityType, String alias, String aliasType, BigDecimal confidenceBase, String referenceType,
			UUID referenceId) {
	}

	public record DetectedEntityRecord(UUID businessId, UUID canonicalEntityId, String entityType, String entityKey,
			String entityValue, String resolutionMethod, String matchedAlias, BigDecimal confidence,
			String referenceType, UUID referenceId) {
	}
}
