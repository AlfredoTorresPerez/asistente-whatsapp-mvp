# Cambios - calendario mensual siempre visible

## Objetivo

Ajustar la pantalla **Citas** para que el calendario mensual exista visualmente aunque el mes no tenga citas registradas todavia, y para que las reservas creadas o confirmadas desde WhatsApp sean mas faciles de encontrar en la vista mensual.

## Cambios aplicados

### 1. Calendario mensual siempre renderizado

Archivo modificado:

```text
frontend-react/src/modules/bookings/pages/AppointmentsPage.tsx
```

Antes, si el mes no tenia citas, la pantalla reemplazaba el calendario por un estado vacio. Ahora el calendario del mes se muestra siempre, con o sin citas.

Esto evita la confusion de que "no existe la agenda del mes". La agenda mensual queda disponible para cualquier mes visible.

### 2. Seleccion automatica del primer dia con cita

Cuando el mes tiene citas y el dia seleccionado no contiene registros, la pantalla selecciona automaticamente el primer dia con una cita del mes. Esto permite que una reserva confirmada para el dia 15, por ejemplo, aparezca tambien en la lista diaria sin que el usuario tenga que buscar manualmente el dia.

### 3. Cambio de mes consistente

Al presionar **Mes anterior** o **Mes siguiente**, la pantalla actualiza tambien el dia seleccionado al primer dia del nuevo mes. Luego, si existen citas en ese mes, selecciona la primera fecha con cita.

### 4. Texto de estado vacio mas preciso

Se reemplazo el mensaje **Dia sin agenda** por **Dia sin citas**, porque la agenda si existe; simplemente puede no tener reservas para el dia seleccionado.

## Resultado esperado

- La vista **Citas** muestra siempre el calendario mensual.
- Si no hay citas, igual se puede seleccionar un dia y crear una cita.
- Si hay citas en el mes, se selecciona automaticamente la primera fecha con registros.
- Las reservas confirmadas desde WhatsApp quedan visibles sin depender de que el usuario haya seleccionado manualmente el dia correcto.
