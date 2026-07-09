# Matriz de Pruebas Automatizadas — Asistente de Negocios WhatsApp

## Proposito
Matriz completa de pruebas E2E, ordenadas desde lo mas basico a lo mas avanzado,
que permite validar todas las funcionalidades principales del asistente de negocios
multi-sucursal con agenda digital, reservas WhatsApp, confirmacion publica y
gestion de clientes.

## Convenciones
- **ID**: `QA-NIVEL-NUMERO` (ej: `QA-01-001`)
- **Prefijo datos prueba**: `QA_AUTO_`
- **Zona horaria**: `America/Santiago` (GMT-4 / GMT-3 segun horario)
- **Estados permitidos**: PASSED / FAILED / SKIPPED / BLOCKED

---

## NIVEL 1 — Smoke Tests basicos
*Validan que el sistema levanta y responde.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-01-001 | Frontend carga en http://localhost:5173 | Pagina renderiza sin errores JS | Frontend UP |
| QA-01-002 | Backend responde health check en http://localhost:8080 | HTTP 200 con status UP | Backend UP |
| QA-01-003 | PostgreSQL disponible via backend | Backend reporta DB saludable | DB UP |
| QA-01-004 | Login page carga | Formulario email + password visible | Frontend UP |
| QA-01-005 | Menu principal se visualiza tras login | Sidebar con modulos visibles | Auth OK |
| QA-01-006 | Modulo Agenda completa se abre | Calendario semanal con columnas y horas | Auth OK |
| QA-01-007 | Modulo Conversaciones se abre | Lista de conversaciones carga | Auth OK |

## NIVEL 2 — Pruebas funcionales basicas
*Validan navegacion y operacion simple.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-02-001 | Login correcto con usuario valido | Redirige a /dashboard | Backend Auth |
| QA-02-002 | Login incorrecto muestra error | Mensaje de error visible | Backend Auth |
| QA-02-003 | Logout funciona | Redirige a /login, sesion limpia | Auth OK |
| QA-02-004 | Cambio de modulo desde sidebar | Navega sin recargar | Auth OK |
| QA-02-005 | Visualizacion de usuario logueado | Nombre y rol en Topbar | Auth OK |
| QA-02-006 | Carga de negocio/sucursal activa | Datos de negocio disponibles | Backend OK |
| QA-02-007 | Carga inicial de agenda semanal | Calendario con 7 columnas | Auth OK |
| QA-02-008 | Carga de conversaciones | Lista de conversaciones | Auth OK |
| QA-02-009 | Carga de clientes/prospectos | Tabla de prospectos | Auth OK |

## NIVEL 3 — Agenda digital basica
*Validan visualizacion de calendario.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-03-001 | Agenda muestra semana Lun-Dom | 7 columnas Lun-Dom | Frontend |
| QA-03-002 | Agenda muestra columna de horas | Columna hora 09-21 | Frontend |
| QA-03-003 | Agenda respeta horario 09:00-21:00 | Franja visible de 09 a 21 | Frontend |
| QA-03-004 | Agenda muestra zona horaria configurada | America/Santiago o GMT-4 | Frontend |
| QA-03-005 | Agenda muestra fecha seleccionada | Semana segun fecha elegida | Frontend |
| QA-03-006 | Agenda permite cambiar rango semanal | Navegacion siguiente/anterior | Frontend |
| QA-03-007 | Agenda muestra reservas existentes | Event cards visibles en celdas | Backend |
| QA-03-008 | Agenda NO muestra canceladas como ocupadas | Status CANCELLED filtrado | Backend |
| QA-03-009 | Agenda muestra temporales pendientes | Status PENDIENTE_CONFIRMACION | Backend |
| QA-03-010 | Agenda muestra reservas confirmadas | Status CONFIRMED visible | Backend |

## NIVEL 4 — Filtros de Agenda completa
*Validan filtros por ID (dropdown/select).*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-04-001 | Filtro Servicio es dropdown/select | Select de opciones carga | API |
| QA-04-002 | Filtro Profesional es dropdown/select | Select de opciones carga | API |
| QA-04-003 | Filtro Cabina es dropdown/select | Select de opciones carga | API |
| QA-04-004 | Filtro Sucursal funciona | Select con sucursales activas | API |
| QA-04-005 | Opcion "Todos" muestra todas | Sin filtro, agenda completa | API |
| QA-04-006 | Seleccionar Servicio filtra | Solo reservas de ese servicio | API |
| QA-04-007 | Seleccionar Profesional filtra | Solo reservas de ese profesional | API |
| QA-04-008 | Seleccionar Cabina filtra | Solo reservas de esa cabina | API |
| QA-04-009 | Combinacion 3 filtros funciona | Filtro compuesto correcto | API |
| QA-04-010 | Limpiar filtros recupera completa | Reset a "Todos" | API |

