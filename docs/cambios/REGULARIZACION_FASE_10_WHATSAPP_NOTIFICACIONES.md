# Regularizacion Fase 10 - Canal de WhatsApp y notificaciones

Fecha: 2026-08-03

## Alcance aplicado

Esta fase separa la lectura operativa del canal de WhatsApp, limpia informacion sensible o tecnica de la interfaz y regulariza el centro de notificaciones interno sin crear datos simulados ni exponer identificadores.

## Cambios implementados

### Canal de WhatsApp

- Se separo la pantalla en estado operativo, configuracion, diagnostico e historial de eventos.
- Se eliminaron botones duplicados de conexion y desconexion.
- La desconexion mantiene confirmacion obligatoria.
- El numero asociado se muestra parcialmente oculto.
- Se dejo de mostrar el identificador tecnico del numero y los identificadores de entrega.
- Se reemplazaron textos tecnicos por mensajes operativos en espanol.
- Se quitaron numeros de prueba incrustados en el formulario de envio controlado.
- El envio controlado ya no muestra identificadores externos al usuario.
- La auditoria de IA usa lenguaje de negocio y no menciona base de datos, productos ni stock.

Archivos:

- `frontend-react/src/modules/administration/pages/WhatsAppChannelPage.tsx`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/application/WhatsAppChannelAdministrationService.java`

### Indicadores reales del canal

- El estado del canal ahora expone:
  - Ultimo mensaje recibido.
  - Ultimo mensaje enviado.
  - Mensajes entregados.
  - Mensajes leidos.
  - Mensajes fallidos.
- Los indicadores se calculan desde `message` y `message_delivery_log`.

Archivos:

- `backend-java/src/main/java/com/asistentewhatsapp/administration/api/WhatsAppChannelStatusResponse.java`
- `backend-java/src/main/java/com/asistentewhatsapp/channels/infrastructure/WhatsAppChannelJdbcRepository.java`
- `frontend-react/src/services/api/types.ts`

### Notificaciones

- Se regularizo el centro de notificaciones como centro de alertas internas.
- Se agrego columna de canal visible.
- Se agrego fecha de lectura visible.
- Se agregaron tipos de notificacion relacionados con citas y envios.
- Se elimino la ruta visual a pedidos fuera del alcance activo.
- Se mantuvieron filtros, paginacion, marcado individual y marcado masivo como leidas.

Archivos:

- `frontend-react/src/modules/notifications/pages/NotificationsPage.tsx`

## Validaciones ejecutadas

- `mvn -q -DskipTests compile`
- `pnpm build`
- `pnpm test -- --run src/modules/notifications/pages/NotificationsPage.test.tsx`

Resultado: compilacion backend correcta, compilacion frontend correcta y prueba de notificaciones correcta.

## Pendientes controlados

- El historico completo de envios transaccionales por plantilla, intentos, error y cita relacionada existe distribuido entre tablas de mensajes, bitacoras de entrega y correos. Una pantalla unificada de auditoria de envios requiere un endpoint dedicado no destructivo.
- La vigencia exacta de credenciales de Meta no esta persistida como fecha de expiracion; la interfaz muestra estado operativo derivado sin inventar vencimientos.
