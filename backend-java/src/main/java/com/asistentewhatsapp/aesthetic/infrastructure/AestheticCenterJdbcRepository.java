package com.asistentewhatsapp.aesthetic.infrastructure;

import com.asistentewhatsapp.aesthetic.api.AestheticBusinessRuleResponse;
import com.asistentewhatsapp.aesthetic.api.UpsertAestheticServiceRequest;
import com.asistentewhatsapp.aesthetic.api.UpsertAestheticProductRequest;
import com.asistentewhatsapp.aesthetic.api.UpsertAestheticBusinessRuleRequest;
import com.asistentewhatsapp.aesthetic.api.AestheticCategoryResponse;
import com.asistentewhatsapp.aesthetic.api.AestheticIntentLogResponse;
import com.asistentewhatsapp.aesthetic.api.AestheticProductResponse;
import com.asistentewhatsapp.aesthetic.api.AestheticServiceResponse;
import com.asistentewhatsapp.aesthetic.api.IntentAnalysisResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AestheticCenterJdbcRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public AestheticCenterJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public PagedResponse<AestheticCategoryResponse> findServiceCategories(UUID businessId, int page, int size,
			Boolean active) {
		return findCategories("aesthetic_service_category", businessId, page, size, active);
	}

	public PagedResponse<AestheticCategoryResponse> findProductCategories(UUID businessId, int page, int size,
			Boolean active) {
		return findCategories("aesthetic_product_category", businessId, page, size, active);
	}

	private PagedResponse<AestheticCategoryResponse> findCategories(String tableName, UUID businessId, int page,
			int size, Boolean active) {
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("limit", size).addValue("offset", page * size);
		StringBuilder where = new StringBuilder(" where business_id = :businessId\n");
		if (active != null) {
			where.append(" and active = :active\n");
			parameters.addValue("active", active);
		}
		Long totalItems = jdbcTemplate.queryForObject("select count(*) from " + tableName + where, parameters,
				Long.class);
		long resolvedTotalItems = totalItems == null ? 0 : totalItems;
		boolean isServiceCategory = tableName.equals("aesthetic_service_category");
		String displayOrderCol = isServiceCategory ? ", display_order" : "";
		List<AestheticCategoryResponse> items = jdbcTemplate.query("""
				select id, code, name, description, active%s
				from %s
				%s
				order by name asc
				limit :limit
				offset :offset
				""".formatted(displayOrderCol, tableName, where), parameters, categoryRowMapper());
		return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages(resolvedTotalItems, size));
	}

	public PagedResponse<AestheticServiceResponse> findServices(UUID businessId, int page, int size, String search,
			String categoryCode, Boolean active) {
		QueryParts queryParts = buildServicesQuery(businessId, search, categoryCode, active);
		long totalItems = count(queryParts);
		int totalPages = totalPages(totalItems, size);
		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);

		List<AestheticServiceResponse> items = jdbcTemplate.query(serviceSelect() + queryParts.fromAndWhere() + """
				order by c.name asc, s.name asc
				limit :limit
				offset :offset
				""", parameters, serviceRowMapper());
		items = enrichServicesWithAssignments(businessId, items);
		return new PagedResponse<>(items, page, size, totalItems, totalPages);
	}

	private List<AestheticServiceResponse> enrichServicesWithAssignments(UUID businessId,
			List<AestheticServiceResponse> services) {
		if (services.isEmpty()) {
			return services;
		}
		List<UUID> serviceIds = services.stream().map(AestheticServiceResponse::id).toList();
		MapSqlParameterSource batchParams = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("serviceIds", serviceIds);

		MapSqlParameterSource professionalParams = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("serviceIds", serviceIds);
		List<AssignmentIdRecord> professionalAssignments = jdbcTemplate.query("""
				select service_id, professional_id
				from agenda_professional_service
				where business_id = :businessId
				  and service_id in (:serviceIds)
				  and active = true
				""", professionalParams, (rs, rowNum) -> new AssignmentIdRecord(rs.getObject("service_id", UUID.class),
				rs.getObject("professional_id", UUID.class)));

		List<AssignmentIdRecord> roomAssignments = jdbcTemplate.query("""
				select service_id, room_id
				from agenda_room_service
				where business_id = :businessId
				  and service_id in (:serviceIds)
				  and active = true
				""", batchParams, (rs, rowNum) -> new AssignmentIdRecord(rs.getObject("service_id", UUID.class),
				rs.getObject("room_id", UUID.class)));

		java.util.Map<UUID, List<UUID>> profMap = new java.util.HashMap<>();
		java.util.Map<UUID, List<UUID>> roomMap = new java.util.HashMap<>();
		for (AssignmentIdRecord rec : professionalAssignments) {
			profMap.computeIfAbsent(rec.serviceId(), k -> new ArrayList<>()).add(rec.assignedId());
		}
		for (AssignmentIdRecord rec : roomAssignments) {
			roomMap.computeIfAbsent(rec.serviceId(), k -> new ArrayList<>()).add(rec.assignedId());
		}

		return services.stream().map(svc -> {
			List<UUID> profIds = profMap.getOrDefault(svc.id(), List.of());
			List<UUID> rmIds = roomMap.getOrDefault(svc.id(), List.of());
			return new AestheticServiceResponse(svc.id(), svc.code(), svc.name(), svc.description(), svc.categoryCode(),
					svc.categoryName(), svc.durationMinutes(), svc.priceBase(), svc.professionalRequired(),
					svc.supplies(), svc.contraindications(), svc.availabilityRules(), svc.bookingRules(),
					svc.cancellationRules(), svc.aftercareRecommendations(), svc.requiresPriorEvaluation(),
					svc.requiresInformedConsent(), svc.active(), svc.createdAt(), svc.updatedAt(), profIds, rmIds);
		}).toList();
	}

	private record AssignmentIdRecord(UUID serviceId, UUID assignedId) {
	}

	public AestheticServiceResponse findService(UUID businessId, UUID serviceId) {
		List<AestheticServiceResponse> items = jdbcTemplate.query(serviceSelect() + """
				from aesthetic_service s
				join aesthetic_service_category c
				  on c.id = s.category_id
				 and c.business_id = s.business_id
				where s.business_id = :businessId
				  and s.id = :serviceId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId),
				serviceRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el servicio estetico solicitado.");
		}
		AestheticServiceResponse response = items.getFirst();
		return new AestheticServiceResponse(response.id(), response.code(), response.name(), response.description(),
				response.categoryCode(), response.categoryName(), response.durationMinutes(), response.priceBase(),
				response.professionalRequired(), response.supplies(), response.contraindications(),
				response.availabilityRules(), response.bookingRules(), response.cancellationRules(),
				response.aftercareRecommendations(), response.requiresPriorEvaluation(),
				response.requiresInformedConsent(), response.active(), response.createdAt(), response.updatedAt(),
				findServiceProfessionalIds(businessId, serviceId), findServiceRoomIds(businessId, serviceId));
	}

	public AestheticServiceResponse insertService(UUID businessId, UpsertAestheticServiceRequest request) {
		UUID serviceId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into aesthetic_service (
						    id, business_id, category_id, code, name, description, duration_minutes, price_base,
						    professional_required, supplies, contraindications, availability_rules, booking_rules,
						    cancellation_rules, aftercare_recommendations, requires_prior_evaluation,
						    requires_informed_consent, active
						)
						values (
						    :serviceId, :businessId,
						    (select id from aesthetic_service_category where business_id = :businessId and code = :categoryCode),
						    :code, :name, :description, :durationMinutes, :priceBase, :professionalRequired,
						    :supplies, :contraindications, :availabilityRules, :bookingRules, :cancellationRules,
						    :aftercareRecommendations, :requiresPriorEvaluation, :requiresInformedConsent, :active
						)
						""",
				serviceParameters(businessId, serviceId, request));
		syncServiceAssignments(businessId, serviceId, request.professionalIds(), request.roomIds());
		return findService(businessId, serviceId);
	}

	public AestheticServiceResponse updateService(UUID businessId, UUID serviceId,
			UpsertAestheticServiceRequest request) {
		int updated = jdbcTemplate.update(
				"""
						update aesthetic_service
						set category_id = (select id from aesthetic_service_category where business_id = :businessId and code = :categoryCode),
						    code = :code,
						    name = :name,
						    description = :description,
						    duration_minutes = :durationMinutes,
						    price_base = :priceBase,
						    professional_required = :professionalRequired,
						    supplies = :supplies,
						    contraindications = :contraindications,
						    availability_rules = :availabilityRules,
						    booking_rules = :bookingRules,
						    cancellation_rules = :cancellationRules,
						    aftercare_recommendations = :aftercareRecommendations,
						    requires_prior_evaluation = :requiresPriorEvaluation,
						    requires_informed_consent = :requiresInformedConsent,
						    active = :active,
						    updated_at = current_timestamp
						where business_id = :businessId
						  and id = :serviceId
						""",
				serviceParameters(businessId, serviceId, request));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro el servicio estetico solicitado.");
		}
		syncServiceAssignments(businessId, serviceId, request.professionalIds(), request.roomIds());
		return findService(businessId, serviceId);
	}

	public Optional<AestheticServiceResponse> findServiceByText(UUID businessId, String normalizedText) {
		List<AestheticServiceResponse> items = jdbcTemplate.query(serviceSelect() + """
				from aesthetic_service s
				join aesthetic_service_category c
				  on c.id = s.category_id
				 and c.business_id = s.business_id
				where s.business_id = :businessId
				  and s.active = true
				  and (
				      lower(s.name) like :search
				      or lower(s.description) like :search
				      or lower(c.name) like :search
				  )
				order by length(s.name) asc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("search",
				"%" + normalizedText + "%"), serviceRowMapper());
		return items.stream().findFirst();
	}

	public PagedResponse<AestheticProductResponse> findProducts(UUID businessId, int page, int size, String search,
			String categoryCode, Boolean active, Boolean lowStockOnly) {
		QueryParts queryParts = buildProductsQuery(businessId, search, categoryCode, active, lowStockOnly);
		long totalItems = count(queryParts);
		int totalPages = totalPages(totalItems, size);
		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);

		List<AestheticProductResponse> items = jdbcTemplate.query(productSelect() + queryParts.fromAndWhere() + """
				order by c.name asc, p.name asc
				limit :limit
				offset :offset
				""", parameters, productRowMapper());
		return new PagedResponse<>(items, page, size, totalItems, totalPages);
	}

	public AestheticProductResponse findProduct(UUID businessId, UUID productId) {
		List<AestheticProductResponse> items = jdbcTemplate.query(productSelect() + """
				from aesthetic_product p
				join aesthetic_product_category c
				  on c.id = p.category_id
				 and c.business_id = p.business_id
				where p.business_id = :businessId
				  and p.id = :productId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("productId", productId),
				productRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro el producto estetico solicitado.");
		}
		return items.getFirst();
	}

	public AestheticProductResponse insertProduct(UUID businessId, UpsertAestheticProductRequest request) {
		UUID productId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into aesthetic_product (
						    id, business_id, category_id, code, name, description, price, stock, stock_minimum,
						    supplier, expiration_date, compatible_services, recommendation_rules, cross_sell_rules,
						    usage_restrictions, active
						)
						values (
						    :productId, :businessId,
						    (select id from aesthetic_product_category where business_id = :businessId and code = :categoryCode),
						    :code, :name, :description, :price, :stock, :stockMinimum, :supplier, :expirationDate,
						    :compatibleServices, :recommendationRules, :crossSellRules, :usageRestrictions, :active
						)
						""",
				productParameters(businessId, productId, request));
		return findProduct(businessId, productId);
	}

	public AestheticProductResponse updateProduct(UUID businessId, UUID productId,
			UpsertAestheticProductRequest request) {
		int updated = jdbcTemplate.update(
				"""
						update aesthetic_product
						set category_id = (select id from aesthetic_product_category where business_id = :businessId and code = :categoryCode),
						    code = :code,
						    name = :name,
						    description = :description,
						    price = :price,
						    stock = :stock,
						    stock_minimum = :stockMinimum,
						    supplier = :supplier,
						    expiration_date = :expirationDate,
						    compatible_services = :compatibleServices,
						    recommendation_rules = :recommendationRules,
						    cross_sell_rules = :crossSellRules,
						    usage_restrictions = :usageRestrictions,
						    active = :active,
						    updated_at = current_timestamp
						where business_id = :businessId
						  and id = :productId
						""",
				productParameters(businessId, productId, request));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro el producto estetico solicitado.");
		}
		return findProduct(businessId, productId);
	}

	public Optional<AestheticProductResponse> findProductByText(UUID businessId, String normalizedText) {
		List<AestheticProductResponse> items = jdbcTemplate.query(productSelect() + """
				from aesthetic_product p
				join aesthetic_product_category c
				  on c.id = p.category_id
				 and c.business_id = p.business_id
				where p.business_id = :businessId
				  and p.active = true
				  and (
				      lower(p.name) like :search
				      or lower(p.description) like :search
				      or lower(c.name) like :search
				  )
				order by length(p.name) asc
				limit 1
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("search",
				"%" + normalizedText + "%"), productRowMapper());
		return items.stream().findFirst();
	}

	public PagedResponse<AestheticBusinessRuleResponse> findRules(UUID businessId, int page, int size, String ruleType,
			Boolean active) {
		QueryParts queryParts = buildRulesQuery(businessId, ruleType, active);
		long totalItems = count(queryParts);
		int totalPages = totalPages(totalItems, size);
		MapSqlParameterSource parameters = queryParts.parameters().addValue("limit", size).addValue("offset",
				page * size);
		List<AestheticBusinessRuleResponse> items = jdbcTemplate.query("""
				select
				    id,
				    code,
				    name,
				    rule_type,
				    description,
				    priority,
				    active,
				    rule_payload::text as rule_payload,
				    created_at,
				    updated_at
				""" + queryParts.fromAndWhere() + """
				order by priority asc, name asc
				limit :limit
				offset :offset
				""", parameters, ruleRowMapper());
		return new PagedResponse<>(items, page, size, totalItems, totalPages);
	}

	public AestheticBusinessRuleResponse findRule(UUID businessId, UUID ruleId) {
		List<AestheticBusinessRuleResponse> items = jdbcTemplate.query("""
				select
				    id,
				    code,
				    name,
				    rule_type,
				    description,
				    priority,
				    active,
				    rule_payload::text as rule_payload,
				    created_at,
				    updated_at
				from aesthetic_business_rule
				where business_id = :businessId
				  and id = :ruleId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("ruleId", ruleId),
				ruleRowMapper());
		if (items.isEmpty()) {
			throw new ResourceNotFoundException("No se encontro la regla solicitada.");
		}
		return items.getFirst();
	}

	public AestheticBusinessRuleResponse insertRule(UUID businessId, UpsertAestheticBusinessRuleRequest request) {
		UUID ruleId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
						insert into aesthetic_business_rule (
						    id, business_id, code, name, rule_type, description, priority, active, rule_payload
						)
						values (
						    :ruleId, :businessId, :code, :name, :ruleType, :description, :priority, :active, cast(:rulePayload as jsonb)
						)
						""",
				ruleParameters(businessId, ruleId, request));
		return findRule(businessId, ruleId);
	}

	public AestheticBusinessRuleResponse updateRule(UUID businessId, UUID ruleId,
			UpsertAestheticBusinessRuleRequest request) {
		int updated = jdbcTemplate.update("""
				update aesthetic_business_rule
				set code = :code,
				    name = :name,
				    rule_type = :ruleType,
				    description = :description,
				    priority = :priority,
				    active = :active,
				    rule_payload = cast(:rulePayload as jsonb),
				    updated_at = current_timestamp
				where business_id = :businessId
				  and id = :ruleId
				""", ruleParameters(businessId, ruleId, request));
		if (updated == 0) {
			throw new ResourceNotFoundException("No se encontro la regla solicitada.");
		}
		return findRule(businessId, ruleId);
	}

	public List<AestheticPromotionSummary> findActivePromotions(UUID businessId) {
		return jdbcTemplate.query("""
				select
				    code,
				    name,
				    description,
				    discount_type,
				    discount_value,
				    starts_on,
				    ends_on,
				    conditions
				from aesthetic_promotion
				where business_id = :businessId
				  and active = true
				  and (starts_on is null or starts_on <= current_date)
				  and (ends_on is null or ends_on >= current_date)
				order by starts_on nulls first, name asc
				limit 30
				""", new MapSqlParameterSource().addValue("businessId", businessId),
				(resultSet, rowNumber) -> new AestheticPromotionSummary(resultSet.getString("code"),
						resultSet.getString("name"), resultSet.getString("description"),
						resultSet.getString("discount_type"), resultSet.getBigDecimal("discount_value"),
						resultSet.getObject("starts_on", LocalDate.class),
						resultSet.getObject("ends_on", LocalDate.class), resultSet.getString("conditions")));
	}

	public boolean customerExists(UUID businessId, UUID customerId) {
		if (customerId == null) {
			return false;
		}
		Long count = jdbcTemplate.queryForObject("""
				select count(*)
				from customer
				where business_id = :businessId
				  and id = :customerId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("customerId", customerId),
				Long.class);
		return count != null && count > 0;
	}

	public boolean conversationExists(UUID businessId, UUID conversationId) {
		if (conversationId == null) {
			return false;
		}
		Long count = jdbcTemplate.queryForObject("""
				select count(*)
				from conversation
				where business_id = :businessId
				  and id = :conversationId
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("conversationId",
				conversationId), Long.class);
		return count != null && count > 0;
	}

	public void insertIntentLog(UUID businessId, UUID customerId, UUID conversationId, String sourceMessage,
			IntentAnalysisResponse response) {
		jdbcTemplate.update("""
				insert into aesthetic_intent_log (
				    id,
				    business_id,
				    customer_id,
				    conversation_id,
				    source_message,
				    intent,
				    confidence,
				    entities,
				    requires_database_lookup,
				    requires_human_handoff,
				    handoff_reason,
				    suggested_response,
				    model_name
				)
				values (
				    :id,
				    :businessId,
				    :customerId,
				    :conversationId,
				    :sourceMessage,
				    :intent,
				    :confidence,
				    cast(:entities as jsonb),
				    :requiresDatabaseLookup,
				    :requiresHumanHandoff,
				    :handoffReason,
				    :suggestedResponse,
				    :modelName
				)
				""", new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("businessId", businessId)
				.addValue("customerId", customerId).addValue("conversationId", conversationId)
				.addValue("sourceMessage", sourceMessage).addValue("intent", response.intencion())
				.addValue("confidence", response.confianza()).addValue("entities", toJson(response.entidades()))
				.addValue("requiresDatabaseLookup", response.requiereConsultaBaseDatos())
				.addValue("requiresHumanHandoff", response.requiereDerivacionHumana())
				.addValue("handoffReason", response.motivoDerivacion())
				.addValue("suggestedResponse", response.respuestaSugerida()).addValue("modelName", response.modelo()));
	}

	public PagedResponse<AestheticIntentLogResponse> findIntentLogs(UUID businessId, int page, int size) {
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId)
				.addValue("limit", size).addValue("offset", page * size);
		Long totalItems = jdbcTemplate.queryForObject(
				"select count(*) from aesthetic_intent_log where business_id = :businessId", parameters, Long.class);
		long resolvedTotalItems = totalItems == null ? 0 : totalItems;
		List<AestheticIntentLogResponse> items = jdbcTemplate.query("""
				select
				    id,
				    source_message,
				    intent,
				    confidence,
				    entities::text as entities,
				    requires_database_lookup,
				    requires_human_handoff,
				    handoff_reason,
				    suggested_response,
				    model_name,
				    created_at
				from aesthetic_intent_log
				where business_id = :businessId
				order by created_at desc
				limit :limit
				offset :offset
				""", parameters, intentLogRowMapper());
		return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages(resolvedTotalItems, size));
	}

	private MapSqlParameterSource serviceParameters(UUID businessId, UUID serviceId,
			UpsertAestheticServiceRequest request) {
		return new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId)
				.addValue("categoryCode", request.categoryCode()).addValue("code", request.code())
				.addValue("name", request.name()).addValue("description", request.description())
				.addValue("durationMinutes", request.durationMinutes()).addValue("priceBase", request.priceBase())
				.addValue("professionalRequired", request.professionalRequired())
				.addValue("supplies", request.supplies()).addValue("contraindications", request.contraindications())
				.addValue("availabilityRules", request.availabilityRules())
				.addValue("bookingRules", request.bookingRules())
				.addValue("cancellationRules", request.cancellationRules())
				.addValue("aftercareRecommendations", request.aftercareRecommendations())
				.addValue("requiresPriorEvaluation", request.requiresPriorEvaluation())
				.addValue("requiresInformedConsent", request.requiresInformedConsent())
				.addValue("active", request.active());
	}

	private MapSqlParameterSource productParameters(UUID businessId, UUID productId,
			UpsertAestheticProductRequest request) {
		return new MapSqlParameterSource().addValue("businessId", businessId).addValue("productId", productId)
				.addValue("categoryCode", request.categoryCode()).addValue("code", request.code())
				.addValue("name", request.name()).addValue("description", request.description())
				.addValue("price", request.price()).addValue("stock", request.stock())
				.addValue("stockMinimum", request.stockMinimum()).addValue("supplier", request.supplier())
				.addValue("expirationDate", request.expirationDate())
				.addValue("compatibleServices", request.compatibleServices())
				.addValue("recommendationRules", request.recommendationRules())
				.addValue("crossSellRules", request.crossSellRules())
				.addValue("usageRestrictions", request.usageRestrictions()).addValue("active", request.active());
	}

	private MapSqlParameterSource ruleParameters(UUID businessId, UUID ruleId,
			UpsertAestheticBusinessRuleRequest request) {
		return new MapSqlParameterSource().addValue("businessId", businessId).addValue("ruleId", ruleId)
				.addValue("code", request.code()).addValue("name", request.name())
				.addValue("ruleType", request.ruleType()).addValue("description", request.description())
				.addValue("priority", request.priority()).addValue("active", request.active())
				.addValue("rulePayload", request.rulePayload());
	}

	private QueryParts buildServicesQuery(UUID businessId, String search, String categoryCode, Boolean active) {
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId);
		StringBuilder query = new StringBuilder("""
				from aesthetic_service s
				join aesthetic_service_category c
				  on c.id = s.category_id
				 and c.business_id = s.business_id
				where s.business_id = :businessId
				""");
		appendSearch(query, parameters, search, "s.name", "s.description", "c.name");
		if (categoryCode != null && !categoryCode.isBlank()) {
			query.append(" and c.code = :categoryCode\n");
			parameters.addValue("categoryCode", categoryCode.trim().toUpperCase());
		}
		if (active != null) {
			query.append(" and s.active = :active\n");
			parameters.addValue("active", active);
		}
		return new QueryParts(query.toString(), parameters);
	}

	private QueryParts buildProductsQuery(UUID businessId, String search, String categoryCode, Boolean active,
			Boolean lowStockOnly) {
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId);
		StringBuilder query = new StringBuilder("""
				from aesthetic_product p
				join aesthetic_product_category c
				  on c.id = p.category_id
				 and c.business_id = p.business_id
				where p.business_id = :businessId
				""");
		appendSearch(query, parameters, search, "p.name", "p.description", "c.name");
		if (categoryCode != null && !categoryCode.isBlank()) {
			query.append(" and c.code = :categoryCode\n");
			parameters.addValue("categoryCode", categoryCode.trim().toUpperCase());
		}
		if (active != null) {
			query.append(" and p.active = :active\n");
			parameters.addValue("active", active);
		}
		if (Boolean.TRUE.equals(lowStockOnly)) {
			query.append(" and p.stock <= p.stock_minimum\n");
		}
		return new QueryParts(query.toString(), parameters);
	}

	private QueryParts buildRulesQuery(UUID businessId, String ruleType, Boolean active) {
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId);
		StringBuilder query = new StringBuilder("""
				from aesthetic_business_rule
				where business_id = :businessId
				""");
		if (ruleType != null && !ruleType.isBlank()) {
			query.append(" and rule_type = :ruleType\n");
			parameters.addValue("ruleType", ruleType.trim().toUpperCase());
		}
		if (active != null) {
			query.append(" and active = :active\n");
			parameters.addValue("active", active);
		}
		return new QueryParts(query.toString(), parameters);
	}

	private void appendSearch(StringBuilder query, MapSqlParameterSource parameters, String search, String firstColumn,
			String secondColumn, String thirdColumn) {
		if (search == null || search.isBlank()) {
			return;
		}
		query.append("""
				 and (
				    lower(%s) like :search
				    or lower(%s) like :search
				    or lower(%s) like :search
				 )
				""".formatted(firstColumn, secondColumn, thirdColumn));
		parameters.addValue("search", "%" + normalizeSearch(search) + "%");
	}

	private String normalizeSearch(String value) {
		return value.trim().toLowerCase().replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o')
				.replace('ú', 'u').replace('ñ', 'n');
	}

	private long count(QueryParts queryParts) {
		Long totalItems = jdbcTemplate.queryForObject("select count(*) " + queryParts.fromAndWhere(),
				queryParts.parameters(), Long.class);
		return totalItems == null ? 0 : totalItems;
	}

	private int totalPages(long totalItems, int size) {
		return totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			return "{}";
		}
	}

	private String serviceSelect() {
		return """
				select
				    s.id,
				    s.code,
				    s.name,
				    s.description,
				    c.code as category_code,
				    c.name as category_name,
				    s.duration_minutes,
				    s.price_base,
				    s.professional_required,
				    s.supplies,
				    s.contraindications,
				    s.availability_rules,
				    s.booking_rules,
				    s.cancellation_rules,
				    s.aftercare_recommendations,
				    s.requires_prior_evaluation,
				    s.requires_informed_consent,
				    s.active,
				    s.created_at,
				    s.updated_at
				""";
	}

	private String productSelect() {
		return """
				select
				    p.id,
				    p.code,
				    p.name,
				    p.description,
				    c.code as category_code,
				    c.name as category_name,
				    p.price,
				    p.stock,
				    p.stock_minimum,
				    p.supplier,
				    p.expiration_date,
				    p.compatible_services,
				    p.recommendation_rules,
				    p.cross_sell_rules,
				    p.usage_restrictions,
				    (p.stock <= p.stock_minimum) as low_stock,
				    p.active,
				    p.created_at,
				    p.updated_at
				""";
	}

	private RowMapper<AestheticCategoryResponse> categoryRowMapper() {
		return (resultSet, rowNumber) -> {
			Integer displayOrder = null;
			try {
				displayOrder = resultSet.getObject("display_order", Integer.class);
			} catch (Exception ignored) {
			}
			return new AestheticCategoryResponse(resultSet.getObject("id", UUID.class), resultSet.getString("code"),
					resultSet.getString("name"), resultSet.getString("description"), resultSet.getBoolean("active"),
					displayOrder);
		};
	}

	private RowMapper<AestheticServiceResponse> serviceRowMapper() {
		return (resultSet, rowNumber) -> new AestheticServiceResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("code"), resultSet.getString("name"), resultSet.getString("description"),
				resultSet.getString("category_code"), resultSet.getString("category_name"),
				resultSet.getInt("duration_minutes"), resultSet.getBigDecimal("price_base"),
				resultSet.getString("professional_required"), resultSet.getString("supplies"),
				resultSet.getString("contraindications"), resultSet.getString("availability_rules"),
				resultSet.getString("booking_rules"), resultSet.getString("cancellation_rules"),
				resultSet.getString("aftercare_recommendations"), resultSet.getBoolean("requires_prior_evaluation"),
				resultSet.getBoolean("requires_informed_consent"), resultSet.getBoolean("active"),
				resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class), List.of(), List.of());
	}

	private RowMapper<AestheticProductResponse> productRowMapper() {
		return (resultSet, rowNumber) -> new AestheticProductResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("code"), resultSet.getString("name"), resultSet.getString("description"),
				resultSet.getString("category_code"), resultSet.getString("category_name"),
				resultSet.getBigDecimal("price"), resultSet.getInt("stock"), resultSet.getInt("stock_minimum"),
				resultSet.getString("supplier"), resultSet.getObject("expiration_date", java.time.LocalDate.class),
				resultSet.getString("compatible_services"), resultSet.getString("recommendation_rules"),
				resultSet.getString("cross_sell_rules"), resultSet.getString("usage_restrictions"),
				resultSet.getBoolean("low_stock"), resultSet.getBoolean("active"),
				resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<AestheticBusinessRuleResponse> ruleRowMapper() {
		return (resultSet, rowNumber) -> new AestheticBusinessRuleResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("code"), resultSet.getString("name"), resultSet.getString("rule_type"),
				resultSet.getString("description"), resultSet.getInt("priority"), resultSet.getBoolean("active"),
				resultSet.getString("rule_payload"), resultSet.getObject("created_at", OffsetDateTime.class),
				resultSet.getObject("updated_at", OffsetDateTime.class));
	}

	private RowMapper<AestheticIntentLogResponse> intentLogRowMapper() {
		return (resultSet, rowNumber) -> new AestheticIntentLogResponse(resultSet.getObject("id", UUID.class),
				resultSet.getString("source_message"), resultSet.getString("intent"),
				resultSet.getBigDecimal("confidence"), resultSet.getString("entities"),
				resultSet.getBoolean("requires_database_lookup"), resultSet.getBoolean("requires_human_handoff"),
				resultSet.getString("handoff_reason"), resultSet.getString("suggested_response"),
				resultSet.getString("model_name"), resultSet.getObject("created_at", OffsetDateTime.class));
	}

	public List<ServiceBranchRecord> findServiceBranches(UUID businessId, UUID serviceId) {
		return jdbcTemplate.query(
				"""
						select bl.id, bl.name, bl.address, bl.commune, bl.phone,
						       bl.latitude, bl.longitude, bl.daily_booking_capacity,
						       (select count(*) from aesthetic_professional_location pl
						        where pl.business_id = bl.business_id and pl.location_id = bl.id and pl.active = true) as professional_count
						from business_location bl
						where bl.business_id = :businessId
						  and bl.active = true
						  and exists (
						      select 1 from aesthetic_service_location sl
						      where sl.business_id = bl.business_id
						        and sl.location_id = bl.id
						        and sl.service_id = :serviceId
						        and sl.active = true
						  )
						order by bl.name asc
						""",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId),
				(rs, rowNum) -> new ServiceBranchRecord(rs.getObject("id", UUID.class), rs.getString("name"),
						rs.getString("address"), rs.getString("commune"), rs.getString("phone"),
						rs.getInt("professional_count"), rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"),
						(Integer) rs.getObject("daily_booking_capacity")));
	}

	private void syncServiceAssignments(UUID businessId, UUID serviceId, List<UUID> professionalIds,
			List<UUID> roomIds) {
		jdbcTemplate.update(
				"delete from agenda_professional_service where business_id = :businessId and service_id = :serviceId",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId));
		jdbcTemplate.update(
				"delete from agenda_room_service where business_id = :businessId and service_id = :serviceId",
				new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId));

		if (professionalIds != null) {
			for (UUID professionalId : professionalIds) {
				if (professionalId == null) {
					continue;
				}
				jdbcTemplate
						.update("""
								insert into agenda_professional_service (id, business_id, service_id, professional_id, active)
								values (:id, :businessId, :serviceId, :professionalId, true)
								on conflict (business_id, service_id, professional_id) do update set active = true, updated_at = current_timestamp
								""",
								new MapSqlParameterSource().addValue("id", UUID.randomUUID())
										.addValue("businessId", businessId).addValue("serviceId", serviceId)
										.addValue("professionalId", professionalId));
			}
		}

		if (roomIds != null) {
			for (UUID roomId : roomIds) {
				if (roomId == null) {
					continue;
				}
				jdbcTemplate
						.update("""
								insert into agenda_room_service (id, business_id, service_id, room_id, active)
								values (:id, :businessId, :serviceId, :roomId, true)
								on conflict (business_id, service_id, room_id) do update set active = true, updated_at = current_timestamp
								""",
								new MapSqlParameterSource().addValue("id", UUID.randomUUID())
										.addValue("businessId", businessId).addValue("serviceId", serviceId)
										.addValue("roomId", roomId));
			}
		}
	}

	private List<UUID> findServiceProfessionalIds(UUID businessId, UUID serviceId) {
		List<UUID> ids = jdbcTemplate.queryForList("""
				select professional_id from agenda_professional_service
				where business_id = :businessId and service_id = :serviceId and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId),
				UUID.class);
		return ids == null ? List.of() : ids;
	}

	private List<UUID> findServiceRoomIds(UUID businessId, UUID serviceId) {
		List<UUID> ids = jdbcTemplate.queryForList("""
				select room_id from agenda_room_service
				where business_id = :businessId and service_id = :serviceId and active = true
				""", new MapSqlParameterSource().addValue("businessId", businessId).addValue("serviceId", serviceId),
				UUID.class);
		return ids == null ? List.of() : ids;
	}

	public record ServiceBranchRecord(UUID id, String name, String address, String commune, String phone,
			int professionalCount, BigDecimal latitude, BigDecimal longitude, Integer dailyBookingCapacity) {
	}

	public record AestheticPromotionSummary(String code, String name, String description, String discountType,
			BigDecimal discountValue, LocalDate startsOn, LocalDate endsOn, String conditions) {

		public String discountLabel() {
			if ("PERCENTAGE".equals(discountType)) {
				return discountValue.stripTrailingZeros().toPlainString() + "%";
			}
			return "$" + discountValue.stripTrailingZeros().toPlainString();
		}
	}

	private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
	}
}
