# Cambios v23.4.23

- El entorno local ahora deja `APP_AI_AGENTS_AUTO_REPLY_ENABLED=true` para que las respuestas reales al cliente usen el mismo flujo transaccional que la matriz de pruebas IA.
- Se corrige la persistencia de sede: si el cliente escribe `Providencia`, `Las Condes` u otra sede activa, la conversación deja de aparecer como `Sin sucursal`.
- Se agregan trazas `CONVERSATION_LOCATION_ASSIGNED` para validar la asignación automática de sede.
- Se mantiene la vista previa del panel como modo `dryRun=true`; la respuesta automática al cliente usa `dryRun=false`.
