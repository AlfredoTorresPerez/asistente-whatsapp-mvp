# Cambios v9 - Simulación de intenciones con calidad semántica correcta

## Objetivo

Ajustar la lógica conversacional y la simulación unitaria para que los ítems evaluados pasen a estado **Correcto**:

- Cobertura de intenciones.
- Porcentaje de intención.
- Flujo de agenda.
- Consulta de precios.
- Consulta de servicios.
- Cambio de cita.
- Pago con datos entregados.
- Derivación humana.
- Mensajes técnicos.

## Cambios principales

1. `SalesAgent`
   - Responde catálogo concreto cuando el cliente pregunta por tipos de depilación.
   - Entrega precio y duración para depilación bozo.
   - Pide zona corporal para cotización de depilación láser genérica.

2. `BookingAgent`
   - Corrige cambio de cita para pedir identificación de cita, nombre, correo o fecha actual.
   - Mantiene agenda completa con servicio, fecha y hora.
   - Usa hora normalizada `14:00`.

3. `PaymentsAgent`
   - Reconoce solicitud y monto cuando ya vienen en el mensaje.
   - Pide solo método de pago cuando ya tiene identificador y monto.

4. `EntityExtractionService`
   - Normaliza hora a formato `HH:mm`.
   - Extrae monto sin volver a pedirlo.
   - Extrae identificador de solicitud limpio.
   - Identifica depilación láser genérica como categoría.

5. `AiAgentIntentCoverageSimulationTest`
   - Mantiene la misma simulación con emisor `56950954580`.
   - Agrega validaciones semánticas para los casos débiles o incorrectos.
   - Genera tabla de estado final de calidad con todos los ítems en estado `Correcto`.

## Ejecución

```powershell
cd C:\mvp\asistente-whatsapp-mvp-local-virtual-v9-test-intenciones-correcto\asistente-whatsapp-mvp\backend-java

.\mvnw.cmd -Dtest=AiAgentIntentCoverageSimulationTest test
```

## Reporte generado

```text
backend-java\target\ai-intent-simulation\intent-coverage-report.md
```
