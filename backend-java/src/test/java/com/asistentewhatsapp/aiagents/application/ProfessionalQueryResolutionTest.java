package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.agenda.api.AgendaFilterOptionResponse;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * FASE 07 — Consultas de profesionales.
 *
 * Verifica que las consultas de profesionales se resuelvan contra datos reales
 * / deterministas: no se inventan profesionales ni disponibilidad. Los nombres
 * se validan contra el catálogo activo del negocio; los nombres no registrados
 * se marcan como profesional_no_encontrado y no se atribuyen horarios a
 * profesionales inexistentes.
 */
class ProfessionalQueryResolutionTest {

	private static final UUID BUSINESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CARLA_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");
	private static final UUID VALENTINA_ID = UUID.fromString("a2000000-0000-0000-0000-000000000002");
	private static final UUID PROV_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");

	private final CompleteAgendaJdbcRepository agendaRepository = Mockito.mock(CompleteAgendaJdbcRepository.class);
	private final ProfessionalCatalogService professionalCatalogService = new ProfessionalCatalogService(
			agendaRepository);
	private final AiBusinessKnowledgeService knowledgeService = new AiBusinessKnowledgeService(new TestKnowledgeRepo());
	private final BusinessLocationJdbcRepository locationRepository = provinciaLocationRepository();
	private final TransactionalAgendaBookingService transactional = Mockito
			.mock(TransactionalAgendaBookingService.class);

	private EntityExtractionService extractor;
	private BookingAgent bookingAgent;

