# TASKS_PHASE_1

## Objetivo

Descomponer Fase 1 en tareas pequenas, ordenadas y verificables. Ninguna tarea debe iniciarse si la verificacion de la anterior no esta aprobada.

## Secuencia de implementacion

| ID | Etapa | Objetivo | Entregable verificable | Verificacion |
| --- | --- | --- | --- | --- |
| T00 | Congelamiento contractual | Dejar esta carpeta `docs/` como fuente de verdad inicial. | Documentacion aprobada. | Revision manual de consistencia entre pantallas, API y datos. |
| T01 | Bootstrap del monorepo | Crear `frontend-react`, `backend-java`, `whatsapp-web-service` y `docker-compose.yml`. | Repositorio compila en vacio con comandos base. | `pnpm --dir frontend-react build` y `./backend-java/mvnw test`. |
| T02 | Infra local | Levantar PostgreSQL, `backend-java`, `frontend-react` y `whatsapp-web-service`. | `docker compose up` funcional con healthchecks. | Ver estados `healthy`, `http://localhost:8080/actuator/health`, `http://localhost:5173` y `http://localhost:3001/health`. |
| T03 | Design tokens y layouts | Implementar `PublicLayout`, `AppLayout`, `Sidebar`, `Topbar` y estados globales. | Shell navegable con rutas placeholder. | Navegacion entre `/login` y `/dashboard` sin datos reales. |
| T04 | Autenticacion backend | Login, refresh, logout, forgot/reset password. | Endpoints `auth` operativos con validaciones. | Tests de `login`, `forgot-password`, `reset-password`. |
| T05 | Autenticacion frontend | Formularios de acceso y recuperacion conectados al backend. | Flujo completo desde `/login` a `/dashboard`. | Prueba manual del ciclo login -> logout. |
| T06 | Perfil y contrasena | Perfil de usuario y cambio de contrasena. | `/profile` y `/profile/change-password` funcionales. | Guardado exitoso y error de validacion cubiertos. |
| T07 | Dashboard y notificaciones | Resumen inicial del negocio y centro de notificaciones. | `/dashboard` y `/notifications` consumiendo datos reales. | Cards, tabla y marcado de leidas verificados. |
| T08 | Administracion base | Empresa, seguridad, usuarios y roles. | Pantallas `/admin/*` operativas. | Crear usuario, editar usuario y guardar politicas. |
| T09 | Catalogo | CRUD basico de productos. | `/catalog`, crear y editar producto. | Crear producto, editar precio y desactivar. |
| T10 | Plantillas | CRUD basico de plantillas de respuesta. | `/templates` y `/templates/new`. | Crear plantilla y usarla desde contrato de conversaciones. |
| T11 | Prospectos | Listado, alta, detalle y edicion. | `/prospects*` funcional. | Crear prospecto manual y editarlo. |
| T12 | Agenda | Listado, detalle, alta y reprogramacion de citas. | `/appointments*` funcional. | Crear cita, reprogramar y marcar completada. |
| T13 | Pedidos y pagos | Listado, alta, detalle y registro de pagos. | `/orders*` funcional. | Crear pedido, registrar pago y recalcular saldo. |
| T14 | Conversaciones base | Listado, detalle, nueva conversacion y composer. | `/conversations*` funcional. | Crear conversacion y enviar mensaje saliente simulado. |
| T15 | Integracion WhatsApp Web | Conectar backend Java con `whatsapp-web-service`. | Estado de sesion, QR y recepcion de mensajes. | Recibir mensaje real y crear/actualizar conversacion. |
| T16 | Flujos desde conversacion | Crear prospecto, pedido y cita desde detalle de conversacion. | Formularios contextuales enlazados. | Cada accion crea entidad y vuelve al flujo correcto. |
| T17 | Reglas de automatizacion | Listado, alta, edicion y prueba de reglas. | `/automation-rules*` funcional. | Ejecutar prueba sobre mensaje simulado. |
| T18 | Reportes basicos | Resumen comercial con filtros y graficos. | `/reports` funcional. | Cambiar rango y validar coherencia con datos base. |
| T19 | Hardening UX | Manejo de vacio, carga, error, sin conexion y confirmaciones. | Estados transversales en todas las pantallas. | Checklist manual por ruta principal. |
| T20 | QA final Fase 1 | Cierre integral de la fase. | Smoke test end-to-end y backlog de bugs priorizado. | Aprobacion funcional sobre todos los flujos de `SCREEN_SPEC`. |

## Dependencias clave

- `T01` no puede iniciar sin cerrar `T00`.
- `T03` depende de `T01`.
- `T04` depende de `T01` y `T02`.
- `T05` depende de `T03` y `T04`.
- `T07` a `T18` dependen de autenticacion y shell (`T03` a `T05`).
- `T15` depende de `T02`, `T04` y `T14`.
- `T17` depende de `T10`, `T14` y `T15`.

## Criterios de verificacion transversales

- Toda vista protegida debe rechazar acceso sin sesion valida.
- Todo listado debe cubrir vacio, carga y error.
- Todo formulario debe cubrir validacion cliente y servidor.
- Todo detalle debe permitir volver al listado de origen.
- Toda accion destructiva o sensible debe pedir confirmacion.

## Riesgos a vigilar

- Ambiguedad entre rutas, endpoints y entidades si `docs/` no se congela bien en `T00`.
- Atraso en la integracion real con WhatsApp.
- Inconsistencia entre estados UI y enums backend.
- Sobrecarga de alcance en automatizaciones.
- Dependencia excesiva de polling si las consultas no son paginadas.
