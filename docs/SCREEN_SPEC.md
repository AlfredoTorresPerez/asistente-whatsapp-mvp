# SCREEN_SPEC

## Convenciones globales

- Todos los endpoints indicados usan prefijo `/api/v1`.
- Las pantallas privadas renderizan `AppLayout`, `Sidebar`, `Topbar` y `OfflineBanner`.
- Las pantallas publicas renderizan `PublicLayout`.
- `Cancelar` siempre vuelve a la vista anterior o al listado de origen sin persistir.
- `Guardar` siempre persiste, muestra confirmacion visual y navega al destino definido.

## 01. Inicio de sesion

- Ruta: `/login`
- Tipo: pagina publica
- Componentes visibles: `PublicLayout`, logo, tarjeta de login, `Input` email, `Input` password, `Button` ingresar, enlace recuperar contrasena
- Botones: `Ingresar`, `Recuperar contrasena`
- Campos: `email`, `password`
- Validaciones: `email` requerido y formato email; `password` requerida, minimo 8, maximo 72
- Acciones: enviar credenciales; navegar a recuperacion
- Destino de cada accion: `Ingresar` -> `/dashboard`; `Recuperar contrasena` -> `/forgot-password`
- Endpoint asociado: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`
- Estado vacio: no aplica
- Estado de carga: bloquear formulario y mostrar spinner en `Ingresar`
- Estado de error: mensaje inline por credenciales invalidas o cuenta inactiva
- Estado sin conexion: mostrar `OfflineBanner` y deshabilitar submit
- Comportamiento al cancelar: no aplica
- Comportamiento al guardar: guardar token de acceso, cargar perfil y redirigir a `/dashboard`

## 02. Recuperar contrasena

- Ruta: `/forgot-password`
- Tipo: pagina publica
- Componentes visibles: `PublicLayout`, tarjeta de formulario, `Input` email, texto de ayuda
- Botones: `Enviar enlace`, `Volver al inicio`
- Campos: `email`
- Validaciones: requerido, formato email
- Acciones: solicitar recuperacion; volver a login
- Destino de cada accion: `Enviar enlace` -> `/forgot-password/sent`; `Volver al inicio` -> `/login`
- Endpoint asociado: `POST /api/v1/auth/forgot-password`
- Estado vacio: no aplica
- Estado de carga: boton `Enviar enlace` en loading
- Estado de error: mensaje inline si el correo no puede procesarse
- Estado sin conexion: banner offline y submit bloqueado
- Comportamiento al cancelar: vuelve a `/login`
- Comportamiento al guardar: persiste solicitud y navega a confirmacion de correo enviado

## 03. Confirmacion de correo enviado

- Ruta: `/forgot-password/sent`
- Tipo: pagina publica
- Componentes visibles: `PublicLayout`, mensaje de exito, resumen del correo destino, ayuda para revisar spam
- Botones: `Volver al inicio`, `Reenviar enlace`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: volver a login; reenviar correo de recuperacion
- Destino de cada accion: `Volver al inicio` -> `/login`; `Reenviar enlace` -> permanece en `/forgot-password/sent`
- Endpoint asociado: `POST /api/v1/auth/forgot-password`
- Estado vacio: no aplica
- Estado de carga: mostrar spinner solo en `Reenviar enlace`
- Estado de error: toast si el reenvio falla
- Estado sin conexion: banner offline y reenvio deshabilitado
- Comportamiento al cancelar: vuelve a `/login`
- Comportamiento al guardar: no aplica; al reenviar muestra toast de confirmacion

## 04. Restablecer contrasena

- Ruta: `/reset-password?token=:token`
- Tipo: pagina publica
- Componentes visibles: `PublicLayout`, tarjeta de formulario, `Input` nueva contrasena, `Input` confirmar contrasena
- Botones: `Guardar nueva contrasena`, `Cancelar`
- Campos: `token`, `newPassword`, `confirmPassword`
- Validaciones: `token` valido; `newPassword` minimo 8, con reglas activas; `confirmPassword` debe coincidir
- Acciones: validar token; guardar nueva contrasena; cancelar
- Destino de cada accion: `Guardar nueva contrasena` -> `/login`; `Cancelar` -> `/login`
- Endpoint asociado: `GET /api/v1/auth/reset-password/validate`, `POST /api/v1/auth/reset-password`
- Estado vacio: no aplica
- Estado de carga: validar token al entrar y bloquear submit mientras guarda
- Estado de error: token expirado o contrasena invalida
- Estado sin conexion: banner offline y submit bloqueado
- Comportamiento al cancelar: vuelve a `/login`
- Comportamiento al guardar: actualiza credencial, muestra confirmacion y redirige a `/login`

## 05. Panel principal

- Ruta: `/dashboard`
- Tipo: pagina privada
- Componentes visibles: cards KPI, grafico de conversaciones, grafico de pedidos, agenda del dia, actividad reciente
- Botones: `Ver conversaciones`, `Ver prospectos`, `Ver agenda`, `Ver pedidos`, `Actualizar`
- Campos: `dateRange`, `ownerUserId` opcional
- Validaciones: rango de fechas valido; `dateRange` obligatorio
- Acciones: filtrar datos; navegar a modulos operativos
- Destino de cada accion: `Ver conversaciones` -> `/conversations`; `Ver prospectos` -> `/prospects`; `Ver agenda` -> `/appointments`; `Ver pedidos` -> `/orders`; `Actualizar` -> permanece en `/dashboard`
- Endpoint asociado: `GET /api/v1/dashboard/summary`
- Estado vacio: mostrar `EmptyState` con mensaje "Aun no hay actividad registrada"
- Estado de carga: skeleton de cards, graficos y tabla
- Estado de error: `ErrorState` con opcion `Reintentar`
- Estado sin conexion: banner offline y ultimo snapshot cacheado
- Comportamiento al cancelar: limpia filtros no guardados y mantiene `/dashboard`
- Comportamiento al guardar: no aplica; al refrescar solo actualiza datos

## 06. Centro de notificaciones basico

- Ruta: `/notifications`
- Tipo: pagina privada
- Componentes visibles: lista de notificaciones, filtros, tabs `Todas` y `No leidas`
- Botones: `Marcar todas como leidas`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `status`, `type`, `search`
- Validaciones: `search` maximo 80 caracteres
- Acciones: filtrar; marcar una o todas como leidas; abrir entidad relacionada
- Destino de cada accion: clic en notificacion -> detalle relacionado; `Marcar todas como leidas` -> permanece en `/notifications`
- Endpoint asociado: `GET /api/v1/notifications`, `PATCH /api/v1/notifications/{notificationId}/read`, `PATCH /api/v1/notifications/read-all`
- Estado vacio: `EmptyState` con mensaje "No tienes notificaciones"
- Estado de carga: skeleton de lista
- Estado de error: `ErrorState` con reintento
- Estado sin conexion: banner offline y marcado como leido deshabilitado
- Comportamiento al cancelar: restablece filtros
- Comportamiento al guardar: no aplica; al marcar leidas actualiza la misma pantalla

## 07. Menu de usuario

- Ruta: `overlay://user-menu`
- Tipo: overlay contextual
- Componentes visibles: resumen de usuario, accesos rapidos, accion de cierre de sesion
- Botones: `Ver perfil`, `Cambiar contrasena`, `Notificaciones`, `Cerrar sesion`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: navegar a perfil, cambio de contrasena, notificaciones o confirmacion de logout
- Destino de cada accion: `Ver perfil` -> `/profile`; `Cambiar contrasena` -> `/profile/change-password`; `Notificaciones` -> `/notifications`; `Cerrar sesion` -> `modal://logout-confirmed`
- Endpoint asociado: `GET /api/v1/auth/me`
- Estado vacio: no aplica
- Estado de carga: mostrar skeleton del bloque de usuario si el perfil no esta cargado
- Estado de error: cerrar menu y mostrar toast si no se puede cargar el perfil
- Estado sin conexion: permitir solo rutas ya disponibles localmente
- Comportamiento al cancelar: cerrar overlay
- Comportamiento al guardar: no aplica

