# Correccion de confirmacion publica con error 403

## Problema observado

Al confirmar una reserva desde el enlace publico de WhatsApp, el navegador recibia `403 Forbidden` en:

```text
/api/v1/public/booking-confirmations/{token}/confirm
```

La pagina publica cargaba los datos de la reserva, pero la accion de confirmacion quedaba bloqueada y mostraba un error generico.

## Cambios aplicados

1. Se agrego una cadena de seguridad dedicada y prioritaria para:

```text
/api/v1/public/booking-confirmations/**
```

Esta cadena permite explicitamente `GET`, `POST` y `OPTIONS` sin autenticacion.

2. Se reforzo `JwtAuthenticationFilter` para omitir rutas publicas validando:

- `servletPath`
- `requestURI`
- `requestURI` sin `contextPath`

Esto evita que diferencias de proxy, Nginx o tunel publico hagan que el filtro JWT procese indebidamente una ruta publica.

3. Se agrego un fallback publico por `GET` para confirmar:

```text
GET /api/v1/public/booking-confirmations/{token}/confirm
```

La operacion de confirmacion es idempotente: si la reserva ya esta confirmada, devuelve el estado confirmado sin duplicar datos. Este fallback ayuda en entornos de prueba, tuneles o navegadores donde el `POST` publico pueda quedar bloqueado.

4. El frontend intenta primero con `POST`. Si recibe `403` o `405`, reintenta una vez con `GET` publico sin cache.

## Resultado esperado

El boton `Confirmar reserva` debe cambiar la reserva desde `PENDIENTE_CONFIRMACION` a `CONFIRMED` sin requerir sesion de administrador ni token JWT.
