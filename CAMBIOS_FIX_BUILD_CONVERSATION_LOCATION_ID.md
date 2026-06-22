# Correccion de compilacion frontend - locationId en pruebas de conversaciones

## Problema

Durante `docker compose -f docker-compose.local.yml up -d --build`, el contenedor `frontend-react` fallaba en `pnpm build` por un error de TypeScript en:

`src/modules/conversations/pages/conversationInbox.test.ts`

El helper de pruebas `conversation(...)` construia objetos `ConversationSummaryResponse`, pero no incluia los nuevos campos obligatorios agregados por soporte multisede:

- `locationId`
- `locationName`

## Cambio aplicado

Se agregaron valores por defecto en el helper de pruebas:

```ts
locationId: null,
locationName: null,
```

## Archivo modificado

- `frontend-react/src/modules/conversations/pages/conversationInbox.test.ts`

## Validacion pendiente

Ejecutar nuevamente:

```bash
docker compose -f docker-compose.local.yml up -d --build
```

