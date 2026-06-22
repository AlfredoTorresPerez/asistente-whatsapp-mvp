# CAMBIOS_AGENDA_CELDAS_CONTENIDAS_HORA_ACTUAL

## Problema detectado

La agenda semanal ya mostraba reservas y una línea punteada de hora actual, pero las tarjetas de reservas se renderizaban con posicionamiento absoluto sobre el calendario completo. Cuando existían varias reservas dentro de una misma hora, algunas tarjetas invadían visualmente la franja horaria siguiente.

Ejemplo observado:

- 15:00 - 15:30
- 15:30 - 16:00
- 16:05 - 16:35

Las reservas cercanas se apilaban, pero podían montarse sobre la fila 16:00 o 17:00 porque la fila no crecía como contenedor real antes de pintar las tarjetas.

## Solución aplicada

Se cambió el modelo visual de la agenda semanal desde eventos flotantes sobre una grilla fija hacia filas horarias reales con altura dinámica.

Ahora:

1. Cada hora es una fila real de la grilla.
2. Cada fila calcula su altura antes de renderizar los eventos.
3. La altura se calcula según la mayor cantidad de reservas existentes en esa hora, considerando todos los días visibles.
4. Las reservas se agrupan por fecha local `America/Santiago` y por hora local de inicio.
5. Las tarjetas se renderizan dentro de la celda día/hora correspondiente usando `flex-column`.
6. Las tarjetas ya no usan `position: absolute` global sobre el calendario.
7. Cada celda tiene `overflow-hidden`, evitando que una reserva invada visualmente otra hora.
8. La línea punteada `Ahora HH:mm` recalcula su posición usando la suma de alturas dinámicas de filas anteriores.

## Reglas de agrupación aplicadas

- 15:00 - 15:30 pertenece a la fila 15:00.
- 15:30 - 16:00 pertenece a la fila 15:00.
- 16:05 - 16:35 pertenece a la fila 16:00.

Esto evita que una tarjeta de una hora contamine visualmente otra franja.

## Archivos modificados

- `frontend-react/src/modules/agenda/pages/CompleteAgendaPage.tsx`
- `CAMBIOS_AGENDA_CELDAS_CONTENIDAS_HORA_ACTUAL.md`

## Detalle técnico

Se reemplazaron los cálculos basados en:

```ts
top = hourSlot.top + minuteOffset + stackedOffset
```

por una grilla real de filas:

```tsx
hourRow[15:00]
  hourLabel
  dayCell[LUN]
  dayCell[MAR]
  ...
```

Cada fila usa una altura dinámica:

```ts
height = Math.max(
  baseHourHeight,
  rowVerticalPadding * 2 + maxItems * eventCardHeight + (maxItems - 1) * eventGap,
)
```

La línea de hora actual ahora usa:

```ts
top = hourSlot.top + (minute / 60) * hourSlot.height
```

y no una multiplicación fija por `baseHourHeight`.

## Cómo probar

1. Copiar los archivos modificados sobre el proyecto.
2. Reconstruir frontend:

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml build --no-cache frontend-react
docker compose -f docker-compose.local.yml up -d
```

3. Abrir:

```text
http://localhost:5173
```

4. Forzar recarga:

```text
Ctrl + F5
```

## Casos de aceptación visual

Validar con reservas:

- Viernes 19/06/2026 15:00 - 15:30.
- Viernes 19/06/2026 15:30 - 16:00.
- Viernes 19/06/2026 16:05 - 16:35.
- Jueves 18/06/2026 16:00 - 17:00.

Resultado esperado:

- Las reservas de 15:00 y 15:30 quedan dentro de la fila 15:00.
- La reserva de 16:05 queda dentro de la fila 16:00.
- La fila 16:00 no queda tapada por tarjetas de 15:00.
- La fila 17:00 no queda invadida por tarjetas de 16:00.
- Si una hora tiene varias reservas, la fila crece.
- La línea `Ahora HH:mm` sigue cruzando la semana completa en la posición correcta.
- La vista semanal conserva 7 columnas.
- La carga de filtros y reservas no cambia de contrato.

## Validación en sandbox

No se ejecutó `pnpm build` dentro del sandbox porque el proyecto limpio no incluye `node_modules` y el entorno puede no tener acceso de red para descargar dependencias. La validación final debe ejecutarse con el flujo Docker local del proyecto.
