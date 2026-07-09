# Matriz de Pruebas — Agenda Digital Asistente WhatsApp

**Fuente:** `agenda_digital_whatsapp_casuisticas.xlsx` (10 hojas)
**Versión:** 1.0
**Fecha:** 2026-07-06

## Resumen por hoja

| Hoja | Casos | Cobertura esperada |
|------|-------|--------------------|
| 01_Capacidades | 50 capacidades | 100% en tests de regresión |
| 02_Pre_Reserva | 56 pre-validaciones | 80% backend API, 20% UI |
| 03_Post_Reserva | 27 post-validaciones | 70% backend API, 30% UI |
| 04_Reprogramar | 30 validaciones | 75% backend API, 25% UI |
| 05_Cancelar | 24 validaciones | 75% backend API, 25% UI |
| 06_MultiSucursal | 8+10 casos | 60% API + UI |
| 07_Profesional | 20 casos | 80% backend API |
| 08_Motor_Disponibilidad | 15 reglas | 70% API |
| 09_Backlog_Tests | 137+ tests | Mapeados a specs |
| 10_Datos_Minimos | 40 campos | Validación en backend |

## Mapeo Excel → Test Specs

### NIVEL 1: Smoke Tests (01-smoke.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| CAP-001 | Frontend carga | `01-smoke.spec.ts` | PASSED |
| CAP-002 | Health check backend | `01-smoke.spec.ts` | PASSED |
| CAP-003 | Login page visible | `01-smoke.spec.ts` | PASSED |
| CAP-004 | Menú principal visible | `01-smoke.spec.ts` | PASSED |
| CAP-005 | Módulo Agenda abre | `01-smoke.spec.ts` | PASSED |
| CAP-006 | Módulo Conversaciones abre | `01-smoke.spec.ts` | PASSED |

### NIVEL 2: Auth (02-auth.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| CAP-007 | Login exitoso | `02-auth.spec.ts` | PASSED |
| CAP-008 | Login inválido muestra error | `02-auth.spec.ts` | PASSED |
| CAP-009 | Logout funciona | `02-auth.spec.ts` | PASSED |
| CAP-010 | Sesión persistente | `02-auth.spec.ts` | PASSED |
| CAP-011 | Ruta protegida sin sesión | `02-auth.spec.ts` | PASSED |

### NIVEL 3: Agenda Básica (03-agenda-basica.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| CAP-012 | Agenda 7 días | `03-agenda-basica.spec.ts` | PASSED |
| CAP-013 | Columna de horas visible | `03-agenda-basica.spec.ts` | PASSED |
| CAP-014 | Zona horaria America/Santiago | `03-agenda-basica.spec.ts` | PASSED |
| CAP-015 | Navegación de semana | `03-agenda-basica.spec.ts` | PASSED |
| TST-060 | Slot ocupado bloqueado | `03-agenda-basica.spec.ts` | PASSED |

### NIVEL 4: Disponibilidad Horaria (10-disponibilidad-horaria.spec.ts + 04-agenda-visual.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| PRE-RES-015 | Horario fuera de atención | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-016 | Feriado bloquea | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-019 | Profesional inactivo | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-022 | Fuera de horario profesional | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-023 | Bloqueo manual | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-024 | Ausencia profesional | `10-disponibilidad-horaria.spec.ts` | BLOCKED (endpoint simulación ausencia no expuesto) |
| PRE-RES-025 | Solapamiento | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-027 | Fecha pasada | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-030 | Duración no cabe | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-031 | Buffer previo | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-032 | Buffer posterior | `10-disponibilidad-horaria.spec.ts` | PASSED |
| PRE-RES-036 | Cabina no disponible | `10-disponibilidad-horaria.spec.ts` | PASSED |

### NIVEL 5: Filtros de Agenda (09-filtros-agenda.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| CAP-020 | Filtro servicio | `09-filtros-agenda.spec.ts` | PASSED |
| CAP-021 | Filtro profesional | `09-filtros-agenda.spec.ts` | PASSED |
| CAP-022 | Filtro cabina | `09-filtros-agenda.spec.ts` | PASSED |
| CAP-023 | Filtro sucursal (2+ suc) | `09-filtros-agenda.spec.ts` | PASSED |
| CAP-024 | Opción "Todos" | `09-filtros-agenda.spec.ts` | PASSED |
| TST-MUL-001 | 1 sucursal auto-seleccionada | `09-filtros-agenda.spec.ts` | PASSED |

### NIVEL 6: Reserva desde Interfaz (11-reservas-interfaz.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| TST-001 | Cliente sin nombre | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-006 | Servicio inactivo | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-012 | Servicio no disponible sucursal | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-013 | Sucursal inactiva | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-015 | Horario cerrado | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-025 | Solapamiento | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-027 | Fecha pasada | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-028 | Anticipación mínima | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-036 | Cabina no disponible | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-044 | Cliente con inasistencias | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-057 | Reserva guardada | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-058 | Estado inicial correcto | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-059 | No duplicidad | `11-reservas-interfaz.spec.ts` | PASSED |
| TST-062 | Agenda actualizada | `11-reservas-interfaz.spec.ts` | PASSED |

### NIVEL 7: Reprogramación desde Interfaz (pendiente crear 13-reprogramacion-interfaz.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| TST-084 | Reserva no existe | `13-reprogramacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-085 | Estado no reprogramable | `13-reprogramacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-092 | Nueva fecha pasada | `13-reprogramacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-094 | Nuevo slot ocupado | `13-reprogramacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-107 | Mantener original hasta confirmar nuevo | `13-reprogramacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-108 | Cambio atómico | `13-reprogramacion-interfaz.spec.ts` | BLOCKED (requiere API) |

