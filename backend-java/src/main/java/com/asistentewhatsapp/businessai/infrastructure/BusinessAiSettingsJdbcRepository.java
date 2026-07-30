package com.asistentewhatsapp.businessai.infrastructure;

import com.asistentewhatsapp.businessai.api.BusinessAiSettingsResponse;
import com.asistentewhatsapp.businessai.api.PromptTemplateResponse;
import com.asistentewhatsapp.businessai.api.UpsertBusinessAiSettingsRequest;
import com.asistentewhatsapp.businessai.api.UpsertPromptTemplateRequest;
import com.asistentewhatsapp.shared.exception.ConflictException;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BusinessAiSettingsJdbcRepository {

	private final NamedParameterJdbcTemplate jdbc;
	private final ObjectMapper objectMapper;

	public BusinessAiSettingsJdbcRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
		this.jdbc = jdbc;
		this.objectMapper = objectMapper;
	}

	public Optional<BusinessAiSettingsResponse> findSettings(UUID businessId) {
		var sql = """
				select id, business_id, active, mode, tone, language,
				       escalation_threshold, allow_prices, allow_booking, allow_promotions,
				       require_availability_check, allowed_topics::text as allowed_topics,
				       blocked_topics::text as blocked_topics, active_prompt_version,
				       updated_by, created_at, updated_at
				from business_ai_settings
				where business_id = :businessId
				""";
		var params = new MapSqlParameterSource("businessId", businessId);
		List<BusinessAiSettingsResponse> results = jdbc.query(sql, params, settingsRowMapper());
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	public BusinessAiSettingsResponse upsertSettings(UUID businessId, UUID userId,
			UpsertBusinessAiSettingsRequest request) {
		var existing = findSettings(businessId);
		if (existing.isPresent()) {
			return updateSettings(businessId, userId, request, existing.get().id());
		}
		return insertSettings(businessId, userId, request);
	}

	private BusinessAiSettingsResponse insertSettings(UUID businessId, UUID userId,
			UpsertBusinessAiSettingsRequest request) {
		UUID id = UUID.randomUUID();
		var sql = """
				insert into business_ai_settings (
				    id, business_id, active, mode, tone, language,
				    escalation_threshold, allow_prices, allow_booking, allow_promotions,
				    require_availability_check, allowed_topics, blocked_topics, updated_by
				) values (
				    :id, :businessId, :active, :mode, :tone, :language,
				    :escalationThreshold, :allowPrices, :allowBooking, :allowPromotions,
				    :requireAvailabilityCheck, cast(:allowedTopics as jsonb), cast(:blockedTopics as jsonb), :updatedBy
				)
				""";
		var params = buildParams(id, businessId, userId, request);
		try {
			jdbc.update(sql, params);
		} catch (DuplicateKeyException e) {
			throw new ConflictException("Ya existe una configuracion de IA para esta empresa.",
					Map.of("businessId", businessId.toString()));
		}
		return findSettings(businessId).orElseThrow();
	}

	private BusinessAiSettingsResponse updateSettings(UUID businessId, UUID userId,
			UpsertBusinessAiSettingsRequest request, UUID id) {
		var sql = """
				update business_ai_settings set
				    active = :active,
				    mode = :mode,
				    tone = :tone,
				    language = :language,
				    escalation_threshold = :escalationThreshold,
				    allow_prices = :allowPrices,
				    allow_booking = :allowBooking,
				    allow_promotions = :allowPromotions,
				    require_availability_check = :requireAvailabilityCheck,
				    allowed_topics = cast(:allowedTopics as jsonb),
				    blocked_topics = cast(:blockedTopics as jsonb),
				    updated_by = :updatedBy,
				    updated_at = :updatedAt
				where id = :id and business_id = :businessId
				""";
		var params = buildParams(id, businessId, userId, request).addValue("updatedAt", OffsetDateTime.now());
		int updated = jdbc.update(sql, params);
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro la configuracion de IA para esta empresa.");
		}
		return findSettings(businessId).orElseThrow();
	}

	public List<PromptTemplateResponse> findPrompts(UUID businessId) {
		var sql = """
				select id, business_id, codigo, nombre, descripcion, modulo, tipo,
				       contenido, prioridad, activo, version, fecha_creacion, fecha_actualizacion
				from ai_prompt_template
				where business_id = :businessId
				order by prioridad, version desc
				""";
		var params = new MapSqlParameterSource("businessId", businessId);
		return jdbc.query(sql, params, promptRowMapper());
	}

	public PromptTemplateResponse insertPrompt(UUID businessId, UpsertPromptTemplateRequest request) {
		UUID id = UUID.randomUUID();
		var sql = """
				insert into ai_prompt_template (
				    id, business_id, codigo, nombre, descripcion, modulo, tipo,
				    contenido, prioridad, activo, version
				) values (
				    :id, :businessId, :codigo, :nombre, :descripcion, :modulo, :tipo,
				    :contenido, :prioridad, true, coalesce(
				        (select max(pt.version) + 1 from ai_prompt_template pt
				         where pt.business_id = :businessId and pt.codigo = :codigo), 1
				    )
				)
				""";
		var params = new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
				.addValue("codigo", request.codigo()).addValue("nombre", request.nombre())
				.addValue("descripcion", request.descripcion()).addValue("modulo", request.modulo())
				.addValue("tipo", request.tipo()).addValue("contenido", request.contenido())
				.addValue("prioridad", request.prioridad());
		try {
			jdbc.update(sql, params);
		} catch (DuplicateKeyException e) {
			throw new ConflictException("Ya existe un prompt con ese codigo y version para esta empresa.",
					Map.of("codigo", request.codigo()));
		}
		return findPrompt(id).orElseThrow();
	}

	public void activatePrompt(UUID businessId, UUID promptId) {
		var prompt = findPrompt(promptId)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontro el prompt solicitado."));
		if (!prompt.businessId().equals(businessId)) {
			throw new ResourceNotFoundException("No se encontro el prompt solicitado para esta empresa.");
		}
		var sql = """
				update business_ai_settings
				set active_prompt_version = :version,
				    updated_at = :now
				where business_id = :businessId
				""";
		var params = new MapSqlParameterSource().addValue("version", prompt.version())
				.addValue("now", OffsetDateTime.now()).addValue("businessId", businessId);
		jdbc.update(sql, params);
	}

	private Optional<PromptTemplateResponse> findPrompt(UUID promptId) {
		var sql = """
				select id, business_id, codigo, nombre, descripcion, modulo, tipo,
				       contenido, prioridad, activo, version, fecha_creacion, fecha_actualizacion
				from ai_prompt_template
				where id = :id
				""";
		var params = new MapSqlParameterSource("id", promptId);
		List<PromptTemplateResponse> results = jdbc.query(sql, params, promptRowMapper());
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	private MapSqlParameterSource buildParams(UUID id, UUID businessId, UUID userId,
			UpsertBusinessAiSettingsRequest request) {
		return new MapSqlParameterSource().addValue("id", id).addValue("businessId", businessId)
				.addValue("active", request.active()).addValue("mode", request.mode()).addValue("tone", request.tone())
				.addValue("language", request.language()).addValue("escalationThreshold", request.escalationThreshold())
				.addValue("allowPrices", request.allowPrices()).addValue("allowBooking", request.allowBooking())
				.addValue("allowPromotions", request.allowPromotions())
				.addValue("requireAvailabilityCheck", request.requireAvailabilityCheck())
				.addValue("allowedTopics", toJson(request.allowedTopics()))
				.addValue("blockedTopics", toJson(request.blockedTopics())).addValue("updatedBy", userId);
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error serializando JSON", e);
		}
	}

	private RowMapper<BusinessAiSettingsResponse> settingsRowMapper() {
		return (rs, rowNum) -> {
			try {
				return new BusinessAiSettingsResponse(UUID.fromString(rs.getString("id")),
						UUID.fromString(rs.getString("business_id")), rs.getBoolean("active"), rs.getString("mode"),
						rs.getString("tone"), rs.getString("language"), rs.getBigDecimal("escalation_threshold"),
						rs.getBoolean("allow_prices"), rs.getBoolean("allow_booking"),
						rs.getBoolean("allow_promotions"), rs.getBoolean("require_availability_check"),
						objectMapper.readValue(rs.getString("allowed_topics"),
								objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)),
						objectMapper.readValue(rs.getString("blocked_topics"),
								objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)),
						rs.getObject("active_prompt_version", Integer.class), rs.getObject("updated_by", UUID.class),
						rs.getObject("created_at", OffsetDateTime.class),
						rs.getObject("updated_at", OffsetDateTime.class));
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Error deserializando JSON", e);
			}
		};
	}

	private RowMapper<PromptTemplateResponse> promptRowMapper() {
		return (rs, rowNum) -> new PromptTemplateResponse(UUID.fromString(rs.getString("id")),
				UUID.fromString(rs.getString("business_id")), rs.getString("codigo"), rs.getString("nombre"),
				rs.getString("descripcion"), rs.getString("modulo"), rs.getString("tipo"), rs.getString("contenido"),
				rs.getInt("prioridad"), rs.getBoolean("activo"), rs.getInt("version"),
				rs.getObject("fecha_creacion", OffsetDateTime.class),
				rs.getObject("fecha_actualizacion", OffsetDateTime.class));
	}
}
