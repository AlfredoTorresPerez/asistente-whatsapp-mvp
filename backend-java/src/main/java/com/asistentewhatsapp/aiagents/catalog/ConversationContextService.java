package com.asistentewhatsapp.aiagents.catalog;

import com.asistentewhatsapp.aiagents.application.AgentRoutingResult;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository.ConversationContextSnapshot;
import org.springframework.stereotype.Component;

/**
 * Servicio de puente entre el contexto único (Fase 6) y las estructuras de la
 * capa de aplicación: construye {@link ConversationContext} a partir de un
 * snapshot persistido o de un resultado de ruteo, y une la máquina de estados.
 */
@Component
public class ConversationContextService {

	private final ConversationStateMachine stateMachine;

	public ConversationContextService(ConversationStateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}

	public ConversationContext fromSnapshot(ConversationContextSnapshot snapshot, java.util.UUID businessId,
			java.util.UUID conversationId, java.util.UUID customerId) {
		ConversationContext context = ConversationContext.of(businessId, conversationId, customerId);
		if (snapshot == null) {
			context.state(stateMachine.initialState());
			return context;
		}
		context.activeAgent(snapshot.activeAgent() == null ? null : snapshot.activeAgent().name())
				.primaryIntent(snapshot.primaryIntent() == null ? null : snapshot.primaryIntent().name())
				.secondaryIntent(snapshot.secondaryIntent() == null ? null : snapshot.secondaryIntent().name())
				.state(stateMachine.fromLegacy(legacyFromSnapshot(snapshot))).mergeEntities(snapshot.extractedData())
				.missingData(snapshot.missingData());
		return context;
	}

	public ConversationContext fromResult(AgentRoutingResult result) {
		if (result == null) {
			return ConversationContext.of(null, null, null);
		}
		ConversationState state = stateMachine.fromLegacy(stateMachine.deriveLegacyColumn(result.agentType(),
				result.primaryIntent(), result.requiresHuman(), result.missingData(), result.responseToCustomer()));
		return ConversationContext.of(result.businessId(), result.conversationId(), result.customerId())
				.activeAgent(result.agentType() == null ? null : result.agentType().name())
				.primaryIntent(result.primaryIntent() == null ? null : result.primaryIntent().name())
				.secondaryIntent(result.secondaryIntent() == null ? null : result.secondaryIntent().name())
				.urgency(result.urgency()).requiresHuman(result.requiresHuman()).state(state)
				.mergeEntities(result.extractedData()).missingData(result.missingData());
	}

	/** Calcula el estado canónico de una intención según el catálogo. */
	public ConversationState canonicalStateFor(AgentIntent intent, boolean requiresHuman,
			java.util.List<String> missingData) {
		return stateMachine.nextState(stateMachine.initialState(), intent, requiresHuman, missingData);
	}

	private String legacyFromSnapshot(ConversationContextSnapshot snapshot) {
		if (snapshot == null) {
			return null;
		}
		// Si el snapshot no expone el estado, se infiere de la intención y datos.
		return snapshot.primaryIntent() == null ? null : snapshot.primaryIntent().name();
	}
}