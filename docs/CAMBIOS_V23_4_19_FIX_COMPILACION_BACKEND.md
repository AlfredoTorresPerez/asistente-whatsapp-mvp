# Cambios v23.4.19 - correccion de compilacion backend

Esta version corrige dos errores detectados al ejecutar `docker compose -f docker-compose.local.yml up -d --build --force-recreate` en la version v23.4.18.

## Correcciones

1. `EntityExtractionService.java`
   - Se agrega la constante `RELATIVE_DATE_TIME_LOCATION_PATTERN`, usada por `addTimeIfFound` para interpretar mensajes con fecha relativa y hora.
   - Esto corrige el error: `cannot find symbol variable RELATIVE_DATE_TIME_LOCATION_PATTERN`.

2. `SupportAgent.java`
   - `locationResponse(...)` ahora retorna `LocationAnswer`, no `String`.
   - Esto corrige el error: `java.lang.String cannot be converted to SupportAgent.LocationAnswer`.

3. Reporte de matriz IA
   - Se actualiza la ruta esperada del reporte a `backend-java\target\ai-matrix\reporte_matriz_excel_ia_v23_4_19.md`.

## Comando de ejecucion

```powershell
docker compose -f docker-compose.local.yml up -d --build --force-recreate
```

## Comando de pruebas estrictas

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```
