# CAMBIOS V23.4.6 — Formato WhatsApp para reservas destacadas

## Objetivo

Mejorar exclusivamente el formato visual de los mensajes enviados por WhatsApp para reservas temporales, vista previa, enlaces, cancelación, reprogramación, no disponibilidad y casos sensibles.

## Problema detectado

El mensaje con enlace de confirmación llegaba demasiado plano, en una estructura poco destacada:

```text
Perfecto ✅ Dejé una reserva temporal para: Servicio: ... Sucursal: ... Confirma tu reserva aquí: ...
```

Aunque funcionaba, el cliente podía no identificar rápidamente que la reserva era temporal, que debía confirmar y que el enlace vencía.

## Cambios realizados

- Se agregó `WhatsAppMessageFormatter` como clase centralizada de formato.
- Se actualizó el mensaje de reserva temporal con encabezado, negritas, bloques y enlace en línea separada.
- Se actualizó el mensaje de vista previa `dryRun` para dejar claro que no crea reserva ni enlace real.
- Se actualizó no disponibilidad con encabezado y alternativas numeradas, sin duplicados y máximo 3 opciones.
- Se actualizaron mensajes de datos faltantes: servicio, sucursal, fecha y hora.
- Se actualizaron mensajes de cancelación, reprogramación, enlace vencido y caso sensible.
- Se mantuvieron los logs `AI_TRACE` y se agregaron logs `WHATSAPP_MESSAGE_FORMATTED`.
- Se ajustó el mensaje usado por `BookingConfirmationService` cuando el enlace se envía por WhatsApp.

## Formato principal nuevo

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

## Validación pendiente

En este entorno no se pudo compilar con Maven ni levantar Docker Compose porque no están disponibles localmente. La compilación final debe ejecutarse en el ambiente local del proyecto.

