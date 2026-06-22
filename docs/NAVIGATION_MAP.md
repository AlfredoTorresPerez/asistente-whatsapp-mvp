# NAVIGATION_MAP

## Objetivo

Este documento describe la matriz de navegacion de Fase 1. Su funcion es dejar explicito que dispara cada boton, enlace, tarjeta, fila, filtro, grafico, modal o panel lateral.

## Tipos de destino

- `route`: navega a una ruta completa.
- `same-route`: permanece en la misma ruta y refresca estado o datos.
- `modal`: abre una confirmacion o dialogo.
- `drawer`: abre un panel lateral contextual.
- `overlay`: abre un overlay contextual.
- `state`: cambia a un estado transversal como vacio, carga, error u offline.

## Publico y autenticacion

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `/login` | boton | `Ingresar` | `route` | `/dashboard` | Requiere credenciales validas. |
| `/login` | enlace | `Recuperar contrasena` | `route` | `/forgot-password` | Flujo publico. |
| `/forgot-password` | boton | `Enviar enlace` | `route` | `/forgot-password/sent` | Persiste la solicitud. |
| `/forgot-password` | boton o enlace | `Volver al inicio` | `route` | `/login` | No persiste cambios. |
| `/forgot-password/sent` | boton | `Volver al inicio` | `route` | `/login` | Cierra el flujo. |
| `/forgot-password/sent` | boton | `Reenviar enlace` | `same-route` | `/forgot-password/sent` | Reintenta `forgot-password`. |
| `/reset-password` | boton | `Guardar nueva contrasena` | `route` | `/login` | Solo con token valido. |
| `/reset-password` | boton | `Cancelar` | `route` | `/login` | Descarta el formulario. |

## Shell privado global

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `Sidebar` | enlace | `Dashboard` | `route` | `/dashboard` | Navegacion principal. |
| `Sidebar` | enlace | `Conversaciones` | `route` | `/conversations` | Navegacion principal. |
| `Sidebar` | enlace | `Prospectos` | `route` | `/prospects` | Navegacion principal. |
| `Sidebar` | enlace | `Agenda` | `route` | `/appointments` | Navegacion principal. |
| `Sidebar` | enlace | `Pedidos` | `route` | `/orders` | Navegacion principal. |
| `Sidebar` | enlace | `Catalogo` | `route` | `/catalog` | Navegacion principal. |
| `Sidebar` | enlace | `Reglas` | `route` | `/automation-rules` | Navegacion principal. |
| `Sidebar` | enlace | `Reportes` | `route` | `/reports` | Navegacion principal. |
| `Sidebar` | enlace | `Administracion` | `route` | `/admin` | Navegacion principal. |
| `Topbar` | icono o boton | `Notificaciones` | `route` | `/notifications` | Puede mostrar contador. |
| `Topbar` | avatar o boton | `Menu de usuario` | `overlay` | `overlay://user-menu` | Overlay contextual. |
| `overlay://user-menu` | boton | `Ver perfil` | `route` | `/profile` | Cierra overlay. |
| `overlay://user-menu` | boton | `Cambiar contrasena` | `route` | `/profile/change-password` | Cierra overlay. |
| `overlay://user-menu` | boton | `Notificaciones` | `route` | `/notifications` | Cierra overlay. |
| `overlay://user-menu` | boton | `Cerrar sesion` | `modal` | `modal://logout-confirmed` | Confirmacion obligatoria. |
| `modal://logout-confirmed` | boton | `Cerrar sesion` | `route` | `/login` | Ejecuta logout remoto y local. |
| `modal://logout-confirmed` | boton | `Cancelar` | `same-route` | ruta privada actual | Solo cierra modal. |

