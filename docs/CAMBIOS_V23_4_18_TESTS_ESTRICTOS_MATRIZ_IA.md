# Cambios v23.4.18 - tests estrictos de matriz IA

Esta version corrige los fallos observados en la ejecucion estricta de la matriz IA v23.4.17.

## Correcciones principales

- El detector clasifica `Soy Carolina` como `GREETING` cuando el cliente solo entrega su nombre.
- `Reprogramar mi cita` ahora entra como `BOOKING_CHANGE`.
- `Tengo una reserva` ahora entra como `BOOKING_STATUS`.
- `Me cobraron doble, quiero devolucion` ahora entra como `PAYMENT_PROBLEM` y requiere derivacion humana.
- `Politica de cancelacion` ahora se trata como `KNOWLEDGE_QUERY` y no como cancelacion de reserva.
- `Retomar una cotizacion pendiente` ahora entra como `FOLLOW_UP`.
- Se agrega extraccion controlada de hora para textos como `mañana 10 Providencia` sin volver a confundir fechas tipo `viernes 12 de junio`.
- Se agrega marca funcional `fecha_u_hora_valida` para horas invalidas como `99:99`.
- Se agrega marca funcional `servicio_configurado` para solicitudes con servicio no configurado.
- Las respuestas de ubicacion ya no reportan dato faltante cuando la sucursal fue identificada.
- El reporte Markdown se escribe como UTF-8 saneado, evitando `UnmappableCharacterException` en Windows.
- La validacion estricta de precio ignora palabras de prueba como `Prueba` y valida tokens significativos del servicio.
- La validacion de enlace no exige URL para el caso de reenvio cuando el propio caso declara que depende de un flujo externo.

## Comando

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```

## Reporte esperado

```text
backend-java\target\ai-matrix\reporte_matriz_excel_ia_v23_4_18.md
```
