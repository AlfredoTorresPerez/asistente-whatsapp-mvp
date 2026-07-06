# Cambios v23.4.13 - Zona horaria, opciones de agenda y expiración 12 horas

## Objetivo

Corregir diferencias observadas en pruebas locales con WhatsApp Web, VNC y agenda:

1. Diferencia horaria entre Windows, VNC, backend y contenedores.
2. Respuesta `1`, `2` o `3` interpretada como hora `01:00`, `02:00` o `03:00` en vez de opción de agenda.
3. Extracción incorrecta de hora cuando el cliente escribe fecha con día del mes, por ejemplo `viernes 12 de junio 2026 a las 13:00`.
4. Expiración de enlaces de confirmación configurada a 12 horas.

## Cambios implementados

### 1. Zona horaria Chile

Se agregó configuración horaria en `docker-compose.local.yml`:

- `TZ=America/Santiago`
- `JAVA_TOOL_OPTIONS=-Duser.timezone=America/Santiago`
- `SPRING_JACKSON_TIME_ZONE=America/Santiago`
- `APP_TIME_ZONE=America/Santiago`
- `PGTZ=America/Santiago` para PostgreSQL.

También se agregó `tzdata` en los contenedores de WhatsApp Web y frontend.

### 2. Selección de opciones 1/2/3

Se agregó resolución contextual en `AgentCoordinatorService`.

Ahora, si la última respuesta de IA contiene opciones como:

```text
1. viernes a las 16:00
2. viernes a las 16:15
3. viernes a las 16:30
```

y el cliente responde:

```text
1
```

el sistema lo interpreta como:

```text
viernes a las 16:00
```

No como `01:00`.

### 3. Extracción segura de hora

Se corrigió `EntityExtractionService` para priorizar horas explícitas:

- `a las 13:00`
- `15:00`
- `a las 16 horas`
- `horario 10:30`

Ya no debe tomar el número de una fecha, por ejemplo `viernes 12 de junio`, como hora `12:00`.

### 4. Expiración 12 horas

La expiración por defecto del enlace de confirmación quedó en:

```text
APP_BOOKING_CONFIRMATION_EXPIRATION_MINUTES=720
```

El mensaje de WhatsApp ahora muestra `12 horas` cuando el valor es `720`.

## Comandos recomendados tras actualizar

```powershell
docker compose -f docker-compose.local.yml up -d --build --force-recreate
```

Validar variables:

```powershell
docker compose -f docker-compose.local.yml exec backend-java printenv APP_BOOKING_CONFIRMATION_EXPIRATION_MINUTES
docker compose -f docker-compose.local.yml exec backend-java printenv TZ
docker compose -f docker-compose.local.yml exec whatsapp-web-service date
```

## Prueba recomendada

Enviar:

```text
Hola, quiero una limpieza facial para el viernes 12 de junio 2026 a las 13:00 en Providencia. Mi nombre es Alfredo.
```

Validar en logs:

```text
hora=13:00
```

Si responde con alternativas, elegir solo:

```text
1
```

Debe resolver la opción 1 usando el contexto anterior.
