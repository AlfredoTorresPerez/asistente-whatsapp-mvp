package com.asistentewhatsapp.aiagents.catalog;

import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.RuleDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Evaluador central de reglas de conversación (antes embebidas en
 * {@code ConversationSpecCatalog.detectSafetyRule}). Las reglas habilitadas se
 * leen del catálogo maestro (sección {@code rules} con type=CONVERSATION) y se
 * aplican con la misma semántica histórica para no alterar ningún caso
 * existente.
 */
@Component
public class ConversationRuleEvaluator {

	private static final String CONVERSATION_RULE = "CONVERSATION";
	private static final List<String> BARE_REJECTIONS = List.of("mejor no", "sabes que mejor no", "sabes que, mejor no",
			"no mejor no");

	private final MasterConversationCatalog catalog;
	private final LanguageNormalizer languageNormalizer;

	public ConversationRuleEvaluator(MasterConversationCatalog catalog, LanguageNormalizer languageNormalizer) {
		this.catalog = catalog;
		this.languageNormalizer = languageNormalizer;
	}

	public ConversationRuleEvaluator() {
		this(MasterConversationCatalog.shared(), LanguageNormalizer.shared());
	}

	public Optional<RuleViolation> evaluate(String normalizedText) {
		if (normalizedText == null || normalizedText.isBlank()) {
			return Optional.empty();
		}
		RuleDefinition safety1 = rule("SAFETY-1");
		if (safety1 != null && isIsolatedEmojiOrAck(normalizedText)) {
			return violation(safety1, "mensaje ambiguo segun reglas de conversacion");
		}
		RuleDefinition safety2 = rule("SAFETY-2");
		if (safety2 != null && isBareRejectionWithoutObject(normalizedText)) {
			return violation(safety2, "rechazo breve sin objeto de agenda");
		}
		RuleDefinition safety3 = rule("SAFETY-3");
		if (safety3 != null) {
			boolean negatedCancel = containsAny(normalizedText, "no quiero cancelar", "no deseo cancelar",
					"no es para cancelar", "no cancelar");
			boolean changeSignal = containsAny(normalizedText, "cambiar", "reprogramar", "reagendar", "mover");
			if (negatedCancel && changeSignal) {
				return violation(safety3, "negacion explicita inhibe cancelar");
			}
		}
		RuleDefinition safety4 = rule("SAFETY-4");
		if (safety4 != null) {
			boolean cannotAttend = containsAny(normalizedText, "no puedo", "no podre", "no podré", "no alcanzo",
					"no llego");
			boolean hasNewDateOrTime = containsAny(normalizedText, "manana", "mañana", "hoy", "lunes", "martes",
					"miercoles", "miércoles", "jueves", "viernes", "sabado", "sábado", "domingo", " a las ")
					|| normalizedText.matches(".*\\b[0-2]?\\d(?::[0-5]\\d)?\\b.*");
			if (cannotAttend && hasNewDateOrTime) {
				return violation(safety4, "fecha u horario nuevo junto a imposibilidad de asistir");
			}
		}
		return Optional.empty();
	}

	private RuleDefinition rule(String id) {
		return catalog.findRule(id).orElse(null);
	}

	private Optional<RuleViolation> violation(RuleDefinition definition, String fallbackReason) {
		String reason = definition.reason() == null || definition.reason().isBlank()
				? fallbackReason
				: definition.reason();
		return Optional.of(new RuleViolation(definition.id(), definition.intent(), confidence(definition),
				definition.urgency(), reason));
	}

	private double confidence(RuleDefinition definition) {
		return Double.isNaN(definition.confidence()) ? 0.7 : definition.confidence();
	}

	private boolean isIsolatedEmojiOrAck(String normalizedText) {
		return normalizedText.equals("ok") || normalizedText.equals("oki")
				|| (normalizedText.length() <= 2 && !normalizedText.matches(".*[a-z0-9].*"));
	}

	private boolean isBareRejectionWithoutObject(String normalizedText) {
		return BARE_REJECTIONS.contains(normalizedText);
	}

	private boolean containsAny(String normalizedText, String... candidates) {
		for (String candidate : candidates) {
			if (normalizedText.contains(languageNormalizer.normalize(candidate))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Resultado de una regla de conversación aplicada. {@code intent} es el código
	 * de intención (ver AgentIntent.name()).
	 */
	public record RuleViolation(String ruleId, String intent, double confidence, String urgency, String reason) {
	}
}