# Cambios v23.4.16 - matriz Excel IA y correccion de orquestador

## Objetivo

Incorporar pruebas automatizadas basadas en la matriz Excel entregada para revisar preguntas de clientes y respuestas de IA con cobertura amplia de combinaciones.

## Cambios funcionales

1. Correccion de clasificacion de agenda:
   - Si el mensaje contiene un servicio y datos de agenda, como fecha, hora o sede, se enruta a agenda aunque no use explicitamente la palabra `agendar`.
   - Caso protegido: `Hola, quiero una limpieza facial para el viernes 12 de junio 2026 a las 16:00 en Providencia.`

2. Correccion de continuidad de contexto en preview:
   - `AgentCoordinatorService.preview(...)` ahora persiste contexto conversacional cuando la auditoria esta habilitada.
   - Esto permite que respuestas fragmentadas como `depilacion bozo` continuen el flujo de agenda previo.

## Cambios de prueba

1. Nuevo test:
   - `backend-java/src/test/java/com/asistentewhatsapp/aiagents/application/AiExcelMatrixOrchestratorCoverageTest.java`

2. Recursos generados desde Excel:
   - `backend-java/src/test/resources/ai-matrix/matriz_qa_v23_4_10.json`
   - `backend-java/src/test/resources/ai-matrix/servicios_v23_4_10.json`
   - `backend-java/src/test/resources/ai-matrix/alias_entidades_v23_4_10.json`

3. Scripts de ejecucion:
   - `scripts/tests/run_ai_matrix_excel_tests.ps1`
   - `scripts/tests/run_ai_matrix_excel_tests.sh`

## Modos de ejecucion

Auditoria:

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1
```

Estricto:

```powershell
.\scripts\tests\run_ai_matrix_excel_tests.ps1 -Strict
```

## Nota de verificacion

En el entorno de generacion no fue posible ejecutar Maven porque el wrapper requiere descargar Maven desde `repo.maven.apache.org`. El ZIP contiene el codigo y los scripts de prueba listos para ejecutar en el ambiente local Windows con acceso a dependencias Maven.
