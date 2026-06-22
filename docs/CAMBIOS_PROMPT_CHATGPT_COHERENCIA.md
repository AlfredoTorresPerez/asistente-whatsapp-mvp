# Cambios aplicados desde prompt ChatGPT de auditoria conversacional

Este paquete aplica el prompt de auditoria de coherencia conversacional solicitado para Centro Estetico Bella.

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/domain/AgentIntent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/IntentDetectorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentRegistry.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/SupportAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/ReceptionAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentCoordinatorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`
- `backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiAgentCoherenceTest.java`

## Resultado esperado

- Mejor continuidad de agenda multi-turno.
- Menos preguntas repetidas por servicio, fecha u hora.
- Mejor tratamiento de saludos sociales.
- Mensajes tecnicos separados del flujo comercial.
- Contexto enriquecido para auditoria posterior.

## Nota de validacion

El entorno donde se genero este paquete no tiene acceso a Maven Central, por lo que no se pudo ejecutar `mvn test` localmente. El proyecto conserva compilacion Docker porque el `Dockerfile` descarga dependencias durante `docker compose up --build` en tu equipo.
