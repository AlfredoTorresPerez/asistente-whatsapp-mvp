# Informe de eliminacion completa de `whatsapp-web-service`

> **Fecha:** 2026-08-01
> **Objetivo:** eliminar completa y verificablemente el servicio externo de WhatsApp Web (whatsapp-web.js / Puppeteer / QR / puerto 3001) del repositorio, conservando la WhatsApp Cloud API de Meta y el proveedor simulado embebido.

## 1. Objetivo

- Eliminar `whatsapp-web-service` (servicio Node/Express con `whatsapp-web.js`, Puppeteer/Chromium, QR y puerto 3001) de todo el repositorio: codigo, UI, configuracion, scripts, documentacion e infraestructura.
- Conservar intactos: WhatsApp Cloud API de Meta (webhook firmado `X-Hub-Signature-256`) y el proveedor simulado embebido.
- Los unicos proveedores de canal habilitados son `META_CLOUD_API` y `SIMULATED`, con fail-fast ante valores desconocidos y default local `SIMULATED`.
- Entregar evidencia de validacion reproducible.

## 2. Alcance

- Inventario previo de referencias (codigo activo, UI, envs, composes, scripts, docs).
- Validacion de que la Cloud API de Meta es independiente del servicio Node.
- Eliminacion por fases sin afectar reservas, agenda, IA ni la Cloud API.
- Validaciones finales: greps residuales, compilacion, tests, composes y frontend.

## 3. Inventario previo

El servicio Node ya no estaba en ningun compose (eliminado en el commit `3b8e6d9`). Quedaban:

- Directorio `whatsapp-web-service/` en disco (13 archivos).
- Codigo Java activo: paquete `channels/infrastructure/whatsappweb/`, capa admin `WhatsAppWeb*`, endpoint `/refresh-qr`, repositorio de sesiones.
- UI: pagina de conexion WhatsApp Web (`/admin/whatsapp-web`) con QR, tarjetas en administracion y configuracion.
- Envs: variables `APP_WHATSAPP_WEB_*`, `WHATSAPP_WEB_*` y perfiles compose.
- Scripts: `start_mvp_public_link`, `test-ai-auto-reply-local`, `test-manual-auto-reply`, `test-webhook-local`, `test-whatsapp-webhook-local`, `verify_mvp_local` (10 archivos).
- Documentacion: ~40 archivos con referencias.

## 4. Validacion de Meta Cloud API independiente

- `WhatsAppCloudApiAdapter` es una clase Java que usa `RestClient` hacia `graph.facebook.com`, con dry-run, metricas y manejo de errores.
- Webhook GET/POST con verificacion de firma `X-Hub-Signature-256` (HMAC-SHA256 con App Secret) en tiempo constante.
- Suite de pruebas: `WhatsAppCloudApiAdapterTest` 13/13 y `WhatsAppCloudWebhookParserTest` 8/8 verdes.
- No existe ninguna dependencia de la Cloud API sobre el servicio Node: funcionaba como adaptador alternativo del canal y es la opcion productiva.

## 5. Impacto por fases

1. **Backend:** eliminar paquete `whatsappweb/`, capa admin `WhatsAppWeb*`, endpoint `/refresh-qr`; consolidar repositorio; agregar simulador embebido y admin de canal.
2. **Frontend:** reemplazar pagina QR por `WhatsAppChannelPage`; migrar rutas, labels y tipos.
3. **Configuracion:** limpiar envs, composes, gitignore y perfiles.
4. **Scripts y tooling:** eliminar scripts del servicio y perfiles `whatsapp`.
5. **Documentacion:** eliminar 10 docs obsoletos y actualizar el resto.
6. **Validacion:** greps, tests, composes y veredicto.

## 6. Cambios backend

