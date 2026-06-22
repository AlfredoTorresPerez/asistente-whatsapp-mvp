# Incorporación V23 - Agentes especializados IA negocio

Esta versión conserva todo lo incluido en V22 y agrega una capa determinística para completar la operación del asistente de negocio por WhatsApp de Centro Estético Bella.

## Cobertura agregada

- Orquestador principal con prioridad de intención.
- Clasificador de intención para reenvío de enlace, enlace expirado, reprogramación, cancelación, derivación humana, caso sensible, ubicación y pago/señal.
- Extractor de entidades reforzado para sucursales explícitas y fechas como "sábado en la mañana".
- Respuestas determinísticas para agentes especializados.
- Migración `V23__complete_specialized_ai_agents_for_business.sql` con alias, reglas y prompts.
- Script de test funcional V23 con validación de intención, entidades, texto esperado y texto prohibido.

## Limitación

La versión fue modificada y revisada estáticamente. La compilación y ejecución real deben validarse en el entorno local con Docker Compose.
