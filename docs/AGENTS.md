# AGENTS

## Proposito

Este documento fija las reglas contractuales del proyecto `asistente-whatsapp-mvp` para Fase 1. Si una etapa posterior necesita cambiar alcance, arquitectura o comportamiento, primero debe actualizar esta documentacion y luego el codigo.

## Orden de precedencia

Cuando exista ambiguedad entre documentos, el orden de decision es:

1. `docs/AGENTS.md`
2. `docs/SCREEN_SPEC.md`
3. `docs/API_CONTRACTS.md`
4. `docs/DATA_MODEL.md`
5. `docs/UI_COMPONENTS.md`
6. `docs/NAVIGATION_MAP.md`
7. `docs/TASKS_PHASE_1.md`

## Alcance funcional de Fase 1

- Aplicacion web interna para operar conversaciones de WhatsApp.
- Gestion basica de prospectos, citas, pedidos, catalogo y usuarios.
- Reglas de automatizacion simples sobre eventos definidos.
- Integracion con un unico numero de WhatsApp por empresa mediante el canal nativo del backend.
- El canal soporta dos proveedores: `META_CLOUD_API` (WhatsApp Cloud API de Meta, webhook firmado `X-Hub-Signature-256`) y `SIMULATED` (proveedor simulado embebido, default local). No existe servicio externo ni QR.
- Una sola empresa por despliegue.
- Interfaz de usuario en espanol.
- Identificadores de codigo, nombres de clases, paquetes, tablas y campos en ingles.

## Fuera de alcance en Fase 1

- Multiempresa dentro de la misma instancia.
- Mas de un canal WhatsApp activo por empresa.
- Mensajes multimedia, notas de voz, documentos o stickers.
- Bots con IA generativa, NLP avanzado o intenciones libres.
- Workflows visuales complejos con ramas multiples.
- Inventario, stock, devoluciones, facturacion electronica o impuestos avanzados.
- Edicion dinamica de permisos por pantalla; solo roles fijos.
- Aplicaciones moviles nativas.
- WebSockets; la UI debe operar con HTTP y polling controlado.

## Pila tecnologica contractual

### Frontend

- React 18 + TypeScript 5.
- Vite como build tool.
- React Router para routing.
- TanStack Query para data fetching, cache y polling.
- React Hook Form + Zod para formularios y validacion.
- Tailwind CSS con variables CSS para tokens visuales.
- Day.js para fechas.

### Backend

- Java 21.
- Spring Boot 3.x.
- Spring Web.
- Spring Security.
- Spring Validation.
- Spring Data JPA.
- PostgreSQL 16.
- Flyway para migraciones.
- Jackson para JSON.

### Integraciones y soporte

- Canal WhatsApp nativo del backend, sin servicio externo: proveedores `META_CLOUD_API` y `SIMULATED` (embebido) seleccionados con `APP_WHATSAPP_CHANNEL_PROVIDER`.
- Docker Compose para levantar dependencias locales.
- Redis opcional para cache liviano, rate limit y colas simples de reintento.

### Testing

- Frontend: Vitest + React Testing Library.
- Backend: JUnit 5 + MockMvc + Testcontainers.

## Arquitectura objetivo

### Vista general

1. El frontend consume exclusivamente el backend Java.
2. El backend Java es la fuente de verdad de negocio.
3. El canal WhatsApp es nativo del backend y no es una fuente de verdad funcional.
4. Los mensajes entrantes llegan por el canal: webhook firmado `X-Hub-Signature-256` de Cloud API, o simulacion local via `POST /api/v1/test/whatsapp-inbound`.
5. La UI actualiza datos por polling en endpoints REST.
6. El backend Java depende de interfaces internas (`CanalWhatsApp`); nunca depende de un servicio externo ni de sesiones de dispositivo con QR.

### Estructura esperada del repositorio

```text
/
|-- backend-java/
|-- frontend-react/
|-- docs/
|-- docker-compose.local.yml   (canonico local; scripts\local-start.ps1)
|-- docker-compose.full.yml    (alternativo, NO simultaneo con el local)
`-- README.md
```

### Estructura esperada del backend

```text
backend-java/src/main/java/com/asistentewhatsapp/
|-- shared/
|-- security/
|-- administration/
|-- channels/
|   |-- domain/
|   |-- application/
|   |-- infrastructure/whatsappcloud/
|   `-- infrastructure/simulated/
|-- conversations/
|-- automation/
|-- leads/
|-- bookings/
|-- orders/
|-- catalog/
|-- reports/
|-- notifications/
`-- integrations/
```

### Estructura esperada del frontend

```text
frontend-react/src/
|-- app/
|-- modules/
|   |-- auth/
|   |-- dashboard/
|   |-- conversations/
|   |-- rules/
|   |-- leads/
|   |-- agenda/
|   |-- orders/
|   |-- catalog/
|   |-- reports/
|   |-- administration/
|   `-- security/
|-- components/
|-- services/
`-- lib/
```

### Modulos de dominio

- Auth
- Dashboard
- Notifications
- Conversations
- Templates
- Prospects
- Appointments
- Orders
- Catalog
- Automation Rules
- Administration
- WhatsApp Channel

## Comandos contractuales

Estos comandos no existen todavia en el repositorio vacio, pero deben quedar disponibles cuando se implemente la base tecnica:

```bash
pnpm --dir frontend-react install
pnpm --dir frontend-react dev
pnpm --dir frontend-react build
pnpm --dir frontend-react test
pnpm --dir frontend-react lint

