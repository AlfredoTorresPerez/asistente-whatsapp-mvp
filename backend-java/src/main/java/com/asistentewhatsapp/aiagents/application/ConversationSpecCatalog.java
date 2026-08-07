package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.catalog.ConversationRuleEvaluator;
import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog;
import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.TaxonomyPhrase;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Catálogo de conversación: adaptador sobre el
 * {@link MasterConversationCatalog}. La taxonomía de frases ya no se lee de
 * {@code intents.json} en runtime (ese archivo es ahora solo un artefacto de
 * documento); la fuente única son las frases consolidadas del catálogo maestro.
 * Las reglas de conversación se delegan en {@link ConversationRuleEvaluator}.
 */
@Component
public class ConversationSpecCatalog {

	private final List<IntentPhrase> phrases;
	private final ConversationRuleEvaluator ruleEvaluator;

	public ConversationSpecCatalog() {
		this(MasterConversationCatalog.shared().taxonomy().stream().map(ConversationSpecCatalog::toIntentPhrase)
				.filter(java.util.Objects::nonNull).toList());
	}

	ConversationSpecCatalog(List<IntentPhrase> phrases) {
		this(phrases, new ConversationRuleEvaluator());
	}

	ConversationSpecCatalog(List<IntentPhrase> phrases, ConversationRuleEvaluator ruleEvaluator) {
		this.phrases = phrases == null ? List.of() : List.copyOf(phrases);
		this.ruleEvaluator = ruleEvaluator == null ? new ConversationRuleEvaluator() : ruleEvaluator;
	}

	private static IntentPhrase toIntentPhrase(TaxonomyPhrase phrase) {
		AgentIntent intent;
		try {
			intent = AgentIntent.valueOf(phrase.intent());
		} catch (IllegalArgumentException exception) {
			return null;
		}
		return new IntentPhrase(phrase.normalizedPhrase() == null ? "" : phrase.normalizedPhrase(), intent,
				phrase.confidence(), phrase.urgency(), phrase.requiresHuman(), phrase.reason());
	}

	public Optional<IntentDetectionResult> detect(String normalizedText) {
		if (normalizedText == null || normalizedText.isBlank()) {
			return Optional.empty();
		}

		Optional<ConversationRuleEvaluator.RuleViolation> safetyRule = ruleEvaluator.evaluate(normalizedText);
		if (safetyRule.isPresent()) {
			AgentIntent intent = parsedIntent(safetyRule.get().intent());
			return Optional.of(new IntentDetectionResult(intent, null, safetyRule.get().confidence(),
					safetyRule.get().urgency(), false, safetyRule.get().reason()));
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

	private AgentIntent parsedIntent(String intentCode) {
		if (intentCode == null || intentCode.isBlank()) {
			return AgentIntent.AMBIGUOUS;
		}
		try {
			return AgentIntent.valueOf(intentCode);
		} catch (IllegalArgumentException exception) {
			return AgentIntent.AMBIGUOUS;
		}
	}

	record IntentPhrase(String normalizedPhrase, AgentIntent intent, double confidence, String urgency,
			boolean requiresHuman, String reason) {
	}
}