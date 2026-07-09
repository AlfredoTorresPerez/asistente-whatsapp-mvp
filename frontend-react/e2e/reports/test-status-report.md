# Status de Pruebas Automatizadas — Asistente de Negocios WhatsApp

**Fecha de ejecución:** 2026-07-07T01:06:51.495Z
**Frontend URL:** http://localhost:5173
**Backend URL:** http://localhost:8080
**Fuente de casuísticas:** agenda_digital_whatsapp_casuisticas.xlsx

## Resumen ejecutivo

| Total | Passed | Failed | Skipped | Blocked |
|-------|-------:|-------:|--------:|--------:|
| 7 | 0 | 0 | 0 | 7 |

## Detalle de casos

| ID | Estado | Evidencia | Error | Recomendación |
|----|--------|----------|-------|---------------|
| POST-RES-001 | BLOCKED | - | No hay conexión segura a base de datos de pruebas. Se requiere configuración de testcontainers o DB de test con credenciales read-only. | - |
| POST-RES-003 | BLOCKED | - | Requiere verificar índice único parcial uq_booking_customer_professional_active en DB | - |
| POST-RES-004 | BLOCKED | - | Requiere verificar exclusión constraint ex_booking_professional_no_overlap_active | - |
| TST-034 | BLOCKED | - | Requiere verificación de exclusión constraint + manejo de error concurrente en API | - |
| TST-112 | BLOCKED | - | Requiere verificar tabla booking_status_history tras cambio de estado | - |
| TST-034 | BLOCKED | - | Prueba de concurrencia requiere: 1) Token de autenticación real, 2) Endpoint de creación de reserva, 3) Ejecución de 2 requests paralelos al mismo slot. Sin endpoint de simulación de autenticación en tests, esta prueba queda bloqueada. | Crear endpoint de test que permita bypass de auth en ambiente de pruebas |
| TST-034-2 | BLOCKED | - | Requiere validar que lectura de disponibilidad no muestre slot como libre si hay transacción concurrente | - |

## Recomendaciones
- **Alta**: Implementar endpoint de simulación WhatsApp para desbloquear pruebas TST-001 a TST-056
- **Alta**: Crear seed QA_AUTO_ en backend para datos de prueba aislados
- **Media**: Configurar testcontainers para pruebas de base de datos
- **Media**: Agregar endpoint de concurrencia controlada para prueba TST-034
