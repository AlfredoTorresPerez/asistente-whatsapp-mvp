const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const BASE_URL = 'http://localhost:8080';
const AUTH = { email: 'admin@demo.cl', password: 'Cambiar123!' };
const PROGRESS_FILE = path.join(__dirname, '..', 'progreso_460.json');
const QUESTIONS_FILE = path.join(__dirname, '..', 'preguntas_parsed.json');
const REGISTRY_FILE = path.join(__dirname, '..', 'registro_ejecucion_IA.json');
const OUTPUT_FILE = path.join(__dirname, '..', 'preguntas_respuesta_IA.md');
const JSON_OUTPUT = path.join(__dirname, '..', 'registro_ejecucion_IA.json');

const BATCH_SIZE = 10;
const POLL_WAIT_MS = 5000;
const POLL_ATTEMPTS = 10;
const SAVE_INTERVAL = 50;

let jwtToken = null;
const QUESTIONS = [];

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function login() {
  const resp = await fetch(`${BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(AUTH)
  });
  if (!resp.ok) throw new Error(`Login failed: ${resp.status}`);
  const data = await resp.json();
  jwtToken = data.accessToken;
}

async function sendMessage(phone, body) {
  const resp = await fetch(`${BASE_URL}/api/v1/test/whatsapp-inbound`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}` },
    body: JSON.stringify({ from: phone, body })
  });
  if (!resp.ok) throw new Error(`Send failed (${resp.status})`);
}

