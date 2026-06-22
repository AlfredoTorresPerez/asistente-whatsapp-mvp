# Cambios: pantalla IA del negocio funcional

## Objetivo

Convertir la pantalla `IA del negocio` desde una vista demostrativa con datos estaticos a un panel operativo conectado con los datos reales del negocio.

## Cambios aplicados

1. La pantalla ahora consume datos reales desde los endpoints existentes del modulo estetico:
   - Servicios.
   - Productos.
   - Reglas del negocio.
   - Auditoria de intenciones.
   - Categorias de servicios y productos.

2. Los indicadores superiores ahora se calculan desde la auditoria de intenciones:
   - IA activa.
   - Conversaciones resueltas estimadas.
   - Confianza media.
   - Derivaciones humanas.
   - Respuestas sugeridas hoy.

3. La base de conocimiento dejo de usar registros fijos y ahora permite:
   - Filtrar por pestanas: Servicios, Productos, Reglas IA, Politicas y Auditoria.
   - Buscar por titulo, categoria o descripcion.
   - Filtrar por estado.
   - Ver una base completa en modal.
   - Agregar contenido.
   - Editar servicios, productos y reglas.

4. El simulador ahora usa el endpoint real de analisis de intencion:
   - `POST /api/v1/esthetic/intent/analyze`

5. La vista previa de conversacion ahora muestra:
   - Mensaje probado.
   - Respuesta sugerida por IA.
   - Confianza.
   - Intencion detectada.
   - Edicion manual de la respuesta.
   - Aprobacion copiando la respuesta al portapapeles.

6. El boton `Guardar prompt` ahora persiste la configuracion como regla del negocio:
   - Codigo: `PROMPT_OPERATIVO_IA_NEGOCIO`
   - Tipo: `AI_PROMPT`

7. La configuracion del asistente ahora es interactiva:
   - Activar o pausar IA.
   - Modo sugerido o automatico.
   - Tono de comunicacion.
   - Umbral de derivacion.
   - Permisos de precios, agenda y promociones.
   - Validacion obligatoria de disponibilidad.

## Archivos modificados

- `frontend-react/src/modules/business-ai/pages/BusinessAiPage.tsx`

## Validacion funcional esperada

1. Entrar a `/business-ai`.
2. Confirmar que se cargan servicios y reglas reales del negocio.
3. Escribir un escenario de prueba.
4. Presionar `Probar IA`.
5. Ver respuesta, intencion y confianza.
6. Editar la respuesta si corresponde.
7. Presionar `Aprobar y copiar`.
8. Crear o editar un servicio desde `Agregar contenido`.
9. Guardar el prompt operativo.

## Limites pendientes

- `Aprobar y copiar` no envia aun el mensaje directamente a una conversacion real porque la pantalla no recibe `conversation_id` ni destinatario.
- La configuracion del prompt se guarda como regla del negocio, pero su uso por el motor de respuesta depende de que el backend la lea dentro del orquestador conversacional.
- La disponibilidad real sigue dependiendo del modulo agenda; la pantalla no confirma reservas automaticamente.
