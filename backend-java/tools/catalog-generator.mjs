#!/usr/bin/env node
/**
 * Generador del Catálogo Maestro de Conversación.
 *
 * Este script es la UNICA fuente autorizada para regenerar
 * `master-conversation-catalog.json` a partir de la configuración histórica
 * distribuida (listas Java de IntentDetectorService, intents.json, códigos de
 * catálogo BD, estados y transiciones de la máquina de estados).
 *
 * TAREA DE MANTENIMIENTO: tras ejecutar el generador, volcar el JSON generado en
 * `../src/main/resources/conversation/master/master-conversation-catalog.json`.
 * El runtime NUNCA lee este script: solo lee el JSON commiteado.
 */
import { fileURLToPath } from "node:url";
import path from "node:path";
import fs from "node:fs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const RESOURCE_DIR = path.resolve(__dirname, "../src/main/resources/conversation");
const INTENTS_RESOURCE = path.join(RESOURCE_DIR, "intents.json");
const OUT_FILE = path.join(RESOURCE_DIR, "master/master-conversation-catalog.json");

/* ------------------------------------------------------------------ *
 * 1) TAXONOMÍA: se replica la lógica de ConversationSpecCatalog.loadPhrases
 *    para que el runtime solo dependa del catálogo maestro.
 * ------------------------------------------------------------------ */
function normalizeTaxonomy(value) {
	if (value == null) return "";
	return value
		.toLowerCase()
		.normalize("NFD")
		.replace(/[\u0300-\u036f]/g, "")
		.replace(/[^a-z0-9 ]/g, " ")
		.replace(/\s+/g, " ")
		.trim();
}

const SPREADSHEET_TO_INTENT = {
	saludar: "GREETING",
	despedirse: "THANKS_OR_FAREWELL",
	agradecer: "THANKS_OR_FAREWELL",
	solicitar_ayuda: "SUPPORT_GENERAL",
	consultar_servicios: "COMMERCIAL_INQUIRY",
	consultar_precio: "PRICE_REQUEST",
	consultar_duracion: "SERVICE_INFORMATION",
	consultar_promocion: "COMMERCIAL_INQUIRY",
	consultar_sucursales: "LOCATION_QUERY",
	consultar_direccion: "LOCATION_QUERY",
	consultar_horario_empresa: "BUSINESS_HOURS_QUERY",
	consultar_profesionales: "PROFESSIONAL_QUERY",
	seleccionar_profesional: "PROFESSIONAL_QUERY",
	consultar_disponibilidad: "AVAILABILITY_QUERY",
	rechazar_horario: "AVAILABILITY_QUERY",
	reservar: "BOOKING_REQUEST",
	seleccionar_servicio: "BOOKING_REQUEST",
	seleccionar_sucursal: "BOOKING_REQUEST",
	seleccionar_horario: "BOOKING_REQUEST",
	confirmar_reserva: "BOOKING_REQUEST",
	enviar_datos_cliente: "BOOKING_REQUEST",
	corregir_dato: "BOOKING_REQUEST",
	consultar_reserva: "BOOKING_STATUS",
	listar_reservas: "BOOKING_STATUS",
	seleccionar_reserva: "BOOKING_STATUS",
	confirmar_asistencia: "BOOKING_STATUS",
	consultar_politica_cancelacion: "BOOKING_STATUS",
	reprogramar: "BOOKING_CHANGE",
	confirmar_reprogramacion: "BOOKING_CHANGE",
	rechazar_reprogramacion: "BOOKING_CHANGE",
	cancelar: "BOOKING_CANCEL",
	confirmar_cancelacion: "BOOKING_CANCEL",
	rechazar_cancelacion: "BOOKING_CANCEL",
	consultar_medio_pago: "PAYMENT_INQUIRY",
	consultar_abono: "PAYMENT_INQUIRY",
	solicitar_comprobante: "PAYMENT_INQUIRY",
	solicitar_devolucion: "PAYMENT_PROBLEM",
	hablar_con_persona: "HUMAN_REQUEST",
	presentar_reclamo: "COMPLAINT",
	mensaje_fuera_de_contexto: "AMBIGUOUS",
	mensaje_incomprensible: "AMBIGUOUS",
	cambiar_intencion: "AMBIGUOUS",
};

const REQUIRES_HUMAN = new Set(["HUMAN_REQUEST", "COMPLAINT", "PAYMENT_PROBLEM"]);

function taxonomyConfidence(priority) {
	const p = String(priority || "").toLowerCase();
	if (p.includes("crit")) return 0.9;
	if (p.includes("alta")) return 0.84;
	if (p.includes("media")) return 0.78;
	return 0.72;
}

function taxonomyUrgency(priority) {
	const p = String(priority || "").toLowerCase();
	return p.includes("crit") ? "alto" : p.includes("alta") ? "medio" : "bajo";
}

function loadTaxonomy() {
	const envelope = JSON.parse(fs.readFileSync(INTENTS_RESOURCE, "utf8"));
	const items = Array.isArray(envelope.items) ? envelope.items : [];
	const rows = [];
	for (const item of items) {
		if (!item || typeof item !== "object") continue;
		const intent = SPREADSHEET_TO_INTENT[String(item.intencion || "").trim()];
		if (!intent) continue;
		const examples = String(item.ejemplos_tipicos || "").split("|");
		for (const example of examples) {
			const normalizedPhrase = normalizeTaxonomy(example);
			if (!normalizedPhrase) continue;
			rows.push({
				normalizedPhrase,
				intent,
				confidence: taxonomyConfidence(item.prioridad),
				urgency: taxonomyUrgency(item.prioridad),
				requiresHuman: REQUIRES_HUMAN.has(intent),
				reason: `taxonomia_conversacion:${String(item.intencion || "").trim()}`,
			});
		}
	}
	return rows;
}

/* ------------------------------------------------------------------ *
 * 2) GRUPOS DE SINÓNIMOS (antes hardcodeados en IntentDetectorService).
 *    Cada grupo conserva EXACTAMENTE los términos históricos.
 * ------------------------------------------------------------------ */
