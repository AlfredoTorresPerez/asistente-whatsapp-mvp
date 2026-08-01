# SCREEN IMAGE MAPPING

## Objetivo

Esta matriz une cada pantalla o componente visual relevante con su referencia grafica oficial, su ruta esperada y su estado actual dentro del frontend. Debe consultarse antes de crear o retocar cualquier vista.

## Estados usados

- `Implementado`: existe en codigo y ruta, pero igual debe compararse con el prototipo.
- `Implementado parcial`: existe, pero no respeta aun la forma visual exacta del prototipo o la superficie no coincide.
- `Placeholder`: existe una ruta, pero la UI real todavia es de relleno.
- `Pendiente`: no existe todavia o no tiene ruta dedicada.

## Autenticacion

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| LoginPage | `/login` | `01_autenticacion_inicio_recuperacion_demo_contacto.png` | Publico, 2 columnas | trust panel azul, selector de idioma, card blanca, CTA azul, CTA secundario | copy, mensajes de validacion, loader en submit | Implementado, requiere ETAPA 3C |
| ForgotPasswordPage | `/forgot-password` | `01_autenticacion_inicio_recuperacion_demo_contacto.png` | Publico, 2 columnas | trust panel azul, card blanca, CTA azul, enlace de regreso | copy, ayuda contextual, estado submit | Implementado, requiere ETAPA 3C |
| PasswordResetSentPage | `/forgot-password/sent` | `01_autenticacion_inicio_recuperacion_demo_contacto.png` | Publico, 2 columnas | panel azul, card blanca con icono de correo, CTA principal y secundario | texto de confirmacion, cuenta regresiva opcional | Implementado parcial. En codigo actual se llama `ForgotPasswordSentPage` |
| ResetPasswordPage | `/reset-password?token=:token` | `01_autenticacion_inicio_recuperacion_demo_contacto.png` | Publico, 2 columnas | panel azul, card blanca, reglas de password visibles, CTA azul | mensajes de politica, loader, validacion de token | Implementado, requiere ETAPA 3C |
| RequestDemoPage | `/request-demo` | `01_autenticacion_inicio_recuperacion_demo_contacto.png` | Publico, 2 columnas | panel azul comercial, formulario blanco mas largo, CTA azul, bloque legal | campos concretos, ayuda de contacto | Pendiente |
| ContactSalesPage | `/contact-sales` | `01_autenticacion_inicio_recuperacion_demo_contacto.png` | Publico, 2 columnas | panel azul comercial, formulario blanco, textarea, CTA azul | copy y tiempos de respuesta | Pendiente |

## Base privada

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| DashboardPage | `/dashboard` | `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png` | Privado base | sidebar azul, topbar, metric cards, donut o charts, actividad reciente, tarjetas blancas | metricas y datasets reales | Implementado, requiere ETAPA 3C |
| NotificationsPanel | `panel://topbar/notifications` y respaldo `/notifications` | `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png` | Panel lateral | campana en topbar, drawer derecho, tabs de filtro, lista de eventos, CTA ver todo | filtros y cantidad de tabs | Implementado parcial. Existe pagina `/notifications`, no panel lateral fiel |
| UserMenu | `overlay://topbar/user-menu` | `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png` | Dropdown global | avatar, rol, accesos a perfil, seguridad, novedades y logout | cantidad de links secundarios | Implementado parcial |
| ProfilePage | `/profile` | `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png` | Privado base con tabs internas | PageHeader, tabs, card de informacion, foto/avatar, formulario de perfil, CTA verde | secciones, avatar real, ayuda de password | Implementado, requiere ETAPA 3C |
| ChangePasswordPage | `/profile/change-password` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Privado base, card de seguridad | card blanca, politicas de password, CTA azul, cancelacion clara | copy de seguridad, checklist de politica | Implementado, requiere ETAPA 3C |