## 08. Perfil de usuario

- Ruta: `/profile`
- Tipo: pagina privada
- Componentes visibles: formulario de perfil, resumen de rol, datos de contacto
- Botones: `Guardar cambios`, `Cancelar`, `Cambiar contrasena`
- Campos: `firstName`, `lastName`, `email` solo lectura, `phone`, `timezone`
- Validaciones: `firstName` y `lastName` requeridos; `phone` formato internacional; `timezone` requerida
- Acciones: editar perfil; navegar a cambio de contrasena
- Destino de cada accion: `Guardar cambios` -> permanece en `/profile`; `Cancelar` -> `/dashboard`; `Cambiar contrasena` -> `/profile/change-password`
- Endpoint asociado: `GET /api/v1/users/me`, `PATCH /api/v1/users/me`
- Estado vacio: no aplica
- Estado de carga: skeleton de formulario
- Estado de error: mensaje inline por campo o `ErrorState` de pagina
- Estado sin conexion: banner offline y guardado deshabilitado
- Comportamiento al cancelar: descarta cambios y vuelve a `/dashboard`
- Comportamiento al guardar: actualiza perfil, muestra toast y permanece en `/profile`

## 09. Conversaciones

- Ruta: `/conversations`
- Tipo: pagina privada
- Componentes visibles: `DataTable`, filtros, contador de no leidas, acceso a nueva conversacion
- Botones: `Nueva conversacion`, `Aplicar filtros`, `Limpiar filtros`, `Actualizar`
- Campos: `search`, `status`, `ownerUserId`, `unreadOnly`, `dateRange`
- Validaciones: `search` maximo 120 caracteres; rango de fechas valido
- Acciones: listar, filtrar, abrir detalle, refrescar
- Destino de cada accion: clic en fila -> `/conversations/:conversationId`; `Nueva conversacion` -> `/conversations/new`
- Endpoint asociado: `GET /api/v1/conversations`
- Estado vacio: `EmptyState` con CTA `Nueva conversacion`
- Estado de carga: skeleton de tabla
- Estado de error: `ErrorState` con `Reintentar`
- Estado sin conexion: banner offline y tabla cacheada si existe
- Comportamiento al cancelar: limpia filtros
- Comportamiento al guardar: no aplica

## 10. Detalle de conversacion

- Ruta: `/conversations/:conversationId`
- Tipo: pagina privada
- Componentes visibles: hilo de mensajes, composer, resumen del contacto, acciones contextuales, acceso a plantillas
- Botones: `Enviar mensaje`, `Usar plantilla`, `Crear prospecto`, `Crear pedido`, `Crear cita`, `Volver`
- Campos: `messageBody`, `templateId` opcional
- Validaciones: `messageBody` requerido si no hay plantilla, maximo 1000 caracteres; `templateId` debe existir y estar activa
- Acciones: enviar mensaje, abrir drawer de plantillas, iniciar creacion de entidades relacionadas
- Destino de cada accion: `Enviar mensaje` -> permanece en la misma ruta; `Usar plantilla` -> drawer contextual; `Crear prospecto` -> `/conversations/:conversationId/prospects/new`; `Crear pedido` -> `/conversations/:conversationId/orders/new`; `Crear cita` -> `/conversations/:conversationId/appointments/new`; `Volver` -> `/conversations`
- Endpoint asociado: `GET /api/v1/conversations/{conversationId}`, `GET /api/v1/conversations/{conversationId}/messages`, `POST /api/v1/conversations/{conversationId}/messages`, `GET /api/v1/templates?active=true`
- Estado vacio: mostrar hilo vacio con sugerencia para enviar el primer mensaje
- Estado de carga: skeleton del detalle y spinner en el composer al enviar
- Estado de error: error de carga del hilo o del envio del mensaje
- Estado sin conexion: banner offline, polling detenido y envio deshabilitado
- Comportamiento al cancelar: cierra drawer o vuelve a `/conversations`
- Comportamiento al guardar: limpia composer, actualiza hilo y muestra toast de mensaje enviado

## 11. Nueva conversacion

