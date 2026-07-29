package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConversationWorkbookResourceIntegrityTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void productionResourcesAreGeneratedFromWorkbookWithoutRuntimeXlsxDependency() throws Exception {
		List<Map<String, Object>> intents = items("/conversation/intents.json");
		List<Map<String, Object>> states = items("/conversation/states.json");
		List<Map<String, Object>> transitions = items("/conversation/transitions.json");
		List<Map<String, Object>> responses = items("/conversation/responses.json");
		List<Map<String, Object>> rules = items("/conversation/rules.json");
		List<Map<String, Object>> ambiguous = items("/conversation/ambiguous-contexts.json");

		assertThat(intents).hasSize(42);
		assertThat(states).hasSize(24);
		assertThat(transitions).hasSize(30);
		assertThat(responses).hasSize(47);
		assertThat(rules).hasSize(10);
		assertThat(ambiguous).hasSize(10);

		Set<String> intentNames = values(intents, "intencion");
		Set<String> stateNames = values(states, "estado");
		stateNames.add("CUALQUIERA");

		assertThat(intentNames).contains("reservar", "consultar_disponibilidad", "reprogramar", "cancelar",
				"confirmar_reserva", "confirmar_cancelacion", "confirmar_reprogramacion");

		for (Map<String, Object> response : responses) {
			assertThat(intentNames).as("response intent " + response).contains((String) response.get("intencion"));
			assertThat(stateNames).as("response state " + response).contains((String) response.get("estado"));
		}
		for (Map<String, Object> transition : transitions) {
			assertThat(stateNames).as("transition origin " + transition)
					.contains((String) transition.get("estado_origen"));
			assertThat(stateNames).as("transition destination " + transition)
					.contains((String) transition.get("estado_destino"));
		}
	}

	@Test
	void evaluationFixturesPreserveWorkbookCoverage() throws Exception {
		List<Map<String, Object>> evaluation = items("/conversation/evaluation-cases.json");
		List<Map<String, Object>> scenarios = items("/conversation/multiturn-scenarios.json");
		List<Map<String, Object>> turns = items("/conversation/multiturn-turns.json");
		Map<String, Object> coverage = resource("/conversation/coverage-recalculated.json");

		assertThat(evaluation).hasSize(104);
		assertThat(scenarios).hasSize(36);
		assertThat(turns).hasSize(118);
		assertThat((Map<?, ?>) coverage.get("coverage_recalculated")).hasSize(42);
		assertThat((List<?>) coverage.get("errors")).isEmpty();
		assertThat((List<?>) coverage.get("warnings")).isEmpty();

		Set<String> splits = values(evaluation, "conjunto");
		assertThat(splits).contains("entrenamiento", "prueba");
		assertThat(splits.stream().anyMatch(value -> value.startsWith("validaci"))).isTrue();
	}

	private Set<String> values(List<Map<String, Object>> rows, String key) {
		Set<String> values = new HashSet<>();
		for (Map<String, Object> row : rows) {
			Object value = row.get(key);
			if (value != null) {
				values.add(value.toString());
			}
		}
		return values;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> items(String resource) throws Exception {
		return (List<Map<String, Object>>) resource(resource).get("items");
	}

	private Map<String, Object> resource(String resource) throws Exception {
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			assertThat(input).as(resource).isNotNull();
			return mapper.readValue(input, new TypeReference<>() {
			});
		}
	}
}
