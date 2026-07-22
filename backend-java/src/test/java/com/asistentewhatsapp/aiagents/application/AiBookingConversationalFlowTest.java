package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.infrastructure.AiAgentJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import java.util.List;
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
    private final AiBusinessKnowledgeService knowledgeService = new AiBusinessKnowledgeService(new TestAiKnowledgeRepo());
    private final BusinessLocationJdbcRepository locationRepository = mockLocationRepository();
    private final TransactionalAgendaBookingService transactionalAgendaBookingService = Mockito.mock();
    private final AiAgentJdbcRepository aiAgentJdbcRepository = Mockito.mock();
    private final EntityExtractionService extractor = new EntityExtractionService(knowledgeService, locationRepository);
    private final AgentRegistry registry = new AgentRegistry(List.of(
            new ReceptionAgent(),
            new SalesAgent(knowledgeService),
            new BookingAgent(knowledgeService, transactionalAgendaBookingService),
            new PaymentsAgent(knowledgeService),
            new SupportAgent(locationRepository),
            new KnowledgeAgent(),
            new FollowUpAgent(),
            new HumanHandoffAgent()));
    private final AgentCoordinatorService coordinator;

    AiBookingConversationalFlowTest() {
        Mockito.when(aiAgentJdbcRepository.findConversationContext(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        coordinator = new AgentCoordinatorService(
                enabledProperties(), detector, extractor, registry, aiAgentJdbcRepository);
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(transactionalAgendaBookingService);
        Mockito.when(transactionalAgendaBookingService.resolveEffectiveLocation(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new TransactionalAgendaBookingService.ResolvedLocation(null, "MISSING"));
        Mockito.when(transactionalAgendaBookingService.createTemporaryBookingLink(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.empty());
        Mockito.when(transactionalAgendaBookingService.generateBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new TransactionalAgendaBookingService.BookingLinkResult("http://localhost/reservar", false));
    }

    private AgentConversationRequest request(String message) {
        return new AgentConversationRequest(businessId, channelAccountId, conversationId, customerId,
                "+56900000000", "Cliente Test", message,
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), null, null, "trace-test", false);
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
        assertThat(second.get().primaryIntent()).isIn(AgentIntent.BOOKING_REQUEST, AgentIntent.AMBIGUOUS, AgentIntent.COMMERCIAL_INQUIRY);
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
    void bookingFlowWithAllEntitiesReturnsLink() {
        Mockito.when(transactionalAgendaBookingService.generateBookingLink(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new TransactionalAgendaBookingService.BookingLinkResult("http://localhost/reservar?token=abc", true));
        var result = coordinator.route(request("quiero agendar depilacion bozo manana a las 14"));
        assertThat(result).isPresent();
        assertThat(result.get().responseToCustomer()).contains("/reservar");
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
            return List.of(
                    new ServiceCatalogItem("FAC-LIMPIEZA", "Limpieza facial profunda", "FACIAL", 60, new java.math.BigDecimal("34990")));
        }
        @Override
        public Optional<ResponseRule> findActiveRule(UUID businessId, String code) { return Optional.empty(); }
        @Override
        public List<EntityAlias> findActiveEntityAliases(UUID businessId) { return List.of(); }
    }
}
