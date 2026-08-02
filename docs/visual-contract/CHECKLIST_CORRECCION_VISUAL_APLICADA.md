# Checklist de correccion visual aplicada

## Objetivo

Aplicacion del prompt de homologacion visual sobre el frontend React del proyecto `asistente-whatsapp-mvp`, tomando como referencia obligatoria el contrato visual ubicado en `docs/visual-contract` y las laminas PNG incluidas en `.tmp-visuals/pantallas_visuales_asistente_whatsapp`.

## Archivos modificados

- `frontend-react/src/app/router.tsx`
- `frontend-react/src/app/layouts/AppLayout.tsx`
- `frontend-react/src/components/navigation/Topbar.tsx`
- `frontend-react/src/modules/visual-contract/index.ts`
- `frontend-react/src/modules/visual-contract/pages/VisualContractPages.tsx`

## Correcciones principales

| Area | Estado | Correccion aplicada |
| --- | --- | --- |
| Rutas con pantallas genericas | Corregido | Se reemplazaron rutas basadas en paginas genericas por pantallas visuales especificas. |
| Pedidos | Corregido | Se agregaron listado, alta, detalle y registro de pago con tarjetas, metricas, tabla, estados y panel contextual. |
| Catalogo | Corregido | Se agregaron listado, alta y edicion con categorias, inventario, metricas y tabla visual. |
| Reglas | Corregido | Se agregaron listado, alta, edicion y prueba con historial, condiciones y acciones. |
| Reportes | Corregido | Se agrego vista con metricas, filtros, grafico visual y tabla exportable. |
| Usuarios y roles | Corregido | Se agregaron listado, alta y edicion visualmente consistentes. |
| Seguridad | Corregido | Se agrego pantalla con politicas, sesiones, dispositivos y metricas de seguridad. |
| Pedido desde conversacion | Corregido | Se agrego formulario contextual desde conversacion. |
| Layout privado | Ajustado | Se elimino el contenedor global blanco excesivo y se dejo una composicion mas cercana al contrato: sidebar fija, topbar como tarjeta y superficie clara. |
| Topbar | Ajustado | Se aplico tarjeta blanca con borde, radio alto y sombra suave. |

## Pantallas igualadas parcialmente al contrato visual

- Login
- Recuperar contrasena
- Restablecer contrasena
- Dashboard
- Perfil
- Cambiar contrasena
- Conversaciones
- Prospectos
- Agenda
- Administracion
- Canal WhatsApp

## Pantallas corregidas en esta intervencion

- `/conversations/:conversationId/orders/new`
- `/orders`
- `/orders/new`
- `/orders/:orderId`
- `/orders/:orderId/payments/new`
- `/catalog`
- `/catalog/products/new`
- `/catalog/products/:productId/edit`
- `/automation-rules`
- `/automation-rules/new`
- `/automation-rules/:ruleId/edit`
- `/automation-rules/:ruleId/test`
- `/reports`
- `/admin/users`
- `/admin/users/new`
- `/admin/users/:userId/edit`
- `/admin/security`

## Componentes reutilizables aplicados

- `Card`
- `Button`
- `PageHeader`
- `DataTableShell`
- `StatusBadge`
- `buttonClassName`

## Validacion tecnica ejecutada

- Se ejecuto validacion TypeScript mediante `tsc -b` despues de reconstruir temporalmente los enlaces de dependencias del directorio `node_modules` extraido.
- La validacion TypeScript finalizo sin errores.

## Limitacion de validacion local

No se pudo ejecutar `vite build` ni `vitest` en el contenedor porque el paquete instalado en `node_modules` no contiene el binario nativo opcional de `rolldown` para Linux: `@rolldown/binding-linux-x64-gnu`. Esto no corresponde a un error de las pantallas modificadas, sino a una dependencia nativa faltante dentro del `node_modules` ya incluido en el ZIP original. En un entorno local, se recomienda ejecutar:

```bash
cd frontend-react
pnpm install
pnpm build
pnpm test
```

## Pendientes reales

- Comparacion visual automatizada con capturas lado a lado no ejecutada por falta del binario nativo de construccion.
- Las pantallas ya existentes de conversaciones, prospectos y agenda conservan logica real y solo requieren refinamiento fino de densidad, avatares y distribucion si se exige equivalencia pixel a pixel estricta.
