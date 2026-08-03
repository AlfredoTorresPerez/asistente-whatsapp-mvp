# Regularizacion Fase 4 - Profesionales, Cabinas y Asignaciones

Fecha: 2026-08-03

## Cambios realizados

- Asignaciones ahora permite filtrar por sucursal, categoria, servicio, profesional, cabina y cobertura.
- El servidor aplica filtros reales sobre empresa, sucursal, servicio, categoria, profesional y cabina.
- Las asignaciones de profesionales validan servicio activo, profesional activo, nivel requerido, certificacion vigente y cobertura activa por sucursal.
- Las asignaciones de cabinas validan servicio activo, cabina activa, sucursal activa, compatibilidad del tipo de cabina y disponibilidad del servicio en esa sucursal.
- La lista de asignaciones muestra categoria y sucursales del servicio, evitando exponer codigos tecnicos como dato principal.
- Los formularios de profesionales y cabinas usan selector visual de color para agenda.
- El servidor valida que los colores recibidos tengan formato hexadecimal valido antes de guardar.

## Archivos involucrados

- `backend-java/src/main/java/com/asistentewhatsapp/administration/api/AssignmentController.java`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/api/AssignmentGroupResponse.java`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/api/ProfessionalRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/api/RoomRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/application/AssignmentService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/infrastructure/AssignmentJdbcRepository.java`
- `frontend-react/src/services/api/assignmentsApi.ts`
- `frontend-react/src/services/api/types.ts`
- `frontend-react/src/modules/administration/pages/AdminAssignmentsPage.tsx`
- `frontend-react/src/modules/administration/pages/AdminProfessionalFormPage.tsx`
- `frontend-react/src/modules/administration/pages/AdminRoomFormPage.tsx`
- `frontend-react/src/modules/administration/pages/assignments/AssignmentGroupsList.tsx`

## Validacion

- `backend-java`: `.\mvnw.cmd spotless:apply`
- `backend-java`: `.\mvnw.cmd -DskipTests compile`
- `backend-java`: `.\mvnw.cmd "-Dtest=AdminAuthorizationTest" test`
- `frontend-react`: `pnpm build`