function getDbResponse(phone) {
  const safe = phone.replace(/'/g, "''");
  try {
    const result = execSync(
      `docker exec asistente-postgres psql -U assistant -d asistente_whatsapp -t -A -c "SELECT m.body FROM message m JOIN conversation c ON m.conversation_id = c.id WHERE c.customer_phone = '${safe}' AND m.direction = 'OUTBOUND' ORDER BY m.created_at DESC LIMIT 1;"`,
      { encoding: 'utf8', timeout: 5000 }
    );
    const line = result.trim();
    if (line && line.length > 0 && !line.startsWith('(') && line !== '0') return line;
  } catch (e) {}
  return null;
}

async function processBatch(batch) {
  for (const item of batch) {
    try {
      if (item.setupContext) {
        await sendMessage(item.phone, item.setupContext);
      }
      await sendMessage(item.phone, item.text);
    } catch (err) {
      item.error = err.message;
    }
  }

  await sleep(POLL_WAIT_MS);

  for (const item of batch) {
    if (item.error) continue;
    const startTs = Date.now();
    let response = null;
    for (let attempt = 0; attempt < POLL_ATTEMPTS; attempt++) {
      response = getDbResponse(item.phone);
      if (response) break;
      await sleep(1000);
    }
    item.response = response;
    item.duration = Date.now() - startTs;
    if (!response) item.error = 'TIMEOUT';
  }
}

function classifyModality(question, registryEntry) {
  if (registryEntry && registryEntry.modalidad) return registryEntry.modalidad;
  const text = question.text.toLowerCase();
  const len = text.length;

  if (text.includes('reclam') || text.includes('problema') || text.includes('dolor') ||
      text.includes('quemadura') || text.includes('reaccion') || text.includes('irritacion') ||
      text.includes('cobraron') || text.includes('devolucion') || text.includes('duplicado') ||
      text.includes('alerg')) {
    return 'CASO_SENSIBLE';
  }

  if (text.includes('hablar con') || text.includes('persona') || text.includes('humano') ||
      text.includes('ejecutivo') || text.includes('supervisor') || text.includes('derive') ||
      text.includes('contacten') || text.includes('recepcion') || text.includes('recepcionista') ||
      text.includes('asesor') || text.includes('operador')) {
    return 'DERIVACION_HUMANA';
  }

  if (text.includes('reserv') || text.includes('agend') || text.includes('cita') ||
      text.includes('hora para') || text.includes('cupo') || text.includes('pedir hora') ||
      text.includes('anotar') || text.includes('tomar hora')) {
    if (text.includes('dispon') || text.includes('libre') || text.includes('horario') ||
        text.includes('hay hora') || text.includes('cupo disponible')) {
      return 'CONSULTA_INFORMATIVA';
    }
    return 'ACCION_TRANSACCIONAL';
  }

  if (text.includes('cancel') || text.includes('anular') || text.includes('reprogram') ||
      text.includes('mover') || text.includes('cambiar hora') || text.includes('cambiar fecha') ||
      text.includes('no voy a poder') || text.includes('no puedo asist')) {
    return 'ACCION_TRANSACCIONAL';
  }

  if (text.includes('precio') || text.includes('cuanto') || text.includes('cuesta') ||
      text.includes('vale') || text.includes('costo') || text.includes('promocion') ||
      text.includes('descuento') || text.includes('paquete') || text.includes('cotizacion') ||
      text.includes('tarifa') || text.includes('incluye') || text.includes('duracion') ||
      text.includes('cuanto dura') || text.includes('sesiones') || text.includes('resultados') ||
      text.includes('edad') || text.includes('menor') || text.includes('embaraz') ||
      text.includes('condicion medica') || text.includes('contraindic') ||
      text.includes('donde') || text.includes('direccion') || text.includes('ubicad') ||
      text.includes('sucursal') || text.includes('sede') || text.includes('llego') ||
      text.includes('abren') || text.includes('atienden') || text.includes('horario de atencion') ||
      text.includes('domingo') || text.includes('feriado') ||
      text.includes('profesional') || text.includes('especialista') || text.includes('quien') ||
      text.includes('pago') || text.includes('abono') || text.includes('boleta') ||
      text.includes('factura') || text.includes('reembolso') || text.includes('cobro') ||
      text.includes('transferencia') || text.includes('webpay') || text.includes('comprobante') ||
      text.includes('senal') || text.includes('efectivo') || text.includes('tarjeta')) {
    return 'CONSULTA_INFORMATIVA';
  }

  return 'CONTINUACION_CONVERSACIONAL';
}

function getSetupContext(question) {
  const text = question.text.toLowerCase();
  if (text.includes('otro servicio') || text.includes('otra opcion')) return 'Quiero ver que servicios ofrecen ademas de limpieza facial';
  if (text.includes('la segunda') || text.includes('el primero')) return 'Dime cuales son las opciones disponibles para tratamientos faciales';
  if (text.includes('la misma') || text.includes('con ella') || text.includes('con el')) return 'Antes me atendi con Carla en Providencia para una limpieza facial';
  if (text.includes('a la misma hora')) return 'Tengo una reserva para manana a las 15:00 en Providencia';
  if (text.includes('en la otra')) return 'En Providencia no hay cupo, quiero saber de la otra sucursal';
  if (text.includes('el tratamiento anterior')) return 'Antes me hice una limpieza facial profunda con Carla';
  if (text.includes('no quiero ese') || text.includes('no quiero cancelar')) return 'Te ofreci depilacion laser pero te parece cara';
  if (text.includes('quiero confirmar')) return 'Tengo una reserva pendiente que hice ayer';
  if (text.includes('no puedo ir')) return 'Tengo una cita para manana a las 15:00';
  if (text.includes('necesito cambiar')) return 'Tengo una reserva para el miercoles a las 16:00';
  if (text.includes('quiero ver')) return 'Quiero saber que disponibilidad tienen esta semana';
  if (text.includes('todavia sirve')) return 'Recibi un enlace de confirmacion ayer por whatsapp';
  if (text.includes('esta listo')) return 'Hice una reserva ayer y quiero saber si se confirmo';
  if (text.includes('para despues')) return 'Quiero agendar un servicio para diciembre';
  if (text.includes('pero no ahora')) return 'Quiero agendar para febrero del proximo año';
  if (text.includes('deseo contactar')) return 'Necesito ayuda con un problema de mi ultima visita';
  if (text.includes('falta muy poco') || text.includes('revisaste') || text.includes('revisaron') || text.includes('que paso')) return 'Hice una reserva la semana pasada para este sabado y quiero revisar';
  if (text.includes('que mas puedo')) return 'Ya me hice una limpieza facial la semana pasada';
  if (text.includes('que incluye')) return 'Estoy interesada en el tratamiento de limpieza facial';
  if (text === 'si') return 'Te pregunte si quieres agendar una hora para masaje relajante';
  if (text === 'no') return 'Te di un horario disponible para el miercoles a las 16:00';
  if (text === 'ok') return 'Te propuse agendar una limpieza facial para el miercoles a las 15:00';
  if (text === 'gracias') return 'Acabo de confirmar mi reserva para el viernes a las 11:00';
  return 'Hola necesito informacion sobre sus servicios de estetica';
}

async function main() {
  console.log('=== EJECUCION EN MASA DE 460 CONSULTAS ===\n');

  const questions = JSON.parse(fs.readFileSync(QUESTIONS_FILE, 'utf8'));
  const registry = JSON.parse(fs.readFileSync(REGISTRY_FILE, 'utf8'));
  const registryMap = {};
  registry.forEach(r => { registryMap[r.id] = r; });

  console.log(`Preguntas cargadas: ${questions.length}`);

  let progress = { completed: [], responses: {}, lastSeq: 0 };
  if (fs.existsSync(PROGRESS_FILE)) {
    progress = JSON.parse(fs.readFileSync(PROGRESS_FILE, 'utf8'));
    console.log(`Progreso cargado: ${Object.keys(progress.responses).length}/${questions.length}`);
  }

  await login();
  console.log('Login OK\n');

  const todo = questions.filter((q, i) => {
    const qId = `P${String(q.number).padStart(3, '0')}`;
    return !progress.completed.includes(qId);
  });

  console.log(`Pendientes: ${todo.length}/${questions.length}\n`);

  const startTime = Date.now();

  for (let i = 0; i < todo.length; i += BATCH_SIZE) {
    const batch = todo.slice(i, i + BATCH_SIZE).map(q => {
      const qId = `P${String(q.number).padStart(3, '0')}`;
      const phone = `+5691000${String(q.number).padStart(4, '0')}`;
      const modality = classifyModality(q, registryMap[qId]);
      const needsContext = modality === 'CONTINUACION_CONVERSACIONAL';
      const setupContext = needsContext ? getSetupContext(q) : null;
      return { ...q, qId, phone, modality, setupContext, response: null, duration: 0, error: null };
    });

    const batchNum = Math.floor(i / BATCH_SIZE) + 1;
    const totalBatches = Math.ceil(todo.length / BATCH_SIZE);
    console.log(`[${batchNum}/${totalBatches}] Enviando lote ${i+1}-${Math.min(i+BATCH_SIZE, todo.length)}...`);

    await processBatch(batch);

    for (const item of batch) {
      progress.responses[item.qId] = {
        pregunta: item.text,
        respuesta: item.response,
        phone: item.phone,
        modality: item.modality,
        setupContext: item.setupContext,
        duration: item.duration,
        error: item.error
      };
      progress.completed.push(item.qId);

      const status = item.error === 'TIMEOUT' ? 'TIMEOUT' : item.error ? 'ERROR' : 'OK';
      process.stdout.write(`  ${item.qId}: ${status}${item.response ? ' (' + item.response.substring(0, 60) + '...)' : ''}\n`);
    }

    if (progress.completed.length % SAVE_INTERVAL < BATCH_SIZE || batchNum === totalBatches) {
      progress.lastSeq = i + BATCH_SIZE;
      fs.writeFileSync(PROGRESS_FILE, JSON.stringify(progress, null, 2));
      const elapsed = ((Date.now() - startTime) / 1000 / 60).toFixed(1);
      console.log(`[CHECKPOINT] ${progress.completed.length}/${questions.length} - ${elapsed}min`);
    }
    console.log('');
  }

  const totalTime = ((Date.now() - startTime) / 1000 / 60).toFixed(1);
  console.log(`=== EJECUCION COMPLETA (${totalTime}min) ===`);
  console.log(`Completados: ${progress.completed.length}/${questions.length}`);

  progress.lastSeq = questions.length;
  fs.writeFileSync(PROGRESS_FILE, JSON.stringify(progress, null, 2));

  generarOutputs(questions, progress, registry);
}

function generarOutputs(questions, progress, registry) {
  const results = questions.map(q => {
    const qId = `P${String(q.number).padStart(3, '0')}`;
    const resp = progress.responses[qId];
    return {
      id: qId,
      number: q.number,
      section: q.section,
      pregunta: q.text,
      expectedIntent: q.expectedIntent,
      respuesta: resp ? resp.respuesta : null,
      phone: resp ? resp.phone : null,
      modality: resp ? resp.modality : 'UNKNOWN',
      setupContext: resp ? resp.setupContext : null,
      duration: resp ? resp.duration : 0,
      error: resp ? resp.error : null
    };
  });

  const totalWithResponse = results.filter(r => r.respuesta !== null).length;
  const totalWithout = results.filter(r => r.respuesta === null && !r.error).length;
  const totalError = results.filter(r => r.error && r.error !== 'TIMEOUT').length;
  const totalTimeout = results.filter(r => r.error === 'TIMEOUT').length;

  let md = `# Preguntas y respuestas reales de la IA

- Total esperado: 460
- Total ejecutado: ${results.length}
- Total con respuesta: ${totalWithResponse}
- Total sin respuesta: ${totalWithout}
- Total con error: ${totalError}
- Total timeout: ${totalTimeout}
- Empresa de prueba: Centro Estetico Bella
- Nombre del asistente: Asistente del Centro Estético
- Fecha controlada: 2026-07-27 12:04
- Zona horaria: America/Santiago 2026-07-27 20:15 CLT (hora simulada: 12:04)
- Versión evaluada: workspace-local

| Pregunta cliente | Respuesta IA |
|---|---|
`;

  for (const r of results) {
    const response = r.respuesta
      ? r.respuesta.replace(/\|/g, '\\|').replace(/\n/g, '<br>')
      : (r.error === 'TIMEOUT' ? 'TIEMPO DE RESPUESTA EXCEDIDO'
        : r.error ? `ERROR TÉCNICO: ${r.error.substring(0,80)}`
        : 'SIN RESPUESTA');
    md += `| [${r.id}] ${r.pregunta} | Asistente del Centro Estético: ${response} |\n`;
  }

  fs.writeFileSync(OUTPUT_FILE, md);
  console.log(`\nGenerado: ${OUTPUT_FILE}`);

  const jsonRegistry = results.map(r => {
    const old = registry.find(e => e.id === r.id) || {};
    return {
      id: r.id,
      pregunta: r.pregunta,
      modalidad: r.modality || old.modalidad || 'INDEPENDIENTE',
      contextoAplicado: r.setupContext || old.contextoAplicado || null,
      nombreAsistente: 'Asistente del Centro Estético',
      respuestaExacta: r.respuesta || old.respuestaExacta || null,
      intencionDetectada: old.intencionDetectada || null,
      intencionSecundaria: old.intencionSecundaria || null,
      confianza: old.confianza || null,
      agenteSeleccionado: old.agenteSeleccionado || null,
      estadoAnterior: old.estadoAnterior || null,
      estadoPosterior: old.estadoPosterior || null,
      entidadesDetectadas: old.entidadesDetectadas || {},
      datoEsperado: old.datoEsperado || null,
      derivacionHumana: old.derivacionHumana || false,
      duracionMilisegundos: r.duration || old.duracionMilisegundos || 0,
      resultadoTecnico: r.error ? 'ERROR' : (r.respuesta ? 'OK' : 'NO_RESPONSE'),
      error: r.error || null
    };
  });

  fs.writeFileSync(JSON_OUTPUT, JSON.stringify(jsonRegistry, null, 2));
  console.log(`Generado: ${JSON_OUTPUT}`);

  console.log(`\n=== RESUMEN FINAL ===`);
  console.log(`Total: ${results.length}`);
  console.log(`Con respuesta: ${totalWithResponse}`);
  console.log(`Sin respuesta: ${totalWithout}`);
  console.log(`Errores: ${totalError}`);
  console.log(`Timeouts: ${totalTimeout}`);
}

main().catch(err => {
  console.error('FATAL:', err.message);
  process.exit(1);
});