- Eliminado el paquete `com.asistentewhatsapp.channels.infrastructure.whatsappweb` y la capa de administracion `WhatsAppWeb*`.
- Eliminado el endpoint `POST /refresh-qr` y su flujo en la UI.
- Repositorio consolidado: `WhatsAppChannelJdbcRepository` compartido en `channels/infrastructure/`, con imports actualizados en administracion, aiagents, bookings, channels y tests.
- Nuevo `SimulatedWhatsAppProvider` (proveedor simulado embebido) + `WhatsAppChannelSimulatorController` (`POST /api/v1/test/whatsapp-inbound`).
- Nuevo admin de canal: `AdminWhatsAppChannelController` + `WhatsAppChannelAdministrationService` + DTOs.
- `WhatsAppConfigurationService` sin QR; `WhatsAppChannelProperties.Provider` enum `{META_CLOUD_API, SIMULATED}` con default `SIMULATED`.
- Fail-fast en `WhatsAppChannelBootstrapValidator` ante proveedor no soportado.
- Seguridad: rutas `/whatsapp-web/**` retiradas de `SecurityConfig` y `SecurityPublicPaths`.
- Labels corregidos: `providerLabel` "Modo simulado local", actor de eventos "Canal WhatsApp".

### Hallazgo corregido durante la validacion

La suite completa detecto 12 errores de contexto nuevos (`ConflictingBeanDefinitionException`) por clases con el mismo nombre simple y mismo bean name:

- `administration/api/WhatsAppChannelController` vs `configuration/api/WhatsAppChannelController` -> renombrado a `AdminWhatsAppChannelController`.
- `configuration/infrastructure/WhatsAppChannelJdbcRepository` vs `channels/infrastructure/WhatsAppChannelJdbcRepository` -> bean name explicito `@Repository("whatsAppChannelConfigurationJdbcRepository")` en el de configuration.

Tras la correccion, la suite completa vuelve a su linea base preexistente (ver seccion 12).

## 7. Cambios frontend

- Eliminada `WhatsAppWebConnectionPage.tsx`; `administration/index.ts` exporta `WhatsAppChannelPage`.
- Router: `/admin/whatsapp-web` -> `/admin/whatsapp-channel`.
- `navigation.ts`: labels, rutas y descripciones migrados a "Canal de WhatsApp".
- `AdministrationPage.tsx`: type `AdminArea 'whatsapp-channel'`, helpers sin `QR_PENDING`, usa `summary.whatsapp.status`.
- `ConfigurationPage.tsx`: `PROVIDER_WHATSAPP_WEB` -> `PROVIDER_SIMULATED`, sin `QrConnectionCard`/`QrBlock`/RefreshQr; `SimulatedChannelHeader` con badge "Modo simulado".
- `configurationApi.ts`: eliminado `refreshWhatsAppConfigurationQrRequest()`.
- `types.ts`: eliminados los DTO duplicados de test-message; se conserva el par canonico.
- MSW handlers: rutas `/whatsapp-web/*` -> `/whatsapp-channel/*`; eliminados `/refresh-qr`.
- Tests actualizados: `router.test.tsx`, `conversationInbox.test.ts`, mock `profile.ts` (13/13 verdes).

## 8. Configuracion y entornos

- `backend-java/.env`, `.env.example`, `.env.local.example`, `.env.local.template`: solo `APP_WHATSAPP_CHANNEL_PROVIDER` (`SIMULATED` local / `META_CLOUD_API` prod); bloques `APP_WHATSAPP_WEB_*` y `WHATSAPP_WEB_*` eliminados.
- `docker-compose.local.yml`, `docker-compose.qa.yml`, `docker-compose.prod.yml`, `docker-compose.override.yml.example`: sin servicio `whatsapp-web-service` ni `APP_WHATSAPP_WEB_ENABLED`.
- `.gitignore`: seccion "WhatsApp sessions" (`.wwebjs_auth/`, `.wwebjs_cache/`, `sessions/`, `.sessions/`) eliminada.
- Volumen viejo `.docker/` (whatsapp-session/cache) eliminado del disco.

## 9. Scripts y tooling

