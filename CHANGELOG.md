# Changelog

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
- `whatsapp-web-service` cobertura de pruebas: 1 test (8 líneas). No se modifica por decisión del propietario.
- `start-visual.sh` faltante en Dockerfile: referenciado pero no existe. No se crea ni elimina por decisión del propietario.
- `format:check` reporta 156 archivos con estilo preexistente (no reformateados para evitar cambios masivos).

### Versiones
- Backend: `0.0.1-SNAPSHOT`
- Frontend: `0.1.0`
- WhatsApp Web Service: `0.4.0` (no modificado)
