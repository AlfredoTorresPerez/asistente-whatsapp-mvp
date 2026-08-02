# UI_COMPONENTS

## Principios

- Todos los componentes deben ser reutilizables, accesibles y controlables por props.
- La app debe usar una libreria interna de componentes antes de crear variantes ad hoc.
- El estilo debe centralizarse con variables CSS y utilidades de Tailwind.
- Los componentes visibles por defecto deben soportar estados `default`, `loading`, `disabled`, `error` cuando aplique.
- La direccion visual base de Fase 1 usa sidebar azul oscuro, barra superior clara, cards blancas, tablas con filtros, modales centrados y drawers laterales.
- Toda accion critica debe pasar por `ConfirmDialog` antes de persistir cambios destructivos o sensibles.

## Componentes reutilizables

| Componente | Responsabilidad | Props minimas | Variantes o estados | Reglas |
| --- | --- | --- | --- | --- |
| `AppLayout` | Shell privada con sidebar, topbar, banner offline y area de contenido. | `title`, `children`, `breadcrumbs?`, `actions?` | `default`, `withDrawer`, `withFullWidthContent` | Solo se usa en rutas autenticadas. |
| `PublicLayout` | Shell publica para login y recuperacion. | `title`, `subtitle?`, `children` | `centered`, `split` | No renderiza sidebar. |
| `Sidebar` | Navegacion principal autenticada. | `items`, `currentPath`, `collapsed`, `onToggle` | `desktop`, `mobile`, `collapsed` | Debe destacar ruta activa y permisos por rol. |
| `Topbar` | Controles superiores: breadcrumbs, buscador contextual, notificaciones y menu de usuario. | `title`, `actions?`, `notificationCount`, `user` | `default`, `compact` | Debe incluir disparadores para `UserMenu` y `Notifications`. |
| `Button` | Accion primaria o secundaria. | `label`, `onClick`, `type?` | `primary`, `secondary`, `ghost`, `danger`, `link`; `sm`, `md`, `lg`; `loading`, `disabled` | El estado `loading` bloquea doble submit. |
| `Input` | Campo base de texto. | `name`, `label`, `value?`, `onChange?` | `text`, `email`, `password`, `tel`, `search`, `number` | Soporta `helperText`, `errorMessage`, `prefix`, `suffix`. |
| `Select` | Seleccion simple de opciones. | `name`, `label`, `options`, `value?`, `onChange?` | `single`, `searchable` | No se requiere multi-select en Fase 1. |
| `Modal` | Dialogo centrado para flujos cortos o confirmaciones. | `open`, `title`, `children`, `onClose` | `sm`, `md`, `lg` | Cierre con `Esc` y foco atrapado. |
| `Drawer` | Panel lateral para acciones contextuales. | `open`, `title`, `children`, `side`, `onClose` | `right`, `bottom` | Preferido para acciones desde conversaciones en mobile. |
| `DataTable` | Listado tabular paginado con filtros y acciones por fila. | `columns`, `rows`, `rowKey`, `loading`, `pagination` | `default`, `compact`, `withSelection` | La navegacion principal debe ocurrir al clic en fila. |
| `StatusBadge` | Etiqueta visual de estado. | `label`, `tone` | `success`, `warning`, `danger`, `neutral`, `info` | Debe mapear directamente a enums de backend. |
| `EmptyState` | Estado sin datos de una vista. | `title`, `description`, `primaryAction?`, `secondaryAction?` | `page`, `card`, `table` | Debe sugerir siguiente paso claro. |
| `LoadingState` | Placeholder o skeleton de espera. | `variant`, `message?` | `page`, `card`, `table`, `detail` | Debe conservar estructura visual de la vista final. |
| `ErrorState` | Estado de error recuperable. | `title`, `description`, `onRetry?` | `page`, `card`, `inline` | Mostrar lenguaje accionable, no trazas tecnicas. |
| `OfflineBanner` | Aviso global de conectividad perdida. | `visible`, `message?` | `warning`, `error` | Siempre visible arriba del contenido privado. |
| `ConfirmDialog` | Confirmacion previa a accion sensible. | `open`, `title`, `description`, `confirmLabel`, `cancelLabel`, `onConfirm`, `onCancel` | `danger`, `neutral` | Obligatorio en cierres de sesion, eliminaciones y desconexion del canal WhatsApp. |
| `Toast` | Feedback efimero de exito, alerta o error. | `title`, `description?`, `tone` | `success`, `warning`, `error`, `info` | Debe autocerrarse y permitir cierre manual. |

## Tokens base

### Colores semanticos

- `--color-primary`: accion principal.
- `--color-sidebar`: fondo del sidebar azul oscuro.
- `--color-topbar`: superficie clara de barra superior.
- `--color-success`: guardado, pagado, conectado.
- `--color-warning`: pendiente, aviso de sesion, reintentos.
- `--color-danger`: errores y acciones destructivas.
- `--color-surface`: fondos de cards y modales.
- `--color-border`: bordes y separadores.

### Lenguaje visual

- Sidebar: fondo azul oscuro, tipografia clara, item activo con alto contraste.
- Topbar: fondo claro, breadcrumbs, acciones globales y accesos a notificaciones y usuario.
- Cards: superficie blanca con borde suave y sombra baja.
- Filtros: contenedor horizontal o vertical segun viewport, siempre encima del listado.
- Tablas: lectura rapida, hover claro, clic en fila para abrir detalle.
- Modales: confirmaciones y formularios cortos.
- Drawers: acciones contextuales, especialmente desde conversaciones.

### Tipografia

- Titulos: peso 600 a 700.
- Texto de tabla y formularios: peso 400 a 500.
- No usar fuentes por defecto del navegador como decision final; definir una familia propia del sistema visual en la etapa de bootstrap.

## Patrones de composicion

### Formularios

- `Input` y `Select` siempre reciben estado de error desde React Hook Form.
- Las acciones `Guardar` y `Cancelar` van alineadas al final del formulario.
- `Guardar` usa `Button` variante `primary`.
- `Cancelar` usa `Button` variante `ghost` o `secondary`.

### Listados

- Toda vista de listado combina:
  - bloque de filtros;
  - `DataTable`;
  - `EmptyState`, `LoadingState` o `ErrorState` segun corresponda.

### Detalles

- Toda vista de detalle combina:
  - encabezado con acciones;
  - resumen de entidad;
  - secciones secundarias en cards;
  - historial o relaciones si aplica.

## Reglas de accesibilidad

- Todo boton icon-only necesita `aria-label`.
- Todo modal y drawer debe devolver foco al disparador al cerrar.
- Todo campo obligatorio debe anunciarse a lectores de pantalla.
- Los badges no deben depender solo del color; deben mostrar texto.
