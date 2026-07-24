# Reporte de Madurez Local — Asistente WhatsApp MVP

**Fecha:** 2026-07-24  
**Branch:** `codex/mvp-local-100`  
**Commit:** `de4942c`  

---

## Resumen de Madurez

| Dimensión | Estado | Evidencia |
|---|---|---|
| Build reproducible | ✅ 5/5 | `pnpm install --frozen-lockfile` + `pnpm build` + `pnpm test --run` (90/90 tests) |
| Backend compila | ✅ 5/5 | `.\mvnw.cmd clean verify` con perfil `local` |
| Docker compose | ✅ 5/5 | 3 contenedores healthy (postgres, backend, frontend) |
| Sin dependencias externas | ✅ 5/5 | Perfil `local-base` deshabilita Cloud API, WhatsApp, Calendar |
| Sin archivos huérfanos | ✅ 5/5 | `package-lock.json` eliminado, duplicado `asistente-whatsapp-mvp/` eliminado |
| Scripts de automatización | ✅ 5/5 | 6 scripts: setup, start, stop, reset, verify, package |
| Tests E2E pasan | ✅ 5/5 | `00-local-maturity.spec.ts` — 8 tests pasan |
| Clean temporal | ✅ 5/5 | `clean-local.ps1` elimina node_modules, target, dist, logs, coverage |
| .gitignore completo | ✅ 5/5 | Cubre node_modules, target, dist, *.env, lockfiles |
| Perfiles Spring separados | ✅ 5/5 | `local-base` (seguro) + `local-whatsapp-cloud` (Meta) |
| WhatsApp URLs seguras | ✅ 5/5 | `buildPublicWhatsAppUrl()` retorna `string \| null` |

## Problemas Corregidos

1. **Ciclo mark-read infinito** — Frontend: `useRef` guard + `retry: false` + verificación `isPending`. Backend: `AND unread_count > 0` hace UPDATE idempotente.
2. **Ruido 429 en consola** — `console.warn` en vez de `traceService.error`.
3. **Package manager inconsistente** — Migrado a `pnpm@10.18.3` exclusivo.
4. **Copia duplicada del repo** — Eliminado `asistente-whatsapp-mvp/`.
5. **Sin script de limpieza** — Creado `scripts/clean-local.ps1`.
6. **Perfil único rígido** — Separado en `local-base` + `local-whatsapp-cloud`.
7. **WhatsApp URL sin validación** — `buildPublicWhatsAppUrl()` valida env var, retorna `null` si no existe.
8. **Docker compose sin defaults seguros** — Cloud API disabled, channel SIMULATED por defecto.
9. **Sin scripts de automatización** — Creados 6 scripts para ciclo de vida local.
10. **Sin verificación E2E de madurez** — Creado `00-local-maturity.spec.ts`.

## Scripts Disponibles

| Script | Propósito |
|---|---|
| `scripts/local-setup.ps1` | Verifica prereqs (Java 21, Docker, Node, pnpm), instala deps, compila backend + frontend |
| `scripts/local-start.ps1` | Levanta servicios Docker (-Profile, -Build) |
| `scripts/local-stop.ps1` | Detiene servicios Docker (-Volumes para borrar DB) |
| `scripts/local-reset.ps1` | Reset completo: stop + clean + setup + start + verify |
| `scripts/local-verify.ps1` | Verifica salud (contenedores, health endpoints, login, API smoke) |
| `scripts/local-package.ps1` | Empaqueta JAR + frontend dist + config (-DockerImages) |
| `scripts/clean-local.ps1` | Limpia artefactos regenerables (-CleanDockerVolumes) |

## Estado Actual de Docker

```
asistente-postgres   Up (healthy)   5433:5432
asistente-backend    Up (healthy)   8080:8080
asistente-frontend   Up (healthy)   5173:5173
```

## Próximos Pasos

- Integración CI/CD con GitHub Actions
- Pruebas de carga y estrés
- Documentación de arquitectura
- Monitoreo con Prometheus + Grafana (perfil `monitoring`)
- HTTPS local autosigned con Caddy (perfil `https`)
