package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiAgentIntentCoverageSimulationTest {

    private static final UUID BUSINESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHANNEL_ACCOUNT_ID = UUID.fromString("69500000-0000-0000-0000-000000000001");
    private static final UUID CONVERSATION_ID = UUID.fromString("888141fb-e8fe-4752-b5bd-d5af9dbb60ea");
    private static final UUID CUSTOMER_ID = UUID.fromString("b7ee64b6-4cb8-4d79-86a8-7eecff947c6d");
    private static final String CUSTOMER_PHONE = "56950954580";
    private static final String CUSTOMER_NAME = "Cliente simulador";

    private final IntentDetectorService detector = new IntentDetectorService();
    private final AiBusinessKnowledgeService knowledgeService = new AiBusinessKnowledgeService(new TestAiKnowledgeRepository());
    private final BusinessLocationJdbcRepository locationRepository = emptyLocationRepository();
    private final TransactionalAgendaBookingService transactionalAgendaBookingService = emptyTransactionalAgendaBookingService();
    private final EntityExtractionService extractor = new EntityExtractionService(knowledgeService, locationRepository);
    private final AgentRegistry registry = new AgentRegistry(List.of(
            new ReceptionAgent(),
            new SalesAgent(knowledgeService),
            new BookingAgent(knowledgeService, locationRepository, transactionalAgendaBookingService),
            new PaymentsAgent(knowledgeService),
            new SupportAgent(locationRepository),
            new KnowledgeAgent(),
            new FollowUpAgent(),
            new HumanHandoffAgent()));

    private BusinessLocationJdbcRepository emptyLocationRepository() {
        BusinessLocationJdbcRepository repository = Mockito.mock(BusinessLocationJdbcRepository.class);
        Mockito.when(repository.findActive(Mockito.any())).thenReturn(List.of());
        Mockito.when(repository.countActive(Mockito.any())).thenReturn(0L);
        return repository;
    }

    private TransactionalAgendaBookingService emptyTransactionalAgendaBookingService() {
        TransactionalAgendaBookingService service = Mockito.mock(TransactionalAgendaBookingService.class);
        Mockito.when(service.resolveEffectiveLocation(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(new TransactionalAgendaBookingService.ResolvedLocation(null, "MISSING"));
        Mockito.when(service.createTemporaryBookingLink(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.anyBoolean(),
                        Mockito.anyBoolean(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(Optional.empty());
        return service;
    }

    @Test
    void simulatedSenderAsksAllSupportedQuestionsAndReceivesAiResponsesWithIntentPercentages() throws IOException {
        List<Scenario> scenarios = List.of(
                scenario("saludo", "Hola", AgentIntent.GREETING, AgentType.RECEPTION, 74),
                scenario("saludo social", "Como estas", AgentIntent.GREETING, AgentType.RECEPTION, 78),
                scenario("consulta comercial", "Que tipo de depilacion ofrecen", AgentIntent.COMMERCIAL_INQUIRY, AgentType.SALES, 82),
                scenario("consulta precio", "Cuanto cuesta la depilacion bozo", AgentIntent.PRICE_REQUEST, AgentType.SALES, 88),
                scenario("cotizacion", "Necesito una cotizacion para depilacion laser", AgentIntent.QUOTE_REQUEST, AgentType.SALES, 88),
                scenario("agenda", "Quiero agendar para manana a las 14 horas", AgentIntent.BOOKING_REQUEST, AgentType.BOOKING, 86),
                scenario("venta y agenda", "Quiero agendar depilacion bozo manana a las 14 horas", AgentIntent.COMMERCIAL_AND_BOOKING, AgentType.BOOKING, 90),
                scenario("agenda facial con dia", "Hola, me gustaria reservar una limpieza facial para el viernes", AgentIntent.COMMERCIAL_AND_BOOKING, AgentType.BOOKING, 90),
                scenario("cambio de agenda", "Necesito cambiar hora de mi cita", AgentIntent.BOOKING_CHANGE, AgentType.BOOKING, 90),
                scenario("cancelacion de agenda", "Quiero cancelar mi cita", AgentIntent.BOOKING_CANCEL, AgentType.BOOKING, 90),
                scenario("estado de agenda", "Quiero confirmar mi hora", AgentIntent.BOOKING_STATUS, AgentType.BOOKING, 90),
                scenario("pago", "Quiero pagar mi solicitud ABCD1234 por $15000", AgentIntent.PAYMENT_INQUIRY, AgentType.PAYMENTS, 88),
                scenario("problema de pago", "Tengo un pago duplicado y no aparece", AgentIntent.PAYMENT_PROBLEM, AgentType.HUMAN_HANDOFF, 92),
                scenario("soporte", "Necesito soporte por una falla en mi cuenta", AgentIntent.SUPPORT_GENERAL, AgentType.SUPPORT, 78),
                scenario("mensaje tecnico", "docker compose up --build", AgentIntent.TECHNICAL_MESSAGE, AgentType.SUPPORT, 91),
                scenario("conocimiento", "Quiero ver las politicas de cancelacion", AgentIntent.KNOWLEDGE_QUERY, AgentType.KNOWLEDGE, 82),
                scenario("seguimiento", "Quiero retomar el seguimiento que teniamos", AgentIntent.FOLLOW_UP, AgentType.FOLLOW_UP, 80),
                scenario("reclamo", "Estoy molesto, nadie responde y es urgente", AgentIntent.COMPLAINT, AgentType.HUMAN_HANDOFF, 94),
                scenario("humano", "Quiero hablar con un ejecutivo", AgentIntent.HUMAN_REQUEST, AgentType.HUMAN_HANDOFF, 96),
                scenario("ambiguo", "mmm", AgentIntent.AMBIGUOUS, AgentType.RECEPTION, 58));

        List<SimulationRow> rows = new ArrayList<>();
        Set<AgentIntent> coveredIntents = EnumSet.noneOf(AgentIntent.class);

        for (Scenario scenario : scenarios) {
            AgentConversationRequest request = request(scenario.message());
            IntentDetectionResult detectedIntent = detector.detect(request);
            Map<String, String> entities = new LinkedHashMap<>(extractor.extract(request));
            AgentHandler handler = registry.resolve(detectedIntent);
            AgentRoutingResult result = handler.handle(request, detectedIntent, entities, List.of());
            int intentPercentage = (int) Math.round(detectedIntent.confidence() * 100.0d);

            assertThat(detectedIntent.primaryIntent()).as(scenario.name()).isEqualTo(scenario.expectedIntent());
            assertThat(result.agentType()).as(scenario.name()).isEqualTo(scenario.expectedAgent());
            assertThat(intentPercentage).as(scenario.name()).isGreaterThanOrEqualTo(scenario.minimumPercentage());
            assertThat(result.responseToCustomer()).as(scenario.name()).isNotBlank();
            assertQualityRules(scenario, result);

            coveredIntents.add(detectedIntent.primaryIntent());
            rows.add(new SimulationRow(
                    scenario.name(),
                    CUSTOMER_PHONE,
                    scenario.message(),
                    detectedIntent.primaryIntent(),
                    result.agentType(),
                    intentPercentage,
                    result.requiresHuman(),
                    result.missingData(),
                    result.responseToCustomer()));
        }

        assertThat(coveredIntents).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(AgentIntent.class));
        writeMarkdownReport(rows, coveredIntents.size(), AgentIntent.values().length);
    }

    private Scenario scenario(
            String name,
            String message,
            AgentIntent expectedIntent,
            AgentType expectedAgent,
            int minimumPercentage) {
        return new Scenario(name, message, expectedIntent, expectedAgent, minimumPercentage);
    }

    private AgentConversationRequest request(String body) {
        return new AgentConversationRequest(
                BUSINESS_ID,
                CHANNEL_ACCOUNT_ID,
                CONVERSATION_ID,
                CUSTOMER_ID,
                CUSTOMER_PHONE,
                CUSTOMER_NAME,
                body,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void assertQualityRules(Scenario scenario, AgentRoutingResult result) {
        String response = result.responseToCustomer().toLowerCase(java.util.Locale.ROOT);
        String caseName = scenario.name();

        if ("consulta comercial".equals(caseName)) {
            assertThat(response).contains("bozo", "rostro", "axilas", "cera", "láser");
            assertThat(response).doesNotContain("catálogo vigente");
        }
        if ("consulta precio".equals(caseName)) {
            assertThat(response).contains("$8.990", "15 minutos");
            assertThat(response).doesNotContain("quieres que revise precio");
        }
        if ("cotizacion".equals(caseName)) {
            assertThat(response).contains("depilación láser");
            assertThat(response).contains("zona");
        }
        if ("venta y agenda".equals(caseName)) {
            assertThat(response).contains("depilación bozo", "mañana", "14:00");
            assertThat(response).doesNotContain("a las a las");
            assertThat(response).doesNotContain("qué servicio quieres agendar");
        }
        if ("agenda facial con dia".equals(caseName)) {
            assertThat(response).contains("limpieza facial", "viernes", "horario");
            assertThat(response).doesNotContain("servicio específico");
        }
        if ("cambio de agenda".equals(caseName)) {
            assertThat(response).containsAnyOf("nombre", "correo", "fecha de la cita", "cita actual");
            assertThat(response).doesNotContain("servicio específico");
        }
        if ("pago".equals(caseName)) {
            assertThat(response).contains("abcd1234", "$15.000", "método de pago");
            assertThat(response).doesNotContain("me indicas el monto");
        }
        if ("mensaje tecnico".equals(caseName)) {
            assertThat(response).contains("mensaje parece técnico");
            assertThat(response).contains("servicios", "precios", "agenda");
        }
        if ("reclamo".equals(caseName) || "humano".equals(caseName) || "problema de pago".equals(caseName)) {
            assertThat(result.requiresHuman()).isTrue();
        }
    }

    private void writeMarkdownReport(List<SimulationRow> rows, int covered, int total) throws IOException {
        Path output = Path.of("target", "ai-intent-simulation", "intent-coverage-report.md");
        Files.createDirectories(output.getParent());

        double coverage = total == 0 ? 0.0d : (covered * 100.0d / total);
        StringBuilder report = new StringBuilder();
        report.append("# Simulacion unitaria de intenciones IA\n\n");
        report.append("Emisor simulado: ").append(CUSTOMER_PHONE).append("\n\n");
        report.append("Cobertura de intenciones: ")
                .append(String.format(java.util.Locale.ROOT, "%.2f", coverage))
                .append("% (").append(covered).append("/").append(total).append(")\n\n");
        report.append("## Estado final de calidad\n\n");
        report.append("| Ítem | Estado anterior | Estado nuevo | Evidencia |\n");
        report.append("|---|---|---|---|\n");
        report.append("| Cobertura de intenciones | Correcta | Correcta | ").append(covered).append("/").append(total).append(" |\n");
        report.append("| Porcentaje de intención | Correcto para auditoría | Correcto | porcentaje informado por caso |\n");
        report.append("| Flujo de agenda | Parcialmente correcto | Correcto | no repite servicio y normaliza hora a 14:00 |\n");
        report.append("| Consulta de precios | Débil | Correcta | entrega $8.990 y 15 minutos para depilación bozo |\n");
        report.append("| Consulta de servicios | Débil | Correcta | lista depilación bozo, rostro, axilas, cera y láser |\n");
        report.append("| Cambio de cita | Incorrecto | Correcto | pide identificación de cita, nombre, correo o fecha actual |\n");
        report.append("| Pago con datos entregados | Incorrecto | Correcto | reconoce ABCD1234 y $15.000, pide solo método de pago |\n");
        report.append("| Derivación humana | Correcta | Correcta | deriva reclamo, humano y problema de pago |\n");
        report.append("| Mensajes técnicos | Correcta | Correcta | detecta docker y no activa venta |\n\n");
        report.append("| Caso | Pregunta del emisor | Intencion | Agente | Porcentaje intencion | Deriva humano | Datos faltantes | Respuesta IA |\n");
        report.append("|---|---|---|---|---:|---|---|---|\n");
        for (SimulationRow row : rows) {
            report.append("| ").append(escape(row.caseName()))
                    .append(" | ").append(escape(row.senderMessage()))
                    .append(" | ").append(row.intent())
                    .append(" | ").append(row.agentType())
                    .append(" | ").append(row.intentPercentage()).append("%")
                    .append(" | ").append(row.requiresHuman() ? "Si" : "No")
                    .append(" | ").append(escape(String.join(", ", row.missingData())))
                    .append(" | ").append(escape(row.aiResponse()))
                    .append(" |\n");
        }
        Files.writeString(output, report.toString(), StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replace("|", "\\|").replace("\n", " ").trim();
    }

    private record Scenario(
            String name,
            String message,
            AgentIntent expectedIntent,
            AgentType expectedAgent,
            int minimumPercentage) {
    }

    private record SimulationRow(
            String caseName,
            String senderPhone,
            String senderMessage,
            AgentIntent intent,
            AgentType agentType,
            int intentPercentage,
            boolean requiresHuman,
            List<String> missingData,
            String aiResponse) {
    }

    private static class TestAiKnowledgeRepository implements AiKnowledgeRepository {
        @Override
        public List<ServiceCatalogItem> findActiveServices(UUID businessId) {
            return List.of(
                    new ServiceCatalogItem("DEP-BOZO", "Depilacion bozo", "DEPILACION", 15, new BigDecimal("8990")),
                    new ServiceCatalogItem("DEP-CERA", "Depilacion con cera", "DEPILACION", 30, new BigDecimal("15990")),
                    new ServiceCatalogItem("DEP-ROSTRO", "Depilacion rostro", "DEPILACION", 30, new BigDecimal("18990")),
                    new ServiceCatalogItem("DEP-AXILAS", "Depilacion axilas", "DEPILACION", 25, new BigDecimal("19990")),
                    new ServiceCatalogItem("DEP-LASER", "Depilacion laser", "DEPILACION", 30, new BigDecimal("24990")),
                    new ServiceCatalogItem("DEP-PIERNAS", "Depilacion piernas", "DEPILACION", 45, new BigDecimal("29990")),
                    new ServiceCatalogItem("FAC-LIMPIEZA", "Limpieza facial profunda", "FACIAL", 60, new BigDecimal("34990")));
        }

        @Override
        public java.util.Optional<ResponseRule> findActiveRule(UUID businessId, String code) {
            return switch (code) {
                case "AI_DEPILATION_CATALOG_RESPONSE" -> java.util.Optional.of(rule(code, "Tenemos {services}. ¿Cuál quieres revisar?", Map.of("labels", List.of("depilación bozo", "rostro", "axilas", "cera", "láser"))));
                case "AI_PRICE_KNOWN_SERVICE_RESPONSE" -> java.util.Optional.of(rule(code, "El valor base de {service} es {price} y dura aproximadamente {duration} minutos. ¿Quieres agendar una hora?", Map.of()));
                case "AI_PRICE_UNKNOWN_SERVICE_RESPONSE" -> java.util.Optional.of(rule(code, "Para darte un precio correcto, ¿me indicas el servicio exacto que quieres revisar?", Map.of()));
                case "AI_QUOTE_MISSING_DETAIL_RESPONSE" -> java.util.Optional.of(rule(code, "Puedo ayudarte con la cotización de {category}. ¿Qué zona quieres cotizar: {options}?", Map.of("options", List.of("rostro", "axilas", "u otra"))));
                case "AI_BOOKING_MISSING_SERVICE_RESPONSE" -> java.util.Optional.of(rule(code, "Perfecto. Para revisar disponibilidad necesito el servicio específico. Por ejemplo: {examples}.", Map.of("examples", List.of("depilación bozo", "rostro", "axilas", "piernas", "bikini"))));
                case "AI_BOOKING_MISSING_DATE_RESPONSE" -> java.util.Optional.of(rule(code, "Perfecto, reviso {service}. ¿Para qué día quieres agendar?", Map.of()));
                case "AI_BOOKING_MISSING_TIME_RESPONSE" -> java.util.Optional.of(rule(code, "Perfecto, reviso {service} para {date}. ¿Qué horario te acomoda?", Map.of()));
                case "AI_BOOKING_COMPLETE_RESPONSE" -> java.util.Optional.of(rule(code, "Perfecto. Tengo {service} para {date} a las {time}. Debo validar disponibilidad real en agenda antes de confirmar. ¿Quieres que revise esa hora?", Map.of()));
                case "AI_BOOKING_CHANGE_IDENTIFY_RESPONSE" -> java.util.Optional.of(rule(code, "Claro. Para ayudarte a cambiar la hora, ¿me indicas tu nombre, correo o la fecha de la cita actual?", Map.of()));
                case "AI_BOOKING_CANCEL_IDENTIFY_RESPONSE" -> java.util.Optional.of(rule(code, "Claro. Para revisar tu cancelación, ¿me indicas el nombre o número asociado a la cita?", Map.of()));
                case "AI_BOOKING_STATUS_IDENTIFY_RESPONSE" -> java.util.Optional.of(rule(code, "Puedo ayudarte a revisar tus reservas, pero debo validarlo en agenda. ¿Qué fecha o mes quieres revisar?", Map.of()));
                case "AI_PAYMENT_REQUEST_AMOUNT_RESPONSE" -> java.util.Optional.of(rule(code, "Perfecto. Tengo la solicitud {requestNumber} por {amount}. ¿Qué método de pago quieres usar?", Map.of()));
                case "AI_PAYMENT_MISSING_AMOUNT_RESPONSE" -> java.util.Optional.of(rule(code, "Gracias. Tengo la solicitud {requestNumber}. ¿Me indicas el monto y método de pago?", Map.of()));
                case "AI_PAYMENT_MISSING_REQUEST_RESPONSE" -> java.util.Optional.of(rule(code, "Gracias. Para revisar el pago, ¿me indicas el número de pedido o solicitud?", Map.of()));
                case "AI_SALES_MISSING_SERVICE_RESPONSE" -> java.util.Optional.of(rule(code, "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás buscando exactamente?", Map.of()));
                case "AI_SALES_NEXT_STEP_RESPONSE" -> java.util.Optional.of(rule(code, "Puedo orientarte con {service}. ¿Quieres revisar precio, características o agendar una atención?", Map.of()));
                case "AI_GENERIC_NEXT_STEP" -> java.util.Optional.of(rule(code, "¿Qué necesitas revisar hoy?", Map.of()));
                default -> java.util.Optional.empty();
            };
        }

        @Override
        public List<EntityAlias> findActiveEntityAliases(UUID businessId) {
            return List.of(
                    new EntityAlias("depilacion bozo", "servicio_o_producto", "Depilacion bozo", 300),
                    new EntityAlias("bozo", "servicio_o_producto", "Depilacion bozo", 290),
                    new EntityAlias("depilacion laser", "categoria_servicio", "depilación láser", 280),
                    new EntityAlias("depilacion axilas", "servicio_o_producto", "Depilacion axilas", 270),
                    new EntityAlias("depilacion rostro", "servicio_o_producto", "Depilacion rostro", 260),
                    new EntityAlias("depilacion facial", "servicio_o_producto", "Depilacion rostro", 250),
                    new EntityAlias("depilacion con cera", "servicio_o_producto", "Depilacion con cera", 240),
                    new EntityAlias("manana", "fecha_relativa", "mañana", 100),
                    new EntityAlias("hoy", "fecha_relativa", "hoy", 100),
                    new EntityAlias("esta semana", "fecha_relativa", "esta semana", 100));
        }

        private ResponseRule rule(String code, String template, Map<String, Object> payload) {
            return new ResponseRule(code, template, payload);
        }
    }

}
