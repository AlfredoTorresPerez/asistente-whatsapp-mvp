package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository;
import com.asistentewhatsapp.businessai.application.BusinessAiSettingsService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiBookingConversationalFlowTest {

	private final UUID businessId = UUID.randomUUID();
	private final UUID channelAccountId = UUID.randomUUID();
	private final UUID conversationId = UUID.randomUUID();
	private final UUID customerId = UUID.randomUUID();
	private final IntentDetectorService detector = new IntentDetectorService();
	private final AiBusinessKnowledgeService knowledgeService = new AiBusinessKnowledgeService(
			new TestAiKnowledgeRepo());
	private final BusinessLocationJdbcRepository locationRepository = mockLocationRepository();
	private final TransactionalAgendaBookingService transactionalAgendaBookingService = Mockito.mock();
	private final AiAgentJdbcRepository aiAgentJdbcRepository = Mockito.mock();
	private final BusinessAiSettingsService businessAiSettingsService = Mockito.mock();
	private final EntityExtractionService extractor = new EntityExtractionService(knowledgeService, locationRepository);
	private final AgentRegistry registry = new AgentRegistry(List.of(new ReceptionAgent(),
			new SalesAgent(knowledgeService), new BookingAgent(knowledgeService, transactionalAgendaBookingService),
			new PaymentsAgent(knowledgeService), new SupportAgent(locationRepository), new KnowledgeAgent(),
			new FollowUpAgent(), new HumanHandoffAgent()));
	private final AgentCoordinatorService coordinator;

	AiBookingConversationalFlowTest() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any()))
				.thenReturn(Optional.empty());
		Mockito.when(businessAiSettingsService.findSettingsOpt(Mockito.any())).thenReturn(Optional.empty());
		coordinator = new AgentCoordinatorService(enabledProperties(), detector, extractor, registry,
				aiAgentJdbcRepository, businessAiSettingsService);
	}

	@BeforeEach
	void setUp() {
		Mockito.reset(transactionalAgendaBookingService);
		Mockito.when(transactionalAgendaBookingService.resolveEffectiveLocation(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(new TransactionalAgendaBookingService.ResolvedLocation(null, "MISSING"));
		Mockito.when(transactionalAgendaBookingService.createTemporaryBookingLink(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
				.thenReturn(Optional.empty());
		Mockito.when(transactionalAgendaBookingService.generateBookingLink(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(
						new TransactionalAgendaBookingService.BookingLinkResult("http://localhost/reservar", false));
		Mockito.when(transactionalAgendaBookingService.checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(Optional.empty());
	}

	private AgentConversationRequest request(String message) {
		return new AgentConversationRequest(businessId, channelAccountId, conversationId, customerId, "+56900000000",
				"Cliente Test", message, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), null, null,
				"trace-test", false);
	}

	@Test
	void greetingShouldBeDetected() {
		var result = coordinator.route(request("hola"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.GREETING);
	}

	@Test
	void bookingIntentShouldBeDetected() {
		var result = coordinator.route(request("quiero agendar una hora"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void cancelIntentShouldBeDetected() {
		var result = coordinator.route(request("quiero cancelar mi cita"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_CANCEL);
	}

	@Test
	void rescheduleIntentShouldBeDetected() {
		var result = coordinator.route(request("quiero reprogramar mi cita"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_CHANGE);
	}

	@Test
	void priceRequestShouldBeDetected() {
		var result = coordinator.route(request("cuanto cuesta la limpieza facial"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.PRICE_REQUEST);
	}

	@Test
	void humanHandoffShouldBeDetected() {
		var result = coordinator.route(request("quiero hablar con una persona"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.HUMAN_REQUEST);
	}

	@Test
	void ambiguousCancelShouldBeDetected() {
		var result = coordinator.route(request("ya no puedo ir"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isIn(AgentIntent.BOOKING_CANCEL, AgentIntent.AMBIGUOUS);
	}

	@Test
	void changeOfMindShouldNotBeBookingCancel() {
		var result = coordinator.route(request("sabes que, mejor no"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isNotEqualTo(AgentIntent.BOOKING_CANCEL);
	}

	@Test
	void followUpShouldUseContext() {
		var first = coordinator.route(request("quiero agendar una hora"));
		assertThat(first).isPresent();
		var second = coordinator.route(request("es para limpieza facial"));
		assertThat(second).isPresent();
		assertThat(second.get().primaryIntent()).isIn(AgentIntent.BOOKING_REQUEST, AgentIntent.AMBIGUOUS,
				AgentIntent.COMMERCIAL_INQUIRY);
	}

	@Test
	void expiredLinkShouldBeDetected() {
		var result = coordinator.route(request("el enlace expiro"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void resendLinkShouldBeDetected() {
		var result = coordinator.route(request("no me llego el link"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void brokenLinkShouldBeDetected() {
		var result = coordinator.route(request("no funciona el link"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void bookingFlowWithAllEntitiesDoesNotReturnGenericLinkWhenLocationIsMissing() {
		var result = coordinator.route(request("quiero agendar depilacion bozo manana a las 14"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
		assertThat(result.get().missingData()).contains("sucursal");
		assertThat(result.get().responseToCustomer()).doesNotContain("/reservar");
	}

	@Test
	void observedCustomerQueriesShouldNotReceiveGenericBookingLink() {
		var cases = List.of(
				caseExpectation("Quiero reservar una limpieza facial", AgentIntent.BOOKING_REQUEST, "sucursal",
						"fecha_deseada"),
				caseExpectation("¿Tienen hora mañana en Providencia?", AgentIntent.AVAILABILITY_QUERY,
						"servicio_o_producto"),
				caseExpectation("Reserva hidratación facial para el jueves en la tarde", AgentIntent.BOOKING_REQUEST,
						"sucursal"),
				caseExpectation("Qué horarios hay para manicure", AgentIntent.AVAILABILITY_QUERY, "sucursal"),
				caseExpectation("Quiero agendar con Carla Mendez", AgentIntent.BOOKING_REQUEST, "motivo_o_servicio"),
				caseExpectation("Tengo piel sensible, ¿qué tratamiento me recomiendas?",
						AgentIntent.SERVICE_RECOMMENDATION, "servicio_si_desea_agendar"),
				caseExpectation("Quiero algo para hidratar la piel", AgentIntent.SERVICE_RECOMMENDATION,
						"servicio_si_desea_agendar"),
				caseExpectation("Qué servicio sirve para relajarme", AgentIntent.SERVICE_RECOMMENDATION,
						"servicio_si_desea_agendar"),
				caseExpectation("Busco un tratamiento facial no invasivo", AgentIntent.SERVICE_RECOMMENDATION,
						"servicio_si_desea_agendar"),
				caseExpectation("Quiero hablar con una persona", AgentIntent.HUMAN_REQUEST, "contexto_para_ejecutivo"),
				caseExpectation(
						"Hola, quiero reservar una hidratación facial en Providencia para el jueves en la tarde",
						AgentIntent.BOOKING_REQUEST, "horario_preferido"));

		for (CaseExpectation item : cases) {
			AgentRoutingResult result = coordinator.route(request(item.query())).orElseThrow();

			assertThat(result.primaryIntent()).as(item.query()).isEqualTo(item.intent());
			assertThat(result.missingData()).as(item.query()).containsAll(item.expectedMissing());
			assertThat(result.responseToCustomer()).as(item.query()).doesNotContain("/reservar");
			if (item.intent() == AgentIntent.HUMAN_REQUEST) {
				assertThat(result.responseToCustomer()).contains("persona del equipo");
			} else {
				assertThat(result.responseToCustomer()).as(item.query()).doesNotContain("Reserva en línea");
			}
		}
	}

	@Test
	void standaloneBookingRequestShouldNotReuseConflictingBookingContext() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any())).thenReturn(Optional
				.of(previousBookingContext("Limpieza facial profunda", "FAC-LIMPIEZA", List.of("horario_preferido"))));

		AgentRoutingResult result = coordinator.route(request("Reserva hidratación facial para el jueves en la tarde"))
				.orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Hidratacion facial");
		assertThat(result.extractedData()).doesNotContainEntry("servicio_o_producto", "Limpieza facial profunda");
		assertThat(result.extractedData()).doesNotContainKey("sede");
		assertThat(result.missingData()).contains("sucursal");
	}

	@Test
	void professionalOnlyBookingRequestShouldNotReuseUnansweredBookingContext() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any())).thenReturn(
				Optional.of(previousBookingContext("Depilacion bozo", "DEP-BOZO", List.of("horario_preferido"))));

		AgentRoutingResult result = coordinator.route(request("Quiero agendar con Carla Mendez")).orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
		assertThat(result.extractedData()).containsEntry("profesional", "Carla Mendez");
		assertThat(result.extractedData()).doesNotContainKey("servicio_o_producto");
		assertThat(result.extractedData()).doesNotContainKey("preferencia_horaria");
		assertThat(result.missingData()).contains("motivo_o_servicio");
	}

	@Test
	void directAnswerToPendingBookingSlotShouldReuseCompatibleContext() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any())).thenReturn(Optional
				.of(previousBookingContext("Hidratacion facial", "FAC-HIDRATACION", List.of("horario_preferido"))));

		AgentRoutingResult result = coordinator.route(request("en la tarde")).orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Hidratacion facial");
		assertThat(result.extractedData()).containsEntry("sede", "Providencia");
		assertThat(result.extractedData()).containsEntry("fecha_relativa", "jueves");
		assertThat(result.extractedData()).containsEntry("tramo_horario", "tarde");
	}

	@Test
	void availabilityQuestionForPendingBookingTimeShouldReuseBookingContext() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any())).thenReturn(Optional
				.of(previousBookingContext("Limpieza facial profunda", "FAC-LIMPIEZA", List.of("horario_preferido"))));
		Mockito.when(transactionalAgendaBookingService.checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(Optional.of("Encontré horarios disponibles"));

		AgentRoutingResult result = coordinator.route(request("que horarios tienes disponibles")).orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.AVAILABILITY_QUERY);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Limpieza facial profunda");
		assertThat(result.extractedData()).containsEntry("sede", "Providencia");
		assertThat(result.extractedData()).containsEntry("fecha_relativa", "jueves");
		assertThat(result.responseToCustomer()).isEqualTo("Encontré horarios disponibles");
		Mockito.verify(transactionalAgendaBookingService).checkAvailability(Mockito.any(), Mockito.any(),
				Mockito.eq("Limpieza facial profunda"), Mockito.eq("Providencia"), Mockito.eq("jueves"), Mockito.any(),
				Mockito.any(), Mockito.eq("http://localhost/reservar"), Mockito.any(), Mockito.any());
	}

	@Test
	void numericAnswerToPresentedBookingOptionsShouldSelectSlotAndReuseContext() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any()))
				.thenReturn(Optional.of(previousBookingContextWithResponse("Hidratacion facial", "FAC-HIDRATACION",
						List.of("horario_preferido"),
						"Encontré estos horarios para Hidratacion facial en Providencia el jueves:\n\n"
								+ "1. 12:00 con Carla Mendez\n" + "2. 12:15 con Carla Mendez\n"
								+ "3. 12:30 con Carla Mendez\n\n" + "¿Cuál prefieres?")));
		Mockito.when(transactionalAgendaBookingService.createTemporaryBookingLink(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
				.thenReturn(Optional.of("Reserva temporal creada"));

		AgentRoutingResult result = coordinator.route(request("1")).orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Hidratacion facial");
		assertThat(result.extractedData()).containsEntry("sede", "Providencia");
		assertThat(result.extractedData()).containsEntry("fecha_relativa", "jueves");
		assertThat(result.extractedData()).containsEntry("hora", "12:00");
		assertThat(result.extractedData()).containsEntry("opcion_agenda_seleccionada", "1");
		assertThat(result.missingData()).isEmpty();
		assertThat(result.responseToCustomer()).isEqualTo("Reserva temporal creada");
	}

	@Test
	void numericAnswerWithDotToUnavailableTimeAlternativesShouldSelectAlternativeSlot() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any())).thenReturn(
				Optional.of(previousBookingContextWithResponse("Limpieza facial profunda", "FAC-LIMPIEZA", List.of(),
						"⚠️ Horario no disponible\n\n" + "No encontré disponibilidad para:\n\n"
								+ "Servicio: Limpieza facial profunda\n" + "Sucursal: Providencia\n" + "Fecha: mañana\n"
								+ "Hora solicitada: 15:00\n\n"
								+ "Puedo revisar otros horarios cercanos. Opciones disponibles:\n\n"
								+ "1. mañana a las 15:45\n" + "2. mañana a las 16:00\n" + "3. mañana a las 16:15\n\n"
								+ "¿Cuál prefieres?")));
		Mockito.when(transactionalAgendaBookingService.createTemporaryBookingLink(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
				.thenReturn(Optional.of("Reserva temporal creada"));

		AgentRoutingResult result = coordinator.route(request("1.")).orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Limpieza facial profunda");
		assertThat(result.extractedData()).containsEntry("sede", "Providencia");
		assertThat(result.extractedData()).containsEntry("fecha_relativa", "mañana");
		assertThat(result.extractedData()).containsEntry("hora", "15:45");
		assertThat(result.extractedData()).containsEntry("opcion_agenda_seleccionada", "1");
		assertThat(result.responseToCustomer()).isEqualTo("Reserva temporal creada");
	}

	@Test
	void serviceAnswerToPendingAvailabilityQueryShouldContinueAvailabilityLookup() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any()))
				.thenReturn(Optional.of(previousAvailabilityContext(List.of("servicio_o_producto"))));
		Mockito.when(transactionalAgendaBookingService.checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(Optional.of("Encontré horarios disponibles"));

		AgentRoutingResult result = coordinator.route(request("Hidratación facial")).orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.AVAILABILITY_QUERY);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Hidratacion facial");
		assertThat(result.extractedData()).containsEntry("sede", "Providencia");
		assertThat(result.extractedData()).containsEntry("fecha_relativa", "mañana");
		assertThat(result.responseToCustomer()).isEqualTo("Encontré horarios disponibles");
		Mockito.verify(transactionalAgendaBookingService).checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq("http://localhost/reservar"),
				Mockito.any(), Mockito.any());
	}

	@Test
	void numericAnswerToAvailabilityOptionsShouldCreateTemporaryBooking() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any()))
				.thenReturn(Optional.of(previousAvailabilityOptionsContext(List.of("horario_preferido"))));
		Mockito.when(transactionalAgendaBookingService.createTemporaryBookingLink(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
				.thenReturn(Optional.of("Reserva temporal creada"));

		AgentRoutingResult result = coordinator.route(request("1")).orElseThrow();

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
		assertThat(result.extractedData()).containsEntry("servicio_o_producto", "Hidratacion facial");
		assertThat(result.extractedData()).containsEntry("sede", "Providencia");
		assertThat(result.extractedData()).containsEntry("fecha_relativa", "mañana");
		assertThat(result.extractedData()).containsEntry("hora", "09:00");
		assertThat(result.responseToCustomer()).isEqualTo("Reserva temporal creada");
	}

	private CaseExpectation caseExpectation(String query, AgentIntent intent, String... expectedMissing) {
		return new CaseExpectation(query, intent, List.of(expectedMissing));
	}

	private AiAgentJdbcRepository.ConversationContextSnapshot previousBookingContext(String service, String serviceCode,
			List<String> missingData) {
		return previousBookingContextWithResponse(service, serviceCode, missingData, "respuesta anterior");
	}

	private AiAgentJdbcRepository.ConversationContextSnapshot previousBookingContextWithResponse(String service,
			String serviceCode, List<String> missingData, String lastResponse) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("servicio_o_producto", service);
		data.put("servicio_codigo", serviceCode);
		data.put("sede", "Providencia");
		data.put("fecha_relativa", "jueves");
		data.put("tramo_horario", "tarde");
		data.put("preferencia_horaria", "tarde");
		data.put("profesional", "Carla Mendez");
		data.put("ultimo_mensaje_cliente", "mensaje anterior");
		data.put("ultima_respuesta_ia", lastResponse);
		return new AiAgentJdbcRepository.ConversationContextSnapshot(AgentType.BOOKING, AgentIntent.BOOKING_REQUEST,
				null, data, missingData);
	}

	private AiAgentJdbcRepository.ConversationContextSnapshot previousAvailabilityContext(List<String> missingData) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("sede", "Providencia");
		data.put("fecha_relativa", "mañana");
		data.put("ultimo_mensaje_cliente", "Tienen hora mañana en Providencia?");
		data.put("ultima_respuesta_ia",
				"Sí, puedo revisarlo para mañana en Providencia. ¿Para qué servicio necesitas una hora?");
		return new AiAgentJdbcRepository.ConversationContextSnapshot(AgentType.BOOKING, AgentIntent.AVAILABILITY_QUERY,
				null, data, missingData);
	}

	private AiAgentJdbcRepository.ConversationContextSnapshot previousAvailabilityOptionsContext(
			List<String> missingData) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("servicio_o_producto", "Hidratacion facial");
		data.put("servicio_codigo", "FAC-HIDRATACION");
		data.put("sede", "Providencia");
		data.put("fecha_relativa", "mañana");
		data.put("ultimo_mensaje_cliente", "Hidratación facial");
		data.put("ultima_respuesta_ia", "Encontré estos horarios para Hidratacion facial en Providencia mañana:\n\n"
				+ "1. 09:00 con Carla Mendez\n" + "2. 09:15 con Carla Mendez\n" + "3. 11:00 con Carla Mendez\n\n"
				+ "Puedes responder con el número que prefieres o reservar aquí:\n\nhttp://localhost/reservar");
		return new AiAgentJdbcRepository.ConversationContextSnapshot(AgentType.BOOKING, AgentIntent.AVAILABILITY_QUERY,
				null, data, missingData);
	}

	private static AiAgentProperties enabledProperties() {
		AiAgentProperties props = new AiAgentProperties();
		props.setEnabled(true);
		return props;
	}

	private static BusinessLocationJdbcRepository mockLocationRepository() {
		BusinessLocationJdbcRepository repo = Mockito.mock(BusinessLocationJdbcRepository.class);
		BusinessLocationJdbcRepository.BusinessLocationRecord providencia = new BusinessLocationJdbcRepository.BusinessLocationRecord(
				UUID.fromString("81000000-0000-0000-0000-000000000001"), UUID.randomUUID(), "PROVIDENCIA",
				"Providencia", "Av. Providencia 2450", "Santiago", "Providencia", "+56955550100", null,
				"America/Santiago", null, null, null, true, java.time.OffsetDateTime.now(),
				java.time.OffsetDateTime.now());
		Mockito.when(repo.findActive(Mockito.any())).thenReturn(List.of(providencia));
		Mockito.when(repo.countActive(Mockito.any())).thenReturn(1L);
		return repo;
	}

	private record CaseExpectation(String query, AgentIntent intent, List<String> expectedMissing) {
	}

	private static class TestAiKnowledgeRepo implements AiKnowledgeRepository {
		@Override
		public List<ServiceCatalogItem> findActiveServices(UUID businessId) {
			return List.of(
					new ServiceCatalogItem("FAC-LIMPIEZA", "Limpieza facial profunda", "FACIAL", 60,
							new java.math.BigDecimal("34990")),
					new ServiceCatalogItem("FAC-HIDRATACION", "Hidratacion facial", "FACIAL", 45,
							new java.math.BigDecimal("29990")),
					new ServiceCatalogItem("MAN-PERM", "Manicure permanente", "MANICURE_PEDICURE", 60,
							new java.math.BigDecimal("24990")),
					new ServiceCatalogItem("COR-MASAJE", "Masaje relajante", "CORPORAL", 60,
							new java.math.BigDecimal("39990")),
					new ServiceCatalogItem("DEP-CERA", "Depilacion con cera", "DEPILACION", 30,
							new java.math.BigDecimal("15990")),
					new ServiceCatalogItem("DEP-BOZO", "Depilacion bozo", "DEPILACION", 15,
							new java.math.BigDecimal("8990")));
		}
		@Override
		public Optional<ResponseRule> findActiveRule(UUID businessId, String code) {
			return Optional.empty();
		}
		@Override
		public List<EntityAlias> findActiveEntityAliases(UUID businessId) {
			return List.of(new EntityAlias("facial", "servicio_o_producto", "Limpieza facial profunda", 120));
		}
	}
}
