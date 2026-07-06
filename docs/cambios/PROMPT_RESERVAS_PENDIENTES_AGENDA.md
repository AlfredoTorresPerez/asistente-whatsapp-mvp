# Prompt ejecutado: reservas pendientes visibles en agenda

Modificar el modulo de agenda y reservas del asistente WhatsApp para que las reservas pendientes de confirmacion sean visibles en la agenda administrativa.

Objetivo:
Cuando un cliente solicite una cita y el sistema genere un enlace de confirmacion, la reserva debe registrarse inmediatamente como una cita preliminar con estado PENDIENTE_CONFIRMACION. Esta cita debe bloquear temporalmente el cupo seleccionado hasta su vencimiento.

Reglas funcionales:
1. Al crear una reserva pendiente, insertar o registrar una cita visible en la agenda.
2. La cita debe tener estado PENDIENTE_CONFIRMACION.
3. La agenda administrativa debe listar citas pendientes, confirmadas, canceladas y expiradas segun filtros.
4. El cupo asociado debe considerarse ocupado mientras la reserva pendiente no expire.
5. Al confirmar el enlace, cambiar la cita de PENDIENTE_CONFIRMACION a CONFIRMED.
6. Al vencer el enlace, cambiar la cita a EXPIRADA y liberar el cupo.
7. Si el cliente intenta confirmar una reserva vencida, mostrar mensaje especifico de reserva expirada.
8. Si el cupo fue tomado por inconsistencia, mostrar mensaje especifico de cupo no disponible.
9. Evitar mensajes genericos que mezclen expiracion y disponibilidad.
10. Registrar eventos de auditoria para creacion, confirmacion, expiracion y cancelacion.

Reglas tecnicas:
1. La confirmacion debe ejecutarse dentro de una transaccion.
2. La reserva o cita debe bloquearse mediante bloqueo pesimista durante la confirmacion.
3. La validacion de vencimiento debe usar una referencia temporal consistente.
4. La operacion debe ser idempotente: confirmar dos veces no debe duplicar citas.
5. Si la cita ya esta CONFIRMED, retornar estado exitoso controlado indicando que ya fue confirmada.
6. Si la cita esta EXPIRADA, no confirmar y mostrar causa exacta.
7. Si la cita esta CANCELLED, no confirmar y mostrar causa exacta.
8. Agregar migracion de base de datos si faltan estados, fecha de expiracion, identificador de confirmacion o trazabilidad.
