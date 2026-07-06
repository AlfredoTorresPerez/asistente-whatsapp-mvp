# Cambios aplicados: cancelar y reprogramar reservas por WhatsApp

## Objetivo
Corregir el flujo conversacional para que las solicitudes del cliente por WhatsApp puedan cancelar o reprogramar una reserva existente, especialmente casos como:

- "Quiero cancelar la hora de hoy de las 14:00."
- "Quiero reprogramar mi hora de hoy a las 14:00."

## Cambios principales

### 1. Resolucion de fecha con zona horaria del negocio
Se ajusto la resolucion de fechas relativas como `hoy`, `manana` y `pasado manana` para usar la zona horaria del negocio. Si no existe zona configurada, se usa `America/Santiago`.

Esto evita que `hoy` sea desplazado al dia siguiente por conversiones UTC.

### 2. Busqueda por rango diario local
La busqueda de reservas activas ahora calcula rango diario local:

- inicio del dia local
- termino del dia local

La consulta sigue usando `starts_at >= from` y `starts_at < to`, pero los valores se construyen desde la fecha local del negocio.

### 3. Comparacion de hora local
La hora solicitada por el cliente se compara contra la hora local de la reserva en la zona del negocio, no contra el valor crudo de `OffsetDateTime`.

### 4. Busqueda progresiva de reservas candidatas
Se agregaron capas de busqueda:

1. Contexto estricto: cliente, conversacion, telefono, fecha, hora, sede y servicio.
2. Contexto sin filtros debiles: remueve sede y servicio si no encuentra resultados.
3. Telefono normalizado: busca por telefono aunque falle conversacion o cliente.
4. Fallback activo: busca reservas activas del contexto sin fecha estricta.

Los filtros de sede y servicio ahora sirven para priorizar, no para descartar de inmediato.

### 5. Normalizacion robusta de telefono
La consulta ahora compara:

- telefono completo normalizado
- ultimos 9 digitos
- ultimos 8 digitos
- ultimos 4 digitos como respaldo para datos de demostracion

Esto ayuda en casos como telefonos largos provenientes de WhatsApp y clientes de prueba como `Contacto 0505`.

### 6. Reprogramacion sin confundir hora original con nueva hora
Si el cliente dice:

`Quiero reprogramar mi hora de hoy a las 14:00.`

el sistema usa esa fecha y hora para encontrar la reserva original, pero ya no interpreta automaticamente esa misma hora como nuevo horario destino. En ese caso debe preguntar por el nuevo dia y horario.

### 7. Consistencia de intencion
Se sincroniza `extractedData.intencion` con la intencion principal:

- `BOOKING_CANCEL` => `cancelar_reserva`
- `BOOKING_CHANGE` => `reprogramar_reserva`

## Archivos modificados

- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/TransactionalAgendaBookingService.java`
- `backend-java/src/main/java/com/asistentewhatsapp/agenda/infrastructure/CompleteAgendaJdbcRepository.java`
- `backend-java/src/main/java/com/asistentewhatsapp/aiagents/application/BookingAgent.java`

## Validacion
Se intento ejecutar compilacion con Maven Wrapper, pero el entorno no tiene acceso a internet para descargar Maven:

`wget: Failed to fetch https://repo.maven.apache.org/.../apache-maven-3.9.15-bin.zip`

Se hizo una verificacion sintactica parcial con `javac`, que no reporto errores de sintaxis en los cambios antes de detenerse por dependencias externas no disponibles.
