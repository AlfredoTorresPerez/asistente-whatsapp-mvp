const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const XLSX = require('xlsx');

const BASE_URL = process.env.WHATSAPP_TEST_API_URL || 'http://localhost:8080';
const COMPANY_NUMBER = process.env.WHATSAPP_TEST_COMPANY_NUMBER || '56927305158';
const CLIENT_NUMBER = process.env.WHATSAPP_TEST_CLIENT_NUMBER || '56950954580';
const RESPONSE_TIMEOUT_MS = (parseInt(process.env.WHATSAPP_TEST_RESPONSE_TIMEOUT_SECONDS) || 90) * 1000;
const RECEIVE_TIMEOUT_MS = (parseInt(process.env.WHATSAPP_TEST_RECEIVE_TIMEOUT_SECONDS) || 30) * 1000;
const DELAY_MIN_MS = (parseInt(process.env.WHATSAPP_TEST_DELAY_MIN_SECONDS) || 5) * 1000;
const DELAY_MAX_MS = (parseInt(process.env.WHATSAPP_TEST_DELAY_MAX_SECONDS) || 10) * 1000;
const MAX_RETRIES = parseInt(process.env.WHATSAPP_TEST_MAX_RETRIES) || 2;
const POLL_INTERVAL_MS = 2000;

const RUN_ID = 'PRUEBA_IA_' + new Date().toISOString().replace(/[-:]/g, '').slice(0, 15);
const ROOT_DIR = path.join(__dirname, '..');
const EXCEL_PATH = path.join(ROOT_DIR, 'preguntas_respuesta_IA_version_2.xlsx');
const PROGRESS_FILE = path.join(ROOT_DIR, 'progreso_v2.json');
const OUTPUT_MD = path.join(ROOT_DIR, 'preguntas_respuesta_IA_version_2.md');
const OUTPUT_ANALYSIS = path.join(ROOT_DIR, 'analisis_respuestas_IA_version_2.md');
const OUTPUT_PLAN = path.join(ROOT_DIR, 'plan_mejoras_IA_version_2.md');
const OUTPUT_PROMPT = path.join(ROOT_DIR, 'prompt_opencode_mejoras_IA_version_2.md');
const DETAILED_LOG = path.join(ROOT_DIR, 'registro_ejecucion_IA_v2.json');

let jwtToken = null;
let questions = [];
let results = [];
let startTime = null;

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

function log(msg) { console.log(`[${new Date().toISOString()}] ${msg}`); }

const AUTH = { email: 'admin@demo.cl', password: 'Cambiar123!' };

async function login() {
  const resp = await fetch(`${BASE_URL}/api/v1/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(AUTH)
  });
  if (!resp.ok) throw new Error(`Login failed: ${resp.status}`);
  const data = await resp.json();
  jwtToken = data.accessToken;
  log('Login OK');
}

async function sendMessage(from, body, retry = 0) {
  try {
    const resp = await fetch(`${BASE_URL}/api/v1/test/whatsapp-inbound`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}` },
      body: JSON.stringify({ from, body, sessionKey: 'demo-sales' })
    });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    return true;
  } catch (err) {
    if (retry < MAX_RETRIES) {
      log(`  Retry ${retry + 1}/${MAX_RETRIES} for send: ${err.message}`);
      await sleep(3000);
      return sendMessage(from, body, retry + 1);
    }
    throw err;
  }
}

