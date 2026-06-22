# Correccion de coherencia conversacional multiagente

## Problema observado

Durante una conversacion de prueba, el asistente respondia de forma inconsistente cuando el cliente entregaba la informacion por partes. Ejemplo:

1. El cliente consulta tipos de depilacion.
2. Luego solicita agendar para manana.
3. Luego entrega el servicio especifico: depilacion bozo.
4. Luego repite fecha y hora.

El sistema podia volver a preguntar el servicio aunque ya habia sido informado.

## Causa tecnica

La orquestacion clasificaba mensajes como `depilacion bozo` como consulta comercial aislada, aunque el contexto anterior estaba en flujo de agenda esperando el servicio. Esto hacia que el contexto pasara de agenda a ventas y la siguiente respuesta perdiera coherencia.

Tambien se detecto que mensajes sociales como `como estas` podian interpretarse como nombre, porque la extraccion de nombre aceptaba frases cortas sin validar intencion conversacional.

## Cambios realizados

1. `AgentCoordinatorService`
   - Ahora conserva el flujo de agenda cuando el contexto previo estaba esperando servicio, fecha u hora.
   - Si el cliente responde solo con el servicio, fecha u hora, se mantiene el agente de agenda.
   - Si habia un flujo de agenda abierto, un saludo no reinicia el flujo.

2. `AgentRegistry`
   - La intencion mixta comercial mas agenda ahora se enruta al agente de agenda, no al agente de ventas.

3. `BookingAgent`
   - Responde usando los datos ya capturados: servicio, fecha y hora.
   - Si el servicio es demasiado generico, pide modalidad especifica.
   - Si ya tiene servicio, fecha y hora, deja de repetir la pregunta por servicio.

4. `EntityExtractionService`
   - Evita interpretar `como estas`, `que tal`, correos y frases operativas como nombre.
   - Extrae correo cuando viene en el mensaje.

5. `IntentDetectorService`
   - Reconoce saludos sociales como saludo, no como dato de cliente.

6. `ConversationService`
   - En sugerencias de respuesta manual, da prioridad al agente de agenda cuando hay un flujo de agenda activo.
   - Mantiene el catalogo como primera opcion para consultas puramente comerciales.

## Resultado esperado

Para una conversacion como:

- Cliente: Quiero agendar para manana.
- Asistente: Perfecto. Para revisar disponibilidad necesito el servicio especifico.
- Cliente: Depilacion bozo.
- Asistente: Perfecto, reviso depilacion bozo. Que horario te acomoda?
- Cliente: Manana a las 14 horas.
- Asistente: Perfecto. Tengo estos datos: depilacion bozo, manana a las 14 horas. Debo validar disponibilidad real en agenda antes de confirmar. Quieres que lo deje como solicitud de reserva?

## Nota operacional

Si aparecen dos respuestas para un mismo mensaje, revisar si esta activo el envio automatico y al mismo tiempo se esta usando el boton de responder con IA desde el panel. Para pruebas controladas se recomienda usar solo uno de los dos modos.
