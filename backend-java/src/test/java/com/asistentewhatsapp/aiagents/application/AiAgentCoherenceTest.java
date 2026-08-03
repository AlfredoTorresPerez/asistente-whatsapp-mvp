package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.businessai.api.BusinessAiSettingsResponse;
import com.asistentewhatsapp.businessai.application.BusinessAiSettingsService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiAgentCoherenceTest {

	private final UUID businessId = UUID.randomUUID();
	private final UUID channelAccountId = UUID.randomUUID();
	private final UUID conversationId = UUID.randomUUID();
	private final UUID customerId = UUID.randomUUID();
	private final AiBusinessKnowledgeService knowledgeService = new AiBusinessKnowledgeService(
			new TestAiKnowledgeRepository());
	private final BusinessLocationJdbcRepository locationRepository = emptyLocationRepository();
	private final TransactionalAgendaBookingService transactionalAgendaBookingService = emptyTransactionalAgendaBookingService();
	private final IntentDetectorService detector = new IntentDetectorService();
	private final EntityExtractionService extractor = new EntityExtractionService(knowledgeService, locationRepository);
	private final AgentRegistry registry = new AgentRegistry(List.of(new ReceptionAgent(),
			new SalesAgent(knowledgeService),
			new BookingAgent(knowledgeService, transactionalAgendaBookingService,
					Mockito.mock(com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.class),
					Mockito.mock(com.asistentewhatsapp.bookings.application.BookingConfirmationService.class),
					Mockito.mock(com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.class)),
			new PaymentsAgent(knowledgeService), new SupportAgent(locationRepository), new KnowledgeAgent(),
			new FollowUpAgent(), new HumanHandoffAgent()));
	private final BusinessAiSettingsService businessAiSettingsService = activeBusinessAiSettingsService();

	private BusinessAiSettingsService activeBusinessAiSettingsService() {
		BusinessAiSettingsService service = Mockito.mock(BusinessAiSettingsService.class);
		Mockito.when(service.findSettingsOpt(Mockito.any()))
				.thenReturn(Optional.of(new BusinessAiSettingsResponse(UUID.randomUUID(), UUID.randomUUID(), true,
						"auto", "amigable", "es", new java.math.BigDecimal("0.5"), true, true, true, true,
						java.util.List.of(), java.util.List.of(), null, null, null, null)));
		return service;
	}

	private BusinessLocationJdbcRepository emptyLocationRepository() {
		BusinessLocationJdbcRepository repository = Mockito.mock(BusinessLocationJdbcRepository.class);
		Mockito.when(repository.findActive(Mockito.any())).thenReturn(List.of());
		Mockito.when(repository.countActive(Mockito.any())).thenReturn(0L);
		return repository;
	}

	private TransactionalAgendaBookingService emptyTransactionalAgendaBookingService() {
		TransactionalAgendaBookingService service = Mockito.mock(TransactionalAgendaBookingService.class);
		Mockito.when(service.resolveEffectiveLocation(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(new TransactionalAgendaBookingService.ResolvedLocation(null, "MISSING"));
		Mockito.when(service.createTemporaryBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
		Mockito.when(service.generateBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(
						new TransactionalAgendaBookingService.BookingLinkResult("http://localhost/reservar", false));
		Mockito.when(service.checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(Optional.empty());
		return service;
	}

	@Test
	void bookingAgentDoesNotAskServiceAgainWhenServiceDateAndTimeAreKnown() {
		BookingAgent agent = new BookingAgent(knowledgeService, transactionalAgendaBookingService,
				Mockito.mock(com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.class),
				Mockito.mock(com.asistentewhatsapp.bookings.application.BookingConfirmationService.class),
				Mockito.mock(com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.class));
		Map<String, String> entities = new LinkedHashMap<>();
		entities.put("servicio_o_producto", "Depilacion bozo");
		entities.put("fecha_relativa", "mañana");
		entities.put("hora", "14:00");

		AgentRoutingResult result = agent.handle(request("a las 14 horas"),
				new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.9, "bajo", false, null), entities,
				java.util.List.of());

		assertThat(result.missingData()).containsExactly("sucursal");
		assertThat(result.responseToCustomer()).doesNotContain("/reservar");
		assertThat(result.responseToCustomer()).doesNotContain("qué servicio", "servicio específico", "a las a las");
	}

	@Test
	void freeEvaluationConversationFlowKeepsCommercialAndBookingCoherence() {
		assertRoute("Hola", AgentIntent.GREETING, AgentType.RECEPTION, "servicios", "precios", "agenda");
		assertRoute("Qué tipo de depilación tienen", AgentIntent.COMMERCIAL_INQUIRY, AgentType.SALES, "depilacion bozo",
				"rostro", "axilas", "cera", "laser");
		assertRoute("Cuanto cuesta depilacion bozo", AgentIntent.PRICE_REQUEST, AgentType.SALES, "$8.990",
				"15 minutos");
		assertRoute("Quiero agendar depilación bozo mañana a las 14 horas", AgentIntent.BOOKING_REQUEST,
				AgentType.BOOKING, "sucursal");
		assertRoute("Quiero hablar con una persona", AgentIntent.HUMAN_REQUEST, AgentType.HUMAN_HANDOFF, "derivar",
				"persona del equipo");
		assertRoute("Estoy molesta, nadie responde", AgentIntent.COMPLAINT, AgentType.HUMAN_HANDOFF, "derivar",
				"persona del equipo");
	}

	@Test
	void bookingAgentAsksOnlyForMissingData() {
		assertRoute("Quiero agendar depilación bozo", AgentIntent.BOOKING_REQUEST, AgentType.BOOKING, "sucursal",
				"día");
		assertRoute("Quiero agendar mañana", AgentIntent.BOOKING_REQUEST, AgentType.BOOKING, "servicio específico");
		assertRoute("Quiero agendar a las 14 horas", AgentIntent.BOOKING_REQUEST, AgentType.BOOKING,
				"servicio específico");
	}

	@Test
	void bookingExtractsFacialServiceAndWeekdayWithoutAskingServiceAgain() {
		AgentRoutingResult result = route("Hola, me gustaría reservar una limpieza facial para el viernes");

		assertThat(result.agentType()).isEqualTo(AgentType.BOOKING);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Limpieza facial profunda");
		assertThat(result.extractedData()).containsEntry("fecha_relativa", "viernes");
		assertThat(result.missingData()).doesNotContain("motivo_o_servicio", "fecha_deseada");
		assertThat(result.missingData()).containsExactly("sucursal");
		assertThat(result.responseToCustomer()).doesNotContain("/reservar");
	}

	@Test
	void priceAndServiceQuestionsUseCatalogWithoutMixingSimilarServices() {
		AgentRoutingResult depilation = route("Qué tipo de depilación tienen");
		assertNormalizedContains(depilation.responseToCustomer(), "depilacion bozo", "rostro", "axilas", "cera",
				"laser");
		assertThat(normalize(depilation.responseToCustomer())).doesNotContain("limpieza facial");

		AgentRoutingResult price = route("precio de depilacion facial");
		assertNormalizedContains(price.responseToCustomer(), "precio correcto");
		assertThat(normalize(price.responseToCustomer())).doesNotContain("limpieza facial profunda");
	}

	@Test
	void availabilityQuestionDoesNotConfirmCalendarSlot() {
		AgentRoutingResult result = route("Tienen disponibilidad mañana a las 14 para depilacion bozo?");

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.AVAILABILITY_QUERY);
		assertThat(result.responseToCustomer()).doesNotContain("/reservar");
		assertThat(normalize(result.responseToCustomer())).doesNotContain("confirmada", "confirmado");
	}

	@Test
	void technicalCommandsAreNotTreatedAsCommercialRequests() {
		IntentDetectionResult result = detector.detect(request("docker compose up --build"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.TECHNICAL_MESSAGE);
	}

	@Test
	void socialGreetingIsNotExtractedAsCustomerName() {
		Map<String, String> entities = extractor.extract(request("Como estas"));

		assertThat(entities).doesNotContainKey("nombre");
	}

	@Test
	void supportAgentGivesBusinessSafeAnswerForTechnicalMessages() {
		SupportAgent agent = new SupportAgent(locationRepository);

		AgentRoutingResult result = agent.handle(request("docker compose up --build"),
				new IntentDetectionResult(AgentIntent.TECHNICAL_MESSAGE, null, 0.91, "bajo", false, null),
				new LinkedHashMap<>(), java.util.List.of());

		assertThat(result.responseToCustomer()).contains("mensaje parece técnico");
		assertThat(result.responseToCustomer()).contains("servicios, precios o agenda");
	}

	@Test
	void nonUsefulMessageDoesNotProduceCommercialResponse() {
		AgentCoordinatorService coordinator = new AgentCoordinatorService(enabledProperties(), detector, extractor,
				registry, new InMemoryAiAgentRepository(), businessAiSettingsService);

		assertThat(coordinator.preview(request(""))).isEmpty();
		assertThat(coordinator.preview(request("Mensaje recibido sin texto"))).isEmpty();
	}

	@Test
	void contextAwareBookingKeepsPreviousDataAcrossTurns() {
		InMemoryAiAgentRepository repository = new InMemoryAiAgentRepository();
		AgentCoordinatorService coordinator = new AgentCoordinatorService(enabledProperties(), detector, extractor,
				registry, repository, businessAiSettingsService);

		AgentRoutingResult first = coordinator.route(request("Quiero agendar depilacion bozo")).orElseThrow();
		assertThat(first.missingData()).contains("sucursal");

		AgentRoutingResult second = coordinator.route(request("mañana")).orElseThrow();
		assertThat(second.missingData()).contains("sucursal");

		AgentRoutingResult third = coordinator.route(request("a las 14 horas")).orElseThrow();
		assertThat(third.missingData()).contains("sucursal");
		assertThat(third.responseToCustomer()).doesNotContain("/reservar");
	}

	private AgentConversationRequest request(String body) {
		return new AgentConversationRequest(businessId, channelAccountId, conversationId, customerId, "56950954580",
				"Contacto de prueba", body, OffsetDateTime.now(ZoneOffset.UTC));
	}

	private AgentRoutingResult route(String body) {
		AgentConversationRequest request = request(body);
		IntentDetectionResult detected = detector.detect(request);
		Map<String, String> entities = new LinkedHashMap<>(extractor.extract(request));
		AgentHandler handler = registry.resolve(detected);
		return handler.handle(request, detected, entities, List.of());
	}

	private void assertRoute(String body, AgentIntent intent, AgentType agentType, String... expectedResponseParts) {
		AgentConversationRequest request = request(body);
		IntentDetectionResult detected = detector.detect(request);
		Map<String, String> entities = new LinkedHashMap<>(extractor.extract(request));
		AgentRoutingResult result = registry.resolve(detected).handle(request, detected, entities, List.of());

		assertThat(detected.primaryIntent()).as(body).isEqualTo(intent);
		assertThat(result.agentType()).as(body).isEqualTo(agentType);
		assertNormalizedContains(result.responseToCustomer(), expectedResponseParts);
	}

	private void assertNormalizedContains(String actual, String... expectedParts) {
		String normalizedActual = normalize(actual);
		for (String expected : expectedParts) {
			assertThat(normalizedActual).as("response should contain %s", expected).contains(normalize(expected));
		}
	}

	private String normalize(String value) {
		return TextNormalizer.normalize(value);
	}

	private AiAgentProperties enabledProperties() {
		AiAgentProperties properties = new AiAgentProperties();
		properties.setEnabled(true);
		properties.setAuditEnabled(true);
		properties.setAutoReplyEnabled(false);
		return properties;
	}

	private class InMemoryAiAgentRepository
			extends
				com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository {
		private AgentRoutingResult lastResult;

		InMemoryAiAgentRepository() {
			super(null, new com.fasterxml.jackson.databind.ObjectMapper());
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

	private static class TestAiKnowledgeRepository implements AiKnowledgeRepository {
		@Override
		public List<ServiceCatalogItem> findActiveServices(UUID businessId) {
			return List.of(
					new ServiceCatalogItem("DEP-BOZO", "Depilacion bozo", "DEPILACION", 15, new BigDecimal("8990")),
					new ServiceCatalogItem("DEP-CERA", "Depilacion con cera", "DEPILACION", 30,
							new BigDecimal("15990")),
					new ServiceCatalogItem("DEP-ROSTRO", "Depilacion rostro", "DEPILACION", 30,
							new BigDecimal("18990")),
					new ServiceCatalogItem("DEP-AXILAS", "Depilacion axilas", "DEPILACION", 25,
							new BigDecimal("19990")),
					new ServiceCatalogItem("DEP-LASER", "Depilacion laser", "DEPILACION", 30, new BigDecimal("24990")),
					new ServiceCatalogItem("FAC-LIMPIEZA", "Limpieza facial profunda", "FACIAL", 60,
							new BigDecimal("34990")));
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
				default -> Optional.empty();
			};
		}

		@Override
		public List<EntityAlias> findActiveEntityAliases(UUID businessId) {
			return List.of(new EntityAlias("depilacion bozo", "servicio_o_producto", "Depilacion bozo", 300),
					new EntityAlias("bozo", "servicio_o_producto", "Depilacion bozo", 290),
					new EntityAlias("manana", "fecha_relativa", "mañana", 100),
					new EntityAlias("hoy", "fecha_relativa", "hoy", 100));
		}

		@Override
		public List<IntentExpression> findActiveIntentExpressions(UUID businessId) {
			return List.of();
		}

		private ResponseRule rule(String code, String template, Map<String, Object> payload) {
			return new ResponseRule(code, template, payload);
		}
	}
}