- `git rm` de 10 scripts: `start_mvp_public_link.ps1/.sh`, `test-ai-auto-reply-local.ps1/.sh`, `test-manual-auto-reply.ps1`, `test-webhook-local.sh`, `test-whatsapp-webhook-local.ps1/.sh`, `verify_mvp_local.ps1/.sh`.
- `scripts/run-all.ps1`: linea "whatsapp-web-service y start-visual.sh excluidos" eliminada.
- `scripts/dev.ps1`: comando `up:whatsapp` eliminado (ValidateSet, case y help).
- `scripts/local-start.ps1`: perfil `whatsapp` eliminado del help.
- `scripts/local-stop.ps1`: parametro `Profile` eliminado.
- `scripts/build_mvp_review_docx.py`: 13 cadenas "WhatsApp Web/whatsapp-web" reemplazadas.
- `frontend-react/package.json`: script `docker:up:whatsapp` eliminado.
- `LOCAL_ENV_SETUP_PROMPT.md`: fila del perfil `whatsapp` eliminada.

## 10. Documentacion

- **Eliminados (10):** `WHATSAPP_WEB_ADAPTER`, `CONFIGURACION_WHATSAPP_WEB_LOCAL`, `PRUEBAS_LOCAL_NUMERO_EMPRESA_VIRTUAL`, `DEBUGGING_AUTO_REPLY_LOCAL`, `VALIDACION_LOCAL_3MIN`, `BAILEYS_ADAPTER`, `BAILEYS_IMPLEMENTATION_CHECKLIST`, `WHATSAPP_WEB_REPLACEMENT_CHECKLIST`, `WHATSAPP_WEBJS_IMPLEMENTATION_CHECKLIST`, `WHATSAPP_WEBJS_VISUAL_ADAPTER`.
- **Actualizados (15):** `AGENTS`, `API_CONTRACTS`, `DATA_MODEL`, `DEMO_LOCAL_READINESS`, `MVP_CONTROLLED_DEMO_READINESS`, `AMBIENTES_LOCAL_PROD_RESUMEN`, `AMBIENTES_WHATSAPP`, `AI_AGENTS_ORCHESTRATION`, `WHATSAPP_CLOUD_API_SETUP`, `CHECKLIST_DEMO_LOCAL`, `SCREEN_SPEC`, `NAVIGATION_MAP`, `TASKS_PHASE_1`, `visual-contract/SCREEN_IMAGE_MAPPING`, `DEMO_GUIDE`.
- **Historicos de raiz ajustados:** `AUDITORIA_FALTANTES_RESERVAS`, `RESULTADOS_QA_IA_RESERVAS`, `CHANGELOG` (nueva entrada 0.2.0), `PROMPT_CORRECCION_MVP_ORQUESTADOR_V23_4_10` (nota historica), `READEME_DEMO_LOCAL` (puertos 3001/6080 y seccion de troubleshooting sin el servicio).

## 11. Limpieza fisica y git

- `whatsapp-web-service/`: `git rm -r` (13 archivos) + `Remove-Item -Recurse -Force`; `Test-Path` confirma que ya no existe.
- `.docker/`: volumenes whatsapp-session/cache eliminados del disco.
- `git status` confirma los deletions en staged/working tree.

## 12. Validaciones ejecutadas (evidencia)

| Validacion | Resultado |
|---|---|
| `mvn spotless:apply` (gate del build) | OK |
| Compilacion y test-compile backend | OK |
| Suite completa `mvn test` | 611 tests, 38 fallas + 11 errores (linea base preexistente, 7 clases de IA/agenda; no relacionadas) |
| Paquetes afectados (8 clases, 42 tests) | 42/42 verdes |
| `docker compose -f docker-compose.local.yml config --quiet` | OK (exit 0) |
| `docker compose -f docker-compose.qa.yml config --quiet` | OK (exit 0) |
| `docker compose -f docker-compose.prod.yml config --quiet` | OK (exit 0) |
| Vitest dirigido (`router.test.tsx` + `conversationInbox.test.ts`) | 13/13 verdes |
| Frontend grep `whatsapp-web\|whatsappWeb\|WhatsAppWeb\|WHATSAPP_WEB` | Sin coincidencias |

## 13. Greps residuales y exclusiones de diseno

