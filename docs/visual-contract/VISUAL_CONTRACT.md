# VISUAL CONTRACT

## Proposito

Este contrato visual congela la referencia estetica oficial del producto para Fase 1. Las imagenes incluidas en `pantallas_visuales_asistente_whatsapp_separadas.zip` son la fuente visual obligatoria para toda implementacion React del proyecto `asistente-whatsapp-mvp`.

Este documento no reemplaza `docs/SCREEN_SPEC.md` ni `docs/API_CONTRACTS.md`. Su funcion es fijar como debe verse cada pantalla, panel, modal, tabla, formulario y estado antes de aceptar cualquier cambio de frontend.

## Fuente visual oficial

Las siguientes laminas son la referencia oficial del producto:

1. `01_autenticacion_inicio_recuperacion_demo_contacto.png`
2. `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png`
3. `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png`
4. `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png`
5. `05_prospectos_lista_crear_detalle_editar_importar_exportar.png`
6. `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png`
7. `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png`
8. `08_catalogo_productos_servicios_categorias_inventario_importar.png`
9. `09_reportes_filtros_detalle_exportar_programar_estados.png`
10. `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png`

## Regla de autoridad

- Si una pantalla o componente tiene imagen de referencia en el ZIP, esa imagen manda sobre cualquier decision estetica no documentada.
- Si existe tension entre funcionalidad y presentacion, la funcionalidad puede ajustarse tecnicamente, pero la estructura visual base no puede romperse sin actualizar este contrato.
- Una pantalla no se considera terminada solo porque funcione. Debe verse alineada con su prototipo.

## Modulos cubiertos por cada imagen

| Imagen | Modulos cubiertos |
| --- | --- |
| `01_autenticacion_inicio_recuperacion_demo_contacto.png` | Login, recuperar contrasena, confirmacion de enlace, reset password, solicitar demo, contactar ventas, selector de idioma, trust panel publico |
| `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png` | Layout privado base, dashboard, centro de notificaciones, menu de usuario, perfil, ayuda y soporte, confirmacion de logout |
| `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png` | Lista de conversaciones, detalle, panel contextual, plantillas, nueva conversacion, crear prospecto, pedido y cita desde conversacion |
| `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png` | Lista de reglas, asistente de creacion, edicion, builder de condiciones, builder de acciones, prueba, historial |
| `05_prospectos_lista_crear_detalle_editar_importar_exportar.png` | Lista de prospectos, crear, detalle, editar, importar, exportar y estado vacio |
| `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png` | Agenda principal, detalle de cita, crear cita, editar, reprogramar, configuracion de disponibilidad |
| `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png` | Lista de pedidos, detalle, crear, editar, pago, despacho, comprobante |
| `08_catalogo_productos_servicios_categorias_inventario_importar.png` | Catalogo, crear, detalle, editar, categorias, inventario, importacion |
| `09_reportes_filtros_detalle_exportar_programar_estados.png` | Reportes, filtros avanzados, detalle, exportar, programar, estado de carga, estado vacio |
| `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png` | Administracion, empresa, usuarios, roles, Canal WhatsApp, integraciones, plan y facturacion, metodos de pago, seguridad, 2FA, sesiones, dispositivos, access log |

## Reglas obligatorias por modulo

- Las pantallas publicas deben seguir el estilo de `01_autenticacion_inicio_recuperacion_demo_contacto.png`.
- Las pantallas privadas deben seguir el estilo de `02_entrada_navegacion_global_dashboard_notificaciones_perfil.png`.
- Conversaciones debe seguir `03_conversaciones_detalle_plantillas_prospecto_pedido_cita.png`.
- Reglas debe seguir `04_reglas_automatizacion_crear_editar_condiciones_acciones_prueba_historial.png`.
- Prospectos debe seguir `05_prospectos_lista_crear_detalle_editar_importar_exportar.png`.
- Agenda debe seguir `06_agenda_reservas_crear_detalle_editar_reprogramar_disponibilidad.png`.
- Pedidos debe seguir `07_pedidos_lista_crear_detalle_editar_pago_despacho_comprobante.png`.
- Catalogo debe seguir `08_catalogo_productos_servicios_categorias_inventario_importar.png`.
- Reportes debe seguir `09_reportes_filtros_detalle_exportar_programar_estados.png`.
- Administracion y seguridad deben seguir `10_administracion_seguridad_usuarios_roles_integraciones_2fa_accesos.png`.

