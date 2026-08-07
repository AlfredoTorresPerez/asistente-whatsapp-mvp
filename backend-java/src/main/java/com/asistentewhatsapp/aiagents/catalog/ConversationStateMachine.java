package com.asistentewhatsapp.aiagents.catalog;

import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.TransitionDefinition;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Máquina de estados de conversación (Fase 5). Centraliza la derivación del
 * estado a partir de un resultado de ruteo (sustituyendo a
 * {@code AiAgentJdbcRepository.deriveConversationState}) y calcula transiciones
 * usando las transiciones declaradas en el catálogo maestro.
 */
@Component
public class ConversationStateMachine {

	private final MasterConversationCatalog catalog;

	public ConversationStateMachine(MasterConversationCatalog catalog) {
		this.catalog = catalog;
	}

	public static ConversationStateMachine defaults() {
		return new ConversationStateMachine(MasterConversationCatalog.shared());
	}

	public ConversationState initialState() {
		return ConversationState.INICIO;
	}

	/** Mapea un estado persistido (canónico o legado) a su equivalente canónico. */
	public ConversationState fromLegacy(String legacy) {
		if (legacy == null || legacy.isBlank()) {
			return ConversationState.INICIO;
		}
		if (isCanonical(legacy)) {
			return ConversationState.valueOf(legacy);
		}
		return ConversationState.fromLegacy(catalog.mapLegacyState(legacy).orElse(legacy));
	}

	public boolean isCanonical(String value) {
		try {
			ConversationState.valueOf(value);
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	/**
	 * Deriva el estado de conversación con la precedencia histórica de
	 * {@code AiAgentJdbcRepository.deriveConversationState}. La columna legada
	 * devuelta es idéntica a la que se venía persistiendo.
	 */
	public String deriveLegacyColumn(AgentType agentType, AgentIntent primaryIntent, boolean requiresHuman,
			List<String> missingData, String responseToCustomer) {
		if (requiresHuman || agentType == AgentType.HUMAN_HANDOFF) {
			return ConversationState.DERIVAR_HUMANO.legacyName();
		}
		List<String> missing = missingData == null ? List.of() : missingData;
		if (missing.contains("motivo_o_servicio") || missing.contains("servicio_o_producto")) {
			return "ESPERANDO_SERVICIO";
		}
		if (missing.contains("sucursal")) {
			return "ESPERANDO_SUCURSAL";
		}
		if (missing.contains("fecha_deseada")) {
			return "ESPERANDO_FECHA";
		}
		if (missing.contains("horario_preferido")) {
			return ConversationState.VERIFICAR_DISPONIBILIDAD.legacyName();
		}
		if (missing.contains("seleccion_reserva")) {
			return ConversationState.CONSULTAR_RESERVA.legacyName();
		}
		if (missing.contains("confirmacion_cancelacion")) {
			return ConversationState.CANCELAR_CITA.legacyName();
		}
		if (missing.contains("nueva_fecha_u_horario")) {
			return "ESPERANDO_FECHA_REPROGRAMACION";
		}
		if (primaryIntent == AgentIntent.BOOKING_REQUEST && contains(responseToCustomer, "/reservas/confirmar/")) {
			return ConversationState.CONFIRMAR_CITA.legacyName();
		}
		if (primaryIntent == AgentIntent.BOOKING_CHANGE && contains(responseToCustomer, "reprogram")) {
			return ConversationState.REPROGRAMAR_CITA.legacyName();
		}
		if (primaryIntent == AgentIntent.BOOKING_CANCEL && contains(responseToCustomer, "cancel")) {
			return ConversationState.CANCELAR_CITA.legacyName();
		}
		return ConversationState.INICIO.legacyName();
	}

	/** Convierte un estado canónico a su nombre de persistencia legado. */
	public String toLegacy(ConversationState state) {
		return state == null ? ConversationState.INICIO.legacyName() : state.legacyName();
	}

	/**
	 * Calcula el siguiente estado para la intención detectada usando las
	 * transiciones del catálogo. Las derivaciones humanas y los datos pendientes
	 * dominan sobre las transiciones declarativas.
	 */
	public ConversationState nextState(ConversationState current, AgentIntent intent, boolean requiresHuman,
			List<String> missingData) {
		if (current == null) {
			current = initialState();
		}
		if (requiresHuman) {
			return ConversationState.DERIVAR_HUMANO;
		}
		if (intent == null) {
			return current;
		}
		Optional<TransitionDefinition> transition = findTransition(current, intent);
		if (transition.isPresent()) {
			ConversationState target = parsedState(transition.get().to());
			if (requiresData(intent) && (missingData == null || missingData.isEmpty())) {
				return target;
			}
			return target;
		}
		return current;
	}

	private boolean requiresData(AgentIntent intent) {
		return intent == AgentIntent.BOOKING_REQUEST || intent == AgentIntent.BOOKING_CHANGE
				|| intent == AgentIntent.COMMERCIAL_AND_BOOKING;
	}

	private Optional<TransitionDefinition> findTransition(ConversationState state, AgentIntent intent) {
		Optional<TransitionDefinition> direct = catalog.transitions().stream()
				.filter(transition -> intent.name().equals(transition.onIntent()))
				.filter(transition -> state.name().equals(transition.from())).findFirst();
		if (direct.isPresent()) {
			return direct;
		}
		return catalog.transitions().stream().filter(transition -> intent.name().equals(transition.onIntent()))
				.filter(transition -> ConversationState.INICIO.name().equals(transition.from())).findFirst();
	}

	private ConversationState parsedState(String code) {
		try {
			return ConversationState.valueOf(code);
		} catch (IllegalArgumentException exception) {
			return ConversationState.INICIO;
		}
	}

	private boolean contains(String value, String expected) {
		return value != null && expected != null && value.toLowerCase(Locale.ROOT).contains(expected);
	}
}