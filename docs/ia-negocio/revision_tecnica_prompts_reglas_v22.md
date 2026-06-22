# Revisión técnica del último ZIP: reglas, funcionalidades y prompts de IA

## Contexto de revisión

Archivo revisado: `asistente_whatsapp_mvp_orquestador_agenda_ia_fix.zip`.

Tipo de revisión: estática. Se inspeccionó estructura, migraciones, código Java, código React, documentación y archivos de configuración. No se ejecutó compilación real en este entorno.

---

## A. Resumen ejecutivo

El proyecto corresponde a un asistente de negocios por WhatsApp para centro estético, con backend Java/Spring Boot, frontend React, base PostgreSQL con Flyway, servicio WhatsApp Web y módulos de conversación, catálogo, agenda, citas, reglas, IA de negocio, multisede, administración, pedidos, seguridad y auditoría.

Hallazgos principales:

1. El proyecto tiene una estructura funcional amplia y modular.
2. La base de datos ya contiene `aesthetic_business_rule`, que permite almacenar reglas y también prompts mediante `rule_type = 'AI_PROMPT'`.
3. El último ZIP contiene prompts en frontend como `defaultPrompt`, pero no trae sembrado automáticamente `PROMPT_OPERATIVO_IA_NEGOCIO` como regla persistida en base de datos.
4. Existen reglas `AI_RESPONSE`, `BOOKING`, `AVAILABILITY`, `SAFETY`, `PAYMENT`, `CATALOG`, `COMMERCIAL`, `RECOMMENDATION`, `INVENTORY` y `CUSTOMER_HISTORY`.
5. La agenda digital está implementada con disponibilidad por sucursal, servicio, profesional, cabina, horario, bloqueo, feriado, reserva temporal, expiración, historial y recordatorios.
6. El envío de enlace de confirmación por WhatsApp está implementado para reservas temporales, pero el flujo conversacional automático hasta crear la reserva aún debe validarse en ambiente local.
7. Hay reglas y prompts que deberían persistirse para que la respuesta de IA sea coherente, especialmente el prompt operativo principal, orquestador de agenda, extracción de entidades, datos faltantes, enlace de confirmación y derivación humana.

Brechas principales:

- No hay `AI_PROMPT` sembrado por migración.
- Algunas respuestas de agenda siguen parcialmente codificadas en Java y no son completamente editables desde base de datos.
- Preferencias horarias como “en la mañana” o “en la tarde” no están completamente conectadas al motor de disponibilidad.
- Reprogramación, cancelación y pago tienen soporte parcial; no hay evidencia de enlaces públicos completos para reprogramar/cancelar/pagar.
- El prompt operativo guardado desde frontend se crea solo al presionar “Guardar instrucción”.

---

## B. Inventario de funcionalidades

