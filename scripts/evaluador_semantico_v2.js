const fs = require('fs');
const path = require('path');

function normalize(text) {
  if (!text) return '';
  return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

function containsAny(text, words) {
  if (!text) return false;
  const n = normalize(text);
  return words.some(w => n.includes(normalize(w)));
}

const GREETING_TEXT = normalize("Hola, gracias por escribirnos. ¿Te ayudo con servicios, precios o agenda?");
const CATALOG_TEXT = normalize("Puedo ayudarte con información del catálogo, pero necesito el servicio específico que quieres revisar.");
const CATALOG_TEXT2 = normalize("Perfecto, puedo ayudarte. ¿Qué producto o servicio estás buscando exactamente?");

const GENERIC_RESPONSES = [GREETING_TEXT, CATALOG_TEXT, CATALOG_TEXT2];

const INTENT_KEYWORDS = {
  greeting: ['hola', 'buenas', 'buen dia', 'buenos dias', 'ayuda'],
  booking: ['agendar', 'reservar', 'cita', 'hora', 'turno', 'cupo', 'reserva'],
  cancel: ['cancelar', 'anular', 'cancel', 'anula'],
  reschedule: ['reprogramar', 'cambiar hora', 'reagendar', 'mover'],
  price: ['precio', 'cuanto cuesta', 'cuanto vale', 'tarifa', 'cuesta', 'sale', 'valor'],
  service_info: ['incluye', 'duracion', 'tratamiento', 'servicio', 'informacion'],
  location: ['donde queda', 'direccion', 'ubicacion', 'sucursal'],
  schedule: ['horario', 'abren', 'atienden', 'sabado', 'domingo'],
  complaint: ['reclamo', 'problema', 'queja', 'molesto', 'conforme'],
  human: ['persona', 'ejecutivo', 'supervisor', 'hablar con', 'humano', 'asesor'],
  sensitive: ['quemadura', 'dolor', 'reaccion', 'alergia', 'irritacion', 'ardor'],
  status: ['tengo agendado', 'mi reserva', 'mis citas', 'confirmar'],
  professional: ['profesional', 'carla', 'quien realiza'],
  thanks: ['gracias', 'eso era todo', 'chao', 'hasta luego'],
  availability: ['disponibilidad', 'horas libres', 'hay hora', 'tienen hora']
};

function detectExpectedIntent(question) {
  const nq = normalize(question);
  const matches = [];
  for (const [intent, words] of Object.entries(INTENT_KEYWORDS)) {
    if (containsAny(nq, words)) matches.push(intent);
  }
  if (matches.length === 0) return 'ambiguous';
  return matches.join('+');
}

function evaluate(question, response) {
  const nq = normalize(question);
  const nr = normalize(response);
  const qWords = nq.split(/\s+/).length;
  const rWords = nr.split(/\s+/).length;

  const scores = {
    comprehension: 0,
    accuracy: 0,
    action: 0,
    safety: 0,
    context: 0,
    clarity: 0,
    tone: 0
  };

  const errors = [];
  const warnings = [];
  const expectedIntent = detectExpectedIntent(question);
  const isGenericGreeting = GENERIC_RESPONSES.includes(nr);
  const isCatalogOnly = nr === CATALOG_TEXT || nr === CATALOG_TEXT2;

  // --- Critical error detection ---
  const hasSensitive = containsAny(nq, ['quemadura', 'dolor fuerte', 'reaccion', 'irritacion', 'ardor', 'alergia']);
  const hasComplaint = containsAny(nq, ['reclamo', 'problema', 'queja', 'molesto', 'conforme']);
  const hasHumanRequest = containsAny(nq, ['persona', 'humano', 'supervisor', 'ejecutivo', 'hablar con', 'asesor']);
  const hasBookingIntent = containsAny(nq, ['agendar', 'reservar', 'cita', 'hora', 'turno', 'cupo']);
  const hasCancelIntent = containsAny(nq, ['cancelar', 'anular']);
  const hasPriceIntent = containsAny(nq, ['precio', 'cuanto cuesta', 'cuanto vale', 'tarifa']);
  const hasLocationIntent = containsAny(nq, ['donde queda', 'direccion', 'ubicacion', 'sucursal']);
  const hasScheduleIntent = containsAny(nq, ['horario', 'abren', 'atienden']);

  // CRITICAL: Responding with catalog/greeting for complaints, sensitive issues
  if ((hasSensitive || hasComplaint) && isGenericGreeting) {
    return {
      total: 0, classification: 'ERROR_CRITICO',
      scores: { comprehension: 0, accuracy: 0, action: 0, safety: 0, context: 0, clarity: 0, tone: 0 },
      errors: ['ERROR_CRITICO: respuesta genérica para caso sensible/reclamo'],
      expectedIntent, responseType: 'generic_greeting'
    };
  }

  // CRITICAL: Never showing empathy for sensitive cases
  if (hasSensitive && !containsAny(nr, ['lamento', 'derivare', 'persona del equipo', 'urgencia', 'empatia', 'sentimos', 'consulta con un profesional', 'molestias'])) {
    errors.push('ERROR_CRITICO: no muestra empatía ni deriva en caso sensible');
  }

  // CRITICAL: Not routing human requests
  if (hasHumanRequest && isGenericGreeting) {
    return {
      total: 0, classification: 'ERROR_CRITICO',
      scores: { comprehension: 0, accuracy: 0, action: 0, safety: 0, context: 0, clarity: 0, tone: 0 },
      errors: ['ERROR_CRITICO: solicitud humana respondida con saludo genérico'],
      expectedIntent, responseType: 'generic_greeting'
    };
  }

  // CRITICAL: Not routing to human for human requests
  if (hasHumanRequest && !containsAny(nr, ['derivare', 'persona del equipo', 'derivar', 'humano', 'ejecutivo', 'supervisor'])) {
    errors.push('ERROR_CRITICO: no deriva solicitud humana a persona');
  }

  // CRITICAL: Inventing information
  const hasPriceInResponse = containsAny(nr, ['$', 'precio', 'valor']);
  const hasScheduleInResponse = containsAny(nr, ['horario', 'abrimos', 'atendemos']);
  if (!hasPriceIntent && hasPriceInResponse && !isGenericGreeting && !containsAny(nr, ['necesito el servicio', 'que servicio', 'producto o servicio'])) {
    errors.push('ERROR_CRITICO: posible invención de precio no solicitado');
  }

  // --- 1. Comprehension (20 pts) ---
  if (isGenericGreeting) {
    // Generic greeting shows complete lack of comprehension
    scores.comprehension = 2;
    errors.push('Comprensión deficiente: respuesta genérica sin distinguir intención');
  } else if (hasSensitive && containsAny(nr, ['lamento', 'derivare', 'urgencia', 'sentimos'])) {
    scores.comprehension = 20;
  } else if (hasHumanRequest && containsAny(nr, ['derivare', 'persona'])) {
    scores.comprehension = 20;
  } else if (hasBookingIntent && containsAny(nr, ['servicio', 'agendar', 'reservar', 'hora', 'cita'])) {
    scores.comprehension = 18;
  } else if (hasCancelIntent && containsAny(nr, ['cancel', 'anular', 'reserva'])) {
    scores.comprehension = 18;
  } else if (hasPriceIntent && containsAny(nr, ['precio', 'servicio', 'cuesta', '$'])) {
    scores.comprehension = 18;
  } else if (hasLocationIntent && containsAny(nr, ['sucursal', 'direccion', 'ubicacion'])) {
    scores.comprehension = 18;
  } else if (hasScheduleIntent && containsAny(nr, ['horario', 'sucursal'])) {
    scores.comprehension = 18;
  } else if (containsAny(nr, ['servicio', 'ayudar', 'informacion'])) {
    scores.comprehension = 10;
  } else if (rWords > 3) {
    scores.comprehension = 8;
  } else {
    scores.comprehension = 2;
    errors.push('Comprensión deficiente');
  }

  // --- 2. Accuracy / Functional correctness (20 pts) ---
  if (isGenericGreeting || isCatalogOnly) {
    scores.accuracy = 0;
    if (!errors.some(e => e.includes('ERROR_CRITICO'))) {
      errors.push('Respuesta no funcional: saludo o catálogo sin acción concreta');
    }
  } else if (hasSensitive && containsAny(nr, ['derivare'])) {
    scores.accuracy = 20;
  } else if (containsAny(nr, ['servicio', 'agendar', 'reserva'])) {
    scores.accuracy = 16;
  } else if (containsAny(nr, ['precio', 'servicio', 'cuesta']) && !isGenericGreeting) {
    scores.accuracy = 16;
  } else if (rWords > 10) {
    scores.accuracy = 12;
  } else {
    scores.accuracy = 4;
  }

  // --- 3. Action / Next step (20 pts) ---
  const hasQuestion = nr.includes('¿') || nr.includes('?');
  const hasOffering = containsAny(nr, ['quieres', 'gustaria', 'dime', 'indica', 'puedo ayudarte']);
  if (isGenericGreeting || isCatalogOnly) {
    scores.action = 6;
    if (!errors.some(e => e.includes('ERROR_CRITICO'))) {
      errors.push('Baja acción: solo saludo/catálogo');
    }
  } else if (hasQuestion && hasOffering) {
    scores.action = 20;
  } else if (hasQuestion) {
    scores.action = 16;
  } else if (hasOffering) {
    scores.action = 14;
  } else if (hasSensitive && containsAny(nr, ['derivare'])) {
    scores.action = 18;
  } else {
    scores.action = 6;
    errors.push('Sin acción ni siguiente paso');
  }

  // --- 4. Safety / No invention (15 pts) ---
  if (hasSensitive && containsAny(nr, ['derivare', 'persona del equipo'])) {
    scores.safety = 15;
  } else if (hasHumanRequest && containsAny(nr, ['derivare', 'persona'])) {
    scores.safety = 15;
  } else if (isGenericGreeting) {
    scores.safety = 12; // Generic greeting is safe but useless
  } else if (containsAny(nr, ['servicio', 'ayudar'])) {
    scores.safety = 14;
  } else {
    scores.safety = 10;
  }

  // --- 5. Context usage (10 pts) ---
  if (isGenericGreeting) {
    scores.context = 2;
  } else if (containsAny(nr, ['servicio', 'agendar', 'reserva', 'cancel', 'precio', 'sucursal', 'horario'])) {
    scores.context = 8;
  } else if (rWords > 10) {
    scores.context = 6;
  } else {
    scores.context = 3;
  }

  // --- 6. Clarity for WhatsApp (10 pts) ---
  if (rWords >= 10 && rWords <= 50 && hasQuestion) {
    scores.clarity = 10;
  } else if (rWords >= 5 && rWords <= 80) {
    scores.clarity = 8;
  } else if (rWords > 80) {
    scores.clarity = 4;
    warnings.push('Respuesta muy extensa para WhatsApp');
  } else {
    scores.clarity = 5;
  }

  // --- 7. Tone (5 pts) ---
  if (hasSensitive && containsAny(nr, ['lamento', 'sentimos', 'empatia'])) {
    scores.tone = 5;
  } else if (isGenericGreeting) {
    scores.tone = 4; // Generic greeting has decent tone
  } else if (containsAny(nr, ['gracias', 'hola', 'claro', 'perfecto', 'encantada'])) {
    scores.tone = 5;
  } else {
    scores.tone = 3;
  }

  const total = Object.values(scores).reduce((a, b) => a + b, 0);

  let classification;
  if (errors.some(e => e.includes('ERROR_CRITICO'))) {
    classification = 'ERROR_CRITICO';
  } else if (total >= 85) {
    classification = 'OPTIMA';
  } else if (total >= 65) {
    classification = 'ACEPTABLE_CON_MEJORAS';
  } else {
    classification = 'DEFICIENTE';
  }

  return {
    total,
    classification,
    scores,
    errors: errors.length > 0 ? errors : undefined,
    warnings: warnings.length > 0 ? warnings : undefined,
    expectedIntent,
    responseType: isGenericGreeting ? 'generic_greeting' : isCatalogOnly ? 'catalog_only' : 'specific'
  };
}

// --- Main evaluator ---
function evaluateResults(results) {
  return results.map(r => {
    if (!r.response || r.technicalStatus !== 'OK') {
      return { ...r, total: 0, classification: 'ERROR_CRITICO', scores: null, errors: ['Sin respuesta'] };
    }
    const evalResult = evaluate(r.question, r.response);
    return {
      ...r,
      total: evalResult.total,
      classification: evalResult.classification,
      scores: evalResult.scores,
      errors: evalResult.errors || [],
      warnings: evalResult.warnings || [],
      expectedIntent: evalResult.expectedIntent,
      responseType: evalResult.responseType
    };
  });
}

function generateEvaluationReport(evaluated, outputPath) {
  const optimas = evaluated.filter(r => r.classification === 'OPTIMA');
  const aceptables = evaluated.filter(r => r.classification === 'ACEPTABLE_CON_MEJORAS');
  const deficientes = evaluated.filter(r => r.classification === 'DEFICIENTE');
  const errores = evaluated.filter(r => r.classification === 'ERROR_CRITICO');
  const excluidas = evaluated.filter(r => r.technicalStatus === 'EXCLUDED');

  const successful = evaluated.filter(r => r.technicalStatus === 'OK');
  const avgScore = successful.length > 0
    ? Math.round(successful.reduce((s, r) => s + (r.total || 0), 0) / successful.length)
    : 0;

  let md = `# Evaluación semántica v2\n\n`;
  md += `## Resumen\n\n`;
  md += `| Métrica | Valor |\n|---|---|\n`;
  md += `| Total evaluados | ${evaluated.length} |\n`;
  md += `| Óptimas (>=85) | ${optimas.length} (${evaluated.length ? Math.round(optimas.length / evaluated.length * 100) : 0}%) |\n`;
  md += `| Aceptables (65-84) | ${aceptables.length} (${evaluated.length ? Math.round(aceptables.length / evaluated.length * 100) : 0}%) |\n`;
  md += `| Deficientes (<65) | ${deficientes.length} (${evaluated.length ? Math.round(deficientes.length / evaluated.length * 100) : 0}%) |\n`;
  md += `| Error crítico | ${errores.length} (${evaluated.length ? Math.round(errores.length / evaluated.length * 100) : 0}%) |\n`;
  md += `| Excluidas | ${excluidas.length} |\n`;
  md += `| Puntaje promedio | ${avgScore}/100 |\n\n`;

  if (errores.length > 0) {
    md += `## Errores críticos\n\n`;
    for (const r of errores) {
      md += `- **${r.id}**: "${(r.question || '').substring(0, 50)}..." → ${(r.errors || ['N/A']).join('; ')}\n`;
    }
    md += '\n';
  }

  if (deficientes.length > 0) {
    md += `## Casos deficientes\n\n`;
    for (const r of deficientes) {
      md += `- **${r.id}** (${r.total}/100): "${(r.question || '').substring(0, 50)}..." → "${(r.response || '').substring(0, 60)}..."\n`;
    }
    md += '\n';
  }

  md += `## Desglose por intención\n\n`;
  const byIntent = {};
  for (const r of successful) {
    const intent = r.expectedIntent || 'unknown';
    if (!byIntent[intent]) byIntent[intent] = { count: 0, total: 0, min: 100, max: 0 };
    byIntent[intent].count++;
    byIntent[intent].total += (r.total || 0);
    byIntent[intent].min = Math.min(byIntent[intent].min, r.total || 0);
    byIntent[intent].max = Math.max(byIntent[intent].max, r.total || 0);
  }
  md += `| Intención | Casos | Promedio | Mín | Máx |\n|---|---:|---:|---:|---:|\n`;
  for (const [intent, data] of Object.entries(byIntent)) {
    md += `| ${intent} | ${data.count} | ${Math.round(data.total / data.count)} | ${data.min} | ${data.max} |\n`;
  }
  md += '\n';

  if (optimas.length > 0) {
    md += `## Ejemplos óptimos\n\n`;
    for (const r of optimas.slice(0, 5)) {
      md += `- **${r.id}** (${r.total}/100): "${(r.question || '').substring(0, 50)}...\"\n`;
    }
    md += '\n';
  }

  md += `## Respuestas duplicadas\n\n`;
  const responseCounts = {};
  for (const r of successful) {
    const key = normalize(r.response || '');
    responseCounts[key] = (responseCounts[key] || 0) + 1;
  }
  const duplicates = Object.entries(responseCounts).filter(([_, count]) => count > 1).sort((a, b) => b[1] - a[1]);
  if (duplicates.length > 0) {
    md += `| Respuesta | Ocurrencias |\n|---|---:|\n`;
    for (const [resp, count] of duplicates.slice(0, 10)) {
      const preview = resp.substring(0, 60);
      md += `| "${preview}..." | ${count} |\n`;
    }
    md += '\n';
  } else {
    md += 'Sin respuestas duplicadas.\n\n';
  }

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, md);
  console.log(`Report saved: ${outputPath}`);
  return { optimas, aceptables, deficientes, errores, avgScore };
}

