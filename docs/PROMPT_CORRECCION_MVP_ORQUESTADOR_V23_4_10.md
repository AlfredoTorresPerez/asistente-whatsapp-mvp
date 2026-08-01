# Prompt de corrección y validación del MVP de Asistente WhatsApp

> **Documento histórico** (v23.4.10). Las referencias al adaptador experimental de WhatsApp Web y a `whatsapp-web-service` describen el estado en esa fecha; el servicio externo fue eliminado el 2026-08-01 y el canal es nativo del backend (`META_CLOUD_API`/`SIMULATED`).

Actúa como orquestador técnico senior y auditor de implementación para un MVP de asistente de WhatsApp para negocios, usando como caso principal un centro estético.

## Contexto

El proyecto está en estado de implementación local. Contiene servicio servidor Java con Spring Boot, interfaz web React, PostgreSQL, migraciones Flyway, adaptador experimental de WhatsApp Web, orquestador IA, agentes especializados, agenda, catálogo, reservas temporales y enlaces de confirmación.

## Objetivo

Corregir los bloqueos técnicos mínimos que impiden validar el MVP de forma confiable, documentar los riesgos pendientes y dejar el proyecto preparado para una siguiente ejecución local controlada.

## Detalles que deben solucionarse

### 1. Compilación de pruebas Java

Revisar todas las pruebas que instancien `BookingAgent`.

Problema detectado:

- `BookingAgent` requiere `AiBusinessKnowledgeService`, `BusinessLocationJdbcRepository` y `TransactionalAgendaBookingService`.
- Algunas pruebas lo instanciaban solo con dos dependencias.

Acción requerida:

- Actualizar pruebas para inyectar un doble de prueba de `TransactionalAgendaBookingService`.
- Configurar el doble de prueba para devolver ubicación faltante cuando no exista sede activa.
- Configurar el doble de prueba para devolver `Optional.empty()` al crear enlace temporal, de forma que el agente use la respuesta segura de conocimiento.

Criterio de aceptación:

- No debe quedar ninguna instancia `new BookingAgent(knowledgeService, locationRepository)`.
- Las pruebas deben reflejar el constructor real del componente.

### 2. Compatibilidad de `AgentConversationRequest`

Problema detectado:

- El registro `AgentConversationRequest` tiene nuevos campos para sede seleccionada, trazabilidad y modo de simulación.
- Pruebas antiguas todavía usan el constructor histórico de ocho parámetros.

Acción requerida:

- Agregar un constructor sobrecargado compatible que complete `selectedLocationId`, `selectedLocationName` y `traceId` con `null`, y `dryRun` con `false`.

Criterio de aceptación:

- Las pruebas existentes pueden seguir creando solicitudes simples sin conocer los nuevos campos.
- El código productivo mantiene el constructor completo de doce parámetros.

### 3. Dependencia reproducible del adaptador WhatsApp Web

Problema detectado:

- `whatsapp-web-service/package.json` usaba `whatsapp-web.js` con versión `latest`.
- El archivo de bloqueo fijaba `1.34.7`, generando inconsistencia y riesgo de instalaciones no reproducibles.

Acción requerida:

- Reemplazar `latest` por `1.34.7` en `package.json`.

Criterio de aceptación:

- `package.json` y `pnpm-lock.yaml` deben apuntar a la misma versión.

### 4. Duplicidad de trazas IA

Problema detectado:

- En la vista previa de respuesta IA se registraba dos veces el mismo evento `AI_FINAL_RESPONSE`.

Acción requerida:

- Eliminar la duplicación y dejar una sola traza final.

Criterio de aceptación:

- Cada vista previa registra una sola traza final por respuesta generada.

### 5. Archivos de respaldo en código de interfaz

Problema detectado:

- Existían archivos `.bak` dentro de `frontend-react/src`.

Acción requerida:

- Eliminar respaldos del árbol de código fuente.

Criterio de aceptación:

- No deben quedar archivos `.bak` dentro de `frontend-react/src`.

### 6. Confirmación por enlace usando localhost

Problema detectado:

- La configuración local usa `localhost` para enlaces de confirmación.
- Esto sirve para pruebas en el mismo equipo, pero no para enlaces enviados a un teléfono real.

Acción requerida:

- Documentar explícitamente que para pruebas desde teléfono externo se debe usar un dominio público o túnel HTTPS hacia la interfaz web.
- Mantener `localhost` como valor local por defecto solo para ejecución en el equipo del desarrollador.

Criterio de aceptación:

- `.env.example` debe advertir que no se debe usar `localhost` si se enviarán enlaces reales por WhatsApp.

### 7. Verificación local posterior

Acción requerida:

- Agregar un guion de verificación local que revise permisos, configuración crítica y sintaxis básica del servicio WhatsApp Web.

Criterio de aceptación:

- El guion debe detectar:
  - permiso ejecutable de `backend-java/mvnw`;
  - versión exacta de `whatsapp-web.js`;
  - ausencia de archivos `.bak` en interfaz;
  - existencia de la advertencia de `localhost`;
  - sintaxis básica de `whatsapp-web-service/src/server.js` cuando Node.js esté disponible.

## Restricciones

- No inventar funcionalidades.
- No cambiar la arquitectura sin evidencia.
- No convertir WhatsApp Web en canal productivo.
- No eliminar la configuración local de `localhost`; solo documentar su límite.
- No modificar migraciones históricas salvo que exista un error demostrado.
- Mantener compatibilidad con el flujo actual de centro estético.

## Resultado esperado

Entregar un proyecto corregido, documentado y empaquetado nuevamente como ZIP, junto con un resumen de cambios aplicados y validaciones ejecutadas.
