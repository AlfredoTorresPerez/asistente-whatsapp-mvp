# LAYOUT RULES

## Objetivo

Definir las reglas de composicion visual que toda pantalla React debe respetar para seguir los prototipos del ZIP sin reinterpretaciones libres.

## Regla transversal

- Toda pantalla debe partir desde uno de estos layouts: publico, privado base, tres columnas o configuracion.
- Si una vista necesita una composicion nueva, primero debe documentarse aqui antes de implementarse.

## Layout publico

Basado en `01_autenticacion_inicio_recuperacion_demo_contacto.png`.

### Estructura obligatoria

- Pantalla dividida en dos columnas en escritorio.
- Panel izquierdo azul oscuro con marca, beneficios y bloque de confianza.
- Panel derecho con formulario o contenido dentro de tarjeta blanca.
- Selector de idioma en la parte superior del area clara.
- CTAs principales azules y secundarios con borde.
- Mensajes de soporte y confianza en el panel azul.

### Reglas responsive

- En movil el panel azul no desaparece; se apila arriba del formulario.
- La card blanca conserva radio grande y respiracion lateral.
- Los enlaces secundarios se mantienen visibles y no se esconden dentro de menus.

### Casos de uso

- Login.
- Recuperar contrasena.
- Confirmacion de correo enviado.
- Restablecer contrasena.
- Solicitar demo.
- Contactar ventas.

## Layout privado

Basado en `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png`.

### Estructura obligatoria

- Sidebar izquierdo azul oscuro.
- Logotipo superior dentro del sidebar.
- Navegacion vertical por modulos.
- Bloques inferiores para cuenta actual, ayuda y cierre de sesion.
- Topbar con selector de negocio, rango de fechas, notificaciones y usuario.
- Area principal con fondo gris claro.
- Tarjetas blancas para metricas, formularios, tablas y paneles.
- Modales para confirmaciones y drawers para paneles contextuales.

### Reglas de contenido

- Toda pagina privada debe abrir con jerarquia clara: `PageHeader`, filtros o metricas, luego contenido principal.
- Las tablas deben ir dentro de card blanca y con barra de filtros visible arriba.
- Las acciones globales viven en topbar o `PageHeader`, no dispersas en cualquier borde.

### Reglas responsive

- El sidebar pasa a overlay movil, nunca a top navigation.
- La topbar puede compactarse, pero debe conservar selector de negocio, notificaciones y usuario.
- Las grillas de metricas bajan de varias columnas a una o dos sin romper el lenguaje visual.

## Layout de tres columnas

Basado en `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png`.

### Estructura obligatoria

- Columna izquierda de lista.
- Columna central de detalle.
- Columna derecha de informacion contextual.
- Acciones rapidas visibles en el detalle o panel contextual.
- Paneles laterales para creacion rapida de prospecto, pedido o cita.

### Reglas de comportamiento

- La columna de lista debe mantener busqueda y filtros arriba.
- El detalle central debe permitir leer el historial completo sin perder composer.
- La columna contextual no se reemplaza por un modal si la imagen pide panel persistente.

## Layout de configuracion

Basado en `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png`.

### Estructura obligatoria

- Secciones agrupadas por tarjetas.
- Pestañas o bloques para subsecciones.
- Tablas de usuarios y accesos.
- Tarjetas de seguridad y conexion.
- Estados de integracion visibles con badges o chips.
- Acciones criticas con confirmacion.

### Patrones obligatorios

- Formularios de configuracion dentro de cards, no en fondo plano.
- Crear usuario en modal.
- Editar usuario en panel lateral o card contextual.
- Seguridad combinando cards resumen, modal de contrasena, modal de 2FA y tablas de sesiones o registros.

## Reglas de overlays

### Modal

- Se usa para confirmaciones, formularios cortos y tareas de apoyo.
- Debe centrarse y oscurecer el fondo sin ocultar completamente el contexto.
- Siempre incluye CTA de cancelar y CTA primaria o destructiva.

### Drawer

- Se usa para detalles rapidos, notificaciones, ayuda, formularios contextuales o edicion lateral.
- Debe entrar desde el lado derecho en layout privado salvo documentacion expresa en contrario.
- Mantiene header, contenido scrollable y footer opcional fijo.

### Dropdown

- Se usa para menu de usuario y acciones compactas.
- Debe alinearse con el disparador y mantener borde, radio y sombra del sistema.

## Reglas de estado

- `EmptyState`, `LoadingState` y `ErrorState` siempre viven dentro del mismo contenedor visual donde apareceria el contenido real.
- `OfflineBanner` se muestra arriba del contenido privado o dentro del panel afectado, sin romper el layout base.
- `Toast` no reemplaza confirmaciones destructivas; solo acompana resultados de acciones ya ejecutadas.

## Reglas de espaciado

- Padding de pagina privada: `24px` a `32px`.
- Padding de card: `20px` a `24px`.
- Gap entre cards: `16px` a `24px`.
- Ninguna vista debe pegar cards al borde del viewport.

## Criterio de rechazo

Una pantalla debe rechazarse si incurre en cualquiera de estos casos:

- reemplaza sidebar por top navigation;
- elimina topbar en vista privada sin justificacion contractual;
- usa fondo plano sin cards blancas donde el prototipo usa superficies elevadas;
- mueve formularios publicos fuera de la composicion de dos columnas;
- usa tablas desnudas sin barra de filtros ni contenedor visual;
- convierte un flujo de panel lateral en pantalla independiente sin actualizar el contrato.
