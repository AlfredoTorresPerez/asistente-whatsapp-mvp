# Cambios V23.4 - Cierre transaccional de agenda y enlace

## Objetivo
Cerrar el flujo cuando el cliente entrega servicio, fecha, hora y sucursal en un solo mensaje de WhatsApp.

## Problema corregido
Antes, una solicitud completa como:

```text
Hola, quiero reservar limpieza facial profunda manana a las 10:00 en Providencia
```

podia terminar en una respuesta intermedia del tipo "voy a validar" o podia sugerir que faltaba sucursal aunque el cliente ya habia indicado Providencia.

## Cambios aplicados

- Se agrego `TransactionalAgendaBookingService` para resolver servicio, sucursal, fecha y hora.
- Se conecta la intencion de agenda completa con disponibilidad real de agenda.
- Si existe cupo exacto, se crea reserva temporal.
- Se genera enlace publico de confirmacion.
- La respuesta al cliente incluye resumen de reserva temporal y enlace.
- Si no hay cupo exacto, se responde con falta de disponibilidad y alternativas reales cuando existen.
- `BookingAgent` ahora usa el flujo transaccional cuando no faltan datos.
- `AestheticCenterService` tambien usa el flujo transaccional para el endpoint de analisis usado por las pruebas.
- Se agrega migracion `V26__complete_booking_transactional_link_flow.sql` para reforzar reglas y alias de Providencia.
- Se agrega `test_ia_negocio_conversacional_v23_4.ps1` con casos T01 a T10.

## Archivos principales

```text
backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java
backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java
backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java
backend-java/src/main/resources/db/migration/V26__complete_booking_transactional_link_flow.sql
database/manual/V26__complete_booking_transactional_link_flow.sql
test_ia_negocio_conversacional_v23_4.ps1
```

## Validacion pendiente
No se pudo compilar localmente en este entorno porque no hay Maven ni Docker disponibles y el wrapper Maven no pudo descargar dependencias externas. La validacion final debe ejecutarse en el ambiente local del proyecto.
