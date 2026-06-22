# Cambios: test unitario simulado de intenciones IA

## Resumen

Se agregó una prueba unitaria que simula a un emisor real enviando preguntas representativas de todos los flujos soportados por la IA del negocio.

El emisor simulado es:

```text
56950954580
```

La prueba valida:

- pregunta enviada por el emisor;
- intención detectada;
- agente que responde;
- porcentaje de intención;
- respuesta entregada por la IA;
- cobertura completa de `AgentIntent`.

## Archivo agregado

```text
backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiAgentIntentCoverageSimulationTest.java
```

## Reporte generado por la prueba

Al ejecutar la prueba, se genera automáticamente:

```text
backend-java/target/ai-intent-simulation/intent-coverage-report.md
```

## Comando de ejecución

Desde `backend-java`:

```powershell
./mvnw -Dtest=AiAgentIntentCoverageSimulationTest test
```

## Casos simulados

| Caso | Pregunta | Intención esperada | Porcentaje mínimo |
|---|---|---|---:|
| saludo | Hola | GREETING | 74% |
| saludo social | Como estas | GREETING | 78% |
| consulta comercial | Que tipo de depilacion ofrecen | COMMERCIAL_INQUIRY | 82% |
| consulta precio | Cuanto cuesta la depilacion bozo | PRICE_REQUEST | 88% |
| cotización | Necesito una cotizacion para depilacion laser | QUOTE_REQUEST | 88% |
| agenda | Quiero agendar para manana a las 14 horas | BOOKING_REQUEST | 86% |
| venta y agenda | Quiero agendar depilacion bozo manana a las 14 horas | COMMERCIAL_AND_BOOKING | 90% |
| cambio de agenda | Necesito cambiar hora de mi cita | BOOKING_CHANGE | 90% |
| cancelación de agenda | Quiero cancelar mi cita | BOOKING_CANCEL | 90% |
| estado de agenda | Quiero confirmar mi hora | BOOKING_STATUS | 90% |
| pago | Quiero pagar mi solicitud ABCD1234 por $15000 | PAYMENT_INQUIRY | 88% |
| problema de pago | Tengo un pago duplicado y no aparece | PAYMENT_PROBLEM | 92% |
| soporte | Necesito soporte por una falla en mi cuenta | SUPPORT_GENERAL | 78% |
| mensaje técnico | docker compose up --build | TECHNICAL_MESSAGE | 91% |
| conocimiento | Quiero ver las politicas de cancelacion | KNOWLEDGE_QUERY | 82% |
| seguimiento | Quiero retomar el seguimiento que teniamos | FOLLOW_UP | 80% |
| reclamo | Estoy molesto, nadie responde y es urgente | COMPLAINT | 94% |
| humano | Quiero hablar con un ejecutivo | HUMAN_REQUEST | 96% |
| ambiguo | mmm | AMBIGUOUS | 58% |

## Criterio de éxito

La prueba falla si:

1. una pregunta se clasifica con una intención incorrecta;
2. el agente asignado no corresponde;
3. el porcentaje de intención queda bajo el mínimo esperado;
4. la IA responde vacío;
5. no se cubren todas las intenciones del enum `AgentIntent`.

## Nota técnica

El `Dockerfile` del backend actualmente compila con `-DskipTests`, por lo que esta prueba debe ejecutarse explícitamente con Maven cuando se quiera auditar la cobertura de intención.
