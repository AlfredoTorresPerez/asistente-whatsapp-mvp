# Demo Local - Asistente de Reservas por WhatsApp

> **NOTA:** Este documento ha sido reemplazado por `README-LOCAL.md` (guía técnica) y `docs/DEMO_LOCAL_READINESS.md` (checklist demo).
> Se mantiene por compatibilidad; consultar los documentos actualizados para información completa.

## Requisitos Previos

- Docker Desktop (Windows) con WSL2 habilitado
- Git
- Puertos 5173, 8080, 5433, 5005 disponibles
- Conexion a internet (para builds iniciales y DNS)

## Comando para Levantar la Aplicacion

```bash
# Desde la raiz del proyecto
docker compose -f docker-compose.local.yml up -d --build
```

**Tiempo estimado:** 5-10 minutos (primera vez, por descarga de imagenes y compilacion).

## Puertos Usados

| Puerto | Servicio           | URL                                    |
|--------|--------------------|----------------------------------------|
| 5173   | Frontend (React)   | http://localhost:5173                   |
| 8080   | Backend (Spring)   | http://localhost:8080/api/v1/health     |
| 5433   | PostgreSQL         | jdbc:postgresql://localhost:5433        |
| 5005   | JDWP (Debug)       | IDE remote debug                        |

## URLs de la Aplicacion

- **Landing publica (reserva):** http://localhost:5173/reservar
- **Login administracion:** http://localhost:5173/login
- **Agenda semanal:** http://localhost:5173/agenda
- **Confirmacion publica:** http://localhost:5173/reservas/confirmar/{token}
- **Reprogramacion publica:** http://localhost:5173/reservas/reprogramar/{token}
- **Cancelacion publica:** http://localhost:5173/reservas/cancelar/{token}
- **Pago simulado:** http://localhost:5173/reservas/pagar/{paymentId}
- **Health check:** http://localhost:8080/api/v1/health

## Usuarios de Prueba

| Rol         | Email             | Password    |
|-------------|-------------------|-------------|
| Admin       | admin@demo.cl     | Cambiar123! |
| Agente      | agente@demo.cl    | Cambiar123! |
| Supervisor  | supervisor@demo.cl| Cambiar123! |

## Flujo Recomendado para la Demo

### 1. Reserva desde Landing (Publica)
1. Abrir http://localhost:5173/reservar
2. Seleccionar categoria (ej: Tratamientos faciales)
3. Seleccionar servicio (ej: Dermapen estetico)
4. Ver detalle del servicio (duracion, precio, profesional)
5. Seleccionar sucursal (ej: Maipu, Providencia o Santiago Centro)
6. Elegir fecha (dia siguiente habil)
7. Elegir horario disponible
8. Ingresar nombre, telefono y email del cliente
9. Confirmar reserva
10. Ver mensaje de exito con ID de reserva

### 2. Agenda Semanal (Admin)
1. Iniciar sesion en http://localhost:5173/login con admin@demo.cl / Cambiar123!
2. Ir a "Agenda"
3. Navegar entre semanas
4. Ver reservas existentes con colores por estado
5. Ver linea de hora actual (si es dia actual)
6. Hacer clic en una reserva para ver detalle

### 3. Reprogramacion
1. Desde detalle de reserva (agenda), hacer clic en "Reprogramar"
2. Seleccionar nueva fecha y hora
3. Confirmar cambio
4. Verificar que el estado cambia a REPROGRAMADA

### 4. Cancelacion
1. Desde detalle de reserva, ir a seccion de cancelacion
2. Ingresar motivo opcional
3. Confirmar cancelacion
4. Verificar que el estado cambia a CANCELADA

### 5. Pago Simulado
1. Desde detalle de reserva que require deposito
2. Generar link de pago
3. Abrir link de pago publico
4. Ver monto y estado PENDING
5. (Simulado) El administrador puede registrar pago manual como APROBADO o RECHAZADO

## Datos de Prueba Disponibles

La base de datos se siembra automaticamente via Flyway con:

- **1 empresa:** Centro Estetico Bella
- **4 sucursales:** Principal (Providencia), Maipu, Providencia, Santiago Centro
- **4 profesionales:** Carla Mendez, Valentina Rios, Daniela Soto, Marcela Fuentes
- **8 categorias de servicio:** Facial, Corporal, Depilacion, Manicure/Pedicure, Pestanas/Cejas, Peluqueria, Maquillaje, Medicina no invasiva
- **~56 servicios aestheticos** con precios, duraciones y descripciones
- **3 clientes de prueba:** Sofia Rojas, Paula Diaz, Camila Torres
- **Conversaciones y mensajes** de ejemplo
- **Reservas de ejemplo** en estado PENDIENTE_CONFIRMACION y CONFIRMADA
- **Horarios de atencion** Lunes a Sabado 09:00-18:00
- **Horarios de profesionales** configurados por sucursal
- **8 cabinas** (2 por sucursal)
- **Roles de usuario:** Admin, Agente, Supervisor

