# Correccion paginacion 10 registros

Se corrigio la paginacion solicitada para mostrar 10 registros por pagina en:

- Catalogo de servicios y productos.
- Listado de pedidos.
- Lista de reglas.
- Prospectos.

## Cambios de frontend

- `frontend-react/src/modules/catalog/pages/CatalogPage.tsx`
  - `PAGE_SIZE = 10`.
  - Servicios consultan `size: PAGE_SIZE`.
  - Productos consultan `size: PAGE_SIZE`.
  - Controles visibles: Anterior / Siguiente.

- `frontend-react/src/modules/orders/pages/OrdersPage.tsx`
  - `PAGE_SIZE = 10`.
  - Pedidos consultan `size: PAGE_SIZE`.
  - Controles visibles: Anterior / Siguiente.

- `frontend-react/src/modules/rules/pages/AutomationRulesPage.tsx`
  - `PAGE_SIZE = 10`.
  - Reglas consultan `size: PAGE_SIZE`.
  - Tipo de regla visible en espanol.

- `frontend-react/src/modules/leads/pages/ProspectsPage.tsx`
  - `PAGE_SIZE = 10`.
  - Vista tipo tabla.
  - Se elimina visualizacion tipo ficha.
  - Controles visibles: Anterior / Siguiente.

## Cambios de backend

Se cambiaron los `defaultValue` de `size` a `10` para que tambien quede forzado desde el servidor cuando el frontend no envie parametro.

- `AestheticCenterController.java`
- `CatalogController.java`
- `OrderController.java`
- `LeadController.java`

## Prueba recomendada

Ejecutar:

```powershell
docker compose down
docker compose build --no-cache
docker compose up
```

Luego validar:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/esthetic/services?page=0&size=10" -Method Get
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/esthetic/rules?page=0&size=10" -Method Get
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/catalog/products?page=0&size=10" -Method Get
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/orders?page=0&size=10" -Method Get
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/leads?page=0&size=10" -Method Get
```
