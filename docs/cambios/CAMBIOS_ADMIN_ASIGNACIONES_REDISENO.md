# Cambios aplicados: rediseno de pantalla Administracion > Asignaciones

## Alcance
Se rediseno la pantalla `/admin/assignments` (`AdminAssignmentsPage.tsx`) para convertirla en una vista de gestion de cobertura de agenda: indicadores superiores, filtros, busqueda, listado agrupado por servicio con acordeones, panel de cobertura, paginacion server-side y CRUD completo (crear, ver, activar/desactivar, eliminar). No se adjunto imagen de referencia; el diseno sigue el contrato visual de `docs/visual-contract/`.

## Backend (nuevos endpoints, se mantiene el contrato GET existente)

- `GET /api/v1/admin/assignments` (sin cambios): lista plana, usada por `useBusinessReadiness` y consumidores previos.
- `GET /api/v1/admin/assignments/groups` (nuevo): `PagedResponse<AssignmentGroupResponse>` con `page`, `size` (max 200), `search` (servicio, profesional o cabina), `serviceId` y `coverage` (`covered` | `partial` | `none`).
  - `AssignmentGroupResponse(serviceId, serviceName, serviceCode, professionals[], rooms[], professionalsCount, roomsCount, covered)`.
  - Paginacion por servicio (unidad = `aesthetic_service`); items de profesionales y cabinas se cargan con 2 queries IN (sin N+1).
  - `covered` = al menos un profesional activo Y una cabina activa.
- `GET /api/v1/admin/assignments/summary` (nuevo): `AssignmentSummaryResponse(totalServices, coveredServices, partialServices, uncoveredServices)`.
- `PATCH /api/v1/admin/assignments/{assignmentId}` (nuevo): body `{ active: boolean }`; actualiza `active` + `updated_at` en la tabla correspondiente; 404 si no existe.
- Autorizacion: los 3 nuevos endpoints usan `ASSIGNMENT_VIEW`; el PATCH usa `ASSIGNMENT_MANAGE` (OWNER/ADMIN ya lo tienen sembrado).
- DTOs nuevos: `AssignmentGroupResponse`, `AssignmentSummaryResponse`, `AssignmentActiveRequest`.

## Frontend

- `src/services/api/assignmentsApi.ts`: `listAssignmentGroupsRequest`, `getAssignmentsSummaryRequest`, `setAssignmentActiveRequest` (PATCH). `listAssignmentsRequest` intacto.
- `src/services/api/types.ts`: `AssignmentGroupResponse`, `AssignmentSummaryResponse`, `AssignmentActiveRequest`.
- `AdminAssignmentsPage.tsx` reescrito:
  - 4 tarjetas de indicadores (Servicios, Con cobertura, Parciales, Sin asignar).
  - Filtros: busqueda con debounce de 400ms, servicio y cobertura; la pagina se reinicia al cambiar filtros.
  - Acordeones por servicio (expandibles) con columnas de profesionales y cabinas; badge de estado activo/inactivo por item.
  - CRUD: crear (modal con tipo profesional/cabina, servicio y entidad), activar/desactivar (PATCH), eliminar con `ConfirmDialog`.
  - Panel lateral "Cobertura por servicio" (sticky en desktop).
  - Paginacion "Pagina X de Y · 10 servicios por pagina" con Anterior/Siguiente.
  - Estados de carga, error y vacio; permisos de UI con `usePermissions` (`ASSIGNMENT_MANAGE` oculta crear/activar/eliminar para roles de solo vista).
- Componentes nuevos en `src/modules/administration/pages/assignments/`: `AssignmentsSummaryCards`, `AssignmentsCoveragePanel`, `AssignmentGroupsList`, `CreateAssignmentDialog`.

## Datos y cobertura

- Cobertura e indicadores cuentan solo asignaciones `active = true`.
- El listado muestra todas las asignaciones (activas e inactivas) con su badge, para que la activacion/desactivacion sea operable.
- La base del listado es `aesthetic_service` (todos los servicios del negocio), no solo servicios activos del catalogo.

## Reparacion preexistente (fuera del alcance funcional)

- Los tests de `aiagents/application` no compilaban porque el constructor de `BookingAgent` cambio (sesion previa) de 2 a 5 argumentos. Se actualizaron las 8 invocaciones en 7 archivos de test con mocks de `BookingConfirmationJdbcRepository`, `BookingConfirmationService` y `CompleteAgendaJdbcRepository`.
- Pendiente de la sesion previa (NO parte de este cambio): 49 fallos de comportamiento en tests de `aiagents` (routing de reservas) y 1 fallo de frontend en `router.test.tsx` (Caso 1, item "Sedes" del sidebar). Ambos preexistentes y ajenos a este rediseno.

## Validacion local

- Backend: `mvn spotless:apply compile` OK; `mvn test-compile` OK. SQL validado contra el Postgres real (summary: 56 servicios/56 cubiertos; busqueda por profesional y paginacion OK).
- Frontend: `tsc --noEmit` sin errores; `eslint` sin warnings; `vitest run` 150/151 (1 fallo preexistente).
