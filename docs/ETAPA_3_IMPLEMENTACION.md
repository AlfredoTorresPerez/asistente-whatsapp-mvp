# ETAPA 3 - Autenticacion, seguridad basica y usuarios

## Implementado

### Backend

- Login con JWT en `POST /api/v1/auth/login`.
- Logout logico en `POST /api/v1/auth/logout`.
- Usuario autenticado en `GET /api/v1/auth/me` y `GET /api/v1/users/me`.
- Recuperacion de contrasena en `POST /api/v1/auth/forgot-password`.
- Restablecimiento de contrasena en `POST /api/v1/auth/reset-password`.
- Cambio de contrasena en `POST /api/v1/security/change-password` y `POST /api/v1/users/me/change-password`.
- Hash de contrasena con BCrypt.
- Filtro JWT stateless.
- Roles normalizados para Fase 1: ADMIN, MANAGER, AGENT, READ_ONLY.
- Auditoria basica de login exitoso, login fallido, recuperacion solicitada, recuperacion completada, cambio de contrasena y logout.
- Endpoint de auditoria: `GET /api/v1/security/audit-log`.

### Frontend

- Login conectado al backend real.
- Guardado controlado de token y usuario en localStorage.
- Rutas privadas protegidas.
- Logout con confirmacion.
- Recuperar contrasena conectado a API.
- Confirmacion de correo enviado.
- Restablecer contrasena conectado a API.
- Perfil de usuario basico.
- Cambiar contrasena conectado a API.
- Menu de usuario actualizado.

## Notas tecnicas

- OpenWA no fue modificado en esta etapa.
- WhatsApp Business Platform no fue incorporado.
- Se mantiene el monolito modular.
- El usuario demo `admin@demo.cl` acepta temporalmente `Cambiar123!` para evitar bloqueo por hash seed incompatible en entornos locales.

## Validacion pendiente

No se pudo ejecutar Maven Wrapper porque el entorno no tiene acceso a internet para descargar Maven. Tampoco se pudo ejecutar `pnpm build` porque `pnpm` no esta disponible y el entorno local no resolvio tipos instalados por pnpm con `npm run build`.

Comandos recomendados en ambiente local del desarrollador:

```bash
cd backend-java
chmod +x mvnw
./mvnw test

cd ../frontend-react
pnpm install
pnpm build
```
