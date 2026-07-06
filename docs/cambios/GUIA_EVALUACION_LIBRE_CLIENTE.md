# Guia de evaluacion libre del MVP

Esta guia permite que un cliente o evaluador explore el MVP del asistente empresarial para WhatsApp sin depender de un guion rigido. El negocio demo es Centro Estetico Bella.

## Estado permitido

El MVP es apto para evaluacion libre con restricciones explicitas:

- Puede explorarse la aplicacion web, conversaciones, catalogo, agenda, administracion e IA del negocio.
- La IA opera en modo supervisado: sugiere respuestas y registra trazabilidad, pero la auto-respuesta queda desactivada por defecto.
- WhatsApp Web es experimental. Para produccion se recomienda WhatsApp Cloud API.
- La agenda no confirma disponibilidad automaticamente; debe validar disponibilidad real antes de comprometer un horario.

## Levantar el entorno local

```powershell
docker compose -f docker-compose.local.yml up -d --build
```

Servicios esperados:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080/api/v1/health
- WhatsApp Web visual: http://localhost:6080/vnc.html?autoconnect=true&resize=scale
- Adaptador WhatsApp Web: http://localhost:3001
- PostgreSQL: localhost:5432

Credenciales demo:

- Usuario: `admin@demo.cl`
- Contrasena: `Cambiar123!`

Duracion de sesion demo local: 8 horas (`APP_ACCESS_TOKEN_EXPIRES_IN_SECONDS=28800`).

## Verificar salud

```powershell
docker compose -f docker-compose.local.yml ps
Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -UseBasicParsing
Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing
```

Todos los contenedores deben quedar en estado `healthy` o `Up`.

## Ver registros

```powershell
docker compose -f docker-compose.local.yml logs -f backend-java
docker compose -f docker-compose.local.yml logs -f frontend-react
docker compose -f docker-compose.local.yml logs -f whatsapp-web-service
```

## Que puede probar libremente

- Inicio de sesion.
- Dashboard.
- Conversaciones y detalle de conversacion.
- Envio manual de mensajes.
- Catalogo de servicios y productos.
- Agenda.
- IA del Negocio: configuracion, base de conocimiento, respuestas sugeridas y auditoria.
- Administracion: empresa, usuarios, seguridad y WhatsApp Web.
- Configuracion de canal.

## Mensajes sugeridos para probar IA

Estos mensajes son ejemplos, no un guion obligatorio:

1. `Hola`
2. `Como estas`
3. `Que tipo de depilacion tienen`
4. `Cuanto cuesta depilacion bozo`
5. `Quiero agendar depilacion bozo manana a las 14 horas`
6. `Quiero agendar depilacion bozo`
7. `manana`
8. `a las 14 horas`
9. `Quiero hablar con una persona`
10. `Estoy molesta, nadie responde`
11. `docker compose up --build`
12. `Tienen disponibilidad manana a las 14 para depilacion bozo?`

## Resultado esperado de IA

- Saludo simple: responde con una ayuda abierta sobre servicios, precios o agenda.
- Consulta de depilacion: lista opciones del catalogo, sin mezclar con limpieza facial.
- Precio: responde precio solo si el servicio existe en catalogo.
- Agenda completa: responde que tiene servicio, fecha y hora, pero que debe validar disponibilidad real antes de confirmar.
- Agenda incompleta: pide solo el dato faltante principal.
- Humano o enojo: deriva a una persona del equipo.
- Mensaje tecnico: no activa venta ni agenda.
- Mensaje propio, duplicado o sin texto util: no debe generar respuesta comercial.

## Funcionalidades listas

- Autenticacion demo.
- Navegacion privada principal.
- Dashboard.
- Conversaciones.
- Catalogo.
- Agenda.
- Pedidos.
- Reglas.
- IA del Negocio en modo supervisado.
- Administracion de usuarios y seguridad.
- Auditoria y logs basicos.
- Docker local con backend, frontend, PostgreSQL y WhatsApp Web.

## Funcionalidades experimentales

- WhatsApp Web con `whatsapp-web.js`.
- Conexion QR y navegador visual noVNC.
- Orquestacion multiagente con auto-respuesta desactivada.
- Respuestas automaticas si se activan manualmente por variable de entorno.

## Funcionalidades pendientes para produccion

- Canal productivo con WhatsApp Cloud API.
- Monitoreo y alertas.
- Gestion segura de secretos.
- Backups y restauracion formal.
- Hardening de seguridad.
- CI/CD obligatorio.
- Pruebas end-to-end contra flujos reales de WhatsApp.
- Validacion real de disponibilidad de agenda antes de confirmar horas.

## Como reportar errores

Al reportar un error incluir:

- Pantalla o URL.
- Usuario usado.
- Mensaje enviado.
- Resultado esperado.
- Resultado obtenido.
- Hora aproximada.
- Captura si aplica.
- Logs relevantes con:

```powershell
docker compose -f docker-compose.local.yml logs --tail=200 backend-java whatsapp-web-service frontend-react
```

## Limitaciones honestas

- Este ambiente no es produccion.
- WhatsApp Web puede requerir reconexion manual y QR.
- La IA no debe confirmar disponibilidad sin validar agenda real.
- La auto-respuesta no esta activa por defecto.
- Los datos son demo y pueden reiniciarse segun el manejo de volumen local.

## Reiniciar servicios sin borrar datos

```powershell
docker compose -f docker-compose.local.yml restart
```

## Apagar sin borrar volumenes

```powershell
docker compose -f docker-compose.local.yml down
```

No usar `down -v` salvo que se quiera borrar datos locales.
