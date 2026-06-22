# Corrección de tests DB-driven v10.1

## Problema corregido

La versión v10 cambió los constructores de `BookingAgent` y `EntityExtractionService` para depender de `AiBusinessKnowledgeService`, porque la lógica de negocio ya no debe quedar en código duro sino apoyada en reglas, alias y catálogo consultables desde base de datos.

El test `AiAgentCoherenceTest` seguía usando constructores sin argumentos:

- `new BookingAgent()`
- `new EntityExtractionService()`

Por eso Maven fallaba durante `testCompile`.

## Corrección aplicada

Se actualizó:

`backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiAgentCoherenceTest.java`

Cambios:

1. Se agregó un `AiBusinessKnowledgeService` de prueba.
2. Se agregó un repositorio simulado `TestAiKnowledgeRepository` con la misma interfaz DB-driven usada por producción.
3. `BookingAgent` ahora se instancia con `new BookingAgent(knowledgeService)`.
4. `EntityExtractionService` ahora se instancia con `new EntityExtractionService(knowledgeService)`.
5. Se actualizó el caso de agenda para validar hora normalizada `14:00` y evitar `a las a las`.

## Comando de prueba

```powershell
cd C:\mvp\asistente-whatsapp-mvp-local-virtual-v10-1-db-driven-tests-fix\asistente-whatsapp-mvp\backend-java

.\mvnw.cmd -Dtest=AiAgentIntentCoverageSimulationTest test
```

Para ejecutar también la prueba de coherencia:

```powershell
.\mvnw.cmd -Dtest=AiAgentIntentCoverageSimulationTest,AiAgentCoherenceTest test
```
