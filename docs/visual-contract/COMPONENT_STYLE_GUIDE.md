# COMPONENT STYLE GUIDE

## Objetivo

Definir el sistema de componentes reutilizables que debe materializar el contrato visual del ZIP. Ninguna pantalla debe inventar estilos base por su cuenta si ya existe un componente del sistema que cubre esa necesidad.

## Reglas globales

- No duplicar estilos en cada pantalla.
- Toda pantalla debe usar estos componentes base.
- Toda tarjeta debe usar el mismo radio, sombra y borde.
- Toda tabla debe usar `DataTable`.
- Todo formulario debe usar `Input`, `Select`, `Textarea` y `Button` del sistema.
- Toda confirmacion debe usar `ConfirmDialog`.
- Todo panel lateral debe usar `Drawer`.
- Todo modal debe usar `Modal`.

## PublicLayout

- Proposito: shell de autenticacion, demo y contacto comercial.
- Imagen de referencia: `01_autenticacion_inicio_recuperacion_demo_contacto.png`.
- Estructura visual: dos columnas en desktop, panel azul oscuro con marca y beneficios a la izquierda, superficie clara con card blanca a la derecha.
- Variantes: login, forgot, reset, request demo, contact sales.
- Clases Tailwind recomendadas: `min-h-screen bg-[#F7F8FC] lg:grid lg:grid-cols-[minmax(360px,460px)_1fr]`.
- Comportamiento esperado: en movil apila hero y formulario sin perder el panel azul.
- Ejemplo de uso: `PublicLayout > HeroPanel + AuthCard`.

## PrivateLayout

- Proposito: shell privado transversal para dashboard, perfil, seguridad, administracion y modulos operativos.
- Imagen de referencia: `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png`.
- Estructura visual: sidebar izquierdo fijo, topbar superior, contenido sobre fondo claro con cards blancas.
- Variantes: base privada, configuracion, detalle con drawer, panel de ayuda.
- Clases Tailwind recomendadas: `min-h-screen bg-[#F5F7FB] lg:grid lg:grid-cols-[184px_1fr]`.
- Comportamiento esperado: sidebar colapsable en movil con overlay, topbar siempre visible.
- Ejemplo de uso: `PrivateLayout > Sidebar + Topbar + PageContent`.

## Sidebar

- Proposito: navegacion vertical principal.
- Imagen de referencia: `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png`.
- Estructura visual: bloque azul oscuro, logo arriba, links apilados, cuenta actual, uso del plan, ayuda y cierre al pie.
- Variantes: escritorio fijo, sheet movil.
- Clases Tailwind recomendadas: `bg-[linear-gradient(180deg,#081A3A_0%,#0E2C63_100%)] text-white rounded-r-[24px]`.
- Comportamiento esperado: item activo destacado, secciones inferiores persistentes.
- Ejemplo de uso: `Sidebar items={navigationItems}`.

## Topbar

- Proposito: controles globales del area privada.
- Imagen de referencia: `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png`.
- Estructura visual: selector de negocio, rango de fechas, campana, avatar y nombre.
- Variantes: con busqueda opcional, con tabs contextuales debajo.
- Clases Tailwind recomendadas: `min-h-[72px] rounded-[24px] border border-[#E6EBF5] bg-white px-5 xl:px-6`.
- Comportamiento esperado: notificaciones y usuario abren overlays propios.
- Ejemplo de uso: `Topbar businessSwitcher dateRange notificationButton userMenu`.

## PageHeader

- Proposito: titulo, descripcion y acciones de pagina.
- Imagen de referencia: `02` y `10`.
- Estructura visual: titulo fuerte arriba, subtitulo corto y CTA alineadas.
- Variantes: con breadcrumbs, con tabs, con acciones multiples.
- Clases Tailwind recomendadas: `flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between`.
- Comportamiento esperado: acciones bajan en movil sin perder jerarquia.
- Ejemplo de uso: `PageHeader title="Mi perfil" description="Gestiona tu informacion personal."`.