const synonymGroups = {
	HUMAN_WORDS: [
		"ejecutivo", "humano", "asesor", "supervisor", "llamenme", "llamarme", "contactarme", "contactenme",
		"contactenos", "quiero hablar", "hablar con recepcion", "hablar con recepción", "hablar con un",
		"hablar con una", "alguien del centro", "alguien de recepcion", "alguien de recepción", "me llame",
		"me llamen", "me contacten", "me contacte", "que una persona", "una persona revise", "una persona atienda",
		"una persona me ayude", "una persona real", "de una persona que",
	],
	COMPLAINT_WORDS: [
		"reclamo", "molesto", "molesta", "pesimo", "horrible", "denuncia", "fraude", "estafa", "nadie responde",
		"problema grave", "amenaza", "problema con mi atencion", "problema con mi atención", "no quede conforme",
		"no quedé conforme", "error en mi reserva", "desaparecio de la agenda", "desapareció de la agenda",
		"no tenian registrada", "no tenían registrada", "cobraron de mas", "cobraron de más", "devolucion",
		"devolución", "reembolso", "ofrecer a otra", "ofrecerlo a otra", "ofrecerselo a", "ofrecerselo",
	],
	PAYMENT_WORDS: [
		"pagar", "pago", "transferencia", "comprobante", "factura", "boleta", "deuda", "cobro", "link de pago",
	],
	PAYMENT_PROBLEM_WORDS: [
		"pago duplicado", "cobro duplicado", "cobraron dos veces", "no aparece", "no se reflejo", "no se reflejó",
		"monto incorrecto", "me cobraron", "reembolso", "devolucion", "devolución",
	],
	BOOKING_WORDS: [
		"agendar", "agenda una", "agenda la", "reservar", "reserva una", "reserva la", "tomar hora", "pedir hora",
		"sacar hora", "necesito una cita", "quiero una cita", "cita para", "un turno", "turno para", "sacar turno",
		"pedir turno", "apartar", "separar", "programar", "anotar", "inscribir", "matricular", "hacer una reserva",
		"hacer una cita", "hacer una hora", "una horita", "un cupo", "un cupito", "un turnito", "entrar a la agenda",
	],
	AVAILABILITY_WORDS: [
		"tienen hora", "tiene hora", "hay hora", "hay disponibilidad", "disponibilidad", "horarios hay",
		"que horarios", "qué horarios", "que horas", "qué horas", "hora libre", "horas libres", "tiene horas",
		"tienen horas", "primera hora", "ultima hora", "última hora", "cuantas personas", "cuántas personas",
		"cuanta gente", "cuánta gente", "al mismo tiempo", "atender al mismo tiempo", "horarios disponibles",
		"dame horarios", "disponible para",
	],
	RECOMMENDATION_WORDS: [
		"recomiendas", "recomienden", "recomendar", "que me recomiendan", "qué me recomiendan", "sirve para",
		"busco un", "busco una", "quiero algo para", "piel sensible", "hidratar", "hidratacion", "hidratación",
		"relajarme", "relajacion", "relajación", "no invasivo", "no invasiva",
	],
	SERVICE_INFORMATION_WORDS: [
		"que incluye", "qué incluye", "cuanto dura", "cuánto dura", "es invasivo", "es invasiva",
		"tratamientos tienen", "servicios tienen", "que tratamientos", "qué tratamientos", "que servicios",
		"qué servicios", "necesita evaluacion", "necesita evaluación", "informacion", "información",
		"mas informacion", "más información", "quiero informacion", "quiero información", "preparacion",
		"preparación", "prepararme", "como debo prepararme", "cómo debo prepararme",
		"hacer algo antes del tratamiento", "hacer algo antes de la sesion", "hacer algo antes de la sesión",
		"suspender algun producto", "suspender algún producto", "suspender algun", "cuanto antes debo llegar",
		"cuánto antes debo llegar", "cuanto tiempo antes", "puedo ir acompañada", "puedo ir acompañado",
		"puedo comer antes", "que ropa", "qué ropa", "contraindicacion", "contraindicación", "como debo", "cómo debo",
	],
	PROFESSIONAL_WORDS: [
		"carla", "profesional", "quien realiza", "quién realiza", "atienden con", "trabaja los", "trabaja el",
		"hace hidratacion", "hace hidratación",
	],
	BUSINESS_HOURS_WORDS: [
		"a que hora abren", "a qué hora abren", "hasta que hora", "hasta qué hora", "atienden los sabados",
		"atienden los sábados", "trabajan domingos", "trabajan los domingos", "horario de atencion",
		"horario de atención", "esta abierto", "está abierto", "atienden en la manana", "atienden en la mañana",
		"atienden en la tarde", "horario de apertura", "abren los", "abren el", "abren en", "feriado",
		"a la hora de almuerzo", "hora de almuerzo", "despues de las", "después de las",
		"apertura extraordinaria", "atienden en dias", "atienden en días", "abren los sabados", "abren los sábados",
		"abren los domingos", "abren feriados", "que horarios tienen", "qué horarios tienen", "horarios tienen",
		"horarios atienden", "cual es el horario", "cuál es el horario", "cuales son los horarios",
		"cuáles son los horarios",
	],
	THANKS_WORDS: ["gracias", "muchas gracias", "hasta luego", "chao", "eso era todo"],
	BOOKING_STATUS_WORDS: [
		"tengo agendado", "tengo agendada", "tengo reserva", "tengo una reserva", "mi reserva", "mis reservas",
		"revisar agenda", "revisa la agenda", "revisala la agenda", "agenda de junio", "agenda de este mes",
		"estado reserva", "confirmar mi hora", "ver mi cita", "tengo cita", "tengo una cita", "confirmar mi cita",
		"confirmar mi reserva", "confirmacion de cita", "confirmación de cita", "confirmacion de reserva",
		"confirmación de reserva", "codigo de reserva", "código de reserva", "codigo de cita", "código de cita",
		"codigo de la reserva", "código de la reserva", "codigo de la cita", "código de la cita", "buscar mi cita",
		"buscar mi reserva", "buscar por telefono", "buscar por teléfono", "ver lo de manana", "ver lo de mañana",
		"quiero confirmar", "esta listo", "está listo", "ya pague", "ya pagué", "todavia sirve", "todavía sirve",
		"me enviaran una nueva confirmacion", "me enviarán una nueva confirmación",
		"me enviaran una confirmacion", "me enviarán una confirmación", "enviaran confirmacion",
		"enviarán confirmación", "confirmacion por whatsapp", "confirmación por whatsapp",
		"confirmacion por correo", "confirmación por correo", "no me llego confirmacion",
		"no me llegó confirmación", "me pueden recordar la cita", "me pueden recordar la hora", "me avisaran si",
		"me avisarán si", "me avisan por", "aviso de recordatorio",
	],
	CHANGE_BOOKING_WORDS: [
		"reagendar", "reprogramar", "reprogramacion", "reprogramación", "cambiar hora", "cambiar mi hora",
		"cambiar reserva", "cambiar mi reserva", "cambiar cita", "cambiar mi cita", "cambio de hora",
		"cambio la hora", "cambiarme", "cámbiame", "cambieme", "cambiar la hora", "cambiar de hora",
		"modificar cita", "modificar mi cita", "mover", "mover mi hora", "mover mi reserva", "cambio de fecha",
		"cambiar de fecha", "cambiar fecha", "elegir otro dia", "elegir otro día", "elegir otra hora",
		"cambie la cita", "cambie la reserva", "necesito cambiar", "mantener mi hora actual", "mantener la hora",
	],
	CANCEL_BOOKING_WORDS: [
		"cancelar", "cancela", "cancele", "cancelo", "canceló", "cancelada", "cancelado", "anular", "anule",
		"anula", "cancelacion", "cancelación", "no voy a poder ir", "no voy a poder asistir", "no puedo ir",
		"no voy a ir", "no poder asistir", "no pude asistir",
	],
	PRICE_WORDS: [
		"precio", "valor", "cuanto cuesta", "cuánto cuesta", "cuanto vale", "cuánto vale", "tarifa", "sale", "cuesta",
	],
	QUOTE_WORDS: ["cotizar", "cotizacion", "cotización", "presupuesto"],
	SALES_WORDS: [
		"producto", "servicio", "plan", "promocion", "promoción", "comprar", "contratar", "disponible", "stock",
		"depilacion", "depilación", "axilas", "piernas", "bikini", "bozo", "rostro", "facial", "limpieza facial",
		"laser", "láser", "manicure", "pedicure", "masaje",
	],
	SUPPORT_WORDS: [
		"ayuda", "soporte", "problema", "error", "falla", "no funciona", "horario", "ubicacion", "ubicación",
		"direccion", "dirección", "estacionamiento", "estacionar", "donde estacionar", "dónde estacionar",
		"llegar en auto", "llegar en micro", "llegar en bus", "llegar en metro", "acceso", "estacionarse",
	],
	KNOWLEDGE_WORDS: [
		"politica", "política", "manual", "documento", "faq", "preguntas frecuentes", "catalogo", "catálogo",
		"terminos", "términos", "penalizacion", "penalización", "no show", "inasistencia", "reembolso",
		"devolucion", "devolución", "bloqueada", "bloqueado", "registradas", "edad minima", "edad mínima",
		"menor de edad", "edad", "tutor", "tutora", "autorizacion del tutor", "autorización del tutor",
		"adulto responsable", "datos del tutor", "consentimiento", "firmar un consentimiento",
		"aceptar el consentimiento", "como cancelo mi cita", "como cancelar", "cómo cancelo mi cita",
		"cómo cancelar", "como anular", "cómo anular", "proceso de cancelacion", "proceso de cancelación",
		"paso para cancelar", "penalizacion por cancelar", "penalización por cancelar",
		"tratamientos que no se realizan a menores", "tratamiento a menor", "servicio requiere abono",
		"abono es reembolsable", "abono si reprogramo", "abono para otra cita", "usar el abono", "abono confirma",
		"abono", "cuanto tengo que abonar", "cuánto tengo que abonar", "cuanto abonar", "cuánto abonar",
		"me devolveran el dinero", "me devolverán el dinero", "me devolveran", "me devolverán", "perdi el abono",
		"perdí el abono", "no aparezco en la agenda", "no tenian registrada", "no tenían registrada",
		"cuantas veces puedo cambiar", "cuántas veces puedo cambiar",
	],
	FOLLOW_UP_WORDS: [
		"seguimiento", "retomar", "cotizacion pendiente", "cotización pendiente", "recordatorio", "me contactaron",
	],
	SOCIAL_GREETING_WORDS: [
		"como estas", "como esta", "que tal", "hola como estas", "hola que tal", "buen dia", "buen día",
	],
	TECHNICAL_COMMAND_WORDS: [
		"docker compose", "docker", "kubectl", "mvn", "maven", "gradle", "npm", "pnpm", "yarn", "git ", "curl",
		"http://", "https://", "localhost", "stacktrace", "exception", "sql ", "select ", "insert ", "update ",
		"delete ", "dockerfile", "compose up", "--build",
	],
	SENSITIVE_WORDS: [
		"reaccion", "reacción", "ardor", "me ardio", "me ardió", "inflamacion", "inflamación", "alergia",
		"irritacion", "irritación", "quemadura", "dolor fuerte", "infeccion", "infección", "embarazada",
		"condicion medica", "condición médica",
	],
	LINK_RESEND_WORDS: [
		"no me llego el link", "no me llegó el link", "no me llego el enlace", "no me llegó el enlace", "reenviar",
		"reenvia", "reenvía", "mandame el link", "mándame el link", "mandame el enlace", "mándame el enlace",
	],
	LINK_EXPIRED_WORDS: [
		"enlace expiro", "enlace expiró", "link expiro", "link expiró", "link vencio", "link venció",
		"no funciona el enlace", "no funciona el link", "me dice expirado",
	],
	LOCATION_WORDS: [
		"donde queda", "dónde queda", "direccion", "dirección", "ubicacion", "ubicación", "ubicados", "como llego",
		"cómo llego", "sucursal", "sede",
	],
	WAITLIST_WORDS: [
		"lista de espera", "listo de espera", "cupo que se libero", "cupo que se liberó", "salir de la lista",
		"posicion en la lista", "posición en la lista", "aceptar el cupo",
	],
	HELP_WORDS: [
		"no se por donde empezar", "no sé por dónde empezar", "por donde empezar", "por dónde empezar",
		"que cosas puedo hacer", "qué cosas puedo hacer", "que puedo hacer", "qué puedo hacer", "como funciona",
		"cómo funciona", "que haces", "qué haces", "que puedes hacer", "qué puedes hacer", "ayudame a empezar",
		"ayúdame a empezar", "quiero hacer una consulta", "quisiera hacer una consulta", "necesito orientacion",
		"necesito orientación", "pueden orientarme", "pueden orientarme", "me puedes orientar",
		"me pueden orientar", "puedes orientarme", "puede orientarme",
	],
	/* Grupos con propósitos auxiliares del detector (antes inline). */
	NEGATED_AGENDA_ACTION_WORDS: [
		"no quiero cancelar", "no deseo cancelar", "no necesito cancelar", "no voy a cancelar", "no es para cancelar",
		"no quiero anular", "no quiero agendar", "no deseo agendar", "no necesito agendar", "no voy a agendar",
		"no es para agendar", "no quiero reservar", "no deseo reservar", "no quiero tomar hora", "no quiero pedir hora",
		"no quiero sacar hora",
	],
	INFO_ONLY_MARKERS_WORDS: [
		"solo consultar", "solo preguntar", "solo saber", "solo quiero saber", "era una pregunta", "es una pregunta",
		"consulta", "consultar", "pregunta", "informacion", "información",
	],
	GREETING_WORDS: ["hola", "buenas", "buenos dias", "buenos días", "buenas tardes", "buenas noches"],
	CANCEL_FOLLOW_UP_WORDS: [
		"no voy a poder ir", "no puedo ir", "no voy a ir", "no pude asistir", "no poder asistir",
	],
	STATUS_FOLLOW_UP_WORDS: [
		"ya pague", "ya pagué", "esta listo", "está listo", "todavia sirve", "todavía sirve", "quiero confirmar",
		"listo", "confirmar mi cita", "confirmar mi reserva", "confirmar mi hora",
	],
	AFFIRMATIVE_WORDS: [
		"si", "sí", "ok", "okay", "dale", "claro", "simon", "sep", "yes", "por supuesto", "obvio", "correcto",
		"confirmo",
	],
	CONTINUE_BOOKING_WORDS: [
		"la misma de la otra vez", "el tratamiento anterior", "a la misma hora", "con ella", "con el",
		"no quiero ese", "no quiero esa", "quiero otra opcion", "quiero otra opción", "la de la otra vez",
		"el anterior", "lo mismo de antes", "la misma hora", "quiero lo mismo",
	],
};