- Ruta: `/conversations/new`
- Tipo: pagina privada
- Componentes visibles: formulario de nueva conversacion
- Botones: `Crear conversacion`, `Cancelar`
- Campos: `customerName`, `customerPhone`, `ownerUserId`, `initialMessage` opcional
- Validaciones: `customerName` requerido; `customerPhone` requerido en formato E.164; `initialMessage` maximo 1000 caracteres
- Acciones: crear conversacion; cancelar
- Destino de cada accion: `Crear conversacion` -> `/conversations/:conversationId`; `Cancelar` -> `/conversations`
- Endpoint asociado: `POST /api/v1/conversations`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: errores de validacion o numero invalido
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve a `/conversations`
- Comportamiento al guardar: crea la conversacion y redirige al detalle

## 12. Plantillas de respuesta

- Ruta: `/templates`
- Tipo: pagina privada
- Componentes visibles: listado de plantillas, filtros, acciones por fila
- Botones: `Crear plantilla`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `search`, `category`, `active`
- Validaciones: `search` maximo 80 caracteres
- Acciones: listar, filtrar, activar/desactivar, eliminar
- Destino de cada accion: `Crear plantilla` -> `/templates/new`; clic en fila -> permanece en `/templates` con acciones inline
- Endpoint asociado: `GET /api/v1/templates`, `PATCH /api/v1/templates/{templateId}`, `DELETE /api/v1/templates/{templateId}`
- Estado vacio: `EmptyState` con CTA `Crear plantilla`
- Estado de carga: skeleton de tabla
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y acciones de mutacion bloqueadas
- Comportamiento al cancelar: limpia filtros o cierra confirmacion
- Comportamiento al guardar: no aplica; los cambios inline muestran toast y mantienen la ruta

## 13. Crear plantilla

- Ruta: `/templates/new`
- Tipo: pagina privada
- Componentes visibles: formulario de plantilla, ayuda sobre variables soportadas
- Botones: `Guardar plantilla`, `Cancelar`
- Campos: `name`, `category`, `body`, `active`
- Validaciones: `name` requerido maximo 80; `category` requerida; `body` requerido maximo 1000
- Acciones: crear plantilla; cancelar
- Destino de cada accion: `Guardar plantilla` -> `/templates`; `Cancelar` -> `/templates`
- Endpoint asociado: `POST /api/v1/templates`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: errores inline por validacion o cuerpo duplicado
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve a `/templates`
- Comportamiento al guardar: crea plantilla, muestra toast y vuelve al listado

## 14. Crear prospecto desde conversacion

- Ruta: `/conversations/:conversationId/prospects/new`
- Tipo: pagina privada
- Componentes visibles: formulario con datos precargados desde la conversacion
- Botones: `Guardar prospecto`, `Cancelar`
- Campos: `firstName`, `lastName`, `phone`, `email`, `stage`, `notes`, `assignedUserId`
- Validaciones: `firstName` requerido; `phone` requerido; `email` formato valido si se informa; `stage` requerida
- Acciones: crear prospecto vinculado; cancelar
- Destino de cada accion: `Guardar prospecto` -> `/prospects/:prospectId`; `Cancelar` -> `/conversations/:conversationId`
- Endpoint asociado: `POST /api/v1/conversations/{conversationId}/prospects`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: errores inline o conflicto por telefono duplicado
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al detalle de conversacion
- Comportamiento al guardar: crea prospecto, vincula la conversacion y redirige al detalle del prospecto

## 15. Crear pedido desde conversacion

- Ruta: `/conversations/:conversationId/orders/new`
- Tipo: pagina privada
- Componentes visibles: formulario de pedido, selector de productos, resumen de totales
- Botones: `Guardar pedido`, `Cancelar`, `Agregar producto`
- Campos: `prospectId` opcional, `items[]`, `notes`, `dueDate`
- Validaciones: al menos un item; cada item con `productId`, `quantity > 0`, `unitPrice >= 0`; `dueDate` valida si se informa
- Acciones: crear pedido vinculado; agregar o quitar items; cancelar
- Destino de cada accion: `Guardar pedido` -> `/orders/:orderId`; `Cancelar` -> `/conversations/:conversationId`
- Endpoint asociado: `GET /api/v1/products?active=true`, `POST /api/v1/conversations/{conversationId}/orders`
- Estado vacio: si no hay productos, mostrar CTA a `/catalog/products/new`
- Estado de carga: submit y carga del catalogo
- Estado de error: error de catalogo o validaciones de items
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al detalle de conversacion
- Comportamiento al guardar: crea pedido, recalcula totales y navega al detalle

## 16. Crear cita desde conversacion

- Ruta: `/conversations/:conversationId/appointments/new`
- Tipo: pagina privada
- Componentes visibles: formulario de cita, ayuda contextual del prospecto o contacto
- Botones: `Guardar cita`, `Cancelar`
- Campos: `subject`, `prospectId` opcional, `startsAt`, `durationMinutes`, `location`, `notes`
- Validaciones: `subject` requerido; `startsAt` requerida y futura; `durationMinutes` requerido y mayor a 0
- Acciones: crear cita vinculada; cancelar
- Destino de cada accion: `Guardar cita` -> `/appointments/:appointmentId`; `Cancelar` -> `/conversations/:conversationId`
- Endpoint asociado: `POST /api/v1/conversations/{conversationId}/appointments`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: conflictos de horario o validaciones inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al detalle de conversacion
- Comportamiento al guardar: crea cita y redirige al detalle

## 17. Reglas de automatizacion

- Ruta: `/automation-rules`
- Tipo: pagina privada
- Componentes visibles: tabla de reglas, filtros por trigger y estado, acciones por fila
- Botones: `Crear regla`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `search`, `triggerType`, `active`
- Validaciones: `search` maximo 80 caracteres
- Acciones: listar, filtrar, activar/desactivar, editar, probar
- Destino de cada accion: `Crear regla` -> `/automation-rules/new`; `Editar` -> `/automation-rules/:ruleId/edit`; `Probar` -> `/automation-rules/:ruleId/test`
- Endpoint asociado: `GET /api/v1/automation-rules`, `PATCH /api/v1/automation-rules/{ruleId}`, `PATCH /api/v1/automation-rules/{ruleId}/status`
- Estado vacio: `EmptyState` con CTA `Crear regla`
- Estado de carga: skeleton de tabla
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y acciones de cambio deshabilitadas
- Comportamiento al cancelar: limpia filtros o cierra confirmacion
- Comportamiento al guardar: no aplica; cambios de estado muestran toast