| Funcionalidad | Descripción | Regla asociada | Prompt requerido | Estado |
|---|---|---|---|---|
| Autenticación y seguridad | Login, sesión, perfil, recuperación de contraseña, roles y permisos. | Seguridad, auditoría, control de acceso. | No crítico para IA. | Implementada |
| Administración | Gestión de usuarios, empresa, seguridad, WhatsApp Web. | Roles OWNER/ADMIN, configuración de canal. | No crítico para IA. | Implementada |
| Dashboard | Métricas de operación. | Lectura agregada de datos. | No. | Implementada |
| Conversaciones WhatsApp | Bandeja, mensajes entrantes/salientes, vista previa IA, aprobar y enviar. | No responder mensajes no accionables; registrar mensajes y entregas. | `PROMPT_OPERATIVO_IA_NEGOCIO`, `PROMPT_FALLBACK_BAJA_CONFIANZA`. | Implementada/parcial |
| Canal WhatsApp Web | Servicio externo con QR, estado, conexión, envío y webhook. | Solo canal WhatsApp en MVP. | Prompt no requerido; configuración sí. | Implementada/parcial |
| IA del negocio | Simulador, reglas, catálogo, auditoría, prompt operativo editable. | No inventar datos, responder desde catálogo, auditar intención. | Sí, falta sembrar `AI_PROMPT`. | Implementada con brecha de datos |
| Orquestador de agentes IA | Detecta intención, extrae entidades, enruta a agentes y guarda contexto. | Mantener contexto, no repetir datos, derivar cuando corresponda. | `PROMPT_ORQUESTADOR_AGENDA_WHATSAPP`, `PROMPT_EXTRACCION_ENTIDADES_AGENDA`. | Implementada parcial |
| Agente de agenda | Maneja reserva, cambio, cancelación y estado de reserva. | Servicio/sucursal/fecha/hora antes de confirmar. | `PROMPT_RESPUESTA_DATOS_FALTANTES_AGENDA`. | Implementada parcial |
| Catálogo estético | Servicios, productos, categorías, precios, duración, stock y reglas. | Responder solo con catálogo interno activo. | `PROMPT_CATALOGO_COMERCIAL_WHATSAPP`. | Implementada |
| Reglas de negocio | Reglas editables por tipo y prioridad. | `aesthetic_business_rule`. | Sí, tipo `AI_PROMPT` ausente por migración. | Implementada con brecha |
| Sucursales | Sedes, horarios, teléfonos, WhatsApp, acceso por usuario y rutas multisede. | Si hay varias sucursales, preguntar sucursal. | Incluido en agenda. | Implementada |
| Agenda digital completa | Disponibilidad por sede, servicio, profesional, cabina, horarios, bloqueos y feriados. | No confirmar sin validar agenda. | `PROMPT_ORQUESTADOR_AGENDA_WHATSAPP`. | Implementada |
| Reserva temporal | Crea bloqueo temporal con expiración. | Vence y libera cupo. | `PROMPT_ENVIO_ENLACE_CONFIRMACION_RESERVA`. | Implementada |
| Enlace confirmación | Genera token seguro, URL pública, estado, expiración, apertura y confirmación. | No enviar si falta dato crítico; marcar SENT si se envía. | `PROMPT_ENVIO_ENLACE_CONFIRMACION_RESERVA`. | Implementada |
| Expiración de enlace | Tarea programada libera reservas vencidas. | TEMPORARY/REQUESTED pasan a RELEASED. | Prompt de vencimiento recomendado. | Implementada |
| Recordatorios | Crea recordatorios de confirmación, 24 horas, 2 horas y post atención. | Estado SCHEDULED/SENT/FAILED/CANCELLED. | Prompt futuro de recordatorios. | Estructura implementada; envío no completamente validado |
| Reprogramación | Endpoint para reprogramar con validación de disponibilidad. | Debe existir cita y nuevo horario disponible. | `PROMPT_REPROGRAMACION_RESERVA`. | Implementada API; conversación parcial |
| Cancelación | Endpoint para cancelar con motivo. | Debe identificar reserva y confirmar intención. | `PROMPT_CANCELACION_RESERVA`. | Implementada API; conversación parcial |
| Pagos/señal | Columnas de depósito y estado de pago; módulo de pagos/pedidos. | No confirmar pago sin validación. | `PROMPT_PAGO_SENAL_RESERVA`. | Parcial |
| Pedidos | Gestión de pedidos, ítems, pago y resumen. | Stock y pago. | Prompt comercial/pago si se usa por WhatsApp. | Implementada |
| Prospectos | Leads/prospectos con conversación y sucursal. | Etapas y responsable. | No crítico. | Implementada |
| Auditoría | Registro de intención, auditoría de seguridad, mensajes y eventos. | Registrar intención, confianza, entidades y respuesta. | Incluido en reglas existentes. | Implementada |

---

## C. Inventario de reglas