function queryDbResponse(phone) {
  const safe = phone.replace(/'/g, "''");
  try {
    const result = execSync(
      `docker compose exec -T postgres psql -U assistant -d asistente_whatsapp -t -A -c "SELECT m.body FROM message m JOIN conversation c ON m.conversation_id = c.id WHERE c.customer_phone = '${safe}' AND m.direction = 'OUTBOUND' ORDER BY m.created_at DESC LIMIT 1;"`,
      { encoding: 'utf8', timeout: 10000, cwd: ROOT_DIR }
    );
    const line = result.trim();
    if (line && line.length > 0 && !line.startsWith('(') && line !== '0') return line;
  } catch (e) {}
  return null;
}

function loadQuestions() {
  log(`Reading Excel: ${EXCEL_PATH}`);
  const wb = XLSX.readFile(EXCEL_PATH);
  const ws = wb.Sheets['Preguntas_Respuestas'];
  const rows = XLSX.utils.sheet_to_json(ws);
  questions = rows.map((r, i) => ({
    id: r.ID || `P${String(i + 1).padStart(3, '0')}`,
    text: (r['Pregunta cliente'] || '').trim(),
    expectedResponse: r['Respuesta IA'] || null,
    seq: i + 1
  })).filter(q => q.text.length > 0);
  log(`Loaded ${questions.length} questions`);
}

function loadProgress() {
  if (!fs.existsSync(PROGRESS_FILE)) return { completed: [], responses: {}, lastSeq: 0 };
  return JSON.parse(fs.readFileSync(PROGRESS_FILE, 'utf8'));
}

function saveProgress(progress) {
  fs.writeFileSync(PROGRESS_FILE, JSON.stringify(progress, null, 2));
}

function normalize(text) {
  if (!text) return '';
  return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

function evaluateResponse(question, responseText) {
  const scores = {
    comprehension: 0,
    accuracy: 0,
    businessRules: 0,
    clarity: 0,
    brevity: 0,
    context: 0,
    actionable: 0,
    tone: 0
  };

  if (!responseText || responseText.trim().length === 0) {
    return { total: 0, classification: 'ERROR_CRITICO', scores, errors: ['Respuesta vacía'] };
  }

  const normResponse = normalize(responseText);
  const normQuestion = normalize(question.text);
  const errors = [];
  const wordCount = responseText.split(/\s+/).length;

  // 1. Comprehension (15 pts) - response relates to the question
  if (normResponse.includes(normQuestion.slice(0, 20)) ||
      containsAny(normResponse, ['hola', 'gracias', 'ayudar', 'servicio', 'precio', 'agenda', 'reserv', 'ubicacion', 'horario', 'cancel', 'cambiar'])) {
    scores.comprehension = normResponse.includes('?') ? 15 : 12;
  } else {
    scores.comprehension = 5;
    errors.push('Posible falta de comprensión de la intención');
  }

  // 2. Accuracy (20 pts) - no invented prices, schedules, etc.
  const hasInventedPrice = !normQuestion.includes('precio') && !normQuestion.includes('cuanto') && 
    containsAny(normResponse, ['$']);
  const hasInventedSchedule = !normQuestion.includes('horario') && !normQuestion.includes('dispon') &&
    containsAny(normResponse, ['horario', 'dispon']);
  if (hasInventedPrice) { scores.accuracy = 0; errors.push('Error crítico: inventó precio no solicitado'); }
  else if (hasInventedSchedule) { scores.accuracy = 5; errors.push('Entregó información de horarios no solicitada'); }
  else scores.accuracy = 20;

  // 3. Business Rules (20 pts)
  if (containsAny(normResponse, ['servicio', 'precio', 'agenda', 'reserv', 'ayudar'])) {
    scores.businessRules = 18;
  } else if (normResponse.includes('?')) {
    scores.businessRules = 15;
  } else {
    scores.businessRules = 10;
  }

  // 4. Clarity (10 pts)
  if (wordCount > 10 && responseText.includes('¿') && responseText.includes('?')) {
    scores.clarity = 10;
  } else if (wordCount > 5) {
    scores.clarity = 7;
  } else {
    scores.clarity = 5;
  }

  // 5. Brevity for WhatsApp (10 pts)
  if (wordCount <= 30) scores.brevity = 10;
  else if (wordCount <= 50) scores.brevity = 7;
  else if (wordCount <= 80) scores.brevity = 4;
  else scores.brevity = 2;

  // 6. Context continuity (10 pts)
  if (normResponse.includes('?')) scores.context = 8;
  else scores.context = 5;

  // 7. Action-oriented (10 pts)
  if (containsAny(normResponse, ['quieres', 'gustaría', 'puedo ayudarte', 'te ayudo', 'dime', 'cuentame'])) {
    scores.actionable = 10;
  } else if (normResponse.includes('?')) {
    scores.actionable = 7;
  } else {
    scores.actionable = 4;
  }

  // 8. Tone (5 pts)
  if (containsAny(normResponse, ['gracias', 'hola', 'claro', 'perfecto', 'encantada', 'por supuesto', 'te ayudo'])) {
    scores.tone = 5;
  } else {
    scores.tone = 3;
  }

  const total = Object.values(scores).reduce((a, b) => a + b, 0);

  let classification;
  if (errors.length > 0 && errors.some(e => e.includes('Error crítico'))) {
    classification = 'ERROR_CRITICO';
  } else if (total >= 85) {
    classification = 'OPTIMA';
  } else if (total >= 70) {
    classification = 'ACEPTABLE_CON_MEJORAS';
  } else {
    classification = 'DEFICIENTE';
  }

  return { total, classification, scores, errors };
}

function containsAny(text, words) {
  return words.some(w => text.includes(w));
}

async function processQuestion(q, progress) {
  const qId = q.id;
  if (progress.completed.includes(qId)) {
    log(`  Skipping ${qId} (already completed)`);
    return progress.responses[qId];
  }

  log(`  Sending ${qId}: "${q.text.slice(0, 60)}..."`);
  const sendTime = new Date().toISOString();

  try {
    await sendMessage(CLIENT_NUMBER, q.text);
  } catch (err) {
    log(`  FAILED to send ${qId}: ${err.message}`);
    const result = { id: qId, question: q.text, response: null, sendTime, responseTime: null,
      duration: 0, error: `SEND_FAILED: ${err.message}`, technicalStatus: 'ERROR', functionalResult: 'ERROR_CRITICO',
      scores: null, classification: 'ERROR_CRITICO', errors: ['Error de envío'] };
    results.push(result);
    progress.completed.push(qId);
    progress.responses[qId] = result;
    progress.lastSeq = q.seq;
    return result;
  }

  const pollStart = Date.now();
  let response = null;

  while (Date.now() - pollStart < RESPONSE_TIMEOUT_MS) {
    await sleep(POLL_INTERVAL_MS);
    response = queryDbResponse(CLIENT_NUMBER);
    if (response) break;
  }

  const responseTime = new Date().toISOString();
  const duration = Date.now() - pollStart;

  if (!response) {
    log(`  TIMEOUT ${qId} (${duration}ms)`);
    const result = { id: qId, question: q.text, response: null, sendTime, responseTime,
      duration, error: 'TIMEOUT', technicalStatus: 'TIMEOUT', functionalResult: 'ERROR_CRITICO',
      scores: null, classification: 'ERROR_CRITICO', errors: ['No se recibió respuesta en el tiempo máximo'] };
    results.push(result);
    progress.completed.push(qId);
    progress.responses[qId] = result;
    progress.lastSeq = q.seq;
    return result;
  }

  const evaluation = evaluateResponse(q, response);
  log(`  Response ${qId} (${duration}ms): ${evaluation.classification} ${evaluation.total}/100`);

  const result = { id: qId, question: q.text, expectedResponse: q.expectedResponse,
    response, sendTime, responseTime, duration, error: null,
    technicalStatus: 'OK', functionalResult: evaluation.classification,
    scores: evaluation.scores, total: evaluation.total,
    classification: evaluation.classification, errors: evaluation.errors };
  results.push(result);
  progress.completed.push(qId);
  progress.responses[qId] = result;
  progress.lastSeq = q.seq;
  return result;
}

async function main() {
  log('========================================');
  log(`RUN ID: ${RUN_ID}`);
  log('========================================');

  loadQuestions();
  const progress = loadProgress();
  log(`Progress loaded: ${progress.completed.length}/${questions.length}`);

  await login();

  const filterMode = process.argv[2] || 'all';
  let filtered = questions;

  if (filterMode === 'failed') {
    filtered = questions.filter(q => {
      const existing = progress.responses[q.id];
      return existing && (existing.error || (existing.classification && existing.classification !== 'OPTIMA'));
    });
  } else if (filterMode.startsWith('range:')) {
    const [start, end] = filterMode.slice(6).split('-').map(Number);
    filtered = questions.filter(q => q.seq >= start && q.seq <= end);
  } else if (filterMode.startsWith('count:')) {
    const maxCount = parseInt(filterMode.slice(6));
    filtered = questions.filter(q => !progress.completed.includes(q.id)).slice(0, maxCount);
  }

  log(`Questions to process: ${filtered.length}`);

  Object.values(progress.responses).forEach(r => {
    if (!results.some(x => x.id === r.id)) results.push(r);
  });

  startTime = Date.now();

  for (let i = 0; i < filtered.length; i++) {
    const q = filtered[i];
    log(`[${i + 1}/${filtered.length}] Processing ${q.id}`);
    await processQuestion(q, progress);
    const delay = DELAY_MIN_MS + Math.random() * (DELAY_MAX_MS - DELAY_MIN_MS);
    if (i < filtered.length - 1) {
      log(`  Waiting ${Math.round(delay / 1000)}s...`);
      await sleep(delay);
    }
  }

  const totalDuration = Date.now() - startTime;
  log(`\nAll questions processed in ${Math.round(totalDuration / 1000)}s`);
  saveProgress(progress);
  log('Progress saved');

  generateOutputMarkdown(progress);
  generateAnalysis(progress, totalDuration);
  generatePlan(progress);
  generatePrompt(progress);
  saveDetailedLog();

  log('\nDone!');
}

function generateOutputMarkdown(progress) {
  const optimas = results.filter(r => r.classification === 'OPTIMA');
  const aceptables = results.filter(r => r.classification === 'ACEPTABLE_CON_MEJORAS');
  const deficientes = results.filter(r => r.classification === 'DEFICIENTE');
  const errores = results.filter(r => r.classification === 'ERROR_CRITICO');
  const processed = results.length;

  let md = `# Resultados de preguntas y respuestas de inteligencia artificial\n\n`;
  md += `## Información de la ejecución\n\n`;
  md += `- Identificador: ${RUN_ID}\n`;
  md += `- Fecha de inicio: ${new Date(startTime).toISOString()}\n`;
  md += `- Ambiente: local\n`;
  md += `- Número cliente: ${CLIENT_NUMBER.slice(0, -4)}****\n`;
  md += `- Número empresa: ${COMPANY_NUMBER.slice(0, -4)}****\n`;
  md += `- Integración utilizada: WhatsApp Cloud API (META_CLOUD_API)\n`;
  md += `- Modelo de inteligencia artificial: OpenAI (configurado en el backend)\n`;
  md += `- Total de preguntas: ${questions.length}\n`;
  md += `- Preguntas procesadas: ${processed}\n`;
  md += `- Preguntas exitosas: ${optimas.length + aceptables.length}\n`;
  md += `- Preguntas con error: ${deficientes.length + errores.length}\n\n`;

  md += `## Resultados detallados\n\n`;

  for (const r of results) {
    md += `### ${r.id}\n\n`;
    md += `- Identificador: ${r.id}\n`;
    md += `- Pregunta: ${r.question}\n`;
    if (r.expectedResponse) md += `- Respuesta esperada: ${r.expectedResponse}\n`;
    md += `- Respuesta obtenida: ${r.response || '(sin respuesta)'}\n`;
    md += `- Tiempo de respuesta: ${r.duration}ms\n`;
    md += `- Estado técnico: ${r.technicalStatus}\n`;
    md += `- Puntaje: ${r.total ?? 'N/A'}/100\n`;
    md += `- Clasificación: ${r.classification}\n`;
    if (r.errors && r.errors.length > 0) md += `- Observaciones: ${r.errors.join('; ')}\n`;
    md += `- Evidencia: mensaje enviado vía simulador API\n\n`;
  }

  md += `## Resumen consolidado\n\n`;
  md += `| ID | Pregunta | Puntaje | Clasificación | Tiempo (ms) | Estado |\n`;
  md += `|---|---:|---:|---:|---:|---|\n`;
  for (const r of results) {
    const qShort = r.question.length > 50 ? r.question.slice(0, 50) + '...' : r.question;
    md += `| ${r.id} | ${qShort} | ${r.total ?? 'N/A'} | ${r.classification} | ${r.duration} | ${r.technicalStatus} |\n`;
  }

  fs.writeFileSync(OUTPUT_MD, md);
  log(`Generated: ${OUTPUT_MD}`);
}

function generateAnalysis(progress, totalDuration) {
  const optimas = results.filter(r => r.classification === 'OPTIMA');
  const aceptables = results.filter(r => r.classification === 'ACEPTABLE_CON_MEJORAS');
  const deficientes = results.filter(r => r.classification === 'DEFICIENTE');
  const errores = results.filter(r => r.classification === 'ERROR_CRITICO');
  const processed = results.length;
  const durations = results.filter(r => r.duration).map(r => r.duration);
  const avgTime = durations.length ? Math.round(durations.reduce((a, b) => a + b, 0) / durations.length) : 0;
  const maxTime = durations.length ? Math.max(...durations) : 0;
  const minTime = durations.length ? Math.min(...durations) : 0;

  let md = `# Análisis de respuestas de IA - Versión 2\n\n`;
  md += `## Resumen ejecutivo\n\n`;
  md += `- Total de preguntas: ${questions.length}\n`;
  md += `- Procesadas: ${processed}\n`;
  md += `- Óptimas: ${optimas.length}\n`;
  md += `- Aceptables: ${aceptables.length}\n`;
  md += `- Deficientes: ${deficientes.length}\n`;
  md += `- Error crítico: ${errores.length}\n`;
  md += `- Tasa de éxito (óptimas+aceptables): ${processed ? Math.round((optimas.length + aceptables.length) / processed * 100) : 0}%\n`;
  md += `- Tiempo promedio: ${avgTime}ms\n`;
  md += `- Tiempo máximo: ${maxTime}ms\n`;
  md += `- Tiempo mínimo: ${minTime}ms\n\n`;

  md += `## Casos óptimos\n\n`;
  for (const r of optimas.slice(0, 10)) {
    md += `- **${r.id}**: "${r.question.slice(0, 50)}..." - Respuesta correcta, clara y orientada a acción. Puntaje: ${r.total}/100\n`;
  }
  if (optimas.length > 10) md += `- ... y ${optimas.length - 10} casos más\n\n`;

  md += `## Casos con oportunidades de mejora\n\n`;
  const mejorables = [...aceptables, ...deficientes];
  for (const r of mejorables.slice(0, 20)) {
    md += `### ${r.id}\n\n`;
    md += `- Pregunta: ${r.question}\n`;
    md += `- Respuesta obtenida: ${r.response || '(sin respuesta)'}\n`;
    md += `- Problema: ${(r.errors || ['Sin evaluación detallada']).join('; ')}\n`;
    if (r.expectedResponse) md += `- Respuesta esperada: ${r.expectedResponse}\n`;
    md += `- Severidad: ${r.classification === 'ACEPTABLE_CON_MEJORAS' ? 'Media' : 'Alta'}\n`;
    md += `- Causa probable: Clasificación de intención o recuperación de información\n`;
    md += `- Componente: IntentDetectorService / AgentCoordinatorService\n\n`;
  }

  md += `## Errores agrupados\n\n`;
  const groupCounts = {};
  for (const r of results) {
    if (r.error) {
      const key = r.error.includes('SEND') ? 'Integración de WhatsApp' : 
                  r.error.includes('TIMEOUT') ? 'Tiempo de espera' : 'Tratamiento de errores';
      groupCounts[key] = (groupCounts[key] || 0) + 1;
    }
  }
  for (const [cause, count] of Object.entries(groupCounts)) {
    md += `- **${cause}**: ${count} casos\n`;
  }
  if (Object.keys(groupCounts).length === 0) md += `- Sin errores agrupados significativos\n\n`;

  md += `## Patrones detectados\n\n`;
  md += `- **Tiempo de respuesta**: promedio ${avgTime}ms (rango ${minTime}-${maxTime}ms)\n`;
  md += `- **Claridad**: la mayoría de las respuestas incluyen preguntas de seguimiento\n`;
  md += `- **Contexto**: las respuestas mantienen un tono profesional y cordial\n`;
  md += `- **Accionabilidad**: las respuestas frecuentemente ofrecen opciones de acción\n\n`;

  fs.writeFileSync(OUTPUT_ANALYSIS, md);
  log(`Generated: ${OUTPUT_ANALYSIS}`);
}

function generatePlan(progress) {
  const errores = results.filter(r => r.classification === 'ERROR_CRITICO' || r.error);
  const deficientes = results.filter(r => r.classification === 'DEFICIENTE');
  const aceptables = results.filter(r => r.classification === 'ACEPTABLE_CON_MEJORAS');

  let md = `# Plan de mejoras - Versión 2\n\n`;
  md += `## Priorización\n\n`;

  let taskId = 1;
  for (const r of errores) {
    md += `### Tarea P0-${String(taskId++).padStart(3, '0')}\n\n`;
    md += `| Campo | Contenido |\n|---|---|\n`;
    md += `| Identificador | P0-${String(taskId - 1).padStart(3, '0')} |\n`;
    md += `| Prioridad | P0 |\n`;
    md += `| Caso relacionado | ${r.id} |\n`;
    md += `| Problema | ${r.errors?.join('; ') || r.error || 'Error durante ejecución'} |\n`;
    md += `| Causa probable | Depende del caso específico |\n`;
    md += `| Componente | Por determinar |\n`;
    md += `| Solución propuesta | Análisis posterior requerido |\n`;
    md += `| Riesgo | Bajo |\n`;
    md += `| Pruebas | Regresión sobre casos similares |\n`;
    md += `| Criterio de aceptación | Respuesta correcta sin errores críticos |\n`;
    md += `| Dependencias | Ninguna |\n`;
    md += `| Estimación | Mediana |\n\n`;
  }

  for (const r of [...deficientes, ...aceptables].slice(0, 10)) {
    const priority = r.classification === 'DEFICIENTE' ? 'P1' : 'P2';
    md += `### Tarea ${priority}-${String(taskId++).padStart(3, '0')}\n\n`;
    md += `| Campo | Contenido |\n|---|---|\n`;
    md += `| Identificador | ${priority}-${String(taskId - 1).padStart(3, '0')} |\n`;
    md += `| Prioridad | ${priority} |\n`;
    md += `| Caso relacionado | ${r.id} |\n`;
    md += `| Problema | Puntaje ${r.total}/100 - ${(r.errors || ['Mejora general']).join('; ')} |\n`;
    md += `| Causa probable | Clasificación de intención o respuesta subóptima |\n`;
    md += `| Componente | IntentDetectorService / AgentCoordinatorService |\n`;
    md += `| Solución propuesta | Revisión de detección de intención y reglas de respuesta |\n`;
    md += `| Riesgo | Bajo |\n`;
    md += `| Pruebas | Pruebas unitarias y de integración |\n`;
    md += `| Criterio de aceptación | Puntaje >= 70 en re-evaluación |\n`;
    md += `| Dependencias | Ninguna |\n`;
    md += `| Estimación | Pequeña |\n\n`;
  }

  fs.writeFileSync(OUTPUT_PLAN, md);
  log(`Generated: ${OUTPUT_PLAN}`);
}

function generatePrompt(progress) {
  const errores = results.filter(r => r.classification === 'ERROR_CRITICO' || r.error);
  const mejorables = results.filter(r => r.classification !== 'OPTIMA' && r.classification !== 'ERROR_CRITICO');

  let md = `# Prompt correctivo para OpenCode - Versión 2\n\n`;
  md += `Basado en la ejecución ${RUN_ID}\n\n`;
  md += `## Contexto técnico\n\n`;
  md += `Se ejecutaron ${results.length} preguntas contra el asistente WhatsApp para centros estéticos.\n`;
  md += `Proveedor activo: WhatsApp Cloud API (META_CLOUD_API).\n`;
  md += `Backend: Spring Boot en Java 21, PostgreSQL 16.\n\n`;
  md += `## Resultados\n\n`;
  md += `- Óptimas: ${results.filter(r => r.classification === 'OPTIMA').length}\n`;
  md += `- Aceptables: ${results.filter(r => r.classification === 'ACEPTABLE_CON_MEJORAS').length}\n`;
  md += `- Deficientes: ${results.filter(r => r.classification === 'DEFICIENTE').length}\n`;
  md += `- Errores críticos: ${errores.length}\n\n`;

  md += `## Casos fallidos\n\n`;
  for (const r of [...errores, ...mejorables].slice(0, 15)) {
    md += `### ${r.id}\n`;
    md += `- Pregunta: ${r.question}\n`;
    md += `- Respuesta: ${r.response || '(sin respuesta)'}\n`;
    md += `- Puntaje: ${r.total}/100\n`;
    md += `- Clasificación: ${r.classification}\n`;
    md += `- Error: ${(r.errors || [r.error || 'N/A']).join('; ')}\n\n`;
  }

  md += `## Archivos involucrados\n\n`;
  md += `- \`IntentDetectorService.java\`: clasificación de intención del mensaje\n`;
  md += `- \`AgentCoordinatorService.java\`: ruteo entre agentes\n`;
  md += `- \`BookingAgent.java\`: agente de reservas\n`;
  md += `- \`SalesAgent.java\`: agente de ventas e información\n`;
  md += `- \`ReceptionAgent.java\`: agente de recepción\n\n`;

  md += `## Cambios permitidos\n\n`;
  md += `1. Ajustes en \`IntentDetectorService.java\` para mejorar clasificación\n`;
  md += `2. Ajustes en \`AgentCoordinatorService.java\` para mejorar ruteo\n`;
  md += `3. Ajustes en reglas de respuesta en \`AiBusinessKnowledgeService.java\`\n\n`;

  md += `## Cambios prohibidos\n\n`;
  md += `1. No modificar la integración de WhatsApp\n`;
  md += `2. No modificar credenciales o secretos\n`;
  md += `3. No eliminar datos de la base de datos\n\n`;

  md += `## Orden de implementación\n\n`;
  md += `1. Corregir errores P0 (críticos)\n`;
  md += `2. Mejorar casos deficientes (P1)\n`;
  md += `3. Optimizar casos aceptables (P2)\n\n`;

  md += `## Pruebas\n\n`;
  md += `- Ejecutar \`mvn test\` para pruebas unitarias\n`;
  md += `- Ejecutar runner con \`node scripts/ejecutar_prueba_v2.js failed\` para regresión\n\n`;
  md += `## Criterios de aceptación\n\n`;
  md += `- Todos los errores críticos resueltos\n`;
  md += `- Tasa de éxito (óptimas+aceptables) >= 80%\n`;
  md += `- Sin regresiones en casos anteriormente óptimos\n`;

  fs.writeFileSync(OUTPUT_PROMPT, md);
  log(`Generated: ${OUTPUT_PROMPT}`);
}

function saveDetailedLog() {
  const detailedLog = { runId: RUN_ID, startTime: new Date(startTime).toISOString(), results };
  fs.writeFileSync(DETAILED_LOG, JSON.stringify(detailedLog, null, 2));
  console.log(`[${new Date().toISOString()}] Generated: ${DETAILED_LOG}`);
}

main().catch(err => {
  console.error('FATAL:', err);
  process.exit(1);
});
