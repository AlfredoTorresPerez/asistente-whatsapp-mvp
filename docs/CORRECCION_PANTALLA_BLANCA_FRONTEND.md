# Correccion de pantalla blanca en frontend local

## Cambio aplicado

Se agrego el archivo `frontend-react/vite.config.ts` para declarar explicitamente los plugins de React y Tailwind en Vite.

## Archivo agregado

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    port: 5173,
  },
})
```

## Validacion sugerida

1. Ejecutar `docker compose down`.
2. Ejecutar `docker compose up --build`.
3. Abrir `http://localhost:5173/`.
4. Si el navegador conserva sesion anterior, limpiar `sessionStorage` y `localStorage` desde consola.
