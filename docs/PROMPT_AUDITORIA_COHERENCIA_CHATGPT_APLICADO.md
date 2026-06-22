# Prompt de auditoria de coherencia aplicado

Se aplicaron reglas de coherencia conversacional al orquestador multiagente del asistente WhatsApp para Centro Estetico Bella.

## Objetivo aplicado

- Mantener contexto entre turnos de agenda.
- Evitar preguntas repetidas cuando el cliente ya entrego servicio, fecha u hora.
- No tratar mensajes tecnicos como solicitudes comerciales.
- No interpretar saludos sociales como nombre del cliente.
- No confirmar disponibilidad sin validacion previa de agenda.
- Mantener respuestas breves y accionables para WhatsApp.

## Cambios tecnicos

1. Se agrego la intencion `TECHNICAL_MESSAGE` para mensajes como `docker compose up --build`, `npm`, `pnpm`, `mvn`, `git`, `sql`, `localhost`, entre otros.
2. `SupportAgent` responde los mensajes tecnicos con una salida segura orientada al centro estetico, sin activar venta ni agenda.
3. `ReceptionAgent` reconoce saludos sociales como `Como estas` y responde sin pedir nombre nuevamente.
4. `AgentCoordinatorService` guarda datos de contexto conversacional:
   - `ultimo_mensaje_cliente`
   - `ultima_respuesta_ia`
   - `ultimo_dato_solicitado`
   - `timestamp_ultimo_turno`
5. `BookingAgent` resume servicio, fecha y hora antes de pasar a validacion de disponibilidad.
6. Se agregaron pruebas unitarias en `AiAgentCoherenceTest`.

## Casos cubiertos

- Cliente entrega servicio, fecha y hora en mensajes separados.
- Cliente responde solo el servicio luego de iniciar agenda.
- Cliente escribe un comando tecnico.
- Cliente escribe saludo social.
- Asistente no debe preguntar otra vez el servicio cuando ya existe en contexto.

## Reglas operativas incorporadas

- Si la conversacion esta en agenda, se mantiene agenda hasta cerrar, validar o cancelar.
- Si existe servicio, no se vuelve a preguntar servicio.
- Si existe fecha, no se vuelve a preguntar fecha.
- Si existe hora, no se vuelve a preguntar hora.
- Si existen servicio, fecha y hora, se resume y se solicita validacion de disponibilidad.
- Los mensajes tecnicos quedan fuera del flujo comercial.