/* ------------------------------------------------------------------ *
 * 3) CÓDIGOS DE CATÁLOGO BD (subconsulta ai_intent_expression y reglas).
 * ------------------------------------------------------------------ */
const CATALOG_CODE_TO_INTENT = {
	BOOKING_CREATE: "BOOKING_REQUEST",
	BOOKING_RESCHEDULE: "BOOKING_CHANGE",
	BOOKING_CANCEL: "BOOKING_CANCEL",
	BOOKING_AVAILABILITY: "AVAILABILITY_QUERY",
	BOOKING_STATUS: "BOOKING_STATUS",
	SERVICE_INFORMATION: "SERVICE_INFORMATION",
	SERVICE_PRICE: "PRICE_REQUEST",
	BUSINESS_HOURS: "BUSINESS_HOURS_QUERY",
	BUSINESS_LOCATION: "LOCATION_QUERY",
	PAYMENT_INFORMATION: "PAYMENT_INQUIRY",
	PAYMENT_STATUS: "PAYMENT_INQUIRY",
	GREETING: "GREETING",
	THANKS: "THANKS_OR_FAREWELL",
	GOODBYE: "THANKS_OR_FAREWELL",
	HUMAN_REQUEST: "HUMAN_REQUEST",
	COMMERCIAL_INQUIRY: "COMMERCIAL_INQUIRY",
	SERVICE_RECOMMENDATION: "SERVICE_RECOMMENDATION",
	PROFESSIONAL_QUERY: "PROFESSIONAL_QUERY",
	QUOTE_REQUEST: "QUOTE_REQUEST",
	PAYMENT_PROBLEM: "PAYMENT_PROBLEM",
	SUPPORT_GENERAL: "SUPPORT_GENERAL",
	TECHNICAL_MESSAGE: "TECHNICAL_MESSAGE",
	KNOWLEDGE_QUERY: "KNOWLEDGE_QUERY",
	FOLLOW_UP: "FOLLOW_UP",
	COMPLAINT: "COMPLAINT",
	WAITLIST_QUERY: "WAITLIST_QUERY",
};

/* ------------------------------------------------------------------ *
 * 4) INTENCIONES (26) con metadatos completos.
 * ------------------------------------------------------------------ */
