# Correccion de trazabilidad, sanitizacion y URL publica

## Problema detectado

En la ejecucion local el proyecto compilaba correctamente, pero se observaron dos problemas operativos:

1. El script de enlace publico podia tomar `https://api.trycloudflare.com` como si fuera la URL publica del frontend.
2. La trazabilidad del backend podia registrar contenido excesivo o sensible, especialmente `qrCode` en base64, numeros telefonicos y firmas `sha256`.

## Cambios aplicados

### Backend Java

- Se amplio `TraceSanitizer` para ocultar:
  - `qrCode` y `qr_code`.
  - `phoneNumber` y `phone_number`.
  - firmas `sha256=...`.
  - `signature`, `x-signature` y `x_signature`.
  - imagenes `data:image/...;base64,...`.
  - telefonos de Chile con prefijo `56`.
- Se redujo el largo por defecto de payload de trazabilidad a 600 caracteres.
- Se ajusto `application.yml` para usar `APP_METHOD_TRACING_MAX_PAYLOAD_LENGTH` con valor por defecto 600.

### Scripts de tunel publico

- Se corrigio `scripts/start_mvp_public_link.ps1`.
- Se corrigio `scripts/start_mvp_public_link.sh`.
- Ahora ambos scripts ignoran `https://api.trycloudflare.com` y esperan una URL real del tipo:

```text
https://nombre-generado.trycloudflare.com
```

### WhatsApp web service

- Se agrego sanitizacion basica en logs del adaptador Node.js para no imprimir identificadores completos de WhatsApp, telefonos, IDs y campos sensibles en mensajes informativos.

## Validaciones realizadas

- Se valido sintaxis del servicio Node.js con `node --check`.
- Se valido la expresion de extraccion de URL publica para evitar seleccionar `api.trycloudflare.com`.
- Se valido que las expresiones de sanitizacion Java redacten `qrCode`, `phoneNumber` y `sha256`.
- Se valido integridad del ZIP generado.

## Nota operativa

Los mensajes de Cloudflare Tunnel sobre `context deadline exceeded`, `Application error 0x0` o reintentos QUIC pueden ocurrir en tuneles rapidos sin cuenta. En el log analizado, el tunel finalmente llego a `Registered tunnel connection`, por lo que el problema principal era que el script estaba leyendo una URL intermedia incorrecta antes de que apareciera la URL publica real.
