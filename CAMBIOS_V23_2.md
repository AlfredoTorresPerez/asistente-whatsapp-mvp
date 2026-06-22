# Cambios V23.2 - Ajuste de coherencia IA

Esta version parte desde V23.1 y conserva todo lo incorporado en V22 y V23.

## Cambios aplicados

1. Se corrige la resolucion de servicio para que `limpieza de rostro`, `limpieza rostro`, `higiene de rostro` y `limpieza de cutis` tengan prioridad sobre `depilacion rostro` y se resuelvan como `Limpieza facial profunda`.
2. Se corrige la respuesta parcial de agenda para conservar la sucursal indicada por el cliente. Ejemplo: `Quiero reservar limpieza facial en Providencia` ahora debe responder incluyendo `en Providencia` antes de preguntar el dia.
3. Se agrega migracion `V24__fix_facial_location_and_test_rules.sql` con alias reforzados y reglas de trazabilidad.
4. Se actualiza el script de prueba V23.2 para reparar texto con mojibake UTF-8/ISO-8859-1 antes de evaluar respuestas, evitando falsos negativos por tildes o caracteres como `señal`, `día`, `qué`.

## Validacion local recomendada

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_2.ps1
```

## Casos objetivo

- `Me gustaria una limpieza de rostro esta semana` debe detectar `Limpieza facial profunda`.
- `Quiero reservar limpieza facial en Providencia` debe conservar `Providencia` en la respuesta.
- `Quiero cambiar mi hora` no debe fallar por codificacion de `dia`/`día`.
- `Tengo que pagar una senal para reservar?` no debe fallar por codificacion de `senal`/`señal`.