	@BeforeEach
	void setUp() {
		Mockito.reset(agendaRepository);
		Mockito.when(agendaRepository.findProfessionalFilterOptions(BUSINESS_ID, null))
				.thenReturn(List.of(
						new AgendaFilterOptionResponse(CARLA_ID, "Carla Mendez",
								"Cosmetologia facial y evaluacion estetica", PROV_ID, true),
						new AgendaFilterOptionResponse(VALENTINA_ID, "Valentina Rios",
								"Tratamientos corporales y masoterapia", PROV_ID, true),
						new AgendaFilterOptionResponse(UUID.randomUUID(), "Daniela Soto",
								"Depilacion, cejas y pestanas", PROV_ID, true)));
		Mockito.when(transactional.resolveEffectiveLocation(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(new TransactionalAgendaBookingService.ResolvedLocation(null, "MISSING"));
		Mockito.when(transactional.generateBookingLink(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(
						new TransactionalAgendaBookingService.BookingLinkResult("http://localhost/reservar", false));
		Mockito.when(transactional.checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(Optional.empty());
		extractor = new EntityExtractionService(knowledgeService, locationRepository, null, professionalCatalogService);
		bookingAgent = new BookingAgent(knowledgeService, transactional,
				Mockito.mock(com.asistentewhatsapp.bookings.infrastructure.BookingConfirmationJdbcRepository.class),
				Mockito.mock(com.asistentewhatsapp.bookings.application.BookingConfirmationService.class),
				agendaRepository, professionalCatalogService);
	}

	private AgentConversationRequest request(String message) {
		return new AgentConversationRequest(BUSINESS_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				"+56900000000", "Cliente Test", message, OffsetDateTime.now(ZoneOffset.UTC), null, null, "trace-test",
				false);
	}

	@Test
	void professionalEspecificoConfirmadoSeExtraeDelCatalogo() {
		Map<String, String> entities = extractor.extract(request("¿Quiero agendar con Carla Mendez?"));
		assertThat(entities).containsEntry("profesional", "Carla Mendez");
		assertThat(entities).doesNotContainKey("profesional_no_encontrado");
	}

	@Test
	void profesionalNoEncontradoNoSeInventaYSeMarca() {
		Map<String, String> entities = extractor.extract(request("¿Tienen disponibilidad con María?"));
		assertThat(entities).containsKey("profesional_no_encontrado");
		assertThat(entities).doesNotContainKey("profesional");
	}

	@Test
	void nombreComunSinContextoProfesionalNoSeExtraeComoProfesional() {
		Map<String, String> entities = extractor.extract(request("Hola María"));
		assertThat(entities).doesNotContainKey("profesional");
		assertThat(entities).doesNotContainKey("profesional_no_encontrado");
	}

	@Test
	void tituloGenericoDoctorNoSeExtraeComoProfesional() {
		Map<String, String> entities = extractor.extract(request("el doctor"));
		assertThat(entities).doesNotContainKey("profesional");
		assertThat(entities).doesNotContainKey("profesional_no_encontrado");
	}

	@Test
	void nombreDeSucursalNoSeConfundeConProfesional() {
		Map<String, String> entities = extractor.extract(request("¿En Providencia trabaja la persona de láser?"));
		assertThat(entities).doesNotContainKey("profesional");
		assertThat(entities).doesNotContainKey("profesional_no_encontrado");
	}

	@Test
	void catalogoResuelveProfesionalesActivosPorEspecialidad() {
		List<ProfessionalCatalogService.ProfessionalInfo> bySpecialty = professionalCatalogService
				.findBySpecialty(BUSINESS_ID, "tratamientos corporales");
		assertThat(bySpecialty).map(ProfessionalCatalogService.ProfessionalInfo::name)
				.containsExactly("Valentina Rios");
		assertThat(professionalCatalogService.isActiveProfessional(BUSINESS_ID, "Carla mendez")).isTrue();
		assertThat(professionalCatalogService.isActiveProfessional(BUSINESS_ID, "María")).isFalse();
	}

	@Test
	void handleProfessionalConfirmadoPreguntaServicioYDiaSinInventarDisponibilidad() {
		Map<String, String> entities = new LinkedHashMap<>();
		entities.put("profesional", "Carla Mendez");
		AgentRoutingResult result = bookingAgent.handle(request("¿con Carla Mendez?"),
				new IntentDetectionResult(AgentIntent.PROFESSIONAL_QUERY, null, 0.86, "bajo", false, null), entities,
				List.of());
		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.PROFESSIONAL_QUERY);
		assertThat(result.responseToCustomer()).contains("Carla Mendez");
		assertThat(result.responseToCustomer()).contains("servicio");
		assertThat(result.responseToCustomer()).contains("día");
		assertThat(result.missingData()).contains("servicio_o_producto");
		Mockito.verifyNoInteractions(transactional);
	}

	@Test
	void handleProfessionalNoEncontradoPreguntaServicioYDiaYNoReservaBajoNombreInventado() {
		Map<String, String> entities = new LinkedHashMap<>();
		entities.put("profesional_no_encontrado", "María");
		AgentRoutingResult result = bookingAgent.handle(request("¿con María?"),
				new IntentDetectionResult(AgentIntent.PROFESSIONAL_QUERY, null, 0.86, "bajo", false, null), entities,
				List.of());
		assertThat(result.responseToCustomer()).contains("María");
		assertThat(result.responseToCustomer()).contains("servicio");
		assertThat(result.responseToCustomer()).contains("día");
	}

	@Test
	void handleProfessionalGenericoListaProfesionalesReales() {
		Map<String, String> entities = new LinkedHashMap<>();
		AgentRoutingResult result = bookingAgent.handle(request("¿quiénes atienden?"),
				new IntentDetectionResult(AgentIntent.PROFESSIONAL_QUERY, null, 0.86, "bajo", false, null), entities,
				List.of());
		assertThat(result.responseToCustomer()).contains("Carla Mendez");
		assertThat(result.responseToCustomer()).contains("Valentina Rios");
	}

	@Test
	void handleAvailabilityWithProfessionalConsultaDisponibilidadConProfessionalIdConfirmado() {
		Mockito.when(transactional.checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(UUID.class), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(Optional.of("1. 10:00 con Carla Mendez"));
		Map<String, String> entities = new LinkedHashMap<>();
		entities.put("profesional", "Carla Mendez");
		entities.put("servicio_o_producto", "Hidratacion facial");
		entities.put("sede", "Providencia");
		entities.put("fecha_relativa", "mañana");
		AgentRoutingResult result = bookingAgent.handle(request("Hidratación facial mañana con Carla"),
				new IntentDetectionResult(AgentIntent.AVAILABILITY_QUERY, null, 0.91, "bajo", false, null), entities,
				List.of());
		assertThat(result.responseToCustomer()).contains("10:00");
		assertThat(result.responseToCustomer()).contains("Carla Mendez");
		Mockito.verify(transactional).checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(CARLA_ID), Mockito.any(), Mockito.any(),
				Mockito.any());
	}

	@Test
	void nombreNoEnCatalogoNoSeAtribuyeProfessionalIdAlDisponibilidad() {
		Map<String, String> entities = new LinkedHashMap<>();
		entities.put("profesional_no_encontrado", "María");
		entities.put("servicio_o_producto", "Hidratacion facial");
		entities.put("sede", "Providencia");
		entities.put("fecha_relativa", "mañana");
		AgentRoutingResult result = bookingAgent.handle(request("Hidratación facial mañana con María"),
				new IntentDetectionResult(AgentIntent.AVAILABILITY_QUERY, null, 0.91, "bajo", false, null), entities,
				List.of());
		assertThat(result.responseToCustomer()).doesNotContain("disponibilidad con María");
		Mockito.verify(transactional, Mockito.never()).checkAvailability(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(UUID.class), Mockito.any(),
				Mockito.any(), Mockito.any());
	}

	private BusinessLocationJdbcRepository provinciaLocationRepository() {
		BusinessLocationJdbcRepository repo = Mockito.mock(BusinessLocationJdbcRepository.class);
		BusinessLocationJdbcRepository.BusinessLocationRecord providencia = new BusinessLocationJdbcRepository.BusinessLocationRecord(
				PROV_ID, BUSINESS_ID, "PROVIDENCIA", "Providencia", "Av. Providencia 2450", "Santiago", "Providencia",
				"+56955550100", null, "America/Santiago", null, null, null, true, OffsetDateTime.now(),
				OffsetDateTime.now());
		Mockito.when(repo.findActive(Mockito.any())).thenReturn(List.of(providencia));
		Mockito.when(repo.countActive(Mockito.any())).thenReturn(1L);
		return repo;
	}

	private static class TestKnowledgeRepo implements AiKnowledgeRepository {
		@Override
		public List<ServiceCatalogItem> findActiveServices(UUID businessId) {
			return List.of(new ServiceCatalogItem("FAC-HIDRATACION", "Hidratacion facial", "FACIAL", 45,
					new BigDecimal("29990")));
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
