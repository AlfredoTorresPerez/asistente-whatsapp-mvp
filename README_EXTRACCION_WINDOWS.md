# Extraccion en Windows

Este paquete fue regenerado para evitar el error 0x80010135 de Windows: ruta de acceso demasiado larga.

Cambios aplicados al paquete comprimido:

- Se excluyo `frontend-react/node_modules` porque contiene rutas internas muy largas y se puede reconstruir con el gestor de paquetes.
- Se acorto la carpeta raiz interna de `asistente_fix_agendar` a `asistente`.
- Se excluyeron carpetas de salida como `target`, `dist` y `build` si existian.

Pasos sugeridos despues de extraer:

1. Abrir una terminal en `asistente/frontend-react`.
2. Instalar dependencias con el gestor que corresponda segun el archivo de bloqueo presente, por ejemplo `pnpm install`, `npm install` o `yarn install`.
3. Abrir una terminal en `asistente/backend-java`.
4. Compilar con `./mvnw clean package` en Linux/macOS o `mvnw.cmd clean package` en Windows.

Nota: el codigo fuente con la trazabilidad aplicada se mantiene dentro del paquete.