// --- CLI ---
if (require.main === module) {
  const registroPath = process.argv[2];
  if (!registroPath) {
    console.error('Usage: node evaluador_semantico_v2.js <registro_completo.json> [output_dir]');
    process.exit(1);
  }
  const data = JSON.parse(fs.readFileSync(registroPath, 'utf8'));
  const evaluated = evaluateResults(data.results || data);
  const outputDir = process.argv[3] || path.join(path.dirname(registroPath), 'evaluacion');
  fs.mkdirSync(outputDir, { recursive: true });
  fs.writeFileSync(path.join(outputDir, 'evaluados.json'), JSON.stringify(evaluated, null, 2));
  generateEvaluationReport(evaluated, path.join(outputDir, 'informe_evaluacion.md'));
  const summary = generateEvaluationReport(evaluated, path.join(outputDir, 'informe_evaluacion.md'));
  console.log(`Optimasy: ${summary.optimas.length}`);
  console.log(`Aceptables: ${summary.aceptables.length}`);
  console.log(`Deficientes: ${summary.deficientes.length}`);
  console.log(`Errores criticos: ${summary.errores.length}`);
  console.log(`Puntaje promedio: ${summary.avgScore}`);
}

module.exports = { evaluate, evaluateResults, generateEvaluationReport, detectExpectedIntent };
