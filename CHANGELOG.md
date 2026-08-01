# Changelog

## 0.2.0 (2026-08-01)

### Eliminado
- `whatsapp-web-service` (servicio Node/Express con `whatsapp-web.js`, Puppeteer/Chromium, QR y puerto 3001): eliminado de repositorio, composes, scripts, UI, docs y configuración. El canal de WhatsApp es nativo del backend con proveedores `META_CLOUD_API` (WhatsApp Cloud API de Meta, webhook firmado `X-Hub-Signature-256`) y `SIMULATED` (embebido, default local). Ver informe completo en `docs/INFORME_ELIMINACION_WHATSAPP_WEB.md`.

### Corregido
- Referencias residuales a `whatsapp-web-service` en documentación de raíz y `docs/` actualizadas a la realidad actual.

## 0.1.0 (2026-07-15)

### Agregado
- Perfil `local` de Spring Boot (`application-local.yml`) para desarrollo desde IDE sin dependencia de Docker ni seed data automática.
- Perfiles Maven (`unit`, `integration`, `local`) con separación Surefire/Failsafe.
- MSW (Mock Service Worker) en frontend: `src/test/mocks/server.ts`, `handlers.ts`, `data/`.
- Pruebas de perfil de usuario (`ProfilePage.test.tsx`) — 7 tests (carga, error, formulario, botones, navegación). 48 → 55 tests frontend.
- Prettier: `.prettierrc`, `.prettierignore`, scripts `format` y `format:check`.
- Task runner central: `scripts/run-all.ps1` y `scripts/run-all.sh`.
- Backup sidecar en docker-compose (perfil `backup`, cron configurable, rotación 7 días).
- `CHANGELOG.md`.
- `DEVELOPMENT.md`, `CONTRIBUTING.md`.

### Corregido
- Tests de `BookingConfirmationPage`: fechas dinámicas para evitar bloqueo por ventana de cancelación cerrada (`changeWindowClosed`).
- Test de `NotificationsPage`: `'READ'` → `'Leída'` (texto en español).
- Eliminados 8 `console.log` de depuración en `CompleteAgendaPage.tsx`, `CreatePublicBookingPage.tsx`, `NewAppointmentPage.tsx`.
- CI/CD: workflow frontend actualizado a pnpm con format:check, lint, test, build.

### Seguridad
- Ningún `.env` con secretos trackeado por Git (solo `.env.example`).
- No se agregaron secretos ni credenciales reales.

### Brechas conocidas diferidas
- `whatsapp-web-service` (eliminado en 0.2.0, 2026-08-01): cobertura de pruebas en ese momento, 1 test (8 líneas). No se modificó por decisión del propietario.
- `start-visual.sh` faltante en Dockerfile: referenciado pero no existe. No se crea ni elimina por decisión del propietario.
- `format:check` reporta 156 archivos con estilo preexistente (no reformateados para evitar cambios masivos).

### Versiones
- Backend: `0.0.1-SNAPSHOT`
- Frontend: `0.1.0`
- WhatsApp Web Service: `0.4.0` (eliminado en 0.2.0, 2026-08-01)