## 18. Crear regla

- Ruta: `/automation-rules/new`
- Tipo: pagina privada
- Componentes visibles: formulario de regla, resumen de triggers soportados
- Botones: `Guardar regla`, `Cancelar`
- Campos: `name`, `triggerType`, `keyword`, `delayMinutes`, `actionType`, `templateId`, `assignedUserId`, `active`
- Validaciones: `name` requerido; `triggerType` requerido; `actionType` requerido; `templateId` requerido si `actionType=SEND_TEMPLATE`; `delayMinutes >= 0`
- Acciones: crear regla; cancelar
- Destino de cada accion: `Guardar regla` -> `/automation-rules`; `Cancelar` -> `/automation-rules`
- Endpoint asociado: `GET /api/v1/templates?active=true`, `GET /api/v1/admin/users?active=true`, `POST /api/v1/automation-rules`
- Estado vacio: no aplica
- Estado de carga: submit y carga de datos auxiliares
- Estado de error: validaciones inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al listado de reglas
- Comportamiento al guardar: crea regla, muestra toast y vuelve al listado

## 19. Editar regla

- Ruta: `/automation-rules/:ruleId/edit`
- Tipo: pagina privada
- Componentes visibles: mismo formulario de regla con datos precargados
- Botones: `Guardar cambios`, `Cancelar`
- Campos: `name`, `triggerType`, `keyword`, `delayMinutes`, `actionType`, `templateId`, `assignedUserId`, `active`
- Validaciones: mismas de crear regla
- Acciones: editar regla; cancelar
- Destino de cada accion: `Guardar cambios` -> `/automation-rules`; `Cancelar` -> `/automation-rules`
- Endpoint asociado: `GET /api/v1/automation-rules/{ruleId}`, `GET /api/v1/templates?active=true`, `GET /api/v1/admin/users?active=true`, `PATCH /api/v1/automation-rules/{ruleId}`
- Estado vacio: no aplica
- Estado de carga: skeleton de formulario
- Estado de error: `ErrorState` o errores inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al listado
- Comportamiento al guardar: actualiza regla, muestra toast y vuelve al listado

## 20. Probar regla

- Ruta: `/automation-rules/:ruleId/test`
- Tipo: pagina privada
- Componentes visibles: formulario de prueba, visor de resultado, panel con la regla activa
- Botones: `Ejecutar prueba`, `Cancelar`
- Campos: `conversationId`, `sampleMessage`, `executedAt`
- Validaciones: `conversationId` requerido; `sampleMessage` requerido si el trigger depende de contenido
- Acciones: ejecutar simulacion; cancelar
- Destino de cada accion: `Ejecutar prueba` -> permanece en `/automation-rules/:ruleId/test`; `Cancelar` -> `/automation-rules`
- Endpoint asociado: `POST /api/v1/automation-rules/{ruleId}/test`
- Estado vacio: resultado vacio hasta correr la primera prueba
- Estado de carga: boton en loading y placeholder del resultado
- Estado de error: mostrar detalle funcional del fallo
- Estado sin conexion: banner offline y prueba deshabilitada
- Comportamiento al cancelar: vuelve a `/automation-rules`
- Comportamiento al guardar: no aplica; la prueba no persiste cambios en la regla

## 21. Prospectos

- Ruta: `/prospects`
- Tipo: pagina privada
- Componentes visibles: tabla de prospectos, filtros, metricas ligeras
- Botones: `Crear prospecto`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `search`, `stage`, `assignedUserId`, `source`
- Validaciones: `search` maximo 120 caracteres
- Acciones: listar, filtrar, abrir detalle
- Destino de cada accion: `Crear prospecto` -> `/prospects/new`; clic en fila -> `/prospects/:prospectId`
- Endpoint asociado: `GET /api/v1/prospects`
- Estado vacio: `EmptyState` con CTA `Crear prospecto`
- Estado de carga: skeleton de tabla
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y cache local si existe
- Comportamiento al cancelar: limpia filtros
- Comportamiento al guardar: no aplica

## 22. Crear prospecto

- Ruta: `/prospects/new`
- Tipo: pagina privada
- Componentes visibles: formulario de prospecto
- Botones: `Guardar prospecto`, `Cancelar`
- Campos: `firstName`, `lastName`, `phone`, `email`, `stage`, `source`, `notes`, `assignedUserId`
- Validaciones: `firstName`, `phone`, `stage` y `source` requeridos; `email` valido si se informa
- Acciones: crear prospecto; cancelar
- Destino de cada accion: `Guardar prospecto` -> `/prospects/:prospectId`; `Cancelar` -> `/prospects`
- Endpoint asociado: `GET /api/v1/admin/users?active=true`, `POST /api/v1/prospects`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: errores inline o telefono duplicado
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve a `/prospects`
- Comportamiento al guardar: crea prospecto y redirige al detalle

## 23. Detalle de prospecto

- Ruta: `/prospects/:prospectId`
- Tipo: pagina privada
- Componentes visibles: resumen del prospecto, timeline, conversaciones relacionadas, pedidos, citas
- Botones: `Editar prospecto`, `Crear pedido`, `Crear cita`, `Ver conversacion`, `Volver`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: abrir edicion, crear entidades relacionadas, navegar a conversaciones
- Destino de cada accion: `Editar prospecto` -> `/prospects/:prospectId/edit`; `Crear pedido` -> `/orders/new?prospectId=:prospectId`; `Crear cita` -> `/appointments/new?prospectId=:prospectId`; `Ver conversacion` -> `/conversations/:conversationId`; `Volver` -> `/prospects`
- Endpoint asociado: `GET /api/v1/prospects/{prospectId}`
- Estado vacio: si no tiene relaciones, mostrar cards vacias con CTA de creacion
- Estado de carga: skeleton de detalle
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y datos cacheados si existen
- Comportamiento al cancelar: vuelve al listado
- Comportamiento al guardar: no aplica

## 24. Editar prospecto