const AGENT_BY_INTENT = {
	GREETING: "RECEPTION",
	THANKS_OR_FAREWELL: "RECEPTION",
	AMBIGUOUS: "RECEPTION",
	COMMERCIAL_INQUIRY: "SALES",
	SERVICE_INFORMATION: "SALES",
	SERVICE_RECOMMENDATION: "SALES",
	PRICE_REQUEST: "SALES",
	QUOTE_REQUEST: "SALES",
	COMMERCIAL_AND_BOOKING: "BOOKING",
	AVAILABILITY_QUERY: "BOOKING",
	PROFESSIONAL_QUERY: "BOOKING",
	BOOKING_REQUEST: "BOOKING",
	BOOKING_CHANGE: "BOOKING",
	BOOKING_CANCEL: "BOOKING",
	BOOKING_STATUS: "BOOKING",
	WAITLIST_QUERY: "BOOKING",
	PAYMENT_INQUIRY: "PAYMENTS",
	PAYMENT_PROBLEM: "PAYMENTS",
	LOCATION_QUERY: "SUPPORT",
	BUSINESS_HOURS_QUERY: "SUPPORT",
	SUPPORT_GENERAL: "SUPPORT",
	TECHNICAL_MESSAGE: "SUPPORT",
	KNOWLEDGE_QUERY: "KNOWLEDGE",
	FOLLOW_UP: "FOLLOW_UP",
	COMPLAINT: "HUMAN_HANDOFF",
	HUMAN_REQUEST: "HUMAN_HANDOFF",
};

const REQUIRES_HUMAN_INTENTS = new Set(["HUMAN_REQUEST", "COMPLAINT", "PAYMENT_PROBLEM"]);
const URGENCY_BY_PRIORITY = { critica: "alto", alta: "medio", media: "bajo", baja: "bajo" };

function intent(
	code,
	name,
	priority,
	{
		description = "",
		synonymGroups = [],
		requiredEntities = [],
		allowedStates = [],
		requiresAi = false,
		confidence = 0.8,
		minimumConfidence = 0.5,
		notes = "",
	},
) {
	return {
		code,
		name,
		description,
		priority,
		confidence,
		minimumConfidence,
		urgency: URGENCY_BY_PRIORITY[priority] ?? "bajo",
		agent: AGENT_BY_INTENT[code],
		requiresHuman: REQUIRES_HUMAN_INTENTS.has(code),
		requiresAi,
		requiredEntities,
		allowedStates,
		synonymGroups,
		catalogCodes: Object.entries(CATALOG_CODE_TO_INTENT)
			.filter(([, intentCode]) => intentCode === code)
			.map(([codeId]) => codeId),
		notes,
	};
}

const ESTADOS = ["INICIO", "IDENTIFICAR_INTENCION", "VERIFICAR_DISPONIBILIDAD", "CAPTURAR_DATOS", "CONSULTAR_PRECIO",
	"CONFIRMAR_CITA", "REPROGRAMAR_CITA", "CANCELAR_CITA", "CONSULTAR_RESERVA", "CONSULTAR_SERVICIOS",
	"CONSULTAR_HORARIOS", "REGISTRAR_PAGO", "GESTIONAR_RECLAMO", "DERIVAR_HUMANO"];

const intents = [
	intent("GREETING", "Saludar / Presentarse", "media", {
		description: "Inicio o reanudación cordial de la conversación.",
		synonymGroups: ["SOCIAL_GREETING_WORDS", "GREETING_WORDS"],
		allowedStates: ["INICIO", "IDENTIFICAR_INTENCION"],
	}),
	intent("THANKS_OR_FAREWELL", "Agradecer / Despedirse", "media", {
		description: "Cierre cortés de la conversación.",
		synonymGroups: ["THANKS_WORDS"],
		allowedStates: ["INICIO", "IDENTIFICAR_INTENCION"],
	}),
	intent("AMBIGUOUS", "Mensaje ambiguo o fuera de contexto", "media", {
		description: "No se puede determinar una intención con confianza mínima.",
		synonymGroups: [],
		allowedStates: ["INICIO", "IDENTIFICAR_INTENCION"],
	}),
	intent("COMMERCIAL_INQUIRY", "Consulta comercial", "media", {
		description: "Pregunta abierta sobre productos, servicios o promociones.",
		synonymGroups: ["HELP_WORDS", "SALES_WORDS"],
		requiresAi: true,
		requiredEntities: [],
		allowedStates: ["CONSULTAR_SERVICIOS", "CAPTURAR_DATOS"],
	}),
	intent("SERVICE_INFORMATION", "Información sobre un servicio", "media", {
		description: "Detalles, duración, preparación o contraindicaciones de un tratamiento.",
		synonymGroups: ["SERVICE_INFORMATION_WORDS"],
		allowedStates: ["CONSULTAR_SERVICIOS"],
	}),
	intent("SERVICE_RECOMMENDATION", "Recomendación de servicio", "media", {
		description: "El cliente pide sugerencia según su necesidad o tipo de piel.",
		synonymGroups: ["RECOMMENDATION_WORDS"],
		requiresAi: true,
		allowedStates: ["CONSULTAR_SERVICIOS"],
	}),
	intent("PRICE_REQUEST", "Consultar precio", "alta", {
		description: "Pregunta por el precio o valor de un servicio o producto.",
		synonymGroups: ["PRICE_WORDS"],
		allowedStates: ["CONSULTAR_PRECIO", "CONSULTAR_SERVICIOS"],
		notes: "En combinaciones con agenda, precio tiene prioridad para inhibir acción (ver reglas).",
	}),
	intent("QUOTE_REQUEST", "Solicitar cotización", "media", {
		description: "El cliente solicita una cotización o presupuesto a medida.",
		synonymGroups: ["QUOTE_WORDS"],
		allowedStates: ["CONSULTAR_PRECIO"],
	}),
	intent("BOOKING_REQUEST", "Reservar hora", "critica", {
		description: "El cliente desea agendar, reservar, apartar o separar una hora.",
		synonymGroups: ["BOOKING_WORDS"],
		requiredEntities: ["servicio_o_producto", "sede", "fecha", "hora", "cliente"],
		allowedStates: ["INICIO", "VERIFICAR_DISPONIBILIDAD", "CAPTURAR_DATOS", "CONFIRMAR_CITA"],
	}),
	intent("BOOKING_CHANGE", "Reprogramar / Cambiar reserva", "alta", {
		description: "El cliente desea cambiar fecha u hora de una reserva existente.",
		synonymGroups: ["CHANGE_BOOKING_WORDS"],
		requiredEntities: ["servicio_o_producto", "sede", "fecha", "hora"],
		allowedStates: ["REPROGRAMAR_CITA", "CAPTURAR_DATOS"],
	}),
	intent("BOOKING_CANCEL", "Cancelar reserva", "alta", {
		description: "El cliente desea cancelar una reserva existente.",
		synonymGroups: ["CANCEL_BOOKING_WORDS", "CANCEL_FOLLOW_UP_WORDS"],
		allowedStates: ["CANCELAR_CITA"],
	}),
	intent("BOOKING_STATUS", "Consultar estado de reserva", "media", {
		description: "El cliente consulta su agenda, confirmación o código de reserva.",
		synonymGroups: ["BOOKING_STATUS_WORDS", "LINK_RESEND_WORDS", "LINK_EXPIRED_WORDS", "STATUS_FOLLOW_UP_WORDS"],
		allowedStates: ["CONSULTAR_RESERVA"],
	}),
	intent("AVAILABILITY_QUERY", "Consultar disponibilidad", "alta", {
		description: "El cliente pregunta por horarios, cupos o disponibilidad.",
		synonymGroups: ["AVAILABILITY_WORDS"],
		allowedStates: ["VERIFICAR_DISPONIBILIDAD"],
	}),
	intent("PROFESSIONAL_QUERY", "Consultar por profesional", "media", {
		description: "El cliente pregunta por profesionales o quienes realizan un tratamiento.",
		synonymGroups: ["PROFESSIONAL_WORDS"],
		allowedStates: ["CONSULTAR_SERVICIOS", "IDENTIFICAR_INTENCION"],
	}),
	intent("LOCATION_QUERY", "Consultar ubicación", "media", {
		description: "El cliente pregunta por dirección, ubicación o cómo llegar.",
		synonymGroups: ["LOCATION_WORDS"],
		allowedStates: ["IDENTIFICAR_INTENCION"],
	}),
	intent("BUSINESS_HOURS_QUERY", "Consultar horario de atención", "media", {
		description: "El cliente pregunta por horarios de apertura, cierre o atención.",
		synonymGroups: ["BUSINESS_HOURS_WORDS"],
		allowedStates: ["CONSULTAR_HORARIOS"],
	}),
	intent("PAYMENT_INQUIRY", "Consultar pago", "media", {
		description: "El cliente consulta métodos de pago, abono o comprobantes.",
		synonymGroups: ["PAYMENT_WORDS"],
		allowedStates: ["REGISTRAR_PAGO"],
	}),
	intent("PAYMENT_PROBLEM", "Problema de pago", "critica", {
		description: "El cliente reporta un problema de pago (duplicado, monto incorrecto, devolución).",
		synonymGroups: ["PAYMENT_PROBLEM_WORDS"],
		allowedStates: ["REGISTRAR_PAGO", "DERIVAR_HUMANO"],
		notes: "Requiere revisión humana por defecto.",
	}),
	intent("SUPPORT_GENERAL", "Soporte general", "media", {
		description: "El cliente solicita ayuda, soporte o información genérica.",
		synonymGroups: ["SUPPORT_WORDS"],
		allowedStates: ["IDENTIFICAR_INTENCION"],
	}),
	intent("TECHNICAL_MESSAGE", "Mensaje técnico", "baja", {
		description: "Mensaje irrelevante con contenido de comandos o stack traces.",
		synonymGroups: ["TECHNICAL_COMMAND_WORDS"],
		allowedStates: ["INICIO"],
	}),
	intent("KNOWLEDGE_QUERY", "Consulta de conocimiento interno", "media", {
		description: "Políticas, términos, penalizaciones, datos del negocio.",
		synonymGroups: ["KNOWLEDGE_WORDS"],
		allowedStates: ["IDENTIFICAR_INTENCION", "CONSULTAR_HORARIOS"],
	}),
	intent("FOLLOW_UP", "Seguimiento comercial", "media", {
		description: "El cliente retoma una consulta o cotización previa.",
		synonymGroups: ["FOLLOW_UP_WORDS", "CONTINUE_BOOKING_WORDS"],
		allowedStates: ["IDENTIFICAR_INTENCION"],
	}),
	intent("COMPLAINT", "Reclamo", "critica", {
		description: "El cliente expresa molestia, reclamo o urgencia que requiere persona.",
		synonymGroups: ["COMPLAINT_WORDS", "SENSITIVE_WORDS"],
		requiredEntities: [],
		allowedStates: ["GESTIONAR_RECLAMO", "DERIVAR_HUMANO"],
		notes: "Siempre deriva a humano.",
	}),
	intent("HUMAN_REQUEST", "Solicitud de atención humana", "critica", {
		description: "El cliente pide explícitamente hablar con una persona.",
		synonymGroups: ["HUMAN_WORDS"],
		allowedStates: ["GESTIONAR_RECLAMO", "DERIVAR_HUMANO"],
		notes: "Siempre deriva a humano.",
	}),
	intent("COMMERCIAL_AND_BOOKING", "Consulta comercial con agenda", "alta", {
		description: "Combinación de consulta de precio o información con intención de reservar.",
		synonymGroups: ["BOOKING_WORDS", "PRICE_WORDS"],
		requiredEntities: ["servicio_o_producto", "sede", "fecha", "hora"],
		allowedStates: ["CONSULTAR_SERVICIOS", "CONFIRMAR_CITA"],
	}),
	intent("WAITLIST_QUERY", "Lista de espera", "media", {
		description: "El cliente desea entrar, salir o conocer su posición en la lista de espera.",
		synonymGroups: ["WAITLIST_WORDS"],
		allowedStates: ["VERIFICAR_DISPONIBILIDAD"],
	}),
];