## Conversaciones

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| ConversationsPage | `/conversations` | `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Tres columnas | sidebar, topbar, lista de conversaciones, buscador, detalle central, panel contextual derecho | badges de estado, paginacion, filtros | Placeholder |
| ConversationDetail | `/conversations/:conversationId` | `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Tres columnas | header del cliente, hilo, composer, panel de info, quick actions | etiquetas, notas internas, resumen | Placeholder |
| NewConversationPanel | `drawer://conversations/new` y ruta actual `/conversations/new` | `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Drawer | buscador/contact picker, tabs, CTA principal | tabs de contactos y recientes | Placeholder |
| ResponseTemplatesPanel | `drawer://conversations/templates` y ruta actual `/templates` | `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Drawer | search, lista de plantillas, estados, CTA crear | categorias y preview de mensaje | Placeholder |
| CreateLeadFromConversationPanel | `drawer://conversations/:conversationId/prospects/new` y ruta actual `/conversations/:conversationId/prospects/new` | `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Drawer | formulario compacto, selects, textarea, CTA azul | origen fijo, responsable, interes | Placeholder |
| CreateOrderFromConversationPanel | `drawer://conversations/:conversationId/orders/new` y ruta actual `/conversations/:conversationId/orders/new` | `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Drawer | cliente fijo, selector de producto, cantidad, total, CTA | descuentos, notas, totales | Placeholder |
| CreateBookingFromConversationPanel | `drawer://conversations/:conversationId/appointments/new` y ruta actual `/conversations/:conversationId/appointments/new` | `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Drawer | cliente fijo, servicio, fecha, hora, duracion, CTA | profesional, notas | Placeholder |

## Reglas

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| AutomationRulesPage | `/automation-rules` | `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Privado base | metricas laterales, tabla principal, filtros, toggles de estado, CTA nueva regla | filtros y resumenes | Placeholder |
| CreateRulePage | `/automation-rules/new` | `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Wizard asistido | panel azul lateral, steps, form principal, CTA continuar | longitud del flujo y copy | Placeholder |
| EditRulePage | `/automation-rules/:ruleId/edit` | `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Wizard asistido | panel azul lateral, steps, datos cargados, CTA guardar y continuar | prioridad, estado, descripcion | Placeholder |
| ConditionsBuilder | `/automation-rules/:ruleId/edit?step=conditions` | `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Wizard step | cards de condicion, operadores, resumen lateral, CTA agregar | tipos de condicion | Pendiente |
| ActionsBuilder | `/automation-rules/:ruleId/edit?step=actions` | `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Wizard step | lista de acciones, preview lateral, CTA agregar accion | tipos de accion y orden | Pendiente |
| TestRuleModal | `modal://automation-rules/:ruleId/test` y ruta actual `/automation-rules/:ruleId/test` | `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Modal o card de prueba | simulador, entrada de ejemplo, resultado, CTA de continuar | dataset de ejemplo | Placeholder |
| ExecutionHistoryPage | `/automation-rules/:ruleId/history` | `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Privado base | filtros, tabla de ejecuciones, metricas, badges de resultado | rangos y filtros | Pendiente |

## Prospectos

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| LeadsPage | `/prospects` | `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` | Privado base | metric cards, buscador, filtros, tabla, CTA crear/importar/exportar | columnas y filtros | Placeholder |
| CreateLeadPanel | `/prospects/new` | `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` | Form page o drawer | formulario blanco, secciones, origen y canal, CTA guardar | responsable, notas, checkbox de crear otro | Placeholder |
| LeadDetailPanel | `/prospects/:prospectId` | `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` | Detail split | resumen, tabs, notas, proxima accion, quick actions | historial y resumen de conversacion | Placeholder |
| EditLeadPanel | `/prospects/:prospectId/edit` | `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` | Form page | formulario blanco, campos precargados, CTA guardar cambios | responsable, necesidad, notas | Placeholder |
| ImportLeadsModal | `modal://prospects/import` | `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` | Modal | dropzone, formatos permitidos, checks de importacion | plantilla de ejemplo, flags opcionales | Pendiente |
| ExportLeadsModal | `modal://prospects/export` | `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` | Modal | filtros, formato de salida, CTA exportar | CSV/XLSX | Pendiente |

## Agenda

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| AgendaPage | `/appointments` | `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png` | Privado base, agenda | metric cards, mini calendario, timeline diaria, filtros, detalle lateral | vista dia/semana/mes | Placeholder |
| CreateBookingPanel | `/appointments/new` | `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png` | Form page o drawer | formulario blanco, cliente, servicio, fecha, hora, duracion, CTA | profesional, notas | Placeholder |
| BookingDetailPanel | `/appointments/:appointmentId` | `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png` | Detail card | estado visible, datos de reserva, info cliente, CTA reprogramar/editar/cancelar | historial y contacto | Placeholder |
| EditBookingPanel | `/appointments/:appointmentId/edit` | `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png` | Form page | form precargado, CTA guardar, estado visible | profesional y notas | Pendiente |
| RescheduleBookingModal | `modal://appointments/:appointmentId/reschedule` y ruta actual `/appointments/:appointmentId/reschedule` | `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png` | Modal | resumen corto, selector de fecha, slots, CTA confirmar | bloques horarios | Placeholder |
| AvailabilitySettingsPage | `/appointments/availability` | `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png` | Configuracion | tabla de dias, horarios, duracion por defecto, excepciones, CTA guardar | intervalos y reglas simples | Pendiente |