- Ruta: `/prospects/:prospectId/edit`
- Tipo: pagina privada
- Componentes visibles: formulario precargado del prospecto
- Botones: `Guardar cambios`, `Cancelar`
- Campos: `firstName`, `lastName`, `phone`, `email`, `stage`, `source`, `notes`, `assignedUserId`
- Validaciones: mismas de crear prospecto
- Acciones: editar prospecto; cancelar
- Destino de cada accion: `Guardar cambios` -> `/prospects/:prospectId`; `Cancelar` -> `/prospects/:prospectId`
- Endpoint asociado: `GET /api/v1/prospects/{prospectId}`, `PATCH /api/v1/prospects/{prospectId}`
- Estado vacio: no aplica
- Estado de carga: skeleton de formulario
- Estado de error: `ErrorState` o errores inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al detalle
- Comportamiento al guardar: actualiza prospecto y vuelve al detalle

## 25. Agenda

- Ruta: `/appointments`
- Tipo: pagina privada
- Componentes visibles: vista calendario o lista, filtros, resumen del dia
- Botones: `Crear cita`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `dateRange`, `assignedUserId`, `status`, `viewMode`
- Validaciones: rango valido; `viewMode` entre `list` o `calendar`
- Acciones: listar, filtrar, abrir detalle, crear cita
- Destino de cada accion: `Crear cita` -> `/appointments/new`; clic en fila o bloque -> `/appointments/:appointmentId`
- Endpoint asociado: `GET /api/v1/appointments`
- Estado vacio: `EmptyState` con CTA `Crear cita`
- Estado de carga: skeleton de calendario o lista
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y ultima agenda cacheada
- Comportamiento al cancelar: limpia filtros
- Comportamiento al guardar: no aplica

## 26. Crear cita

- Ruta: `/appointments/new`
- Tipo: pagina privada
- Componentes visibles: formulario de cita
- Botones: `Guardar cita`, `Cancelar`
- Campos: `subject`, `prospectId`, `startsAt`, `durationMinutes`, `location`, `notes`
- Validaciones: `subject`, `startsAt`, `durationMinutes` requeridos; fecha futura; `durationMinutes > 0`
- Acciones: crear cita; cancelar
- Destino de cada accion: `Guardar cita` -> `/appointments/:appointmentId`; `Cancelar` -> `/appointments`
- Endpoint asociado: `GET /api/v1/prospects`, `POST /api/v1/appointments`
- Estado vacio: si no hay prospectos, mantener opcion de crear cita sin prospecto
- Estado de carga: submit y carga de prospectos
- Estado de error: conflictos de agenda o validaciones inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve a agenda
- Comportamiento al guardar: crea cita y redirige al detalle

## 27. Detalle de cita

- Ruta: `/appointments/:appointmentId`
- Tipo: pagina privada
- Componentes visibles: resumen de cita, datos del prospecto, estado, notas, enlaces relacionados
- Botones: `Reprogramar`, `Marcar completada`, `Cancelar cita`, `Volver`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: reprogramar; cambiar estado; volver
- Destino de cada accion: `Reprogramar` -> `/appointments/:appointmentId/reschedule`; `Marcar completada` -> permanece en `/appointments/:appointmentId`; `Cancelar cita` -> permanece en `/appointments/:appointmentId`; `Volver` -> `/appointments`
- Endpoint asociado: `GET /api/v1/appointments/{appointmentId}`, `PATCH /api/v1/appointments/{appointmentId}`
- Estado vacio: no aplica
- Estado de carga: skeleton de detalle
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y mutaciones bloqueadas
- Comportamiento al cancelar: vuelve al listado o cierra dialogo
- Comportamiento al guardar: no aplica; cambios de estado muestran toast y refrescan el detalle

## 28. Reprogramar cita

- Ruta: `/appointments/:appointmentId/reschedule`
- Tipo: pagina privada
- Componentes visibles: formulario corto con fecha nueva y motivo
- Botones: `Guardar reprogramacion`, `Cancelar`
- Campos: `startsAt`, `reason`
- Validaciones: `startsAt` requerido y futuro; `reason` requerido maximo 250
- Acciones: reprogramar cita; cancelar
- Destino de cada accion: `Guardar reprogramacion` -> `/appointments/:appointmentId`; `Cancelar` -> `/appointments/:appointmentId`
- Endpoint asociado: `PATCH /api/v1/appointments/{appointmentId}/reschedule`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: conflicto de horario o validacion inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al detalle
- Comportamiento al guardar: actualiza fecha, muestra toast y vuelve al detalle

## 29. Pedidos

- Ruta: `/orders`
- Tipo: pagina privada
- Componentes visibles: tabla de pedidos, filtros, resumen de cobranza
- Botones: `Crear pedido`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `search`, `status`, `paymentStatus`, `dateRange`
- Validaciones: rango de fechas valido; `search` maximo 120 caracteres
- Acciones: listar, filtrar, abrir detalle
- Destino de cada accion: `Crear pedido` -> `/orders/new`; clic en fila -> `/orders/:orderId`
- Endpoint asociado: `GET /api/v1/orders`
- Estado vacio: `EmptyState` con CTA `Crear pedido`
- Estado de carga: skeleton de tabla
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y ultimo resultado cacheado
- Comportamiento al cancelar: limpia filtros
- Comportamiento al guardar: no aplica

## 30. Crear pedido

- Ruta: `/orders/new`
- Tipo: pagina privada
- Componentes visibles: formulario de pedido, selector de prospecto, items y totales
- Botones: `Guardar pedido`, `Cancelar`, `Agregar producto`
- Campos: `prospectId`, `conversationId` opcional, `items[]`, `notes`, `dueDate`
- Validaciones: al menos un item; `quantity > 0`; `unitPrice >= 0`
- Acciones: crear pedido; manipular items; cancelar
- Destino de cada accion: `Guardar pedido` -> `/orders/:orderId`; `Cancelar` -> `/orders`
- Endpoint asociado: `GET /api/v1/prospects`, `GET /api/v1/products?active=true`, `POST /api/v1/orders`
- Estado vacio: si no hay productos, CTA a `/catalog/products/new`
- Estado de carga: submit y carga de catalogo
- Estado de error: errores de items, totales o validacion inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al listado
- Comportamiento al guardar: crea pedido y navega al detalle

## 31. Detalle de pedido

