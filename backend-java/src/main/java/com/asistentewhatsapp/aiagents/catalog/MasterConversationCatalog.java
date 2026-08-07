package com.asistentewhatsapp.aiagents.catalog;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Catálogo Maestro de Conversación: fuente única de verdad (SSOT) para el motor
 * de intenciones del asistente. Centraliza intenciones, entidades, estados,
 * transiciones, reglas, respuestas, sinónimos y el mapeo de códigos de catálogo
 * que antes vivían distribuidos en IntentDetectorService,
 * ConversationSpecCatalog, AgentRegistry y la BD.
 *
 * <p>
 * El catálogo se carga una sola vez desde
 * {@code /conversation/master/master-conversation-catalog.json}. El JSON se
 * regenera con {@code tools/catalog-generator.mjs}; el runtime solo lee el
 * JSON.
 */
@Component
public class MasterConversationCatalog {

	public static final String RESOURCE = "/conversation/master/master-conversation-catalog.json";

	private static volatile MasterConversationCatalog sharedInstance;

	private final Map<String, IntentDefinition> intentsByCode;
	private final Map<String, EntityDefinition> entitiesByKey;
	private final Map<String, List<String>> synonymGroups;
	private final Map<String, AgentIntent> catalogCodes;
	private final List<TaxonomyPhrase> taxonomy;
	private final Map<String, StateDefinition> statesByCode;
	private final List<TransitionDefinition> transitions;
	private final Map<String, RuleDefinition> rulesById;
	private final Map<String, String> legacyStateMap;
	private final Map<String, ResponseDefinition> responsesByIntent;
	private final Map<String, AgentDefinition> agentsByType;

	public MasterConversationCatalog() {
		this(loadEnvelope());
	}

	MasterConversationCatalog(JsonNode envelope) {
		this.intentsByCode = parseIntents(envelope.path("intents"));
		this.entitiesByKey = parseEntities(envelope.path("entities"));
		this.synonymGroups = parseSynonymGroups(envelope.path("synonymGroups"));
		this.catalogCodes = parseCatalogCodes(envelope.path("catalogCodes"));
		this.taxonomy = parseTaxonomy(envelope.path("taxonomy"));
		this.statesByCode = parseStates(envelope.path("states"));
		this.transitions = parseTransitions(envelope.path("stateTransitions"));
		this.rulesById = parseRules(envelope.path("rules"));
		this.legacyStateMap = parseStringMap(envelope.path("legacyStateMap"));
		this.responsesByIntent = parseResponses(envelope.path("responses"));
		this.agentsByType = parseAgents(envelope.path("agents"));
	}

	/**
	 * Instancia compartida no gestionada por Spring para uso dentro de objetos
	 * construidos a mano en tests o en modos sin contexto de aplicación.
	 */
	public static MasterConversationCatalog shared() {
		MasterConversationCatalog current = sharedInstance;
		if (current == null) {
			synchronized (MasterConversationCatalog.class) {
				current = sharedInstance;
				if (current == null) {
					current = new MasterConversationCatalog();
					sharedInstance = current;
				}
			}
		}
		return current;
	}

	public Optional<IntentDefinition> findIntent(String code) {
		return code == null ? Optional.empty() : Optional.ofNullable(intentsByCode.get(code));
	}

	public IntentDefinition intent(String code) {
		return intentsByCode.get(code);
	}

	public List<IntentDefinition> intents() {
		return List.copyOf(intentsByCode.values());
	}

	public Optional<EntityDefinition> findEntity(String key) {
		return key == null ? Optional.empty() : Optional.ofNullable(entitiesByKey.get(key));
	}

	public List<EntityDefinition> entities() {
		return List.copyOf(entitiesByKey.values());
	}

	/** Devuelve el grupo de sinónimos por su nombre; lista vacía si no existe. */
	public List<String> synonymGroup(String name) {
		List<String> group = synonymGroups.get(name);
		return group == null ? List.of() : List.copyOf(group);
	}