## NIVEL 5 — Disponibilidad horaria
*Validan reglas reales de disponibilidad.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-05-001 | Horario libre aparece disponible | Slot con badge "Disponible" | API Agenda |
| QA-05-002 | Horario ocupado aparece bloqueado | Slot con badge "Bloqueado" | API Agenda |
| QA-05-003 | Reserva confirmada bloquea cupo | Slot no disponible | API Agenda |
| QA-05-004 | Reserva temporal vigente bloquea | Slot no disponible | API Agenda |
| QA-05-005 | Reserva cancelada libera cupo | Slot disponible tras cancelar | API Agenda |
| QA-05-006 | Bloque manual (agenda_block) bloquea | Slot no disponible | API Agenda |
| QA-05-007 | Feriado (agenda_holiday) afecta | Dia sin disponibilidad | API Agenda |
| QA-05-008 | Fuera de horario atencion no disponible | Slot no listado | Backend |
| QA-05-009 | Fuera de horario profesional no disponible | Slot no listado | Backend |
| QA-05-010 | Disponibilidad considera todos los recursos | Servicio+Profesional+Cabina+Sede | Backend |

## NIVEL 6 — Reservas desde interfaz
*Validan creacion manual de reservas.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-06-001 | Crear reserva con datos validos | Reserva creada exitosamente | API |
| QA-06-002 | Crear reserva sin cliente error | Error campo obligatorio | Frontend |
| QA-06-003 | Crear reserva sin servicio error | Error campo obligatorio | Frontend |
| QA-06-004 | Reserva en horario ocupado rechazada | Error solapamiento | Backend |
| QA-06-005 | Reserva fuera de horario rechazada | Error fuera de horario | Backend |
| QA-06-006 | Reserva valida aparece en agenda | Event card en calendario | Backend |
| QA-06-007 | Reserva muestra datos correctos | Servicio, cliente, prof, cabina, hora | Backend |
| QA-06-008 | Reserva respeta zone America/Santiago | Hora local correcta | Backend |
| QA-06-009 | Reserva no se desplaza a UTC | Hora coherente en UI | Backend |
| QA-06-010 | Reserva persistida en DB | Query DB confirma registro | DB |

## NIVEL 7 — Flujo WhatsApp simulado: reservar
*Validar flujo conversacional sin WhatsApp real.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-07-001 | Cliente envia "Quiero reservar limpieza facial" | Sistema recibe y procesa | Endpoint simulacion |
| QA-07-002 | IA solicita datos faltantes | Respuesta pide fecha/hora/sede | Backend AI |
| QA-07-003 | Cliente entrega fecha | Sistema reconoce fecha valida | Backend AI |
| QA-07-004 | Cliente entrega hora | Sistema reconoce hora valida | Backend AI |
| QA-07-005 | Cliente entrega sede | Sistema reconoce sede valida | Backend AI |
| QA-07-006 | Backend valida disponibilidad | Slots disponibles consultados | API Agenda |
| QA-07-007 | Sistema crea reserva temporal | Booking creada status TEMPORARY | Backend |
| QA-07-008 | Sistema entrega enlace confirmacion | Link generado | Backend |
| QA-07-009 | Reserva aparece en Agenda completa | Visible en calendario | Backend |
| QA-07-010 | Estado inicial PENDIENTE_CONFIRMACION | Status correcto | Backend |

## NIVEL 8 — Confirmacion publica de reserva
*Validar enlace publico de confirmacion.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-08-001 | Abrir link publico valido | Pagina carga sin login | Endpoint publico |
| QA-08-002 | Pagina publica carga sin auth | Sin redirect a login | Public endpoint |
| QA-08-003 | Pagina muestra datos de reserva | Servicio, fecha, hora, sede | Public endpoint |
| QA-08-004 | Confirmar reserva cambia a CONFIRMED | Status actualizado | Public endpoint |
| QA-08-005 | Reserva confirmada en agenda | Event card status CONFIRMED | Backend |
| QA-08-006 | Link ya confirmado rechaza doble confirmacion | Error controlado | Public endpoint |
| QA-08-007 | Link expirado muestra mensaje | Mensaje expiracion claro | Public endpoint |
| QA-08-008 | Link invalido muestra error 404/400 | Error controlado | Public endpoint |

