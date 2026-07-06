# Cambios ejecutados - Agenda equivalente a Google Calendar

## Objetivo
Corregir la agenda completa para que use una vista semanal de 7 dias, horario 09:00-21:00, timezone America/Santiago / GMT-04 y reservas dibujadas como bloques proporcionales sobre la grilla.

## Backend

### Endpoint agregado
- `GET /api/v1/agenda/filter-options?locationId=<uuid>`
- Alias compatible: `GET /api/agenda/filter-options?locationId=<uuid>`

Retorna opciones reales para los filtros controlados de agenda:
- servicios activos
- profesionales activos
- cabinas/salas activas

### DTOs agregados
- `AgendaFilterOptionResponse`
- `AgendaFilterOptionsResponse`

### Repositorio
Se agregaron consultas en `CompleteAgendaJdbcRepository` para cargar:
- `findServiceFilterOptions(...)`
- `findProfessionalFilterOptions(...)`
- `findRoomFilterOptions(...)`

### DTO de calendario enriquecido
`AgendaCalendarItemResponse` ahora incluye campos locales para que frontend no dependa de conversiones ambiguas:
- `startsAtLocal`
- `endsAtLocal`
- `dateLocal`
- `startTimeLocal`
- `endTimeLocal`
- `timezone`
- `type`

## Frontend

### Agenda completa
Archivo modificado:
- `frontend-react/src/modules/agenda/pages/CompleteAgendaPage.tsx`

Cambios:
- Vista semanal de 7 dias, lunes a domingo.
- Horario visible extendido a 09:00-21:00.
- Indicacion visual de `GMT-04 / America/Santiago`.
- Posicionamiento de reservas con hora local de agenda.
- Agrupacion de reservas por `dateLocal` o conversion explicita `America/Santiago`.
- Reemplazo de campos libres por dropdowns para:
  - Servicio
  - Profesional
  - Cabina
- Carga de opciones desde backend usando `getAgendaFilterOptionsRequest`.
- Envio de filtros por ID: `serviceId`, `professionalId`, `roomId`.

## API Frontend
Archivo modificado:
- `frontend-react/src/services/api/completeAgendaApi.ts`

Se agrego:
- `getAgendaFilterOptionsRequest(...)`

Archivo modificado:
- `frontend-react/src/services/api/types.ts`

Se agregaron los tipos:
- `AgendaFilterOptionResponse`
- `AgendaFilterOptionsResponse`

## Verificacion recomendada

1. Reconstruir imagenes:
```powershell
docker compose -f docker-compose.local.yml build --no-cache backend-java frontend-react
```

2. Levantar ambiente:
```powershell
docker compose -f docker-compose.local.yml up -d
```

3. Validar endpoint:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/agenda/filter-options" -Headers @{ Authorization = "Bearer <TOKEN>" }
```

4. Validar UI:
- Abrir `http://localhost:5173`
- Ir a Agenda completa
- Verificar semana de 7 dias
- Verificar grilla 09:00-21:00
- Verificar dropdowns Servicio, Profesional y Cabina
- Verificar que una reserva del 19/06/2026 a las 15:00 aparezca el viernes 19 a las 15:00.

## Query SQL de diagnostico
```sql
SELECT
  b.id,
  b.status,
  b.subject,
  c.display_name AS cliente,
  bl.name AS sede,
  s.name AS servicio,
  p.full_name AS profesional,
  r.name AS sala,
  b.starts_at,
  b.ends_at,
  b.starts_at AT TIME ZONE 'America/Santiago' AS inicio_chile,
  b.ends_at AT TIME ZONE 'America/Santiago' AS fin_chile,
  b.conversation_id,
  b.source_channel
FROM booking b
LEFT JOIN customer c ON c.id = b.customer_id
LEFT JOIN business_location bl ON bl.id = b.location_id
LEFT JOIN aesthetic_service s ON s.id = b.service_id
LEFT JOIN aesthetic_professional p ON p.id = b.professional_id
LEFT JOIN agenda_room r ON r.id = b.room_id
WHERE b.starts_at < TIMESTAMPTZ '2026-06-20 00:00:00-04'
  AND coalesce(b.ends_at, b.starts_at + (b.duration_minutes || ' minutes')::interval) > TIMESTAMPTZ '2026-06-19 00:00:00-04'
ORDER BY b.starts_at;
```

## Nota de validacion
No fue posible ejecutar `pnpm build` ni Maven dentro del sandbox porque este entorno no tiene acceso de red para descargar pnpm y no tiene Maven instalado. Los cambios estan listos para validarse con Docker en el equipo local, donde el build del proyecto ya descarga dependencias durante `docker compose build`.
