# Informe técnico V23.4.6 — Formato destacado para WhatsApp

## Problema visual detectado

El enlace de confirmación de reserva llegaba en un mensaje funcional pero visualmente plano. El cliente podía no distinguir con claridad:

- que la reserva era temporal;
- que debía confirmar desde el enlace;
- que el enlace vencía;
- que el cupo podía liberarse.

## Limitación técnica de WhatsApp

WhatsApp no permite controlar desde el backend el color de la burbuja, aplicar HTML ni definir estilos visuales avanzados en mensajes normales. Por eso la solución usa recursos compatibles con WhatsApp:

- saltos de línea;
- emojis moderados;
- negritas con `*texto*`;
- encabezados claros;
- enlace en línea independiente;
- bloques de información separados.

## Solución aplicada

Se creó `WhatsAppMessageFormatter`, una clase centralizada para generar mensajes consistentes. Esto evita duplicar textos y facilita mantener el tono visual en todos los flujos.

### Métodos incorporados

- `temporaryBookingCreated(...)`
- `bookingPreview(...)`
- `bookingConfirmed(...)`
- `confirmationLinkResent(...)`
- `confirmationLinkExpired()`
- `noAvailability(...)`
- `askService()`
- `askLocation()`
- `askDate()`
- `askTime()`
- `cancellationRequest()`
- `rescheduleRequest()`
- `sensitiveCase()`

## Ejemplo antes

```text
Perfecto ✅ Dejé una reserva temporal para:
Servicio: Limpieza facial profunda
Sucursal: Providencia
Fecha: mañana
Hora: 10:00
Confirma tu reserva aquí:
http://localhost:5173/reservas/confirmar/...
El enlace vence en 30 minutos...
```

## Ejemplo después

```text
✅ *Reserva temporal creada*

Hola, dejé una *reserva temporal* para ti:

*Servicio:* Limpieza facial profunda
*Sucursal:* Providencia
*Fecha:* mañana
*Hora:* 10:00

👉 *Confirma tu reserva aquí:*
http://localhost:5173/reservas/confirmar/...

⏳ *Importante:* este enlace vence en *30 minutos*.
Si no confirmas a tiempo, el cupo puede liberarse.
```

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/WhatsAppMessageFormatter.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/HumanHandoffAgent.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/bookings/application/BookingConfirmationService.java`
- `test_ia_negocio_conversacional_v23_4_6.ps1`

## Pruebas agregadas

Se agregó `test_ia_negocio_conversacional_v23_4_6.ps1` para validar:

- reserva temporal con enlace destacado;
- vista previa `dryRun` sin enlace real;
- falta de servicio;
- falta de sucursal;
- no disponibilidad o alternativa funcional;
- reenvío de enlace;
- caso sensible.

## Cómo ejecutar

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
```

Luego:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_6.ps1
```

Para probar preview + envío real:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_6.ps1 -ConversationId "ID_CONVERSACION" -TestConversationRealSend
```

## Cómo revisar logs

```powershell
docker compose -f docker-compose.local.yml logs -f backend-java
```

Filtrado recomendado:

```powershell
docker compose -f docker-compose.local.yml logs --tail=2000 backend-java | Select-String -Pattern "WHATSAPP_MESSAGE_FORMATTED","TEMPORARY_BOOKING_CREATED","CONFIRMATION_LINK_CREATED","AI_REAL_SEND_RESULT"
```

## Riesgos

- WhatsApp puede renderizar negritas según cliente/dispositivo.
- Los emojis pueden verse distinto según sistema operativo.
- Si algún canal no interpreta saltos de línea, el mensaje podría verse menos separado, pero seguirá siendo texto válido.

## Limitaciones

- No se puede cambiar el color de burbuja desde backend.
- No se puede usar HTML en mensajes normales de WhatsApp.
- No se implementaron botones interactivos porque el canal actual usa WhatsApp Web experimental.

## Validación pendiente

No se pudo ejecutar Maven ni Docker Compose en este entorno. Se verificó de forma parcial que la clase `WhatsAppMessageFormatter` compila con `javac`, pero la validación completa debe hacerse localmente con Docker Compose.

