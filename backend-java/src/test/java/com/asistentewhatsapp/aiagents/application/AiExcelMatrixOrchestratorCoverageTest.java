
package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.businessai.api.BusinessAiSettingsResponse;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

class AiExcelMatrixOrchestratorCoverageTest {

	private static final boolean STRICT_MATRIX_MODE = Boolean.getBoolean("ai.matrix.strict");
	private static final UUID BUSINESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CHANNEL_ACCOUNT_ID = UUID.fromString("69500000-0000-0000-0000-000000000001");
	private static final UUID CUSTOMER_ID = UUID.fromString("e60d76fe-7169-4f0b-b259-0d290b07e89c");
	private static final String CUSTOMER_PHONE = "224145803620505";
	private static final List<AuditRow> AUDIT_ROWS = new CopyOnWriteArrayList<>();

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void excelMatrixResourcesAreLoadedWithExpectedCoverage() throws Exception {
		List<MatrixCase> matrix = loadMatrixCases();
		List<ServiceFixture> services = loadServices();
		List<EntityAliasFixture> aliases = loadAliases();

		assertThat(matrix).hasSize(119);
		assertThat(services).hasSizeGreaterThanOrEqualTo(56);
		assertThat(aliases).hasSizeGreaterThanOrEqualTo(19);
		assertThat(matrix).extracting(MatrixCase::expectedIntent).contains("BOOKING_REQUEST", "PRICE_REQUEST",
				"COMMERCIAL_AND_BOOKING");
		assertThat(matrix).extracting(MatrixCase::expectedAgent).contains("BOOKING", "SALES", "RECEPTION",
				"HUMAN_HANDOFF");
	}

	@TestFactory
	Stream<DynamicTest> allExcelQuestionsReturnAuditableAiResponses() throws Exception {
		return loadMatrixCases().stream()
				.map(item -> DynamicTest.dynamicTest(item.id() + " - " + item.customerQuestion(), () -> {
					Harness harness = new Harness(loadServices(), loadAliases());
					UUID conversationId = UUID.nameUUIDFromBytes(item.id().getBytes(StandardCharsets.UTF_8));
					AgentRoutingResult result = isPreviewCase(item)
							? harness.preview(item.customerQuestion(), conversationId)
									.orElseGet(() -> harness.route(item.customerQuestion(), conversationId))
							: harness.route(item.customerQuestion(), conversationId);
					AuditRow audit = AuditRow.from(item, result);
					AUDIT_ROWS.add(audit);

					assertThat(result.responseToCustomer()).as(item.id()).isNotBlank();

					if (STRICT_MATRIX_MODE) {
						assertThat(result.primaryIntent().name()).as(item.id() + " expected intent")
								.isEqualTo(item.expectedIntent());
						assertThat(result.agentType().name()).as(item.id() + " expected agent")
								.isEqualTo(item.expectedAgent());
						if (expectsLink(item)) {
							assertThat(result.responseToCustomer()).as(item.id() + " expected confirmation link")
									.contains("/reservas/confirmar/");
						}
						assertExpectedMissingData(item, result);
					}
				}));
	}

	@Test
	void bookingMessageWithServiceDateTimeAndLocationRoutesToBookingEvenWithoutWordAgendar() throws Exception {
		Harness harness = new Harness(loadServices(), loadAliases());

		AgentRoutingResult result = harness.route(
				"Hola, quiero una limpieza facial para el viernes 12 de junio 2026 a las 16:00 en Providencia.",
				UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"));

		assertThat(result.agentType()).isEqualTo(AgentType.BOOKING);
		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.COMMERCIAL_AND_BOOKING);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Limpieza facial profunda");
		assertThat(result.extractedData()).containsEntry("hora", "16:00");
		assertThat(result.missingData()).doesNotContain("motivo_o_servicio", "sede", "fecha_deseada",
				"horario_preferido");
		assertThat(result.responseToCustomer()).contains("/reservas/confirmar/");
	}

	@Test
	void persistedContextAllowsFragmentedClientAnswersToContinueBookingFlow() throws Exception {
		Harness harness = new Harness(loadServices(), loadAliases());
		UUID conversationId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

		AgentRoutingResult first = harness.routePersisted("Quiero agendar manana a las 16:00 horas", conversationId)
				.orElseThrow();
		assertThat(first.agentType()).isEqualTo(AgentType.BOOKING);
		assertThat(first.missingData()).contains("motivo_o_servicio", "sucursal");
		assertThat(first.responseToCustomer()).doesNotContain("/reservar");

		AgentRoutingResult second = harness.preview("depilacion bozo", conversationId).orElseThrow();
		assertThat(second.agentType()).isEqualTo(AgentType.BOOKING);
		assertThat(second.extractedData()).containsEntry("servicio_o_producto", "Depilacion bozo");
		assertThat(second.missingData()).doesNotContain("motivo_o_servicio");
	}