## NIVEL 9 — Flujo WhatsApp simulado: cancelar
*Validar cancelacion via WhatsApp simulado.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-09-001 | Cliente con reserva activa pide cancelar | Sistema reconoce intencion | Endpoint simulacion |
| QA-09-002 | Sistema identifica reserva activa | Busca booking por telefono | Backend |
| QA-09-003 | Una sola reserva: confirma cancelacion | Solicita confirmacion o cancela directo | Backend AI |
| QA-09-004 | Multiples reservas: lista opciones | Muestra numeros de reserva | Backend AI |
| QA-09-005 | Cliente selecciona opcion numerica | Sistema procesa seleccion | Backend AI |
| QA-09-006 | Sistema cancela reserva correcta | Booking cancelado | Backend |
| QA-09-007 | Estado cambia a CANCELLED | Status actualizado | Backend |
| QA-09-008 | Reserva cancelada libera disponibilidad | Slot disponible nuevamente | Backend |
| QA-09-009 | Agenda deja de mostrar como ocupada | Event card desaparece/quita | Backend |
| QA-09-010 | Historial de estado registrado | booking_status_history creado | DB |

## NIVEL 10 — Flujo WhatsApp simulado: reprogramar
*Validar reprogramacion via WhatsApp simulado.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-10-001 | Cliente con reserva activa pide reprogramar | Sistema reconoce intencion | Endpoint simulacion |
| QA-10-002 | Sistema identifica reserva activa | Busca booking por telefono | Backend |
| QA-10-003 | Cliente entrega nueva fecha | Sistema reconoce fecha | Backend AI |
| QA-10-004 | Cliente entrega nueva hora | Sistema reconoce hora | Backend AI |
| QA-10-005 | Sistema valida disponibilidad nueva | Slot disponible consultado | API Agenda |
| QA-10-006 | Disponible: crea nueva reserva o actualiza | Segun regla de negocio | Backend |
| QA-10-007 | Reserva anterior queda RESCHEDULED | Status actualizado | Backend |
| QA-10-008 | Nueva reserva aparece en agenda | Event card nueva fecha | Backend |
| QA-10-009 | Nueva hora bloquea disponibilidad | Slot ocupado | Backend |
| QA-10-010 | Si ocupado: sistema rechaza y ofrece alternativas | Mensaje claro de rechazo | Backend AI |

## NIVEL 11 — Pruebas visuales tipo Google Calendar
*Validar interfaz visual del calendario.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-11-001 | Semana muestra 7 columnas completas | Grid de 7 columnas | Frontend |
| QA-11-002 | Hora actual muestra linea punteada | Linea roja "Ahora HH:mm" | Frontend |
| QA-11-003 | Linea actual cruza toda la semana | Travesia horizontal | Frontend |
| QA-11-004 | Linea respeta America/Santiago | Hora correcta segun TZ | Frontend |
| QA-11-005 | Reservas en hora correcta | Posicion vertical correcta | Frontend |
| QA-11-006 | Multiples reservas en una hora contenidas | Sin solapamiento visual | Frontend |
| QA-11-007 | Celda crece si hay varias reservas | Altura dinamica segun items | Frontend |
| QA-11-008 | Ninguna tarjeta invade hora siguiente | Max 60min por celda | Frontend |
| QA-11-009 | Eventos muestran badge WA si origen WhatsApp | Badge "WA" visible | Frontend |
| QA-11-010 | Screenshot visual generado como evidencia | PNG en screenshots/ | Test |

