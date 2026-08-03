# Regularizacion Fase 2 - Modelo de cita y estados

Fecha: 2026-08-03

## Alcance aplicado

- Se centralizo la traduccion de estados de cita en `BookingStateMachine` y `bookingOptions.ts`.
- Se incorporo el estado canonico `EN_ATENCION`.
- Se cambio el estado final visible/canonico de atencion terminada a `COMPLETADA`.
- Se mantuvo compatibilidad de lectura con estados historicos y aliases como `COMPLETED`, `ATTENDED`, `ATENDIDA`, `CONFIRMED`, `REQUESTED`, `NO_SHOW` e `IN_PROGRESS`.
- Se ajustaron transiciones criticas del servidor para impedir cambios desde estados cerrados y permitir el ciclo operativo:
  - Pendiente de confirmacion -> Confirmada.
  - Confirmada -> En atencion.
  - En atencion -> Completada.
  - Confirmada/Reprogramada -> Cancelada o Inasistencia.
- Se regularizaron filtros visibles de citas y reportes para enviar estados canonicos en espanol.
- Se adapto reportes para que el filtro `COMPLETADA` incluya registros historicos almacenados como `ATENDIDA`.
- Se dejo de mostrar el identificador interno de reserva en la vista publica de reservas del cliente.
- Se tradujo el estado de pago visible en la pagina publica de confirmacion de reserva.

## Archivos principales

- `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingStateMachine.java`
- `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/bookings/infrastructure/BookingJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/bookings/infrastructure/BookingConfirmationJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingPublicActionService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/reports/infrastructure/ReportsJdbcRepository.java`
- `frontend-react/src/modules/bookings/bookingOptions.ts`
- `frontend-react/src/modules/bookings/pages/AppointmentsPage.tsx`
- `frontend-react/src/modules/bookings/pages/AppointmentDetailPage.tsx`
- `frontend-react/src/modules/bookings/pages/BookingConfirmationPage.tsx`
- `frontend-react/src/modules/bookings/pages/CustomerBookingsPage.tsx`
- `frontend-react/src/modules/agenda/components/agendaUtils.ts`
- `frontend-react/src/modules/agenda/pages/CompleteAgendaPage.tsx`
- `frontend-react/src/modules/reports/pages/ReportsPage.tsx`

## Validacion ejecutada

- `backend-java`: `.\mvnw.cmd spotless:apply`
- `backend-java`: `.\mvnw.cmd -DskipTests compile`
- `backend-java`: `.\mvnw.cmd "-Dtest=BookingStateMachineTest,BookingServiceTest,BookingConfirmationServiceTest,BookingPublicActionServiceTest,CompleteDigitalAgendaServiceTest,CompleteAgendaJdbcRepositoryTest,CustomerSearchServiceIntegrationTest,PhoneUtilsTest" test`
- `frontend-react`: `pnpm build`
- `frontend-react`: `pnpm test -- --run src/modules/bookings/pages/BookingConfirmationPage.test.tsx src/modules/bookings/pages/RescheduleAppointmentPage.test.tsx`

Resultado: compilacion backend correcta, build frontend correcto, 83 pruebas backend enfocadas correctas y 19 pruebas frontend enfocadas correctas.

## Pendientes de fases posteriores

- Persistir todos los campos ampliados del modelo de cita solicitados para conversacion, prospecto, usuarios modificadores, recordatorios y motivos estructurados cuando el esquema existente no los tenga completos.
- Completar acciones operativas de Agenda para iniciar atencion, completar, cancelar con motivo obligatorio y confirmar por WhatsApp con vista previa y control de duplicados.
- Revisar permisos efectivos para cada accion de cita, especialmente reprogramacion, cancelacion, envio y auditoria.