	@TestFactory
	Stream<DynamicTest> allCatalogServicesCanAnswerPriceQuestions() throws Exception {
		List<ServiceFixture> services = loadServices();
		List<EntityAliasFixture> aliases = loadAliases();
		return services.stream()
				.map(service -> DynamicTest.dynamicTest("PRICE-" + service.code() + " - " + service.name(), () -> {
					Harness harness = new Harness(services, aliases);
					AgentRoutingResult result = harness.route("Cuanto cuesta " + service.name(),
							UUID.nameUUIDFromBytes(("PRICE-" + service.code()).getBytes(StandardCharsets.UTF_8)));

					assertThat(result.agentType()).as(service.name()).isEqualTo(AgentType.SALES);
					assertThat(result.primaryIntent()).as(service.name()).isEqualTo(AgentIntent.PRICE_REQUEST);
					assertThat(result.responseToCustomer()).as(service.name()).isNotBlank();
					if (STRICT_MATRIX_MODE) {
						assertThat(containsSignificantServiceToken(result.responseToCustomer(), service.name()))
								.as(service.name() + " response should reference a significant service token").isTrue();
					}
				}));
	}

	@TestFactory
	Stream<DynamicTest> allCatalogServicesCanEnterBookingFlowWithCompleteData() throws Exception {
		List<ServiceFixture> services = loadServices();
		List<EntityAliasFixture> aliases = loadAliases();
		return services.stream()
				.map(service -> DynamicTest.dynamicTest("BOOKING-" + service.code() + " - " + service.name(), () -> {
					Harness harness = new Harness(services, aliases);
					AgentRoutingResult result = harness.route(
							"Quiero agendar " + service.name() + " mañana a las 10:00 en Providencia",
							UUID.nameUUIDFromBytes(("BOOKING-" + service.code()).getBytes(StandardCharsets.UTF_8)));

					assertThat(result.agentType()).as(service.name()).isEqualTo(AgentType.BOOKING);
					assertThat(result.missingData()).as(service.name()).doesNotContain("motivo_o_servicio", "sede",
							"fecha_deseada", "horario_preferido");
					assertThat(result.responseToCustomer()).as(service.name()).isNotBlank();
					if (STRICT_MATRIX_MODE) {
						assertThat(result.responseToCustomer()).as(service.name()).contains("/reservas/confirmar/");
					}
				}));
	}

	@AfterAll
	static void writeAuditReport() throws IOException {
		if (AUDIT_ROWS.isEmpty()) {
			return;
		}
		Path reports = Path.of("target", "ai-matrix");
		Files.createDirectories(reports);
		Path report = reports.resolve("reporte_matriz_excel_ia_v23_4_22.md");
		StringBuilder builder = new StringBuilder();
		long mismatches = AUDIT_ROWS.stream().filter(row -> !row.intentOk() || !row.agentOk()).count();
		builder.append("# Reporte matriz Excel IA v23.4.22\n\n");
		builder.append("Modo estricto: ").append(STRICT_MATRIX_MODE).append("\n\n");
		builder.append("Casos auditados: ").append(AUDIT_ROWS.size()).append("\n\n");
		builder.append("Diferencias de intención/agente: ").append(mismatches).append("\n\n");
		builder.append(
				"| ID | Pregunta | Intención esperada | Intención real | Agente esperado | Agente real | Enlace | Respuesta IA |\n");
		builder.append("|---|---|---|---|---|---|---|---|\n");
		for (AuditRow row : AUDIT_ROWS) {
			builder.append("| ").append(escape(row.id())).append(" | ").append(escape(row.question())).append(" | ")
					.append(escape(row.expectedIntent())).append(" | ").append(row.intentOk() ? "OK " : "FAIL ")
					.append(escape(row.actualIntent())).append(" | ").append(escape(row.expectedAgent())).append(" | ")
					.append(row.agentOk() ? "OK " : "FAIL ").append(escape(row.actualAgent())).append(" | ")
					.append(row.containsLink() ? "Sí" : "No").append(" | ")
					.append(escape(truncate(row.response(), 180))).append(" |\n");
		}
		Files.writeString(report, sanitizeForUtf8(builder.toString()), StandardCharsets.UTF_8);
	}

