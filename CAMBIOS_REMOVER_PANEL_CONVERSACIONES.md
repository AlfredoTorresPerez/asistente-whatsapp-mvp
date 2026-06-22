# Cambios - Remocion de panel antiguo de conversaciones

Se elimino la pantalla/panel antiguo de lista compacta que aparecia dentro del detalle de conversaciones con el encabezado "Todas las conversaciones".

## Ajustes realizados

- La ruta `/conversations` conserva la nueva bandeja de entrada tipo grilla con filtros, contadores y paginacion.
- La ruta `/conversations/:conversationId` ahora muestra solo el detalle de la conversacion seleccionada.
- Se agrego accion "Volver a bandeja" en el detalle para regresar a `/conversations`.
- Se elimino del renderizado el panel lateral antiguo con filas tipo tarjeta.
- Se eliminaron referencias visibles a "Todas las conversaciones" y "Ordenar: Recientes" dentro del detalle.
- Se mantuvieron acciones del detalle: asignarme, crear/ver prospecto, agendar cita, crear pedido, responder con IA, enviar mensaje, cerrar/reabrir.

## Archivo modificado

- `frontend-react/src/modules/conversations/pages/ConversationsPage.tsx`

## Validacion realizada

Se hizo una validacion de parseo TypeScript/TSX con `tsc` global. El entorno no tiene dependencias locales de React ni pnpm instaladas, por lo que no fue posible ejecutar build completo localmente en este contenedor.

## Comandos sugeridos en ambiente local

```powershell
docker compose -f docker-compose.local.yml down --remove-orphans
docker rm -f asistente-whatsapp-whatsapp-web asistente-whatsapp-postgres asistente-whatsapp-backend asistente-whatsapp-frontend
docker compose -f docker-compose.local.yml up -d --build
```

## Resultado esperado

- `/conversations` muestra la bandeja nueva.
- `/conversations/{id}` muestra solo el detalle del hilo, sin el panel antiguo de lista compacta.
