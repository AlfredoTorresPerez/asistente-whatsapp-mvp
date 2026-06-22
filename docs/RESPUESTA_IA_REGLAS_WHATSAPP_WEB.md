# Recuadro Respuesta IA y reglas aplicadas

## Objetivo

Se agrego en la pantalla `Conexion WhatsApp Web` un recuadro de auditoria llamado `Respuesta IA y reglas aplicadas`.

El recuadro permite validar, desde la administracion del canal, como el modulo de centro estetico interpreta un mensaje y que reglas operativas se aplican antes de responder al cliente.

## Ubicacion visual

Ruta visual:

```text
Administracion -> Conexion WhatsApp Web
```

El recuadro queda ubicado entre:

```text
Estado del canal / Historial del adaptador
```

y:

```text
Mensaje de prueba real
```

## Datos mostrados

El recuadro muestra:

- Intencion detectada.
- Confianza.
- Si requiere consulta a base de datos.
- Si requiere derivacion humana.
- Entidades detectadas: servicio, producto, fecha, hora, profesional y cliente.
- Reglas aplicadas derivadas de la decision.
- Respuesta sugerida por la IA o por el clasificador local.
- Auditoria: origen, modelo y fecha del analisis.
- Motivo de derivacion, si existe.
- Mensaje analizado.

## Fuentes de datos

El recuadro usa dos fuentes:

1. Ultimo mensaje real analizado:

```text
GET /api/v1/esthetic/intent/logs?page=0&size=1
```

2. Analisis manual del mensaje de prueba:

```text
POST /api/v1/esthetic/intent/analyze
```

## Comportamiento esperado

- Si existe un log real, se muestra el ultimo analisis registrado.
- Si el usuario presiona `Analizar mensaje de prueba`, se muestra una vista previa con el resultado del mensaje escrito en el formulario.
- Si no existe analisis, se muestra un estado vacio explicando como probarlo.
- Si el modulo detecta riesgo clinico, contraindicacion o baja confianza, el estado cambia visualmente.

## Estados visuales

```text
RESPUESTA AUTOMATICA
REQUIERE CONSULTA INTERNA
INFORMACION INSUFICIENTE
DERIVAR A HUMANO
SIN ANALISIS
```

## Reglas visualizadas

Las reglas se derivan de la respuesta del motor de intencion:

- Consultar base de datos antes de responder datos operativos.
- Derivar a atencion humana antes de confirmar o recomendar.
- Aplicar bloqueo de seguridad estetica por condicion sensible.
- Pedir aclaracion por baja confianza de interpretacion.
- Validar disponibilidad y profesional antes de confirmar agenda.
- Usar catalogo vigente; no inventar precio, stock, duracion ni promociones.
- No emitir diagnosticos ni prometer resultados garantizados.
- Mostrar respuesta sugerida filtrada por reglas del centro.

## Archivo principal modificado

```text
frontend-react/src/modules/administration/pages/WhatsAppWebConnectionPage.tsx
```

## Dependencias reutilizadas

- `listAestheticIntentLogs`
- `analyzeAestheticIntent`
- `StatusBadge`
- `LoadingState`
- `ErrorState`
- `EmptyState`
- `Button`
- `Card`
