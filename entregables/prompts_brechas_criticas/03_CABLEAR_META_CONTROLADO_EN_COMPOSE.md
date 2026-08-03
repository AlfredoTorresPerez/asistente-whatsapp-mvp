# Prompt 03 — Hacer reproducible el arranque Meta controlado con Docker Compose

Actúa como ingeniero DevOps y backend. Corrige el cableado incompleto entre `.env.local`, Docker Compose, los perfiles Spring y los scripts oficiales, de forma que el modo Meta controlado pueda arrancar de manera reproducible y el modo seguro permanezca como predeterminado.

## Contexto verificado

- `application-local-meta-controlled.yml` exige acknowledgement y una lista permitida.
- `docker-compose.local.yml` no pasa actualmente esas dos variables al contenedor backend.
- `--env-file` sirve para interpolación de Compose; no inyecta automáticamente todas las variables al contenedor.
- `local-start.ps1` valida sintaxis Compose, pero `local-verify.ps1` no comprueba la modalidad efectiva ni sus controles.
- El nombre `-Profile` en los scripts se usa para perfiles de servicios Compose, no para la modalidad segura/Meta, lo que puede inducir errores operativos.

## Objetivo

Ofrecer comandos explícitos, reproducibles y fail-closed para iniciar `safe` o `meta-controlled`, verificando la configuración efectiva sin mostrar secretos.

## Trabajo requerido

1. Inspecciona `docker-compose.local.yml`, `.env.local.template`, scripts `local-setup/start/verify/stop` en PowerShell y Bash, perfiles Spring y documentación.
2. Inyecta explícitamente al backend todas las propiedades requeridas por Meta controlado, incluyendo acknowledgement y lista permitida, sin imprimir sus valores.
3. Introduce una selección clara de modalidad, por ejemplo `-Mode safe|meta-controlled` y su equivalente Bash. No mezcles esta selección con los perfiles opcionales de servicios como observabilidad o túnel.
4. Mantén `safe` como modalidad predeterminada. Meta controlado debe requerir una acción explícita en cada arranque o una configuración local inequívoca validada.
5. Ordena el preflight correctamente:
   - validar herramientas y archivos;
   - restaurar secretos desde el almacén aprobado;
   - validar presencia sin mostrar valores;
   - generar/validar configuración Compose efectiva con salida silenciosa;
   - iniciar únicamente si la política pasa.
6. Haz fallar el arranque Meta cuando falte firma obligatoria, acknowledgement, allowlist, credenciales o proveedor correcto. No continúes con advertencias.
7. Amplía `local-verify` para comprobar, de manera sanitizada:
   - perfiles Spring activos;
   - proveedor efectivo;
   - firma requerida;
   - tamaño de allowlist mayor que cero, nunca sus entradas;
   - acknowledgement verdadero;
   - estado de auto-reply y safe mode;
   - ausencia de salidas externas no autorizadas en modo seguro.
8. Restringe los puertos locales a loopback cuando no necesiten exposición LAN. El túnel público debe seguir siendo opcional y explícito.
9. No reinicies ni recrees `public-tunnel` durante la implementación sin avisar y recibir autorización, porque su URL puede cambiar.
10. Actualiza `README-LOCAL.md`, quickstart y plantillas con ejemplos sin números ni secretos reales.

## Pruebas obligatorias

- Matriz `docker compose config --quiet` para modo seguro y Meta controlado, con y sin perfiles opcionales.
- Casos negativos por cada variable obligatoria ausente.
- Prueba de que `--env-file` más el mapeo de `environment` llega realmente al contexto Spring.
- Prueba del script en modo `WhatIf` o equivalente que no inicia contenedores ni imprime secretos.
- Prueba del verificador sobre un contenedor seguro y, si es posible sin contactar Meta, sobre una configuración Meta controlada en dry validation.
- Sintaxis PowerShell y Bash.

## Criterios de aceptación

- Un usuario puede elegir explícitamente `safe` o `meta-controlled` con comandos documentados.
- El backend recibe acknowledgement y allowlist mediante Compose.
- El modo Meta incompleto falla antes de iniciar.
- `local-verify` detecta una deriva hacia el perfil legado o firma deshabilitada.
- Los servicios locales no quedan expuestos a la red salvo los estrictamente necesarios.
- Ninguna prueba envía mensajes ni cambia el túnel.

## Entrega esperada

Implementa el cableado, muestra la matriz de pruebas sanitizada y documenta los comandos finales de arranque y verificación.
