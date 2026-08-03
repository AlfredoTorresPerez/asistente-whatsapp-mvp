# Regularizacion Fase 3 - Agenda y Citas

Fecha: 2026-08-03

## Cambios realizados

- Agenda quedo enfocada en operacion diaria: filtros, calendario semanal, detalle, acciones y trazabilidad.
- Se elimino de Agenda el formulario extenso de creacion temporal al final de la pantalla. La creacion queda en el flujo guiado "Nueva cita".
- Se reemplazo la seccion "Agentes involucrados" por "Resumen operativo" con estado, pago, abono, saldo, origen, notificaciones e historial.
- Se agregaron acciones operativas explicitas desde Agenda:
  - Confirmar.
  - Iniciar atencion.
  - Completar.
  - Registrar inasistencia.
- Las acciones se validan en servidor con la maquina de estados de citas y registran historial/auditoria.
- La actualizacion de estado exige que la cita siga en el estado esperado al momento de guardar, para evitar sobrescrituras por acciones simultaneas.
- La pantalla Citas se oriento a consulta/historial y permite exportar resultados a CSV sin incluir identificadores internos.
- Los estados del resumen de citas ahora salen normalizados desde el servidor.

## Archivos involucrados

- `backend-java/src/main/java/com/asistentewhatsapp/agenda/api/AgendaLifecycleRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/agenda/api/CompleteDigitalAgendaController.java`
- `backend-java/src/main/java/com/asistentewhatsapp/agenda/application/CompleteDigitalAgendaService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/bookings/infrastructure/BookingJdbcRepository.java`
- `frontend-react/src/services/api/completeAgendaApi.ts`
- `frontend-react/src/services/api/types.ts`
- `frontend-react/src/modules/agenda/pages/CompleteAgendaPage.tsx`
- `frontend-react/src/modules/agenda/components/AppointmentDetailPanel.tsx`
- `frontend-react/src/modules/bookings/pages/AppointmentsPage.tsx`

## Validacion

- `backend-java`: `.\mvnw.cmd spotless:apply`
- `backend-java`: `.\mvnw.cmd -DskipTests compile`
- `backend-java`: `.\mvnw.cmd "-Dtest=BookingStateMachineTest,BookingServiceTest,BookingConfirmationServiceTest,CompleteDigitalAgendaServiceTest" test`
- `frontend-react`: `pnpm build`
- `frontend-react`: `pnpm test -- --run src/modules/agenda/components/agendaUtils.test.ts src/modules/bookings/pages/BookingConfirmationPage.test.tsx src/modules/bookings/pages/RescheduleAppointmentPage.test.tsx`