## Dashboard y notificaciones

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `/dashboard` | boton o tarjeta | `Ver conversaciones` | `route` | `/conversations` | CTA principal. |
| `/dashboard` | boton o tarjeta | `Ver prospectos` | `route` | `/prospects` | CTA principal. |
| `/dashboard` | boton o tarjeta | `Ver agenda` | `route` | `/appointments` | CTA principal. |
| `/dashboard` | boton o tarjeta | `Ver pedidos` | `route` | `/orders` | CTA principal. |
| `/dashboard` | boton | `Actualizar` | `same-route` | `/dashboard` | Reconsulta el resumen. |
| `/dashboard` | grafico | grafico de conversaciones | `route` | `/conversations` | Debe pasar filtros relevantes. |
| `/dashboard` | grafico | grafico de pedidos | `route` | `/orders` | Debe pasar filtros relevantes. |
| `/dashboard` | fila | actividad reciente | `route` | detalle relacionado | Abre entidad enlazada. |
| `/notifications` | tab | `Todas` | `same-route` | `/notifications` | Aplica filtro de estado. |
| `/notifications` | tab | `No leidas` | `same-route` | `/notifications` | Aplica filtro `unread`. |
| `/notifications` | boton | `Aplicar filtros` | `same-route` | `/notifications` | Refresca lista paginada. |
| `/notifications` | boton | `Limpiar filtros` | `same-route` | `/notifications` | Restablece filtros. |
| `/notifications` | boton | `Marcar todas como leidas` | `same-route` | `/notifications` | Mutacion masiva. |
| `/notifications` | fila | clic en notificacion | `route` | detalle relacionado | Puede abrir conversacion, prospecto, cita o pedido. |

## Perfil y seguridad personal

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `/profile` | boton | `Guardar cambios` | `same-route` | `/profile` | Muestra toast de exito. |
| `/profile` | boton | `Cancelar` | `route` | `/dashboard` | Descarta cambios locales. |
| `/profile` | boton | `Cambiar contrasena` | `route` | `/profile/change-password` | Ruta privada. |
| `/profile/change-password` | boton | `Guardar nueva contrasena` | `route` | `/profile` | Muestra toast de exito. |
| `/profile/change-password` | boton | `Cancelar` | `route` | `/profile` | Sin persistencia. |

## Conversaciones y plantillas

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `/conversations` | boton | `Nueva conversacion` | `route` | `/conversations/new` | Alta manual. |
| `/conversations` | boton | `Aplicar filtros` | `same-route` | `/conversations` | Refresca lista. |
| `/conversations` | boton | `Limpiar filtros` | `same-route` | `/conversations` | Restablece filtros. |
| `/conversations` | boton | `Actualizar` | `same-route` | `/conversations` | Reconsulta datos. |
| `/conversations` | fila | clic en fila | `route` | `/conversations/:conversationId` | Entra al detalle. |
| `/conversations/:conversationId` | boton | `Enviar mensaje` | `same-route` | `/conversations/:conversationId` | Refresca hilo. |
| `/conversations/:conversationId` | boton | `Usar plantilla` | `drawer` | drawer de plantillas | Panel lateral contextual. |
| `/conversations/:conversationId` | boton | `Crear prospecto` | `route` | `/conversations/:conversationId/prospects/new` | Flujo contextual. |
| `/conversations/:conversationId` | boton | `Crear pedido` | `route` | `/conversations/:conversationId/orders/new` | Flujo contextual. |
| `/conversations/:conversationId` | boton | `Crear cita` | `route` | `/conversations/:conversationId/appointments/new` | Flujo contextual. |
| `/conversations/:conversationId` | boton | `Volver` | `route` | `/conversations` | Regresa al listado. |
| `drawer de plantillas` | fila | clic en plantilla | `same-route` | `/conversations/:conversationId` | Inserta el contenido en composer. |
| `/conversations/new` | boton | `Crear conversacion` | `route` | `/conversations/:conversationId` | Navega al detalle creado. |
| `/conversations/new` | boton | `Cancelar` | `route` | `/conversations` | Sin persistencia. |
| `/templates` | boton | `Crear plantilla` | `route` | `/templates/new` | Alta directa. |
| `/templates` | boton | `Aplicar filtros` | `same-route` | `/templates` | Refresca listado. |
| `/templates` | boton | `Limpiar filtros` | `same-route` | `/templates` | Restablece filtros. |
| `/templates` | fila o accion inline | `Activar o desactivar` | `same-route` | `/templates` | Cambia estado. |
| `/templates` | accion inline | `Eliminar` | `modal` | `modal://confirm-delete` | Confirmacion obligatoria. |
| `/templates/new` | boton | `Guardar plantilla` | `route` | `/templates` | Vuelve al listado. |
| `/templates/new` | boton | `Cancelar` | `route` | `/templates` | Sin persistencia. |

