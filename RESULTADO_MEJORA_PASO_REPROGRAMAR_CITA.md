# Resultado: Simplificación de la pantalla "Reprogramar cita" (admin)

**Fecha:** 2026-08-01
**Alcance:** `RescheduleAppointmentPage` (ruta `/appointments/:appointmentId/reschedule`) y
`BookingService.reschedule` (backend)
**Estado:** Implementado y validado (frontend 173 tests, build OK, e2e 2/2, flujo API en vivo 200/409)

---

## 1. Resumen ejecutivo

Se rediseñó la pantalla admin de reprogramación de citas para que muestre **solo** un selector
de fecha ("Selecciona una nueva fecha"), la grilla de horarios disponibles reales y los botones
**Cancelar** / **Guardar nueva fecha**. Se eliminaron la duración, la sucursal, la ubicación
complementaria, las notas de reprogramación, el campo combinado de fecha/hora y el ingreso
manual de hora. La disponibilidad proviene del endpoint real `/agenda/availability`, los
horarios se ordenan ascendentemente (Java + defensivo en React), el horario seleccionado se
destaca con borde/fondo/indicador (no solo color) y al guardar el frontend envía únicamente
`{ startsAt }`; el backend conserva duración, sucursal y notas originales y revalida el slot
antes de confirmar (409 `BOOKING_SLOT_NOT_AVAILABLE`).

## 2. Diagnóstico inicial

Existían **dos** pantallas de reprogramación:

- **Admin** (`RescheduleAppointmentPage.tsx`): formulario con `react-hook-form` + zod y campos
  `datetime-local` combinado (fecha+hora), `durationMinutes`, `locationId`/`location`,
  `notes` (Textarea) y botones Cancelar / "Guardar nueva fecha".
- **Pública** (`BookingReschedulePage` → `CustomerBookingsPage mode="reschedule"`): ya era
  selector de fecha + tarjetas de horario, consultaba `/agenda/availability` y validaba con
  `ensureSlotAvailable`. **No se modifica.**

El backend `BookingService.reschedule` original permitía cambiar duración/sucursal/notas en la
misma llamada (usaba `request.durationMinutes()`, `resolveLocation(...)`, `request.notes()`).
Se reescribió para respetar estrictamente los datos de la cita actual.

## 3. Backend: cambios

- **`bookings/application/BookingService.java`** — `reschedule(...)` reescrita:
  - Usa **solo** `request.startsAt()` (sigue siendo `@NotNull`).
  - Conserva `current.durationMinutes()`, `current.locationId()`, `current.location()` y
    `current.notes()` de la cita actual.
  - Valida disponibilidad con `ensureSlotAvailable(businessId, bookingId, current.locationId(),
    newStartsAt, duration)` → `hasOverlappingActiveBooking` → 409 `BOOKING_SLOT_NOT_AVAILABLE`
    ("El horario ya esta ocupado para esta sucursal." + `fieldErrors.startsAt`).
  - Audita con `previousStartsAt`/`newStartsAt` y mantiene la duración en el historial.
- **`bookings/api/BookingDetailResponse.java`** + **`bookings/infrastructure/BookingJdbcRepository.java`**:
  el detalle de la cita ahora expone `serviceId`, `professionalId` y `roomId`
  (SQL `b.service_id, b.professional_id, b.room_id` + rowMapper), necesarios para consultar la
  disponibilidad desde la pantalla. Se actualizaron los constructores de los tests que usan el
  record (`BookingServiceTest`, `CompleteDigitalAgendaServiceTest`).
- **Sin cambios** en `CompleteDigitalAgendaService.availability`: ya ordena ascendentemente
  `slots.sort(Comparator.comparing(AgendaSlotResponse::startsAt))` (requisito 6.1 ya cumplido en
  el servicio, verificado en vivo).

## 4. Frontend: cambios

**`bookings/pages/RescheduleAppointmentPage.tsx`** reescrita (se eliminan
`react-hook-form`/`zod`, `Textarea`, `BusinessLocationSelect`, `datetime-local`):

- **Campo fecha**: `<input type="date">` con label "Selecciona una nueva fecha" y `min` = hoy.
- **Disponibilidad real**: al elegir fecha, `getAgendaAvailabilityRequest` con
  `locationId`/`serviceId`/`professionalId` tomados del detalle de la cita y `maxSlots: 40`.
