package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.application.AiKnowledgeRepository.EntityAlias;
import com.asistentewhatsapp.aiagents.application.AiKnowledgeRepository.ResponseRule;
import com.asistentewhatsapp.aiagents.application.AiKnowledgeRepository.ServiceCatalogItem;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository.ConversationContextSnapshot;
import com.asistentewhatsapp.businessai.application.BusinessAiSettingsService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiClientQuestionsAuditTest {

	private static final Path QUESTIONS_FILE = Path.of("C:", "Users", "altp2", "Downloads", "preguntas_clientes.md");
	private static final Path MATRIX_FILE = Path.of("C:", "Users", "altp2", "Downloads",
			"agenda_digital_whatsapp_casuisticas(2).xlsx");
	private static final Path OUTPUT_ROOT = Path.of("..").toAbsolutePath().normalize();
	private static final UUID BUSINESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CHANNEL_ACCOUNT_ID = UUID.fromString("69500000-0000-0000-0000-000000000001");
	private static final UUID CUSTOMER_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");
	private static final String CUSTOMER_PHONE = "56950000000";
	private static final String BUSINESS_NAME = "Centro Estetico Bella";
	private static final String ASSISTANT_NAME = "Asistente del Centro Estético";
	private static final OffsetDateTime CONTROLLED_NOW = OffsetDateTime.of(2026, 7, 27, 12, 4, 0, 0,
			ZoneId.of("America/Santiago").getRules().getOffset(java.time.LocalDateTime.of(2026, 7, 27, 12, 4)));
	private static final Pattern CUSTOMER_QUESTION = Pattern.compile("^(\\d+)\\.\\s+(.+)$");

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void executeAndEvaluateAllClientQuestions() throws Exception {
		assertThat(Files.exists(QUESTIONS_FILE)).as("preguntas_clientes.md").isTrue();
		assertThat(Files.exists(MATRIX_FILE)).as("agenda_digital_whatsapp_casuisticas(2).xlsx").isTrue();

		List<QuestionCase> questions = extractQuestions();
		assertThat(questions).hasSize(460);

		List<ServiceFixture> services = loadJson("/ai-matrix/servicios_v23_4_10.json",
				new TypeReference<List<ServiceFixture>>() {
				});
		List<EntityAliasFixture> aliases = loadJson("/ai-matrix/alias_entidades_v23_4_10.json",
				new TypeReference<List<EntityAliasFixture>>() {
				});

		List<ExecutionRow> rows = new ArrayList<>();
		for (QuestionCase question : questions) {
			rows.add(executeQuestion(question, services, aliases));
			if (rows.size() % 25 == 0) {
				writeProgress(rows);
			}
		}

		List<EvaluationRow> evaluated = rows.stream().map(this::evaluate).toList();
		writeQuestionsResponses(rows);
		writeExecutionJson(rows);
		writeEvaluatorInstruction();
		writeEvaluation(evaluated);
		writeCorrectionPlan(evaluated);

		assertThat(rows).hasSize(460);
		assertThat(rows.stream().map(ExecutionRow::id).distinct().count()).isEqualTo(460);
		assertThat(evaluated).hasSize(460);
	}

	private ExecutionRow executeQuestion(QuestionCase question, List<ServiceFixture> services,
			List<EntityAliasFixture> aliases) {
		long started = System.nanoTime();
		Harness harness = new Harness(services, aliases);
		UUID conversationId = UUID.nameUUIDFromBytes(("TEST-" + question.id()).getBytes(StandardCharsets.UTF_8));
		ConversationContextSnapshot before = null;
		try {
			List<String> setup = setupPrompts(question);
			for (String prompt : setup) {
				harness.route(prompt, conversationId, "SETUP-" + question.id(), true);
			}
			before = harness.contextSnapshot();
			AgentRoutingResult result = harness.route(question.text(), conversationId, question.id(), false);
			long durationMs = (System.nanoTime() - started) / 1_000_000L;
			return ExecutionRow.ok(question, contextDescription(setup), before, harness.contextSnapshot(), result,
					durationMs);
		} catch (Exception ex) {
			long durationMs = (System.nanoTime() - started) / 1_000_000L;
			return ExecutionRow.error(question, contextDescription(setupPrompts(question)), before, durationMs, ex);
		}
	}

	private List<QuestionCase> extractQuestions() throws IOException {
		List<String> lines = Files.readAllLines(QUESTIONS_FILE, StandardCharsets.UTF_8);
		List<QuestionCase> questions = new ArrayList<>();
		String section = "Sin seccion";
		String expected = null;
		int sectionNumber = 0;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith("## Datos") || line.startsWith("## Resultado") || line.startsWith("## Fuente")) {
				sectionNumber = 0;
				section = "Fuera del catalogo de consultas";
				expected = null;
				continue;
			}
			if (line.startsWith("## ")) {
				section = line.substring(3).trim();
				sectionNumber = parseSectionNumber(section);
				expected = null;
				continue;
			}
			if (line.contains("**Intención esperada:**")) {
				expected = line.substring(line.indexOf("**Intención esperada:**") + "**Intención esperada:**".length())
						.replace("`", "").trim();
				continue;
			}
			Matcher matcher = CUSTOMER_QUESTION.matcher(line.trim());
			if (matcher.matches() && sectionNumber > 0 && sectionNumber <= 25) {
				int sourceNumber = Integer.parseInt(matcher.group(1));
				String id = "P" + "%03d".formatted(questions.size() + 1);
				String text = matcher.group(2).trim();
				questions.add(new QuestionCase(id, sourceNumber, i + 1, text, section, sectionNumber,
						expected == null ? "NO_DECLARADA" : expected, classify(sectionNumber, text)));
			}
		}
		return questions;
	}

	private int parseSectionNumber(String section) {
		Matcher matcher = Pattern.compile("^(\\d+)\\.").matcher(section);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
	}

	private Modality classify(int sectionNumber, String text) {
		String normalized = normalize(text);
		if (sectionNumber == 23 || containsAny(normalized, "quemadura", "dolor fuerte", "irritacion", "reclamo",
				"supervisor", "devolucion", "cobro duplicado", "problema grave")) {
			return Modality.CASO_SENSIBLE;
		}
		if (containsAny(normalized, "persona", "ejecutivo", "supervisor", "recepcion", "llame", "contactarme")) {
			return Modality.DERIVACION_HUMANA;
		}
		if (sectionNumber == 12 || containsAny(normalized, "pague", "pago", "abono", "comprobante", "devolucion",
				"reembolso", "cobraron")) {
			return Modality.ACCION_TRANSACCIONAL;
		}
		if (sectionNumber == 25 || containsAny(normalized, "eso", "ella", "misma", "mismo", "ese", "confirmarlo",
				"cancelarlo", "otra opcion", "todavia sirve", "esta listo", "para despues")) {
			return Modality.CONTINUACION_CONVERSACIONAL;
		}
		if (sectionNumber == 5 || sectionNumber == 6 || sectionNumber == 7 || sectionNumber == 14 || sectionNumber == 15
				|| sectionNumber == 17) {
			return Modality.INDEPENDIENTE;
		}
		if (sectionNumber == 1 || sectionNumber == 25) {
			return Modality.CASO_AMBIGUO;
		}
		return Modality.CONSULTA_INFORMATIVA;
	}

	private List<String> setupPrompts(QuestionCase question) {
		if (question.modality() != Modality.CONTINUACION_CONVERSACIONAL) {
			return List.of();
		}
		String normalized = normalize(question.text());
		if (containsAny(normalized, "cancelarlo", "no quiero cancelar")) {
			return List.of("Quiero cancelar mi hora de limpieza facial de mañana a las 15:00 en Providencia");
		}
		if (containsAny(normalized, "otra opcion", "para despues", "a la misma hora", "en la otra sucursal")) {
			return List.of("Quiero reservar limpieza facial en Providencia para mañana a las 15:00");
		}
		return List.of("Quiero reservar limpieza facial en Providencia para mañana a las 15:00");
	}

	private String contextDescription(List<String> setup) {
		return setup == null || setup.isEmpty() ? null : String.join(" | ", setup);
	}

	private EvaluationRow evaluate(ExecutionRow row) {
		if (!"OK".equals(row.resultadoTecnico())) {
			return EvaluationRow.of(row, 0, EvalState.ERROR_TECNICO, "La ejecución produjo una excepción técnica.",
					"Debe generar una respuesta funcional o registrar un error controlado.", "ERROR_PERSISTENCIA",
					"registro_ejecucion_IA.json; clase AgentCoordinatorService o dependencia invocada");
		}
		if (isBlank(row.respuestaExacta())) {
			return EvaluationRow.of(row, 0, EvalState.SIN_RESPUESTA, "El flujo no produjo respuesta.",
					"Debe responder de forma breve indicando el siguiente paso.", "CASO_NO_IMPLEMENTADO",
					"AgentCoordinatorService.route; AgentRegistry.resolve");
		}

		EnumSet<AgentIntent> acceptable = acceptableIntents(row.question());
		AgentIntent actual = row.intencionDetectada();
		boolean intentOk = actual != null && acceptable.contains(actual);
		String response = row.respuestaExacta();
		String normalizedResponse = normalize(response);
		String normalizedQuestion = normalize(row.pregunta());

		int score = 0;
		score += intentOk ? 20 : relatedIntent(actual, acceptable) ? 12 : 4;
		score += pertains(normalizedQuestion, normalizedResponse, actual) ? 20 : 8;
		score += functionalCorrectness(row, normalizedResponse);
		score += continuityScore(row, normalizedResponse);
		score += inventionScore(row, normalizedResponse);
		score += response.length() <= 700 ? 10 : response.length() <= 1100 ? 6 : 2;
		score += safetyScore(row);

		Finding finding = finding(row, intentOk, normalizedResponse, score);
		EvalState state = stateFor(row, score, finding);
		return EvaluationRow.of(row, Math.min(score, 100), state, finding.problem(), expectedBehavior(row),
				finding.cause(), finding.evidence());
	}

	private EnumSet<AgentIntent> acceptableIntents(QuestionCase question) {
		String normalized = normalize(question.text());
		return switch (question.sectionNumber()) {
			case 1 -> containsAny(normalized, "persona", "ejecutivo", "llame", "recepcion")
					? EnumSet.of(AgentIntent.HUMAN_REQUEST)
					: EnumSet.of(AgentIntent.GREETING, AgentIntent.SUPPORT_GENERAL, AgentIntent.AMBIGUOUS,
							AgentIntent.HUMAN_REQUEST);
			case 2 -> EnumSet.of(AgentIntent.SERVICE_INFORMATION, AgentIntent.COMMERCIAL_INQUIRY,
					AgentIntent.SERVICE_RECOMMENDATION);
			case 3 -> EnumSet.of(AgentIntent.SERVICE_RECOMMENDATION, AgentIntent.SERVICE_INFORMATION,
					AgentIntent.HUMAN_REQUEST);
			case 4 -> EnumSet.of(AgentIntent.PRICE_REQUEST, AgentIntent.QUOTE_REQUEST, AgentIntent.PAYMENT_INQUIRY,
					AgentIntent.SERVICE_INFORMATION);
			case 5 -> EnumSet.of(AgentIntent.BOOKING_REQUEST, AgentIntent.COMMERCIAL_AND_BOOKING,
					AgentIntent.AVAILABILITY_QUERY);
			case 6 -> EnumSet.of(AgentIntent.BOOKING_REQUEST, AgentIntent.AVAILABILITY_QUERY,
					AgentIntent.BUSINESS_HOURS_QUERY);
			case 7 -> EnumSet.of(AgentIntent.AVAILABILITY_QUERY, AgentIntent.BOOKING_REQUEST);
			case 8 -> EnumSet.of(AgentIntent.LOCATION_QUERY, AgentIntent.BOOKING_REQUEST,
					AgentIntent.AVAILABILITY_QUERY, AgentIntent.BUSINESS_HOURS_QUERY, AgentIntent.SUPPORT_GENERAL);
			case 9 ->
				EnumSet.of(AgentIntent.PROFESSIONAL_QUERY, AgentIntent.BOOKING_REQUEST, AgentIntent.AVAILABILITY_QUERY);
			case 10 -> EnumSet.of(AgentIntent.AVAILABILITY_QUERY, AgentIntent.BOOKING_REQUEST,
					AgentIntent.SERVICE_INFORMATION, AgentIntent.SUPPORT_GENERAL);
			case 11 -> EnumSet.of(AgentIntent.BOOKING_STATUS, AgentIntent.BOOKING_REQUEST);
			case 12 -> containsAny(normalized, "duplic", "devol", "reembolso", "rechaz", "no corresponde")
					? EnumSet.of(AgentIntent.PAYMENT_PROBLEM, AgentIntent.HUMAN_REQUEST)
					: EnumSet.of(AgentIntent.PAYMENT_INQUIRY, AgentIntent.PAYMENT_PROBLEM, AgentIntent.HUMAN_REQUEST);
			case 13 -> EnumSet.of(AgentIntent.BOOKING_STATUS, AgentIntent.KNOWLEDGE_QUERY, AgentIntent.SUPPORT_GENERAL);
			case 14 -> EnumSet.of(AgentIntent.BOOKING_CHANGE);
			case 15 -> EnumSet.of(AgentIntent.BOOKING_CANCEL);
			case 16 -> EnumSet.of(AgentIntent.FOLLOW_UP, AgentIntent.AVAILABILITY_QUERY, AgentIntent.BOOKING_REQUEST,
					AgentIntent.HUMAN_REQUEST);
			case 17 -> EnumSet.of(AgentIntent.BOOKING_REQUEST, AgentIntent.COMMERCIAL_AND_BOOKING,
					AgentIntent.SERVICE_INFORMATION);
			case 18, 19 -> EnumSet.of(AgentIntent.SERVICE_INFORMATION, AgentIntent.BOOKING_REQUEST,
					AgentIntent.HUMAN_REQUEST, AgentIntent.KNOWLEDGE_QUERY, AgentIntent.SERVICE_RECOMMENDATION);
			case 20 -> EnumSet.of(AgentIntent.BOOKING_STATUS, AgentIntent.HUMAN_REQUEST, AgentIntent.BOOKING_CHANGE,
					AgentIntent.COMPLAINT);
			case 21 -> EnumSet.of(AgentIntent.BUSINESS_HOURS_QUERY, AgentIntent.AVAILABILITY_QUERY);
			case 22 -> EnumSet.of(AgentIntent.BOOKING_STATUS, AgentIntent.HUMAN_REQUEST, AgentIntent.PROFESSIONAL_QUERY,
					AgentIntent.BOOKING_CHANGE);
			case 23 -> EnumSet.of(AgentIntent.COMPLAINT, AgentIntent.HUMAN_REQUEST, AgentIntent.PAYMENT_PROBLEM);
			case 24 -> expectedForShortPhrase(normalized);
			case 25 -> EnumSet.of(AgentIntent.AMBIGUOUS, AgentIntent.BOOKING_REQUEST, AgentIntent.BOOKING_STATUS,
					AgentIntent.BOOKING_CANCEL, AgentIntent.BOOKING_CHANGE, AgentIntent.PRICE_REQUEST);
			default -> EnumSet.allOf(AgentIntent.class);
		};
	}

	private EnumSet<AgentIntent> expectedForShortPhrase(String normalized) {
		if (containsAny(normalized, "cancel", "anular", "no voy", "no puedo ir")) {
			return EnumSet.of(AgentIntent.BOOKING_CANCEL, AgentIntent.AMBIGUOUS);
		}
		if (containsAny(normalized, "cambiar", "mover")) {
			return EnumSet.of(AgentIntent.BOOKING_CHANGE);
		}
		if (containsAny(normalized, "link", "enlace", "pague")) {
			return EnumSet.of(AgentIntent.BOOKING_STATUS, AgentIntent.PAYMENT_INQUIRY, AgentIntent.PAYMENT_PROBLEM);
		}
		if (containsAny(normalized, "donde")) {
			return EnumSet.of(AgentIntent.LOCATION_QUERY, AgentIntent.SUPPORT_GENERAL);
		}
		if (containsAny(normalized, "persona")) {
			return EnumSet.of(AgentIntent.HUMAN_REQUEST);
		}
		return EnumSet.of(AgentIntent.BOOKING_REQUEST, AgentIntent.AVAILABILITY_QUERY, AgentIntent.AMBIGUOUS);
	}

	private boolean relatedIntent(AgentIntent actual, Set<AgentIntent> acceptable) {
		if (actual == null) {
			return false;
		}
		if (acceptable.contains(AgentIntent.BOOKING_REQUEST) && EnumSet
				.of(AgentIntent.AVAILABILITY_QUERY, AgentIntent.COMMERCIAL_AND_BOOKING, AgentIntent.BOOKING_STATUS)
				.contains(actual)) {
			return true;
		}
		if (acceptable.contains(AgentIntent.SERVICE_INFORMATION) && EnumSet
				.of(AgentIntent.COMMERCIAL_INQUIRY, AgentIntent.SERVICE_RECOMMENDATION, AgentIntent.PRICE_REQUEST)
				.contains(actual)) {
			return true;
		}
		return acceptable.contains(AgentIntent.HUMAN_REQUEST) && actual == AgentIntent.COMPLAINT;
	}

	private boolean pertains(String question, String response, AgentIntent intent) {
		if (response.isBlank()) {
			return false;
		}
		if (containsAny(question, "precio", "cuanto", "vale", "cuesta")
				&& !containsAny(response, "$", "valor", "precio", "cotizacion", "servicio exacto")) {
			return false;
		}
		if (containsAny(question, "direccion", "ubic", "donde queda") && !containsAny(response, "sucursal", "direccion",
				"providencia", "condes", "ubicacion", "persona del equipo")) {
			return false;
		}
		return intent != AgentIntent.TECHNICAL_MESSAGE || containsAny(response, "te ayudo", "mensaje parece tecnico");
	}

	private int functionalCorrectness(ExecutionRow row, String response) {
		int score = 20;
		String question = normalize(row.pregunta());
		if (containsAny(question, "disponibilidad", "hora", "cupo") && containsAny(response, "disponible")
				&& row.entidadesDetectadas().get("servicio_o_producto") == null
				&& row.entidadesDetectadas().get("sede") == null) {
			score -= 8;
		}
		if (containsAny(response, "reserva temporal creada", "reserva confirmada")
				&& containsAny(question, "cuanto", "precio", "donde", "que servicios")) {
			score -= 12;
		}
		if (row.question().sectionNumber() == 12 && containsAny(question, "duplic", "devol", "reembolso", "rechaz")
				&& !row.derivacionHumana()) {
			score -= 10;
		}
		return Math.max(0, score);
	}

	private int continuityScore(ExecutionRow row, String response) {
		int score = 15;
		if (row.question().modality() == Modality.CONTINUACION_CONVERSACIONAL
				&& (row.estadoAnterior() == null || row.estadoAnterior().equals("IDLE"))) {
			score -= 8;
		}
		if (countQuestionMarks(response) > 2) {
			score -= 4;
		}
		return Math.max(0, score);
	}

	private int inventionScore(ExecutionRow row, String response) {
		if (containsAny(response, "confirmada correctamente", "pago aprobado", "devolucion realizada")) {
			return 2;
		}
		return 10;
	}

	private int safetyScore(ExecutionRow row) {
		if (row.question().modality() == Modality.CASO_SENSIBLE
				|| row.question().modality() == Modality.DERIVACION_HUMANA) {
			return row.derivacionHumana()
					|| containsAny(normalize(row.respuestaExacta()), "persona del equipo", "deriv") ? 5 : 0;
		}
		return 5;
	}

	private Finding finding(ExecutionRow row, boolean intentOk, String normalizedResponse, int score) {
		if ((row.question().modality() == Modality.CASO_SENSIBLE
				|| row.question().modality() == Modality.DERIVACION_HUMANA) && !row.derivacionHumana()
				&& !containsAny(normalizedResponse, "persona del equipo", "deriv")) {
			return new Finding("No derivó correctamente una situación sensible o solicitud humana.",
					"DERIVACION_HUMANA",
					"IntentDetectorService.detect; HumanHandoffAgent.handle; WhatsAppMessageFormatter.sensitiveCase");
		}
		if (!intentOk) {
			return new Finding("La intención detectada no coincide con la intención esperada para la sección.",
					"DETECCION_INTENCION",
					"IntentDetectorService.detect; ConversationSpecCatalog; AgentRegistry.resolve");
		}
		if (score < 85
				&& containsAny(normalizedResponse, "servicio especifico", "que servicio", "que dia", "sucursal")) {
			return new Finding(
					"Respuesta funcional pero incompleta: solicita datos faltantes o no entrega información concreta.",
					"PLANTILLA_RESPUESTA", "BookingAgent.bookingMissingDataResponse; WhatsAppMessageFormatter");
		}
		if (score < 85) {
			return new Finding("Respuesta parcialmente alineada pero requiere validación funcional más específica.",
					"CASO_NO_IMPLEMENTADO", "Agente seleccionado=" + row.agenteSeleccionado());
		}
		return new Finding("Sin problema crítico detectado.", "SIN_CAUSA", "registro_ejecucion_IA.json");
	}

	private EvalState stateFor(ExecutionRow row, int score, Finding finding) {
		if ("DERIVACION_HUMANA".equals(finding.cause()) || inventionScore(row, normalize(row.respuestaExacta())) < 5) {
			return EvalState.RIESGOSA;
		}
		if (score >= 85) {
			return EvalState.APROBADA;
		}
		if (score >= 65) {
			return EvalState.PARCIALMENTE_CORRECTA;
		}
		return EvalState.INCORRECTA;
	}

	private String expectedBehavior(ExecutionRow row) {
		return switch (row.question().sectionNumber()) {
			case 5 -> "Debe iniciar flujo de reserva y pedir el primer dato faltante, normalmente el servicio.";
			case 6, 7 -> "Debe consultar disponibilidad solo con datos suficientes o pedir el dato faltante principal.";
			case 12 -> "Debe orientar sobre pagos sin confirmar pagos inexistentes y derivar problemas o devoluciones.";
			case 14 -> "Debe identificar la reserva antes de reprogramar y no cambiarla sin confirmación.";
			case 15 -> "Debe identificar la reserva antes de cancelar y pedir confirmación segura.";
			case 23 ->
				"Debe derivar a una persona y no entregar diagnósticos clínicos ni resolver reclamos graves automáticamente.";
			default -> "Debe responder de forma breve, contextual y sin inventar datos no disponibles.";
		};
	}

	private void writeQuestionsResponses(List<ExecutionRow> rows) throws IOException {
		long ok = rows.stream().filter(row -> "OK".equals(row.resultadoTecnico()) && !isBlank(row.respuestaExacta()))
				.count();
		long errors = rows.stream().filter(row -> !"OK".equals(row.resultadoTecnico())).count();
		long noResponse = rows.size() - ok - errors;
		StringBuilder out = new StringBuilder();
		out.append("# Preguntas y respuestas reales de la IA\n\n");
		out.append("- Total esperado: 460\n");
		out.append("- Total ejecutado: ").append(rows.size()).append("\n");
		out.append("- Total con respuesta: ").append(ok).append("\n");
		out.append("- Total sin respuesta: ").append(noResponse).append("\n");
		out.append("- Total con error: ").append(errors).append("\n");
		out.append("- Empresa de prueba: ").append(BUSINESS_NAME).append("\n");
		out.append("- Nombre del asistente: ").append(ASSISTANT_NAME).append("\n");
		out.append("- Fecha controlada: 2026-07-27 12:04\n");
		out.append("- Zona horaria: America/Santiago\n");
		out.append("- Versión evaluada: workspace-local\n\n");
		out.append("| Pregunta cliente | Respuesta IA |\n|---|---|\n");
		for (ExecutionRow row : rows) {
			String response = switch (row.resultadoTecnico()) {
				case "OK" ->
					isBlank(row.respuestaExacta()) ? "SIN RESPUESTA" : ASSISTANT_NAME + ": " + row.respuestaExacta();
				default -> "ERROR TÉCNICO: " + row.error();
			};
			out.append("| ").append(mdCell("[" + row.id() + "] " + row.pregunta())).append(" | ")
					.append(mdCell(response)).append(" |\n");
		}
		Files.writeString(OUTPUT_ROOT.resolve("preguntas_respuesta_IA.md"), out.toString(), StandardCharsets.UTF_8);
	}

	private void writeExecutionJson(List<ExecutionRow> rows) throws IOException {
		List<Map<String, Object>> values = new ArrayList<>();
		for (ExecutionRow row : rows) {
			Map<String, Object> value = new LinkedHashMap<>();
			value.put("id", row.id());
			value.put("pregunta", row.pregunta());
			value.put("modalidad", row.question().modality().name());
			value.put("contextoAplicado", row.contextoAplicado());
			value.put("nombreAsistente", ASSISTANT_NAME);
			value.put("respuestaExacta", row.respuestaExacta());
			value.put("intencionDetectada", row.intencionDetectada() == null ? null : row.intencionDetectada().name());
			value.put("intencionSecundaria",
					row.intencionSecundaria() == null ? null : row.intencionSecundaria().name());
			value.put("confianza", row.confianza());
			value.put("agenteSeleccionado", row.agenteSeleccionado());
			value.put("estadoAnterior", row.estadoAnterior());
			value.put("estadoPosterior", row.estadoPosterior());
			value.put("entidadesDetectadas", row.entidadesDetectadas());
			value.put("datoEsperado", row.datoEsperado());
			value.put("derivacionHumana", row.derivacionHumana());
			value.put("duracionMilisegundos", row.duracionMilisegundos());
			value.put("resultadoTecnico", row.resultadoTecnico());
			value.put("error", row.error());
			values.add(value);
		}
		Files.writeString(OUTPUT_ROOT.resolve("registro_ejecucion_IA.json"),
				mapper.writerWithDefaultPrettyPrinter().writeValueAsString(values), StandardCharsets.UTF_8);
	}

	private void writeEvaluatorInstruction() throws IOException {
		String content = """
				# Instrucción evaluadora independiente de respuestas de IA

				Evalúa las 460 respuestas usando como fuentes: preguntas_clientes.md, preguntas_respuesta_IA.md,
				registro_ejecucion_IA.json, agenda_digital_whatsapp_casuisticas(2).xlsx, código fuente, configuración local y datos de prueba.

				Criterios: intención real, pertinencia, correctitud funcional, continuidad conversacional,
				ausencia de invención, calidad WhatsApp y seguridad/derivación. No uses coincidencia literal como único criterio.

				Revisa evidencia técnica en: WhatsAppInboundMessageService, AiReplyOutboxProcessor, AgentCoordinatorService,
				IntentDetectorService, EntityExtractionService, AgentRegistry, BookingAgent, SalesAgent, PaymentsAgent,
				SupportAgent, HumanHandoffAgent, AiBusinessKnowledgeService, TransactionalAgendaBookingService,
				AiAgentJdbcRepository, AiReplyOutboxJdbcRepository, BookingConfirmationService, application.yml,
				docker-compose.local.yml y migraciones Flyway.

				Clasifica cada respuesta como APROBADA, PARCIALMENTE_CORRECTA, INCORRECTA, RIESGOSA,
				SIN_RESPUESTA, ERROR_TECNICO o NO_EVALUABLE. Puntúa de 0 a 100 con la rúbrica definida.
				No implementes correcciones; solo diagnostica y planifica.
				""";
		Files.writeString(OUTPUT_ROOT.resolve("instruccion_evaluadora_respuestas_IA.md"), content,
				StandardCharsets.UTF_8);
	}

	private void writeEvaluation(List<EvaluationRow> rows) throws IOException {
		Map<EvalState, Long> counts = new LinkedHashMap<>();
		for (EvalState state : EvalState.values()) {
			counts.put(state, rows.stream().filter(row -> row.estado() == state).count());
		}
		StringBuilder out = new StringBuilder("# Evaluación de respuestas de la IA\n\n## Resumen\n\n");
		out.append("| Estado | Cantidad | Porcentaje |\n|---|---:|---:|\n");
		for (EvalState state : EvalState.values()) {
			long count = counts.get(state);
			out.append("| ").append(label(state)).append(" | ").append(count).append(" | ")
					.append(String.format(Locale.ROOT, "%.2f%%", count * 100.0 / rows.size())).append(" |\n");
		}
		out.append("\n## Detalle\n\n");
		out.append(
				"| ID | Pregunta | Respuesta obtenida | Intención esperada | Intención detectada | Puntuación | Estado | Problema encontrado | Respuesta esperada | Evidencia |\n");
		out.append("|---|---|---|---|---|---:|---|---|---|---|\n");
		for (EvaluationRow row : rows) {
			out.append("| ").append(row.execution().id()).append(" | ").append(mdCell(row.execution().pregunta()))
					.append(" | ").append(mdCell(row.execution().respuestaExacta())).append(" | ")
					.append(mdCell(row.execution().question().expectedIntent())).append(" | ")
					.append(row.execution().intencionDetectada()).append(" | ").append(row.puntuacion()).append(" | ")
					.append(row.estado()).append(" | ").append(mdCell(row.problema())).append(" | ")
					.append(mdCell(row.respuestaEsperada())).append(" | ").append(mdCell(row.evidencia()))
					.append(" |\n");
		}
		Files.writeString(OUTPUT_ROOT.resolve("evaluacion_respuestas_IA.md"), out.toString(), StandardCharsets.UTF_8);
	}

	private void writeCorrectionPlan(List<EvaluationRow> rows) throws IOException {
		List<EvaluationRow> findings = rows.stream().filter(row -> row.estado() != EvalState.APROBADA).toList();
		Map<String, List<EvaluationRow>> grouped = new LinkedHashMap<>();
		for (EvaluationRow row : findings) {
			grouped.computeIfAbsent(row.causaTecnica(), ignored -> new ArrayList<>()).add(row);
		}
		StringBuilder out = new StringBuilder("# Plan de correcciones de respuestas de IA\n\n## Resumen ejecutivo\n\n");
		out.append("- Total de preguntas: 460\n");
		out.append("- Aprobadas: ").append(count(rows, EvalState.APROBADA)).append("\n");
		out.append("- Parciales: ").append(count(rows, EvalState.PARCIALMENTE_CORRECTA)).append("\n");
		out.append("- Incorrectas: ").append(count(rows, EvalState.INCORRECTA)).append("\n");
		out.append("- Riesgosas: ").append(count(rows, EvalState.RIESGOSA)).append("\n");
		out.append("- Principales causas: ").append(String.join(", ", grouped.keySet())).append("\n");
		out.append("- Cobertura actual: ").append(count(rows, EvalState.APROBADA)).append("/460 aprobadas\n");
		out.append("- Cobertura objetivo: 460/460 aprobadas sin riesgos críticos\n\n");
		out.append("## Correcciones priorizadas\n\n");
		out.append(
				"| ID | Prioridad | Problema raíz | Preguntas afectadas | Evidencia | Componentes afectados | Corrección propuesta | Pruebas requeridas | Riesgo | Esfuerzo | Dependencias | Criterio de aceptación |\n");
		out.append("|---|---|---|---|---|---|---|---|---|---|---|---|\n");
		int index = 1;
		for (Map.Entry<String, List<EvaluationRow>> entry : grouped.entrySet().stream()
				.sorted(Comparator.comparingInt((Map.Entry<String, List<EvaluationRow>> e) -> priorityRank(e.getKey())))
				.toList()) {
			List<EvaluationRow> affected = entry.getValue();
			out.append("| C").append("%02d".formatted(index++)).append(" | ").append(priority(entry.getKey(), affected))
					.append(" | ").append(mdCell(rootProblem(entry.getKey()))).append(" | ")
					.append(mdCell(affected.stream().map(row -> row.execution().id()).limit(80).toList().toString()))
					.append(" | ").append(mdCell(affected.getFirst().evidencia())).append(" | ")
					.append(mdCell(components(entry.getKey()))).append(" | ")
					.append(mdCell(proposedFix(entry.getKey()))).append(" | ")
					.append(mdCell(requiredTests(entry.getKey()))).append(" | ").append(mdCell(risk(entry.getKey())))
					.append(" | ").append(effort(entry.getKey())).append(" | ")
					.append(mdCell(dependencies(entry.getKey()))).append(" | ")
					.append(mdCell(
							"Las preguntas afectadas obtienen >=85 puntos, no inventan datos y no ejecutan acciones externas."))
					.append(" |\n");
		}
		Files.writeString(OUTPUT_ROOT.resolve("plan_correcciones_IA.md"), out.toString(), StandardCharsets.UTF_8);
	}

	private long count(List<EvaluationRow> rows, EvalState state) {
		return rows.stream().filter(row -> row.estado() == state).count();
	}

	private int priorityRank(String cause) {
		return switch (cause) {
			case "DERIVACION_HUMANA" -> 0;
			case "DETECCION_INTENCION" -> 1;
			case "CASO_NO_IMPLEMENTADO" -> 2;
			case "PLANTILLA_RESPUESTA" -> 3;
			default -> 4;
		};
	}

	private String priority(String cause, List<EvaluationRow> affected) {
		if ("DERIVACION_HUMANA".equals(cause)
				|| affected.stream().anyMatch(row -> row.estado() == EvalState.RIESGOSA)) {
			return "P0";
		}
		if ("DETECCION_INTENCION".equals(cause)) {
			return "P1";
		}
		if ("CASO_NO_IMPLEMENTADO".equals(cause)) {
			return "P2";
		}
		return "P3";
	}

	private String rootProblem(String cause) {
		return switch (cause) {
			case "DERIVACION_HUMANA" ->
				"Casos sensibles o solicitud de persona no siempre terminan en derivación explícita.";
			case "DETECCION_INTENCION" -> "El detector clasifica algunas frases en una intención distinta a la matriz.";
			case "PLANTILLA_RESPUESTA" -> "Plantillas genéricas o incompletas para datos faltantes.";
			case "CASO_NO_IMPLEMENTADO" -> "Caso conversacional cubierto parcialmente o sin regla específica.";
			default -> cause;
		};
	}

	private String components(String cause) {
		return switch (cause) {
			case "DERIVACION_HUMANA" -> "IntentDetectorService, HumanHandoffAgent, WhatsAppMessageFormatter";
			case "DETECCION_INTENCION" ->
				"IntentDetectorService, ConversationSpecCatalog, EntityExtractionService, AgentRegistry";
			case "PLANTILLA_RESPUESTA" ->
				"BookingAgent, SalesAgent, AiBusinessKnowledgeService, WhatsAppMessageFormatter";
			default -> "AgentCoordinatorService, agentes especializados, reglas de conocimiento";
		};
	}

	private String proposedFix(String cause) {
		return switch (cause) {
			case "DERIVACION_HUMANA" ->
				"Agregar o ajustar patrones de riesgo y pruebas de derivación para reclamos, reacciones adversas y problemas graves de pago.";
			case "DETECCION_INTENCION" ->
				"Ampliar cobertura de expresiones del catálogo y reglas de prioridad entre reserva, disponibilidad, precio, cancelación y reprogramación.";
			case "PLANTILLA_RESPUESTA" ->
				"Hacer plantillas dinámicas por dato faltante, con una pregunta principal por turno y alternativas configurables.";
			default ->
				"Definir regla específica del caso y conectar al agente existente sin crear integraciones externas nuevas.";
		};
	}

	private String requiredTests(String cause) {
		String base = "JUnit: detector de intenciones, negaciones, frases ambiguas, extracción de fechas/horas, contexto, sucursales, servicios activos/inactivos, profesionales, disponibilidad, reserva temporal, confirmación, cancelación, reprogramación, pagos, derivación humana, aislamiento multiempresa/clientes, respuestas configurables, caracteres especiales y errores del proveedor IA.";
		return base + " Causa foco: " + cause;
	}

	private String risk(String cause) {
		return "DERIVACION_HUMANA".equals(cause)
				? "Alto si no se deriva un caso sensible."
				: "Medio por regresión conversacional.";
	}

	private String effort(String cause) {
		return switch (cause) {
			case "DETECCION_INTENCION" -> "mediano";
			case "DERIVACION_HUMANA" -> "pequeño";
			case "PLANTILLA_RESPUESTA" -> "pequeño";
			default -> "mediano";
		};
	}

	private String dependencies(String cause) {
		return switch (cause) {
			case "DETECCION_INTENCION" -> "Catálogo de expresiones y matriz de casuísticas.";
			case "PLANTILLA_RESPUESTA" -> "Catálogo y reglas configurables de respuesta.";
			default -> "Datos de prueba y políticas del MVP.";
		};
	}

	private String label(EvalState state) {
		return switch (state) {
			case APROBADA -> "Aprobada";
			case PARCIALMENTE_CORRECTA -> "Parcialmente correcta";
			case INCORRECTA -> "Incorrecta";
			case RIESGOSA -> "Riesgosa";
			case SIN_RESPUESTA -> "Sin respuesta";
			case ERROR_TECNICO -> "Error técnico";
			case NO_EVALUABLE -> "No evaluable";
		};
	}

	private void writeProgress(List<ExecutionRow> rows) throws IOException {
		Map<String, Object> progress = new LinkedHashMap<>();
		progress.put("ultimoIdCompletado", rows.getLast().id());
		progress.put("totalCompletado", rows.size());
		progress.put("fechaControlada", CONTROLLED_NOW.toString());
		Files.writeString(OUTPUT_ROOT.resolve("registro_ejecucion_IA.progreso.json"),
				mapper.writerWithDefaultPrettyPrinter().writeValueAsString(progress), StandardCharsets.UTF_8);
	}

	private <T> T loadJson(String resource, TypeReference<T> type) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			assertThat(input).as("resource " + resource).isNotNull();
			return mapper.readValue(input, type);
		}
	}

	private static boolean containsAny(String value, String... parts) {
		String safe = value == null ? "" : value;
		for (String part : parts) {
			if (safe.contains(part)) {
				return true;
			}
		}
		return false;
	}

	private static String normalize(String value) {
		return TextNormalizer.normalize(value == null ? "" : value);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private int countQuestionMarks(String value) {
		int count = 0;
		for (int i = 0; value != null && i < value.length(); i++) {
			if (value.charAt(i) == '?') {
				count++;
			}
		}
		return count;
	}

	private String mdCell(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("|", "\\|").replace("\r", "").replace("\n", "<br>");
	}

	private enum Modality {
		INDEPENDIENTE, CONTINUACION_CONVERSACIONAL, ACCION_TRANSACCIONAL, CONSULTA_INFORMATIVA, CASO_AMBIGUO, CASO_SENSIBLE, DERIVACION_HUMANA
	}

	private enum EvalState {
		APROBADA, PARCIALMENTE_CORRECTA, INCORRECTA, RIESGOSA, SIN_RESPUESTA, ERROR_TECNICO, NO_EVALUABLE
	}

	private record QuestionCase(String id, int sourceNumber, int lineNumber, String text, String section,
			int sectionNumber, String expectedIntent, Modality modality) {
	}

	private record Finding(String problem, String cause, String evidence) {
	}

	private record EvaluationRow(ExecutionRow execution, int puntuacion, EvalState estado, String problema,
			String respuestaEsperada, String causaTecnica, String evidencia) {
		static EvaluationRow of(ExecutionRow execution, int puntuacion, EvalState estado, String problema,
				String respuestaEsperada, String causaTecnica, String evidencia) {
			return new EvaluationRow(execution, puntuacion, estado, problema, respuestaEsperada, causaTecnica,
					evidencia);
		}
	}

	private record ExecutionRow(QuestionCase question, String contextoAplicado, ConversationContextSnapshot before,
			ConversationContextSnapshot after, AgentRoutingResult result, long duracionMilisegundos,
			String resultadoTecnico, String error) {
		static ExecutionRow ok(QuestionCase question, String contextoAplicado, ConversationContextSnapshot before,
				ConversationContextSnapshot after, AgentRoutingResult result, long duracionMilisegundos) {
			return new ExecutionRow(question, contextoAplicado, before, after, result, duracionMilisegundos, "OK",
					null);
		}

		static ExecutionRow error(QuestionCase question, String contextoAplicado, ConversationContextSnapshot before,
				long duracionMilisegundos, Exception ex) {
			return new ExecutionRow(question, contextoAplicado, before, null, null, duracionMilisegundos, "ERROR",
					ex.getClass().getSimpleName() + ": " + ex.getMessage());
		}

		String id() {
			return question.id();
		}

		String pregunta() {
			return question.text();
		}

		String respuestaExacta() {
			return result == null ? null : result.responseToCustomer();
		}

		AgentIntent intencionDetectada() {
			return result == null ? null : result.primaryIntent();
		}

		AgentIntent intencionSecundaria() {
			return result == null ? null : result.secondaryIntent();
		}

		double confianza() {
			return result == null ? 0.0d : result.confidence();
		}

		String agenteSeleccionado() {
			return result == null ? null : result.agentType().name();
		}

		String estadoAnterior() {
			return state(before);
		}

		String estadoPosterior() {
			return state(after);
		}

		Map<String, String> entidadesDetectadas() {
			return result == null ? Map.of() : result.extractedData();
		}

		String datoEsperado() {
			if (result == null || result.missingData() == null || result.missingData().isEmpty()) {
				return null;
			}
			return String.join(",", result.missingData());
		}

		boolean derivacionHumana() {
			return result != null && result.requiresHuman();
		}

		private static String state(ConversationContextSnapshot snapshot) {
			if (snapshot == null) {
				return "IDLE";
			}
			List<String> missing = snapshot.missingData() == null ? List.of() : snapshot.missingData();
			if (missing.contains("motivo_o_servicio") || missing.contains("servicio_o_producto")) {
				return "WAITING_SERVICE";
			}
			if (missing.contains("sucursal")) {
				return "WAITING_LOCATION";
			}
			if (missing.contains("fecha_deseada")) {
				return "WAITING_DATE";
			}
			if (missing.contains("horario_preferido")) {
				return "WAITING_TIME";
			}
			if (!missing.isEmpty()) {
				return "WAITING_" + missing.getFirst().toUpperCase(Locale.ROOT);
			}
			return snapshot.primaryIntent() == null ? "IDLE" : snapshot.primaryIntent().name();
		}
	}

	private record ServiceFixture(String code, String name, int durationMinutes, String priceText,
			String requiresEvaluation, String requiresConsent, String source) {
	}

	private record EntityAliasFixture(String alias, String entityKey, String entityValue, int priority, String source) {
	}

	private static class Harness {
		private final List<ServiceFixture> services;
		private final List<EntityAliasFixture> aliases;
		private final BusinessLocationRecord providencia = new BusinessLocationRecord(
				UUID.fromString("81000000-0000-0000-0000-000000000001"), BUSINESS_ID, "providencia", "Providencia",
				"Av. Providencia 1234", "Santiago", "Providencia", "+56911111111", "+56911111111", "America/Santiago",
				null, null, null, true, CONTROLLED_NOW.withOffsetSameInstant(ZoneOffset.UTC),
				CONTROLLED_NOW.withOffsetSameInstant(ZoneOffset.UTC));
		private final BusinessLocationRecord lasCondes = new BusinessLocationRecord(
				UUID.fromString("81000000-0000-0000-0000-000000000002"), BUSINESS_ID, "las-condes", "Las Condes",
				"Av. Apoquindo 4567", "Santiago", "Las Condes", "+56922222222", "+56922222222", "America/Santiago",
				null, null, null, true, CONTROLLED_NOW.withOffsetSameInstant(ZoneOffset.UTC),
				CONTROLLED_NOW.withOffsetSameInstant(ZoneOffset.UTC));
		private final InMemoryAiAgentRepository contextRepository = new InMemoryAiAgentRepository();
		private final BusinessAiSettingsService businessAiSettingsService = Mockito.mock();
		private final AgentCoordinatorService coordinator;

		Harness(List<ServiceFixture> services, List<EntityAliasFixture> aliases) {
			this.services = services;
			this.aliases = aliases;
			AiBusinessKnowledgeService knowledge = new AiBusinessKnowledgeService(
					new AuditKnowledgeRepository(services, aliases));
			BusinessLocationJdbcRepository locations = locationRepository();
			TransactionalAgendaBookingService transactional = transactionalAgendaBookingService();
			IntentDetectorService detector = new IntentDetectorService();
			EntityExtractionService extractor = new EntityExtractionService(knowledge, locations);
			AgentRegistry registry = new AgentRegistry(List.of(new ReceptionAgent(), new SalesAgent(knowledge),
					new BookingAgent(knowledge, transactional, Mockito.mock(
							com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.class),
							Mockito.mock(com.asistentewhatsapp.bookings.application.BookingConfirmationService.class),
							Mockito.mock(
									com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.class)),
					new PaymentsAgent(knowledge), new SupportAgent(locations), new KnowledgeAgent(),
					new FollowUpAgent(), new HumanHandoffAgent()));
			Mockito.when(businessAiSettingsService.findSettingsOpt(Mockito.any())).thenReturn(Optional.empty());
			this.coordinator = new AgentCoordinatorService(enabledProperties(), detector, extractor, registry,
					contextRepository, businessAiSettingsService);
		}

		AgentRoutingResult route(String body, UUID conversationId, String traceSuffix, boolean dryRun) {
			return coordinator.route(new AgentConversationRequest(BUSINESS_ID, CHANNEL_ACCOUNT_ID, conversationId,
					CUSTOMER_ID, CUSTOMER_PHONE, "Cliente Auditoria", body, CONTROLLED_NOW, null, null,
					"AUDIT-" + traceSuffix, dryRun)).orElseThrow();
		}

		ConversationContextSnapshot contextSnapshot() {
			return contextRepository.snapshot().orElse(null);
		}

		private BusinessLocationJdbcRepository locationRepository() {
			BusinessLocationJdbcRepository repository = Mockito.mock(BusinessLocationJdbcRepository.class);
			Mockito.when(repository.findActive(Mockito.any())).thenReturn(List.of(providencia, lasCondes));
			Mockito.when(repository.countActive(Mockito.any())).thenReturn(2L);
			return repository;
		}

		private TransactionalAgendaBookingService transactionalAgendaBookingService() {
			TransactionalAgendaBookingService service = Mockito.mock(TransactionalAgendaBookingService.class);
			Mockito.when(service.resolveEffectiveLocation(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
						String combined = normalize(String.join(" ", safe(invocation.getArgument(1, String.class)),
								safe(invocation.getArgument(2, String.class)),
								safe(invocation.getArgument(4, String.class))));
						if (combined.contains("providencia")) {
							return new TransactionalAgendaBookingService.ResolvedLocation(providencia, "MESSAGE_TEXT");
						}
						if (combined.contains("condes")) {
							return new TransactionalAgendaBookingService.ResolvedLocation(lasCondes, "MESSAGE_TEXT");
						}
						return new TransactionalAgendaBookingService.ResolvedLocation(null, "MISSING");
					});
			Mockito.when(service.generateBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new TransactionalAgendaBookingService.BookingLinkResult(
							"https://audit.local/reservas/confirmar/TOKEN-DE-PRUEBA", true));
			Mockito.when(service.checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
					.thenAnswer(invocation -> {
						String serviceName = invocation.getArgument(2, String.class);
						String location = invocation.getArgument(3, String.class);
						String date = invocation.getArgument(4, String.class);
						String time = invocation.getArgument(5, String.class);
						String slot = invocation.getArgument(6, String.class);
						if (isBlank(serviceName) || isBlank(location) || isBlank(date)) {
							return Optional.empty();
						}
						String requested = isBlank(time) ? (isBlank(slot) ? "10:00" : slot) : time;
						return Optional.of("Puedo revisar disponibilidad real para " + serviceName + " en " + location
								+ " el " + date + ". Opciones disponibles:\n\n1. " + date + " a las " + requested
								+ "\n2. " + date + " a las 16:00\n\n¿Cuál prefieres?");
					});
			Mockito.when(service.createTemporaryBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
					.thenAnswer(invocation -> {
						String serviceName = invocation.getArgument(6, String.class);
						String location = invocation.getArgument(7, String.class);
						String date = invocation.getArgument(8, String.class);
						String time = invocation.getArgument(9, String.class);
						Boolean dryRun = invocation.getArgument(11, Boolean.class);
						if (isBlank(serviceName) || isBlank(location) || isBlank(date) || isBlank(time)) {
							return Optional.empty();
						}
						if (Boolean.TRUE.equals(dryRun)) {
							return Optional
									.of(WhatsAppMessageFormatter.bookingPreview(serviceName, location, date, time));
						}
						return Optional.of(WhatsAppMessageFormatter.temporaryBookingCreated(serviceName, location, date,
								time, "https://audit.local/reservas/confirmar/TOKEN-DE-PRUEBA", 720));
					});
			Mockito.when(service.handleCancelBookingFromWhatsApp(Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(WhatsAppMessageFormatter.cancellationLinkGenerated("Limpieza facial profunda", "mañana",
							"15:00", "Providencia", "https://audit.local/reservas/cancelar/TOKEN-DE-PRUEBA", 720));
			Mockito.when(service.handleRescheduleBookingFromWhatsApp(Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(WhatsAppMessageFormatter.rescheduleRequest());
			return service;
		}

		private static String safe(String value) {
			return value == null ? "" : value;
		}

		private AiAgentProperties enabledProperties() {
			AiAgentProperties properties = new AiAgentProperties();
			properties.setEnabled(true);
			properties.setAuditEnabled(true);
			properties.setAutoReplyEnabled(false);
			return properties;
		}
	}

	private static class InMemoryAiAgentRepository extends AiAgentJdbcRepository {
		private AgentRoutingResult lastResult;

		InMemoryAiAgentRepository() {
			super(null, new ObjectMapper());
		}

		Optional<ConversationContextSnapshot> snapshot() {
			return findConversationContext(BUSINESS_ID, UUID.randomUUID());
		}

		@Override
		public Optional<ConversationContextSnapshot> findConversationContext(UUID businessId, UUID conversationId) {
			if (lastResult == null) {
				return Optional.empty();
			}
			return Optional.of(new ConversationContextSnapshot(lastResult.agentType(), lastResult.primaryIntent(),
					lastResult.secondaryIntent(), lastResult.extractedData(), lastResult.missingData()));
		}

		@Override
		public void upsertConversationContext(AgentRoutingResult result) {
			lastResult = result;
		}

		@Override
		public void insertDecisionLog(AgentRoutingResult result) {
		}

		@Override
		public void incrementMetric(AgentRoutingResult result) {
		}

		@Override
		public void insertHumanHandoff(AgentRoutingResult result) {
		}
	}

	private static class AuditKnowledgeRepository implements AiKnowledgeRepository {
		private final List<ServiceFixture> services;
		private final List<EntityAliasFixture> aliases;

		AuditKnowledgeRepository(List<ServiceFixture> services, List<EntityAliasFixture> aliases) {
			this.services = services;
			this.aliases = aliases;
		}

		@Override
		public List<ServiceCatalogItem> findActiveServices(UUID businessId) {
			return services
					.stream().map(service -> new ServiceCatalogItem(service.code(), service.name(),
							categoryCode(service.code()), service.durationMinutes(), parsePrice(service.priceText())))
					.toList();
		}

		@Override
		public Optional<ResponseRule> findActiveRule(UUID businessId, String code) {
			return switch (code) {
				case "AI_DEPILATION_CATALOG_RESPONSE" ->
					Optional.of(rule(code, "Tenemos {services}. ¿Cuál quieres revisar?",
							Map.of("labels", List.of("depilación bozo", "rostro", "axilas", "cera", "láser"))));
				case "AI_PRICE_KNOWN_SERVICE_RESPONSE" -> Optional.of(rule(code,
						"El valor base de {service} es {price} y dura aproximadamente {duration} minutos. ¿Quieres agendar una hora?",
						Map.of()));
				case "AI_PRICE_UNKNOWN_SERVICE_RESPONSE" -> Optional.of(
						rule(code, "Para darte un precio correcto, ¿me indicas el servicio exacto que quieres revisar?",
								Map.of()));
				case "AI_SALES_MISSING_SERVICE_RESPONSE" -> Optional.of(rule(code,
						"Perfecto, puedo ayudarte. ¿Qué producto o servicio estás buscando exactamente?", Map.of()));
				case "AI_SALES_NEXT_STEP_RESPONSE" -> Optional.of(rule(code,
						"Puedo orientarte con {service}. ¿Quieres revisar precio, características o agendar una atención?",
						Map.of()));
				case "AI_BOOKING_STATUS_IDENTIFY_RESPONSE" -> Optional.of(rule(code,
						"Puedo revisar tu reserva. Para identificarla, indícame tu nombre, teléfono o la fecha de atención.",
						Map.of()));
				case "AI_QUOTE_MISSING_DETAIL_RESPONSE" -> Optional.of(rule(code,
						"Puedo ayudarte con la cotización de {category}. ¿Qué zona quieres cotizar: {options}?",
						Map.of("options", List.of("bozo", "rostro", "axilas", "piernas", "bikini"))));
				case "AI_PAYMENT_REQUEST_AMOUNT_RESPONSE" -> Optional.of(rule(code,
						"Recibí tu referencia {requestNumber} por {amount}. Puedo orientarte con el estado del pago.",
						Map.of()));
				default -> Optional.empty();
			};
		}

		@Override
		public List<EntityAlias> findActiveEntityAliases(UUID businessId) {
			List<EntityAlias> loaded = new ArrayList<>(aliases.stream().map(
					alias -> new EntityAlias(alias.alias(), alias.entityKey(), alias.entityValue(), alias.priority()))
					.toList());
			for (ServiceFixture service : services) {
				loaded.add(new EntityAlias(service.name(), "servicio_o_producto", service.name(), 200));
			}
			loaded.add(new EntityAlias("manana", "fecha_relativa", "mañana", 100));
			loaded.add(new EntityAlias("mañana", "fecha_relativa", "mañana", 100));
			loaded.add(new EntityAlias("pasado mañana", "fecha_relativa", "pasado mañana", 100));
			loaded.add(new EntityAlias("providencia", "sede", "Providencia", 100));
			loaded.add(new EntityAlias("las condes", "sede", "Las Condes", 100));
			return loaded;
		}

		private ResponseRule rule(String code, String template, Map<String, Object> payload) {
			return new ResponseRule(code, template, payload);
		}

		private static String categoryCode(String code) {
			if (code == null) {
				return "GENERAL";
			}
			String normalized = code.toUpperCase(Locale.ROOT);
			if (normalized.startsWith("DEP"))
				return "DEPILACION";
			if (normalized.startsWith("FAC"))
				return "FACIAL";
			if (normalized.startsWith("COR") || normalized.startsWith("MAS") || normalized.startsWith("DRE")
					|| normalized.startsWith("CAV") || normalized.startsWith("PRE"))
				return "CORPORAL";
			if (normalized.startsWith("CEJ"))
				return "CEJAS";
			return "GENERAL";
		}

		private static BigDecimal parsePrice(String value) {
			if (value == null || value.isBlank()) {
				return BigDecimal.ZERO;
			}
			String digits = value.replaceAll("[^0-9]", "");
			return digits.isBlank() ? BigDecimal.ZERO : new BigDecimal(digits);
		}
	}
}
