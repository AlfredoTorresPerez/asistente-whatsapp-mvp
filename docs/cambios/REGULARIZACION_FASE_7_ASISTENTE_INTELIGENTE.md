# Regularizacion Fase 7 - Asistente inteligente

Fecha: 2026-08-03

## Alcance aplicado

- Se eliminaron referencias visibles a productos y textos de fase en la pantalla de IA.
- La configuracion general mantiene un boton explicito `Guardar configuracion`, aun sin cambios pendientes.
- Se muestra ultima modificacion y usuario modificador cuando el backend entrega esa informacion.
- Se agrego una politica visible de confianza minima por tipo de accion: informar, precio, disponibilidad, crear cita, reprogramar, cancelar, promociones y derivacion.
- El panel `Probar asistente` ahora muestra mensaje recibido, intencion detectada, datos extraidos, servicio, sucursal, fecha, hora, profesional, confianza, reglas aplicadas, disponibilidad consultada, advertencias, respuesta propuesta y motivo de derivacion.
- El envio a conversacion real conserva el flujo seleccionar, revisar, confirmar y registrar por backend; los telefonos se muestran enmascarados.
- La informacion del negocio usa mensajes positivos: servicios con precio, servicios con duracion, cobertura, disponibilidad, horarios y promociones vencidas.
- Las consultas por revisar muestran fecha, cliente, pregunta, respuesta, confianza, regla aplicada, motivo de revision, correccion propuesta, estado y responsable con textos de negocio.

## Archivos modificados

- `frontend-react/src/modules/business-ai/pages/BusinessAiPage.tsx`
- `frontend-react/src/modules/business-ai/components/AssistantGeneralSettings.tsx`
- `frontend-react/src/modules/business-ai/components/AssistantCapabilities.tsx`
- `frontend-react/src/modules/business-ai/components/AssistantTestPanel.tsx`
- `frontend-react/src/modules/business-ai/components/BusinessAiAdvancedSettings.tsx`
- `frontend-react/src/modules/business-ai/components/KnowledgeBaseModal.tsx`
- `frontend-react/src/modules/business-ai/components/UnresolvedQueriesPanel.tsx`
- `frontend-react/src/modules/business-ai/hooks/useBusinessAiPreview.ts`
- `frontend-react/src/modules/business-ai/hooks/useBusinessKnowledgeHealth.ts`
- `frontend-react/src/modules/business-ai/hooks/useBusinessReadiness.ts`
- `frontend-react/src/modules/business-ai/lib/constants.ts`
- `frontend-react/src/modules/business-ai/__tests__/BusinessAiPage.test.tsx`

## Validacion

- Backend: `mvn -q -DskipTests compile`
- Frontend: `pnpm build`
- Pruebas relacionadas: `pnpm test -- --run src/modules/business-ai/__tests__/BusinessAiPage.test.tsx`

## Riesgos residuales

- El backend persiste actualmente un umbral general de derivacion. La separacion por accion quedo visible y documentada; persistirla por empresa requiere una migracion no destructiva y ajuste del contrato API.
