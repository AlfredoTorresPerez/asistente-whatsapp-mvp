# Catalogo y reglas editables para Centro Estetico

## Correccion implementada

La version anterior cargaba catalogo, productos y reglas en migraciones y exponia rutas de consulta, pero las pantallas `/catalog` y `/automation-rules` seguian usando datos visuales estaticos. Esta correccion reemplaza esas pantallas por vistas conectadas al backend.

## Pantalla Catalogo

Ruta principal:

```text
/catalog
```

Ahora permite:

- ver servicios reales desde `GET /api/v1/esthetic/services`;
- ver productos reales desde `GET /api/v1/esthetic/products`;
- filtrar por busqueda, categoria y estado;
- paginar resultados;
- editar servicios;
- editar productos;
- crear servicios;
- crear productos.

Rutas de edicion y creacion:

```text
/catalog/services/new
/catalog/services/{serviceId}/edit
/catalog/products/new
/catalog/products/{productId}/edit
```

## Pantalla Reglas

Ruta principal:

```text
/automation-rules
```

Ahora permite:

- ver reglas reales desde `GET /api/v1/esthetic/rules`;
- filtrar por tipo y estado;
- editar reglas;
- crear reglas;
- validar formato JSON del payload antes de guardar.

Rutas de edicion y creacion:

```text
/automation-rules/new
/automation-rules/{ruleId}/edit
```

## Backend agregado

Se agregaron endpoints de escritura:

```text
POST /api/v1/esthetic/services
PUT  /api/v1/esthetic/services/{serviceId}
POST /api/v1/esthetic/products
PUT  /api/v1/esthetic/products/{productId}
POST /api/v1/esthetic/rules
PUT  /api/v1/esthetic/rules/{ruleId}
GET  /api/v1/esthetic/rules/{ruleId}
GET  /api/v1/esthetic/service-categories
GET  /api/v1/esthetic/product-categories
```

## Relacion con la IA

La IA usa los servicios, productos y reglas activos como contexto interno para responder. Por eso la edicion del catalogo y reglas afecta directamente las respuestas generadas por el motor inteligente.

## Nota operativa

Si los datos no aparecen luego de levantar esta version, limpiar cache del navegador y reconstruir contenedores:

```bash
docker compose down
docker compose build --no-cache
docker compose up
```

Si la base ya estaba inicializada antes de las migraciones del centro estetico y los registros no existen, reiniciar el volumen de base de datos:

```bash
docker compose down -v
docker compose build --no-cache
docker compose up
```

Este ultimo comando elimina datos locales de PostgreSQL.
