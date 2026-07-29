package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistry {

	private final Map<AgentType, AgentHandler> handlers = new EnumMap<>(AgentType.class);

	public AgentRegistry(List<AgentHandler> agentHandlers) {
		for (AgentHandler handler : agentHandlers) {
			handlers.put(handler.type(), handler);
		}
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
		return switch (primaryIntent) {
			case GREETING, THANKS_OR_FAREWELL, AMBIGUOUS -> AgentType.RECEPTION;
			case COMMERCIAL_INQUIRY, SERVICE_INFORMATION, SERVICE_RECOMMENDATION, PRICE_REQUEST, QUOTE_REQUEST ->
				AgentType.SALES;
			case COMMERCIAL_AND_BOOKING -> AgentType.BOOKING;
			case AVAILABILITY_QUERY, PROFESSIONAL_QUERY, BOOKING_REQUEST, BOOKING_CHANGE, BOOKING_CANCEL,
					BOOKING_STATUS, WAITLIST_QUERY ->
				AgentType.BOOKING;
			case PAYMENT_INQUIRY, PAYMENT_PROBLEM -> AgentType.PAYMENTS;
			case LOCATION_QUERY, BUSINESS_HOURS_QUERY, SUPPORT_GENERAL, TECHNICAL_MESSAGE -> AgentType.SUPPORT;
			case KNOWLEDGE_QUERY -> AgentType.KNOWLEDGE;
			case FOLLOW_UP -> AgentType.FOLLOW_UP;
			case COMPLAINT, HUMAN_REQUEST -> AgentType.HUMAN_HANDOFF;
		};
	}
}