/* ------------------------------------------------------------------ *
 * 5) ENTIDADES maestras.
 * ------------------------------------------------------------------ */
const entities = [
	{ key: "servicio_o_producto", entityType: "SERVICE", name: "Servicio o producto", required: true, deterministic: true, source: "catálogo de servicios + inferencia + alias", synonyms: ["servicio", "producto", "tratamiento", "plan"] },
	{ key: "sede", entityType: "LOCATION", name: "Sucursal", required: true, deterministic: true, source: "ubicaciones de negocio + comuna/ciudad", synonyms: ["sucursal", "sede", "ubicación", "dirección"] },
	{ key: "profesional", entityType: "PROFESSIONAL", name: "Profesional", required: false, deterministic: true, source: "catálogo de profesionales + prefijo 'con'", synonyms: ["profesional", "especialista"] },
	{ key: "cabina", entityType: "CABIN", name: "Cabina", required: false, deterministic: true, source: "capacidad / planificación", synonyms: ["cabina", "box"] },
	{ key: "fecha", entityType: "DATE", name: "Fecha explícita", required: false, deterministic: true, source: "regex fecha dd/mm o mes", synonyms: [] },
	{ key: "fecha_relativa", entityType: "RELATIVE_DATE", name: "Fecha relativa", required: false, deterministic: true, source: "días de semana, hoy, mañana, esta/próxima semana", synonyms: ["hoy", "mañana", "lunes", "martes", "sábado"] },
	{ key: "hora", entityType: "TIME", name: "Hora", required: true, deterministic: true, source: "regex hora (HH:mm, HH hrs, a las HH)", synonyms: ["hrs", "horas"] },
	{ key: "tramo_horario", entityType: "TIME_RANGE", name: "Tramo horario", required: false, deterministic: true, source: "mañana/tarde/noche/primera hora/mediodía", synonyms: ["en la mañana", "en la tarde", "en la noche"] },
	{ key: "cliente", entityType: "CUSTOMER", name: "Cliente", required: true, deterministic: false, source: "displayName de la sesión WhatsApp", synonyms: ["cliente"] },
	{ key: "nombre", entityType: "CUSTOMER_NAME", name: "Nombre del cliente", required: false, deterministic: true, source: "soy / me llamo / mi nombre es + nombre suelto", synonyms: [] },
	{ key: "correo", entityType: "EMAIL", name: "Correo electrónico", required: false, deterministic: true, source: "regex email", synonyms: ["correo", "email"] },
	{ key: "telefono", entityType: "PHONE", name: "Teléfono", required: false, deterministic: true, source: "número de la sesión WhatsApp", synonyms: ["teléfono"] },
	{ key: "observaciones", entityType: "NOTES", name: "Observaciones", required: false, deterministic: false, source: "notas libres del cliente", synonyms: ["observaciones", "notas", "comentario"] },
	{ key: "preferencia_horaria", entityType: "PREFERENCE", name: "Preferencia horaria", required: false, deterministic: true, source: "aliases canónicos PREFERENCE", synonyms: [] },
	{ key: "monto", entityType: "AMOUNT", name: "Monto", required: false, deterministic: true, source: "regex monto $", synonyms: ["$"] },
	{ key: "numero_solicitud", entityType: "ORDER_NUMBER", name: "Número de pedido/solicitud", required: false, deterministic: true, source: "regex pedido #", synonyms: ["pedido", "orden", "folio"] },
	{ key: "accion_pendiente", entityType: "PENDING_ACTION", name: "Acción pendiente", required: false, deterministic: false, source: "máquina de estados (cancelación/reprogramación en curso)", synonyms: [] },
	{ key: "fecha_limite", entityType: "DEADLINE", name: "Fecha límite", required: false, deterministic: false, source: "resultado de cambio/cancelación", synonyms: [] },
];

/* ------------------------------------------------------------------ *
 * 6) ESTADOS (14 canónicos) + mapeo de los legados persistidos.
 * ------------------------------------------------------------------ */