## Card

- Proposito: contenedor base de modulos, formularios y paneles.
- Imagen de referencia: todas las laminas del ZIP.
- Estructura visual: fondo blanco, borde tenue, radio grande, sombra suave.
- Variantes: default, compact, dashed empty, danger, success.
- Clases Tailwind recomendadas: `rounded-[24px] border border-[#E6EBF5] bg-white shadow-[0_18px_50px_rgba(15,23,42,0.08)]`.
- Comportamiento esperado: padding coherente con densidad de contenido.
- Ejemplo de uso: `Card className="p-6"`.

## Button

- Proposito: accion primaria, secundaria o destructiva.
- Imagen de referencia: `01`, `02`, `10`.
- Estructura visual: radio suave, altura consistente, peso semibold.
- Variantes: primary blue, primary green, secondary outline, destructive, ghost.
- Clases Tailwind recomendadas: ver `DESIGN_TOKENS.md`.
- Comportamiento esperado: loading, disabled, icon left/right.
- Ejemplo de uso: `Button variant="primaryBlue">Guardar</Button>`.

## Input

- Proposito: captura de texto corto.
- Imagen de referencia: `01`, `10`.
- Estructura visual: fondo blanco, borde suave, icono opcional, label arriba.
- Variantes: text, email, password, phone, search.
- Clases Tailwind recomendadas: `h-11 rounded-xl border border-[#E6EBF5] bg-white px-3 text-sm`.
- Comportamiento esperado: focus visible, mensaje de error abajo.
- Ejemplo de uso: `Input label="Correo electronico" type="email"`.

## Select

- Proposito: seleccion controlada.
- Imagen de referencia: `01`, `02`, `10`.
- Estructura visual: caja igual a `Input`, chevron discreto, label opcional.
- Variantes: simple, grouped, status filter.
- Clases Tailwind recomendadas: `h-11 rounded-xl border border-[#E6EBF5] bg-white px-3 text-sm`.
- Comportamiento esperado: placeholder y estado disabled claros.
- Ejemplo de uso: `Select label="Rol" options={roles}`.

## Textarea

- Proposito: texto largo en contacto, notas y descripcion.
- Imagen de referencia: `01` y modales del sistema.
- Estructura visual: mismo lenguaje del input con altura mayor.
- Variantes: fixed, autosize.
- Clases Tailwind recomendadas: `min-h-[112px] rounded-xl border border-[#E6EBF5] bg-white px-3 py-3 text-sm`.
- Comportamiento esperado: contador opcional si el prototipo lo pide.
- Ejemplo de uso: `Textarea label="Mensaje"`.

## Modal

- Proposito: confirmaciones, formularios cortos y tareas de apoyo.
- Imagen de referencia: `02` y `10`.
- Estructura visual: card blanca centrada con backdrop oscuro tenue.
- Variantes: regular, compact, destructive, form.
- Clases Tailwind recomendadas: `rounded-[24px] border border-[#E6EBF5] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.18)]`.
- Comportamiento esperado: cierre por boton visible, Escape y backdrop cuando no sea destructivo irreversible.
- Ejemplo de uso: `Modal title="Cerrar sesion" open={isOpen}`.

## Drawer

- Proposito: crear o editar sin abandonar el contexto actual.
- Imagen de referencia: `02`, `03`, `10`.
- Estructura visual: panel lateral blanco con header, body scrollable y footer fijo opcional.
- Variantes: right drawer, contextual info, quick create.
- Clases Tailwind recomendadas: `h-full w-full max-w-[440px] border-l border-[#E6EBF5] bg-white`.
- Comportamiento esperado: foco atrapado y animacion lateral.
- Ejemplo de uso: `Drawer title="Notificaciones" side="right"`.

## ConfirmDialog

