# Cambios aplicados al proyecto Asistente WhatsApp MVP

## Alcance ejecutado

Se aplicaron correcciones sobre el proyecto para cubrir los requerimientos del prompt: catálogo, reglas, reorganización de IA del Negocio, responsividad básica, paginación de auditoría y pruebas auxiliares.

## Archivos modificados o agregados

- frontend-react/src/modules/catalog/pages/CatalogPage.tsx
- frontend-react/src/modules/rules/pages/AutomationRulesPage.tsx
- frontend-react/src/modules/rules/pages/AutomationRuleFormPage.tsx
- frontend-react/src/modules/rules/lib/ruleTypeLabels.ts
- frontend-react/src/modules/rules/lib/ruleTypeLabels.test.ts
- frontend-react/src/modules/business-ai/pages/BusinessAiPage.tsx
- frontend-react/src/modules/business-ai/lib/businessAiHelpers.ts
- frontend-react/src/modules/business-ai/lib/businessAiHelpers.test.ts

## Cambios funcionales

- Catálogo: se agregaron acciones visibles para editar y desactivar servicios y productos.
- Catálogo: la desactivación pide confirmación y preserva el registro como inactivo cuando el contrato existente lo permite.
- Reglas: se agregaron acciones visibles para editar y desactivar reglas.
- Reglas: se incorporó mapeo visual de tipos técnicos a etiquetas en español, sin cambiar el contrato técnico con el servidor.
- IA del Negocio: se separó la operación en Configuración del asistente, Vista previa de conversación y Base de conocimiento.
- Configuración del asistente: se agregó horario editable de lunes a domingo con hora de inicio y hora de término.
- Vista previa de conversación: se incorporaron temas permitidos y bloqueados como casillas seleccionadas por defecto.
- Auditoría IA: se ordenan mensajes por fecha descendente y se muestran 5 registros por página.
- Simulador: queda ubicado dentro de Vista previa de conversación.
- Base de conocimiento: queda como área propia con listado compacto y controles de navegación.

## Cambios visuales

- Se reemplazaron tablas principales por listados compactos en tarjetas responsivas para evitar desbordamiento horizontal.
- Se redujo densidad visual en áreas modificadas mediante grillas, tarjetas compactas, paginación y contenedores acotados.
- Se diferenciaron visualmente registros activos e inactivos.
- Se normalizaron textos visibles relevantes al español.

## Pruebas agregadas

- Pruebas del mapeo visual de tipos de reglas.
- Pruebas de días requeridos, validación horaria y paginación de auditoría IA.

## Validación ejecutada

- Revisión sintáctica de los archivos TypeScript y TSX modificados mediante transpilación local con TypeScript: correcta.
- Validación manual de funciones auxiliares de reglas, horario y auditoría: correcta.

## Comandos no completados por entorno

- pnpm test: no se pudo ejecutar porque pnpm no está disponible en el entorno.
- npm test: no se pudo ejecutar porque el ejecutable local de Vitest no tiene permisos correctos y las dependencias del archivo comprimido están incompletas o con enlaces no resolubles.
- pnpm build: no se pudo ejecutar por la misma limitación de dependencias locales.

## Recomendación para validar en entorno local

Dentro de frontend-react ejecutar:

```bash
pnpm install
pnpm test -- --run
pnpm build
```

Si se usa npm en lugar de pnpm, regenerar dependencias de forma limpia antes de ejecutar pruebas y compilación.

## Riesgos pendientes

- La persistencia real del horario de atención queda preparada en interfaz, pero depende de que exista un punto de acceso en el servidor.
- La selección de temas permitidos y bloqueados queda en estado local si no existe contrato de persistencia en el servidor.
- La validación completa de compilación y pruebas debe repetirse en un entorno con dependencias reinstaladas correctamente.
