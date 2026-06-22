# Cambios v23.4.18 - Tests estrictos matriz IA

## Objetivo

Corregir los fallos detectados al ejecutar:

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```

## Correcciones

1. `IntentDetectorService`
   - La palabra `asesoria` ya no se interpreta como solicitud de humano por contener `asesor`.
   - La deteccion de solicitud humana ahora usa coincidencia por palabra para terminos simples como `asesor`, `humano`, `persona`.
   - Las frases completas como `quiero hablar` siguen funcionando.

2. `IntentDetectorService`
   - Si el cliente dice `Quiero agendar <servicio> <fecha> <hora> <sede>`, la intencion queda como `BOOKING_REQUEST`.
   - Si el cliente combina precio/cotizacion con reserva, por ejemplo `Cuanto sale ... y quiero reservar ...`, se conserva `COMMERCIAL_AND_BOOKING`.

3. `AiExcelMatrixOrchestratorCoverageTest`
   - `expectedMissingData = ninguno` ahora se interpreta igual que `ninguna` y `no aplica`.
   - El reporte ahora se genera como `reporte_matriz_excel_ia_v23_4_18.md`.

4. `scripts/tests/run_ai_matrix_excel_tests.ps1`
   - Actualiza la ruta esperada del reporte a `reporte_matriz_excel_ia_v23_4_18.md`.

## Comando de validacion

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```

## Reporte esperado

```text
backend-java\target\ai-matrix\reporte_matriz_excel_ia_v23_4_18.md
```