	private static String truncate(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		String clean = value.replace("\n", " ").replace("\r", " ").trim();
		return clean.length() <= maxLength ? clean : clean.substring(0, maxLength - 3) + "...";
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("|", "\\|").replace("\n", " ").replace("\r", " ").trim();
	}

	private static boolean isPreviewCase(MatrixCase item) {
		return item.templateType() != null && normalize(item.templateType()).equals("booking preview");
	}

	private static boolean expectsLink(MatrixCase item) {
		if (item.generatesLink() == null || !item.generatesLink().toLowerCase(Locale.ROOT).startsWith("sí")) {
			return false;
		}
		String condition = normalize(item.linkCondition());
		return !(condition.contains("si existe") || condition.contains("externo")
				|| condition.contains("ubica reserva temporal vigente"));
	}

	private static void assertExpectedMissingData(MatrixCase item, AgentRoutingResult result) {
		String expected = normalize(item.expectedMissingData());
		if (expected.isBlank() || expected.equals("ninguna") || expected.equals("ninguno")
				|| expected.startsWith("ninguno ") || expected.startsWith("ninguna ") || expected.equals("no aplica")) {
			assertThat(result.missingData()).as(item.id() + " expected no missing data").isEmpty();
			return;
		}
		for (String part : expected.split(",|/")) {
			String token = normalize(part);
			if (!token.isBlank() && !token.equals("ninguna") && !token.equals("ninguno")
					&& !token.equals("no aplica")) {
				assertThat(result.missingData().stream().map(AiExcelMatrixOrchestratorCoverageTest::normalize).toList())
						.as(item.id() + " missing data should include " + token)
						.anyMatch(value -> value.contains(token) || token.contains(value));
			}
		}
	}

	private static boolean containsSignificantServiceToken(String response, String serviceName) {
		String normalizedResponse = normalize(response);
		for (String token : normalize(serviceName).split(" ")) {
			if (token.length() >= 4 && !token.equals("prueba") && normalizedResponse.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private static String sanitizeForUtf8(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		StringBuilder clean = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char current = value.charAt(i);
			if (Character.isHighSurrogate(current)) {
				if (i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
					clean.append(current).append(value.charAt(++i));
				}
				continue;
			}
			if (!Character.isLowSurrogate(current)) {
				clean.append(current);
			}
		}
		return clean.toString();
	}

	private List<MatrixCase> loadMatrixCases() throws IOException {
		return loadJson("/ai-matrix/matriz_qa_v23_4_10.json", new TypeReference<List<MatrixCase>>() {
		});
	}

	private List<ServiceFixture> loadServices() throws IOException {
		return loadJson("/ai-matrix/servicios_v23_4_10.json", new TypeReference<List<ServiceFixture>>() {
		});
	}

	private List<EntityAliasFixture> loadAliases() throws IOException {
		return loadJson("/ai-matrix/alias_entidades_v23_4_10.json", new TypeReference<List<EntityAliasFixture>>() {
		});
	}

	private <T> T loadJson(String resource, TypeReference<T> type) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			assertThat(input).as("resource " + resource).isNotNull();
			return mapper.readValue(input, type);
		}
	}

	private static String normalize(String value) {
		return TextNormalizer.normalize(value == null ? "" : value);
	}

