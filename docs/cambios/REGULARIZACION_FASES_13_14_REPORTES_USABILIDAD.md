# Regularizacion Fases 13 y 14 - Reportes y usabilidad

Fecha: 03-08-2026

## 1. Resumen ejecutivo

Se regularizo el modulo de Reportes para usar metricas persistidas, filtros reales, permisos de exportacion, zona horaria de la empresa y formato chileno. Tambien se aplicaron ajustes transversales de consistencia visual: estados traducidos, formatos centralizados, tablas con mejor ajuste responsivo y foco visible global.

## 2. Matriz de problemas corregidos

| Modulo | Estado anterior | Problema | Riesgo | Cambio aplicado | Pruebas |
|---|---|---|---|---|---|
| Reportes | KPI basicos | Periodo anterior con cero podia inducir porcentajes enganosos | Decision operativa incorrecta | Variacion `null` y texto "Sin periodo anterior" | `ReportsJdbcRepositoryTest`, `ReportsPage.test.tsx` |
| Reportes | Indicadores limitados | Faltaban metricas operativas de ocupacion, ingresos, conversion, clientes y agenda | Reporte incompleto | 15 KPI operativos y ocupacion por profesional, cabina y sucursal | API real y pruebas unitarias |
| Reportes | Exportacion local en UI | CSV no garantizaba permisos ni filtros servidor | Exposicion de datos | Endpoint servidor `/api/v1/reports/summary.csv` con permiso `REPORTS_EXPORT` | HTTP real CSV 200 |
| Reportes | Fechas por cliente | Periodo no estaba centralizado en respuesta | Inconsistencia temporal | `ReportsPeriodResponse` con zona horaria | API real |
| Base de datos | Indices parciales | Faltaban indices de telefono y recordatorios pendientes | Consultas degradadas con volumen | `V108` y `V109` no destructivas | Flyway v109 y `EXPLAIN ANALYZE` |
| Interfaz | Formatos dispersos | Fechas, moneda, porcentajes y telefonos sin helper comun | Inconsistencia visual | `formatters.ts` compartido | Build y Vitest |
| Interfaz | Accesibilidad parcial | Foco dependia de cada componente | Navegacion por teclado pobre | Foco visible global | Capturas escritorio/movil |
| Pruebas | Dependencia de URL local/tunel | Tests fallaban segun ambiente | Falsos negativos | Propiedad fija en calendario y mock de API flexible | Suites completas |

## 3. Fases completadas

- Fase 13 Reportes: completada y validada con API real.
- Fase 14 Consistencia visual y usabilidad: completada para el alcance transversal aplicado en Reportes y componentes compartidos.

## 4. Archivos modificados

- Backend reportes: `ReportsController.java`, `ReportsService.java`, `ReportsJdbcRepository.java`, `ReportsKpiItem.java`, `ReportsSummaryResponse.java`.
- Frontend reportes: `ReportsPage.tsx`, `reportsApi.ts`, `types.ts`.
- UI compartida: `formatters.ts`, `statusFormatters.ts`, `DataTableShell.tsx`, `index.css`.
- Pruebas estabilizadas: `CreatePublicBookingPage.test.tsx`, `CalendarIntegrationControllerTest.java`.

## 5. Componentes creados

- `OccupancyPanel` dentro de `ReportsPage.tsx`.
- Evidencias visuales: `docs/cambios/evidencias/fase-13-14-reportes-escritorio.png` y `docs/cambios/evidencias/fase-13-14-reportes-movil.png`.

## 6. Servicios modificados

- `ReportsService`: periodo con zona horaria, nuevos KPI, CSV servidor, validacion de permiso de exportacion.
- `ReportsJdbcRepository`: consultas de metricas operativas, ocupacion, servicios solicitados y variaciones sin porcentaje enganoso.

## 7. Entidades modificadas

No se modificaron entidades JPA. Se agregaron DTO de respuesta:

- `ReportsPeriodResponse`
- `ReportsOccupancyResponse`
- `ReportsServiceDemandResponse`

## 8. Migraciones agregadas

- `V108__reports_export_permission_and_indexes.sql`: permiso `REPORTS_EXPORT` e indices de reportes.
- `V109__reports_phone_and_pending_reminder_indexes.sql`: indices para cliente por telefono normalizado y recordatorios pendientes.

Flyway local: versiones 108 y 109 aplicadas correctamente.

## 9. Puntos de acceso modificados

- `GET /api/v1/reports/summary`: respuesta ampliada con periodo, KPI operativos, ocupacion y servicios.
- `GET /api/v1/reports/summary.csv`: exportacion CSV con filtros, permisos y formato de fechas local.

## 10. Pruebas agregadas

- `ReportsJdbcRepositoryTest`
- `ReportsPage.test.tsx`

Tambien se corrigieron pruebas existentes para no depender del tunel publico ni de una base absoluta distinta en Vitest.

## 11. Resultado de compilacion

- Backend: `mvn -q -DskipTests compile` correcto.
- Frontend: `pnpm build` correcto.

## 12. Resultado de pruebas

- Backend completo: `mvn -q test` correcto.
- Frontend completo: `pnpm test` correcto, 20 archivos y 182 pruebas.
- Pruebas especificas: `ReportsJdbcRepositoryTest`, `ReportsPage.test.tsx`, `CreatePublicBookingPage.test.tsx` y `CalendarIntegrationControllerTest` correctas.

## 13. Riesgos pendientes

- En la base local hay poco volumen; `EXPLAIN ANALYZE` eligio lectura secuencial por costo, aunque los indices existen. Con datos reales debe repetirse el analisis de planes.
- La integracion Meta local sigue mostrando una advertencia de permisos del proveedor externo; no bloquea Reportes ni esta fase.

## 14. Decisiones tecnicas

- La exportacion CSV se movio al servidor para aplicar permisos y filtros antes de generar datos.
- Las variaciones con periodo anterior en cero no muestran porcentaje calculado.
- `V109` se creo aparte porque `V108` ya estaba aplicada; editarla habria roto el checksum de Flyway.
- Los indicadores usan fechas de atencion para metricas operativas de citas y fecha de creacion cuando el indicador mide captacion.

## 15. Reglas de negocio asumidas

- Solo `OWNER`, `ADMIN` o usuarios con `REPORTS_EXPORT` pueden exportar CSV.
- Las citas canceladas no cuentan como horas reservadas ni como retencion operativa.
- Ocupacion se calcula como minutos reservados sobre minutos disponibles.

## 16. Funciones no implementadas y motivo

- No se agrego programacion automatica de reportes; se considera funcion futura y no debe mostrarse hasta tener flujo servidor.
- No se implementaron graficos avanzados nuevos; la fase priorizo coherencia funcional, permisos, exportacion y datos reales.

## 17. Evidencias

- API resumen real: periodo 05-07-2026 a 03-08-2026, zona horaria America/Santiago, 6 KPI principales, 15 KPI operativos y ocupacion por profesional/cabina/sucursal.
- CSV real: HTTP 200, 2867 bytes.
- `EXPLAIN ANALYZE`: consultas representativas de citas, clientes y recordatorios bajo 1 ms de ejecucion en datos locales.
- Capturas: `docs/cambios/evidencias/fase-13-14-reportes-escritorio.png` y `docs/cambios/evidencias/fase-13-14-reportes-movil.png`.
