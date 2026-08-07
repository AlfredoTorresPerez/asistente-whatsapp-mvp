package com.asistentewhatsapp.aiagents.catalog;

/**
 * Estados canónicos de la conversación (Fase 5). Sustituyen al conjunto de
 * cadenas ad-hoc que se persistían en {@code ai_conversation_context}. Cada
 * estado conoce un nombre de persistencia legado equivalente.
 */
public enum ConversationState {

	INICIO("INICIO"), IDENTIFICAR_INTENCION("INICIO"), VERIFICAR_DISPONIBILIDAD("ESPERANDO_HORARIO"), CAPTURAR_DATOS(
			"ESPERANDO_SERVICIO"), CONSULTAR_PRECIO("INICIO"), CONFIRMAR_CITA(
					"ESPERANDO_CONFIRMACION_RESERVA"), REPROGRAMAR_CITA(
							"ESPERANDO_CONFIRMACION_REPROGRAMACION"), CANCELAR_CITA(
									"ESPERANDO_CONFIRMACION_CANCELACION"), CONSULTAR_RESERVA(
											"ESPERANDO_SELECCION_RESERVA"), CONSULTAR_SERVICIOS(
													"INICIO"), CONSULTAR_HORARIOS("INICIO"), REGISTRAR_PAGO(
															"INICIO"), GESTIONAR_RECLAMO(
																	"INICIO"), DERIVAR_HUMANO("DERIVADO_HUMANO");

	private final String legacyName;

	ConversationState(String legacyName) {
		this.legacyName = legacyName;
	}

	/** Nombre histórico persistido en {@code ai_conversation_context}. */
	public String legacyName() {
		return legacyName;
	}

	public static ConversationState fromLegacy(String legacy) {
		if (legacy == null || legacy.isBlank()) {
			return INICIO;
		}
		for (ConversationState state : values()) {
			if (state.legacyName.equalsIgnoreCase(legacy)) {
				return state;
			}
		}
		try {
			return valueOf(legacy);
		} catch (IllegalArgumentException exception) {
			return INICIO;
		}
	}
}