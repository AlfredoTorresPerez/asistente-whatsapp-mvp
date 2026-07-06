# Cambios V23.4.10 - Auditoría y correcciones del MVP local

## Resumen

Se aplicaron correcciones mínimas derivadas de la auditoría del MVP de asistente WhatsApp para negocios. El foco fue estabilizar pruebas, reproducibilidad del adaptador WhatsApp Web, trazabilidad IA, limpieza de archivos de interfaz y documentación de riesgos de enlaces locales.

## Correcciones aplicadas

1. Se actualizó la creación de `BookingAgent` en pruebas para inyectar `TransactionalAgendaBookingService` mediante doble de prueba.
2. Se agregó constructor compatible de ocho parámetros en `AgentConversationRequest` para pruebas y usos simples existentes.
3. Se fijó `whatsapp-web.js` en versión `1.34.7` dentro de `whatsapp-web-service/package.json`.
4. Se eliminó una traza duplicada `AI_FINAL_RESPONSE` en `ConversationService`.
5. Se eliminaron archivos `.bak` de `frontend-react/src`.
6. Se agregó advertencia en `.env.example` sobre el uso de `localhost` en enlaces enviados por WhatsApp.
7. Se agregó `docs/PROMPT_CORRECCION_MVP_ORQUESTADOR_V23_4_10.md` con el prompt operativo de corrección.
8. Se agregó `scripts/verify_mvp_local.sh` para validaciones locales rápidas.

## Validaciones realizadas en este entorno

- Se validó que no quedaran instancias antiguas de `new BookingAgent(knowledgeService, locationRepository)`.
- Se validó que `whatsapp-web.js` quedara fijado en `1.34.7`.
- Se validó que no quedaran archivos `.bak` en `frontend-react/src`.
- Se validó la sintaxis básica de `whatsapp-web-service/src/server.js` con `node --check`.
- Se validó que `backend-java/mvnw` conserve permiso de ejecución.

## Validaciones no ejecutadas completamente

No se ejecutó compilación completa con Maven ni instalación completa con pnpm porque el entorno no cuenta con Maven del sistema y la descarga de dependencias externas puede requerir red. Estas validaciones deben ejecutarse en el ambiente local del desarrollador o en integración continua con acceso al registro de dependencias.

## Próximos pasos recomendados

1. Ejecutar `./scripts/verify_mvp_local.sh`.
2. Ejecutar `cd backend-java && ./mvnw test`.
3. Ejecutar `cd frontend-react && corepack pnpm install --frozen-lockfile && corepack pnpm build`.
4. Ejecutar `cd whatsapp-web-service && corepack pnpm install --frozen-lockfile && corepack pnpm check`.
5. Levantar `docker compose -f docker-compose.local.yml up -d --build`.
6. Probar el flujo completo de mensaje WhatsApp, orquestador, agenda, reserva temporal, enlace y confirmación.