- Proposito: confirmar acciones sensibles.
- Imagen de referencia: `02` y `10`.
- Estructura visual: icono, pregunta corta, descripcion, dos CTA.
- Variantes: logout, delete, disconnect, destructive confirmation.
- Clases Tailwind recomendadas: `Modal + footer with secondary and destructive button`.
- Comportamiento esperado: CTA principal destructiva o de confirmacion segun riesgo.
- Ejemplo de uso: `ConfirmDialog variant="danger" confirmLabel="Eliminar"`.

## Toast

- Proposito: feedback no bloqueante de exito, error o informacion.
- Imagen de referencia: sistema de confirmaciones del ZIP.
- Estructura visual: tarjeta chica con icono, titulo corto y descripcion breve.
- Variantes: success, error, warning, info.
- Clases Tailwind recomendadas: `rounded-2xl border border-[#E6EBF5] bg-white px-4 py-3`.
- Comportamiento esperado: auto dismiss y stacking ordenado.
- Ejemplo de uso: `Toast variant="success" title="Guardado"`.

## StatusBadge

- Proposito: representar estado de entidad o integracion.
- Imagen de referencia: `02`, `05`, `07`, `10`.
- Estructura visual: chip redondeado con color de fondo suave y texto semibold.
- Variantes: success, warning, error, info, neutral, experimental.
- Clases Tailwind recomendadas: `inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium`.
- Comportamiento esperado: color consistente segun semantica.
- Ejemplo de uso: `StatusBadge tone="success">Activo</StatusBadge>`.

## DataTable

- Proposito: listados principales del sistema.
- Imagen de referencia: `05`, `07`, `09`, `10`.
- Estructura visual: header de tabla claro, filas aireadas, hover sutil, paginacion abajo.
- Variantes: simple, selectable, sortable.
- Clases Tailwind recomendadas: `w-full text-left text-[13px] text-[#102247]`.
- Comportamiento esperado: filtros arriba, estado vacio debajo del header, row click cuando aplique.
- Ejemplo de uso: `DataTable columns={columns} rows={rows}`.

## FilterBar

- Proposito: concentrar busqueda, filtros y CTA de listado.
- Imagen de referencia: `05`, `07`, `09`, `10`.
- Estructura visual: linea o card superior con search, selects y botones.
- Variantes: compacta, wrap en movil.
- Clases Tailwind recomendadas: `flex flex-col gap-3 lg:flex-row lg:items-center`.
- Comportamiento esperado: no esconder filtros principales detras de menus innecesarios.
- Ejemplo de uso: `FilterBar leftSlot={<SearchInput />} rightSlot={<Button />}`.

## SearchInput

- Proposito: filtro textual rapido.
- Imagen de referencia: `02`, `05`, `10`.
- Estructura visual: input con icono de lupa y placeholder descriptivo.
- Variantes: inline, wide.
- Clases Tailwind recomendadas: `h-11 rounded-xl border border-[#E6EBF5] bg-white pl-10 pr-3 text-sm`.
- Comportamiento esperado: debounce de datos, no de tipografia.
- Ejemplo de uso: `SearchInput placeholder="Buscar usuario..."`.

## Tabs

- Proposito: organizar secciones internas sin cambiar el layout global.
- Imagen de referencia: `02`, `10`.
- Estructura visual: tabs lineales con activo azul y linea inferior.
- Variantes: underline, pill suave.
- Clases Tailwind recomendadas: `inline-flex gap-6 border-b border-[#EEF2F8]`.
- Comportamiento esperado: tab activa visible y transicion simple.
- Ejemplo de uso: `Tabs items={profileTabs}`.

## EmptyState

- Proposito: estado vacio con orientacion.
- Imagen de referencia: `05` y `09`.
- Estructura visual: card clara, icono o ilustracion, titulo, descripcion, CTA.
- Variantes: list empty, no search results, setup required.
- Clases Tailwind recomendadas: `rounded-[24px] border border-dashed border-[#D8E1F0] bg-white p-10 text-center`.
- Comportamiento esperado: CTA lleva a la primera accion util.
- Ejemplo de uso: `EmptyState title="Sin prospectos"`.

## LoadingState