- Ruta: `/orders/:orderId`
- Tipo: pagina privada
- Componentes visibles: resumen del pedido, items, pagos, saldo, prospecto relacionado
- Botones: `Registrar pago`, `Cambiar estado`, `Volver`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: registrar pago; cambiar estado del pedido; volver
- Destino de cada accion: `Registrar pago` -> `/orders/:orderId/payments/new`; `Cambiar estado` -> permanece en `/orders/:orderId`; `Volver` -> `/orders`
- Endpoint asociado: `GET /api/v1/orders/{orderId}`, `PATCH /api/v1/orders/{orderId}`
- Estado vacio: si no hay pagos, mostrar bloque vacio con CTA `Registrar pago`
- Estado de carga: skeleton de detalle
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y acciones de mutacion bloqueadas
- Comportamiento al cancelar: vuelve al listado o cierra dialogo
- Comportamiento al guardar: no aplica; cambios de estado muestran toast y refrescan el detalle

## 32. Registrar pago

- Ruta: `/orders/:orderId/payments/new`
- Tipo: pagina privada
- Componentes visibles: formulario corto de pago y resumen del saldo pendiente
- Botones: `Registrar pago`, `Cancelar`
- Campos: `amount`, `method`, `paidAt`, `reference`, `notes`
- Validaciones: `amount > 0`; `method` requerido; `paidAt` requerido
- Acciones: registrar pago; cancelar
- Destino de cada accion: `Registrar pago` -> `/orders/:orderId`; `Cancelar` -> `/orders/:orderId`
- Endpoint asociado: `POST /api/v1/orders/{orderId}/payments`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: monto invalido o pago excede el saldo
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al detalle del pedido
- Comportamiento al guardar: registra pago, recalcula saldo y vuelve al detalle

## 33. Catalogo

- Ruta: `/catalog`
- Tipo: pagina privada
- Componentes visibles: tabla de productos, filtros, acciones por fila
- Botones: `Crear producto`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `search`, `category`, `active`
- Validaciones: `search` maximo 80 caracteres
- Acciones: listar, filtrar, editar, activar/desactivar
- Destino de cada accion: `Crear producto` -> `/catalog/products/new`; `Editar` -> `/catalog/products/:productId/edit`
- Endpoint asociado: `GET /api/v1/products`, `PATCH /api/v1/products/{productId}`
- Estado vacio: `EmptyState` con CTA `Crear producto`
- Estado de carga: skeleton de tabla
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y mutaciones bloqueadas
- Comportamiento al cancelar: limpia filtros o cierra confirmacion
- Comportamiento al guardar: no aplica; cambios inline muestran toast

## 34. Crear producto

- Ruta: `/catalog/products/new`
- Tipo: pagina privada
- Componentes visibles: formulario de producto
- Botones: `Guardar producto`, `Cancelar`
- Campos: `name`, `sku`, `category`, `description`, `price`, `active`
- Validaciones: `name` requerido; `sku` requerido maximo 40; `price >= 0`; `category` requerida
- Acciones: crear producto; cancelar
- Destino de cada accion: `Guardar producto` -> `/catalog`; `Cancelar` -> `/catalog`
- Endpoint asociado: `POST /api/v1/products`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: validaciones inline o SKU duplicado
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al catalogo
- Comportamiento al guardar: crea producto, muestra toast y vuelve al listado

## 35. Editar producto

- Ruta: `/catalog/products/:productId/edit`
- Tipo: pagina privada
- Componentes visibles: formulario precargado del producto
- Botones: `Guardar cambios`, `Cancelar`
- Campos: `name`, `sku`, `category`, `description`, `price`, `active`
- Validaciones: mismas de crear producto
- Acciones: editar producto; cancelar
- Destino de cada accion: `Guardar cambios` -> `/catalog`; `Cancelar` -> `/catalog`
- Endpoint asociado: `GET /api/v1/products/{productId}`, `PATCH /api/v1/products/{productId}`
- Estado vacio: no aplica
- Estado de carga: skeleton de formulario
- Estado de error: `ErrorState` o validaciones inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al catalogo
- Comportamiento al guardar: actualiza producto, muestra toast y vuelve al listado

## 36. Reportes basicos

- Ruta: `/reports`
- Tipo: pagina privada
- Componentes visibles: cards KPI, graficos por estado, tabla resumida, filtros de rango
- Botones: `Aplicar filtros`, `Limpiar filtros`, `Ver conversaciones`, `Ver pedidos`, `Ver agenda`
- Campos: `dateRange`, `ownerUserId`, `channel`
- Validaciones: rango valido; `channel` solo admite `WHATSAPP`
- Acciones: recalcular metricas; navegar a modulos detallados desde cards o graficos
- Destino de cada accion: `Ver conversaciones` -> `/conversations`; `Ver pedidos` -> `/orders`; `Ver agenda` -> `/appointments`
- Endpoint asociado: `GET /api/v1/reports/overview`
- Estado vacio: `EmptyState` con mensaje "No hay datos para el rango seleccionado"
- Estado de carga: skeleton de cards y graficos
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y ultimo snapshot cacheado
- Comportamiento al cancelar: limpia filtros
- Comportamiento al guardar: no aplica

## 37. Administracion

- Ruta: `/admin`
- Tipo: pagina privada
- Componentes visibles: cards de configuracion, resumen de empresa, usuarios y canal WhatsApp
- Botones: `Configuracion de empresa`, `Usuarios y roles`, `Canal de WhatsApp`, `Seguridad`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: navegar a submodulos administrativos
- Destino de cada accion: `Configuracion de empresa` -> `/admin/company`; `Usuarios y roles` -> `/admin/users`; `Canal de WhatsApp` -> `/admin/whatsapp-channel`; `Seguridad` -> `/admin/security`
- Endpoint asociado: `GET /api/v1/admin/summary`
- Estado vacio: no aplica
- Estado de carga: skeleton de cards
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y datos cacheados
- Comportamiento al cancelar: vuelve a la ultima ruta privada
- Comportamiento al guardar: no aplica

## 38. Configuracion de empresa

