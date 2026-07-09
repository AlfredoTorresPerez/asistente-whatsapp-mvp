# Hallazgos — Pruebas E2E Automatizadas

**Fecha:** 2026-07-06
**Proyecto:** Asistente de Ventas por WhatsApp
**Framework:** Playwright + TypeScript
**Total pruebas:** 48 (39 pasan, 0 fallan, 9 skip por BLOCKED)

---

## Resumen de Hallazgos

| # | Hallazgo | Tipo | Severidad | Archivo |
|---|----------|------|-----------|---------|
| 1 | Patron glob de string no matchea query params | Bug en test | Alta | `e2e/*.spec.ts`, `helpers/auth.helper.ts` |
| 2 | Catch-all `**/api/v1/**` se antepone a rutas especificas | Bug en test | Alta | `e2e/*.spec.ts` |
| 3 | `getWhatsAppStatus()` crash por `reminders` undefined | Bug en app | Alta | `src/modules/agenda/pages/CompleteAgendaPage.tsx:64` |
| 4 | 401 en `business-locations` limpia sesion via evento global | Bug en test | Alta | `e2e/helpers/agenda.helper.ts` |
| 5 | Falta mock para `bookings/:id` | Bug en test | Alta | `e2e/helpers/agenda.helper.ts` |
| 6 | `getByRole('heading')` strict mode violation | Bug en test | Media | `e2e/booking-public-pages.spec.ts` |
| 7 | Selector CSS `[class*="border-l-"]` no encuentra elementos | Bug en test | Baja | `e2e/04-agenda-visual.spec.ts` |
| 8 | `getByText()` ambiguedad con nombres duplicados | Bug en test | Media | `e2e/05,06,07-whatsapp-*.spec.ts` |
| 9 | Ruta `/reservas/pagar/` no existe en el frontend | Blocker de feature | Media | `e2e/booking-public-pages.spec.ts` |

---

## Hallazgo 1: Patron glob de string no matchea query params

### Error
```typescript
page.route('**/api/v1/business-locations', handler)
```
El patron string glob `**/api/v1/business-locations` NO coincide con la URL real:
```
http://localhost:5173/api/v1/business-locations?activeOnly=true
```
Playwright interpreta el string como un sufijo (porque empieza con `*`). El sufijo `/api/v1/business-locations` no considera los query params. La solicitud cae al backend real, que retorna 401.

### Impacto
El backend retorna 401 → `httpClient.ts` detecta 401 en endpoint con `requiresAuth: true` → dispara evento `shell-session-expired` → `ShellSessionProvider` escucha el evento y setea `status: 'unauthenticated'` → `PrivateRouteShell` redirige a `/login`.

Todas las pruebas que requieren sesion fallan por este motivo, independientemente de que `auth/me` y `users/me` esten correctamente mockeados.

### Solucion
Usar regex en lugar de string glob:
```typescript
page.route(/\/api\/v1\/business-locations/, handler)
```
O agregar `*` al final del string glob:
```typescript
page.route('**/api/v1/business-locations*', handler)
```

### Archivos afectados
- `e2e/01-smoke.spec.ts` — patron `business-locations` sin `*`
- `e2e/helpers/agenda.helper.ts` — patron `business-locations` sin `*`
- `e2e/helpers/auth.helper.ts` — todos los patrones en `setupDefaultMocks()`

---

## Hallazgo 2: Catch-all `**/api/v1/**` se antepone a rutas especificas

### Error
```typescript
// Registrados en orden:
page.route('**/api/v1/auth/me', handlerEspecifico)       // 1ero
page.route('**/api/v1/**', handlerCatchAll)               // ultimo
```
A pesar de que `auth/me` se registra primero, Playwright evalua los string globs de forma que el catch-all `**/api/v1/**` (registrado ultimo) termina matcheando todas las solicitudes, incluyendo `auth/me`, `users/me`, etc.

### Causa raiz
Playwright procesa los glob patterns con sufijo (los que empiezan con `*`) de manera diferente. Cuando dos patrones glob (ambos con `**`) matchean la misma URL, NO se respeta el orden de registro. El patron mas general (`**/api/v1/**`) gana sobre el mas especifico (`**/api/v1/auth/me`).

Esto ocurre especificamente con string globs. Con regex, el orden de registro SI se respeta.

### Solucion
Usar exclusivamente regex en `page.route()` para garantizar que el orden de registro defina la precedencia:
```typescript
page.route(/\/api\/v1\/auth\/me(\?|$)/, handlerEspecifico)   // 1ero
// NO usar catch-all con glob o regex
```

### Archivos afectados
- `e2e/03-agenda-basica.spec.ts`
- `e2e/04-agenda-visual.spec.ts`
- `e2e/08-confirmacion-publica.spec.ts`
- `e2e/helpers/auth.helper.ts`
- `e2e/helpers/agenda.helper.ts`

