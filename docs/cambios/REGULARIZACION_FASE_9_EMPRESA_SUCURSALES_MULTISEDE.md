# Regularizacion Fase 9 - Empresa, sucursales y operacion multisede

Fecha: 2026-08-03

## Alcance aplicado

Esta fase regulariza la experiencia operativa de empresa, sucursales y multisede sin cambiar de arquitectura ni introducir datos simulados. Los cambios mantienen los contratos actuales y agregan indicadores de lectura donde el backend ya puede calcularlos.

## Cambios implementados

### Empresa

- Se elimino el boton duplicado de guardado en la cabecera de configuracion de empresa.
- Se mantuvo un unico guardado explicito en el formulario.
- Se reemplazaron textos tecnicos o de etapa por textos operativos en espanol.
- Se ajustaron placeholders para no exponer datos reales, locales ni identificadores internos.
- Se mantuvo compatibilidad con zona horaria America/Santiago y moneda CLP.

Archivos:

- `frontend-react/src/modules/administration/pages/CompanySettingsPage.tsx`

### Sucursales

- Se redujo la tabla principal para evitar desplazamiento horizontal excesivo.
- Las acciones de cada sucursal se agruparon en un menu: ver, editar, profesionales, cabinas, QR comercial y desactivar.
- La desactivacion ahora requiere confirmacion y muestra el nombre de la sucursal afectada.
- Se corrigieron textos visibles y placeholders para evitar datos de prueba o referencias tecnicas.
- Se mantiene el uso de las APIs existentes para crear, editar, consultar horarios y desactivar sucursales.

Archivos:

- `frontend-react/src/modules/administration/pages/AdminLocationsPage.tsx`

### Operacion Multisede

- Se agregaron indicadores operativos:
  - Citas del dia.
  - Ocupacion.
  - Profesionales activos.
  - Cabinas disponibles.
  - Cabinas en mantencion.
  - Alertas.
- Se renombro la administracion visible de catalogo a servicios por sucursal cuando la pantalla opera sobre servicios.
- Se mantuvo la edicion de precio especifico por sucursal.
- Se ocultaron campos e indicadores de productos, pedidos y stock en la interfaz operativa.
- La validacion multisede dejo de ser una lista estatica y ahora se ejecuta desde el boton "Ejecutar validacion".
- La validacion muestra fecha de ejecucion, estado, cantidad de registros afectados, evidencia y enlace de correccion.
- Los estados y modos de canal se presentan traducidos.

Archivos:

- `frontend-react/src/modules/multisite/pages/MultisiteOperationsPage.tsx`
- `frontend-react/src/modules/multisite/pages/MultisiteOperationsPage.test.tsx`
- `frontend-react/src/services/api/types.ts`

### Backend Multisede

- El resumen por sucursal ahora entrega datos agregados para la operacion multisede:
  - `bookingsToday`
  - `roomsAvailable`
  - `roomsInMaintenance`
  - `alerts`
- Los indicadores se calculan con datos reales de citas, profesionales, cabinas y estado de sucursal.

Archivos:

- `backend-java/src/main/java/com/asistentewhatsapp/multisite/api/MultisiteLocationSummaryResponse.java`
- `backend-java/src/main/java/com/asistentewhatsapp/multisite/infrastructure/MultisiteJdbcRepository.java`

## Validaciones ejecutadas

- `mvn -q -DskipTests compile`
- `pnpm build`
- `pnpm test -- --run src/modules/multisite/pages/MultisiteOperationsPage.test.tsx`

Resultado: compilacion backend correcta, compilacion frontend correcta y pruebas de multisede correctas.

## Pendientes controlados

- La configuracion de empresa conserva el contrato actual. Campos como RUT, giro, logotipo, politicas y remitente requieren ampliacion no destructiva de API y persistencia.
- Las sucursales conservan el contrato actual. Campos como coordenadas, instrucciones de llegada, estacionamiento, accesibilidad y capacidad de reservas requieren ampliacion no destructiva de modelo.
- La mantencion de cabinas se calcula por estado no operativo mientras no exista un estado estructurado de mantencion independiente.
- La ocupacion multisede se muestra como estimacion operativa usando citas del dia y capacidad base por profesionales activos.