- Ruta: `/admin/company`
- Tipo: pagina privada
- Componentes visibles: formulario de datos corporativos
- Botones: `Guardar cambios`, `Cancelar`
- Campos: `companyName`, `businessName`, `timezone`, `currency`, `contactEmail`, `supportPhone`, `address`
- Validaciones: `companyName`, `timezone`, `currency`, `contactEmail` requeridos; `contactEmail` formato email; `supportPhone` formato internacional
- Acciones: editar configuracion; cancelar
- Destino de cada accion: `Guardar cambios` -> `/admin/company`; `Cancelar` -> `/admin`
- Endpoint asociado: `GET /api/v1/company`, `PATCH /api/v1/company`
- Estado vacio: no aplica
- Estado de carga: skeleton de formulario
- Estado de error: errores inline o `ErrorState`
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve a `/admin`
- Comportamiento al guardar: persiste configuracion y permanece en la misma pantalla

## 39. Usuarios y roles

- Ruta: `/admin/users`
- Tipo: pagina privada
- Componentes visibles: tabla de usuarios, filtros, resumen de roles
- Botones: `Crear usuario`, `Aplicar filtros`, `Limpiar filtros`
- Campos: `search`, `role`, `active`
- Validaciones: `search` maximo 80 caracteres
- Acciones: listar, filtrar, editar, activar/desactivar
- Destino de cada accion: `Crear usuario` -> `/admin/users/new`; clic en fila o `Editar` -> `/admin/users/:userId/edit`
- Endpoint asociado: `GET /api/v1/admin/users`, `GET /api/v1/admin/roles`, `PATCH /api/v1/admin/users/{userId}`
- Estado vacio: `EmptyState` con CTA `Crear usuario`
- Estado de carga: skeleton de tabla
- Estado de error: `ErrorState`
- Estado sin conexion: banner offline y mutaciones bloqueadas
- Comportamiento al cancelar: limpia filtros o cierra confirmacion
- Comportamiento al guardar: no aplica; cambios de estado muestran toast

## 40. Crear usuario

- Ruta: `/admin/users/new`
- Tipo: pagina privada
- Componentes visibles: formulario de usuario, resumen de rol seleccionado
- Botones: `Guardar usuario`, `Cancelar`
- Campos: `firstName`, `lastName`, `email`, `phone`, `role`, `active`
- Validaciones: `firstName`, `lastName`, `email`, `role` requeridos; `email` unico; `phone` valido si se informa
- Acciones: crear usuario; cancelar
- Destino de cada accion: `Guardar usuario` -> `/admin/users`; `Cancelar` -> `/admin/users`
- Endpoint asociado: `GET /api/v1/admin/roles`, `POST /api/v1/admin/users`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: validaciones inline o email duplicado
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al listado
- Comportamiento al guardar: crea usuario, envia acceso inicial segun backend y vuelve al listado

## 41. Editar usuario

- Ruta: `/admin/users/:userId/edit`
- Tipo: pagina privada
- Componentes visibles: formulario precargado del usuario
- Botones: `Guardar cambios`, `Cancelar`
- Campos: `firstName`, `lastName`, `email`, `phone`, `role`, `active`
- Validaciones: mismas de crear usuario
- Acciones: editar usuario; cancelar
- Destino de cada accion: `Guardar cambios` -> `/admin/users`; `Cancelar` -> `/admin/users`
- Endpoint asociado: `GET /api/v1/admin/users/{userId}`, `GET /api/v1/admin/roles`, `PATCH /api/v1/admin/users/{userId}`
- Estado vacio: no aplica
- Estado de carga: skeleton de formulario
- Estado de error: `ErrorState` o errores inline
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al listado
- Comportamiento al guardar: actualiza usuario, muestra toast y vuelve al listado

## 42. Canal de WhatsApp

- Ruta: `/admin/whatsapp-channel`
- Tipo: pagina privada
- Componentes visibles: estado del canal (proveedor, conexion), telefono vinculado, ultimos eventos, acciones de conexion
- Botones: `Conectar`, `Desconectar`, `Enviar mensaje de prueba`, `Reintentar`
- Campos: formulario de mensaje de prueba (`recipientPhone`, `body`)
- Validaciones: `recipientPhone` y `body` requeridos
- Acciones: conectar, desconectar, enviar mensaje de prueba, volver a consultar estado
- Destino de cada accion: todas permanecen en `/admin/whatsapp-channel`
- Endpoint asociado: `GET /api/v1/whatsapp-channel/status`, `POST /api/v1/whatsapp-channel/connect`, `POST /api/v1/whatsapp-channel/disconnect`, `POST /api/v1/whatsapp-channel/test-message`
- Estado vacio: mostrar `EmptyState` con CTA `Conectar` cuando no exista canal inicializado
- Estado de carga: spinner sobre tarjeta de estado
- Estado de error: `ErrorState` o banner contextual si el canal no responde
- Estado sin conexion: banner offline y acciones bloqueadas
- Comportamiento al cancelar: cierra dialogos de confirmacion
- Comportamiento al guardar: no aplica; acciones exitosas actualizan el estado en la misma ruta

## 43. Seguridad

- Ruta: `/admin/security`
- Tipo: pagina privada
- Componentes visibles: formulario de politicas y resumen de seguridad
- Botones: `Guardar cambios`, `Cancelar`
- Campos: `sessionTimeoutMinutes`, `passwordMinLength`, `requireUppercase`, `requireNumber`, `requireSymbol`, `maxFailedLoginAttempts`
- Validaciones: todos los numericos mayores a 0; `passwordMinLength` entre 8 y 32
- Acciones: editar politicas; cancelar
- Destino de cada accion: `Guardar cambios` -> `/admin/security`; `Cancelar` -> `/admin`
- Endpoint asociado: `GET /api/v1/admin/security`, `PATCH /api/v1/admin/security`
- Estado vacio: no aplica
- Estado de carga: skeleton de formulario
- Estado de error: errores inline o `ErrorState`
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve a `/admin`
- Comportamiento al guardar: persiste politicas y permanece en la pantalla

## 44. Cambiar contrasena