const states = [
	{ code: "INICIO", name: "Inicio", description: "Sin conversación activa o intención no determinada todavía.", entryData: [], allowedIntents: ["GREETING", "THANKS_OR_FAREWELL", "AMBIGUOUS", "TECHNICAL_MESSAGE"] },
	{ code: "IDENTIFICAR_INTENCION", name: "Identificando intención", description: "El turno actual clasifica el mensaje del cliente.", entryData: [], allowedIntents: ["GREETING", "THANKS_OR_FAREWELL", "COMMERCIAL_INQUIRY", "SERVICE_INFORMATION", "SERVICE_RECOMMENDATION", "PROFESSIONAL_QUERY", "LOCATION_QUERY", "BUSINESS_HOURS_QUERY", "SUPPORT_GENERAL", "KNOWLEDGE_QUERY", "FOLLOW_UP", "PAYMENT_INQUIRY", "PAYMENT_PROBLEM", "AMBIGUOUS"] },
	{ code: "VERIFICAR_DISPONIBILIDAD", name: "Verificando disponibilidad", description: "Se consultan cupos y horarios del servicio/sucursal.", entryData: ["servicio_o_producto", "sede"], allowedIntents: ["AVAILABILITY_QUERY", "BOOKING_REQUEST", "WAITLIST_QUERY", "COMMERCIAL_AND_BOOKING"] },
	{ code: "CAPTURAR_DATOS", name: "Capturando datos de reserva", description: "Se recopilan servicio, sucursal, fecha, hora y datos del cliente.", entryData: ["servicio_o_producto", "sede", "fecha", "hora", "cliente"], allowedIntents: ["BOOKING_REQUEST", "BOOKING_CHANGE", "COMMERCIAL_AND_BOOKING", "AMBIGUOUS"] },
	{ code: "CONSULTAR_PRECIO", name: "Consultando precio", description: "Se informa precio con fuente del catálogo.", entryData: ["servicio_o_producto"], allowedIntents: ["PRICE_REQUEST", "QUOTE_REQUEST", "COMMERCIAL_INQUIRY"] },
	{ code: "CONFIRMAR_CITA", name: "Confirmando cita", description: "Se envía enlace de confirmación y se espera aceptación.", entryData: ["servicio_o_producto", "sede", "fecha", "hora", "cliente"], allowedIntents: ["BOOKING_REQUEST", "BOOKING_STATUS", "AMBIGUOUS"] },
	{ code: "REPROGRAMAR_CITA", name: "Reprogramando cita", description: "Cambio de fecha/hora de una reserva existente.", entryData: ["servicio_o_producto", "sede", "fecha", "hora"], allowedIntents: ["BOOKING_CHANGE", "BOOKING_STATUS", "AMBIGUOUS"] },
	{ code: "CANCELAR_CITA", name: "Cancelando cita", description: "Anulación de una reserva con confirmación.", entryData: ["fecha", "hora"], allowedIntents: ["BOOKING_CANCEL", "BOOKING_STATUS", "AMBIGUOUS"] },
	{ code: "CONSULTAR_RESERVA", name: "Consultando reserva", description: "Estado, código o confirmación de una reserva.", entryData: [], allowedIntents: ["BOOKING_STATUS", "BOOKING_CHANGE", "BOOKING_CANCEL", "AMBIGUOUS"] },
	{ code: "CONSULTAR_SERVICIOS", name: "Consultando servicios", description: "Información, precios o recomendaciones de la oferta.", entryData: [], allowedIntents: ["COMMERCIAL_INQUIRY", "SERVICE_INFORMATION", "SERVICE_RECOMMENDATION", "PRICE_REQUEST", "QUOTE_REQUEST", "PROFESSIONAL_QUERY", "COMMERCIAL_AND_BOOKING"] },
	{ code: "CONSULTAR_HORARIOS", name: "Consultando horarios / ubicación", description: "Horarios de atención, horarios de apertura o ubicación.", entryData: [], allowedIntents: ["BUSINESS_HOURS_QUERY", "LOCATION_QUERY", "KNOWLEDGE_QUERY"] },
	{ code: "REGISTRAR_PAGO", name: "Gestionando pago", description: "Métodos de pago, abono, comprobantes o incidencias.", entryData: [], allowedIntents: ["PAYMENT_INQUIRY", "PAYMENT_PROBLEM"] },
	{ code: "GESTIONAR_RECLAMO", name: "Gestionando reclamo", description: "Reclamos o solicitudes de atención humana.", entryData: [], allowedIntents: ["COMPLAINT", "HUMAN_REQUEST"] },
	{ code: "DERIVAR_HUMANO", name: "Derivando a humano", description: "Conversación asignada a un agente humano.", entryData: [], allowedIntents: ["COMPLAINT", "HUMAN_REQUEST", "PAYMENT_PROBLEM"] },
];
const LEGACY_STATE_MAP = {
	ESPERANDO_SERVICIO: "CAPTURAR_DATOS",
	ESPERANDO_SUCURSAL: "CAPTURAR_DATOS",
	ESPERANDO_FECHA: "CAPTURAR_DATOS",
	ESPERANDO_HORARIO: "VERIFICAR_DISPONIBILIDAD",
	ESPERANDO_SELECCION_RESERVA: "CONSULTAR_RESERVA",
	ESPERANDO_CONFIRMACION_RESERVA: "CONFIRMAR_CITA",
	ESPERANDO_CONFIRMACION_REPROGRAMACION: "REPROGRAMAR_CITA",
	ESPERANDO_FECHA_REPROGRAMACION: "REPROGRAMAR_CITA",
	ESPERANDO_CONFIRMACION_CANCELACION: "CANCELAR_CITA",
	DERIVADO_HUMANO: "DERIVAR_HUMANO",
};

/* ------------------------------------------------------------------ *
 * 7) TRANSICIONES canónicas por intención.
 * ------------------------------------------------------------------ */
