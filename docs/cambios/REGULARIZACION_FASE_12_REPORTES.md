# Regularizacion Fase 12 - Reportes

Fecha: 2026-08-03

## Alcance aplicado

Esta fase alinea reportes con estados traducidos, filtros operativos y exportacion CSV sin exponer textos tecnicos al usuario final.

## Cambios implementados

### Interfaz de reportes

- Se renombro la pantalla a "Reportes" para evitar una lectura limitada del modulo.
- Se reemplazo el mensaje de carga tecnico por un texto operativo.
- Los canales se muestran traducidos:
  - WhatsApp.
  - Sitio publico.
  - Ingreso manual.
  - Correo electronico.
- La exportacion CSV usa etiquetas visibles en espanol para etapas y estados.
- La exportacion CSV escapa valores para evitar columnas rotas por comas o caracteres especiales.
- Se mantuvieron filtros por fecha, sucursal, profesional, servicio y estado de cita.

Archivo:

- `frontend-react/src/modules/reports/pages/ReportsPage.tsx`

### Backend de reportes

- Se corrigio el conteo de prospectos para reutilizar los parametros de filtros ya aplicados.
- Se elimino una referencia interna de estado en ayuda de KPI.
- Se mantiene el aislamiento por empresa y filtros por sucursal, profesional, servicio, estado y responsable.

Archivo:

- `backend-java/src/main/java/com/asistentewhatsapp/reports/infrastructure/ReportsJdbcRepository.java`

## Validaciones ejecutadas

- `mvn -q -DskipTests compile`
- `pnpm build`

Resultado: compilacion backend correcta y compilacion frontend correcta.

## Pendientes controlados

- No existen pruebas focalizadas de reportes en el frontend actual.
- La programacion automatica de envios de reportes requiere cola/plantilla y politica de destinatarios; no se implemento sin contrato existente.
