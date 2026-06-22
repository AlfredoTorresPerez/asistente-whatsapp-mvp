# Pruebas de matriz Excel IA v23.4.16

Esta version incorpora una prueba automatizada basada en la planilla `matriz_preguntas_respuestas_ia_whatsapp_orquestador_v23_4_10.xlsx`.

## Archivos incorporados

- `backend-java/src/test/resources/ai-matrix/matriz_qa_v23_4_10.json`: 119 casos de preguntas y respuestas esperadas.
- `backend-java/src/test/resources/ai-matrix/servicios_v23_4_10.json`: 56 servicios usados por la prueba.
- `backend-java/src/test/resources/ai-matrix/alias_entidades_v23_4_10.json`: alias de entidades usados para detectar servicios, fechas y sucursales.
- `backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiExcelMatrixOrchestratorCoverageTest.java`: prueba JUnit basada en la matriz.
- `scripts/tests/run_ai_matrix_excel_tests.ps1`: ejecucion Windows.
- `scripts/tests/run_ai_matrix_excel_tests.sh`: ejecucion Linux/macOS.

## Ejecucion en modo auditoria

Este modo recorre toda la matriz y genera un reporte sin bloquear el build por diferencias funcionales. Sirve para revisar brechas entre la planilla y la IA.

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1
```

Reporte esperado:

```text
backend-java\target\ai-matrix\reporte_matriz_excel_ia_v23_4_16.md
```

## Ejecucion en modo estricto

Este modo falla si la intencion, el agente, los datos faltantes o el enlace esperado no coinciden con la matriz.

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```

Equivalente directo Maven:

```powershell
cd backend-java
.\mvnw.cmd -Dtest=AiExcelMatrixOrchestratorCoverageTest -Dai.matrix.strict=true test
```

## Correcciones incluidas para el caso detectado

Se ajusto el detector de intencion para que mensajes con servicio + datos de agenda, por ejemplo:

```text
Hola, quiero una limpieza facial para el viernes 12 de junio 2026 a las 16:00 en Providencia.
```

se enruten al agente de agenda y no al agente comercial.

Tambien se persiste contexto durante preview de IA para que una conversacion fragmentada pueda continuar:

```text
Cliente: Quiero agendar mañana a las 16:00 horas
IA: ¿Qué servicio quieres agendar?
Cliente: depilacion bozo
```

En este flujo, la segunda respuesta debe seguir en agenda y no reiniciarse como consulta comercial.
