# Cambios de trazabilidad en frontend y backend

Se agrego una estrategia centralizada de trazabilidad para registrar llamadas relevantes en la capa de interfaz y en la capa de servidor.

## Frontend - capa de interfaz

Archivos agregados:

- `frontend-react/src/services/trace/traceService.ts`
- `frontend-react/src/services/trace/index.ts`

Archivo modificado:

- `frontend-react/src/services/api/httpClient.ts`

Cambios aplicados:

- Registro automatico en consola del navegador para cada solicitud HTTP ejecutada mediante `apiFetch`.
- Registro de inicio, fin, error, metodo invocado, proposito, ruta segura, metodo HTTP, tiempo de ejecucion e identificador de correlacion.
- Propagacion de `X-Correlation-Id` hacia el backend.
- Sanitizacion de claves sensibles como `password`, `token`, `secret`, `authorization`, `apiKey`, entre otras.
- Correccion del orden de construccion de `fetch` para preservar cabeceras calculadas por el cliente centralizado.
- Utilidades reutilizables `traceSync` y `traceAsync` para instrumentar funciones especificas sin duplicar llamadas directas a `console`.

Variables disponibles:

```properties
VITE_FRONTEND_TRACE_ENABLED=true
VITE_TRACE_MAX_PAYLOAD_LENGTH=1200
```

Ejemplo de uso manual en una funcion de frontend:

```ts
await traceService.traceAsync(
  {
    componentName: 'UsuarioComponent',
    methodName: 'cargarUsuarios',
    purpose: 'Obtener usuarios activos para mostrarlos en pantalla',
    data: { filtros },
  },
  () => cargarUsuariosDesdeApi(filtros),
)
```

## Backend - capa de servidor

Archivos agregados:

- `backend-java/src/main/java/com/asistentewhatsapp/shared/observability/CorrelationIdFilter.java`
- `backend-java/src/main/java/com/asistentewhatsapp/shared/observability/MethodTraceAspect.java`
- `backend-java/src/main/java/com/asistentewhatsapp/shared/observability/TraceProperties.java`
- `backend-java/src/main/java/com/asistentewhatsapp/shared/observability/TraceSanitizer.java`

Archivos modificados:

- `backend-java/pom.xml`
- `backend-java/src/main/resources/application.yml`
- `backend-java/src/main/java/com/asistentewhatsapp/security/SecurityConfig.java`
- `backend-java/src/main/java/com/asistentewhatsapp/shared/api/ApiErrorResponse.java`
- `backend-java/src/main/java/com/asistentewhatsapp/shared/exception/GlobalExceptionHandler.java`
- `backend-java/src/main/java/com/asistentewhatsapp/security/JwtAccessDeniedHandler.java`
- `backend-java/src/main/java/com/asistentewhatsapp/security/JwtAuthenticationEntryPoint.java`

Cambios aplicados:

- Dependencia `spring-boot-starter-aop` para trazabilidad automatizada mediante aspectos.
- Filtro global de correlacion para leer o generar `X-Correlation-Id`.
- Uso de MDC para incluir el identificador de correlacion en registros.
- Aspecto que registra inicio, fin, tiempo de ejecucion, parametros sanitizados, errores controlados y errores inesperados en controladores, servicios y repositorios.
- Sanitizacion de argumentos y registros Java antes de escribir en logs.
- Respuestas de error con `correlationId` para facilitar diagnostico entre frontend y backend.
- CORS ajustado para permitir y exponer la cabecera `X-Correlation-Id`.

Variables disponibles:

```properties
APP_METHOD_TRACING_ENABLED=true
APP_METHOD_TRACING_LOG_ARGUMENTS=true
APP_METHOD_TRACING_LOG_RESULT=false
APP_METHOD_TRACING_MAX_PAYLOAD_LENGTH=1200
APP_METHOD_TRACING_SLOW_THRESHOLD_MS=1500
```

## Recomendacion operativa

- En ambiente local se puede mantener la trazabilidad activa para diagnostico.
- En produccion se recomienda mantener `APP_METHOD_TRACING_LOG_RESULT=false` y evaluar `APP_METHOD_TRACING_LOG_ARGUMENTS=false` si el volumen de registros es alto.
- No registrar contrasenas, tokens, cabeceras de autorizacion ni datos bancarios.
- Usar el `correlationId` para unir consola del navegador, solicitud HTTP, log del servidor y respuesta de error.
