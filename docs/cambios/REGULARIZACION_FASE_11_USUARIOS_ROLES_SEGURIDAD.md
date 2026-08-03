# Regularizacion Fase 11 - Usuarios, roles y seguridad

Fecha: 2026-08-03

## Alcance aplicado

Esta fase regulariza la gestion administrativa de usuarios y seguridad sin exponer contrasenas temporales ni identificadores internos al usuario final. Los cambios reutilizan el flujo existente de restablecimiento de contrasena para emitir enlaces de un solo uso.

## Cambios implementados

### Usuarios

- El listado ahora muestra nombre, correo, rol traducido, estado, sucursales segun alcance, MFA, intentos fallidos, ultimo acceso y fecha de creacion.
- Se agregaron acciones reales:
  - Ver.
  - Editar.
  - Desactivar.
  - Restablecer acceso.
  - Revocar sesiones.
- Las acciones sensibles requieren confirmacion.
- La desactivacion revoca sesiones activas del usuario.
- Se evita mostrar identificadores internos junto al rol.

Archivos:

- `frontend-react/src/modules/administration/pages/AdminUsersPage.tsx`
- `frontend-react/src/services/api/administrationApi.ts`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/api/AdminUserController.java`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/application/AdminUserService.java`

### Creacion y acceso de usuario

- El formulario dejo de pedir una contrasena temporal visible al administrador.
- El servidor genera una credencial inicial interna no visible.
- Al crear un usuario activo se solicita un enlace de acceso de un solo uso mediante el flujo existente de restablecimiento de contrasena.
- El restablecimiento administrativo reutiliza el mismo flujo y registra auditoria.
- Los perfiles privilegiados muestran MFA como requisito operativo.

Archivos:

- `frontend-react/src/modules/administration/pages/AdminUserFormPage.tsx`
- `backend-java/src/main/java/com/asistentewhatsapp/administration/application/AdminUserService.java`

### Seguridad

- Se agrego bloqueo de contrasenas comunes en validacion de servidor.
- La pantalla de seguridad muestra controles de contrasenas comunes, MFA para perfiles privilegiados y revocacion de sesiones.
- Se agrego matriz operativa por modulo con permisos minimos: ver, crear, editar, eliminar, exportar, administrar, aprobar y enviar.
- Se mantiene auditoria al modificar politicas y acciones administrativas.

Archivos:

- `backend-java/src/main/java/com/asistentewhatsapp/security/application/PasswordPolicyService.java`
- `frontend-react/src/modules/administration/pages/AdminSecurityPage.tsx`

## Validaciones ejecutadas

- `mvn -q -DskipTests compile`
- `pnpm build`
- `pnpm test -- --run src/modules/security/pages/ProfilePage.test.tsx`

Resultado: compilacion backend correcta, compilacion frontend correcta y prueba de seguridad disponible correcta.

## Pendientes controlados

- La activacion tecnica de MFA aun requiere modelo persistente y verificacion de segundo factor.
- Las excepciones por sucursal y permisos efectivos por usuario requieren un endpoint dedicado para mostrar calculo real por usuario.
- El vencimiento de bloqueo de cuenta no esta modelado como campo independiente; se mantiene el bloqueo por estado de cuenta e intentos fallidos.
