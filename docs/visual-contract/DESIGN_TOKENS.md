# DESIGN TOKENS

## Objetivo

Estos tokens son aproximaciones fieles derivadas de las 10 laminas raster del ZIP. Deben usarse como base en Tailwind CSS para reproducir la familia visual del prototipo y evitar estilos improvisados por pantalla.

## Regla de implementacion

- Cuando un token nativo de Tailwind no alcanza para parecerse al prototipo, se permiten valores arbitrarios.
- Los tokens de este documento son obligatorios para ETAPA 3C y posteriores correcciones visuales.
- Los modulos pueden cambiar contenido y densidad de datos, pero no su sistema de color, radio, sombra y espaciado base.

## Colores

| Token | Valor aproximado | Uso principal | Tailwind recomendado |
| --- | --- | --- | --- |
| Sidebar dark navy | `#081A3A` | sidebar, hero publico, panel lateral oscuro | `bg-[#081A3A] text-white` |
| Sidebar navy gradient end | `#0E2C63` | profundidad del panel azul | `bg-[linear-gradient(180deg,#081A3A_0%,#0E2C63_100%)]` |
| Main surface | `#F5F7FB` | fondo privado | `bg-[#F5F7FB]` |
| Public surface | `#F7F8FC` | fondo claro de autenticacion | `bg-[#F7F8FC]` |
| Card background | `#FFFFFF` | tarjetas, modales, drawers, formularios | `bg-white` |
| Primary blue | `#2453FF` | CTA principal y elementos activos | `bg-[#2453FF] hover:bg-[#1E47DB] text-white` |
| Primary green | `#16A34A` | confirmaciones, guardar positivo, connectado | `bg-[#16A34A] hover:bg-[#14803C] text-white` |
| Primary red | `#EF4444` | acciones destructivas | `bg-[#EF4444] hover:bg-[#DC2626] text-white` |
| Text main | `#102247` | titulos, labels fuertes | `text-[#102247]` |
| Text secondary | `#6B7A99` | subtitulos, ayudas, metadata | `text-[#6B7A99]` |
| Text muted | `#94A3B8` | placeholders, notas leves | `text-slate-400` |
| Border default | `#E6EBF5` | cards, inputs, tablas | `border border-[#E6EBF5]` |
| Divider soft | `#EEF2F8` | separadores internos | `border-[#EEF2F8]` |
| Error | `#EF4444` | badges y feedback de error | `text-red-500 bg-red-50 border-red-200` |
| Warning | `#F59E0B` | estado pendiente o en espera | `text-amber-600 bg-amber-50 border-amber-200` |
| Success | `#22C55E` | activo, entregado, confirmado | `text-emerald-600 bg-emerald-50 border-emerald-200` |
| Info | `#3B82F6` | info, tags neutrales, acciones secundarias | `text-blue-600 bg-blue-50 border-blue-200` |
| Violet accent | `#8B5CF6` | chips secundarios, tags, metrics puntuales | `text-violet-600 bg-violet-50 border-violet-200` |
| Dark overlay | `rgba(15,23,42,0.45)` | backdrop de modal | `bg-slate-950/45 backdrop-blur-[2px]` |

## Espaciado y medidas

| Token | Valor aproximado | Tailwind recomendado |
| --- | --- | --- |
| Page padding desktop | `24px a 32px` | `px-6 py-6 xl:px-8 xl:py-8` |
| Page padding mobile | `16px a 20px` | `px-4 py-4 sm:px-5 sm:py-5` |
| Card padding | `24px` | `p-6` |
| Card padding compacta | `20px` | `p-5` |
| Gap entre cards | `16px a 24px` | `gap-4 xl:gap-6` |
| Topbar height | `72px` | `min-h-[72px]` |
| Sidebar width | `176px a 192px` | `w-[184px]` |
| Sidebar width mobile sheet | `288px a 320px` | `w-full max-w-[320px]` |
| Drawer width | `400px a 440px` | `w-full max-w-[440px]` |
| Modal width regular | `420px a 520px` | `w-full max-w-[520px]` |
| Border radius base | `24px` | `rounded-[24px]` |
| Border radius button | `12px a 14px` | `rounded-xl` |
| Border radius input | `12px` | `rounded-xl` |
| Border radius badge | `999px` | `rounded-full` |
| Sombra estandar card | suave, larga | `shadow-[0_18px_50px_rgba(15,23,42,0.08)]` |
| Sombra modal | mas alta | `shadow-[0_28px_80px_rgba(15,23,42,0.18)]` |

## Tipografia

