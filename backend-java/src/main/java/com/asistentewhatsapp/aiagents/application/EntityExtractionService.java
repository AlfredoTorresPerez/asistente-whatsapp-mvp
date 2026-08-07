package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.infrastructure.CanonicalEntityJdbcRepository.CanonicalEntityRecord;
import com.asistentewhatsapp.aiagents.catalog.RelativeDateService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityExtractionService {

	private static final Pattern EXPLICIT_TIME_WITH_PREFIX_PATTERN = Pattern.compile(
			"\\b(?:a\\s+las|desde\\s+las|para\\s+las|hora|horario)\\s+([01]?\\d|2[0-3])(?:(?::|\\.)([0-5]\\d)|\\s*(?:h|hrs?|horas?)?)\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern EXPLICIT_TIME_WITH_SEPARATOR_PATTERN = Pattern
			.compile("\\b([01]?\\d|2[0-3])(?::|\\.)([0-5]\\d)\\s*(?:h|hrs?|horas?)?\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern EXPLICIT_TIME_WITH_SUFFIX_PATTERN = Pattern
			.compile("\\b([01]?\\d|2[0-3])\\s*(?:h|hrs?|horas?)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
	private static final Pattern MONTH_NAME_DATE_PATTERN = Pattern.compile(
			"\\b(\\d{1,2})(?:\\s+de)?\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)(?:\\s+de\\s+(\\d{2,4}))?\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\$\\s?([0-9][0-9.]{2,})");
	private static final Pattern ORDER_PATTERN = Pattern
			.compile("(?:pedido|orden|solicitud|folio)\\s*#?\\s*([a-zA-Z0-9-]{4,})", Pattern.CASE_INSENSITIVE);
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern NAME_PATTERN = Pattern.compile(
			"(?:soy|mi nombre es|me llamo)\\s+([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+){0,3})",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PROFESSIONAL_WITH_PREFIX_PATTERN = Pattern.compile(
			"\\b(?:con|atiende\\s+con|atienden\\s+con)\\s+([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+){0,2})",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PROFESSIONAL_ATTENDS_BEFORE_PATTERN = Pattern.compile(
			"(^|[¿\\s])([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+){0,1})(?:\\s+)(?:atiende|atender|recibe|trabaja)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PROFESSIONAL_ATTENDS_AFTER_PATTERN = Pattern.compile(
			"(?:cuando\\s+atiende|atiende|atienden)\\s+([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+){0,1})",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern RELATIVE_DATE_TIME_LOCATION_PATTERN = Pattern.compile(
			"\\b(?:hoy|ma(?:n|ñ)ana|pasado\\s+ma(?:n|ñ)ana|lunes|martes|mi(?:e|é)rcoles|jueves|viernes|s(?:a|á)bado|domingo)\\b.*?"
					+ "\\b(?:a\\s+las|desde\\s+las|para\\s+las)?\\s*([01]?\\d|2[0-3])\\s*(?:h|hrs?|horas?)\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern RELATIVE_DATE_BARE_HOUR_PATTERN = Pattern.compile(
			"\\b(?:hoy|ma(?:n|ñ)ana|pasado\\s+ma(?:n|ñ)ana)\\s+([01]?\\d|2[0-3])\\b(?!\\s*(?:de|/|-|:|\\.))",
			Pattern.CASE_INSENSITIVE);

	private final AiBusinessKnowledgeService knowledgeService;
	private final BusinessLocationJdbcRepository businessLocationJdbcRepository;
	private final CanonicalEntityService canonicalEntityService;
	private final RelativeDateService relativeDateService;
	private final ProfessionalCatalogService professionalCatalogService;

	public EntityExtractionService(AiBusinessKnowledgeService knowledgeService,
			BusinessLocationJdbcRepository businessLocationJdbcRepository) {
		this(knowledgeService, businessLocationJdbcRepository, null, null);
	}

	public EntityExtractionService(AiBusinessKnowledgeService knowledgeService,
			BusinessLocationJdbcRepository businessLocationJdbcRepository,
			CanonicalEntityService canonicalEntityService) {
		this(knowledgeService, businessLocationJdbcRepository, canonicalEntityService, null);
	}

	@Autowired
	public EntityExtractionService(AiBusinessKnowledgeService knowledgeService,
			BusinessLocationJdbcRepository businessLocationJdbcRepository,
			CanonicalEntityService canonicalEntityService, ProfessionalCatalogService professionalCatalogService) {
		this.knowledgeService = knowledgeService;
		this.businessLocationJdbcRepository = businessLocationJdbcRepository;
		this.canonicalEntityService = canonicalEntityService;
		this.professionalCatalogService = professionalCatalogService;
		this.relativeDateService = RelativeDateService.shared();
	}

	public Map<String, String> extract(AgentConversationRequest request) {
		String traceId = AiTraceLogger.traceId(request);
		Map<String, String> entities = new LinkedHashMap<>();
		String message = request.messageBody() == null ? "" : request.messageBody();
		String normalizedForTrace = normalize(message);
		AiTraceLogger.info("MESSAGE_NORMALIZED", traceId, request.conversationId(), null, "EntityExtractionService",
				LogSanitizer.messageSummary("message", message) + " normalizedLength=" + normalizedForTrace.length());

		addTimeIfFound(entities, message);
		addIfFound(entities, "fecha", DATE_PATTERN.matcher(message));
		addIfFound(entities, "fecha", MONTH_NAME_DATE_PATTERN.matcher(message));
		addAmountIfFound(entities, AMOUNT_PATTERN.matcher(message));
		addRequestNumberIfFound(entities, ORDER_PATTERN.matcher(message));
		addIfFound(entities, "correo", EMAIL_PATTERN.matcher(message));
		addNameIfFound(entities, NAME_PATTERN.matcher(message));

		String normalized = normalizedForTrace;
		applyCanonicalEntities(entities, request, normalized);
		applyCanonicalAliases(entities, request, normalized);
		applyDatabaseAliases(entities, request, normalized);
		applyServiceCatalogInference(entities, request, message);
		applyWeekdayRelativeDates(entities, normalized);
		applyTimeRange(entities, normalized);
		applyProfessionalMention(entities, request, message, normalized);
		applyBusinessLocationAliases(entities, request, normalized);

		String standaloneName = inferStandaloneName(message, normalized);
		if (standaloneName != null) {
			entities.putIfAbsent("nombre", standaloneName);
		}

		if (request.customerDisplayName() != null && !request.customerDisplayName().isBlank()) {
			entities.putIfAbsent("cliente", request.customerDisplayName());
		}
		if (request.customerPhone() != null && !request.customerPhone().isBlank()) {
			entities.putIfAbsent("telefono", request.customerPhone());
		}

		AiTraceLogger.info("ENTITIES_EXTRACTED", traceId, request.conversationId(), null, "EntityExtractionService",
				"entities=" + AiTraceLogger.summarizeMap(entities) + " serviceText="
						+ entities.getOrDefault("servicio_o_producto", "") + " dateText="
						+ firstNonBlank(entities.get("fecha"), entities.get("fecha_relativa")) + " timeText="
						+ entities.getOrDefault("hora", "") + " locationText=" + entities.getOrDefault("sede", ""));
		return entities;
	}

	private String firstNonBlank(String first, String second) {
		return first != null && !first.isBlank() ? first : (second == null ? "" : second);
	}

	private void applyCanonicalEntities(Map<String, String> entities, AgentConversationRequest request,
			String normalizedMessage) {
		if (canonicalEntityService == null || request.businessId() == null) {
			return;
		}
		List<CanonicalEntityRecord> catalog = canonicalEntityService.findActive(request.businessId());
		for (CanonicalEntityRecord entity : catalog) {
			if (entity.canonicalName() == null || entity.canonicalName().isBlank()
					|| !normalizedMessage.contains(entity.canonicalName())) {
				continue;
			}
			String displayName = entity.displayName() == null || entity.displayName().isBlank()
					? entity.canonicalName()
					: entity.displayName();
			applyCanonicalByType(entities, request, entity, displayName);
		}
	}

	private void applyCanonicalAliases(Map<String, String> entities, AgentConversationRequest request,
			String normalizedMessage) {
		if (canonicalEntityService == null || request.businessId() == null) {
			return;
		}
		for (CanonicalEntityService.AliasMatch match : canonicalEntityService.resolveAliases(request.businessId(),
				normalizedMessage)) {
			applyAliasMatch(entities, request, match);
		}
	}

	private void applyAliasMatch(Map<String, String> entities, AgentConversationRequest request,
			CanonicalEntityService.AliasMatch match) {
		String key = switch (match.entityType()) {
			case "SERVICE" -> "servicio_o_producto";
			case "PROFESSIONAL" -> "profesional";
			case "LOCATION" -> "sede";
			case "RELATIVE_DATE" -> "fecha_relativa";
			case "PREFERENCE" -> "preferencia_horaria";
			default -> null;
		};
		if (key == null || entities.containsKey(key)) {
			return;
		}
		entities.put(key, match.displayName());
		markResolution(entities, key, match.canonicalEntityId(), match.matchedAlias(), match.confidence());
		if ("SERVICE".equals(match.entityType())) {
			knowledgeService.findService(request.businessId(), match.displayName()).ifPresent(service -> {
				if (service.code() != null) {
					entities.putIfAbsent("servicio_codigo", service.code());
				}
			});
		} else if ("LOCATION".equals(match.entityType()) && "business_location".equals(match.referenceType())
				&& match.referenceId() != null) {
			entities.putIfAbsent("sede_id", match.referenceId().toString());
		}
	}

	private void markResolution(Map<String, String> entities, String key, UUID canonicalId, String matchedAlias,
			BigDecimal confidence) {
		if (canonicalId != null) {
			entities.put(key + "_canonical_id", canonicalId.toString());
			entities.put(key + "_resolution", matchedAlias == null || matchedAlias.isBlank() ? "DATABASE" : "ALIAS");
		}
		if (matchedAlias != null && !matchedAlias.isBlank()) {
			entities.put(key + "_matched_alias", matchedAlias);
		}
		if (confidence != null) {
			entities.put(key + "_confidence", confidence.toPlainString());
		}
	}

	private void applyCanonicalByType(Map<String, String> entities, AgentConversationRequest request,
			CanonicalEntityRecord entity, String displayName) {
		switch (entity.entityType()) {
			case "SERVICE" -> {
				entities.putIfAbsent("servicio_o_producto", displayName);
				markResolution(entities, "servicio_o_producto", entity.id(), null, null);
				Optional<AiKnowledgeRepository.ServiceCatalogItem> service = knowledgeService
						.findService(request.businessId(), displayName);
				if (service.isPresent() && service.get().code() != null) {
					entities.putIfAbsent("servicio_codigo", service.get().code());
				}
			}
			case "PROFESSIONAL" -> {
				entities.putIfAbsent("profesional", displayName);
				markResolution(entities, "profesional", entity.id(), null, null);
			}
			case "LOCATION" -> {
				entities.putIfAbsent("sede", displayName);
				markResolution(entities, "sede", entity.id(), null, null);
				if ("business_location".equals(entity.referenceType()) && entity.referenceId() != null) {
					entities.putIfAbsent("sede_id", entity.referenceId().toString());
				}
			}
			case "RELATIVE_DATE" -> {
				entities.putIfAbsent("fecha_relativa", displayName);
				markResolution(entities, "fecha_relativa", entity.id(), null, null);
			}
			case "PREFERENCE" -> {
				entities.putIfAbsent("preferencia_horaria", displayName);
				markResolution(entities, "preferencia_horaria", entity.id(), null, null);
			}
			default -> {
			}
		}
	}

	private void applyDatabaseAliases(Map<String, String> entities, AgentConversationRequest request,
			String normalizedMessage) {
		for (AiKnowledgeRepository.EntityAlias alias : knowledgeService.findAliases(request.businessId())) {
			String normalizedAlias = normalize(alias.alias());
			if (!normalizedAlias.isBlank() && normalizedMessage.contains(normalizedAlias)) {
				entities.putIfAbsent(alias.entityKey(), alias.entityValue());
			}
		}
	}

	private void applyServiceCatalogInference(Map<String, String> entities, AgentConversationRequest request,
			String message) {
		String currentService = entities.get("servicio_o_producto");
		knowledgeService.findServiceMentionedInText(request.businessId(), message).ifPresent(service -> {
			if (currentService == null || currentService.isBlank() || isGenericService(currentService)
					|| !sameService(currentService, service.name())) {
				entities.put("servicio_o_producto", service.name());
				if (service.code() != null && !service.code().isBlank()) {
					entities.put("servicio_codigo", service.code());
				}
			}
		});
	}

	private boolean sameService(String first, String second) {
		return normalize(first).equals(normalize(second));
	}

	private void applyWeekdayRelativeDates(Map<String, String> entities, String normalizedMessage) {
		if (entities.containsKey("fecha") || entities.containsKey("fecha_relativa")) {
			return;
		}
		for (Map.Entry<String, String> weekday : relativeDateService.weekdayMap().entrySet()) {
			if (containsWholeToken(normalizedMessage, weekday.getKey())) {
				String qualifier = "";
				if (containsWholeToken(normalizedMessage, "proximo") || containsWholeToken(normalizedMessage, "proxima")
						|| containsWholeToken(normalizedMessage, "siguiente")) {
					qualifier = "proximo";
				} else if (containsWholeToken(normalizedMessage, "este")
						|| containsWholeToken(normalizedMessage, "esta")) {
					qualifier = "este";
				}
				entities.put("fecha_relativa",
						qualifier.isBlank() ? weekday.getValue() : qualifier + " " + weekday.getValue());
				return;
			}
		}
		if (normalizedMessage.contains("fin de semana")) {
			entities.put("fecha_relativa", "fin de semana");
			return;
		}
		if (normalizedMessage.contains("pasado manana")) {
			entities.put("fecha_relativa", "pasado mañana");
			return;
		}
		if (containsWholeToken(normalizedMessage, "hoy")) {
			entities.put("fecha_relativa", "hoy");
			return;
		}
		if (containsWholeToken(normalizedMessage, "manana") && !normalizedMessage.contains("en la manana")
				&& !normalizedMessage.contains("por la manana")) {
			entities.put("fecha_relativa", "mañana");
			return;
		}
		if (normalizedMessage.contains("esta semana")) {
			entities.put("fecha_relativa", "esta semana");
			return;
		}
		if (normalizedMessage.contains("proxima semana") || normalizedMessage.contains("la otra semana")) {
			entities.put("fecha_relativa", "próxima semana");
		}
	}

	private boolean isGenericService(String value) {
		String normalized = normalize(value);
		return normalized.equals("depilacion") || normalized.equals("facial") || normalized.equals("servicio");
	}

	private void applyTimeRange(Map<String, String> entities, String normalizedMessage) {
		if (entities.containsKey("tramo_horario")) {
			return;
		}
		if (normalizedMessage.contains("despues de las cinco") || normalizedMessage.contains("despues de 5")) {
			entities.put("tramo_horario", "después de las 17:00");
			return;
		}
		if (normalizedMessage.contains("primera hora")) {
			entities.put("tramo_horario", "primera hora");
			return;
		}
		if (normalizedMessage.contains("al mediodia") || normalizedMessage.contains("medio dia")) {
			entities.put("tramo_horario", "mediodía");
			return;
		}
		if (normalizedMessage.contains("en la manana") || normalizedMessage.contains("por la manana")) {
			entities.put("tramo_horario", "mañana");
			return;
		}
		if (normalizedMessage.contains("en la tarde") || normalizedMessage.contains("por la tarde")) {
			entities.put("tramo_horario", "tarde");
			return;
		}
		if (normalizedMessage.contains("en la noche") || normalizedMessage.contains("por la noche")) {
			entities.put("tramo_horario", "noche");
		}
	}

	private void applyProfessionalMention(Map<String, String> entities, AgentConversationRequest request,
			String message, String normalizedMessage) {
		if (entities.containsKey("profesional")) {
			return;
		}
		UUID businessId = request == null ? null : request.businessId();
		Matcher withPrefix = PROFESSIONAL_WITH_PREFIX_PATTERN.matcher(message == null ? "" : message);
		if (withPrefix.find()) {
			String name = withPrefix.group(1).trim();
			if (!name.isBlank()) {
				putProfessionalIfCatalogOrMarkNotFound(entities, businessId, name);
				return;
			}
		}
		Matcher before = PROFESSIONAL_ATTENDS_BEFORE_PATTERN.matcher(message == null ? "" : message);
		if (before.find()) {
			String name = before.group(2).trim();
			if (!name.isBlank() && !entities.containsKey("servicio_o_producto") && !isLocationName(request, name)) {
				putProfessionalIfCatalogOrMarkNotFound(entities, businessId, name);
				return;
			}
		}
		Matcher after = PROFESSIONAL_ATTENDS_AFTER_PATTERN.matcher(message == null ? "" : message);
		if (after.find()) {
			String name = after.group(1).trim();
			if (!name.isBlank() && !isLocationName(request, name)) {
				putProfessionalIfCatalogOrMarkNotFound(entities, businessId, name);
				return;
			}
		}
		if (containsAny(normalizedMessage, "misma persona", "misma profesional", "la misma", "el mismo",
				"misma de la vez")) {
			entities.putIfAbsent("profesional_mencion_generica", "misma_persona");
		}
	}

	private boolean isLocationName(AgentConversationRequest request, String candidate) {
		if (request == null || candidate == null || candidate.isBlank() || request.businessId() == null) {
			return false;
		}
		String normalizedCandidate = normalize(candidate);
		for (String token : normalizedCandidate.split(" ")) {
			if (token.length() < 3) {
				continue;
			}
			for (BusinessLocationRecord location : businessLocationJdbcRepository.findActive(request.businessId())) {
				if (token.equals(normalize(location.name())) || token.equals(normalize(location.commune()))
						|| token.equals(normalize(location.city())) || token.equals(normalize(location.code()))) {
					return true;
				}
			}
		}
		return false;
	}

	private void putProfessionalIfCatalogOrMarkNotFound(Map<String, String> entities, UUID businessId, String mention) {
		if (professionalCatalogService == null || businessId == null) {
			entities.put("profesional", mention);
			return;
		}
		Optional<ProfessionalCatalogService.ProfessionalInfo> match = professionalCatalogService.findByName(businessId,
				mention);
		if (match.isPresent()) {
			entities.put("profesional", match.get().name());
		} else {
			entities.put("profesional_no_encontrado", mention);
		}
	}

	private void applyBusinessLocationAliases(Map<String, String> entities, AgentConversationRequest request,
			String normalizedMessage) {
		if (normalizedMessage.isBlank() || entities.containsKey("sede")) {
			return;
		}
		var locations = businessLocationJdbcRepository.findActive(request.businessId());
		for (BusinessLocationRecord location : locations) {
			if (containsNormalized(normalizedMessage, location.name())
					|| containsNormalized(normalizedMessage, location.code())) {
				entities.put("sede", location.name());
				entities.put("sede_id", location.id().toString());
				return;
			}
		}
		for (BusinessLocationRecord location : locations) {
			if (containsNormalized(normalizedMessage, location.commune())
					|| containsNormalized(normalizedMessage, location.city())) {
				entities.put("sede", location.name());
				entities.put("sede_id", location.id().toString());
				return;
			}
		}
	}

	private boolean containsNormalized(String normalizedText, String value) {
		String normalizedValue = normalize(value);
		return !normalizedValue.isBlank() && normalizedText.contains(normalizedValue);
	}

	private void addIfFound(Map<String, String> entities, String key, Matcher matcher) {
		if (matcher.find()) {
			entities.put(key, matcher.group());
		}
	}

	private void addTimeIfFound(Map<String, String> entities, String message) {
		Matcher explicitWithPrefix = EXPLICIT_TIME_WITH_PREFIX_PATTERN.matcher(message);
		if (explicitWithPrefix.find()) {
			entities.put("hora", normalizeTime(explicitWithPrefix.group(1), explicitWithPrefix.group(2)));
			return;
		}
		Matcher explicitWithSeparator = EXPLICIT_TIME_WITH_SEPARATOR_PATTERN.matcher(message);
		if (explicitWithSeparator.find()) {
			entities.put("hora", normalizeTime(explicitWithSeparator.group(1), explicitWithSeparator.group(2)));
			return;
		}
		Matcher explicitWithSuffix = EXPLICIT_TIME_WITH_SUFFIX_PATTERN.matcher(message);
		if (explicitWithSuffix.find()) {
			entities.put("hora", normalizeTime(explicitWithSuffix.group(1), null));
			return;
		}
		Matcher relativeDateBareHour = RELATIVE_DATE_BARE_HOUR_PATTERN.matcher(message);
		if (relativeDateBareHour.find()) {
			entities.put("hora", normalizeTime(relativeDateBareHour.group(1), null));
			return;
		}
		Matcher relativeDateTimeLocation = RELATIVE_DATE_TIME_LOCATION_PATTERN.matcher(message);
		if (relativeDateTimeLocation.find()) {
			entities.put("hora", normalizeTime(relativeDateTimeLocation.group(1), null));
		}
	}

	private String normalizeTime(String hour, String minute) {
		int parsedHour = Integer.parseInt(hour);
		int parsedMinute = minute == null ? 0 : Integer.parseInt(minute);
		return String.format(Locale.ROOT, "%02d:%02d", parsedHour, parsedMinute);
	}

	private void addNameIfFound(Map<String, String> entities, Matcher matcher) {
		if (matcher.find()) {
			entities.put("nombre", matcher.group(1).trim());
		}
	}

	private void addAmountIfFound(Map<String, String> entities, Matcher matcher) {
		if (matcher.find()) {
			entities.put("monto", matcher.group(1).replace(".", ""));
		}
	}

	private void addRequestNumberIfFound(Map<String, String> entities, Matcher matcher) {
		if (matcher.find()) {
			entities.put("numero_solicitud", matcher.group(1));
		}
	}

	private String inferStandaloneName(String original, String normalized) {
		if (normalized.isBlank()) {
			return null;
		}
		if (normalized.contains("@") || containsAny(normalized, "hola", "buenas", "como estas", "como esta", "que tal",
				"gracias", "precio", "valor", "agendar", "agenda", "reserva", "reservar", "cita", "hora", "pago",
				"soporte", "necesito", "quiero", "favor")) {
			return null;
		}
		String trimmed = original == null ? "" : original.trim();
		if (!trimmed.matches("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+){0,2}")) {
			return null;
		}
		if (trimmed.length() < 3 || trimmed.length() > 80) {
			return null;
		}
		return trimmed;
	}

	private boolean containsWholeToken(String normalizedText, String token) {
		if (normalizedText == null || normalizedText.isBlank() || token == null || token.isBlank()) {
			return false;
		}
		for (String part : normalizedText.split(" ")) {
			if (part.equals(token)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsAny(String normalized, String... candidates) {
		for (String candidate : candidates) {
			if (normalized.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		return TextNormalizer.normalize(value);
	}
}
