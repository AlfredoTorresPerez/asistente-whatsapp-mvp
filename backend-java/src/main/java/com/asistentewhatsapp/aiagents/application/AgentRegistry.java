package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog;
import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.IntentDefinition;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistry {

	private final Map<AgentType, AgentHandler> handlers = new EnumMap<>(AgentType.class);
	private final Map<AgentIntent, AgentType> intentToAgent;

	public AgentRegistry(List<AgentHandler> agentHandlers) {
		this(agentHandlers, MasterConversationCatalog.shared());
	}

	@Autowired
	public AgentRegistry(List<AgentHandler> agentHandlers, MasterConversationCatalog masterCatalog) {
		for (AgentHandler handler : agentHandlers) {
			handlers.put(handler.type(), handler);
		}
		this.intentToAgent = buildIntentToAgent(masterCatalog);
	}

	private Map<AgentIntent, AgentType> buildIntentToAgent(MasterConversationCatalog masterCatalog) {
		Map<AgentIntent, AgentType> mapping = new EnumMap<>(AgentIntent.class);
		for (IntentDefinition definition : masterCatalog.intents()) {
			if (definition.agent() == null) {
				continue;
			}
			AgentType type;
			try {
				type = AgentType.valueOf(definition.agent());
			} catch (IllegalArgumentException exception) {
				continue;
			}
			AgentIntent intent;
			try {
				intent = AgentIntent.valueOf(definition.code());
			} catch (IllegalArgumentException exception) {
				continue;
			}
			mapping.put(intent, type);
		}
		return mapping;
	}

	public AgentHandler resolve(IntentDetectionResult intent) {
		AgentType agentType = resolveAgentType(intent);
		AgentHandler handler = handlers.get(agentType);
		if (handler == null) {
			return handlers.get(AgentType.RECEPTION);
		}
		return handler;
	}

	private AgentType resolveAgentType(IntentDetectionResult intent) {
		if (intent.requiresHuman()) {
			return AgentType.HUMAN_HANDOFF;
		}
		AgentIntent primaryIntent = intent.primaryIntent();
		AgentType mapped = intentToAgent.get(primaryIntent);
		return mapped == null ? AgentType.RECEPTION : mapped;
	}
}