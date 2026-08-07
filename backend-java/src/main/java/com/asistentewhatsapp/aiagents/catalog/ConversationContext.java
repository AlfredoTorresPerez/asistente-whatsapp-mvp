package com.asistentewhatsapp.aiagents.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contexto único de conversación (Fase 6). Reemplaza el manejo implícito de
 * Map&lt;String,String&gt; aislado por un modelo tipado que une: negocio y
 * conversación, intención (principal + secundaria), estado canónico, entidades
 * extraídas y datos pendientes.
 */
public class ConversationContext {

	private UUID businessId;
	private UUID conversationId;
	private UUID customerId;
	private String activeAgent;
	private String primaryIntent;
	private String secondaryIntent;
	private String urgency;
	private boolean requiresHuman;
	private ConversationState state;
	private final Map<String, String> entities = new LinkedHashMap<>();
	private final List<String> missingData = new ArrayList<>();

	public static ConversationContext of(UUID businessId, UUID conversationId, UUID customerId) {
		ConversationContext context = new ConversationContext();
		context.businessId = businessId;
		context.conversationId = conversationId;
		context.customerId = customerId;
		context.state = ConversationState.INICIO;
		return context;
	}

	public UUID businessId() {
		return businessId;
	}

	public UUID conversationId() {
		return conversationId;
	}

	public UUID customerId() {
		return customerId;
	}

	public String activeAgent() {
		return activeAgent;
	}

	public ConversationContext activeAgent(String activeAgent) {
		this.activeAgent = activeAgent;
		return this;
	}

	public String primaryIntent() {
		return primaryIntent;
	}

	public ConversationContext primaryIntent(String primaryIntent) {
		this.primaryIntent = primaryIntent;
		return this;
	}

	public String secondaryIntent() {
		return secondaryIntent;
	}

	public ConversationContext secondaryIntent(String secondaryIntent) {
		this.secondaryIntent = secondaryIntent;
		return this;
	}

	public String urgency() {
		return urgency;
	}

	public ConversationContext urgency(String urgency) {
		this.urgency = urgency;
		return this;
	}

	public boolean requiresHuman() {
		return requiresHuman;
	}

	public ConversationContext requiresHuman(boolean requiresHuman) {
		this.requiresHuman = requiresHuman;
		return this;
	}

	public ConversationState state() {
		return state;
	}

	public ConversationContext state(ConversationState state) {
		this.state = state;
		return this;
	}

	public Map<String, String> entities() {
		return Map.copyOf(entities);
	}

	public ConversationContext putEntity(String key, String value) {
		if (key != null && value != null) {
			entities.put(key, value);
		}
		return this;
	}

	public ConversationContext mergeEntities(Map<String, String> values) {
		if (values != null) {
			entities.putAll(values);
		}
		return this;
	}

	public List<String> missingData() {
		return List.copyOf(missingData);
	}

	public ConversationContext missingData(List<String> values) {
		missingData.clear();
		if (values != null) {
			missingData.addAll(values);
		}
		return this;
	}

	/** Vista mantenida para compatibilidad con los agentes (Map plano). */
	public Map<String, String> toEntityMap() {
		return new LinkedHashMap<>(entities);
	}

	public String summary() {
		return "state=" + (state == null ? "null" : state.name()) + " agent=" + activeAgent + " intent=" + primaryIntent
				+ " entities=" + entities.size() + " missing=" + missingData.size();
	}
}