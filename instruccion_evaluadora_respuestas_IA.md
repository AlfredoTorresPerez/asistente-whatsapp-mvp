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
