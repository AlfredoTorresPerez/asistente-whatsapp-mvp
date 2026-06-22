# Cambios v23.4.16 - correccion de compilacion de pruebas IA

## Problema corregido

Al ejecutar `scripts/tests/run_ai_matrix_excel_tests.ps1`, Maven compilaba todas las clases de prueba y fallaba porque `SupportAgent` ya no tiene constructor sin argumentos.

Error observado:

```text
constructor SupportAgent in class com.asistentewhatsapp.aiagents.application.SupportAgent cannot be applied to given types;
required: com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository
found: no arguments
```

## Correccion aplicada

Se actualizaron las pruebas para construir `SupportAgent` con el repositorio de sedes simulado:

```java
new SupportAgent(locationRepository)
```

Archivos actualizados:

- `backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiAgentCoherenceTest.java`
- `backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiAgentIntentCoverageSimulationTest.java`
- `backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiExcelMatrixOrchestratorCoverageTest.java`

## Ejecucion

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1
```

Modo estricto:

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```

Reporte esperado:

```text
backend-java\target\ai-matrix\reporte_matriz_excel_ia_v23_4_16.md
```