| Código de regla | Descripción | Módulo | Fuente detectada | Acción requerida |
|---|---|---|---|---|
| NO_INVENTAR_DATOS | No inventar precios, horarios, stock ni disponibilidad. | IA | V7 | Mantener activa |
| NO_DIAGNOSTICO | No diagnosticar ni prometer resultados. | Seguridad/IA | V7 | Mantener activa |
| IA_RESPONDER_CON_CATALOGO | Usar catálogo interno para precios, duración, stock y cuidados. | Catálogo/IA | V8 | Mantener activa |
| IA_PRECIO_SERVICIO_EXACTO | Responder precio base configurado. | Catálogo/IA | V8 | Mantener activa |
| IA_DURACION_SERVICIO_EXACTA | Responder duración configurada. | Catálogo/IA | V8 | Mantener activa |
| IA_STOCK_PRODUCTO_EXACTO | Responder stock y restricciones desde catálogo. | Catálogo/IA | V8 | Mantener activa |
| IA_CUIDADOS_POSTERIORES | Responder cuidados configurados. | Seguridad/IA | V8 | Mantener activa |
| IA_CONTRAINDICACIONES | Mostrar contraindicaciones y derivar si aplica. | Seguridad | V8 | Mantener activa |
| IA_DERIVACION_RIESGO | Derivar por embarazo, alergias, medicamentos, heridas, dolor, etc. | Seguridad | V8 | Mantener activa |
| AGENDA_NO_CONFIRMAR_SIN_BD | No confirmar agenda sin base. | Agenda | V8 | Mantener activa |
| RESERVA_DATOS_MINIMOS | Reserva requiere servicio, nombre, teléfono, fecha u horario. | Agenda | V7 | Reforzar con sucursal y hora |
| BLOQUEO_HORARIOS | No ofrecer horarios ocupados o fuera de atención. | Agenda | V7 | Mantener activa |
| DISPONIBILIDAD_PROFESIONAL | Confirmar solo con profesional activo/disponible. | Agenda | V7 | Mantener activa |
| CANCELACION | Cancelar solo tras identificar cita activa y confirmación explícita. | Agenda | V7 | Mantener activa |
| REPROGRAMACION | Reprogramar solo con cita activa y horario disponible. | Agenda | V7 | Mantener activa |
| PAGO_PARCIAL_TOTAL | Algunos servicios pueden exigir abono. | Pagos | V7 | Validar configuración por servicio |
| CONTROL_STOCK | No vender productos sin stock suficiente. | Inventario | V7 | Mantener activa |
| AUDITORIA_INTENCION | Registrar mensaje, intención, confianza y respuesta. | Auditoría IA | V8 | Mantener activa |
| AI_BOOKING_MISSING_SERVICE_RESPONSE | Plantilla cuando falta servicio. | Agenda/IA | V14/V21 | Mantener y ajustar |
| AI_BOOKING_MISSING_DATE_RESPONSE | Plantilla cuando falta fecha. | Agenda/IA | V14 | Mantener |
| AI_BOOKING_MISSING_TIME_RESPONSE | Plantilla cuando falta hora. | Agenda/IA | V14 | Mantener |
| AI_BOOKING_COMPLETE_RESPONSE | Plantilla cuando existen servicio, fecha y hora. | Agenda/IA | V14 | Ajustar para no confirmar sin sucursal y agenda |
| PROMPT_OPERATIVO_IA_NEGOCIO | Prompt operativo principal. | IA | Frontend, no DB | Insertar en base |
| PROMPT_ORQUESTADOR_AGENDA_WHATSAPP | Orquestación de agenda. | Agenda/IA | Inferido de código y docs | Insertar en base |

---

## D. Inventario de prompts

| Código | Nombre | Tipo | Módulo | Variables | Prioridad | Estado |
|---|---|---|---|---|---:|---|
| PROMPT_OPERATIVO_IA_NEGOCIO | Prompt operativo de IA del negocio | Sistema | IA negocio | nombre_negocio, canal, catálogo, sucursales, agenda, contexto | 1 | Activo sugerido |
| PROMPT_ORQUESTADOR_AGENDA_WHATSAPP | Prompt orquestador de agenda WhatsApp | Agente | Agenda | intención, servicio, sucursal, fecha, hora, datos_faltantes | 2 | Activo sugerido |
| PROMPT_EXTRACCION_ENTIDADES_AGENDA | Prompt extracción entidades agenda | Clasificación | Entidades IA | mensaje_cliente, catálogo, sucursales, fecha_actual | 3 | Activo sugerido |
| PROMPT_RESPUESTA_DATOS_FALTANTES_AGENDA | Prompt respuesta datos faltantes agenda | Respuesta | Agenda | servicio, sucursal, fecha, hora, dato_faltante | 4 | Activo sugerido |
| PROMPT_ENVIO_ENLACE_CONFIRMACION_RESERVA | Prompt envío enlace confirmación reserva | Respuesta | Confirmación | cliente, servicio, sucursal, fecha, hora, enlace, expiración | 5 | Activo sugerido |
| PROMPT_DERIVACION_HUMANA_SEGURIDAD | Prompt derivación humana por seguridad | Derivación | Seguridad | motivo, mensaje_cliente, confianza, intención | 6 | Activo sugerido |
| PROMPT_CATALOGO_COMERCIAL_WHATSAPP | Prompt catálogo comercial WhatsApp | Respuesta | Catálogo | mensaje, servicios, productos, promociones | 7 | Activo sugerido |
| PROMPT_REPROGRAMACION_RESERVA | Prompt reprogramación de reserva | Respuesta | Reprogramación | cliente, teléfono, reserva, nueva fecha/hora | 8 | Activo sugerido, implementación parcial |
| PROMPT_CANCELACION_RESERVA | Prompt cancelación de reserva | Respuesta | Cancelación | cliente, teléfono, reserva, motivo | 9 | Activo sugerido, implementación parcial |
| PROMPT_PAGO_SENAL_RESERVA | Prompt pago o señal de reserva | Respuesta | Pagos | servicio, monto, estado_pago, enlace_pago | 10 | Activo sugerido, implementación parcial |

