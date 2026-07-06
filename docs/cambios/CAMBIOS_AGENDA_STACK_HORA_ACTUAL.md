# Cambios ejecutados: Agenda con stacking dinámico y línea de hora actual

## Objetivo
Ajustar la agenda digital completa para que, cuando una hora tenga más de una reserva en la misma celda/día, la fila horaria aumente su altura y las tarjetas se apilen sin taparse. Además, agregar una línea punteada horizontal que cruce toda la semana indicando la hora actual local.

## Cambios aplicados

### Frontend
Archivo modificado:

- `frontend-react/src/modules/agenda/pages/CompleteAgendaPage.tsx`

Cambios técnicos:

1. Se reemplazó el alto fijo global de agenda por un layout dinámico por hora.
2. Se agregó `buildAgendaHourLayout(...)` para calcular la altura de cada franja horaria según la cantidad máxima de reservas que comienzan en esa hora en cualquier día visible.
3. Se agregó `buildStackedDayItems(...)` para asignar un `stackIndex` a reservas que caen en la misma hora del mismo día.
4. Se modificó `getCalendarItemPosition(...)` para ubicar tarjetas usando:
   - hora local `America/Santiago`;
   - alto dinámico de la fila;
   - desplazamiento vertical por apilamiento.
5. Se agregó `getCurrentTimeIndicator(...)` para calcular la posición de la hora actual en la semana visible.
6. Se agregó una línea punteada horizontal de color rojo/rosado con etiqueta `Ahora HH:mm`, visible solo si el día actual está dentro de la semana mostrada y dentro del rango horario de la grilla.
7. La línea cruza toda la semana desde después de la columna de horas hasta el último día visible.

## Criterios de aceptación cubiertos

- Si una celda horaria tiene más de una reserva, la fila crece en altura.
- Las reservas de la misma hora se muestran apiladas y con `z-index` controlado.
- La hora actual se muestra mediante línea punteada horizontal a lo largo de toda la semana.
- La posición sigue usando zona horaria `America/Santiago`.
- No se cambia el contrato del backend.

## Validación pendiente en ambiente local

Ejecutar:

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml build --no-cache frontend-react
docker compose -f docker-compose.local.yml up -d
```

Luego abrir `http://localhost:5173`, entrar a Agenda completa y presionar `Ctrl + F5`.

## Nota de build en sandbox

No se ejecutó `pnpm build` en este sandbox porque no hay acceso a red para descargar el gestor `pnpm` por Corepack. El cambio es TypeScript/React puro y debe validarse con el build Docker local.