./backend-java/mvnw spring-boot:run
./backend-java/mvnw test
./backend-java/mvnw verify

# Entorno local (comando oficial: docker-compose.local.yml con .env.local)
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
docker compose --env-file .env.local -f docker-compose.local.yml down
```

El compose base se renombro a `docker-compose.full.yml` (Fase 4): es el stack alternativo
fuera del flujo local y NO debe levantarse simultaneamente con `docker-compose.local.yml`
(comparten puertos 5433/8080/5173).

## Convenciones de codigo

- Todo codigo nuevo debe ser ASCII salvo que un archivo existente requiera otro charset.
- UI, mensajes de validacion y textos visibles: espanol.
- Identificadores tecnicos: ingles.
- No usar `any` en TypeScript salvo encapsulacion puntual y justificada.
- Activar `strict` en TypeScript.
- DTOs Java terminan en `Request` y `Response`.
- Entidades JPA no se exponen directamente al frontend.
- Fechas y horas se persisten en UTC y se renderizan en la zona horaria de la empresa.
- Endpoints REST en plural y versionados bajo `/api/v1`.
- Soft delete por bandera `active` o `status` cuando aplique; evitar borrado fisico en Fase 1.
- Cada cambio de contrato debe actualizar esta carpeta `docs/`.

## Reglas frontend

- Usar `PublicLayout` para autenticacion y `AppLayout` para areas privadas.
- Cada pantalla definida en `docs/SCREEN_SPEC.md` debe tener estado normal, vacio, carga, error y sin conexion cuando aplique.
- Formularios siempre con React Hook Form + Zod.
- Validaciones del cliente deben espejar validaciones del backend, sin sustituirlas.
- Tablas con paginacion, filtros y ordenamiento server-side.
- Polling recomendado:
  - conversaciones activas: 15 segundos;
  - notificaciones: 30 segundos;
  - dashboard: 30 segundos;
  - estado del canal WhatsApp: 15 segundos.
- Cancelar en formularios vuelve a la pantalla de origen sin persistir.
- Guardar exitoso siempre muestra `Toast` de confirmacion o redireccion con estado de exito.
- Sidebar fija en escritorio y colapsable en movil.
- No mezclar logica de negocio con componentes visuales puros.

## Reglas backend

- Arquitectura por dominio y capas: controller, service, repository, mapper, dto.
- Los controladores solo orquestan request/response; la logica vive en services.
- Toda entrada publica se valida con Bean Validation.
- Operaciones que cambian estado se ejecutan dentro de transacciones.
- Migraciones de base de datos obligatorias con Flyway.
- Ningun endpoint de frontend expone stacktraces, clases Java ni mensajes tecnicos crudos.
- Auditoria minima para:
  - inicio y cierre de sesion;
  - cambio de contrasena;
  - creacion y edicion de reglas;
  - registro de pagos;
  - cambios de estado del canal WhatsApp.
- Los mensajes salientes a WhatsApp deben pasar por una cola de salida o patron outbox sencillo para tolerar reintentos.
- El backend nunca expone directamente secretos del canal (App Secret, token de acceso de Cloud API).

## Reglas de datos

- Una empresa por despliegue.
- Toda entidad operativa persiste `business_id`, incluso en despliegue de empresa unica.
- Toda consulta operativa filtra por `business_id`, incluso si el cliente no lo envia explicitamente.
- Un usuario pertenece a una empresa.
- Un prospecto puede originarse manualmente o desde una conversacion.
- Un pedido y una cita pueden vincularse a un prospecto y opcionalmente a una conversacion.
- Una conversacion puede existir sin prospecto asociado.
- Los estados de negocio deben ser enums controlados por backend.

## Seguridad

- Autenticacion basada en JWT de acceso de corta duracion.
- Refresh token en cookie `HttpOnly` o mecanismo equivalente administrado por backend.
- Politicas de contrasena configurables desde administracion.
- Autorizacion por rol fijo en Fase 1: `OWNER`, `ADMIN`, `AGENT`, `SALES`.
- Toda llamada privada requiere `Authorization: Bearer <token>`.
- El webhook del canal Cloud API debe verificar la firma `X-Hub-Signature-256` (HMAC-SHA256 del body con App Secret) en tiempo constante.

## Regla de UX para Fase 1

- Todo flujo critico debe poder completarse en maximo 3 clics desde su listado principal.
- Ningun formulario principal debe superar 12 campos visibles sin secciones claras.
- Las tablas deben permitir llegar al detalle haciendo clic en la fila.
- Los errores deben ser accionables y no tecnicos.

## Restricciones tecnicas de Fase 1

- Sin SSR; la aplicacion web sera SPA.
- Sin eventos en tiempo real sobre sockets; usar polling y refresco manual.
- Sin adjuntos binarios en conversaciones.
- Sin edicion inline masiva en tablas.
- Sin builder visual para reglas.
- Sin sincronizacion bidireccional de catalogo con terceros.

## Definition of Done por etapa

Una tarea de implementacion se considera terminada cuando:

1. Respeta los contratos de esta carpeta `docs/`.
2. Tiene validaciones de frontend y backend alineadas.
3. Incluye manejo de estado de carga, error y vacio.
4. Incluye pruebas minimas del camino feliz y de validaciones clave.
5. No introduce dependencias fuera de la pila acordada sin actualizar documentacion.
