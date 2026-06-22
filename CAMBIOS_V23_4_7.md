# CAMBIOS V23.4.7 — Diagnóstico de disponibilidad exacta y motivo de no creación de reserva temporal

## Objetivo

La versión V23.4.7 agrega trazabilidad explícita para explicar por qué no se crea una reserva temporal cuando el flujo llega a `AVAILABILITY_CHECK_RESULT available=false`, aun cuando existan otros horarios disponibles en la agenda.

## Problema detectado

En V23.4.6 se observó el siguiente patrón en logs:

```text
AVAILABILITY_CHECK_RESULT available=false requestedTime=10:00 totalSlots=40 availableSlots=40
AVAILABILITY_ALTERNATIVES_FOUND alternatives=[mañana a las 11:00, mañana a las 11:15, mañana a las 11:30]
WHATSAPP_MESSAGE_FORMATTED type=NO_AVAILABILITY containsLink=false
```

El sistema no creó reserva temporal porque no encontró un slot disponible exactamente a las 10:00. Sin embargo, el log anterior no explicaba claramente por qué existían `availableSlots` pero no existía disponibilidad exacta para la hora solicitada.

## Cambios aplicados

### 1. Diagnóstico de slot exacto

Se agregó una validación explícita de hora exacta con logs nuevos:

- `EXACT_SLOT_VALIDATION_STARTED`
- `EXACT_SLOT_VALIDATION_RESULT`
- `EXACT_SLOT_REJECTED`

Estos logs muestran:

- hora solicitada;
- cantidad total de slots devueltos;
- cantidad de slots disponibles;
- cantidad de candidatos exactamente a la hora solicitada;
- primera hora disponible;
- última hora disponible;
- horarios cercanos;
- motivo probable del rechazo.

### 2. Motivos normalizados de rechazo

Se agregaron motivos técnicos normalizados:

- `NO_SLOTS_RETURNED`
- `NO_AVAILABLE_SLOTS_RETURNED`
- `REQUESTED_TIME_RETURNED_BUT_NOT_AVAILABLE`
- `REQUESTED_TIME_BEFORE_FIRST_AVAILABLE_SLOT_OR_OUTSIDE_HOURS`
- `REQUESTED_TIME_AFTER_LAST_AVAILABLE_SLOT_OR_OUTSIDE_HOURS`
- `REQUESTED_TIME_NOT_IN_AVAILABLE_SLOTS_POSSIBLE_CONFLICT_OR_BLOCK`
- `EXACT_SLOT_AVAILABLE`

### 3. Log `AVAILABILITY_CHECK_RESULT` más claro

Ahora `AVAILABILITY_CHECK_RESULT` incluye `exactSlotReason`, por ejemplo:

```text
AVAILABILITY_CHECK_RESULT available=false requestedTime=10:00 totalSlots=40 availableSlots=40 exactSlotReason=REQUESTED_TIME_NOT_IN_AVAILABLE_SLOTS_POSSIBLE_CONFLICT_OR_BLOCK
```

### 4. Regla de seguridad preservada

La reserva temporal solo se crea si existe un slot disponible exactamente a la hora solicitada. Si no existe, el sistema responde con alternativas y no inserta en `booking`.

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`

## Cómo validar

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
```

Luego probar:

```text
Hola, quiero reservar limpieza facial profunda mañana a las 10:00 en Providencia
```

Filtrar logs:

```powershell
docker compose -f docker-compose.local.yml logs --tail=2000 backend-java | Select-String -Pattern "EXACT_SLOT","AVAILABILITY_CHECK_RESULT","TEMPORARY_BOOKING"
```

## Resultado esperado

Si 10:00 está disponible:

```text
EXACT_SLOT_VALIDATION_RESULT available=true reason=EXACT_SLOT_AVAILABLE
TEMPORARY_BOOKING_CREATE_STARTED
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
```

Si 10:00 no está disponible:

```text
EXACT_SLOT_REJECTED available=false reason=...
AVAILABILITY_ALTERNATIVES_FOUND alternatives=[...]
WHATSAPP_MESSAGE_FORMATTED type=NO_AVAILABILITY
```

## Nota importante

Esta versión no fuerza una reserva en una hora distinta a la solicitada. Si el cliente pidió 10:00 y el sistema solo tiene 11:00, 11:15 y 11:30, debe pedir confirmación del cliente antes de crear la reserva en uno de esos horarios.
