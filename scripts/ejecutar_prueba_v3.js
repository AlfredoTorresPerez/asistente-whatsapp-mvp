const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const XLSX = require('xlsx');
const evaluator = require('./evaluador_semantico_v2.js');

const BASE_URL = process.env.WHATSAPP_TEST_API_URL || 'http://localhost:8080';
const COMPANY_NUMBER = process.env.WHATSAPP_TEST_COMPANY_NUMBER || '56927305158';
const CLIENT_NUMBER = process.env.WHATSAPP_TEST_CLIENT_NUMBER || '56950954580';
const RESPONSE_TIMEOUT_MS = (parseInt(process.env.WHATSAPP_TEST_RESPONSE_TIMEOUT_SECONDS) || 90) * 1000;
const DELAY_MIN_MS = (parseInt(process.env.WHATSAPP_TEST_DELAY_MIN_SECONDS) || 2) * 1000;
const DELAY_MAX_MS = (parseInt(process.env.WHATSAPP_TEST_DELAY_MAX_SECONDS) || 4) * 1000;
const MAX_RETRIES = parseInt(process.env.WHATSAPP_TEST_MAX_RETRIES) || 2;
const POLL_INTERVAL_MS = 1500;
const DUPLICATE_RESPONSE_REJECTION = process.env.WHATSAPP_TEST_REJECT_DUPLICATES !== 'false';

const RUN_ID = 'PRUEBA_IA_V3_' + new Date().toISOString().replace(/[-:]/g, '').slice(0, 15);
const ROOT_DIR = path.join(__dirname, '..');
const EXCEL_PATH = path.join(ROOT_DIR, 'preguntas_respuesta_IA_version_2_corregido.xlsx');
const PROGRESS_FILE = path.join(ROOT_DIR, `progreso_v3_${RUN_ID.split('_').pop()}.json`);
const OUTPUT_DIR = path.join(ROOT_DIR, 'resultados');
const TIMESTAMP_DIR = path.join(OUTPUT_DIR, RUN_ID);

let jwtToken = null;
let questions = [];
let results = [];
let startTime = null;
let lastResponseText = null;

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }
function log(msg) { console.log(`[${new Date().toISOString()}] ${msg}`); }
function sid(q) { return q.id ? q.id.padStart(5, ' ') : '??'; }

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

function generateExternalId(questionId, seq) {
  const ts = Date.now();
  return `test-${RUN_ID}-${questionId}-${seq}-${ts}`;
}

async function sendMessage(from, body, externalMessageId, retry = 0) {
  try {
    const resp = await fetch(`${BASE_URL}/api/v1/test/whatsapp-inbound`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}` },
      body: JSON.stringify({ from, body, sessionKey: 'demo-sales', externalMessageId })
    });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const data = await resp.json();
    return { ok: true, deliveryId: data.deliveryId || null, status: data.status || null };
  } catch (err) {
    if (retry < MAX_RETRIES) {
      log(`  Retry ${retry + 1}/${MAX_RETRIES} send: ${err.message}`);
      await sleep(3000);
      return sendMessage(from, body, externalMessageId, retry + 1);
    }
    return { ok: false, error: err.message };
  }
}

function queryDbResponse(phone, sentTimestamp, externalMessageId) {
  const safePhone = phone.replace(/'/g, "''");
  const safePhoneWithPrefix = ('+' + phone).replace(/'/g, "''");
  try {
    const d = new Date(sentTimestamp);
    const iso = d.toISOString().replace('T', ' ').replace(/\.\d+Z/, '+00');
    const sql = `SELECT body FROM message WHERE direction='OUTBOUND' AND conversation_id=(SELECT id FROM conversation WHERE customer_phone IN ('${safePhone}','${safePhoneWithPrefix}') ORDER BY created_at DESC LIMIT 1) AND created_at > '${iso}'::timestamptz ORDER BY created_at ASC LIMIT 1;`;
    const cmd = `docker compose exec -T postgres psql -U assistant -d asistente_whatsapp -t -A -c "` + sql.replace(/"/g, '""') + `"`;
    const result = execSync(cmd, { encoding: 'utf8', timeout: 15000, cwd: ROOT_DIR });
    const line = result.toString().trim();
    if (line && line.length > 0 && !line.startsWith('(') && line !== '0') {
      return line;
    }
  } catch (e) { log(`  DB error: ${(e.message || '').substring(0, 200)}`); }
  return null;
}

