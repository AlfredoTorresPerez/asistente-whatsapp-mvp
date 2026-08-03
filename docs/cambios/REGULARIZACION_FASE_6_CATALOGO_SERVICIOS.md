# Regularizacion Fase 6 - Catalogo y servicios

Fecha: 2026-08-03

## Alcance aplicado

- Se dejo `CatalogPage` como fuente visible unica para administrar servicios esteticos.
- Las rutas directas de productos del catalogo redirigen al listado de servicios para ocultar funcionalidad fuera del alcance actual sin borrar codigo ni tablas con dependencias.
- Se eliminaron referencias visibles a productos, pedidos, stock, servidor, base de datos y datos simulados en las pantallas tocadas.
- Se mantuvo el formato chileno de precios mediante `Intl.NumberFormat('es-CL', { currency: 'CLP' })`.
- En operacion multisede se renombro la administracion visible a `Servicios por sucursal` y se ocultaron columnas de pedidos/productos para reducir desplazamiento horizontal.
- Se mantuvo compatibilidad con el contrato backend existente enviando valores neutros para campos de stock cuando la operacion corresponde a servicios.

## Archivos modificados

- `frontend-react/src/modules/catalog/pages/CatalogPage.tsx`
- `frontend-react/src/modules/catalog/pages/CatalogFormPage.tsx`
- `frontend-react/src/modules/multisite/pages/MultisiteOperationsPage.tsx`
- `frontend-react/src/modules/multisite/pages/MultisiteOperationsPage.test.tsx`
- `frontend-react/src/modules/administration/pages/AdministrationPage.tsx`
- `frontend-react/src/modules/auth/pages/LoginPage.tsx`
- `frontend-react/src/app/router.tsx`
- `frontend-react/src/lib/navigation.ts`
- `frontend-react/src/app/layouts/PublicLayout.tsx`

## Validacion

- Backend: `mvn -q -DskipTests compile`
- Frontend: `pnpm build`
- Pruebas relacionadas: `pnpm test -- --run src/modules/multisite/pages/MultisiteOperationsPage.test.tsx`

## Riesgos residuales

- El modulo de productos y pedidos permanece en el codigo por dependencias existentes. Quedo oculto en navegacion/rutas relacionadas con catalogo, pero no se elimino almacenamiento ni API.
- La configuracion por sucursal reutiliza un contrato backend historico con campos de stock; la UI envia valores neutros para servicios.