Patrones verificados en todo el repo: `whatsapp-web-service`, `whatsapp-web.js`, `localhost:3001`, `WHATSAPP_WEB`, `APP_WHATSAPP_WEB`, `WhatsAppWebAdapter`, `wwebjs`, `puppeteer`, `chromium`, `LocalAuth`, `RemoteAuth`.

Resultado: sin coincidencias en codigo, UI, configuracion, scripts ni documentacion operativa. Coincidencias restantes, todas intencionales y documentadas:

- **Migraciones Flyway** (`db/migration/V1...V92`): historial inmutable; conservan valores antiguos de `provider_name = 'WHATSAPP_WEB'` y registros seed historicos. No se modifican por diseno.
- **`docs/cambios/*`**: registros historicos de cambios previos; se conservan como tal.
- **`docs/PROMPT_CORRECCION_MVP_ORQUESTADOR_V23_4_10.md`**: cuerpo historico, marcado como tal en cabecera.
- **`docs/DATA_MODEL.md`**: documenta tablas `whatsapp_web_sessions`/`whatsapp_web_events` que persisten en el esquema (registros historicos de sesion); ya aclara que no son fuente de verdad.
- **`docs/AMBIENTES_WHATSAPP.md`**: documenta que los registros `channel_accounts` historicos con `WHATSAPP_WEB` se conservan en BD y que el canal activo es `META_CLOUD_API`/`SIMULATED`.
- **`AUDITORIA_FALTANTES_RESERVAS.md`, `RESULTADOS_QA_IA_RESERVAS.md`, `CHANGELOG.md`**: lineas que mencionan la eliminacion como hecho historico.
- **Playwright/Chromium** (`frontend-react/playwright.config.ts`, `.github/workflows/e2e.yml`, docs E2E, README): navegador de pruebas E2E, sin relacion con whatsapp-web.js.
- **Metodo `refreshQr()`** en la interfaz de dominio `CanalWhatsApp` y sus adaptadores: contrato interno del canal (los adaptadores Cloud API y Simulado lo implementan como no-op/funcion de estado); no expone endpoint ni UI. No estaba en la lista residual del alcance.

## 14. Riesgos y notas

- Los tests de IA/agenda con fallas preexistentes (38F+11E) no se tocaron; son independientes de este cambio y estan registrados en el estado del proyecto.
- `tsc -b` del frontend reporta fallos preexistentes no relacionados (modulo business-ai: `StatusBadge`, `PromptTemplateResponse.updatedAt`, matchers de fetchMock; `LandingImage.tsx` con `fetchPriority`).
- Las migraciones y tablas historicas del canal se conservan para no perder datos de auditoria ni conversaciones.
- MEMANTO no estuvo disponible como comando shell en el entorno de ejecucion; los aprendizajes de esta sesion no pudieron persistirse via `memanto remember` y quedan solo en este informe.

## 15. Definition of Done

1. No existe `whatsapp-web-service` en repositorio ni en disco. OK
2. Sin servicio Node, Puppeteer, Chromium, QR, noVNC ni puerto 3001 en la configuracion. OK
3. Proveedores de canal solo `META_CLOUD_API` y `SIMULATED` con fail-fast y default local simulado. OK
4. Cloud API de Meta intacta y validada por sus tests. OK
5. Frontend opera contra el canal nativo con pantalla admin "Canal de WhatsApp". OK
6. Composes validados, backend compila y suite en linea base, tests de paquetes afectados verdes. OK
7. Greps residuales limpios salvo exclusiones de diseno documentadas. OK
8. Documentacion actualizada y 10 docs obsoletos eliminados. OK

## 16. Conclusion

`whatsapp-web-service` queda eliminado de forma completa y verificable. El canal de WhatsApp es nativo del backend con dos proveedores: `META_CLOUD_API` (WhatsApp Cloud API de Meta, webhook firmado `X-Hub-Signature-256`) para produccion y `SIMULATED` (embebido, default local) para desarrollo y demos. No queda codigo, configuracion, script ni documentacion operativa que dependa del servicio externo.
