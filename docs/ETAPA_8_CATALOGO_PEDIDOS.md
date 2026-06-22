# ETAPA 8 - Catalogo y pedidos

## Alcance implementado

Se agrega implementacion operativa para catalogo de productos y pedidos, manteniendo la arquitectura modular existente del monolito Spring Boot y la interfaz React.

## Catalogo

Endpoints disponibles:

- `GET /api/catalog/products`
- `POST /api/catalog/products`
- `GET /api/catalog/products/{id}`
- `PUT /api/catalog/products/{id}`
- `PATCH /api/catalog/products/{id}/status`
- `GET /api/catalog/categories`
- `POST /api/catalog/categories`

Tambien quedan disponibles bajo `/api/v1` para compatibilidad con el cliente web.

Funciones:

- listar productos reales desde `product_service` con `type = PRODUCT`;
- crear producto;
- editar producto;
- activar o desactivar producto;
- crear categoria simple;
- controlar stock y stock minimo.

## Pedidos

Endpoints disponibles:

- `GET /api/orders`
- `POST /api/orders`
- `GET /api/orders/{id}`
- `PUT /api/orders/{id}`
- `PATCH /api/orders/{id}/status`
- `POST /api/orders/{id}/items`
- `POST /api/orders/{id}/payment`
- `POST /api/orders/{id}/send-summary`
- `POST /api/orders/from-conversation/{conversationId}`
- `POST /api/orders/from-prospect/{prospectId}`

Tambien quedan disponibles bajo `/api/v1`.

Funciones:

- listar pedidos;
- crear pedido manual;
- crear pedido desde conversacion;
- crear pedido desde prospecto;
- ver detalle;
- editar datos principales;
- agregar productos;
- calcular subtotal, descuento, total, pagado y saldo;
- cambiar estado;
- registrar pago parcial o total;
- generar vista previa simple de comprobante;
- enviar resumen por WhatsApp experimental mediante el canal de mensajeria configurado.

## Estados

Estados de pedido soportados:

- `DRAFT`
- `CONFIRMED`
- `PREPARING`
- `READY`
- `DELIVERED`
- `CANCELLED`

Estados de pago soportados:

- `PENDING`
- `PAID`
- `PARTIAL`

## Migracion

Archivo agregado:

- `backend-java/src/main/resources/db/migration/V9__stage8_catalog_orders.sql`

La migracion:

- agrega stock, stock minimo, proveedor y vencimiento a productos;
- permite pedidos sin prospecto obligatorio;
- ajusta estados de pedido y pago;
- agrega indices;
- carga categorias y productos iniciales para centro estetico.

## Interfaz

Pantallas conectadas:

- `/catalog`
- `/catalog/products/new`
- `/catalog/products/{productId}/edit`
- `/orders`
- `/orders/new`
- `/orders/{orderId}`
- `/orders/{orderId}/payments/new`
- `/conversations/{conversationId}/orders/new`
- `/prospects/{prospectId}/orders/new`

## Prueba local sugerida

Para que Flyway ejecute la migracion nueva en desarrollo local desde una base limpia:

```powershell
docker compose down -v
docker compose build --no-cache
docker compose up
```

Si no deseas borrar la base local:

```powershell
docker compose down
docker compose build --no-cache
docker compose up
```

