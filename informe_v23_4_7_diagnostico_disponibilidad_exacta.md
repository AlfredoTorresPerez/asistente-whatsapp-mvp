# Informe técnico V23.4.7 — Diagnóstico de disponibilidad exacta

## 1. Problema detectado

En V23.4.6 el flujo de agenda podía resolver correctamente:

- servicio;
- sucursal;
- fecha;
- hora;
- disponibilidad general.

Sin embargo, en algunos casos no se creaba la reserva temporal porque el sistema concluía:

```text
AVAILABILITY_CHECK_RESULT available=false requestedTime=10:00 totalSlots=40 availableSlots=40
```

La lectura superficial de ese log era confusa, porque `availableSlots=40` parecía indicar disponibilidad general, pero `available=false` indicaba que la hora exacta solicitada no estaba disponible.

## 2. Causa raíz funcional

El flujo solo crea reserva temporal si existe un slot disponible exactamente en la hora solicitada por el cliente.

Ejemplo:

```text
Cliente pide: 10:00
Agenda devuelve: 11:00, 11:15, 11:30...
Resultado: no se crea reserva temporal
```

Esto es correcto desde el punto de vista transaccional: no se debe crear una reserva a una hora distinta sin confirmación explícita del cliente.

## 3. Limitación anterior

Antes de V23.4.7 no existían logs suficientes para distinguir entre:

- agenda sin ningún cupo;
- servicio sin relación con la sucursal;
- hora exacta ocupada;
- hora fuera de horario;
- slots disponibles solo en otros horarios;
- posible conflicto con una reserva temporal previa.

## 4. Solución aplicada

Se instrumentó la validación exacta del slot solicitado.

Nuevos logs:

```text
EXACT_SLOT_VALIDATION_STARTED
EXACT_SLOT_VALIDATION_RESULT
EXACT_SLOT_REJECTED
```

También se agregó `exactSlotReason` al log:

```text
AVAILABILITY_CHECK_RESULT
```

## 5. Ejemplo de diagnóstico esperado

Caso sin disponibilidad exacta:

```text
EXACT_SLOT_VALIDATION_STARTED requestedTime=10:00 totalSlots=40 availableSlots=40 exactCandidates=0 firstAvailable=11:00 lastAvailable=18:45
EXACT_SLOT_REJECTED available=false requestedTime=10:00 reason=REQUESTED_TIME_NOT_IN_AVAILABLE_SLOTS_POSSIBLE_CONFLICT_OR_BLOCK nearestAvailable=[11:00, 11:15, 11:30]
AVAILABILITY_CHECK_RESULT available=false requestedTime=10:00 totalSlots=40 availableSlots=40 exactSlotReason=REQUESTED_TIME_NOT_IN_AVAILABLE_SLOTS_POSSIBLE_CONFLICT_OR_BLOCK
```

Caso con disponibilidad exacta:

```text
EXACT_SLOT_VALIDATION_STARTED requestedTime=10:00 totalSlots=40 availableSlots=40 exactCandidates=1 firstAvailable=09:00 lastAvailable=18:45
EXACT_SLOT_VALIDATION_RESULT available=true requestedTime=10:00 reason=EXACT_SLOT_AVAILABLE
TEMPORARY_BOOKING_CREATE_STARTED
TEMPORARY_BOOKING_CREATED
CONFIRMATION_LINK_CREATED
```

## 6. Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`

## 7. No se cambió

No se modificó:

- lógica de creación de reserva;
- generación de token;
- confirmación de enlace;
- reglas de sucursal;
- relación servicio-sucursal;
- flujo `dryRun`;
- formato WhatsApp agregado en V23.4.6.

## 8. Cómo ejecutar

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
```

## 9. Cómo revisar logs

```powershell
docker compose -f docker-compose.local.yml logs --tail=2000 backend-java | Select-String -Pattern "EXACT_SLOT","AVAILABILITY_CHECK_RESULT","AVAILABILITY_ALTERNATIVES_FOUND","TEMPORARY_BOOKING","CONFIRMATION_LINK","FLOW_ERROR"
```

## 10. Riesgos

El cambio agrega logs adicionales. Si se procesan muchos mensajes, puede aumentar el volumen de consola.

## 11. Limitaciones

Esta versión explica por qué no se creó la reserva temporal, pero no fuerza una reserva en una hora alternativa. Esa decisión debe ser confirmada por el cliente.

## 12. Validación pendiente

No se pudo ejecutar Maven ni Docker Compose en este entorno porque el wrapper de Maven requiere descargar dependencias externas. La validación final debe realizarse en el entorno local del proyecto.