### NIVEL 8: Cancelación desde Interfaz (pendiente crear 14-cancelacion-interfaz.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| TST-114 | Reserva no existe | `14-cancelacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-115 | Estado no cancelable | `14-cancelacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-117 | Fuera de plazo | `14-cancelacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-125 | Liberar slot | `14-cancelacion-interfaz.spec.ts` | BLOCKED (requiere API) |
| TST-130 | Anular recordatorios | `14-cancelacion-interfaz.spec.ts` | BLOCKED (requiere API) |

### NIVEL 9: WhatsApp Simulado Reserva (05-whatsapp-reserva-simulada.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| TST-001 a TST-056 | Flujo WhatsApp reserva | `05-whatsapp-reserva-simulada.spec.ts` | BLOCKED (sin endpoint simulación WhatsApp) |

### NIVEL 10: WhatsApp Simulado Cancelación (06-whatsapp-cancelacion-simulada.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| TST-114 a TST-137 | Flujo WhatsApp cancelación | `06-whatsapp-cancelacion-simulada.spec.ts` | BLOCKED (sin endpoint simulación WhatsApp) |

### NIVEL 11: WhatsApp Simulado Reprogramación (07-whatsapp-reprogramacion-simulada.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| TST-084 a TST-113 | Flujo WhatsApp reprogramación | `07-whatsapp-reprogramacion-simulada.spec.ts` | BLOCKED (sin endpoint simulación WhatsApp) |

### NIVEL 12: Confirmación Pública (08-confirmacion-publica.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| CAP-040 | Link público carga sin auth | `08-confirmacion-publica.spec.ts` | PASSED |
| CAP-041 | Vista previa con datos | `08-confirmacion-publica.spec.ts` | PASSED |
| CAP-042 | Confirmar cambia estado | `08-confirmacion-publica.spec.ts` | PASSED |
| CAP-043 | Doble confirmación rechazada | `08-confirmacion-publica.spec.ts` | PASSED |
| CAP-044 | Link expirado muestra error | `08-confirmacion-publica.spec.ts` | PASSED |
| CAP-045 | Link inválido 404 | `08-confirmacion-publica.spec.ts` | PASSED |
| TST-065 | Link de pago creado | `08-confirmacion-publica.spec.ts` | PASSED |
| TST-066 | Estado de pago coherente | `08-confirmacion-publica.spec.ts` | PASSED |

### NIVEL 13: Sucursales (12-sucursales.spec.ts - pendiente crear)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| TST-MUL-001 | 1 sucursal auto-seleccionada | `12-sucursales.spec.ts` | BLOCKED (requiere seed datos) |
| TST-MUL-002 | 2 sucursales con selector | `12-sucursales.spec.ts` | BLOCKED (requiere seed datos) |
| DETALLE-1 | Horario por sucursal | `12-sucursales.spec.ts` | BLOCKED |
| DETALLE-3 | Servicios por sucursal | `12-sucursales.spec.ts` | BLOCKED |
| DETALLE-4 | Profesionales por sucursal | `12-sucursales.spec.ts` | BLOCKED |

### NIVEL 14: Profesional (13-profesional.spec.ts - pendiente crear)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| PRO-001 | Profesional activo | `13-profesional.spec.ts` | BLOCKED (requiere API data) |
| PRO-003 | Servicio habilitado | `13-profesional.spec.ts` | BLOCKED |
| PRO-005 | Asignación por sede | `13-profesional.spec.ts` | BLOCKED |
| PRO-007 | Horario laboral | `13-profesional.spec.ts` | BLOCKED |
| PRO-009 | Ausencias | `13-profesional.spec.ts` | BLOCKED |
| PRO-010 | Solapamiento | `13-profesional.spec.ts` | BLOCKED |
| PRO-015 | Máximo diario | `13-profesional.spec.ts` | BLOCKED |

### NIVEL 15: Base de Datos (14-base-datos.spec.ts - pendiente crear)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| POST-RES-001 | Reserva guardada correctamente | `14-base-datos.spec.ts` | BLOCKED (sin conexión DB segura) |
| POST-RES-003 | No duplicidad | `14-base-datos.spec.ts` | BLOCKED |
| POST-RES-004 | Slot ocupado | `14-base-datos.spec.ts` | BLOCKED |

### NIVEL 16: Concurrencia (15-concurrencia.spec.ts - pendiente crear)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| PRE-RES-034 | Reserva simultánea | `15-concurrencia.spec.ts` | BLOCKED (requiere API token + endpoints) |

### NIVEL 17: Visual Calendar (admin-locations-visual.spec.ts + 04-agenda-visual.spec.ts)

| Excel ID | Caso | Spec | Estado |
|----------|------|------|--------|
| CAP-050 | Agenda 7 columnas | `04-agenda-visual.spec.ts` | PASSED |
| CAP-051 | Línea hora actual | `04-agenda-visual.spec.ts` | PASSED |
| CAP-052 | Sin superposición | `04-agenda-visual.spec.ts` | PASSED |
| CAP-053 | Diseño responsivo | `admin-locations-visual.spec.ts` | PASSED |

## Leyenda de Estados

| Estado | Significado |
|--------|-------------|
| PASSED | Prueba implementada y ejecutada correctamente |
| FAILED | Prueba implementada pero falla |
| SKIPPED | Prueba no ejecutada por condición válida (ej: solo CI) |
| BLOCKED | Prueba no ejecutable porque falta endpoint, dato, pantalla o configuración |
