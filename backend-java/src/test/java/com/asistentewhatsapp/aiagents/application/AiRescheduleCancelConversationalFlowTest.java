package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository;
import com.asistentewhatsapp.businessai.application.BusinessAiSettingsService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiRescheduleCancelConversationalFlowTest {

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

	AiRescheduleCancelConversationalFlowTest() {
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
		Mockito.when(transactionalAgendaBookingService.generateBookingLink(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(
						new TransactionalAgendaBookingService.BookingLinkResult("http://localhost/reservar", false));
	}

	private AgentConversationRequest request(String message) {
		return new AgentConversationRequest(businessId, channelAccountId, conversationId, customerId, "+56900000000",
				"Cliente Test", message, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), null, null,
				"trace-test", false);
	}

	@Test
	void rescheduleShouldBeDetected() {
		var result = coordinator.route(request("quiero reprogramar mi cita del viernes"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_CHANGE);
	}

	@Test
	void cancelShouldBeDetected() {
		var result = coordinator.route(request("quiero cancelar mi cita"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_CANCEL);
	}

	@Test
	void changeOfMindMessageIsHandled() {
		var result = coordinator.route(request("sabes qué, mejor no"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void ambiguousCancelPhrasesShouldBeRecognized() {
		for (String msg : List.of("ya no puedo", "mejor lo dejamos", "no voy a poder")) {
			var result = coordinator.route(request(msg));
			assertThat(result).as("Mensaje: '%s' debería detectar cancelación", msg).isPresent();
		}
	}

	@Test
	void greetingThenCancelFlowShouldWork() {
		var greeting = coordinator.route(request("hola"));
		assertThat(greeting).isPresent();
		assertThat(greeting.get().primaryIntent()).isEqualTo(AgentIntent.GREETING);

		var cancel = coordinator.route(request("necesito cancelar una hora"));
		assertThat(cancel).isPresent();
		assertThat(cancel.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_CANCEL);
	}

	@Test
	void cancelThenBookAgainShouldWork() {
		var cancel = coordinator.route(request("cancela mi cita"));
		assertThat(cancel).isPresent();

		var book = coordinator.route(request("agenda una nueva"));
		assertThat(book).isPresent();
		assertThat(book.get().primaryIntent()).isIn(AgentIntent.BOOKING_REQUEST, AgentIntent.AMBIGUOUS);
	}

	@Test
	void cancelDirectlyShouldReturnLink() {
		var result = coordinator.route(request("cancela mi cita"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void rescheduleWithServiceNameShouldBeDetected() {
		var result = coordinator.route(request("quiero reprogramar depilacion bozo"));
		assertThat(result).isPresent();
		assertThat(result.get().primaryIntent()).isEqualTo(AgentIntent.BOOKING_CHANGE);
	}

	@Test
	void rescheduleThenCancelShouldWork() {
		var reschedule = coordinator.route(request("quiero reprogramar mi cita del viernes"));
		assertThat(reschedule).isPresent();

		var cancel = coordinator.route(request("mejor cancelala"));
		assertThat(cancel).isPresent();
		assertThat(cancel.get().primaryIntent()).isIn(AgentIntent.BOOKING_CANCEL, AgentIntent.AMBIGUOUS);
	}

	@Test
	void cancelAllSynonymsShouldBeRecognized() {
		for (String msg : List.of("cancelar", "anular", "eliminar cita", "borrar reserva", "dejar sin efecto")) {
			var result = coordinator.route(request(msg));
			assertThat(result).as("Mensaje: '%s' debería procesarse sin error", msg).isPresent();
			assertThat(result.get().responseToCustomer()).as("Mensaje: '%s' debería tener respuesta", msg).isNotBlank();
		}
	}

	private static AiAgentProperties enabledProperties() {
		AiAgentProperties props = new AiAgentProperties();
		props.setEnabled(true);
		return props;
	}

	private static BusinessLocationJdbcRepository mockLocationRepository() {
		BusinessLocationJdbcRepository repo = Mockito.mock(BusinessLocationJdbcRepository.class);
		Mockito.when(repo.findActive(Mockito.any())).thenReturn(List.of());
		Mockito.when(repo.countActive(Mockito.any())).thenReturn(0L);
		return repo;
	}

	private static class TestAiKnowledgeRepo implements AiKnowledgeRepository {
		@Override
		public List<ServiceCatalogItem> findActiveServices(UUID businessId) {
			return List.of(new ServiceCatalogItem("FAC-LIMPIEZA", "Limpieza facial profunda", "FACIAL", 60,
					new java.math.BigDecimal("34990")));
		}
		@Override
		public Optional<ResponseRule> findActiveRule(UUID businessId, String code) {
			return Optional.empty();
		}
		@Override
		public List<EntityAlias> findActiveEntityAliases(UUID businessId) {
			return List.of();
		}
	}
}