---

## E. Prompts completos para insertar

El contenido completo está incluido en el archivo SQL adjunto `prompts_reglas_ia_negocio_v22.sql`. Este archivo inserta los prompts en `aesthetic_business_rule` con `rule_type = 'AI_PROMPT'` y agrega metadatos en `rule_payload`.

Prompts incluidos:

1. `PROMPT_OPERATIVO_IA_NEGOCIO`: prompt operativo principal para WhatsApp, sucursales, agenda, enlaces y derivación.
2. `PROMPT_ORQUESTADOR_AGENDA_WHATSAPP`: prompt del agente de agenda para evitar respuestas comerciales genéricas.
3. `PROMPT_EXTRACCION_ENTIDADES_AGENDA`: prompt de clasificación/extracción de entidades.
4. `PROMPT_RESPUESTA_DATOS_FALTANTES_AGENDA`: prompt para preguntar solo el dato faltante.
5. `PROMPT_ENVIO_ENLACE_CONFIRMACION_RESERVA`: prompt de enlace de confirmación.
6. `PROMPT_DERIVACION_HUMANA_SEGURIDAD`: prompt de derivación humana y seguridad.
7. `PROMPT_CATALOGO_COMERCIAL_WHATSAPP`: prompt de catálogo comercial.
8. `PROMPT_REPROGRAMACION_RESERVA`: prompt de reprogramación.
9. `PROMPT_CANCELACION_RESERVA`: prompt de cancelación.
10. `PROMPT_PAGO_SENAL_RESERVA`: prompt de pago/señal.

---

## F. Propuesta de estructura de base de datos

La aplicación actual ya tiene estructura usable:

Tabla actual: `aesthetic_business_rule`

Campos relevantes:

- `id`
- `business_id`
- `code`
- `name`
- `rule_type`
- `description`
- `priority`
- `active`
- `rule_payload`
- `created_at`
- `updated_at`

Recomendación inmediata: usar `aesthetic_business_rule` para no cambiar el backend.

Recomendación evolutiva: agregar `ai_prompt_template` si se necesita versionado especializado por prompt. El SQL propuesto crea esta tabla, pero aclara que la aplicación actual no la lee todavía.

---

## G. Sentencias SQL

Las sentencias se entregan en:

- `prompts_reglas_ia_negocio_v22.sql`

Incluye:

1. Creación opcional de `ai_prompt_template`.
2. Inserción de `AI_PROMPT` en `aesthetic_business_rule`.
3. Actualización de `AI_BOOKING_COMPLETE_RESPONSE`.
4. Actualización de `AI_BOOKING_MISSING_SERVICE_RESPONSE`.
5. Alias adicionales para preferencias horarias.

---

## H. Observaciones finales

Elementos no detectados como implementación completa:

1. Enlace público de reprogramación de extremo a extremo.
2. Enlace público de cancelación de extremo a extremo.
3. Enlace de pago o integración completa de pasarela de pago.
4. Envío efectivo de recordatorios por WhatsApp desde `booking_reminder`.
5. Persistencia backend del horario editado en pantalla IA del negocio.
6. Uso directo de `AI_PROMPT` por el motor determinístico de agentes; actualmente impacta principalmente como regla visible y contexto si se incorpora a snapshot.

Supuestos usados:

- El negocio demo usa `business_id = '11111111-1111-1111-1111-111111111111'` porque las migraciones lo usan como negocio semilla.
- El canal real del MVP es WhatsApp, no multicanal.
- Las reglas inferidas se basan en código, migraciones y documentación presentes en el ZIP.

Riesgos técnicos:

- La respuesta coherente depende de que el backend priorice el orquestador de agenda antes de respuestas comerciales.
- Si OpenAI devuelve intención distinta, `applySafetyAndOperationalGuards` corrige parcialmente, pero no crea reservas por sí solo.
- El simulador `/business-ai` no equivale a una conversación real con contexto completo si no hay conversación persistida.
- Insertar prompts en base de datos no reemplaza pruebas locales con Docker Compose, PostgreSQL y WhatsApp Web conectados.

Preguntas pendientes:

1. ¿La agenda debe permitir elegir profesional específico o solo asignar automáticamente el primero disponible?
2. ¿Cada sucursal puede tener servicios/precios distintos para clientes finales?
3. ¿El enlace de reprogramación y cancelación debe ser público como el de confirmación?
4. ¿El pago/señal se integrará con Webpay u otra pasarela?
5. ¿Los recordatorios deben enviarse automáticamente por WhatsApp o quedar como tareas administrativas?