| Token | Valor aproximado | Tailwind recomendado |
| --- | --- | --- |
| Familia tipografica | sans moderna, cercana a Inter | `font-['Inter',ui-sans-serif,system-ui]` |
| Titulo de pagina | `32px / semibold` | `text-[32px] leading-[1.1] font-semibold text-[#102247]` |
| Titulo de seccion | `24px / semibold` | `text-2xl font-semibold text-[#102247]` |
| Subtitulo | `16px / regular` | `text-base text-[#6B7A99]` |
| Texto normal | `14px / regular` | `text-sm text-[#102247]` |
| Texto auxiliar | `12px a 13px / regular` | `text-[13px] text-[#6B7A99]` |
| Texto de tabla | `13px / medium` | `text-[13px] font-medium text-[#102247]` |
| Peso de titulo | `600` | `font-semibold` |
| Peso de boton | `600` | `font-semibold` |
| Peso de label | `500 a 600` | `font-medium` |

## Componentes base

| Componente | Regla visual | Tailwind recomendado |
| --- | --- | --- |
| Boton primario azul | alto medio, relleno solido, radio suave, sombra casi nula | `inline-flex items-center justify-center gap-2 rounded-xl bg-[#2453FF] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#1E47DB] disabled:opacity-50` |
| Boton primario verde | mismo shape del primario, pero verde | `inline-flex items-center justify-center gap-2 rounded-xl bg-[#16A34A] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#14803C] disabled:opacity-50` |
| Boton secundario | borde visible, fondo blanco | `inline-flex items-center justify-center gap-2 rounded-xl border border-[#D8E1F0] bg-white px-4 py-2.5 text-sm font-semibold text-[#2453FF] hover:bg-[#F8FAFF]` |
| Boton destructivo | rojo, igual densidad que primario | `inline-flex items-center justify-center gap-2 rounded-xl bg-[#EF4444] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#DC2626]` |
| Input | blanco, borde suave, altura media, placeholder tenue | `h-11 rounded-xl border border-[#E6EBF5] bg-white px-3 text-sm text-[#102247] placeholder:text-slate-400 focus:border-[#2453FF] focus:outline-none focus:ring-2 focus:ring-[#2453FF]/15` |
| Select | misma caja del input | `h-11 rounded-xl border border-[#E6EBF5] bg-white px-3 text-sm text-[#102247] focus:border-[#2453FF] focus:outline-none focus:ring-2 focus:ring-[#2453FF]/15` |
| Textarea | radio igual al input, padding mayor | `min-h-[112px] rounded-xl border border-[#E6EBF5] bg-white px-3 py-3 text-sm text-[#102247] placeholder:text-slate-400 focus:border-[#2453FF] focus:outline-none focus:ring-2 focus:ring-[#2453FF]/15` |
| Modal | tarjeta blanca centrada, radio grande, backdrop oscuro | `rounded-[24px] border border-[#E6EBF5] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.18)]` |
| Drawer | panel blanco lateral, header claro, cierre visible | `h-full w-full max-w-[440px] border-l border-[#E6EBF5] bg-white shadow-[-18px_0_50px_rgba(15,23,42,0.12)]` |
| Card | base comun de pantalla | `rounded-[24px] border border-[#E6EBF5] bg-white shadow-[0_18px_50px_rgba(15,23,42,0.08)]` |
| Table | cabecera suave, filas aireadas, hover sutil | `w-full text-left text-[13px] text-[#102247]` |
| Badge | chip lleno suave con texto de color | `inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium` |
| Toast | tarjeta pequena, borde suave, icono y copy corto | `rounded-2xl border border-[#E6EBF5] bg-white px-4 py-3 shadow-[0_18px_50px_rgba(15,23,42,0.12)]` |

## Tokens especificos de layout publico

- Hero publico: `bg-[radial-gradient(circle_at_top_left,rgba(36,83,255,0.18),transparent_35%),linear-gradient(180deg,#081A3A_0%,#0E2C63_100%)]`
- Card de autenticacion: `rounded-[24px] border border-white/70 bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.12)]`
- Bloques de confianza: `rounded-2xl border border-white/10 bg-white/5 p-4 text-white/90`

## Tokens especificos de layout privado

- Shell privado: `bg-[#F5F7FB]`
- Sidebar activa: `bg-[#3D4BFF] text-white shadow-[inset_0_0_0_1px_rgba(255,255,255,0.08)]`
- Item sidebar inactivo: `text-white/80 hover:bg-white/8`
- Topbar: `rounded-[24px] border border-[#E6EBF5] bg-white`
- Panel de filtros: `rounded-[24px] border border-[#E6EBF5] bg-white p-5`

## Tokens de estados

- Empty state card: `rounded-[24px] border border-dashed border-[#D8E1F0] bg-white p-10 text-center`
- Loading state card: `rounded-[24px] border border-[#E6EBF5] bg-white p-10 text-center`
- Error state card: `rounded-[24px] border border-red-100 bg-red-50/60 p-6`
- Offline banner: `border border-amber-200 bg-amber-50 text-amber-800`

## Regla final

Si una implementacion necesita mas de un token nuevo para parecerse a una lamina, primero debe actualizar este documento y luego el codigo.
