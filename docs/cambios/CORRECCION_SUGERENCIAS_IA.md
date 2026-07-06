# Corrección de sugerencias IA en conversaciones

## Problema corregido

La opción de sugerencia IA del panel de conversaciones seguía usando reglas antiguas en `ConversationService.previewAiReply`.

Eso provocaba respuestas como:

- confundir `depilación facial` con `limpieza facial`
- confirmar disponibilidad sin validar agenda
- mencionar horarios fijos inventados
- repetir respuestas genéricas ante consultas de agenda

## Cambio aplicado

Ahora `previewAiReply` usa una lógica segura:

1. Primero revisa catálogo vigente para preguntas de precio.
2. Si detecta depilación facial ambigua, muestra opciones reales del catálogo.
3. Si el cliente pregunta por reservas o agenda, no confirma nada y pide fecha/mes a revisar.
4. Si no aplica catálogo, usa el `AgentCoordinatorService.preview`, sin escribir logs ni disparar respuestas automáticas.
5. Se eliminaron respuestas hardcodeadas que inventaban disponibilidad.

## Ejemplo esperado

Cliente:

```text
Hola dame el precio de la depilacion facial
```

Respuesta sugerida esperada:

```text
Hola Contacto, para depilación facial tengo estas opciones en catálogo: Depilacion bozo $8.990; Depilacion rostro $18.990; Depilacion laser rostro $29.990. ¿Cuál modalidad quieres revisar?
```

Cliente:

```text
puedes revisar si tengo agendado algo para estos dias
```

Respuesta sugerida esperada:

```text
Hola alfredo, puedo ayudarte a revisar tus reservas, pero debo validarlo en agenda. ¿Qué fecha o mes quieres revisar?
```

Cliente:

```text
mañana a las 11 horas
```

Respuesta sugerida esperada:

```text
Perfecto alfredo. Para revisar disponibilidad en ese horario, ¿qué servicio quieres agendar?
```

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/conversations/application/ConversationService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentCoordinatorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/IntentDetectorService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/EntityExtractionService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/SalesAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/domain/AgentIntent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/AgentRegistry.java`

## Cómo probar

Reconstruye el backend:

```powershell
docker compose --env-file .env.local.example -f docker-compose.local.yml up --build
```

Luego abre:

```text
http://localhost:5173
```

Prueba la conversación desde el panel o con el script:

```powershell
.\scripts\probar-multiagente-local.ps1
```