## Pedidos

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| OrdersPage | `/orders` | `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Privado base | metric cards, filtros, buscador, tabla, CTA nuevo pedido | columnas y filtros | Placeholder |
| CreateOrderPanel | `/orders/new` | `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Form page | cliente, detalle pedido, items, subtotal, descuento, total, CTA verde | tipo de entrega y notas | Placeholder |
| OrderDetailPanel | `/orders/:orderId` | `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Detail grid | cliente, items, resumen, quick actions, estado de despacho | timeline de estado | Placeholder |
| EditOrderPanel | `/orders/:orderId/edit` | `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Form page | formulario precargado, items editables, CTA verde | direccion y notas | Pendiente |
| RegisterPaymentModal | `modal://orders/:orderId/payments/new` y ruta actual `/orders/:orderId/payments/new` | `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Modal | metodo, monto, fecha, referencia, CTA verde | referencia opcional | Placeholder |
| DeliveryStatusPanel | `panel://orders/:orderId/delivery-status` | `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Panel lateral o card | stepper de despacho, notas, CTA guardar estado | cantidad de pasos | Pendiente |
| ReceiptPreviewModal | `modal://orders/:orderId/receipt-preview` | `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Modal | tipo de comprobante, correo, checklist de contenido, CTA generar | pdf o boleta | Pendiente |

## Catalogo

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| CatalogPage | `/catalog` | `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Privado base | tabs servicios/productos, chips de categoria, buscador, tabla/lista, CTA nuevo | filtros y vista por categoria | Placeholder |
| CreateProductPanel | `/catalog/products/new` | `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Form page | info basica, categoria, descripcion, precio, duracion, imagen, toggles | destacar y disponibilidad | Placeholder |
| ProductDetailPanel | `/catalog/products/:productId` | `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Detail card | imagen, badge de disponibilidad, tabs de informacion/inventario, CTA editar | datos de servicio o producto | Pendiente |
| EditProductPanel | `/catalog/products/:productId/edit` | `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Form page | form precargado, preview de imagen, toggles, CTA guardar | disponibilidad y destacado | Placeholder |
| CategoriesManagerModal | `modal://catalog/categories` | `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Modal o pagina corta | tabla de categorias, CTA nueva categoria, edit/delete inline | iconos y colores de categoria | Pendiente |
| InventoryPage | `/catalog/inventory` | `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Privado base | tabla de stock, filtros, estado bajo/agotado, CTA nuevo producto | pestañas productos/insumos | Pendiente |
| ImportCatalogModal | `modal://catalog/import` | `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Modal | dropzone, ayuda de formato, CTA importar | plantilla CSV y resumen de columnas | Pendiente |

## Reportes

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| ReportsPage | `/reports` | `09_reportes_filtros_detalle_exportar_programar_estados.png` | Privado base | metric cards, charts, donut, embudo, CTA filtros, CTA detalle | datasets y rangos | Placeholder |
| AdvancedReportFiltersPanel | `drawer://reports/filters` | `09_reportes_filtros_detalle_exportar_programar_estados.png` | Drawer | rango, canales, etiquetas, agentes, mas filtros, CTA aplicar | campos concretos | Pendiente |
| ReportDetailPage | `/reports/:reportId` | `09_reportes_filtros_detalle_exportar_programar_estados.png` | Privado base detalle | breadcrumbs, chart grande, resumen lateral, tabla por canal, quick actions | tipo de reporte | Pendiente |
| ExportReportModal | `modal://reports/export` | `09_reportes_filtros_detalle_exportar_programar_estados.png` | Modal | formato, inclusiones, CTA exportar | PDF/XLSX/CSV | Pendiente |
| ScheduleReportModal | `modal://reports/schedule` | `09_reportes_filtros_detalle_exportar_programar_estados.png` | Modal | frecuencia, dia, hora, correos, formato, CTA programar | lista de destinatarios | Pendiente |

