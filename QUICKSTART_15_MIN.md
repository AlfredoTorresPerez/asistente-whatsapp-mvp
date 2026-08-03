# QUICKSTART 15 MIN - Primer arranque del ambiente local

Guia unica para que un desarrollador nuevo configure y valide el ambiente
local del asistente de WhatsApp en ~15 minutos. Requisitos de version
unificados (ver seccion "Requisitos").

## Requisitos

| Herramienta | Version minima | Verificar con |
|---|---|---|
| JDK | 21+ (Temurin recomendado) | `java -version` |
| Node.js | 20.19+ (ver `frontend-react/.nvmrc`) | `node --version` |
| pnpm | 10.x (fijado: 10.18.3) | `pnpm --version` |
| Docker Desktop | 20+ con Compose v2 | `docker compose version` |
| Git | cualquiera reciente | `git --version` |
| RAM | 8 GB recomendado (4 GB minimo) | - |

> Nota: el proyecto usa `packageManager: pnpm@10.18.3` en `frontend-react/package.json`.
> Activa la version correcta con:
> `corepack enable && corepack prepare pnpm@10.18.3 --activate`

## Paso 1 - Clonar y configurar (2 min)

```powershell
git clone <url-del-repositorio> asistente
cd asistente
Copy-Item .env.local.template .env.local
```

Edita `.env.local` y define como minimo:

```
APP_JWT_SECRET=<clave-al-azar-larga>        # obligatorio
```

Los demas valores de la plantilla ya traen defaults seguros para el modo
SIMULATED (sin servicios externos). Los secretos del canal Meta y las
cuentas de correo/OpenAI se guardan en el Windows Credential Manager y se
restauran automaticamente al levantar (Windows).

## Paso 2 - Instalar y compilar (3-10 min)

```powershell
.\scripts\local-setup.ps1
```

Resultado esperado:

- `[OK] Java 21`
- `[OK] Node: v20.x`
- `[OK] pnpm: 10.x`
- `[OK] Lockfile presente`
- `pnpm install --frozen-lockfile` sin errores
- `Backend compilado` y `Frontend construido`

> En Linux/macOS usa `bash scripts/local-setup.sh`. En Linux/macOS los
> secretos se leen directamente de `.env.local` (no hay Credential Manager).
>
> `local-setup.ps1` NUNCA omite la instalacion solo porque exista
> `node_modules`: siempre valida contra `pnpm-lock.yaml` (instalacion
> idempotente). Usa `-SkipInstall` solo si sabes que las dependencias
> estan al dia.

## Paso 3 - Levantar el stack (1-2 min)

```powershell
.\scripts\local-start.ps1
```

Resultado esperado: al final se listan los servicios y las URLs:

- Frontend: http://localhost:5173
- Backend:  http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui/index.html
- MailHog:  http://localhost:8025

Perfiles opcionales (todos desactivados por default):

```powershell
.\scripts\local-start.ps1 -Profile observability    # metricas, logs y trazabilidad
.\scripts\local-start.ps1 -Profile all              # todo (observabilidad + backup + tunel + https)
```

## Paso 4 - Verificar (1 min)

```powershell
.\scripts\local-verify.ps1
```

Resultado esperado: `VERIFICACION COMPLETADA: TODO OK` con health de los
contenedores core, backend UP, frontend HTTP 200 y smoke test de login.

## Paso 5 - Probar el asistente

Accede a http://localhost:5173 y entra con las credenciales demo:

- Email: `admin@demo.cl`
- Password: `Cambiar123!`

El chat usa el proveedor SIMULATED por default (sin WhatsApp real). Para
probar el flujo de agenda escribele al chat del centro estetico desde la
misma aplicacion (panel demo).

Datos demo regenerables: si las reservas de ejemplo quedaron con fechas
viejas o quieres volver al estado inicial de datos:

```powershell
.\scripts\local-reset-demo.ps1
```

## Diagnostico del ambiente

Si algo falla o quieres un reporte completo y compartible:

```powershell
.\scripts\diagnose-local.ps1 -OutFile local-diagnostics.txt
```

El reporte es sanitizado (no incluye valores de secretos) y puede enviarse
para soporte. Cada fallo incluye una accion correctiva sugerida. Con esto
quedas listo para `local-start.ps1`.

## Parar y limpiar

```powershell
.\scripts\local-stop.ps1            # detiene servicios, conserva datos
.\scripts\local-stop.ps1 -Volumes   # ademas borra la base de datos
.\scripts\clean-local.ps1           # borra artefactos regenerables
```

`clean-local.ps1`:

- Elimina (regenerable): `node_modules` (con confirmacion), `dist`, `target`,
  `coverage`, caches, logs, `playwright-report`, `test-results`, `.vite`.
- NO elimina nunca: codigo, migraciones, pruebas, `.env.local`, backups,
  volumenes Docker (requieren confirmacion explicita).

## Reset completo

```powershell
.\scripts\local-reset.ps1                  # limpia, reinstala y levanta
.\scripts\local-reset.ps1 -CleanDockerVolumes
```

## Linux / macOS

Mismos flujos con scripts `.sh`:

```bash
bash scripts/local-setup.sh
bash scripts/local-start.sh
bash scripts/local-verify.sh
bash scripts/local-stop.sh
bash scripts/diagnose-local.sh -o local-diagnostics.txt
bash scripts/local-reset-demo.sh
```

## IDE vs Contenedores

- **Contenedores (recomendado)**: flujo completo con Postgres, backend,
  frontend, MailHog y perfiles opcionales (ver secciones anteriores).
- **IDE (IntelliJ IDEA)**: para desarrollar el backend sin Docker usa el
  perfil Maven `local` (H2 embebido, sin datos demo). Detalles en
  `DEVELOPMENT.md` (seccion "Desarrollo desde IDE vs contenedores").

## Si algo falla

1. Ejecuta `.\scripts\diagnose-local.ps1` y revisa las acciones sugeridas.
2. Verifica que Docker Desktop este corriendo (`docker info`).
3. Verifica versiones con la tabla de requisitos.
4. Comprueba que `.env.local` exista y tenga `APP_JWT_SECRET`.
