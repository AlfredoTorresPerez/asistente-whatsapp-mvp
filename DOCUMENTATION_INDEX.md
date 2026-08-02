# Índice Documental — Asistente WhatsApp MVP

> **Propósito:** navegar la documentación del repositorio por categoría y vigencia.
> **Actualizado:** 2026-08-02 (línea base Fase 2, HEAD `e9a321b`).
> Criterios de estado: **CANÓNICO** (vigente, se actualiza continuamente), **CONTRATO** (define comportamiento), **OPERATIVO** (comandos/utilidades), **HISTÓRICO** (registro de un estado pasado, puede estar desactualizado), **REGISTRO** (bitácora de cambios/resultados, histórica por naturaleza), **DISEÑO** (propuesta o arquitectura no necesariamente implementada).

## 1. Canónico (leer primero)

| Documento | Rol |
|---|---|
| `AGENTS.md` | Reglas de trabajo de los agentes; memoria persistente |
| `LOCAL_MATURITY_CONTEXT.md` | Contexto obligatorio de evaluación del ambiente local |
| `LOCAL_MATURITY_REPORT.md` | Informe de madurez local vigente |
| `README-LOCAL.md` | Guía operativa local (levantar, probar, URLs, credenciales demo) |
| `DEVELOPMENT.md` | Guía de desarrollo |
| `CHANGELOG.md` | Historial de versiones |
| `README.md` | Descripción del MVP y primeros pasos |
| `OBSERVABILIDAD_LOCAL.md` | Stack Grafana/Prometheus/Loki/Tempo/Alloy: uso y verificación |
| `DEMO_GUIDE.md` | Guía de demo (sin WhatsApp Web ni QR) |
| `CONTRIBUTING.md` | Convenciones de contribución |
| `DOCUMENTATION_INDEX.md` | Este índice |

## 2. Contratos y diseño (definen comportamiento)

| Documento | Estado |
|---|---|
| `docs/AGENTS.md` | CONTRATO Fase 1 (canal nativo del backend; sin servicio externo ni QR) |
| `docs/SCREEN_SPEC.md`, `docs/API_CONTRACTS.md`, `docs/DATA_MODEL.md`, `docs/UI_COMPONENTS.md`, `docs/NAVIGATION_MAP.md`, `docs/TASKS_PHASE_1.md` | CONTRATO Fase 1 |
| `docs/visual-contract/*` (`VISUAL_CONTRACT.md`, `DESIGN_TOKENS.md`, `LAYOUT_RULES.md`, `COMPONENT_STYLE_GUIDE.md`, `IMPLEMENTATION_CHECKLIST.md`, `SCREEN_IMAGE_MAPPING.md`) | CONTRATO visual |
| `docs/AI_AGENTS_ORCHESTRATION.md` | Diseño de orquestación IA |
| `docs/WHATSAPP_CLOUD_API_SETUP.md` | Setup de Cloud API (número comercial, webhook, firma) |
| `docs/OPENWA_ADAPTER.md` | Propuesta de adaptador externo (no implementado) |

## 3. Operativo

| Documento | Uso |
|---|---|
| `scripts/local-setup.ps1`, `local-start.ps1`, `local-stop.ps1`, `local-reset.ps1`, `local-verify.ps1`, `local-package.ps1`, `clean-local.ps1` | Ciclo de vida local |
| `scripts/observability-verify.ps1` | Verificación del stack de observabilidad (6/6 OK) |
| `scripts/grafana-captures.mjs` | Capturas de dashboards Grafana |
| `scripts/validate-docs.ps1` | Validación automática de enlaces y comandos de la documentación |
| `docs/INSTRUCCIONES_EJECUCION_AGENDA_COMPLETA.md`, `docs/USO_WINDOWS_ENLACE_NAVEGABLE.md`, `docs/DEMO_LOCAL_READINESS.md`, `docs/MVP_CONTROLLED_DEMO_READINESS.md`, `GUIA_EVALUACION_LIBRE_CLIENTE.md`, `docs/REPARACION_DNS_DOCKER_WINDOWS.md`, `docs/SOLUCION_DNS_MAVEN_DOCKER.md` | Guías operativas puntuales |