- Proposito: carga de pagina o bloque.
- Imagen de referencia: `09`.
- Estructura visual: skeleton o indicador suave dentro de una card.
- Variantes: page, card, table, panel.
- Clases Tailwind recomendadas: `rounded-[24px] border border-[#E6EBF5] bg-white p-10`.
- Comportamiento esperado: evitar saltos bruscos de layout.
- Ejemplo de uso: `LoadingState message="Cargando reportes..."`.

## ErrorState

- Proposito: mostrar error traducido y accionable.
- Imagen de referencia: derivado del sistema de cards del ZIP.
- Estructura visual: card clara con tono rojizo, icono, mensaje y CTA reintentar.
- Variantes: page error, inline error, panel error.
- Clases Tailwind recomendadas: `rounded-[24px] border border-red-100 bg-red-50/60 p-6`.
- Comportamiento esperado: nunca exponer stacktrace.
- Ejemplo de uso: `ErrorState onRetry={reload}`.

## OfflineBanner

- Proposito: avisar perdida de conectividad.
- Imagen de referencia: sistema privado general.
- Estructura visual: banner horizontal warning por encima del contenido.
- Variantes: sticky, inline.
- Clases Tailwind recomendadas: `border border-amber-200 bg-amber-50 text-amber-800`.
- Comportamiento esperado: visible mientras no haya red y sin tapar CTA criticos.
- Ejemplo de uso: `OfflineBanner visible={!navigator.onLine}`.

## MetricCard

- Proposito: KPIs de dashboard y listados.
- Imagen de referencia: `02`, `05`, `07`, `09`.
- Estructura visual: titulo pequeno, valor fuerte, delta y comparacion temporal.
- Variantes: numeric, percent, amount.
- Clases Tailwind recomendadas: `Card + p-5 + gap-2`.
- Comportamiento esperado: no mezclar mas de una metrica principal por card.
- Ejemplo de uso: `MetricCard label="Pedidos" value="86" delta="+18%"`.

## ChartCard

- Proposito: contenedor de chart o dona.
- Imagen de referencia: `02` y `09`.
- Estructura visual: titulo, leyenda simple, grafico centrado o alineado.
- Variantes: donut, line, bar.
- Clases Tailwind recomendadas: `Card + p-6`.
- Comportamiento esperado: mantener espacio blanco generoso.
- Ejemplo de uso: `ChartCard title="Conversaciones por canal"`.

## ActivityList

- Proposito: lista breve de actividad reciente.
- Imagen de referencia: `02`.
- Estructura visual: filas con icono, descripcion, sujeto y hora.
- Variantes: dashboard, auditoria resumida.
- Clases Tailwind recomendadas: `divide-y divide-[#EEF2F8]`.
- Comportamiento esperado: maximo 5 a 7 items en cards compactas.
- Ejemplo de uso: `ActivityList items={activity}`.

## NotificationPanel

- Proposito: centro de notificaciones lateral.
- Imagen de referencia: `02`.
- Estructura visual: drawer derecho con tabs, lista de eventos, CTA ver todo.
- Variantes: all, unread, important.
- Clases Tailwind recomendadas: `Drawer + sticky footer`.
- Comportamiento esperado: abrir desde campana topbar y permitir marcar leidas.
- Ejemplo de uso: `NotificationPanel open={isNotificationsOpen}`.

## UserMenuDropdown

- Proposito: accesos rapidos del usuario autenticado.
- Imagen de referencia: `02`.
- Estructura visual: dropdown blanco con avatar, nombre, rol, links y logout en rojo.
- Variantes: compacta, extendida con ayuda.
- Clases Tailwind recomendadas: `absolute right-0 mt-2 w-[280px] rounded-[24px] border border-[#E6EBF5] bg-white p-3 shadow-[0_18px_50px_rgba(15,23,42,0.12)]`.
- Comportamiento esperado: cerrar al hacer click fuera y mantener jerarquia de opciones.
- Ejemplo de uso: `UserMenuDropdown user={currentUser}`.