## NIVEL 12 — Pruebas de regresion avanzada
*Validan estabilidad completa del flujo.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-12-001 | Reserva + confirmacion + cancelacion | Flujo completo sin errores | Todos modulos |
| QA-12-002 | Reserva + confirmacion + reprogramacion | Flujo completo sin errores | Todos modulos |
| QA-12-003 | Reserva temporal expirada libera cupo | Slot disponible tras expiracion | Backend |
| QA-12-004 | Multiples clientes horarios distintos | Reservas independientes OK | Backend |
| QA-12-005 | Intento doble reserva mismo horario | Segunda rechazada | Backend |
| QA-12-006 | Reserva con distinta sede | Filtro por sede funciona | Backend |
| QA-12-007 | Reserva con distinto profesional | Filtro por profesional funciona | Backend |
| QA-12-008 | Reserva con distinta cabina | Filtro por cabina funciona | Backend |
| QA-12-009 | Reserva cruzando cambio de dia | Sin desplazamiento horario | Backend |
| QA-12-010 | Filtros post crear/cancelar/reprogramar | Filtros siguen funcionando | Frontend |

## NIVEL 13 — Pruebas con WhatsApp real de laboratorio (OPCIONAL)
*Solo si existe numero de prueba.* **NO usar numero personal.**

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-13-001 | Conectar sesion WhatsApp Web laboratorio | QR escaneado, sesion activa | WPP Service |
| QA-13-002 | Enviar mensaje real desde num prueba | Mensaje recibido en backend | WhatsApp real |
| QA-13-003 | Validar recepcion en Conversaciones | Conversacion visible en UI | Backend |
| QA-13-004 | Validar respuesta generada | Bot responde automaticamente | Backend AI |
| QA-13-005 | Validar creacion de reserva temporal | Booking en DB | Backend |
| QA-13-006 | Validar cancelacion real | Booking cancelado | Backend |
| QA-13-007 | Validar reprogramacion real | Booking reprogramado | Backend |
| QA-13-008 | No responde duplicado | Deduplicacion funciona | Backend |
| QA-13-009 | Trazabilidad del mensaje | message_log registrado | DB |
| QA-13-010 | Reconexion tras perdida de sesion | Reconexion automatica | WPP Service |

## NIVEL 14 — Seguridad y errores controlados
*Validar robustez del sistema.*

| ID | Caso | Resultado Esperado | Dependencia |
|----|------|--------------------|-------------|
| QA-14-001 | Endpoint protegido sin token devuelve 401 | HTTP 401 Unauthorized | Backend |
| QA-14-002 | Usuario sin permiso no accede a modulo | Redirect o denegado | Backend |
| QA-14-003 | Link publico invalido no expone datos internos | Error generico, sin stacktrace | Public endpoint |
| QA-14-004 | Payload WhatsApp invalido no rompe | Error 400 controlado | Backend |
| QA-14-005 | Mensaje ambiguo deriva correctamente | Pregunta datos faltantes | Backend AI |
| QA-14-006 | Error de disponibilidad muestra mensaje claro | Sin error tecnico crudo | Backend |
| QA-14-007 | Error DB no expone stacktrace al usuario | Error generico | Backend |
| QA-14-008 | Refresh de navegador mantiene sesion | Sesion persistente (si aplica) | Frontend |
| QA-14-009 | Logout invalida sesion | Token inutilizable | Backend |
| QA-14-010 | Datos sensibles no aparecen en logs | Sin password, tokens en log | Backend |

---

## Resumen por Nivel

| Nivel | Nombre | Total Casos | Implementado | BLOCKED |
|-------|--------|:-----------:|:------------:|:-------:|
| 1 | Smoke Tests basicos | 7 | 7 | 0 |
| 2 | Pruebas funcionales basicas | 9 | 9 | 0 |
| 3 | Agenda digital basica | 10 | 10 | 0 |
| 4 | Filtros de Agenda | 10 | 10 | 0 |
| 5 | Disponibilidad horaria | 10 | 0 | 10 |
| 6 | Reservas desde interfaz | 10 | 0 | 10 |
| 7 | Flujo WhatsApp: reservar | 10 | 5 | 5 |
| 8 | Confirmacion publica | 8 | 4 | 4 |
| 9 | Flujo WhatsApp: cancelar | 10 | 5 | 5 |
| 10 | Flujo WhatsApp: reprogramar | 10 | 5 | 5 |
| 11 | Pruebas visuales | 10 | 10 | 0 |
| 12 | Regresion avanzada | 10 | 0 | 10 |
| 13 | WhatsApp real (opcional) | 10 | 0 | 10 |
| 14 | Seguridad y errores | 10 | 0 | 10 |
| **Total** | | **134** | **65** | **69** |

## Leyenda
- **Implementado**: Test automatizado con Playwright listo para ejecutar
- **BLOCKED**: Falta endpoint, datos de prueba, ambiente o dependencia externa