## 4. Histórico (estado pasado; pueden contener referencias desactualizadas)

| Documento | Por qué es histórico |
|---|---|
| `CHECKLIST_DEMO_LOCAL.md` | Checklist de la fase de validación local (jul-2026); usar `README-LOCAL.md` |
| `RESULTADOS_QA_IA_RESERVAS.md` | Resultado de auditoría QA (2026-07-16); no versionado (gitignored) |
| `docs/INFORME_ELIMINACION_WHATSAPP_WEB.md` | Informe de eliminación del servicio `whatsapp-web-service` (2026-08-01) |
| `docs/PROMPT_CORRECCION_MVP_ORQUESTADOR_V23_4_10.md`, `docs/ia-negocio/revision_tecnica_prompts_reglas_v22.md` | Revisiones de versiones anteriores (v22/v23.4.10) |
| `docs/CORRECCION_FK_INTENT_LOG.md`, `docs/RESPUESTA_IA_REGLAS_WHATSAPP_WEB.md`, `docs/CENTRO_ESTETICO_MODULO.md`, `docs/IMPLEMENTACION_CENTRO_ESTETICO_RESUMEN.md` | Registros de funcionalidad previa; referencias al canal antiguo corregidas/marcadas |
| `frontend-react/e2e/ANALISIS_REPOSITORIO_PRUEBAS.md` | Análisis previo a la eliminación de `whatsapp-web-service` |
| `frontend-react/docs/MATRIZ_PRUEBAS_ASISTENTE_NEGOCIOS.md` | Matriz de pruebas con casos obsoletos marcados |
| `analisis_respuestas_IA_version_2.md`, `comparacion_auditoria_IA_antes_despues.md`, `DIAGNOSTICO_PREVIO_CORRECCIONES_IA.md`, `evaluacion_respuestas_IA.md`, `instruccion_evaluadora_respuestas_IA.md`, `plan_correcciones_IA*.md`, `plan_mejoras_IA_version_2.md`, `prompt_opencode_mejoras_IA_version_2.md`, `preguntas*.md`, `BACKLOG_CAPACIDADES_NO_IMPLEMENTADAS_IA.md`, `READEME_DEMO_LOCAL.md`, `LOCAL_ENV_SETUP_PROMPT.md` | Documentos de fases previas de IA y ambiente (raíz) |

## 5. Registro de cambios y resultados (bitácoras, históricas por naturaleza)

| Patrón | Contenido |
|---|---|
| `docs/CAMBIOS_*.md`, `docs/cambios/CAMBIOS_*.md`, `docs/cambios/informe_v23*.md` | Bitácoras de cambios aplicados por versión/etapa |
| `RESULTADO_MEJORA_PASO_FECHA_HORA_RESERVA_PUBLICA.md`, `RESULTADO_MEJORA_PASO_REPROGRAMAR_CITA.md`, `RESULTADO_IMPLEMENTACION_CORRECCIONES_IA.md` | Resultados de pruebas de mejora |
| `docs/qa/PRUEBAS_MATRIZ_EXCEL_IA_V23_4_16.md`, `docs/qa/MATRIZ_EXCEL_IA_RESUMEN_V23_4_16.md` | Pruebas de matriz Excel IA (v23.4.16) |
| `AUDITORIA_FALTANTES_RESERVAS.md` | Auditoría de faltantes de reservas (actualizada al canal nativo) |

## 6. Convenciones de mantenimiento

- Los archivos de evidencia/resultados (`RESULTADOS_QA_IA_RESERVAS.md`, `registro_ejecucion_IA.json`, `docs/observabilidad-capturas/`, `frontend-react/e2e/reports/`, `database/manual/`) están en `.gitignore`; no se versionan.
- Documentos nuevos vigentes van a la sección correspondiente; bitácoras de cambios a `docs/cambios/`.
- Al eliminar funcionalidad, marcar los documentos afectados como HISTÓRICO (no borrarlos) y actualizar el índice.
- El estado de vigencia se declara en la primera línea del documento con `> **ESTADO: ...**`.
- `scripts/validate-docs.ps1` valida que las referencias a `scripts/*` y enlaces relativos de la documentación existan; ejecutar antes de confirmar cambios de documentación.