- Ruta: `/profile/change-password`
- Tipo: pagina privada
- Componentes visibles: formulario de cambio de contrasena, ayuda sobre politicas
- Botones: `Guardar nueva contrasena`, `Cancelar`
- Campos: `currentPassword`, `newPassword`, `confirmPassword`
- Validaciones: `currentPassword` requerida; `newPassword` segun politica activa; `confirmPassword` coincide
- Acciones: cambiar contrasena; cancelar
- Destino de cada accion: `Guardar nueva contrasena` -> `/profile`; `Cancelar` -> `/profile`
- Endpoint asociado: `POST /api/v1/users/me/change-password`
- Estado vacio: no aplica
- Estado de carga: submit en loading
- Estado de error: contrasena actual incorrecta o nueva contrasena invalida
- Estado sin conexion: banner offline y submit deshabilitado
- Comportamiento al cancelar: vuelve al perfil
- Comportamiento al guardar: actualiza credencial, muestra toast y vuelve a `/profile`

## 45. Estado vacio

- Ruta: `state://empty`
- Tipo: estado transversal
- Componentes visibles: `EmptyState`, CTA primario, CTA secundario opcional
- Botones: `Accion primaria`, `Accion secundaria` opcional
- Campos: ninguno
- Validaciones: no aplica
- Acciones: guiar al usuario al siguiente paso disponible
- Destino de cada accion: contextual segun modulo origen
- Endpoint asociado: no aplica
- Estado vacio: es el propio estado
- Estado de carga: no aplica
- Estado de error: no aplica
- Estado sin conexion: puede coexistir con banner offline
- Comportamiento al cancelar: vuelve al contexto origen
- Comportamiento al guardar: no aplica

## 46. Estado de carga

- Ruta: `state://loading`
- Tipo: estado transversal
- Componentes visibles: `LoadingState`, skeletons, indicadores de progreso
- Botones: ninguno
- Campos: ninguno
- Validaciones: no aplica
- Acciones: bloquear interaccion sensible mientras la solicitud esta en curso
- Destino de cada accion: no aplica
- Endpoint asociado: aplica al endpoint activo de la vista origen
- Estado vacio: no aplica
- Estado de carga: es el propio estado
- Estado de error: transiciona a `state://error` si la solicitud falla
- Estado sin conexion: puede transicionar a `state://offline`
- Comportamiento al cancelar: solo si el flujo soporta abortar la peticion
- Comportamiento al guardar: finaliza en vista de exito o error segun respuesta

## 47. Estado de error

- Ruta: `state://error`
- Tipo: estado transversal
- Componentes visibles: `ErrorState`, descripcion clara, boton `Reintentar`
- Botones: `Reintentar`, `Volver`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: reintentar carga o volver al contexto anterior
- Destino de cada accion: `Reintentar` -> misma ruta origen; `Volver` -> listado o ruta anterior
- Endpoint asociado: endpoint activo de la vista origen
- Estado vacio: no aplica
- Estado de carga: puede mostrarse tras un nuevo intento
- Estado de error: es el propio estado
- Estado sin conexion: si el fallo es de red, deriva a `state://offline`
- Comportamiento al cancelar: vuelve al contexto anterior
- Comportamiento al guardar: no aplica

## 48. Estado sin conexion

- Ruta: `state://offline`
- Tipo: estado transversal
- Componentes visibles: `OfflineBanner`, contenido cacheado opcional, mensaje de reintento automatico
- Botones: `Reintentar ahora`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: pausar mutaciones, reintentar consulta, conservar cache
- Destino de cada accion: `Reintentar ahora` -> misma ruta origen
- Endpoint asociado: endpoint activo de la vista origen
- Estado vacio: puede coexistir con vacio si no hay cache
- Estado de carga: aparece durante un reintento manual
- Estado de error: si el reintento falla por otra causa, deriva a `state://error`
- Estado sin conexion: es el propio estado
- Comportamiento al cancelar: no aplica
- Comportamiento al guardar: no aplica mientras no haya red

## 49. Confirmacion de guardado

- Ruta: `toast://saved`
- Tipo: feedback transversal
- Componentes visibles: `Toast` de exito con mensaje contextual
- Botones: ninguno; opcional `Ver detalle`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: confirmar al usuario que la operacion se guardo correctamente
- Destino de cada accion: `Ver detalle` -> detalle de la entidad creada o editada
- Endpoint asociado: endpoint `POST`, `PATCH` o `DELETE` exitoso que origino el guardado
- Estado vacio: no aplica
- Estado de carga: aparece despues de completarse la mutacion
- Estado de error: no aplica
- Estado sin conexion: no aplica
- Comportamiento al cancelar: cerrar toast manualmente si el componente lo permite
- Comportamiento al guardar: es el propio resultado exitoso

## 50. Confirmacion de eliminacion

- Ruta: `modal://confirm-delete`
- Tipo: confirmacion transversal
- Componentes visibles: `ConfirmDialog`, texto de impacto, botones de confirmar y cancelar
- Botones: `Confirmar eliminacion`, `Cancelar`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: confirmar borrado logico o cancelarlo
- Destino de cada accion: `Confirmar eliminacion` -> vuelve a la pantalla origen; `Cancelar` -> cierra modal
- Endpoint asociado: endpoint destructivo contextual, por ejemplo `DELETE /api/v1/templates/{templateId}`
- Estado vacio: no aplica
- Estado de carga: boton de confirmacion en loading mientras responde el backend
- Estado de error: mostrar error inline si no se puede eliminar
- Estado sin conexion: impedir confirmar
- Comportamiento al cancelar: cierra modal sin cambios
- Comportamiento al guardar: al confirmar con exito elimina o desactiva el registro, refresca origen y muestra toast

## 51. Cierre de sesion confirmado

- Ruta: `modal://logout-confirmed`
- Tipo: confirmacion transversal
- Componentes visibles: `ConfirmDialog` con advertencia de cierre de sesion
- Botones: `Cerrar sesion`, `Cancelar`
- Campos: ninguno
- Validaciones: no aplica
- Acciones: confirmar cierre o volver a la aplicacion
- Destino de cada accion: `Cerrar sesion` -> `/login`; `Cancelar` -> vuelve a la ruta privada actual
- Endpoint asociado: `POST /api/v1/auth/logout`
- Estado vacio: no aplica
- Estado de carga: boton `Cerrar sesion` en loading
- Estado de error: toast si el logout falla; se limpia sesion local de todos modos
- Estado sin conexion: permitir cierre local y marcar logout remoto pendiente
- Comportamiento al cancelar: cierra modal
- Comportamiento al guardar: destruye sesion, limpia cache y redirige a `/login`

