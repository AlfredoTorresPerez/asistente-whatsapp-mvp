# Informe tecnico V23.4.2 - Correccion de sucursal efectiva

## Problema detectado

En la pantalla de detalle de conversacion, algunos contactos aparecen como `Sin sucursal`. Cuando el cliente escribe una reserva completa indicando la sucursal en el texto, por ejemplo `en Providencia`, el sistema no siempre usa esa sucursal para la validacion de agenda.

El resultado observado fue una respuesta incorrecta o un error tecnico relacionado con servicio no disponible en la sucursal seleccionada.

## Causa raiz

La causa principal estaba en la resolucion incompleta de sucursal efectiva. El flujo de IA podia depender de la sucursal asociada a la conversacion/contacto y no priorizar de forma estricta la sucursal explicita del mensaje.

Ademas, la busqueda de sucursal podia confundir `Providencia` con la sede principal porque la sede principal tambien tenia comuna `Providencia`. Se cambio la resolucion a un sistema de puntajes que prioriza nombre/codigo/alias de sede por sobre comuna o ciudad.

## Jerarquia de resolucion implementada

1. `MESSAGE_TEXT`: sucursal escrita en el mensaje del cliente.
2. `MESSAGE_TEXT`: entidad de sucursal ya extraida por IA.
3. `CONVERSATION_SELECTED_LOCATION`: sucursal seleccionada en la conversacion.
4. `BUSINESS_DEFAULT_LOCATION`: unica sucursal activa, solo si hay una.
5. `MISSING`: no se pudo resolver sucursal.

## Flujo antes

```text
Contacto sin sucursal
↓
Mensaje con Providencia
↓
El flujo no siempre resolvia Providencia como locationId
↓
Validacion agenda con sucursal nula o incorrecta
↓
404 o pregunta innecesaria de sucursal
```

## Flujo despues

```text
Contacto sin sucursal
↓
Mensaje con Providencia
↓
Se resuelve Providencia desde MESSAGE_TEXT
↓
Se usa locationId de Providencia
↓
Se valida agenda
↓
Si hay cupo: reserva temporal + enlace
Si no hay cupo: no disponibilidad funcional
Si falta configuracion: mensaje funcional, no 404 tecnico
```

## Migracion agregada

`V27__fix_contact_location_resolution_and_service_location_seed.sql`

La migracion:

- asegura que Providencia exista y este activa;
- asegura disponibilidad demo de Limpieza facial profunda en Providencia;
- asegura disponibilidad demo de Depilacion bozo en Providencia;
- agrega reglas documentales de prioridad de sucursal en `aesthetic_business_rule`;
- no borra datos existentes.

## Pruebas agregadas

`test_ia_negocio_conversacional_v23_4_2.ps1`

Incluye casos para:

- contacto sin sucursal con mensaje que contiene Providencia;
- contacto sin sucursal sin sede en el mensaje;
- prioridad de Providencia frente a respaldos;
- servicio no configurado sin 404;
- reenvio de enlace;
- cancelacion;
- caso sensible.

## Como ejecutar

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_ia_negocio_conversacional_v23_4_2.ps1
```

## Riesgos

- Si la base de datos local ya tenia migraciones aplicadas parcialmente, puede requerir recrear volumen o aplicar migracion manual.
- El envio real de WhatsApp depende de que `whatsapp-web-service` este conectado y autenticado.
- Si no existe disponibilidad real de agenda, el resultado esperado sera mensaje funcional de no disponibilidad, no necesariamente enlace.

## Limitaciones

No se ejecuto compilacion Docker Compose en este entorno. La compilacion final y prueba real deben ejecutarse en el ambiente local del proyecto.