function isDuplicateResponse(text) {
  if (!DUPLICATE_RESPONSE_REJECTION || !lastResponseText || !text) return false;
  const a = text.trim().toLowerCase();
  const b = lastResponseText.trim().toLowerCase();
  if (a === b) {
    log(`  DUPLICATE DETECTED: same response as previous question`);
    return true;
  }
  return false;
}

function loadCorregidoExcel() {
  const xlsxPath = EXCEL_PATH;
  if (!fs.existsSync(xlsxPath)) {
    log(`Corregido Excel not found at ${xlsxPath}, trying original...`);
    const origPath = path.join(ROOT_DIR, 'preguntas_respuesta_IA_version_2.xlsx');
    if (!fs.existsSync(origPath)) throw new Error('No Excel file found');
    return loadExcel(origPath);
  }
  return loadExcel(xlsxPath);
}

function loadExcel(filePath) {
  log(`Reading: ${filePath}`);
  const wb = XLSX.readFile(filePath);
  const ws = wb.Sheets['Preguntas_Respuestas'];
  const rows = XLSX.utils.sheet_to_json(ws);
  questions = rows.map((r, i) => ({
    id: r.ID || `P${String(i + 1).padStart(3, '0')}`,
    text: (r['Pregunta cliente'] || '').trim(),
    expectedResponse: r['Respuesta IA'] || null,
    excluded: (r['Excluido del prompt pagos/cobros'] || 'NO').toString().toUpperCase().trim() === 'SÍ',
    expectedIntent: r['Intencion esperada'] || null,
    category: r['Categoria'] || null,
    scenario: r['Escenario'] || null,
    seq: i + 1
  })).filter(q => q.text.length > 0);
  log(`Loaded ${questions.length} questions (${questions.filter(q => q.excluded).length} excluded)`);
}

function loadProgress() {
  if (!fs.existsSync(PROGRESS_FILE)) return { completed: [], responses: {}, lastSeq: 0 };
  return JSON.parse(fs.readFileSync(PROGRESS_FILE, 'utf8'));
}

function saveProgress(progress) {
  fs.writeFileSync(PROGRESS_FILE, JSON.stringify(progress, null, 2));
}

function getResponseForQuestion(questionId, progress) {
  return progress.responses[questionId] || null;
}