## Reglas de automatizacion

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `/automation-rules` | boton | `Crear regla` | `route` | `/automation-rules/new` | Alta directa. |
| `/automation-rules` | boton | `Aplicar filtros` | `same-route` | `/automation-rules` | Refresca tabla. |
| `/automation-rules` | boton | `Limpiar filtros` | `same-route` | `/automation-rules` | Restablece filtros. |
| `/automation-rules` | accion inline | `Editar` | `route` | `/automation-rules/:ruleId/edit` | Carga formulario precargado. |
| `/automation-rules` | accion inline | `Probar` | `route` | `/automation-rules/:ruleId/test` | Pantalla de simulacion. |
| `/automation-rules` | accion inline | `Activar o desactivar` | `same-route` | `/automation-rules` | Muestra toast. |
| `/automation-rules/new` | boton | `Guardar regla` | `route` | `/automation-rules` | Vuelve al listado. |
| `/automation-rules/new` | boton | `Cancelar` | `route` | `/automation-rules` | Sin persistencia. |
| `/automation-rules/:ruleId/edit` | boton | `Guardar cambios` | `route` | `/automation-rules` | Persistencia exitosa. |
| `/automation-rules/:ruleId/edit` | boton | `Cancelar` | `route` | `/automation-rules` | Sin persistencia. |
| `/automation-rules/:ruleId/test` | boton | `Ejecutar prueba` | `same-route` | `/automation-rules/:ruleId/test` | Muestra resultado en la misma vista. |
| `/automation-rules/:ruleId/test` | boton | `Volver a reglas` | `route` | `/automation-rules` | Cierra simulacion. |