- **Grilla de horarios**: solo `slot.available`, botones `<button>` con
  `aria-label="Horario disponible a las HH:mm, finaliza a las HH:mm."`, `aria-pressed`,
  hora + "Hasta HH:mm" y, al seleccionar, borde teal + fondo teal + `ring-2` + marca ✓
  (destacado por más de un canal, no solo color).
- **Orden ascendente** por `dayjs(...).valueOf()` (defensivo; el backend ya ordena).
- **Resumen**: panel "Nueva fecha" (DD/MM/YYYY), "Hora seleccionada", "Finaliza" (inicio +
  duración original de la cita).
- **Estados**: carga (skeleton), vacío ("No encontramos horarios disponibles para esta fecha.
  Selecciona otro día."), error ("No fue posible consultar los horarios disponibles. Intenta
  nuevamente." + botón "Reintentar").
- **Acciones**: Cancelar (navega al detalle) y "Guardar nueva fecha" (deshabilitado hasta elegir
  horario; envía **solo** `{ startsAt: ISO }`; ante 409 muestra "El horario seleccionado acaba de
  dejar de estar disponible. Selecciona otro horario.").
- Cambiar la fecha limpia la selección y deshabilita Guardar.

**`services/api/types.ts`**: `BookingDetailResponse` + `serviceId`, `professionalId`, `roomId`.

## 5. Reglas de negocio intactas (verificación)

- El cálculo de disponibilidad, duración del servicio, ventanas, políticas de agenda y la
  prevención de solapamientos (`hasOverlappingActiveBooking`) no se modificaron.
- El reschedule conserva `durationMinutes`, `locationId`, `location`, `notes` y `roomId`
  originales; solo cambia `startsAt`/`endsAt`, `status → REPROGRAMADA`, `completedAt = null` y
  `version + 1`.
- La pantalla pública (`CustomerBookingsPage mode="reschedule"`) quedó intacta.

## 6. Pruebas frontend (9 nuevas, todas verdes)

`RescheduleAppointmentPage.test.tsx` (mock `fetch` sobre `API_BASE`, patrón del repo):
1. Solo muestra fecha/horarios/acciones: sin `datetime-local`, sin inputs numéricos, sin
   `textarea`, sin labels Duración/Sucursal/Ubicación/Notas; presentes "Selecciona una nueva
   fecha", Cancelar y "Guardar nueva fecha".
2. Sin fecha no hay horarios.
3. Horarios ordenados ascendentemente (09:00, 09:15, 10:00, 11:30 por `aria-label`).
4. Fecha sin disponibilidad muestra mensaje de vacío; cambiar de fecha recarga.
5. Error de consulta muestra mensaje y "Reintentar" recupera.
6. Guardar deshabilitado hasta elegir horario; selección marca `aria-pressed`, habilita Guardar
   y muestra el resumen (Nueva fecha / Hora seleccionada / Finaliza).
7. Cambiar la fecha limpia la selección y deshabilita Guardar.
8. El fin se calcula con la duración original (11:30 → 12:15 para 45 min).
9. Guardar envía solo `{ startsAt }` y navega al detalle; un 409 muestra el mensaje de slot
   caído.

Evidencia: `npm run test -- --run` → **17 archivos / 173 tests, 0 fallos** (9 nuevos).

## 7. Pruebas backend (2 nuevas, verdes)

`BookingServiceTest` (+2):
- `rescheduleUsesOnlyNewStartTimeKeepingOriginalDurationLocationAndNotes`: verifica que
  `rescheduleBooking` recibe el nuevo `startsAt`, duración 60 y la `locationId`/`location`/notas
  originales.
- `rescheduleRejectsWhenSlotAlreadyTaken`: slot ocupado → 409 `BOOKING_SLOT_NOT_AVAILABLE`.

Evidencia: `BookingServiceTest` **6/6 OK**; `CompleteDigitalAgendaServiceTest` 19 run / 1 fail
(**pre-existente**, confirmado con `git stash`: `createTemporaryBookingRejectsMissingInformedConsentWhenServiceRequiresIt`
falla idéntico sin los cambios de esta tarea). `mvn spotless:apply` aplicado.

## 8. Compilación frontend

`npm run build` (tsc + vite): **✓ built**. Se corrigieron además **13 errores TS**: 2 propios de
esta tarea (firma `makeSlot` en el test) y **11 pre-existentes ajenos** (verificados con
`git stash`: aparecen sin los cambios de esta tarea) en `LandingImage.tsx`
(`fetchpriority` → `fetchPriority`), módulo `business-ai` (`StatusBadge` recibía `status`
inexistente → `label`/`tone`; `PromptTemplateResponse` sin `updatedAt` → campo opcional en
`types.ts`; anotaciones de tupla en `BusinessAiPage.test.tsx`).

## 9. Validación funcional en vivo (backend Docker reconstruido)

1. Login `admin@demo.cl` → token con `BOOKINGS_RESCHEDULE` ✓.
2. Detalle de cita (`dff87343...`, CONFIRMADA, 60 min): devuelve `serviceId`, `professionalId`,
   `roomId` (campos nuevos) ✓.
3. `POST /agenda/availability` (2026-08-06): **26 slots, orden ascendente 13:00 → 21:00** ✓.
4. `PATCH /bookings/dff87343.../reschedule` con `{ "startsAt": "2026-08-06T13:00:00-04:00" }`
   → **200**, `status: REPROGRAMADA`, `startsAt: 2026-08-06T17:00:00Z`, duración 60, misma
   sucursal, notas null (sin cambios colaterales) ✓.
5. `PATCH` de otra cita al mismo slot → **409** `BOOKING_SLOT_NOT_AVAILABLE`
   "El horario ya esta ocupado para esta sucursal." + `fieldErrors.startsAt` ✓.

## 10. Evidencia visual (desktop + móvil, login real)

`e2e/evidencia-reprogramacion.spec.ts` (2 tests, backend y frontend reales en Docker):
- Desktop 1440x900 y móvil 390x844: login real → abrir la cita `dff87343...` en
  `/appointments/.../reschedule` → seleccionar fecha 2026-08-06 → esperar grilla de horarios →
  seleccionar el primero (verifica "Guardar nueva fecha" habilitado).
- **2 passed**. Capturas (4) en `frontend-react/e2e/reports/`:
  `evidencia-reprogramacion-desktop-fecha-sin-seleccion.png`,
  `evidencia-reprogramacion-desktop-horario-seleccionado.png`,
  `evidencia-reprogramacion-movil-fecha-sin-seleccion.png`,
  `evidencia-reprogramacion-movil-horario-seleccionado.png`.
- Nota: el contenedor `frontend-react` servía la versión anterior (vite dev obsoleto); se
  reconstruyó (`docker compose ... up -d --build frontend-react`) y la evidencia se capturó con
  la versión nueva (los asserts del spec — label "Selecciona una nueva fecha", botones
  "Horario disponible a las ...", Guardar habilitado — solo existen en la UI nueva).

## 11. Archivos del cambio

Nuevos: `RescheduleAppointmentPage.test.tsx`, `e2e/evidencia-reprogramacion.spec.ts`.
Modificados: `RescheduleAppointmentPage.tsx` (reescrita), `types.ts` (+3 campos y
`updatedAt` opcional), `BookingService.java`, `BookingDetailResponse.java`,
`BookingJdbcRepository.java`, `BookingServiceTest.java`, `CompleteDigitalAgendaServiceTest.java`,
`LandingImage.tsx`, `StatusBadge` usos en `business-ai` (4 componentes) y
`BusinessAiPage.test.tsx`.

## 12. Riesgos y pendientes

- **Token Meta sin permisos** (error 100/33): el envío real de WhatsApp sigue pendiente del
  token generado en WhatsApp Manager → Configuración de API; usar
  `.\scripts\update-cloud-api-token.ps1 -Token "EAAG..." -RestartBackend`.
- **Test pre-existente fallido** `createTemporaryBookingRejectsMissingInformedConsentWhenServiceRequiresIt`
  (confirmado sin relación con esta tarea).
- Los containers `backend-java` y `frontend-react` fueron reconstruidos con los cambios de esta
  tarea (Cloud API sigue habilitada con la configuración anterior).