	private static class Harness {
		private final List<ServiceFixture> services;
		private final List<EntityAliasFixture> aliases;
		private final BusinessLocationRecord providencia = new BusinessLocationRecord(
				UUID.fromString("81000000-0000-0000-0000-000000000001"), BUSINESS_ID, "providencia", "Providencia",
				"Av. Providencia 1234", "Santiago", "Providencia", "+56911111111", "+56911111111", "America/Santiago",
				null, null, null, true, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
		private final BusinessLocationRecord lasCondes = new BusinessLocationRecord(
				UUID.fromString("81000000-0000-0000-0000-000000000002"), BUSINESS_ID, "las-condes", "Las Condes",
				"Av. Apoquindo 4567", "Santiago", "Las Condes", "+56922222222", "+56922222222", "America/Santiago",
				null, null, null, true, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
		private final InMemoryAiAgentRepository contextRepository = new InMemoryAiAgentRepository();
		private final BusinessAiSettingsService businessAiSettingsService = Mockito.mock();
		private final AgentCoordinatorService coordinator;
		private final IntentDetectorService detector;
		private final EntityExtractionService extractor;
		private final AgentRegistry registry;

		Harness(List<ServiceFixture> services, List<EntityAliasFixture> aliases) {
			this.services = services;
			this.aliases = aliases;
			AiBusinessKnowledgeService knowledge = new AiBusinessKnowledgeService(
					new MatrixKnowledgeRepository(services, aliases));
			BusinessLocationJdbcRepository locationRepository = locationRepository();
			TransactionalAgendaBookingService transactional = transactionalAgendaBookingService();
			this.detector = new IntentDetectorService();
			this.extractor = new EntityExtractionService(knowledge, locationRepository);
			this.registry = new AgentRegistry(List.of(new ReceptionAgent(), new SalesAgent(knowledge),
					new BookingAgent(knowledge, transactional, Mockito.mock(
							com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.class),
							Mockito.mock(com.asistentewhatsapp.bookings.application.BookingConfirmationService.class),
							Mockito.mock(
									com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.class)),
					new PaymentsAgent(knowledge), new SupportAgent(locationRepository), new KnowledgeAgent(),
					new FollowUpAgent(), new HumanHandoffAgent()));
			Mockito.when(businessAiSettingsService.findSettingsOpt(Mockito.any()))
					.thenReturn(Optional.of(activeSettings()));
			this.coordinator = new AgentCoordinatorService(enabledProperties(), detector, extractor, registry,
					contextRepository, businessAiSettingsService);
		}

		AgentRoutingResult route(String body, UUID conversationId) {
			AgentConversationRequest request = request(body, conversationId, false);
			IntentDetectionResult detected = detector.detect(request);
			Map<String, String> entities = new LinkedHashMap<>(extractor.extract(request));
			AgentHandler handler = registry.resolve(detected);
			return handler.handle(request, detected, entities, List.of());
		}

		Optional<AgentRoutingResult> preview(String body, UUID conversationId) {
			return coordinator.preview(request(body, conversationId, true));
		}

		Optional<AgentRoutingResult> routePersisted(String body, UUID conversationId) {
			return coordinator.route(request(body, conversationId, false));
		}

		private AgentConversationRequest request(String body, UUID conversationId, boolean dryRun) {
			return new AgentConversationRequest(BUSINESS_ID, CHANNEL_ACCOUNT_ID, conversationId, CUSTOMER_ID,
					CUSTOMER_PHONE, "Contacto 0505", body, OffsetDateTime.now(ZoneOffset.UTC), null, null, null,
					dryRun);
		}

		private BusinessLocationJdbcRepository locationRepository() {
			BusinessLocationJdbcRepository repository = Mockito.mock(BusinessLocationJdbcRepository.class);
			Mockito.when(repository.findActive(Mockito.any())).thenReturn(List.of(lasCondes, providencia));
			Mockito.when(repository.countActive(Mockito.any())).thenReturn(2L);
			return repository;
		}

		private TransactionalAgendaBookingService transactionalAgendaBookingService() {
			TransactionalAgendaBookingService service = Mockito.mock(TransactionalAgendaBookingService.class);
			Mockito.when(service.resolveEffectiveLocation(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
						String message = normalize(invocation.getArgument(1, String.class));
						String extracted = normalize(invocation.getArgument(2, String.class));
						String selected = normalize(invocation.getArgument(4, String.class));
						String combined = String.join(" ", message, extracted, selected);
						if (combined.contains("providencia")) {
							return new TransactionalAgendaBookingService.ResolvedLocation(providencia, "MESSAGE_TEXT");
						}
						if (combined.contains("condes")) {
							return new TransactionalAgendaBookingService.ResolvedLocation(lasCondes, "MESSAGE_TEXT");
						}
						return new TransactionalAgendaBookingService.ResolvedLocation(null, "MISSING");
					});
			Mockito.when(service.createTemporaryBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
					.thenAnswer(invocation -> {
						String serviceText = invocation.getArgument(6, String.class);
						String location = invocation.getArgument(7, String.class);
						String date = invocation.getArgument(8, String.class);
						String time = invocation.getArgument(9, String.class);
						Boolean dryRun = invocation.getArgument(11, Boolean.class);
						if (isBlank(serviceText) || isBlank(location) || isBlank(date) || isBlank(time)) {
							return Optional.empty();
						}
						if (Boolean.TRUE.equals(dryRun)) {
							return Optional
									.of(WhatsAppMessageFormatter.bookingPreview(serviceText, location, date, time));
						}
						return Optional.of(WhatsAppMessageFormatter.temporaryBookingCreated(serviceText, location, date,
								time, "https://matrix-test.trycloudflare.com/reservas/confirmar/TOKEN-DE-PRUEBA", 720));
					});
			Mockito.when(service.generateBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new TransactionalAgendaBookingService.BookingLinkResult(
							"https://matrix-test.trycloudflare.com/reservas/confirmar/TOKEN-DE-PRUEBA", true));
			return service;
		}

		private static boolean isBlank(String value) {
			return value == null || value.isBlank();
		}

		private AiAgentProperties enabledProperties() {
			AiAgentProperties properties = new AiAgentProperties();
			properties.setEnabled(true);
			properties.setAuditEnabled(true);
			properties.setAutoReplyEnabled(false);
			return properties;
		}

		private BusinessAiSettingsResponse activeSettings() {
			return new BusinessAiSettingsResponse(UUID.randomUUID(), UUID.randomUUID(), true, "auto", "amigable", "es",
					new java.math.BigDecimal("0.5"), true, true, true, true, java.util.List.of(), java.util.List.of(),
					null, null, null, null);
		}
	}

	private static class InMemoryAiAgentRepository
			extends
				com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository {
		private AgentRoutingResult lastResult;

		InMemoryAiAgentRepository() {
			super(null, new ObjectMapper());
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

	private static class MatrixKnowledgeRepository implements AiKnowledgeRepository {
		private final List<ServiceFixture> services;
		private final List<EntityAliasFixture> aliases;

		MatrixKnowledgeRepository(List<ServiceFixture> services, List<EntityAliasFixture> aliases) {
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
				case "AI_BOOKING_COMPLETE_RESPONSE" -> Optional.of(rule(code,
						"Perfecto. Tengo {service} para {date} a las {time}. Debo validar disponibilidad real en agenda antes de confirmar. ¿Quieres que revise esa hora?",
						Map.of()));
				case "AI_BOOKING_MISSING_SERVICE_RESPONSE" -> Optional.of(rule(code,
						"Perfecto. Para revisar disponibilidad necesito el servicio específico. Por ejemplo: {examples}.",
						Map.of("examples", List.of("depilación bozo", "rostro", "axilas", "piernas", "bikini"))));
				case "AI_BOOKING_MISSING_DATE_RESPONSE" ->
					Optional.of(rule(code, "Perfecto, reviso {service}. ¿Para qué día quieres agendar?", Map.of()));
				case "AI_BOOKING_MISSING_TIME_RESPONSE" -> Optional
						.of(rule(code, "Perfecto, reviso {service} para {date}. ¿Qué horario te acomoda?", Map.of()));
				case "AI_BOOKING_CHANGE_IDENTIFY_RESPONSE" -> Optional.of(rule(code,
						"Claro. Para ayudarte a cambiar la hora, ¿me indicas tu nombre, correo o la fecha de la cita actual?",
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
			loaded.add(new EntityAlias("providencia", "sede", "Providencia", 100));
			loaded.add(new EntityAlias("las condes", "sede", "Las Condes", 100));
			return loaded;
		}

		@Override
		public List<IntentExpression> findActiveIntentExpressions(UUID businessId) {
			return List.of();
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
			if (digits.isBlank()) {
				return BigDecimal.ZERO;
			}
			return new BigDecimal(digits);
		}
	}

	private record MatrixCase(String id, String functionalGroup, String expectedIntent, String expectedAgent,
			String customerQuestion, String triggerCondition, String expectedEntities, String expectedMissingData,
			String expectedAiResponse, String generatesLink, String templateType, String linkCondition,
			String requiresHuman, String expectedUrgency, String technicalSource, String observation,
			boolean strictIntentAgent, boolean strictResponseContains) {
	}

	private record ServiceFixture(String code, String name, int durationMinutes, String priceText,
			String requiresEvaluation, String requiresConsent, String source) {
	}

	private record EntityAliasFixture(String alias, String entityKey, String entityValue, int priority, String source) {
	}

	private record AuditRow(String id, String question, String expectedIntent, String actualIntent, boolean intentOk,
			String expectedAgent, String actualAgent, boolean agentOk, boolean containsLink, String response) {

		static AuditRow from(MatrixCase item, AgentRoutingResult result) {
			return new AuditRow(item.id(), item.customerQuestion(), item.expectedIntent(),
					result.primaryIntent().name(), item.expectedIntent().equals(result.primaryIntent().name()),
					item.expectedAgent(), result.agentType().name(),
					item.expectedAgent().equals(result.agentType().name()),
					result.responseToCustomer() != null && result.responseToCustomer().contains("/reservas/confirmar/"),
					result.responseToCustomer());
		}
	}
}
