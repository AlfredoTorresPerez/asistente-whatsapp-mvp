# Correccion V23.1 - Compilacion backend Java

## Problema corregido

Durante `docker compose -f docker-compose.local.yml up -d --build`, Maven reporto errores de compilacion por literales de cadena sin cerrar en:

- `backend-java/src/main/java/com/asistentewhatsapp/aesthetic/application/AestheticCenterService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/SupportAgent.java`

## Causa

Las respuestas de ubicacion contenian saltos de linea reales dentro de literales Java:

```java
return "La sucursal " + location.name() + " esta ubicada en:
" + address;
```

Eso rompe la compilacion Java.

## Correccion aplicada

Se reemplazo el salto de linea literal por secuencia escapada `\n`:

```java
return "La sucursal " + location.name() + " esta ubicada en:\n" + address;
```

## Validacion

No se afirma compilacion completa en este entorno. La validacion real debe ejecutarse localmente con:

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.local.yml logs -f
```
