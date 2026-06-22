# Informe tecnico V23.4.9 - CTA local para enlace WhatsApp

## Contexto

La version V23.4.8 logro que el envio real desde una vista previa de reserva cree la reserva temporal, genere el enlace de confirmacion y envie el mensaje final con formato destacado.

Durante la prueba visual se observo que el enlace con `localhost` llega como texto poco destacado. El usuario solicito mantener `localhost` porque esta haciendo pruebas locales.

## Restriccion tecnica

WhatsApp decide como renderiza las URLs. Con `localhost`, WhatsApp puede no mostrar una previsualizacion enriquecida o puede tratar la URL de forma menos destacada que un dominio publico `https`.

No es posible forzar desde texto plano:

- color del enlace;
- boton real;
- preview enriquecida;
- estilo HTML.

## Solucion aplicada

Se mantiene la URL local y se refuerza la instruccion al cliente:

```text
👉 *Toca o copia este enlace para confirmar tu reserva:*

http://localhost:5173/reservas/confirmar/...

_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._
```

Este formato reduce la ambiguedad durante pruebas locales porque indica explicitamente que el usuario debe tocar o copiar la URL.

## Archivo modificado

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/WhatsAppMessageFormatter.java`

## Metodos afectados

- `temporaryBookingCreated(...)`
- `confirmationLinkResent(...)`

## Antes

```text
👉 *Confirma tu reserva aquí:*
http://localhost:5173/reservas/confirmar/...
```

## Despues

```text
👉 *Toca o copia este enlace para confirmar tu reserva:*

http://localhost:5173/reservas/confirmar/...

_Si WhatsApp no lo abre al tocarlo, copia el enlace y pégalo en el navegador._
```

## Pruebas agregadas

- `test_ia_negocio_conversacional_v23_4_9.ps1`

## Comando de logs sugerido

```powershell
docker compose -f docker-compose.local.yml logs --tail=3000 backend-java | Select-String -Pattern "TEMPORARY_BOOKING_CREATED","CONFIRMATION_LINK_CREATED","WHATSAPP_MESSAGE_FORMATTED","Toca o copia","localhost","WHATSAPP_RESPONSE_SEND_RESULT","FLOW_ERROR"
```

## Riesgos

- En telefonos reales, `localhost` apunta al dispositivo del cliente, no al servidor del desarrollador.
- La URL puede no abrir desde un telefono externo salvo que la prueba se haga en el mismo equipo/navegador o con redireccion/tunel local.
- WhatsApp puede no renderizar `localhost` como enlace destacado aunque la URL este bien formada.

## Validacion realizada

Se valido parcialmente `WhatsAppMessageFormatter.java` con `javac`.

## Validacion pendiente

Compilar y ejecutar con Docker Compose en el ambiente local del usuario.