## Administracion y seguridad

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| AdministrationPage | `/admin` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | cards resumen, tabs o bloques por subseccion, CTA a empresa, usuarios, canal WhatsApp y seguridad | orden de cards | Placeholder |
| BusinessSettingsPage | `/admin/company` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | card de datos de empresa, logo, mensaje de bienvenida, integraciones visibles | secciones y ayudas | Placeholder |
| UsersAndRolesPage | `/admin/users` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | tabla de usuarios, buscador, filtros, CTA invitar usuario | columnas y badges de rol | Placeholder |
| CreateUserModal | `modal://admin/users/new` y ruta actual `/admin/users/new` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Modal | formulario corto, rol, telefono, toggle de email, CTA azul | copy de invitacion | Placeholder |
| EditUserPanel | `/admin/users/:userId/edit` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Panel lateral | avatar, datos personales, estado, CTA guardar | rol y estado | Placeholder |
| RolePermissionsPage | `/admin/roles` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | matriz permisos x rol, checks, descripciones | solo lectura o editable | Pendiente |
| WhatsAppChannelPage | `/admin/whatsapp-channel` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | card del canal, proveedor, estado, numero, CTA conectar/desconectar | estados connected/disconnected/error | Placeholder |
| IntegrationsPage | `/admin/integrations` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | cards por integracion, CTA conectar, estado | lista de integraciones activas | Pendiente |
| PlanBillingPage | `/admin/billing/plan` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | plan actual, resumen de facturacion, CTA cambiar plan | copy comercial | Pendiente |
| PaymentMethodsPage | `/admin/billing/payment-methods` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | lista de tarjetas, badge de preferida, CTA agregar metodo | cantidad de metodos | Pendiente |
| AuditLogPage | `/admin/security/audit-log` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | tabla de accesos y eventos, filtros, rango, IP, dispositivo | columnas y filtros | Pendiente |
| SecurityPage | `/admin/security` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | cards de password, sesiones, 2FA, dispositivos y access log | orden de cards | Placeholder |
| TwoFactorSetupPage | `/admin/security/2fa` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion o modal asistido | pasos, QR, codigo manual, CTA activar | apps sugeridas | Pendiente |
| SessionsPage | `/admin/security/sessions` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | lista de sesiones activas, CTA cerrar sesion, CTA cerrar todas | datos de dispositivo | Pendiente |
| AuthorizedDevicesPage | `/admin/security/devices` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | lista de dispositivos, badge de activo, CTA revocar | cantidad de dispositivos | Pendiente |
| AccessLogPage | `/admin/security/access-log` | `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Configuracion | tabla de accesos, filtros, ubicacion, IP, accion | columnas y filtros | Pendiente |

## Estados globales

| Pantalla | Ruta frontend | Imagen de referencia | Tipo de layout | Componentes visuales obligatorios | Variantes permitidas | Estado de implementacion |
| --- | --- | --- | --- | --- | --- | --- |
| EmptyState | `component://empty-state` | `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` y `09_reportes_filtros_detalle_exportar_programar_estados.png` | Estado embebido | ilustracion, titulo claro, explicacion, CTA principal y secundaria | copy e icono segun modulo | Implementado parcial |
| LoadingState | `component://loading-state` | `09_reportes_filtros_detalle_exportar_programar_estados.png` | Estado embebido | ilustracion suave, texto de progreso, checklist opcional | spinner o skeleton segun contexto | Implementado parcial |
| ErrorState | `component://error-state` | derivado del sistema de tarjetas del ZIP | Estado embebido | card blanca, icono, mensaje, CTA reintentar | copy y codigo de error traducido | Implementado parcial |
| OfflineState | `component://offline-banner` | derivado del sistema privado del ZIP | Banner persistente | aviso superior, tono warning, CTA reintentar opcional | texto por modulo | Implementado parcial |
| SaveConfirmationToast | `component://toast` | derivado de confirmaciones del sistema | Overlay breve | icono, titulo, descripcion corta, color success | info, warning, error | Implementado parcial |
| DeleteConfirmationModal | `modal://confirm-delete` | derivado del sistema de modales del ZIP | Modal | titulo, descripcion, CTA cancelar, CTA destructiva | copy por entidad | Pendiente |
| LogoutConfirmationModal | `modal://logout-confirmed` | `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png` | Modal | ilustracion simple, pregunta, CTA cancelar y CTA destructiva | copy de sesion | Implementado |