const stateTransitions = [
	{ from: "INICIO", to: "IDENTIFICAR_INTENCION", onIntent: "AMBIGUOUS", condition: null, priority: "alta" },
	{ from: "INICIO", to: "CAPTURAR_DATOS", onIntent: "BOOKING_REQUEST", condition: "datos de reserva incompletos", priority: "critica" },
	{ from: "INICIO", to: "REPROGRAMAR_CITA", onIntent: "BOOKING_CHANGE", condition: "reserva existente localizada", priority: "alta" },
	{ from: "INICIO", to: "CANCELAR_CITA", onIntent: "BOOKING_CANCEL", condition: "reserva existente localizada", priority: "alta" },
	{ from: "INICIO", to: "CONSULTAR_RESERVA", onIntent: "BOOKING_STATUS", condition: null, priority: "media" },
	{ from: "INICIO", to: "VERIFICAR_DISPONIBILIDAD", onIntent: "AVAILABILITY_QUERY", condition: null, priority: "alta" },
	{ from: "INICIO", to: "CONSULTAR_PRECIO", onIntent: "PRICE_REQUEST", condition: null, priority: "alta" },
	{ from: "INICIO", to: "CONSULTAR_PRECIO", onIntent: "QUOTE_REQUEST", condition: null, priority: "alta" },
	{ from: "INICIO", to: "CONSULTAR_SERVICIOS", onIntent: "COMMERCIAL_INQUIRY", condition: null, priority: "media" },
	{ from: "INICIO", to: "CONSULTAR_SERVICIOS", onIntent: "SERVICE_INFORMATION", condition: null, priority: "media" },
	{ from: "INICIO", to: "CONSULTAR_SERVICIOS", onIntent: "SERVICE_RECOMMENDATION", condition: null, priority: "media" },
	{ from: "INICIO", to: "CONSULTAR_SERVICIOS", onIntent: "PROFESSIONAL_QUERY", condition: null, priority: "media" },
	{ from: "INICIO", to: "CONSULTAR_HORARIOS", onIntent: "BUSINESS_HOURS_QUERY", condition: null, priority: "media" },
	{ from: "INICIO", to: "CONSULTAR_HORARIOS", onIntent: "LOCATION_QUERY", condition: null, priority: "media" },
	{ from: "INICIO", to: "REGISTRAR_PAGO", onIntent: "PAYMENT_INQUIRY", condition: null, priority: "media" },
	{ from: "INICIO", to: "REGISTRAR_PAGO", onIntent: "PAYMENT_PROBLEM", condition: null, priority: "critica" },
	{ from: "INICIO", to: "GESTIONAR_RECLAMO", onIntent: "COMPLAINT", condition: null, priority: "critica" },
	{ from: "INICIO", to: "DERIVAR_HUMANO", onIntent: "HUMAN_REQUEST", condition: null, priority: "critica" },
	{ from: "IDENTIFICAR_INTENCION", to: "VERIFICAR_DISPONIBILIDAD", onIntent: "AVAILABILITY_QUERY", condition: "disponibilidad preguntada", priority: "alta" },
	{ from: "IDENTIFICAR_INTENCION", to: "CAPTURAR_DATOS", onIntent: "BOOKING_REQUEST", condition: "datos de reserva incompletos", priority: "critica" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_PRECIO", onIntent: "PRICE_REQUEST", condition: null, priority: "alta" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_PRECIO", onIntent: "QUOTE_REQUEST", condition: null, priority: "alta" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_SERVICIOS", onIntent: "COMMERCIAL_INQUIRY", condition: null, priority: "media" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_SERVICIOS", onIntent: "SERVICE_INFORMATION", condition: null, priority: "media" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_SERVICIOS", onIntent: "SERVICE_RECOMMENDATION", condition: null, priority: "media" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_SERVICIOS", onIntent: "PROFESSIONAL_QUERY", condition: null, priority: "media" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_HORARIOS", onIntent: "BUSINESS_HOURS_QUERY", condition: null, priority: "media" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_HORARIOS", onIntent: "LOCATION_QUERY", condition: null, priority: "media" },
	{ from: "IDENTIFICAR_INTENCION", to: "CONSULTAR_RESERVA", onIntent: "BOOKING_STATUS", condition: null, priority: "alta" },
	{ from: "IDENTIFICAR_INTENCION", to: "REPROGRAMAR_CITA", onIntent: "BOOKING_CHANGE", condition: "reserva existente localizada", priority: "alta" },
	{ from: "IDENTIFICAR_INTENCION", to: "CANCELAR_CITA", onIntent: "BOOKING_CANCEL", condition: "reserva existente localizada", priority: "alta" },
	{ from: "IDENTIFICAR_INTENCION", to: "REGISTRAR_PAGO", onIntent: "PAYMENT_INQUIRY", condition: null, priority: "media" },
	{ from: "IDENTIFICAR_INTENCION", to: "REGISTRAR_PAGO", onIntent: "PAYMENT_PROBLEM", condition: null, priority: "critica" },
	{ from: "IDENTIFICAR_INTENCION", to: "GESTIONAR_RECLAMO", onIntent: "COMPLAINT", condition: null, priority: "critica" },
	{ from: "IDENTIFICAR_INTENCION", to: "DERIVAR_HUMANO", onIntent: "HUMAN_REQUEST", condition: null, priority: "critica" },
	{ from: "VERIFICAR_DISPONIBILIDAD", to: "CAPTURAR_DATOS", onIntent: "BOOKING_REQUEST", condition: "cliente elige horario", priority: "critica" },
	{ from: "VERIFICAR_DISPONIBILIDAD", to: "CONFIRMAR_CITA", onIntent: "COMMERCIAL_AND_BOOKING", condition: "datos completos", priority: "alta" },
	{ from: "CAPTURAR_DATOS", to: "CONFIRMAR_CITA", onIntent: "BOOKING_REQUEST", condition: "datos completos y validados", priority: "critica" },
	{ from: "REPROGRAMAR_CITA", to: "CONFIRMAR_CITA", onIntent: "BOOKING_CHANGE", condition: "nueva fecha/hora confirmadas", priority: "critica" },
	{ from: "CANCELAR_CITA", to: "INICIO", onIntent: "BOOKING_CANCEL", condition: "cancelación confirmada", priority: "critica" },
	{ from: "CONSULTAR_RESERVA", to: "REPROGRAMAR_CITA", onIntent: "BOOKING_CHANGE", condition: "cliente pide cambio", priority: "alta" },
	{ from: "CONSULTAR_RESERVA", to: "CANCELAR_CITA", onIntent: "BOOKING_CANCEL", condition: "cliente pide cancelación", priority: "alta" },
	{ from: "CONSULTAR_SERVICIOS", to: "CONSULTAR_PRECIO", onIntent: "PRICE_REQUEST", condition: "pregunta de precio", priority: "alta" },
	{ from: "CONSULTAR_SERVICIOS", to: "CAPTURAR_DATOS", onIntent: "COMMERCIAL_AND_BOOKING", condition: "cliente decide reservar", priority: "alta" },
	{ from: "GESTIONAR_RECLAMO", to: "DERIVAR_HUMANO", onIntent: "HUMAN_REQUEST", condition: "requiere persona", priority: "critica" },
	{ from: "REGISTRAR_PAGO", to: "DERIVAR_HUMANO", onIntent: "PAYMENT_PROBLEM", condition: "incidencia de pago", priority: "critica" },
	{ from: "CONFIRMAR_CITA", to: "INICIO", onIntent: "THANKS_OR_FAREWELL", condition: "cita confirmada y conversación terminada", priority: "media" },
];

/* ------------------------------------------------------------------ *
 * 8) REGLAS maestras del modelo + reglas de conversación (safety).
 * ------------------------------------------------------------------ */
const rules = [
	{ id: "R1", name: "Todo mensaje atendido", type: "MODEL", description: "Todo mensaje de un cliente autorizado recibe una respuesta o una derivación; nunca queda sin procesar.", appliesTo: "PROCESAMIENTO" },
	{ id: "R2", name: "No inventar información", type: "MODEL", description: "Precios, horarios, duraciones y políticas se responden SOLO desde catálogo/BD. La IA nunca fabrica valores.", appliesTo: "RESPUESTA" },
	{ id: "R3", name: "Confirmación antes de agendar", type: "MODEL", description: "No se confirma una reserva sin validar servicio, sede, fecha, hora y datos del cliente.", appliesTo: "AGENDA" },
	{ id: "R4", name: "Derivación humana en casos sensibles", type: "MODEL", description: "Reclamos, problemas de pago, reacciones post tratamiento y solicitudes de humano derivan siempre.", appliesTo: "DERIVACION" },
	{ id: "R5", name: "Detección determinista primero", type: "MODEL", description: "La clasificación de intención se resuelve por catálogo determinista; la IA solo asiste en ambigüedad y texto libre.", appliesTo: "DETECCION" },
	{ id: "R6", name: "Solo datos válidos del catálogo", type: "MODEL", description: "Las entidades se resuelven contra servicios, ubicaciones y profesionales activos; nunca se inventan valores.", appliesTo: "ENTIDADES" },
	{ id: "R7", name: "Respuestas en español neutral", type: "MODEL", description: "Tono cordial, claro y en español neutral es-419.", appliesTo: "RESPUESTA" },
	{ id: "R8", name: "Errores ortográficos normalizados", type: "MODEL", description: "Antes de comparar sinónimos se normalizan tildes, mayúsculas y errores ortográficos conocidos.", appliesTo: "NORMALIZACION" },
	{ id: "R9", name: "No exponer datos sensibles", type: "MODEL", description: "Logs, métricas y registros sanitizan números, nombres y datos personales.", appliesTo: "SEGURIDAD" },
	{ id: "R10", name: "Prioridad de consulta sobre acción combinada", type: "MODEL", description: "Negación explícita de agenda con pregunta de precio o información resuelve a consulta, NO a reserva/cancelación.", appliesTo: "DETECCION" },
	{ id: "SAFETY-1", name: "Acknowledgment aislado", type: "CONVERSATION", appliesTo: "DETECCION", intent: "AMBIGUOUS", confidence: 0.72, urgency: "bajo", reason: "mensaje ambiguo segun reglas de conversacion", description: "ok/oki/símbolos aislados no ejecutan acciones." },
	{ id: "SAFETY-2", name: "Rechazo breve sin objeto", type: "CONVERSATION", appliesTo: "DETECCION", intent: "AMBIGUOUS", confidence: 0.74, urgency: "bajo", reason: "rechazo breve sin objeto de agenda", description: "mejor no / sabes que mejor no / no mejor no." },
	{ id: "SAFETY-3", name: "Cancelación negada con cambio", type: "CONVERSATION", appliesTo: "DETECCION", intent: "BOOKING_CHANGE", confidence: 0.93, urgency: "medio", reason: "negacion explicita inhibe cancelar", description: "no quiero cancelar + cambiar/reprogramar/reagendar/mover." },
	{ id: "SAFETY-4", name: "Imposibilidad de asistir + fecha nueva", type: "CONVERSATION", appliesTo: "DETECCION", intent: "BOOKING_CHANGE", confidence: 0.88, urgency: "medio", reason: "fecha u horario nuevo junto a imposibilidad de asistir", description: "no puedo/no podré/no alcanzo + día/hora nuevo." },
];