## Reiniciar Base de Datos

```bash
# Detener y eliminar el volumen de datos
docker compose -f docker-compose.local.yml down -v
# Volver a levantar
docker compose -f docker-compose.local.yml up -d
```

## Limpiar Datos de Demo (sin reiniciar)

Para liberar reservas temporales vencidas y restaurar datos demo a un estado consistente:

```bash
# Ejecutar script de limpieza
docker compose -f docker-compose.local.yml exec postgres psql -U assistant -d asistente_whatsapp
```

Luego en psql ejecutar:

```sql
-- Liberar reservas temporales expiradas
update booking_confirmation_link
set status = 'EXPIRED', expired_at = current_timestamp, updated_at = current_timestamp
where status in ('GENERATED', 'SENT', 'OPENED') and expires_at < current_timestamp;

update booking
set status = 'RELEASED', updated_at = current_timestamp
where status = 'TEMPORARY' and temporary_expires_at < current_timestamp;
```

## Problemas Comunes y Soluciones

### Error: "Role postgres does not exist"
El contenedor de postgres usa usuario `assistant`, no `postgres`. Conectarse con:
```bash
docker compose exec postgres psql -U assistant -d asistente_whatsapp
```

### Error: "Port already in use"
Cerrar otro servicio en el puerto o cambiar la configuracion en `docker-compose.local.yml`.

### Error: "WhatsApp channel not available"
El canal de WhatsApp es nativo del backend: con `APP_WHATSAPP_CHANNEL_PROVIDER=SIMULATED` no requiere conexión externa ni QR. Verificar logs con `docker compose logs backend-java`.

### Error: "Backend fails to start"
Revisar logs con `docker compose logs backend-java`. Posibles causas:
- Base de datos no disponible
- Puerto 8080 ocupado
- Variable de entorno faltante

### Error: "Frontend blank page"
- Verificar que el backend este accesible desde el frontend
- Revisar la consola del navegador
- Verificar que `VITE_API_BASE_URL` este configurado correctamente

## Funcionalidades Reales vs Simuladas

| Funcionalidad            | Estado        | Detalle                                                    |
|--------------------------|---------------|------------------------------------------------------------|
| Catalogo de servicios    | Real          | CRUD completo con categorias, precios, duracion            |
| Disponibilidad           | Real          | Calculo en tiempo real con conflictos                      |
| Reserva publica          | Real          | Wizard 5 pasos con confirmacion                            |
| Agenda semanal           | Real          | Vista semanal con filtros y detalle                        |
| Reprogramacion           | Real          | Publica e interna, con validacion de disponibilidad        |
| Cancelacion              | Real          | Con motivo opcional y actualizacion de estado              |
| Confirmacion por link    | Real          | Enlace publico con token hash                              |
| Envio de WhatsApp        | Simulado      | Canal nativo `SIMULATED`, no envia mensajes reales; en produccion usa la Cloud API de Meta |
| Envio de email           | Simulado      | No requiere credenciales SMTP reales                       |
| Pagos                    | Simulado      | Proveedor SIMULATED, sin integracion real                  |
| Calendario Google        | Deshabilitado | No requiere cuenta Google para demo                        |
| Agentes IA               | Real          | Reglas de negocio, respuestas automaticas                  |
| Autenticacion            | Real          | JWT con roles y permisos                                   |
| Notificaciones           | Real          | Notificaciones internas en la plataforma                   |

## Recomendaciones para la Presentacion

1. **Abrir todo antes:** Levantar la aplicacion 10 minutos antes de la demo
2. **Tener datos frescos:** Si se hicieron pruebas, reiniciar la BD con `down -v` y `up -d`
3. **Preparar ejemplos:** Tener listos un par de servicios para mostrar
4. **Probar conectividad:** Verificar que http://localhost:5173 y http://localhost:8080/api/v1/health respondan
5. **Mostrar flujo completo:** Reserva publica -> Ver en agenda -> Reprogramar -> Cancelar
6. **Mencionar simulaciones:** Explicar que WhatsApp y pagos son simulados pero la logica de negocio es real
