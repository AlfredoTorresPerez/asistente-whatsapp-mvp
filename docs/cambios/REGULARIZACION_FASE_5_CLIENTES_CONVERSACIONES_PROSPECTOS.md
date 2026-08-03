# Regularizacion Fase 5 - Clientes, Conversaciones y Prospectos

Fecha: 2026-08-03

## Cambios realizados

- Conversaciones centraliza etiquetas visibles en espanol para estados operativos: Abierta, Pendiente del cliente, Pendiente del negocio, Resuelta y Archivada.
- Se eliminaron etiquetas visibles de datos simulados y acciones fuera del alcance actual desde el detalle de conversacion.
- Las casillas de seleccion en conversaciones ahora habilitan acciones masivas reales para marcar como leidas y resolver.
- La accion de resolver usa textos de usuario consistentes y confirma el cambio con retroalimentacion clara.
- Prospectos expone sucursal como dato funcional en listado, filtro, alta, edicion, detalle y creacion desde conversacion.
- El servidor valida que la sucursal asignada al prospecto pertenezca a la empresa y este activa.
- Al crear un prospecto desde una conversacion se hereda la sucursal detectada cuando exista, con posibilidad de corregirla antes de guardar.
- El filtro de origen acepta prospectos creados desde pagina publica y los traduce como "Pagina publica".
- Se retiraron textos de demostracion y etiquetas internas del flujo de prospectos.

## Alcance pendiente documentado

- El modelo actual de `lead` no tiene campos estructurados para servicio de interes, probabilidad, proxima accion, fecha de seguimiento, ultimo contacto ni motivo de perdida.
- Editar y eliminar notas requiere una evolucion no destructiva del modelo de notas con auditoria de cambios; no se implemento como cambio rapido para evitar perdida de trazabilidad.

## Archivos involucrados

- `backend-java/src/main/java/com/asistentewhatsapp/leads/api/CreateLeadFromConversationRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/leads/api/CreateLeadRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/leads/api/LeadController.java`
- `backend-java/src/main/java/com/asistentewhatsapp/leads/api/LeadDetailResponse.java`
- `backend-java/src/main/java/com/asistentewhatsapp/leads/api/LeadSummaryResponse.java`
- `backend-java/src/main/java/com/asistentewhatsapp/leads/api/UpdateLeadRequest.java`
- `backend-java/src/main/java/com/asistentewhatsapp/leads/application/LeadService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/leads/infrastructure/LeadJdbcRepository.java`
- `frontend-react/src/modules/conversations/pages/ConversationsPage.tsx`
- `frontend-react/src/modules/conversations/pages/conversationInbox.ts`
- `frontend-react/src/modules/conversations/pages/conversationInbox.test.ts`
- `frontend-react/src/modules/bookings/pages/CustomerBookingsPage.test.tsx`
- `frontend-react/src/modules/leads/leadOptions.ts`
- `frontend-react/src/modules/leads/pages/EditLeadPage.tsx`
- `frontend-react/src/modules/leads/pages/LeadDetailPage.tsx`
- `frontend-react/src/modules/leads/pages/NewLeadFromConversationPage.tsx`
- `frontend-react/src/modules/leads/pages/NewLeadPage.tsx`
- `frontend-react/src/modules/leads/pages/ProspectsPage.tsx`
- `frontend-react/src/services/api/leadsApi.ts`
- `frontend-react/src/services/api/types.ts`

## Validacion

- `backend-java`: `.\mvnw.cmd spotless:apply`
- `backend-java`: `.\mvnw.cmd -DskipTests compile`
- `backend-java`: `.\mvnw.cmd "-Dtest=ConversationServiceDispatchConsistencyTest" test`
- `frontend-react`: `pnpm build`
- `frontend-react`: `pnpm test -- --run src/modules/conversations/pages/conversationInbox.test.ts src/modules/conversations/pages/conversationPagination.test.ts src/modules/bookings/pages/CustomerBookingsPage.test.tsx`

