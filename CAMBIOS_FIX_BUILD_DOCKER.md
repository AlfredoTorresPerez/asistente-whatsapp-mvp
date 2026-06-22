# Correccion de build Docker local

## Problemas detectados

1. El build del frontend fallaba por una importacion con tilde en el identificador `páginateAuditLogs`, mientras el helper exporta `paginateAuditLogs`.
2. El build del backend fallaba por codigo de seguridad duplicado o desalineado:
   - `UserProfileController` duplicaba rutas ya cubiertas por `ProfileController` y `SecurityController`.
   - `DatabaseUserDetailsService` dependia de una entidad distinta a la usada por `UserAccountRepository`.
   - `security/config/SecurityConfig` y `security/infrastructure/JwtAuthenticationFilter` pertenecian a una ruta de autenticacion anterior y chocaban con la configuracion JWT vigente.

## Cambios aplicados

### Frontend React

- `frontend-react/src/modules/business-ai/pages/BusinessAiPage.tsx`
  - Se cambio `páginateAuditLogs` por `paginateAuditLogs`.
  - Se normalizaron variables locales `páginatedRows` y `páginatedLogs` a `paginatedRows` y `paginatedLogs`.

### Backend Java

Se eliminaron clases obsoletas o duplicadas que impedian compilar y/o generaban riesgo de conflicto de beans o rutas:

- `backend-java/src/main/java/com/asistentewhatsapp/security/api/UserProfileController.java`
- `backend-java/src/main/java/com/asistentewhatsapp/security/application/DatabaseUserDetailsService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/security/config/SecurityConfig.java`
- `backend-java/src/main/java/com/asistentewhatsapp/security/infrastructure/JwtAuthenticationFilter.java`

La ruta vigente de perfil queda en `ProfileController` y el cambio de contrasena queda en `SecurityController`, ambos usando `AuthenticatedUser` como principal JWT.

## Comando recomendado

```bash
docker compose -f docker-compose.local.yml up -d --build
```

## Nota de validacion

En este entorno no hay Docker ni Maven disponibles para ejecutar el build completo. La correccion se hizo sobre los errores exactos reportados en la salida de PowerShell y se validaron referencias residuales mediante busqueda estatica.