/* ------------------------------------------------------------------ *
 * 9) RESPUESTAS centralizadas por intención y ranura.
 * ------------------------------------------------------------------ */
const RESPONSE = (intentCode) => ({
	intentCode,
	templates: {
		initial: `${intentCode}_INTRO`,
		missingData: `${intentCode}_MISSING_DATA`,
		success: `${intentCode}_SUCCESS`,
		error: `${intentCode}_ERROR`,
		handoff: `${intentCode}_HANDOFF`,
		farewell: `${intentCode}_FAREWELL`,
	},
});
const responses = [
	RESPONSE("GREETING"), RESPONSE("THANKS_OR_FAREWELL"), RESPONSE("AMBIGUOUS"), RESPONSE("COMMERCIAL_INQUIRY"),
	RESPONSE("SERVICE_INFORMATION"), RESPONSE("SERVICE_RECOMMENDATION"), RESPONSE("PRICE_REQUEST"),
	RESPONSE("QUOTE_REQUEST"), RESPONSE("BOOKING_REQUEST"), RESPONSE("BOOKING_CHANGE"), RESPONSE("BOOKING_CANCEL"),
	RESPONSE("BOOKING_STATUS"), RESPONSE("AVAILABILITY_QUERY"), RESPONSE("PROFESSIONAL_QUERY"),
	RESPONSE("LOCATION_QUERY"), RESPONSE("BUSINESS_HOURS_QUERY"), RESPONSE("PAYMENT_INQUIRY"),
	RESPONSE("PAYMENT_PROBLEM"), RESPONSE("SUPPORT_GENERAL"), RESPONSE("TECHNICAL_MESSAGE"),
	RESPONSE("KNOWLEDGE_QUERY"), RESPONSE("FOLLOW_UP"), RESPONSE("COMPLAINT"), RESPONSE("HUMAN_REQUEST"),
	RESPONSE("COMMERCIAL_AND_BOOKING"), RESPONSE("WAITLIST_QUERY"),
];

/* ------------------------------------------------------------------ *
 * 10) PUNTOS DE INTEGRACIÓN de los agentes de ejecución.
 * ------------------------------------------------------------------ */
const agents = [
	{ type: "RECEPTION", name: "Recepción", execution: "Solo ejecuta acciones: saludo, despedida, bienvenida con capacidades.", intents: ["GREETING", "THANKS_OR_FAREWELL", "AMBIGUOUS"] },
	{ type: "SALES", name: "Ventas", execution: "Consulta de catálogo y precios; NO agenda. Puede recomendar desde catálogo.", intents: ["COMMERCIAL_INQUIRY", "SERVICE_INFORMATION", "SERVICE_RECOMMENDATION", "PRICE_REQUEST", "QUOTE_REQUEST"] },
	{ type: "BOOKING", name: "Agenda", execution: "Crea/cambia/cancela/consulta reservas, disponibilidad y lista de espera.", intents: ["COMMERCIAL_AND_BOOKING", "AVAILABILITY_QUERY", "PROFESSIONAL_QUERY", "BOOKING_REQUEST", "BOOKING_CHANGE", "BOOKING_CANCEL", "BOOKING_STATUS", "WAITLIST_QUERY"] },
	{ type: "PAYMENTS", name: "Pagos", execution: "Informa métodos de pago, abonos y comprobantes; deriva incidencias.", intents: ["PAYMENT_INQUIRY", "PAYMENT_PROBLEM"] },
	{ type: "SUPPORT", name: "Soporte", execution: "Ubicación, horarios de atención y soporte general.", intents: ["LOCATION_QUERY", "BUSINESS_HOURS_QUERY", "SUPPORT_GENERAL", "TECHNICAL_MESSAGE"] },
	{ type: "KNOWLEDGE", name: "Conocimiento", execution: "Responde políticas y conocimiento interno desde la base de conocimiento.", intents: ["KNOWLEDGE_QUERY"] },
	{ type: "FOLLOW_UP", name: "Seguimiento", execution: "Retoma cotizaciones o consultas pendientes.", intents: ["FOLLOW_UP"] },
	{ type: "HUMAN_HANDOFF", name: "Derivación Humana", execution: "Deriva a humano; jamás resuelve reclamos por sí solo.", intents: ["COMPLAINT", "HUMAN_REQUEST"] },
];

/* ------------------------------------------------------------------ *
 * 11) ROL DE LA IA (solo apoyo).
 * ------------------------------------------------------------------ */
const aiIntegration = {
	role: "support_only",
	responsibilities: ["clasificar_texto_libre_sin_confianza_determinista", "extraer_entidades_de_texto_libre", "resolver_ambiguedad_con_contexto"],
	prohibitions: ["crear_reglas", "definir_prioridades", "inventar_precios", "decidir_politicas_de_cancelacion", "derivar_sin_regla"],
};

/* ------------------------------------------------------------------ *
 * Ensamblado final.
 * ------------------------------------------------------------------ */
const catalog = {
	schemaVersion: "1.0",
	metadata: {
		title: "Catálogo Maestro de Conversación",
		description: "Fuente única de verdad del motor de intenciones: intenciones, entidades, estados, transiciones, reglas, respuestas, sinónimos y mapeo de agentes.",
		sourceArtifacts: [
			"IntentDetectorService.java (listas de sinónimos)",
			"conversation/intents.json (taxonomía)",
			"ai_intent_expression (códigos de catálogo BD)",
			"ConversationSpecCatalog.java (reglas de conversación)",
			"AgentRegistry.java + BookingAgent (mapeo de agentes y respuestas)",
			"Estados persistidos de ai_conversation_context (deriveConversationState)",
		],
		generatedBy: "Manual/MEMANTO; regenerable con tools/catalog-generator.mjs",
		generationScript: "catálogo generado; el runtime solo lee este JSON.",
	},
	agents,
	aiIntegration,
	intents,
	entities,
	synonymGroups,
	taxonomy: loadTaxonomy(),
	catalogCodes: CATALOG_CODE_TO_INTENT,
	states,
	legacyStateMap: LEGACY_STATE_MAP,
	stateTransitions,
	rules,
	responses,
};

const output = JSON.stringify(catalog, null, 2) + "\n";
fs.writeFileSync(OUT_FILE, output, "utf8");

console.log(`[OK] Catálogo maestro generado: ${OUT_FILE}`);
console.log(`     intents=${catalog.intents.length} entities=${catalog.entities.length} ` +
	`synonymGroups=${Object.keys(catalog.synonymGroups).length} taxonomy=${catalog.taxonomy.length} ` +
	`states=${catalog.states.length} transitions=${catalog.stateTransitions.length} rules=${catalog.rules.length} ` +
	`responses=${catalog.responses.length}`);