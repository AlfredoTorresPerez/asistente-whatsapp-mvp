package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ConversationSpecCatalog {

	private static final String INTENTS_RESOURCE = "/conversation/intents.json";

	private final List<IntentPhrase> phrases;

	public ConversationSpecCatalog() {
		this(loadPhrases());
	}

	ConversationSpecCatalog(List<IntentPhrase> phrases) {
		this.phrases = phrases == null ? List.of() : List.copyOf(phrases);
	}

	public Optional<IntentDetectionResult> detect(String normalizedText) {
		if (normalizedText == null || normalizedText.isBlank()) {
			return Optional.empty();
		}

		Optional<IntentDetectionResult> safetyRule = detectSafetyRule(normalizedText);
		if (safetyRule.isPresent()) {
			return safetyRule;
		}

		IntentPhrase best = null;
		for (IntentPhrase phrase : phrases) {
			if (phrase.normalizedPhrase().isBlank()) {
				continue;
			}
			boolean matches = normalizedText.equals(phrase.normalizedPhrase())
					|| (phrase.normalizedPhrase().length() >= 8 && normalizedText.contains(phrase.normalizedPhrase()));
			if (matches && (best == null || phrase.normalizedPhrase().length() > best.normalizedPhrase().length())) {
				best = phrase;
			}
		}
		if (best == null) {
			return Optional.empty();
		}
		return Optional.of(new IntentDetectionResult(best.intent(), null, best.confidence(), best.urgency(),
				best.requiresHuman(), best.reason()));
	}

	private Optional<IntentDetectionResult> detectSafetyRule(String normalizedText) {
		if (isIsolatedEmojiOrAck(normalizedText)) {
			return Optional.of(new IntentDetectionResult(AgentIntent.AMBIGUOUS, null, 0.72, "bajo", false,
					"mensaje ambiguo segun reglas de conversacion"));
		}
		if (isBareRejectionWithoutObject(normalizedText)) {
			return Optional.of(new IntentDetectionResult(AgentIntent.AMBIGUOUS, null, 0.74, "bajo", false,
					"rechazo breve sin objeto de agenda"));
		}
		boolean negatedCancel = containsAny(normalizedText, "no quiero cancelar", "no deseo cancelar",
				"no es para cancelar", "no cancelar");
		boolean changeSignal = containsAny(normalizedText, "cambiar", "reprogramar", "reagendar", "mover");
		if (negatedCancel && changeSignal) {
			return Optional.of(new IntentDetectionResult(AgentIntent.BOOKING_CHANGE, null, 0.93, "medio", false,
					"negacion explicita inhibe cancelar"));
		}
		boolean cannotAttend = containsAny(normalizedText, "no puedo", "no podre", "no podré", "no alcanzo",
				"no llego");
		boolean hasNewDateOrTime = containsAny(normalizedText, "manana", "mañana", "hoy", "lunes", "martes",
				"miercoles", "miércoles", "jueves", "viernes", "sabado", "sábado", "domingo", " a las ")
				|| normalizedText.matches(".*\\b[0-2]?\\d(?::[0-5]\\d)?\\b.*");
		if (cannotAttend && hasNewDateOrTime) {
			return Optional.of(new IntentDetectionResult(AgentIntent.BOOKING_CHANGE, null, 0.88, "medio", false,
					"fecha u horario nuevo junto a imposibilidad de asistir"));
		}
		return Optional.empty();
	}

	private boolean isIsolatedEmojiOrAck(String normalizedText) {
		return normalizedText.equals("ok") || normalizedText.equals("oki")
				|| (normalizedText.length() <= 2 && !normalizedText.matches(".*[a-z0-9].*"));
	}

	private boolean isBareRejectionWithoutObject(String normalizedText) {
		return normalizedText.equals("mejor no") || normalizedText.equals("sabes que mejor no")
				|| normalizedText.equals("sabes que, mejor no") || normalizedText.equals("no mejor no");
	}

	private static List<IntentPhrase> loadPhrases() {
		ObjectMapper mapper = new ObjectMapper();
		try (InputStream input = ConversationSpecCatalog.class.getResourceAsStream(INTENTS_RESOURCE)) {
			if (input == null) {
				return List.of();
			}
			Map<String, Object> envelope = mapper.readValue(input, new TypeReference<>() {
			});
			Object rawItems = envelope.get("items");
			if (!(rawItems instanceof List<?> items)) {
				return List.of();
			}
			List<IntentPhrase> loaded = new ArrayList<>();
			for (Object item : items) {
				if (!(item instanceof Map<?, ?> row)) {
					continue;
				}
				String spreadsheetIntent = stringValue(row.get("intencion"));
				AgentIntent mappedIntent = mapSpreadsheetIntent(spreadsheetIntent);
				if (mappedIntent == null) {
					continue;
				}
				String examples = stringValue(row.get("ejemplos_tipicos"));
				for (String example : examples.split("\\|")) {
					String normalizedPhrase = TextNormalizer.normalize(example);
					if (!normalizedPhrase.isBlank()) {
						loaded.add(new IntentPhrase(normalizedPhrase, mappedIntent, confidence(row), urgency(row),
								requiresHuman(mappedIntent), "taxonomia_conversacion:" + spreadsheetIntent));
					}
				}
			}
			return loaded;
		} catch (RuntimeException | java.io.IOException exception) {
			return List.of();
		}
	}

	private static AgentIntent mapSpreadsheetIntent(String intent) {
		return switch (intent == null ? "" : intent.trim()) {
			case "saludar" -> AgentIntent.GREETING;
			case "despedirse", "agradecer" -> AgentIntent.THANKS_OR_FAREWELL;
			case "solicitar_ayuda" -> AgentIntent.SUPPORT_GENERAL;
			case "consultar_servicios" -> AgentIntent.COMMERCIAL_INQUIRY;
			case "consultar_precio" -> AgentIntent.PRICE_REQUEST;
			case "consultar_duracion" -> AgentIntent.SERVICE_INFORMATION;
			case "consultar_promocion" -> AgentIntent.COMMERCIAL_INQUIRY;
			case "consultar_sucursales", "consultar_direccion" -> AgentIntent.LOCATION_QUERY;
			case "consultar_horario_empresa" -> AgentIntent.BUSINESS_HOURS_QUERY;
			case "consultar_profesionales", "seleccionar_profesional" -> AgentIntent.PROFESSIONAL_QUERY;
			case "consultar_disponibilidad", "rechazar_horario" -> AgentIntent.AVAILABILITY_QUERY;
			case "reservar", "seleccionar_servicio", "seleccionar_sucursal", "seleccionar_horario", "confirmar_reserva",
					"enviar_datos_cliente", "corregir_dato" ->
				AgentIntent.BOOKING_REQUEST;
			case "consultar_reserva", "listar_reservas", "seleccionar_reserva", "confirmar_asistencia",
					"consultar_politica_cancelacion" ->
				AgentIntent.BOOKING_STATUS;
			case "reprogramar", "confirmar_reprogramacion", "rechazar_reprogramacion" -> AgentIntent.BOOKING_CHANGE;
			case "cancelar", "confirmar_cancelacion", "rechazar_cancelacion" -> AgentIntent.BOOKING_CANCEL;
			case "consultar_medio_pago", "consultar_abono", "solicitar_comprobante" -> AgentIntent.PAYMENT_INQUIRY;
			case "solicitar_devolucion" -> AgentIntent.PAYMENT_PROBLEM;
			case "hablar_con_persona" -> AgentIntent.HUMAN_REQUEST;
			case "presentar_reclamo" -> AgentIntent.COMPLAINT;
			case "mensaje_fuera_de_contexto", "mensaje_incomprensible", "cambiar_intencion" -> AgentIntent.AMBIGUOUS;
			default -> null;
		};
	}

	private static double confidence(Map<?, ?> row) {
		String priority = stringValue(row.get("prioridad")).toLowerCase(Locale.ROOT);
		if (priority.contains("crit")) {
			return 0.9;
		}
		if (priority.contains("alta")) {
			return 0.84;
		}
		if (priority.contains("media")) {
			return 0.78;
		}
		return 0.72;
	}

	private static String urgency(Map<?, ?> row) {
		String priority = stringValue(row.get("prioridad")).toLowerCase(Locale.ROOT);
		return priority.contains("crit") ? "alto" : priority.contains("alta") ? "medio" : "bajo";
	}

	private static boolean requiresHuman(AgentIntent intent) {
		return intent == AgentIntent.HUMAN_REQUEST || intent == AgentIntent.COMPLAINT
				|| intent == AgentIntent.PAYMENT_PROBLEM;
	}

	private boolean containsAny(String normalized, String... candidates) {
		for (String candidate : candidates) {
			if (normalized.contains(TextNormalizer.normalize(candidate))) {
				return true;
			}
		}
		return false;
	}

	private static String stringValue(Object value) {
		return value == null ? "" : value.toString().trim();
	}

	record IntentPhrase(String normalizedPhrase, AgentIntent intent, double confidence, String urgency,
			boolean requiresHuman, String reason) {
	}
}
