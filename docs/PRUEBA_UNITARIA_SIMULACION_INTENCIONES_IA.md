# Prueba unitaria simulada de intenciones de IA

Se incorporó una prueba unitaria para simular que un emisor real (`56950954580`) envía preguntas representativas de todos los flujos del asistente.

Archivo agregado:

```text
backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiAgentIntentCoverageSimulationTest.java
```

## Objetivo

Validar que el detector de intención y el agente correspondiente respondan de forma consistente para todas las intenciones soportadas por el asistente.

La prueba cubre estos tipos de intención:

- GREETING
- COMMERCIAL_INQUIRY
- PRICE_REQUEST
- QUOTE_REQUEST
- BOOKING_REQUEST
- BOOKING_CHANGE
- BOOKING_CANCEL
- BOOKING_STATUS
- PAYMENT_INQUIRY
- PAYMENT_PROBLEM
- SUPPORT_GENERAL
- TECHNICAL_MESSAGE
- KNOWLEDGE_QUERY
- FOLLOW_UP
- COMPLAINT
- HUMAN_REQUEST
- AMBIGUOUS
- COMMERCIAL_AND_BOOKING

## Qué valida

1. Que cada pregunta del emisor sea clasificada con la intención esperada.
2. Que el agente correcto atienda la intención.
3. Que la respuesta de IA no sea vacía.
4. Que el porcentaje de intención sea igual o superior al umbral esperado.
5. Que todas las intenciones del enum `AgentIntent` queden cubiertas.
6. Que se genere un reporte de auditoría en Markdown.

## Reporte generado

Al ejecutar la prueba se genera este archivo:

```text
backend-java/target/ai-intent-simulation/intent-coverage-report.md
```

El reporte incluye:

- pregunta enviada por el emisor;
- intención detectada;
- agente que respondió;
- porcentaje de intención;
- si deriva a humano;
- datos faltantes;
- respuesta entregada por la IA.

## Ejecución

Desde la carpeta `backend-java`:

```powershell
./mvnw -Dtest=AiAgentIntentCoverageSimulationTest test
```

O usando Docker desde la raíz del proyecto:

```powershell
docker compose build backend-java
```

> Nota: el `Dockerfile` compila con `-DskipTests`; para ejecutar la prueba explícitamente usa Maven local dentro de `backend-java`.
