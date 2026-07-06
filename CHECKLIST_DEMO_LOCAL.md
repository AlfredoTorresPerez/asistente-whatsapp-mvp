# Checklist Demo Local

## Estado General: LISTO CON OBSERVACIONES

| #  | Item                                      | Estado    | Archivo/Modulo                            | Observacion                                                                 | Riesgo |
|----|-------------------------------------------|-----------|-------------------------------------------|-----------------------------------------------------------------------------|--------|
| 1  | Aplicacion levanta localmente             | LISTO     | docker-compose.local.yml                  | `docker compose up -d --build` funciona                                    | Bajo   |
| 2  | Frontend abre sin errores                 | LISTO     | frontend-react/nginx.conf                 | Servido via nginx en :5173                                                  | Bajo   |
| 3  | Backend inicia sin errores                | LISTO     | backend-java                              | Health check responde 200                                                   | Bajo   |
| 4  | Base de datos carga datos de prueba       | LISTO     | V4, V8, V17, V20, V31, etc.               | Flyway ejecuta 39 migraciones; datos completos                             | Bajo   |
| 5  | Categorias se muestran                    | LISTO     | PublicLandingController                   | GET /api/v1/public/landing/categories devuelve 8 categorias                 | Bajo   |
| 6  | Servicios se muestran como tarjetas       | LISTO     | ServiceCard.tsx                           | Se ven correctamente con nombre, precio, duracion                          | Bajo   |
| 7  | Reserva se confirma sin caida             | CORREGIDO | CompleteAgendaJdbcRepository              | Fix hasConflict: ahora detecta profesional cruzando sucursales (era 500, ahora 409) | Alto |
| 8  | Agenda semanal se ve completa             | LISTO     | CompleteAgendaPage.tsx                    | Vista semanal 7 dias, scroll vertical, reservas coloreadas                  | Medio  |
| 9  | Linea de hora actual se posiciona bien    | LISTO     | CompleteAgendaPage.tsx                    | Linea roja + circulo + etiqueta "Ahora HH:mm", actualiza cada 60s           | Bajo   |
| 10 | Reprogamacion funciona                    | LISTO     | BookingReschedulePage / RescheduleAppointmentPage | Flujo publico e interno operativo                                  | Medio  |
| 11 | Cancelacion funciona                      | CORREGIDO | PublicBookingCancellationController       | Agregado GET fallback para clientes de correo                               | Medio  |
| 12 | Pago simulado funciona                    | LISTO     | BookingPaymentPage                        | Proveedor SIMULATED, permite ver estado y registrar pago manual             | Medio  |
| 13 | No hay botones bloqueados injustificadamente | LISTO  | Frontend                                  | Botones habilitados cuando corresponde segun estado de reserva             | Medio  |
| 14 | No hay campos vacios visibles por error   | LISTO     | Frontend                                  | Formularios con datos de prueba cargados                                   | Bajo   |
| 15 | No hay errores tecnicos expuestos al cliente | CORREGIDO | GlobalExceptionHandler                    | Excepciones con mensaje amigable; 500 reemplazado por 409 con mensaje claro | Alto   |
| 16 | Diseno responsivo validado                | LISTO     | Frontend                                  | Adaptable a notebook y monitor; grid semanal usa minmax(0,1fr)             | Medio  |
| 17 | URLs de confirmacion locales              | CORREGIDO | .env                                      | Cambiado de cloudflare.trycloudflare.com a localhost:5173                   | Alto   |
| 18 | Envio de email en modo simulacion          | CORREGIDO | docker-compose.local.yml                  | APP_EMAIL_ENABLED=false, APP_EMAIL_SIMULATION_ENABLED=true                  | Medio  |
| 19 | Calendario Google deshabilitado            | CORREGIDO | docker-compose.local.yml                  | APP_CALENDAR_GOOGLE_ENABLED=false, sin credenciales                         | Bajo   |
| 20 | GET fallback cancelacion/reserva           | CORREGIDO | PublicBookingCancellationController, PublicBookingRescheduleController | Agregados metodos GET /confirm y GET /reject | Bajo |
| 21 | Documentacion local creada                | LISTO     | README_DEMO_LOCAL.md, CHECKLIST_DEMO_LOCAL.md | Comando, URLs, usuarios, flujo recomendado, problemas comunes             | Bajo   |
| 22 | Conflictos de disponibilidad corregidos   | CORREGIDO | CompleteAgendaJdbcRepository              | hasConflict ahora verifica profesional sin filtrar por sucursal             | Alto   |

## Correcciones Aplicadas

1. **`.env`**: URLs de cloudflare reemplazadas por `http://localhost:5173/...`
2. **`CompleteAgendaJdbcRepository.hasConflict()`**: Separada logica profesional (cross-location) de room (misma location). Profesional no puede estar en dos sucursales a la vez.
3. **`PublicBookingCancellationController`**: Agregado `@GetMapping("/confirm")` para clientes de correo que navegan via GET
4. **`PublicBookingRescheduleController`**: Agregados `@GetMapping("/confirm")` y `@GetMapping("/reject")`
5. **`docker-compose.local.yml`**: Email en modo simulacion, Google Calendar deshabilitado, WhatsApp demo fallback activado
6. **`BookingConfirmationService`** (anterior): Eliminados metodos redundantes, creado `BookingConfirmationNotificationsService` con transacciones aisladas

## Archivos Modificados

| Archivo | Cambio |
|---------|--------|
| `.env` | URLs locales en vez de cloudflare |
| `docker-compose.local.yml` | Email simulado, calendar deshabilitado, demo fallback |
| `CompleteAgendaJdbcRepository.java` | Fix hasConflict cross-location |
| `PublicBookingCancellationController.java` | GET fallback |
| `PublicBookingRescheduleController.java` | GET fallback |

## Pendientes no Bloqueantes

| Item | Detalle |
|------|---------|
| Exclusion constraint violations en produccion | `ex_booking_professional_no_overlap_active` causa 500 si no se pasa por hasConflict; ya corregido en codigo pero datos existentes pueden tener conflictos |
| Role "app" no existe en PostgreSQL | Solo en ambientes nuevos; no afecta demo actual |
| Seed booking dates (68000000-...) estan en pasado (Jun 30, Jul 2) | Datos demo originales; no afectan flujo porque se pueden crear nuevas reservas |
| WhatsApp Web requiere escanear QR | Sin QR, los mensajes no se envian realmente; modo demo fallback activado |
| OpenAI deshabilitado por falta de API key | Agentes IA no responden automaticamente; reglas de negocio seed funcionan offline |

## Comando para Levantar Demo Local

```bash
git clone <repo> && cd asistente
docker compose -f docker-compose.local.yml up -d --build
```

Luego abrir http://localhost:5173

## Flujo Sugerido para Presentar al Cliente

1. Landing publica → seleccionar categoria/servicio → fecha/hora → datos cliente → confirmar
2. Login como admin@demo.cl / Cambiar123!
3. Ver la reserva creada en la agenda semanal
4. Reprogramar la reserva a otro horario
5. Cancelar la reserva con motivo
6. (Opcional) Mostrar pago simulado desde detalle de reserva
7. Refrescar la pagina para ver consistencia de datos

## Recomendacion Final

**LISTO CON OBSERVACIONES** — La aplicacion es funcional para una demo local. Lospendientes son no bloqueantes. Se recomienda preparar el entorno 10 minutos antes y tener datos frescos (recrear BD si se hicieron pruebas intensivas).
