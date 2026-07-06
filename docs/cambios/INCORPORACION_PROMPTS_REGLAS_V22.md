# Incorporacion V22 - prompts y reglas de IA del negocio

Este paquete incorpora dentro del proyecto la migracion:

`backend-java/src/main/resources/db/migration/V22__ai_operational_prompts_and_rules.sql`

La migracion persiste prompts de IA como reglas de negocio con `rule_type = 'AI_PROMPT'` en la tabla `aesthetic_business_rule`.

## Contenido incorporado

- Prompt operativo principal del asistente de negocio por WhatsApp.
- Prompt de orquestador de agenda.
- Prompt de extraccion de entidades de agenda.
- Prompt de respuesta para datos faltantes.
- Prompt de envio de enlace de confirmacion.
- Prompt de derivacion humana y seguridad.
- Prompt de catalogo comercial.
- Prompt de reprogramacion.
- Prompt de cancelacion.
- Prompt de pago o senal.
- Alias adicionales para preferencias horarias.
- Ajustes a reglas existentes de respuesta de agenda.

## Validacion esperada

Despues de levantar la aplicacion, en la pantalla:

`Reglas -> Tipo de regla -> Prompt de IA`

deben aparecer reglas activas con codigos como:

- PROMPT_OPERATIVO_IA_NEGOCIO
- PROMPT_ORQUESTADOR_AGENDA_WHATSAPP
- PROMPT_EXTRACCION_ENTIDADES_AGENDA
- PROMPT_RESPUESTA_DATOS_FALTANTES_AGENDA
- PROMPT_ENVIO_ENLACE_CONFIRMACION_RESERVA

## Ejecucion local sugerida

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.local.yml logs -f
```

## Consulta de verificacion

```sql
select code, name, rule_type, priority, active
from aesthetic_business_rule
where business_id = '11111111-1111-1111-1111-111111111111'
  and rule_type = 'AI_PROMPT'
order by priority, code;
```

Nota: este paquete fue actualizado estaticamente. La compilacion y la ejecucion real deben validarse en el entorno local del proyecto.
