package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.application.AiKnowledgeRepository.IntentExpression;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntentDetectorService {

	private final ConversationSpecCatalog conversationSpecCatalog;
	private final IntentExpressionService intentExpressionService;

	private static final Pattern EXPLICIT_TIME_PATTERN = Pattern
			.compile("\\b(?:a\\s+las\\s+)?(?:[01]?\\d|2[0-3])(?::[0-5]\\d)?\\s*(?:hrs?|horas?)?\\b");
	private static final Pattern RELATIVE_TIME_PATTERN = Pattern.compile(
			"\\b(?:primera hora|ultima hora|última hora|despues de las|después de las|antes de las)\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern EXPLICIT_DATE_PATTERN = Pattern.compile(
			"\\b(?:hoy|manana|mañana|pasado\\s+manana|pasado\\s+mañana|lunes|martes|miercoles|miércoles|jueves|viernes|sabado|sábado|domingo|\\d{1,2}\\s+(?:de\\s+)?[a-záéíóúñ]+)\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern QUESTION_PATTERN = Pattern
			.compile("^(?:qué|qué|cuál|cuáles|cuanto|cuánto|cómo|como|puedo|puede|existe|hay|"
					+ "me avisar|me avisarán|qué pasa si|qué pasa sí|hasta cuándo|hasta cuando|"
					+ "cuántas veces|cuantas veces|cuál es|cual es|es posible|se puede|"
					+ "tienen|tiene|existe|existiría|sería posible)", Pattern.CASE_INSENSITIVE);

	private static final List<String> HUMAN_WORDS = List.of("ejecutivo", "humano", "asesor", "supervisor", "llamenme",
			"llamarme", "contactarme", "contactenme", "contactenos", "quiero hablar", "hablar con recepcion",
			"hablar con recepción", "hablar con un", "hablar con una", "alguien del centro", "alguien de recepcion",
			"alguien de recepción", "me llame", "me llamen", "me contacten", "me contacte", "que una persona",
			"una persona revise", "una persona atienda", "una persona me ayude", "una persona real",
			"de una persona que");
	private static final List<String> COMPLAINT_WORDS = List.of("reclamo", "molesto", "molesta", "pesimo", "horrible",
			"denuncia", "fraude", "estafa", "nadie responde", "problema grave", "amenaza", "problema con mi atencion",
			"problema con mi atención", "no quede conforme", "no quedé conforme", "error en mi reserva",
			"desaparecio de la agenda", "desapareció de la agenda", "no tenian registrada", "no tenían registrada",
			"cobraron de mas", "cobraron de más", "devolucion", "devolución", "reembolso", "ofrecer a otra",
			"ofrecerlo a otra", "ofrecerselo a", "ofrecerselo");
	private static final List<String> PAYMENT_WORDS = List.of("pagar", "pago", "transferencia", "comprobante",
			"factura", "boleta", "deuda", "cobro", "link de pago");
	private static final List<String> PAYMENT_PROBLEM_WORDS = List.of("pago duplicado", "cobro duplicado",
			"cobraron dos veces", "no aparece", "no se reflejo", "no se reflejó", "monto incorrecto", "me cobraron",
			"reembolso", "devolucion", "devolución");
	private static final List<String> BOOKING_WORDS = List.of("agendar", "agenda una", "agenda la", "reservar",
			"reserva una", "reserva la", "tomar hora", "pedir hora", "sacar hora", "necesito una cita",
			"quiero una cita", "cita para", "un turno", "turno para", "sacar turno", "pedir turno", "apartar",
			"separar", "programar", "anotar", "inscribir", "matricular", "hacer una reserva", "hacer una cita",
			"hacer una hora", "una horita", "un cupo", "un cupito", "un turnito", "entrar a la agenda");
	private static final List<String> AVAILABILITY_WORDS = List.of("tienen hora", "tiene hora", "hay hora",
			"hay disponibilidad", "disponibilidad", "horarios hay", "que horarios", "qué horarios", "que horas",
			"qué horas", "hora libre", "horas libres", "tiene horas", "tienen horas", "primera hora", "ultima hora",
			"última hora", "cuantas personas", "cuántas personas", "cuanta gente", "cuánta gente", "al mismo tiempo",
			"atender al mismo tiempo", "horarios disponibles", "dame horarios", "disponible para");
	private static final List<String> RECOMMENDATION_WORDS = List.of("recomiendas", "recomienden", "recomendar",
			"que me recomiendan", "qué me recomiendan", "sirve para", "busco un", "busco una", "quiero algo para",
			"piel sensible", "hidratar", "hidratacion", "hidratación", "relajarme", "relajacion", "relajación",
			"no invasivo", "no invasiva");
	private static final List<String> SERVICE_INFORMATION_WORDS = List.of("que incluye", "qué incluye", "cuanto dura",
			"cuánto dura", "es invasivo", "es invasiva", "tratamientos tienen", "servicios tienen", "que tratamientos",
			"qué tratamientos", "que servicios", "qué servicios", "necesita evaluacion", "necesita evaluación",
			"informacion", "información", "mas informacion", "más información", "quiero informacion",
			"quiero información", "preparacion", "preparación", "prepararme", "como debo prepararme",
			"cómo debo prepararme", "hacer algo antes del tratamiento", "hacer algo antes de la sesion",
			"hacer algo antes de la sesión", "suspender algun producto", "suspender algún producto", "suspender algun",
			"cuanto antes debo llegar", "cuánto antes debo llegar", "cuanto tiempo antes", "puedo ir acompañada",
			"puedo ir acompañado", "puedo comer antes", "que ropa", "qué ropa", "contraindicacion", "contraindicación",
			"como debo", "cómo debo");
	private static final List<String> PROFESSIONAL_WORDS = List.of("carla", "profesional", "quien realiza",
			"quién realiza", "atienden con", "trabaja los", "trabaja el", "hace hidratacion", "hace hidratación");
	private static final List<String> BUSINESS_HOURS_WORDS = List.of("a que hora abren", "a qué hora abren",
			"hasta que hora", "hasta qué hora", "atienden los sabados", "atienden los sábados", "trabajan domingos",
			"trabajan los domingos", "horario de atencion", "horario de atención", "esta abierto", "está abierto",
			"atienden en la manana", "atienden en la mañana", "atienden en la tarde", "horario de apertura",
			"abren los", "abren el", "abren en", "feriado", "a la hora de almuerzo", "hora de almuerzo",
			"despues de las", "después de las", "apertura extraordinaria", "atienden en dias", "atienden en días",
			"abren los sabados", "abren los sábados", "abren los domingos", "abren feriados", "que horarios tienen",
			"qué horarios tienen", "horarios tienen", "horarios atienden", "cual es el horario", "cuál es el horario",
			"cuales son los horarios", "cuáles son los horarios");
	private static final List<String> THANKS_WORDS = List.of("gracias", "muchas gracias", "hasta luego", "chao",
			"eso era todo");
	private static final List<String> BOOKING_STATUS_WORDS = List.of("tengo agendado", "tengo agendada",
			"tengo reserva", "tengo una reserva", "mi reserva", "mis reservas", "revisar agenda", "revisa la agenda",
			"revisala la agenda", "agenda de junio", "agenda de este mes", "estado reserva", "confirmar mi hora",
			"ver mi cita", "tengo cita", "tengo una cita", "confirmar mi cita", "confirmar mi reserva",
			"confirmacion de cita", "confirmación de cita", "confirmacion de reserva", "confirmación de reserva",
			"codigo de reserva", "código de reserva", "codigo de cita", "código de cita", "codigo de la reserva",
			"código de la reserva", "codigo de la cita", "código de la cita", "buscar mi cita", "buscar mi reserva",
			"buscar por telefono", "buscar por teléfono", "ver lo de manana", "ver lo de mañana", "quiero confirmar",
			"esta listo", "está listo", "ya pague", "ya pagué", "todavia sirve", "todavía sirve",
			"me enviaran una nueva confirmacion", "me enviarán una nueva confirmación", "me enviaran una confirmacion",
			"me enviarán una confirmación", "enviaran confirmacion", "enviarán confirmación",
			"confirmacion por whatsapp", "confirmación por whatsapp", "confirmacion por correo",
			"confirmación por correo", "no me llego confirmacion", "no me llegó confirmación",
			"me pueden recordar la cita", "me pueden recordar la hora", "me avisaran si", "me avisarán si",
			"me avisan por", "aviso de recordatorio");
	private static final List<String> CHANGE_BOOKING_WORDS = List.of("reagendar", "reprogramar", "reprogramacion",
			"reprogramación", "cambiar hora", "cambiar mi hora", "cambiar reserva", "cambiar mi reserva",
			"cambiar cita", "cambiar mi cita", "cambio de hora", "cambio la hora", "cambiarme", "cámbiame", "cambieme",
			"cambiar la hora", "cambiar de hora", "modificar cita", "modificar mi cita", "mover", "mover mi hora",
			"mover mi reserva", "cambio de fecha", "cambiar de fecha", "cambiar fecha", "elegir otro dia",
			"elegir otro día", "elegir otra hora", "cambie la cita", "cambie la reserva", "necesito cambiar",
			"mantener mi hora actual", "mantener la hora");
	private static final List<String> CANCEL_BOOKING_WORDS = List.of("cancelar", "cancela", "cancele", "cancelo",
			"canceló", "cancelada", "cancelado", "anular", "anule", "anula", "cancelacion", "cancelación",
			"no voy a poder ir", "no voy a poder asistir", "no puedo ir", "no voy a ir", "no poder asistir",
			"no pude asistir");
	private static final List<String> PRICE_WORDS = List.of("precio", "valor", "cuanto cuesta", "cuánto cuesta",
			"cuanto vale", "cuánto vale", "tarifa", "sale", "cuesta");
	private static final List<String> QUOTE_WORDS = List.of("cotizar", "cotizacion", "cotización", "presupuesto");
	private static final List<String> SALES_WORDS = List.of("producto", "servicio", "plan", "promocion", "promoción",
			"comprar", "contratar", "disponible", "stock", "depilacion", "depilación", "axilas", "piernas", "bikini",
			"bozo", "rostro", "facial", "limpieza facial", "laser", "láser", "manicure", "pedicure", "masaje");
	private static final List<String> SUPPORT_WORDS = List.of("ayuda", "soporte", "problema", "error", "falla",
			"no funciona", "horario", "ubicacion", "ubicación", "direccion", "dirección", "estacionamiento",
			"estacionar", "donde estacionar", "dónde estacionar", "llegar en auto", "llegar en micro", "llegar en bus",
			"llegar en metro", "acceso", "estacionarse");
	private static final List<String> KNOWLEDGE_WORDS = List.of("politica", "política", "manual", "documento", "faq",
			"preguntas frecuentes", "catalogo", "catálogo", "terminos", "términos", "penalizacion", "penalización",
			"no show", "inasistencia", "reembolso", "devolucion", "devolución", "bloqueada", "bloqueado", "registradas",
			"edad minima", "edad mínima", "menor de edad", "edad", "tutor", "tutora", "autorizacion del tutor",
			"autorización del tutor", "adulto responsable", "datos del tutor", "consentimiento",
			"firmar un consentimiento", "aceptar el consentimiento", "como cancelo mi cita", "como cancelar",
			"cómo cancelo mi cita", "cómo cancelar", "como anular", "cómo anular", "proceso de cancelacion",
			"proceso de cancelación", "paso para cancelar", "penalizacion por cancelar", "penalización por cancelar",
			"tratamientos que no se realizan a menores", "tratamiento a menor", "servicio requiere abono",
			"abono es reembolsable", "abono si reprogramo", "abono para otra cita", "usar el abono", "abono confirma",
			"abono", "cuanto tengo que abonar", "cuánto tengo que abonar", "cuanto abonar", "cuánto abonar",
			"me devolveran el dinero", "me devolverán el dinero", "me devolveran", "me devolverán", "perdi el abono",
			"perdí el abono", "no aparezco en la agenda", "no tenian registrada", "no tenían registrada",
			"cuantas veces puedo cambiar", "cuántas veces puedo cambiar");
	private static final List<String> FOLLOW_UP_WORDS = List.of("seguimiento", "retomar", "cotizacion pendiente",
			"cotización pendiente", "recordatorio", "me contactaron");
	private static final List<String> SOCIAL_GREETING_WORDS = List.of("como estas", "como esta", "que tal",
			"hola como estas", "hola que tal", "buen dia", "buen día");
	private static final List<String> TECHNICAL_COMMAND_WORDS = List.of("docker compose", "docker", "kubectl", "mvn",
			"maven", "gradle", "npm", "pnpm", "yarn", "git ", "curl", "http://", "https://", "localhost", "stacktrace",
			"exception", "sql ", "select ", "insert ", "update ", "delete ", "dockerfile", "compose up", "--build");

	private static final List<String> SENSITIVE_WORDS = List.of("reaccion", "reacción", "ardor", "me ardio", "me ardió",
			"inflamacion", "inflamación", "alergia", "irritacion", "irritación", "quemadura", "dolor fuerte",
			"infeccion", "infección", "embarazada", "condicion medica", "condición médica");
	private static final List<String> LINK_RESEND_WORDS = List.of("no me llego el link", "no me llegó el link",
			"no me llego el enlace", "no me llegó el enlace", "reenviar", "reenvia", "reenvía", "mandame el link",
			"mándame el link", "mandame el enlace", "mándame el enlace");
	private static final List<String> LINK_EXPIRED_WORDS = List.of("enlace expiro", "enlace expiró", "link expiro",
			"link expiró", "link vencio", "link venció", "no funciona el enlace", "no funciona el link",
			"me dice expirado");
	private static final List<String> LOCATION_WORDS = List.of("donde queda", "dónde queda", "direccion", "dirección",
			"ubicacion", "ubicación", "ubicados", "como llego", "cómo llego", "sucursal", "sede");
	private static final List<String> WAITLIST_WORDS = List.of("lista de espera", "listo de espera",
			"cupo que se libero", "cupo que se liberó", "salir de la lista", "posicion en la lista",
			"posición en la lista", "aceptar el cupo");
	private static final List<String> HELP_WORDS = List.of("no se por donde empezar", "no sé por dónde empezar",
			"por donde empezar", "por dónde empezar", "que cosas puedo hacer", "qué cosas puedo hacer",
			"que puedo hacer", "qué puedo hacer", "como funciona", "cómo funciona", "que haces", "qué haces",
			"que puedes hacer", "qué puedes hacer", "ayudame a empezar", "ayúdame a empezar",
			"quiero hacer una consulta", "quisiera hacer una consulta", "necesito orientacion", "necesito orientación",
			"pueden orientarme", "pueden orientarme", "me puedes orientar", "me pueden orientar", "puedes orientarme",
			"puede orientarme");

	public IntentDetectorService() {
		this(new ConversationSpecCatalog(), null);
	}

	public IntentDetectorService(ConversationSpecCatalog conversationSpecCatalog) {
		this(conversationSpecCatalog, null);
	}

	@Autowired
	public IntentDetectorService(ConversationSpecCatalog conversationSpecCatalog,
			IntentExpressionService intentExpressionService) {
		this.conversationSpecCatalog = conversationSpecCatalog == null
				? new ConversationSpecCatalog()
				: conversationSpecCatalog;
		this.intentExpressionService = intentExpressionService;
	}

	public IntentDetectionResult detect(AgentConversationRequest request) {
		String traceId = AiTraceLogger.traceId(request);
		String text = normalize(request.messageBody());
		String rawText = normalizeRaw(request.messageBody());
		AiTraceLogger.info("MESSAGE_NORMALIZED", traceId, request.conversationId(), null, "IntentDetectorService",
				LogSanitizer.messageSummary("message", request.messageBody()) + " normalizedLength=" + text.length());
		boolean isQuestionText = isQuestion(text);
		AiTraceLogger.info("INTENT_CANDIDATES", traceId, request.conversationId(), null, "IntentDetectorService",
				"human=" + containsAny(text, HUMAN_WORDS) + " sensitive=" + containsAny(text, SENSITIVE_WORDS)
						+ " cancel=" + containsAny(text, CANCEL_BOOKING_WORDS) + " change="
						+ containsAny(text, CHANGE_BOOKING_WORDS) + " booking=" + containsAny(text, BOOKING_WORDS)
						+ " sales=" + containsAny(text, SALES_WORDS) + " payment=" + containsAny(text, PAYMENT_WORDS)
						+ " location=" + containsAny(text, LOCATION_WORDS) + " question=" + isQuestionText);

		if (text.isBlank() || text.equals("mensaje recibido sin texto")) {
			return new IntentDetectionResult(AgentIntent.AMBIGUOUS, null, 0.1, "bajo", false, null);
		}

		if (containsAny(text, TECHNICAL_COMMAND_WORDS)) {
			return new IntentDetectionResult(AgentIntent.TECHNICAL_MESSAGE, null, 0.91, "bajo", false, null);
		}

		if (isNameIntroduction(text)) {
			return new IntentDetectionResult(AgentIntent.GREETING, null, 0.76, "bajo", false, "cliente entrega nombre");
		}

		if (containsAny(text, SENSITIVE_WORDS)) {
			return new IntentDetectionResult(AgentIntent.COMPLAINT, null, 0.96, "alto", true,
					"caso sensible o reacción post tratamiento");
		}

		if (containsHumanRequest(text)) {
			return new IntentDetectionResult(AgentIntent.HUMAN_REQUEST, null, 0.96, "alto", true,
					"cliente solicita atencion humana");
		}

		if (containsAny(text, KNOWLEDGE_WORDS)) {
			return new IntentDetectionResult(AgentIntent.KNOWLEDGE_QUERY, null, 0.82, "bajo", false, null);
		}

		if (containsAny(text, FOLLOW_UP_WORDS)) {
			return new IntentDetectionResult(AgentIntent.FOLLOW_UP, null, 0.8, "bajo", false, null);
		}

		Optional<IntentDetectionResult> catalogSafetyOrTaxonomyIntent = conversationSpecCatalog.detect(text);
		if (catalogSafetyOrTaxonomyIntent.isPresent() && shouldUseCatalogIntent(catalogSafetyOrTaxonomyIntent.get())) {
			return catalogSafetyOrTaxonomyIntent.get();
		}

		Optional<IntentDetectionResult> negatedAgendaActionInformation = detectNegatedAgendaActionInformation(text);
		if (negatedAgendaActionInformation.isPresent()) {
			return negatedAgendaActionInformation.get();
		}

		Optional<IntentDetectionResult> databaseCatalogIntent = detectFromDatabaseCatalog(request, text, rawText,
				traceId);
		if (databaseCatalogIntent.isPresent()) {
			return databaseCatalogIntent.get();
		}

		if (!isInfoQueryNotAction(text) && containsAny(text, CANCEL_BOOKING_WORDS)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_CANCEL, null, 0.9, "medio", false, null);
		}

		if (!isInfoQueryNotAction(text) && containsAny(text, CHANGE_BOOKING_WORDS)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_CHANGE, null, 0.9, "medio", false, null);
		}

		if (containsAny(text, LINK_RESEND_WORDS) || containsAny(text, LINK_EXPIRED_WORDS)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_STATUS, null, 0.9, "medio", false, null);
		}

		if (containsAny(text, SOCIAL_GREETING_WORDS)) {
			return new IntentDetectionResult(AgentIntent.GREETING, null, 0.78, "bajo", false, null);
		}

		boolean hasBooking = containsExplicitBookingRequest(text);

		boolean hasPayment = containsAny(text, PAYMENT_WORDS);
		boolean hasPaymentProblem = containsAny(text, PAYMENT_PROBLEM_WORDS);
		if (hasPayment && hasBooking) {
			return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.86, "bajo", false, null);
		}
		if (hasPaymentProblem) {
			return new IntentDetectionResult(AgentIntent.PAYMENT_PROBLEM, null, 0.92, "alto", true,
					"problema de pago requiere revision humana");
		}
		if (hasPayment) {
			return new IntentDetectionResult(AgentIntent.PAYMENT_INQUIRY, null, 0.88, "medio", false, null);
		}

		if (containsAny(text, COMPLAINT_WORDS)) {
			return new IntentDetectionResult(AgentIntent.COMPLAINT, null, 0.94, "alto", true,
					"reclamo, molestia o urgencia");
		}
		boolean hasBookingStatus = containsAny(text, BOOKING_STATUS_WORDS);
		boolean hasPrice = containsAny(text, PRICE_WORDS);
		boolean hasQuote = containsAny(text, QUOTE_WORDS);
		boolean hasSales = containsAny(text, SALES_WORDS) || hasPrice || hasQuote;
		boolean hasExplicitCommercialQuestion = hasPrice || hasQuote;
		boolean hasSchedulingDate = EXPLICIT_DATE_PATTERN.matcher(text).find();
		boolean hasSchedulingTime = hasExplicitTime(text);
		boolean hasSchedulingLocation = containsAny(text, LOCATION_WORDS) || text.contains(" providencia")
				|| text.contains(" las condes") || text.contains(" en providencia") || text.contains(" en las condes");
		boolean hasSchedulingData = hasSchedulingTime || (hasSchedulingDate && hasSchedulingLocation);
		boolean hasAvailabilityQuestion = containsAny(text, AVAILABILITY_WORDS);
		boolean hasRecommendation = containsAny(text, RECOMMENDATION_WORDS);
		boolean hasServiceInformation = containsAny(text, SERVICE_INFORMATION_WORDS);
		boolean hasProfessional = containsAny(text, PROFESSIONAL_WORDS);
		boolean hasLocationQuery = containsAny(text, LOCATION_WORDS);
		boolean hasBusinessHoursQuery = containsAny(text, BUSINESS_HOURS_WORDS);

		if (hasHelpQuery(text)) {
			return new IntentDetectionResult(AgentIntent.COMMERCIAL_INQUIRY, null, 0.86, "bajo", false, null);
		}

		if (containsAny(text, WAITLIST_WORDS)) {
			return new IntentDetectionResult(AgentIntent.WAITLIST_QUERY, null, 0.86, "bajo", false, null);
		}

		if (hasBookingStatus) {
			return new IntentDetectionResult(AgentIntent.BOOKING_STATUS, null, 0.9, "medio", false, null);
		}

		if (hasBusinessHoursQuery) {
			return new IntentDetectionResult(AgentIntent.BUSINESS_HOURS_QUERY, null, 0.88, "bajo", false, null);
		}

		if (isPureThanksOrFarewell(text)) {
			return new IntentDetectionResult(AgentIntent.THANKS_OR_FAREWELL, null, 0.82, "bajo", false, null);
		}

		if (hasLocationQuery && !hasBooking && !hasAvailabilityQuestion) {
			return new IntentDetectionResult(AgentIntent.LOCATION_QUERY, null, 0.88, "bajo", false, null);
		}

		if (hasLocationQuery && hasAvailabilityQuestion) {
			return new IntentDetectionResult(AgentIntent.LOCATION_QUERY, AgentIntent.AVAILABILITY_QUERY, 0.9, "bajo",
					false, null);
		}

		if (hasAvailabilityQuestion) {
			AgentIntent secondary = hasBooking
					? AgentIntent.BOOKING_REQUEST
					: (hasProfessional ? AgentIntent.PROFESSIONAL_QUERY : null);
			return new IntentDetectionResult(AgentIntent.AVAILABILITY_QUERY, secondary, 0.91, "bajo", false, null);
		}

		if (hasBooking && hasExplicitCommercialQuestion) {
			return new IntentDetectionResult(AgentIntent.COMMERCIAL_AND_BOOKING, AgentIntent.BOOKING_REQUEST, 0.9,
					"bajo", false, null);
		}

		if (!hasBooking && hasSales && hasSchedulingData) {
			return new IntentDetectionResult(AgentIntent.COMMERCIAL_AND_BOOKING, AgentIntent.BOOKING_REQUEST, 0.9,
					"bajo", false, null);
		}

		if (containsAny(text, QUOTE_WORDS)) {
			return new IntentDetectionResult(AgentIntent.QUOTE_REQUEST, null, 0.88, "bajo", false, null);
		}

		if (containsAny(text, PRICE_WORDS)) {
			return new IntentDetectionResult(AgentIntent.PRICE_REQUEST, null, 0.88, "bajo", false, null);
		}

		if (hasRecommendation && !hasBooking) {
			return new IntentDetectionResult(AgentIntent.SERVICE_RECOMMENDATION, null, 0.88, "bajo", false, null);
		}

		if (hasServiceInformation && !hasBooking) {
			return new IntentDetectionResult(AgentIntent.SERVICE_INFORMATION, null, 0.86, "bajo", false, null);
		}

		if (hasProfessional && !hasBooking) {
			return new IntentDetectionResult(AgentIntent.PROFESSIONAL_QUERY, null, 0.86, "bajo", false, null);
		}

		if (hasBooking && hasSales) {
			return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, AgentIntent.SERVICE_INFORMATION, 0.9, "bajo",
					false, null);
		}

		if (!isInfoQueryNotAction(text) && hasBooking && containsAny(text, CANCEL_BOOKING_WORDS)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_CANCEL, null, 0.9, "medio", false, null);
		}

		if (!isInfoQueryNotAction(text) && hasBooking && containsAny(text, CHANGE_BOOKING_WORDS)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_CHANGE, null, 0.9, "medio", false, null);
		}

		if (hasBooking) {
			return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.86, "bajo", false, null);
		}

		if (!hasBooking && hasSchedulingDate && hasSchedulingTime && hasSchedulingLocation) {
			return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.82, "bajo", false, null);
		}

		if (hasSales) {
			return new IntentDetectionResult(AgentIntent.COMMERCIAL_INQUIRY, null, 0.82, "bajo", false, null);
		}

		if (containsAny(text, KNOWLEDGE_WORDS)) {
			return new IntentDetectionResult(AgentIntent.KNOWLEDGE_QUERY, null, 0.82, "bajo", false, null);
		}

		if (containsAny(text, FOLLOW_UP_WORDS)) {
			return new IntentDetectionResult(AgentIntent.FOLLOW_UP, null, 0.8, "bajo", false, null);
		}

		if (containsAny(text, LOCATION_WORDS)) {
			return new IntentDetectionResult(AgentIntent.LOCATION_QUERY, null, 0.82, "medio", false, null);
		}

		if (containsAny(text, SUPPORT_WORDS)) {
			return new IntentDetectionResult(AgentIntent.SUPPORT_GENERAL, null, 0.78, "medio", false, null);
		}

		if (catalogSafetyOrTaxonomyIntent.isPresent()) {
			return catalogSafetyOrTaxonomyIntent.get();
		}

		if (isGreeting(text)) {
			return new IntentDetectionResult(AgentIntent.GREETING, null, 0.74, "bajo", false, null);
		}

		return new IntentDetectionResult(AgentIntent.AMBIGUOUS, null, 0.58, "bajo", false, null);
	}

	private boolean containsExplicitBookingRequest(String text) {
		if (containsAny(text, BOOKING_WORDS)) {
			return true;
		}
		return Pattern
				.compile(
						"\\b(?:quiero|necesito|deseo|busco|me gustaria|me gustaría)\\s+(?:reservar|agendar|apartar)\\b")
				.matcher(text).find()
				|| Pattern.compile("\\b(?:quiero|necesito|deseo|busco)\\s+(?:una\\s+)?(?:hora|cita|turno)\\b")
						.matcher(text).find()
				|| Pattern.compile("^(?:reserva|agenda)\\s+(?!de\\b)").matcher(text).find()
				|| Pattern.compile("\\b(?:apartar|separar|inscribir|matricular|anotar|programar)\\b").matcher(text)
						.find()
				|| Pattern.compile("\\b(?:una|cita|turno|hora)\\s+(?:para|porfa|xfav?|por\\s+favor)\\b").matcher(text)
						.find();
	}

	private Optional<IntentDetectionResult> detectFromDatabaseCatalog(AgentConversationRequest request, String text,
			String rawText, String traceId) {
		if (intentExpressionService == null || request.businessId() == null) {
			return Optional.empty();
		}
		List<IntentExpression> expressions = intentExpressionService.findActive(request.businessId());
		for (IntentExpression expression : expressions) {
			AgentIntent mapped = mapCatalogCodeToAgentIntent(expression.code());
			if (mapped == null) {
				continue;
			}
			boolean matches = isOrthographicError(expression)
					? rawText.contains(expression.expressionNormalized())
					: text.contains(expression.expressionNormalized());
			if (!matches) {
				continue;
			}
			double confidence = toDouble(expression.confidenceBase(), 0.85);
			if (confidence < toDouble(expression.minimumConfidence(), 0.0)) {
				continue;
			}
			AiTraceLogger.info("INTENT_DB_CATALOG", traceId, request.conversationId(), null, "IntentDetectorService",
					"intent=" + mapped + " expressionType=" + expression.expressionType() + " confidence=" + confidence
							+ " source=DATABASE");
			return Optional.of(new IntentDetectionResult(mapped, null, confidence, "bajo", expression.requiresHuman(),
					"intencion desde catalogo BD (ai_intent_expression)", "DATABASE"));
		}
		return Optional.empty();
	}

	private boolean isOrthographicError(IntentExpression expression) {
		return "ORTHOGRAPHIC_ERROR".equals(expression.expressionType());
	}

	private double toDouble(BigDecimal value, double fallback) {
		return value == null ? fallback : value.doubleValue();
	}

	private static AgentIntent mapCatalogCodeToAgentIntent(String code) {
		if (code == null) {
			return null;
		}
		return CATALOG_CODE_TO_INTENT.get(code);
	}

	private static final Map<String, AgentIntent> CATALOG_CODE_TO_INTENT = Map.ofEntries(
			Map.entry("BOOKING_CREATE", AgentIntent.BOOKING_REQUEST),
			Map.entry("BOOKING_RESCHEDULE", AgentIntent.BOOKING_CHANGE),
			Map.entry("BOOKING_CANCEL", AgentIntent.BOOKING_CANCEL),
			Map.entry("BOOKING_AVAILABILITY", AgentIntent.AVAILABILITY_QUERY),
			Map.entry("BOOKING_STATUS", AgentIntent.BOOKING_STATUS),
			Map.entry("SERVICE_INFORMATION", AgentIntent.SERVICE_INFORMATION),
			Map.entry("SERVICE_PRICE", AgentIntent.PRICE_REQUEST),
			Map.entry("BUSINESS_HOURS", AgentIntent.BUSINESS_HOURS_QUERY),
			Map.entry("BUSINESS_LOCATION", AgentIntent.LOCATION_QUERY),
			Map.entry("PAYMENT_INFORMATION", AgentIntent.PAYMENT_INQUIRY),
			Map.entry("PAYMENT_STATUS", AgentIntent.PAYMENT_INQUIRY), Map.entry("GREETING", AgentIntent.GREETING),
			Map.entry("THANKS", AgentIntent.THANKS_OR_FAREWELL), Map.entry("GOODBYE", AgentIntent.THANKS_OR_FAREWELL),
			Map.entry("HUMAN_REQUEST", AgentIntent.HUMAN_REQUEST),
			Map.entry("COMMERCIAL_INQUIRY", AgentIntent.COMMERCIAL_INQUIRY),
			Map.entry("SERVICE_RECOMMENDATION", AgentIntent.SERVICE_RECOMMENDATION),
			Map.entry("PROFESSIONAL_QUERY", AgentIntent.PROFESSIONAL_QUERY),
			Map.entry("QUOTE_REQUEST", AgentIntent.QUOTE_REQUEST),
			Map.entry("PAYMENT_PROBLEM", AgentIntent.PAYMENT_PROBLEM),
			Map.entry("SUPPORT_GENERAL", AgentIntent.SUPPORT_GENERAL),
			Map.entry("TECHNICAL_MESSAGE", AgentIntent.TECHNICAL_MESSAGE),
			Map.entry("KNOWLEDGE_QUERY", AgentIntent.KNOWLEDGE_QUERY), Map.entry("FOLLOW_UP", AgentIntent.FOLLOW_UP),
			Map.entry("COMPLAINT", AgentIntent.COMPLAINT), Map.entry("WAITLIST_QUERY", AgentIntent.WAITLIST_QUERY));

	private String normalizeRaw(String value) {
		return TextNormalizer.normalize(value);
	}

	private Optional<IntentDetectionResult> detectNegatedAgendaActionInformation(String text) {
		boolean negatedAgendaAction = containsAny(text,
				List.of("no quiero cancelar", "no deseo cancelar", "no necesito cancelar", "no voy a cancelar",
						"no es para cancelar", "no quiero anular", "no quiero agendar", "no deseo agendar",
						"no necesito agendar", "no voy a agendar", "no es para agendar", "no quiero reservar",
						"no deseo reservar", "no quiero tomar hora", "no quiero pedir hora", "no quiero sacar hora"));
		if (!negatedAgendaAction) {
			return Optional.empty();
		}
		if (containsAny(text, PRICE_WORDS)) {
			return Optional.of(new IntentDetectionResult(AgentIntent.PRICE_REQUEST, null, 0.9, "bajo", false,
					"negacion explicita inhibe accion de agenda"));
		}
		if (containsAny(text, SERVICE_INFORMATION_WORDS)) {
			return Optional.of(new IntentDetectionResult(AgentIntent.SERVICE_INFORMATION, null, 0.86, "bajo", false,
					"negacion explicita inhibe accion de agenda"));
		}
		boolean asksInformation = containsAny(text,
				List.of("solo consultar", "solo preguntar", "solo saber", "solo quiero saber", "era una pregunta",
						"es una pregunta", "consulta", "consultar", "pregunta", "informacion", "información"));
		if (asksInformation) {
			return Optional.of(new IntentDetectionResult(AgentIntent.COMMERCIAL_INQUIRY, null, 0.84, "bajo", false,
					"negacion explicita inhibe accion de agenda"));
		}
		return Optional.empty();
	}

	private boolean isQuestion(String text) {
		String trimmed = text == null ? "" : text.trim().toLowerCase(java.util.Locale.ROOT);
		if (trimmed.isEmpty())
			return false;
		if (trimmed.contains("?"))
			return true;
		return QUESTION_PATTERN.matcher(trimmed).find();
	}

	private boolean isInfoQueryNotAction(String text) {
		if (!isQuestion(text))
			return false;
		String trimmed = text.trim().toLowerCase(java.util.Locale.ROOT);
		if (trimmed.startsWith("quiero ") || trimmed.startsWith("necesito ") || trimmed.startsWith("puedes ")
				|| trimmed.startsWith("puede ") || trimmed.startsWith("puedo ") || trimmed.startsWith("cancela")
				|| trimmed.startsWith("anula") || trimmed.startsWith("reprogra") || trimmed.startsWith("reagenda")) {
			return false;
		}
		if (trimmed.contains("?")) {
			return !trimmed.contains("quiero ") && !trimmed.contains("necesito ") && !trimmed.contains("puedes ")
					&& !trimmed.contains("puede ") && !trimmed.contains("puedo ");
		}
		return true;
	}

	private boolean isPureThanksOrFarewell(String text) {
		String trimmed = text == null ? "" : text.trim();
		if (trimmed.length() > 40 || trimmed.contains("?")) {
			return false;
		}
		return containsAny(trimmed, THANKS_WORDS);
	}

	private boolean isNameIntroduction(String text) {
		return Pattern.compile("^(?:soy|me llamo|mi nombre es)\s+[a-z][a-z ]{1,60}$").matcher(text.trim()).matches();
	}

	private boolean isGreeting(String text) {
		String trimmed = text.trim();
		return trimmed.equals("hola") || trimmed.equals("buenas") || trimmed.equals("buenos dias")
				|| trimmed.equals("buenos días") || trimmed.equals("buenas tardes") || trimmed.equals("buenas noches")
				|| trimmed.startsWith("hola ");
	}

	private boolean hasExplicitTime(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		if (RELATIVE_TIME_PATTERN.matcher(text).find()) {
			return true;
		}
		if (text.contains(":") || text.contains(" a las ") || text.contains(" horas") || text.contains(" hrs")) {
			return EXPLICIT_TIME_PATTERN.matcher(text).find();
		}
		return false;
	}

	private boolean hasHelpQuery(String text) {
		return containsAny(text, HELP_WORDS);
	}

	private boolean shouldUseCatalogIntent(IntentDetectionResult result) {
		return result.primaryIntent() == AgentIntent.AMBIGUOUS || result.primaryIntent() == AgentIntent.BOOKING_CHANGE
				|| result.primaryIntent() == AgentIntent.HUMAN_REQUEST
				|| result.primaryIntent() == AgentIntent.COMPLAINT;
	}

	private boolean containsAny(String text, List<String> candidates) {
		for (String candidate : candidates) {
			if (text.contains(normalize(candidate))) {
				return true;
			}
		}
		return false;
	}

	private boolean containsHumanRequest(String text) {
		for (String candidate : HUMAN_WORDS) {
			String normalized = normalize(candidate);
			if (normalized.contains(" ")) {
				if (text.contains(normalized)) {
					return true;
				}
				continue;
			}
			if (Pattern.compile("\\b" + Pattern.quote(normalized) + "\\b").matcher(text).find()) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		String t = TextNormalizer.normalize(value);
		t = t.replace("reserbar", "reservar").replace("recervar", "reservar").replace("resarvar", "reservar")
				.replace("ajendar", "agendar").replace("agndar", "agendar").replace("hroa", "hora");
		Pattern standaloneOra = Pattern.compile("(?<![a-z])ora(?![a-z])");
		t = standaloneOra.matcher(t).replaceAll("hora");
		return t;
	}
}