async function processQuestion(q, progress) {
  const qId = q.id;
  const existing = getResponseForQuestion(qId, progress);
  if (existing) {
    log(`${sid(q)} SKIP (already in progress)`);
    results.push(existing);
    if (existing.response) lastResponseText = existing.response;
    return existing;
  }

  if (q.excluded) {
    log(`${sid(q)} SKIP (excluded: payments/billing)`);
    const result = { id: qId, question: q.text, expectedResponse: q.expectedResponse,
      response: null, sendTime: null, responseTime: null, duration: 0,
      error: null, technicalStatus: 'EXCLUDED', functionalResult: 'EXCLUIDO',
      scores: null, total: null, classification: 'EXCLUIDO', errors: [],
      expectedIntent: q.expectedIntent, category: q.category, scenario: q.scenario };
    results.push(result);
    progress.completed.push(qId);
    progress.responses[qId] = result;
    progress.lastSeq = q.seq;
    return result;
  }

  const externalId = generateExternalId(qId, q.seq);
  const beforeTimestamp = new Date().toISOString();
  const sendTime = beforeTimestamp;

  log(`${sid(q)} "${q.text.slice(0, 65)}..."`);
  const sendResult = await sendMessage(CLIENT_NUMBER, q.text, externalId);

  if (!sendResult.ok) {
    log(`${sid(q)} SEND FAILED: ${sendResult.error}`);
    const result = { id: qId, question: q.text, expectedResponse: q.expectedResponse,
      response: null, sendTime, responseTime: null, duration: 0,
      error: `SEND_FAILED: ${sendResult.error}`, technicalStatus: 'SEND_ERROR',
      functionalResult: 'ERROR_CRITICO', scores: null, total: 0, classification: 'ERROR_CRITICO',
      errors: ['Error de envío'], externalMessageId: externalId,
      expectedIntent: q.expectedIntent, category: q.category, scenario: q.scenario };
    results.push(result);
    progress.completed.push(qId);
    progress.responses[qId] = result;
    progress.lastSeq = q.seq;
    return result;
  }

  const pollStart = Date.now();
  let response = null;
  let pollAttempts = 0;

  while (Date.now() - pollStart < RESPONSE_TIMEOUT_MS) {
    await sleep(POLL_INTERVAL_MS);
    pollAttempts++;
    response = queryDbResponse(CLIENT_NUMBER, beforeTimestamp, externalId);
      if (response) {
        if (!isDuplicateResponse(response)) break;
        log(`  ACCEPTING duplicate response anyway`);
        break;
      }
  }

  const responseTime = new Date().toISOString();
  const duration = Date.now() - pollStart;

  if (!response) {
    log(`${sid(q)} TIMEOUT (${duration}ms, ${pollAttempts} polls)`);
    const result = { id: qId, question: q.text, expectedResponse: q.expectedResponse,
      response: null, sendTime, responseTime, duration, error: 'TIMEOUT',
      technicalStatus: 'TIMEOUT', functionalResult: 'ERROR_CRITICO',
      scores: null, total: 0, classification: 'ERROR_CRITICO',
      errors: ['No se recibió respuesta en el tiempo máximo'],
      externalMessageId: externalId,
      expectedIntent: q.expectedIntent, category: q.category, scenario: q.scenario };
    results.push(result);
    progress.completed.push(qId);
    progress.responses[qId] = result;
    progress.lastSeq = q.seq;
    return result;
  }

  log(`${sid(q)} OK (${duration}ms) "${response.slice(0, 70)}..."`);
  lastResponseText = response;

  const result = { id: qId, question: q.text, expectedResponse: q.expectedResponse,
    response, sendTime, responseTime, duration, error: null,
    technicalStatus: 'OK', functionalResult: 'PENDIENTE_EVALUACION',
    scores: null, total: null, classification: 'PENDIENTE_EVALUACION', errors: [],
    externalMessageId: externalId,
    expectedIntent: q.expectedIntent, category: q.category, scenario: q.scenario };
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

  fs.mkdirSync(TIMESTAMP_DIR, { recursive: true });
  loadCorregidoExcel();
  const progress = loadProgress();
  log(`Progress: ${progress.completed.length}/${questions.length}`);

  await login();

  const filterMode = process.argv[2] || 'all';
  let filtered = questions;

  if (filterMode === 'failed') {
    filtered = questions.filter(q => {
      const r = progress.responses[q.id];
      return !r || r.error || r.classification === 'ERROR_CRITICO' || r.classification === 'DEFICIENTE';
    });
  } else if (filterMode.startsWith('range:')) {
    const [start, end] = filterMode.slice(6).split('-').map(Number);
    filtered = questions.filter(q => q.seq >= start && q.seq <= end);
  } else if (filterMode.startsWith('count:')) {
    const maxCount = parseInt(filterMode.slice(6));
    filtered = questions.filter(q => !progress.completed.includes(q.id)).slice(0, maxCount);
  } else if (filterMode === 'all') {
    filtered = questions.filter(q => !q.excluded);
  }

  const toProcess = filtered.filter(q => !progress.completed.includes(q.id) && !q.excluded);
  log(`To process: ${toProcess.length} new (${filtered.length} in scope)`);

  Object.values(progress.responses).forEach(r => {
    if (!results.some(x => x.id === r.id)) results.push(r);
  });

  startTime = Date.now();

  for (let i = 0; i < toProcess.length; i++) {
    const q = toProcess[i];
    log(`[${i + 1}/${toProcess.length}]`);
    await processQuestion(q, progress);
    const delay = DELAY_MIN_MS + Math.random() * (DELAY_MAX_MS - DELAY_MIN_MS);
    if (i < toProcess.length - 1) await sleep(delay);
  }

  const totalDuration = Date.now() - startTime;
  log(`\nProcessed ${toProcess.length} questions in ${Math.round(totalDuration / 1000)}s`);
  saveProgress(progress);

  log('Evaluating results...');
  const evaluatedResults = evaluator.evaluateResults(results);
  progress.evaluatedResults = evaluatedResults;
  saveProgress(progress);

  generateReports(evaluatedResults);
  log('Done!');
}