## Gramaticas visuales compartidas detectadas en las imagenes

- Sidebar siempre en azul oscuro con degradado sutil, marca arriba, navegacion vertical, bloque de cuenta actual, uso de plan, ayuda y cierre de sesion.
- Superficie principal clara con fondo gris muy tenue o blanco calido.
- Tarjetas blancas con borde suave, radio alto, sombra liviana y separacion generosa.
- CTA principal azul para acciones primarias generales.
- CTA verde para acciones positivas u operativas de confirmacion.
- CTA rojo para acciones destructivas o riesgosas.
- Topbar con selector de negocio, rango de fechas, campana de notificaciones y avatar de usuario.
- Tablas ligeras con encabezado visual, filtros arriba, badges de estado, accion al final de fila y paginacion inferior.
- Formularios siempre dentro de tarjeta, panel o modal blanco, con inputs de borde suave y espaciado regular.
- Modales y drawers centrados o laterales segun el contexto, nunca como pantallas desnudas sin contenedor.

## Que esta permitido modificar

- Texto de negocio, labels y ejemplos para alinearlos con el dominio real.
- Datos mostrados, bindings, paginacion, estados de carga, error y vacio.
- Ajustes menores de responsive para movil si preservan la composicion.
- Accesibilidad, focus, contraste, validaciones y ayudas de formulario.
- Iconografia concreta si pertenece a la misma familia visual y conserva tamano, peso y tono.
- Espaciados finos o tamanos exactos cuando sea necesario para aproximar mejor la imagen de referencia.

## Que esta prohibido modificar

- No usar disenos minimalistas genericos si existe una referencia visual en las imagenes.
- No cambiar la estructura base del layout publico.
- No cambiar la estructura base del layout privado.
- No reemplazar sidebar por navegacion superior.
- No eliminar topbar.
- No cambiar arbitrariamente los colores principales.
- No crear formularios sin tarjetas blancas.
- No crear tablas sin filtros, encabezado visual y estados.
- No crear acciones criticas sin modal de confirmacion.
- No convertir paneles laterales del prototipo en paginas planas sin justificacion contractual.
- No sustituir el sistema de badges, chips y estados por texto suelto sin color.
- No mezclar temas visuales distintos entre modulos.

## Criterios para aceptar una pantalla como alineada visualmente

Una pantalla solo se acepta como alineada cuando cumple todo lo siguiente:

1. Usa la imagen de referencia correcta del modulo.
2. Respeta la composicion base: columnas, jerarquia, posicion de CTA, presencia de sidebar y topbar cuando corresponda.
3. Respeta la paleta principal: azul marino, azul accion, verde accion, superficie blanca y fondo claro.
4. Usa componentes del sistema visual compartido y no estilos aislados por pantalla.
5. Mantiene el mismo lenguaje de bordes, radios, sombras, filtros, tablas, badges y modales.
6. Incluye estados vacio, carga, error y sin conexion con el mismo tono del sistema.
7. En responsive no elimina estructura critica; la reordena de forma consistente.
8. El resultado final se reconoce inmediatamente como la misma familia visual del prototipo.

## Regla operativa para futuras etapas

Antes de editar cualquier pantalla React se debe leer:

- `docs/visual-contract/VISUAL_CONTRACT.md`
- `docs/visual-contract/SCREEN_IMAGE_MAPPING.md`
- `docs/visual-contract/DESIGN_TOKENS.md`
- `docs/visual-contract/COMPONENT_STYLE_GUIDE.md`
- `docs/visual-contract/LAYOUT_RULES.md`
- `docs/visual-contract/IMPLEMENTATION_CHECKLIST.md`

Sin esa lectura previa, no se debe iniciar implementacion visual ni refactor de pantallas.