## Prospectos, agenda y pedidos

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `/prospects` | boton | `Crear prospecto` | `route` | `/prospects/new` | Alta manual. |
| `/prospects` | boton | `Aplicar filtros` | `same-route` | `/prospects` | Refresca tabla. |
| `/prospects` | boton | `Limpiar filtros` | `same-route` | `/prospects` | Restablece filtros. |
| `/prospects` | fila | clic en fila | `route` | `/prospects/:prospectId` | Abre detalle. |
| `/prospects/new` | boton | `Guardar prospecto` | `route` | `/prospects/:prospectId` | Detalle del nuevo prospecto. |
| `/prospects/new` | boton | `Cancelar` | `route` | `/prospects` | Sin persistencia. |
| `/prospects/:prospectId` | boton | `Editar` | `route` | `/prospects/:prospectId/edit` | Formulario precargado. |
| `/prospects/:prospectId` | boton | `Crear cita` | `route` | `/appointments/new?prospectId=:prospectId` | Flujo contextual. |
| `/prospects/:prospectId` | boton | `Crear pedido` | `route` | `/orders/new?prospectId=:prospectId` | Flujo contextual. |
| `/prospects/:prospectId` | boton | `Volver` | `route` | `/prospects` | Regresa al listado. |
| `/prospects/:prospectId/edit` | boton | `Guardar cambios` | `route` | `/prospects/:prospectId` | Vuelve al detalle. |
| `/prospects/:prospectId/edit` | boton | `Cancelar` | `route` | `/prospects/:prospectId` | Sin persistencia. |
| `/appointments` | boton | `Crear cita` | `route` | `/appointments/new` | Alta manual. |
| `/appointments` | boton | `Aplicar filtros` | `same-route` | `/appointments` | Refresca tabla. |
| `/appointments` | boton | `Limpiar filtros` | `same-route` | `/appointments` | Restablece filtros. |
| `/appointments` | fila | clic en fila | `route` | `/appointments/:appointmentId` | Abre detalle. |
| `/appointments/new` | boton | `Guardar cita` | `route` | `/appointments/:appointmentId` | Navega al detalle. |
| `/appointments/new` | boton | `Cancelar` | `route` | `/appointments` | Sin persistencia. |
| `/appointments/:appointmentId` | boton | `Reprogramar` | `route` | `/appointments/:appointmentId/reschedule` | Flujo especifico. |
| `/appointments/:appointmentId` | boton | `Marcar completada` | `same-route` | `/appointments/:appointmentId` | Actualiza estado. |
| `/appointments/:appointmentId` | boton | `Volver` | `route` | `/appointments` | Regresa al listado. |
| `/appointments/:appointmentId/reschedule` | boton | `Guardar nueva fecha` | `route` | `/appointments/:appointmentId` | Vuelve al detalle. |
| `/appointments/:appointmentId/reschedule` | boton | `Cancelar` | `route` | `/appointments/:appointmentId` | Sin persistencia. |
| `/orders` | boton | `Crear pedido` | `route` | `/orders/new` | Alta manual. |
| `/orders` | boton | `Aplicar filtros` | `same-route` | `/orders` | Refresca tabla. |
| `/orders` | boton | `Limpiar filtros` | `same-route` | `/orders` | Restablece filtros. |
| `/orders` | fila | clic en fila | `route` | `/orders/:orderId` | Abre detalle. |
| `/orders/new` | boton | `Guardar pedido` | `route` | `/orders/:orderId` | Detalle del nuevo pedido. |
| `/orders/new` | boton | `Cancelar` | `route` | `/orders` | Sin persistencia. |
| `/orders/:orderId` | boton | `Registrar pago` | `route` | `/orders/:orderId/payments/new` | Flujo contextual. |
| `/orders/:orderId` | boton | `Cambiar estado` | `same-route` | `/orders/:orderId` | Actualiza badge y saldo. |
| `/orders/:orderId` | boton | `Volver` | `route` | `/orders` | Regresa al listado. |
| `/orders/:orderId/payments/new` | boton | `Registrar pago` | `route` | `/orders/:orderId` | Vuelve al detalle. |
| `/orders/:orderId/payments/new` | boton | `Cancelar` | `route` | `/orders/:orderId` | Sin persistencia. |

