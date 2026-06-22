# Cambios V23.3 - Ajuste quirúrgico de coherencia IA

## Objetivo

Conservar todo lo aprobado en V23.2 y corregir únicamente los pendientes detectados en el reporte de prueba:

- A04: subir confianza cuando `limpieza de rostro` se resuelve como `Limpieza facial profunda`.
- B03/F01/I01: evitar falsos negativos del script por acentos y mojibake UTF-8.

## Cambios realizados

### Backend Java

Archivo modificado:

- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java`

Cambio:

- En el flujo de agenda, cuando existe servicio resuelto y la intención es de reserva, la confianza final se ajusta al máximo entre la confianza original y la confianza determinística basada en servicio detectado.
- Esto evita que `Me gustaría una limpieza de rostro esta semana` quede con 45% cuando ya fue resuelto correctamente como `Limpieza facial profunda`.

### Base de datos

Migración agregada:

- `backend-java/src/main/resources/db/migration/V25__stabilize_facial_confidence_and_test_normalization.sql`

Incluye:

- Refuerzo de alias faciales fuertes.
- Regla `AI_CONFIDENCE_FACIAL_ALIAS_STRONG_MATCH`.
- Regla `QA_TEST_NORMALIZAR_ACENTOS_Y_UTF8`.

### Script de pruebas

Archivo agregado:

- `test_ia_negocio_conversacional_v23_3.ps1`

Cambios:

- Repara mojibake UTF-8 frecuente.
- Normaliza acentos antes de comparar textos esperados.
- Genera salida en `resultados-test-ia-v23-3`.

## No se modificó

- Prioridad de intenciones V23.
- Reenvío de enlace.
- Enlace expirado.
- Cancelación.
- Derivación humana.
- Caso sensible.
- Ubicación.
- Respeto de sucursal Providencia.
- Flujo de agenda completa.

## Validación local

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_3.ps1
```