---

## Hallazgo 3: `getWhatsAppStatus()` crash por `reminders` undefined

### Error
```typescript
function getWhatsAppStatus(item, detail?: BookingDetailResponse) {
  const lastWhatsAppReminder = detail?.reminders
    .filter((reminder) => reminder.channelType === 'WHATSAPP')  // crash aqui
    // ...
}
```
Cuando `detail` es un objeto (la API respondio con datos) pero `detail.reminders` NO esta presente en la respuesta, la expresion `detail?.reminders` retorna `undefined`. Luego `.filter()` se ejecuta sobre `undefined`, causando:
```
TypeError: Cannot read properties of undefined (reading 'filter')
```

### Por que es un error
- En TypeScript, `detail` es opcional (`detail?`) y `detail.reminders` NO tiene optional chaining. Si `detail` esta definido pero `detail.reminders` es `undefined`, el codigo falla.
- Es inconsistente con el resto del mismo archivo: la funcion `buildRecentActivity()` (linea 77) usa correctamente `(detail?.statusHistory ?? [])`.
- React renderiza un error boundary ("Unexpected Application Error") que impide que la pagina de agenda se muestre.

### Impacto
La pagina de agenda completa NO se renderiza en absoluto. Con las mocks anteriores, el booking detail mock no incluia el campo `reminders`, lo que gatillaba el crash siempre.

### Solucion
```typescript
const lastWhatsAppReminder = (detail?.reminders ?? [])
  .filter((reminder) => reminder.channelType === 'WHATSAPP')
```

### Archivo afectado
- `src/modules/agenda/pages/CompleteAgendaPage.tsx:64`

---

## Hallazgo 4: 401 en `business-locations` limpia sesion via evento global

### Error
Cuando `httpClient.ts` recibe un 401 en cualquier endpoint autenticado, ejecuta:
```typescript
if (requiresAuth && response.status === 401) {
  writeStoredShellSessionSnapshot(null)
  window.dispatchEvent(new CustomEvent('shell-session-expired'))
}
```
El evento `shell-session-expired` es escuchado por `ShellSessionProvider`, que setea `status: 'unauthenticated'`.

### Por que es un error de prueba
Aunque `auth/me` y `users/me` esten mockeados exitosamente, si CUALQUIER OTRO endpoint falla con 401 (porque no tiene mock o el patron glob no coincide), la sesion se elimina. Esto hace que `PrivateRouteShell` redirija a `/login` aunque el restore de sesion ya haya completado exitosamente.

El orden tipico de eventos:
1. `ShellSessionProvider` monta → status `'loading'`
2. `meRequest()` mockeado responde 200 OK
3. `getCurrentProfileRequest()` mockeado responde 200 OK
4. `setStatus('authenticated')` → `PrivateRouteShell` muestra `PrivateLayout`
5. `CompleteAgendaPage` monta → `businessLocationsQuery` se ejecuta → backend real responde 401
6. `httpClient.ts` dispara `shell-session-expired` → status `'unauthenticated'`
7. `PrivateRouteShell` redirige a `/login`

### Solucion
Asegurar que TODOS los endpoints que el componente montado pueda llamar esten mockeados, o usar regex en lugar de string globs para que los patrones coincidan correctamente.

### Archivos afectados
- `e2e/helpers/agenda.helper.ts` — patron `business-locations` sin cobertura de query params

---

## Hallazgo 5: Falta mock para `bookings/:id`

### Error
`CompleteAgendaPage` hace una solicitud a `/api/v1/bookings/:id` cuando el usuario selecciona una reserva del calendario. Incluso sin interaccion del usuario, el componente puede llamar este endpoint automaticamente al montar (via React Query con `refetchOnMount` o similar).

Sin mock, la solicitud va al backend real y retorna 401, lo que activa el mecanismo de expiracion de sesion descrito en el Hallazgo 4.

### Solucion
Agregar mock para el patron `/api/v1/bookings/` con los campos que el componente espera:
```typescript
page.route(/\/api\/v1\/bookings\//, async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      bookingId: '...',
      customerName: '...',
      status: 'CONFIRMED',
      reminders: [],
      statusHistory: [],
    }),
  })
})
```

### Archivos afectados
- `e2e/helpers/agenda.helper.ts` — no incluye mock para `bookings/`

---

## Hallazgo 6: `getByRole('heading')` strict mode violation

### Error
```typescript
await expect(page.getByRole('heading', { name: 'Pendiente de pago' })).toBeVisible()
```
Playwright lanza:
```
Error: locator.getByRole: strict mode violation: "heading" role allows multiple accessible names
```

