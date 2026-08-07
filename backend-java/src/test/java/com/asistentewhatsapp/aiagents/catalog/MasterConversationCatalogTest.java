package com.asistentewhatsapp.aiagents.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.EntityDefinition;
import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.IntentDefinition;
import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog.ResponseDefinition;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MasterConversationCatalogTest {

	private final MasterConversationCatalog catalog = MasterConversationCatalog.shared();
	private final ConversationRuleEvaluator ruleEvaluator = new ConversationRuleEvaluator();
	private final LanguageNormalizer normalizer = LanguageNormalizer.shared();
	private final RelativeDateService relativeDates = RelativeDateService.shared();
	private final ConversationStateMachine machine = ConversationStateMachine.defaults();

	@Test
	void catalogLoadsAllCoreSections() {
		assertThat(catalog.intents()).hasSize(AgentIntent.values().length);
		assertThat(catalog.entities()).isNotEmpty();
		assertThat(catalog.synonymGroup("BOOKING_WORDS")).isNotEmpty();
		assertThat(catalog.taxonomy()).isNotEmpty();
		assertThat(catalog.states()).isNotEmpty();
		assertThat(catalog.transitions()).isNotEmpty();
		assertThat(catalog.rules()).isNotEmpty();
	}

	@Test
	void everyIntentHasAgentConfidenceAndGroupRefs() {
		for (IntentDefinition intent : catalog.intents()) {
			assertThat(intent.agent()).as("agent de %s", intent.code()).isNotBlank();
			assertThat(AgentType.valueOf(intent.agent())).as("AgentType de %s", intent.code()).isNotNull();
			assertThat(intent.confidence()).as("confidence de %s", intent.code()).isGreaterThan(0.0);
			for (String group : intent.synonymGroups()) {
				assertThat(catalog.synonymGroup(group)).as("grupo %s de %s", group, intent.code()).isNotEmpty();
			}
		}
	}

	@Test
	void catalogCoversEveryAgentIntentExactly() {
		Set<String> covered = new HashSet<>();
		for (IntentDefinition intent : catalog.intents()) {
			covered.add(intent.code());
		}
		Set<String> expected = new HashSet<>(Arrays.stream(AgentIntent.values()).map(AgentIntent::name).toList());
		assertThat(covered).containsExactlyInAnyOrderElementsOf(expected);
	}

	@Test
	void intentToAgentMappingMatchesRuntimeRouting() {
		assertThat(catalog.intent("BOOKING_REQUEST").agent()).isEqualTo("BOOKING");
		assertThat(catalog.intent("PRICE_REQUEST").agent()).isEqualTo("SALES");
		assertThat(catalog.intent("COMPLAINT").agent()).isEqualTo("HUMAN_HANDOFF");
		assertThat(catalog.intent("TECHNICAL_MESSAGE").agent()).isEqualTo("SUPPORT");
	}

	@Test
	void catalogCodesMapToIntents() {
		assertThat(catalog.mapCatalogCodeToAgentIntent("BOOKING_CREATE")).contains(AgentIntent.BOOKING_REQUEST);
		assertThat(catalog.mapCatalogCodeToAgentIntent("BOOKING_RESCHEDULE")).contains(AgentIntent.BOOKING_CHANGE);
		assertThat(catalog.mapCatalogCodeToAgentIntent("SERVICE_PRICE")).contains(AgentIntent.PRICE_REQUEST);
		assertThat(catalog.mapCatalogCodeToAgentIntent("DESCONOCIDO")).isEmpty();
	}

	@Test
	void legacyStatesMapToCanonical() {
		assertThat(catalog.mapLegacyState("ESPERANDO_SERVICIO")).contains("CAPTURAR_DATOS");
		assertThat(catalog.mapLegacyState("ESPERANDO_CONFIRMACION_RESERVA")).contains("CONFIRMAR_CITA");
		assertThat(catalog.mapLegacyState("ESPERANDO_HORARIO")).contains("VERIFICAR_DISPONIBILIDAD");
		assertThat(catalog.mapLegacyState("DERIVADO_HUMANO")).contains("DERIVAR_HUMANO");
	}

	@Test
	void stateMachineDerivesSameLegacyColumns() {
		assertThat(machine.deriveLegacyColumn(AgentType.BOOKING, AgentIntent.BOOKING_REQUEST, false,
				List.of("motivo_o_servicio"), null)).isEqualTo("ESPERANDO_SERVICIO");
		assertThat(machine.deriveLegacyColumn(AgentType.BOOKING, AgentIntent.BOOKING_REQUEST, false,
				List.of("horario_preferido"), null)).isEqualTo("ESPERANDO_HORARIO");
		assertThat(machine.deriveLegacyColumn(AgentType.BOOKING, AgentIntent.BOOKING_REQUEST, false,
				List.of("nueva_fecha_u_horario"), null)).isEqualTo("ESPERANDO_FECHA_REPROGRAMACION");
		assertThat(machine.deriveLegacyColumn(AgentType.BOOKING, AgentIntent.BOOKING_REQUEST, false, List.of(),
				"/reservas/confirmar/abc")).isEqualTo("ESPERANDO_CONFIRMACION_RESERVA");
		assertThat(machine.deriveLegacyColumn(AgentType.BOOKING, AgentIntent.BOOKING_REQUEST, false, List.of(), null))
				.isEqualTo("INICIO");
		assertThat(machine.deriveLegacyColumn(AgentType.HUMAN_HANDOFF, AgentIntent.COMPLAINT, true, List.of(), null))
				.isEqualTo("DERIVADO_HUMANO");
	}

	@Test
	void stateMachineTransitionsUseCatalog() {
		assertThat(machine.nextState(ConversationState.INICIO, AgentIntent.AMBIGUOUS, false, List.of()))
				.isEqualTo(ConversationState.IDENTIFICAR_INTENCION);
		ConversationState afterBooking = machine.nextState(ConversationState.INICIO, AgentIntent.BOOKING_REQUEST, false,
				List.of("motivo_o_servicio"));
		assertThat(afterBooking).isEqualTo(ConversationState.CAPTURAR_DATOS);
		assertThat(machine.nextState(ConversationState.INICIO, AgentIntent.COMPLAINT, true, List.of()))
				.isEqualTo(ConversationState.DERIVAR_HUMANO);
	}

	@Test
	void languageNormalizerIsCentralAndBehaviorPreserved() {
		assertThat(normalizer.normalize("Hola, ¿Cómo Estás?")).isEqualTo("hola como estas");
		assertThat(normalizer.normalize("DEPILACIÓN-LÁSER")).isEqualTo("depilacion laser");
		assertThat(normalizer.normalizeWithTypoFix("Quiero reserbar una hora")).isEqualTo("quiero reservar una hora");
		assertThat(normalizer.normalizeWithTypoFix("Quiero pedir ora")).isEqualTo("quiero pedir hora");
		assertThat(normalizer.contains("Hola mundo", "MUNDO")).isTrue();
	}

	@Test
	void relativeDateServiceResolvesLabels() {
		LocalDate today = LocalDate.of(2026, 8, 4); // martes
		assertThat(relativeDates.resolve("mañana", today)).contains(today.plusDays(1));
		assertThat(relativeDates.resolve("hoy", today)).contains(today);
		assertThat(relativeDates.resolve("pasado mañana", today)).contains(today.plusDays(2));
		assertThat(relativeDates.resolve("sábado", today)).contains(today.plusDays(4)); // 2026-08-08 sábado
		assertThat(relativeDates.resolve("lunes", today)).contains(today.plusDays(6)); // 2026-08-10 lunes
		assertThat(relativeDates.resolve("domingo futuro", today)).isEmpty();
		assertThat(relativeDates.weekdayMap()).containsEntry("sabado", "sábado");
	}

	@Test
	void conversationRulesAreCentralized() {
		assertThat(ruleEvaluator.evaluate("no quiero cancelar, solo cambiar la hora")).isPresent()
				.hasValueSatisfying(result -> assertThat(result.intent()).isEqualTo("BOOKING_CHANGE"));
		assertThat(ruleEvaluator.evaluate("ok")).isPresent()
				.hasValueSatisfying(result -> assertThat(result.intent()).isEqualTo("AMBIGUOUS"));
		assertThat(ruleEvaluator.evaluate("mejor no")).isPresent()
				.hasValueSatisfying(result -> assertThat(result.intent()).isEqualTo("AMBIGUOUS"));
		assertThat(ruleEvaluator.evaluate("no puedo ir el lunes")).isPresent()
				.hasValueSatisfying(result -> assertThat(result.intent()).isEqualTo("BOOKING_CHANGE"));
		assertThat(ruleEvaluator.evaluate("hola buenas tardes")).isEmpty();
	}

	@Test
	void entitiesCoverRequiredSlots() {
		List<EntityDefinition> entityList = catalog.entities();
		assertThat(entityList.stream().map(EntityDefinition::key)).contains("servicio_o_producto", "sede", "hora",
				"fecha", "fecha_relativa", "cliente");
	}

	@Test
	void everyIntentHasResponseTemplates() {
		for (IntentDefinition intent : catalog.intents()) {
			ResponseDefinition response = catalog.findResponse(intent.code()).orElse(null);
			assertThat(response).as("respuesta de %s", intent.code()).isNotNull();
			assertThat(response.templates()).containsKeys("initial", "missingData", "success", "error", "handoff",
					"farewell");
		}
	}

	@Test
	void taxonomyHasRepresentativeReservedPhrases() {
		List<String> phrases = catalog.taxonomy().stream()
				.map(MasterConversationCatalog.TaxonomyPhrase::normalizedPhrase).toList();
		assertThat(phrases).contains("hola", "buenas");
		assertThat(phrases.stream().filter(phrase -> phrase.contains("reserv") || phrase.contains("cancelar")))
				.isNotEmpty();
	}
}