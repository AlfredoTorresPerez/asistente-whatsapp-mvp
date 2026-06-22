# Cambios administracion distribuida por bloques

## Objetivo

Se ajusto la pantalla de Administracion para que los bloques administrativos se distribuyan como tarjetas/pestanas horizontales, siguiendo el mismo patron visual aplicado en IA del Negocio.

## Archivo modificado

- `frontend-react/src/modules/administration/pages/AdministrationPage.tsx`

## Cambios aplicados

- Se reemplazo el bloque antiguo de tarjetas 2x2 por una navegacion compacta por areas.
- Se agregaron cuatro areas administrativas seleccionables:
  - Configuracion de empresa.
  - Conexion WhatsApp Web.
  - Usuarios y roles.
  - Seguridad.
- Cada area muestra descripcion breve, indicadores de estado y un panel de detalle inferior.
- Se mantiene acceso directo a las rutas existentes:
  - `/admin/company`
  - `/admin/whatsapp-web`
  - `/admin/users`
  - `/admin/security`
- Se conservaron los indicadores superiores de resumen administrativo.
- Se redujo densidad visual con tarjetas compactas para mejorar contencion en pantalla.

## Validacion realizada

- Se reviso sintaxis TSX del archivo modificado.
- No se ejecuto `pnpm build` en este entorno porque `corepack` intenta descargar `pnpm` desde el registro externo y no hay acceso de red.

## Validacion sugerida

```powershell
docker compose -f docker-compose.local.yml down --remove-orphans
docker rm -f asistente-whatsapp-whatsapp-web asistente-whatsapp-postgres asistente-whatsapp-backend asistente-whatsapp-frontend
docker compose -f docker-compose.local.yml up -d --build
```

Luego abrir:

```text
http://localhost:5173/admin
```
