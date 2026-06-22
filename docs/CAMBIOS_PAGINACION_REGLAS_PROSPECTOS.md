# Cambios de paginacion, reglas y prospectos

## Funcionalidades corregidas

### Catalogo de servicios y productos
- La pantalla de catalogo ahora solicita `size=10` al backend.
- La paginacion muestra explicitamente 10 registros por pagina.
- El detalle de servicio permite desactivar un servicio por falta de cobertura usando el boton `Desactivar servicio por falta de cobertura`.

### Listado de pedidos
- La pantalla de pedidos ahora solicita `size=10` al backend.
- El pie de tabla muestra pagina actual y 10 registros por pagina.

### Lista de reglas
- La pantalla de reglas ahora solicita `size=10` al backend.
- La columna `Tipo` muestra valores traducidos al español.
- El selector de tipo de regla tambien muestra etiquetas en español.

### Detalle de reglas
- El formulario de regla muestra las opciones de tipo en español, manteniendo internamente los codigos tecnicos para compatibilidad con backend.

### Prospectos
- La pantalla de prospectos deja de mostrar fichas.
- Ahora muestra una tabla de detalle con columnas: prospecto, contacto, estado, origen, responsable, ultima actualizacion y accion.
- La paginacion queda configurada en 10 registros por pagina.
