package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository;
import com.asistentewhatsapp.businessai.api.BusinessAiSettingsResponse;
import com.asistentewhatsapp.businessai.application.BusinessAiSettingsService;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiAmbiguityAndErrorsTest {

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
			new SalesAgent(knowledgeService),
			new BookingAgent(knowledgeService, transactionalAgendaBookingService,
					Mockito.mock(com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.class),
					Mockito.mock(com.asistentewhatsapp.bookings.application.BookingConfirmationService.class),
					Mockito.mock(com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository.class)),
			new PaymentsAgent(knowledgeService), new SupportAgent(locationRepository), new KnowledgeAgent(),
			new FollowUpAgent(), new HumanHandoffAgent()));
	private final AgentCoordinatorService coordinator;

	AiAmbiguityAndErrorsTest() {
		Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any()))
				.thenReturn(Optional.empty());
		Mockito.when(businessAiSettingsService.findSettingsOpt(Mockito.any()))
				.thenReturn(Optional.of(activeSettings()));
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
	}

	private AgentConversationRequest request(String message) {
		return new AgentConversationRequest(businessId, channelAccountId, conversationId, customerId, "+56900000000",
				"Cliente Test", message, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), null, null,
				"trace-test", false);
	}

	@Test
	void minimalNonActionableMessageShouldBeHandled() {
		var result = coordinator.route(request("ok"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void gibberishShouldReturnGenericResponse() {
		var result = coordinator.route(request("asdfghjklzxcvbnm"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void veryLongMessageShouldNotCrash() {
		String longMsg = "hola quiero agendar ".repeat(100);
		var result = coordinator.route(request(longMsg));
		assertThat(result).isPresent();
	}

	@Test
	void mixedIntentsShouldNotCrash() {
		var result = coordinator.route(request("quiero agendar y tambien saber precios"));
		assertThat(result).isPresent();
	}

	@Test
	void onlyNumbersShouldBeHandled() {
		var result = coordinator.route(request("12345"));
		assertThat(result).isPresent();
	}

	@Test
	void specialCharactersShouldNotCrash() {
		var result = coordinator.route(request("!!! @@ hola ??? ###"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void uppercaseMessageShouldBeProcessed() {
		var result = coordinator.route(request("QUIERO AGENDAR UNA LIMPIEZA FACIAL"));
		assertThat(result).isPresent();
	}

	@Test
	void repeatedMessagesShouldNotCrash() {
		var first = coordinator.route(request("quiero agendar"));
		assertThat(first).isPresent();
		var second = coordinator.route(request("quiero agendar"));
		assertThat(second).isPresent();
	}

	@Test
	void emptyMessageShouldNotProduceResponse() {
		var result = coordinator.route(request(""));
		assertThat(result).isEmpty();
	}

	@Test
	void whitespaceOnlyShouldNotProduceResponse() {
		var result = coordinator.route(request("   "));
		assertThat(result).isEmpty();
	}

	@Test
	void emojiOnlyShouldBeHandled() {
		var result = coordinator.route(request("👍🎉❤️"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void htmlInjectionShouldNotCrash() {
		var result = coordinator.route(request("<script>alert('xss')</script>"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void sqlInjectionAttemptShouldBeHandled() {
		var result = coordinator.route(request("'; DROP TABLE bookings; --"));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	@Test
	void veryLongSingleWordShouldNotCrash() {
		String longWord = "a".repeat(10000);
		var result = coordinator.route(request(longWord));
		assertThat(result).isPresent();
		assertThat(result.get().responseToCustomer()).isNotBlank();
	}

	private static AiAgentProperties enabledProperties() {
		AiAgentProperties props = new AiAgentProperties();
		props.setEnabled(true);
		return props;
	}

	private static BusinessAiSettingsResponse activeSettings() {
		return new BusinessAiSettingsResponse(UUID.randomUUID(), UUID.randomUUID(), true, "auto", "amigable", "es",
				new java.math.BigDecimal("0.5"), true, true, true, true, java.util.List.of(), java.util.List.of(), null,
				null, null, null);
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

		@Override
		public List<IntentExpression> findActiveIntentExpressions(UUID businessId) {
			return List.of();
		}
	}
}
