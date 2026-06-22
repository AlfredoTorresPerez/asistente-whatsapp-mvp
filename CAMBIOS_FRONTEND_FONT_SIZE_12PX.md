# Cambio frontend - Font size global 12px

## Problema / requerimiento
Se solicitó modificar todo el frontend para que el tamaño de fuente quede en `12px`.

## Solución aplicada
Se modificó el archivo global de estilos del frontend:

- `frontend-react/src/index.css`

Se agregó una regla global sobre `#root` y sus descendientes para forzar `font-size: 12px !important`, de manera que prevalezca sobre clases Tailwind existentes como `text-sm`, `text-lg`, `text-[32px]`, etc.

También se reforzó la regla para controles de formulario:

- `button`
- `input`
- `select`
- `textarea`

## Archivos modificados

- `frontend-react/src/index.css`
- `CAMBIOS_FRONTEND_FONT_SIZE_12PX.md`

## Cómo probar

```powershell
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.local.yml build --no-cache frontend-react
docker compose -f docker-compose.local.yml up -d
```

Luego abrir:

```text
http://localhost:5173
```

y forzar recarga:

```text
Ctrl + F5
```

## Criterios de aceptación

- El texto de la aplicación renderiza a `12px`.
- Inputs, selects, botones y textareas renderizan a `12px`.
- El ajuste aplica sobre pantallas internas y componentes del frontend React.