## Catalogo, reportes y administracion

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| `/catalog` | boton | `Crear producto` | `route` | `/catalog/products/new` | Alta manual. |
| `/catalog` | boton | `Aplicar filtros` | `same-route` | `/catalog` | Refresca tabla. |
| `/catalog` | boton | `Limpiar filtros` | `same-route` | `/catalog` | Restablece filtros. |
| `/catalog` | accion inline | `Editar` | `route` | `/catalog/products/:productId/edit` | Formulario precargado. |
| `/catalog` | accion inline | `Activar o desactivar` | `same-route` | `/catalog` | Cambio de estado. |
| `/catalog/products/new` | boton | `Guardar producto` | `route` | `/catalog` | Vuelve al listado. |
| `/catalog/products/new` | boton | `Cancelar` | `route` | `/catalog` | Sin persistencia. |
| `/catalog/products/:productId/edit` | boton | `Guardar cambios` | `route` | `/catalog` | Vuelve al listado. |
| `/catalog/products/:productId/edit` | boton | `Cancelar` | `route` | `/catalog` | Sin persistencia. |
| `/reports` | boton | `Aplicar filtros` | `same-route` | `/reports` | Recalcula metricas. |
| `/reports` | boton | `Limpiar filtros` | `same-route` | `/reports` | Restablece filtros. |
| `/reports` | tarjeta o grafico | `Ver conversaciones` | `route` | `/conversations` | Puede arrastrar rango. |
| `/reports` | tarjeta o grafico | `Ver pedidos` | `route` | `/orders` | Puede arrastrar rango. |
| `/reports` | tarjeta o grafico | `Ver agenda` | `route` | `/appointments` | Puede arrastrar rango. |
| `/admin` | tarjeta | `Configuracion de empresa` | `route` | `/admin/company` | Configuracion corporativa. |
| `/admin` | tarjeta | `Usuarios y roles` | `route` | `/admin/users` | Gestion de usuarios. |
| `/admin` | tarjeta | `Conexion WhatsApp Web` | `route` | `/admin/whatsapp-web` | Estado del canal experimental. |
| `/admin` | tarjeta | `Seguridad` | `route` | `/admin/security` | Politicas de seguridad. |
| `/admin/company` | boton | `Guardar cambios` | `same-route` | `/admin/company` | Muestra toast. |
| `/admin/company` | boton | `Cancelar` | `route` | `/admin` | Sin persistencia. |
| `/admin/users` | boton | `Crear usuario` | `route` | `/admin/users/new` | Alta manual. |
| `/admin/users` | boton | `Aplicar filtros` | `same-route` | `/admin/users` | Refresca tabla. |
| `/admin/users` | boton | `Limpiar filtros` | `same-route` | `/admin/users` | Restablece filtros. |
| `/admin/users` | fila o accion inline | `Editar` | `route` | `/admin/users/:userId/edit` | Formulario precargado. |
| `/admin/users` | accion inline | `Activar o desactivar` | `same-route` | `/admin/users` | Cambio de estado. |
| `/admin/users/new` | boton | `Guardar usuario` | `route` | `/admin/users` | Vuelve al listado. |
| `/admin/users/new` | boton | `Cancelar` | `route` | `/admin/users` | Sin persistencia. |
| `/admin/users/:userId/edit` | boton | `Guardar cambios` | `route` | `/admin/users` | Vuelve al listado. |
| `/admin/users/:userId/edit` | boton | `Cancelar` | `route` | `/admin/users` | Sin persistencia. |
| `/admin/whatsapp-web` | boton | `Conectar` | `same-route` | `/admin/whatsapp-web` | Solicita nueva sesion o QR. |
| `/admin/whatsapp-web` | boton | `Refrescar QR` | `same-route` | `/admin/whatsapp-web` | Pide nuevo QR. |
| `/admin/whatsapp-web` | boton | `Desconectar` | `modal` | `modal://confirm-disconnect-whatsapp-web` | Confirmacion obligatoria. |
| `/admin/whatsapp-web` | boton | `Reintentar` | `same-route` | `/admin/whatsapp-web` | Reconsulta estado del servicio. |
| `modal://confirm-disconnect-whatsapp-web` | boton | `Confirmar desconexion` | `same-route` | `/admin/whatsapp-web` | Ejecuta corte de sesion. |
| `modal://confirm-disconnect-whatsapp-web` | boton | `Cancelar` | `same-route` | `/admin/whatsapp-web` | Cierra modal. |
| `/admin/security` | boton | `Guardar cambios` | `same-route` | `/admin/security` | Muestra toast. |
| `/admin/security` | boton | `Cancelar` | `route` | `/admin` | Sin persistencia. |

## Estados transversales

| Origen | Tipo de control | Etiqueta o trigger | Destino | Ruta, modal o estado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| Cualquier listado sin datos | estado | `EmptyState` | `state` | `state://empty` | Debe proponer CTA primaria. |
| Cualquier consulta en curso | estado | `LoadingState` | `state` | `state://loading` | Mantiene estructura visual. |
| Cualquier fallo recuperable | estado | `ErrorState` | `state` | `state://error` | Ofrece `Reintentar` y `Volver`. |
| Cualquier perdida de red | estado | `OfflineBanner` | `state` | `state://offline` | Puede convivir con cache. |
| Cualquier mutacion exitosa | feedback | `Toast` de exito | `state` | `toast://saved` | Confirmacion transversal. |
| Cualquier eliminacion o desactivacion sensible | modal | `ConfirmDialog` | `modal` | `modal://confirm-delete` | Antes de borrar o desactivar. |