	public Optional<AgentIntent> mapCatalogCodeToAgentIntent(String code) {
		if (code == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(catalogCodes.get(code));
	}

	public List<TaxonomyPhrase> taxonomy() {
		return List.copyOf(taxonomy);
	}

	public Optional<StateDefinition> findState(String code) {
		return code == null ? Optional.empty() : Optional.ofNullable(statesByCode.get(code));
	}

	public List<StateDefinition> states() {
		return List.copyOf(statesByCode.values());
	}

	public List<TransitionDefinition> transitions() {
		return List.copyOf(transitions);
	}

	/** Mapea un estado persistido legado (ej. ESPERANDO_SERVICIO) al canónico. */
	public Optional<String> mapLegacyState(String legacy) {
		if (legacy == null || legacy.isBlank() || statesByCode.containsKey(legacy)) {
			return Optional.ofNullable(legacy);
		}
		return Optional.ofNullable(legacyStateMap.get(legacy));
	}

	public Optional<RuleDefinition> findRule(String id) {
		return id == null ? Optional.empty() : Optional.ofNullable(rulesById.get(id));
	}

	public List<RuleDefinition> rules() {
		return List.copyOf(rulesById.values());
	}

	public List<RuleDefinition> rulesByType(String type) {
		return rulesById.values().stream().filter(rule -> rule.type().equalsIgnoreCase(type)).toList();
	}

	public Optional<ResponseDefinition> findResponse(String intentCode) {
		return intentCode == null ? Optional.empty() : Optional.ofNullable(responsesByIntent.get(intentCode));
	}

	public List<AgentDefinition> agents() {
		return List.copyOf(agentsByType.values());
	}

	/* ------------------------------------------------------------------ */
	/* Parsing helpers */
	/* ------------------------------------------------------------------ */

	private static JsonNode loadEnvelope() {
		try (InputStream input = MasterConversationCatalog.class.getResourceAsStream(RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException(
						"Catálogo maestro no encontrado en classpath: " + RESOURCE + " (regenerar con el generador)");
			}
			return new ObjectMapper().readTree(input);
		} catch (java.io.IOException exception) {
			throw new IllegalStateException("No se pudo cargar el catálogo maestro: " + RESOURCE, exception);
		}
	}

	private static Map<String, IntentDefinition> parseIntents(JsonNode items) {
		Map<String, IntentDefinition> result = new LinkedHashMap<>();
		for (JsonNode node : items) {
			IntentDefinition definition = new IntentDefinition(text(node, "code"), text(node, "name"),
					text(node, "description"), text(node, "priority"), number(node, "confidence", 0.8),
					number(node, "minimumConfidence", 0.5), text(node, "urgency"), text(node, "agent"),
					node.path("requiresHuman").asBoolean(false), node.path("requiresAi").asBoolean(false),
					stringList(node, "requiredEntities"), stringList(node, "allowedStates"),
					stringList(node, "synonymGroups"), stringList(node, "catalogCodes"), text(node, "notes"));
			if (definition.code() != null) {
				result.put(definition.code(), definition);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, EntityDefinition> parseEntities(JsonNode items) {
		Map<String, EntityDefinition> result = new LinkedHashMap<>();
		for (JsonNode node : items) {
			EntityDefinition definition = new EntityDefinition(text(node, "key"), text(node, "entityType"),
					text(node, "name"), node.path("required").asBoolean(false),
					node.path("deterministic").asBoolean(true), text(node, "source"), stringList(node, "synonyms"));
			if (definition.key() != null) {
				result.put(definition.key(), definition);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, List<String>> parseSynonymGroups(JsonNode groups) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		if (!groups.isObject()) {
			return Collections.unmodifiableMap(result);
		}
		groups.fields().forEachRemaining(entry -> result.put(entry.getKey(), fromArray(entry.getValue())));
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, AgentIntent> parseCatalogCodes(JsonNode codes) {
		Map<String, AgentIntent> result = new LinkedHashMap<>();
		if (!codes.isObject()) {
			return Collections.unmodifiableMap(result);
		}
		codes.fields().forEachRemaining(entry -> {
			String value = entry.getValue().asText(null);
			if (value != null) {
				try {
					result.put(entry.getKey(), AgentIntent.valueOf(value));
				} catch (IllegalArgumentException ignored) {
					// código sin intención mapeada: se ignora
				}
			}
		});
		return Collections.unmodifiableMap(result);
	}

	private static List<TaxonomyPhrase> parseTaxonomy(JsonNode items) {
		List<TaxonomyPhrase> result = new ArrayList<>();
		for (JsonNode node : items) {
			String intentCode = text(node, "intent");
			AgentIntent intent = parseAgentIntent(intentCode);
			if (intent == null) {
				continue;
			}
			result.add(new TaxonomyPhrase(text(node, "normalizedPhrase"), intentCode, number(node, "confidence", 0.72),
					text(node, "urgency"), node.path("requiresHuman").asBoolean(false), text(node, "reason")));
		}
		return List.copyOf(result);
	}

	private static Map<String, StateDefinition> parseStates(JsonNode items) {
		Map<String, StateDefinition> result = new LinkedHashMap<>();
		for (JsonNode node : items) {
			StateDefinition definition = new StateDefinition(text(node, "code"), text(node, "name"),
					text(node, "description"), stringList(node, "entryData"), stringList(node, "allowedIntents"));
			if (definition.code() != null) {
				result.put(definition.code(), definition);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static List<TransitionDefinition> parseTransitions(JsonNode items) {
		List<TransitionDefinition> result = new ArrayList<>();
		for (JsonNode node : items) {
			result.add(new TransitionDefinition(text(node, "from"), text(node, "to"), text(node, "onIntent"),
					text(node, "condition"), text(node, "priority")));
		}
		return List.copyOf(result);
	}

	private static Map<String, RuleDefinition> parseRules(JsonNode items) {
		Map<String, RuleDefinition> result = new LinkedHashMap<>();
		for (JsonNode node : items) {
			RuleDefinition definition = new RuleDefinition(text(node, "id"), text(node, "name"), text(node, "type"),
					text(node, "appliesTo"), text(node, "intent"), text(node, "reason"), text(node, "description"),
					numberOrNull(node, "confidence"), text(node, "urgency"));
			if (definition.id() != null) {
				result.put(definition.id(), definition);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, ResponseDefinition> parseResponses(JsonNode items) {
		Map<String, ResponseDefinition> result = new LinkedHashMap<>();
		for (JsonNode node : items) {
			String intentCode = text(node, "intentCode");
			Map<String, String> templates = new LinkedHashMap<>();
			JsonNode templatesNode = node.path("templates");
			if (templatesNode.isObject()) {
				templatesNode.fields()
						.forEachRemaining(entry -> templates.put(entry.getKey(), entry.getValue().asText("")));
			}
			result.put(intentCode, new ResponseDefinition(intentCode, Collections.unmodifiableMap(templates)));
		}
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, AgentDefinition> parseAgents(JsonNode items) {
		Map<String, AgentDefinition> result = new LinkedHashMap<>();
		for (JsonNode node : items) {
			AgentDefinition definition = new AgentDefinition(text(node, "type"), text(node, "name"),
					text(node, "execution"), stringList(node, "intents"));
			if (definition.type() != null) {
				result.put(definition.type(), definition);
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, String> parseStringMap(JsonNode node) {
		Map<String, String> result = new LinkedHashMap<>();
		if (node.isObject()) {
			node.fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asText("")));
		}
		return Collections.unmodifiableMap(result);
	}

	private static AgentIntent parseAgentIntent(String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		try {
			return AgentIntent.valueOf(code);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static List<String> fromArray(JsonNode node) {
		List<String> result = new ArrayList<>();
		if (node.isArray()) {
			node.forEach(value -> {
				String item = value.asText(null);
				if (item != null && !item.isBlank()) {
					result.add(item);
				}
			});
		}
		return List.copyOf(result);
	}

	private static List<String> stringList(JsonNode parent, String field) {
		return fromArray(parent.path(field));
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isMissingNode() || value.isNull() ? null : value.asText("");
	}

	private static double number(JsonNode node, String field, double fallback) {
		JsonNode value = node.path(field);
		return value.isMissingNode() || value.isNull() || !value.isNumber() ? fallback : value.asDouble();
	}

	private static double numberOrNull(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isMissingNode() || value.isNull() || !value.isNumber() ? Double.NaN : value.asDouble();
	}

	/* ------------------------------------------------------------------ */
	/* Modelo de datos */
	/* ------------------------------------------------------------------ */

	public record IntentDefinition(String code, String name, String description, String priority, double confidence,
			double minimumConfidence, String urgency, String agent, boolean requiresHuman, boolean requiresAi,
			List<String> requiredEntities, List<String> allowedStates, List<String> synonymGroups,
			List<String> catalogCodes, String notes) {
	}

	public record EntityDefinition(String key, String entityType, String name, boolean required, boolean deterministic,
			String source, List<String> synonyms) {
	}

	public record TaxonomyPhrase(String normalizedPhrase, String intent, double confidence, String urgency,
			boolean requiresHuman, String reason) {
	}

	public record StateDefinition(String code, String name, String description, List<String> entryData,
			List<String> allowedIntents) {
	}

	public record TransitionDefinition(String from, String to, String onIntent, String condition, String priority) {
	}

	public record RuleDefinition(String id, String name, String type, String appliesTo, String intent, String reason,
			String description, double confidence, String urgency) {
	}

	public record ResponseDefinition(String intentCode, Map<String, String> templates) {
	}

	public record AgentDefinition(String type, String name, String execution, List<String> intents) {
	}
}