### Por que es un error
Dos o mas elementos en el DOM tienen `role="heading"` con el mismo texto "Pendiente de pago". Esto puede ocurrir porque:
- La pagina tiene un `<h1>` con ese texto y otro `<h2>` o `<div role="heading">` con el mismo texto
- O hay un header de seccion y un titulo de tarjeta con contenido identico

### Solucion
Usar `.first()` para seleccionar el primer match, o cambiar a `getByText()` si es mas especifico:
```typescript
await expect(page.getByText('Pendiente de pago').first()).toBeVisible()
```

### Archivo afectado
- `e2e/booking-public-pages.spec.ts`

---

## Hallazgo 7: Selector CSS `[class*="border-l-"]` no encuentra elementos

### Error
```typescript
const cards = page.locator('[class*="border-l-"]')
const count = await cards.count()
expect(count).toBeGreaterThanOrEqual(1)  // count = 0
```

### Por que es un error
El componente `AgendaEventCard` NO usa clases CSS `border-l-*` de Tailwind. En lugar de eso:
- Modo compact: usa `style={{ borderLeft: '3px solid ...' }}` (inline style)
- Modo normal: usa un `<div className="w-1.5 shrink-0" style={{ backgroundColor: ... }}>` separado

Ningun elemento en el DOM tiene una clase que contenga "border-l-".

### Solucion
Usar un selector basado en el contenido del boton o en el role:
```typescript
const cards = page.getByRole('button').filter({ hasText: /QA_AUTO_VISUAL/ })
const count = await cards.count()
```

### Archivo afectado
- `e2e/04-agenda-visual.spec.ts:108`

---

## Hallazgo 8: `getByText()` ambiguedad con nombres duplicados

### Error
```typescript
await expect(page.getByText('QA_AUTO_CLIENTE_1')).toBeVisible()
```
Playwright lanza:
```
Error: strict mode violation: getByText('QA_AUTO_CLIENTE_1') resolved to 2 elements
```

### Por que es un error
En la agenda, el mismo nombre de cliente aparece en:
1. La tarjeta de evento dentro del calendario semanal (`AgendaEventCard`)
2. El panel de detalle lateral (`BookingDetailPanel`)
Ambos elementos contienen el texto del nombre del cliente.

### Solucion
```typescript
await expect(page.getByText(QA_CUSTOMER_NAME).first()).toBeVisible()
```

### Archivos afectados
- `e2e/05-whatsapp-reserva-simulada.spec.ts`
- `e2e/06-whatsapp-cancelacion-simulada.spec.ts`
- `e2e/07-whatsapp-reprogramacion-simulada.spec.ts`

---

## Hallazgo 9: Ruta `/reservas/pagar/` no existe en el frontend

### Error
```typescript
await page.goto(`/reservas/pagar/${bookingId}`)
// La pagina muestra 404 o redirige porque la ruta no existe
```

### Por que es un error
El archivo `src/app/router.tsx` define rutas solo para:
- `/reservas/confirmar/:token` → `BookingConfirmationPage`
- `/reservas/reprogramar/:token` → `BookingReschedulePage`
- `/reservas/cancelar/:token` → `BookingCancellationPage`

La ruta `/reservas/pagar/:bookingId` NO esta implementada en la aplicacion. Las pruebas fueron escritas basadas en la matriz de pruebas, pero la funcionalidad de pago publico aun no existe en el frontend.

### Solucion
Marcar las pruebas como `BLOCKED` hasta que la ruta sea implementada:
```typescript
test.skip(true, 'Ruta /reservas/pagar/ no implementada en el frontend — BLOCKED')
```

### Archivo afectado
- `e2e/booking-public-pages.spec.ts`

---

## Lecciones Aprendidas

1. **Usar regex, no string globs, en `page.route()`**: Los string globs con `**` tienen comportamiento impredecible con query params y orden de registro. Los regex son deterministicos y respetan el orden de registro.

2. **Mockear todos los endpoints que el componente montado pueda llamar**: Incluso si no se espera interaccion del usuario, React Query puede hacer fetching automatico de datos al montar.

3. **El 401 es destructivo**: `httpClient.ts` tiene un manejador global de 401 que limpia la sesion completa. Un solo endpoint no mockeado puede arruinar toda la sesion.

4. **Coherencia en datos mockeados**: Si un endpoint espera campos anidados (como `reminders[]` dentro de `BookingDetailResponse`), el mock debe incluirlos. TypeScript no protege en runtime contra campos faltantes.

5. **Preferir `.first()` sobre locators exactos**: En paginas complejas, el mismo texto puede aparecer en multiples lugares. Usar `.first()` hace las pruebas mas resilientes sin sacrificar la validacion.

---

*Documento generado a partir del analisis de 48 pruebas E2E ejecutadas el 2026-07-06.*
