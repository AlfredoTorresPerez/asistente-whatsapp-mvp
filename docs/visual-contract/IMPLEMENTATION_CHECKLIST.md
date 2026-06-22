# IMPLEMENTATION CHECKLIST

## Objetivo

Checklist obligatorio antes de aceptar cualquier pantalla nueva o corregida del frontend.

## Checklist visual

- [ ] La pantalla usa el layout correcto segun su imagen de referencia.
- [ ] Respeta `Sidebar` y `Topbar` si es privada.
- [ ] Respeta el layout publico si pertenece a autenticacion, demo o contacto comercial.
- [ ] Usa los colores del contrato visual.
- [ ] Usa tarjetas blancas con bordes redondeados.
- [ ] Usa sombras suaves y consistentes.
- [ ] Usa botones consistentes con el sistema.
- [ ] Usa campos consistentes con el sistema.
- [ ] Usa `StatusBadge` cuando el prototipo muestre estados o chips.
- [ ] Usa estados vacio, carga y error cuando aplique.
- [ ] Usa `OfflineBanner` cuando aplique perdida de conectividad.
- [ ] Usa modal para acciones criticas.
- [ ] Usa panel lateral para detalles o creacion rapida cuando el prototipo lo pida.
- [ ] No tiene estilos inline innecesarios.
- [ ] No se ve como una pantalla generica distinta al prototipo.

## Checklist tecnico

- [ ] Compila con `pnpm build`.
- [ ] No rompe rutas.
- [ ] No rompe consumo API.
- [ ] No duplica componentes base.
- [ ] Usa componentes compartidos del sistema visual.
- [ ] No introduce dependencias innecesarias.
- [ ] No modifica backend si no corresponde.
- [ ] No avanza a otra etapa funcional.

## Checklist documental

- [ ] Se leyo `VISUAL_CONTRACT.md`.
- [ ] Se leyo `SCREEN_IMAGE_MAPPING.md`.
- [ ] Se leyo `DESIGN_TOKENS.md`.
- [ ] Se leyo `COMPONENT_STYLE_GUIDE.md`.
- [ ] Se leyo `LAYOUT_RULES.md`.
- [ ] Se actualizo el estado de implementacion en `SCREEN_IMAGE_MAPPING.md` si la pantalla cambio.

## Criterio de aprobacion

Una pantalla solo puede darse por aceptada cuando el checklist visual y el tecnico quedan completos y su referencia de `SCREEN_IMAGE_MAPPING.md` coincide con lo implementado.