function generateReports(evaluatedResults) {
  fs.writeFileSync(
    path.join(TIMESTAMP_DIR, 'registro_completo.json'),
    JSON.stringify({ runId: RUN_ID, startTime: new Date(startTime).toISOString(), results, evaluatedResults }, null, 2)
  );

  evaluator.generateEvaluationReport(evaluatedResults, path.join(TIMESTAMP_DIR, 'informe_evaluacion.md'));

  const excluded = results.filter(r => r.classification === 'EXCLUIDO');
  const errores = results.filter(r => r.classification === 'ERROR_CRITICO');
  const noEval = results.filter(r => r.classification === 'PENDIENTE_EVALUACION');
  const evaluated = evaluatedResults || results.filter(r => r.total !== null && r.total !== undefined && r.classification !== 'EXCLUIDO' && r.classification !== 'ERROR_CRITICO' && r.classification !== 'PENDIENTE_EVALUACION');

  const md = [
    `# Resultados ejecución ${RUN_ID}`,
    '',
    `- Total preguntas: ${questions.length}`,
    `- Excluidas (pagos): ${excluded.length}`,
    `- Procesadas: ${results.length - excluded.length}`,
    `- Evaluadas: ${evaluated.length}`,
    `- Pendientes evaluación: ${noEval.length}`,
    `- Errores críticos: ${errores.length}`,
    `- Tiempo total: ${Math.round((Date.now() - startTime) / 1000)}s`,
    '',
    `## Detalle`,
    '',
    ...results.map(r => {
      const qShort = (r.question || '').length > 60 ? r.question.slice(0, 60) + '...' : (r.question || '');
      return [
        `### ${r.id}`,
        `- Pregunta: ${qShort}`,
        `- Respuesta: ${r.response || '(sin respuesta)'}`,
        `- Estado: ${r.technicalStatus}`,
        r.total !== null ? `- Puntaje: ${r.total}/100` : '',
        r.classification ? `- Clasificación: ${r.classification}` : '',
        r.excluded ? `- Excluido: pagos/cobros` : '',
        r.error ? `- Error: ${r.error}` : '',
        r.expectedIntent ? `- Intención esperada: ${r.expectedIntent}` : '',
        r.duration ? `- Duración: ${r.duration}ms` : '',
        ''
      ].filter(Boolean).join('\n');
    })
  ].join('\n');
  fs.writeFileSync(path.join(TIMESTAMP_DIR, 'resultados.md'), md);

  const summary = [
    `# Resumen ejecución ${RUN_ID}`,
    '',
    `- **Total**: ${results.length}`,
    `- **Excluidas**: ${excluded.length}`,
    `- **Errores críticos**: ${errores.length}`,
    `- **Pendientes evaluación**: ${noEval.length}`,
    `- **Requieren evaluador semántico (Fase 2)**: ${noEval.length} casos`,
    ''
  ].join('\n');
  fs.writeFileSync(path.join(TIMESTAMP_DIR, 'resumen.md'), summary);

  log(`Reports in ${TIMESTAMP_DIR}`);
}

main().catch(err => {
  console.error('FATAL:', err);
  process.exit(1);
});
