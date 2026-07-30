package com.asistentewhatsapp.aesthetic.application;

import com.asistentewhatsapp.aesthetic.api.AestheticBusinessRuleResponse;
import com.asistentewhatsapp.aesthetic.api.UpsertAestheticServiceRequest;
import com.asistentewhatsapp.aesthetic.api.UpsertAestheticProductRequest;
import com.asistentewhatsapp.aesthetic.api.UpsertAestheticBusinessRuleRequest;
import com.asistentewhatsapp.aesthetic.api.AestheticCategoryResponse;
import com.asistentewhatsapp.aesthetic.api.AestheticIntentLogResponse;
import com.asistentewhatsapp.aesthetic.api.AestheticProductResponse;
import com.asistentewhatsapp.aesthetic.api.AestheticServiceResponse;
import com.asistentewhatsapp.aesthetic.api.IntentAnalysisRequest;
import com.asistentewhatsapp.aesthetic.api.IntentAnalysisResponse;
import com.asistentewhatsapp.aesthetic.api.IntentEntitiesResponse;
import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository;
import com.asistentewhatsapp.aesthetic.infrastructure.openai.OpenAiIntentClient;
import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository.AestheticPromotionSummary;
import com.asistentewhatsapp.aiagents.application.TransactionalAgendaBookingService;
import com.asistentewhatsapp.aiagents.application.AiTraceLogger;
import com.asistentewhatsapp.aiagents.application.WhatsAppMessageFormatter;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import com.asistentewhatsapp.shared.api.PagedResponse;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AestheticCenterService {

	private static final int MAX_PAGE_SIZE = 200;
	private static final Pattern TIME_EXPRESSION_PATTERN = Pattern.compile(
			"\\b(?:a\\s+las\\s+)?([01]?\\d|2[0-3])(?:(?::|\\.)([0-5]\\d)|\\s*(?:h|hrs?|horas?))?\\b",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern DATE_EXPRESSION_PATTERN = Pattern
			.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
	private static final List<String> BOOKING_INTENTS = List.of("reservar_hora", "consultar_disponibilidad_fecha",
			"consultar_disponibilidad_profesional");
	private static final List<String> BOOKING_TRIGGER_WORDS = List.of("reservar", "agendar", "tomar hora", "pedir hora",
			"cita", "hora", "turno", "agenda");
	private static final List<String> WEEKDAY_WORDS = List.of("lunes", "martes", "miercoles", "miércoles", "jueves",
			"viernes", "sabado", "sábado", "domingo");

	private static final List<String> RISK_KEYWORDS = List.of("embarazo", "embarazada", "lactancia", "alergia",
			"alergico", "alergica", "medicamento", "isotretinoina", "anticoagulante", "diabetes", "herida", "infeccion",
			"fiebre", "marcapasos", "trombosis", "cancer", "dolor fuerte", "sangrado", "quemadura", "diagnostico",
			"enfermedad");

	private static final List<String> SENSITIVE_POST_TREATMENT_KEYWORDS = List.of("reaccion", "reacción", "ardio",
			"ardió", "ardor", "inflamo", "inflamó", "inflamada", "inflamado", "alergia", "irritacion", "irritación",
			"quemadura", "quemado", "quemada", "dolor fuerte", "infeccion", "infección");
	private static final List<String> HUMAN_REQUEST_KEYWORDS = List.of("hablar con una persona", "hablar con alguien",
			"humano", "recepcion", "recepción", "asesor", "asesora", "ejecutivo", "ejecutiva", "persona del equipo",
			"quiero una persona", "me atiende una persona");
	private static final List<String> CANCEL_BOOKING_KEYWORDS = List.of("cancelar", "cancela", "anular", "anula",
			"no podre asistir", "no podré asistir", "cancelacion", "cancelación");
	private static final List<String> CHANGE_BOOKING_KEYWORDS = List.of("reprogramar", "cambiar mi hora",
			"cambiar hora", "cambiar la cita", "mover mi hora", "otra hora", "necesito otra hora");
	private static final List<String> LINK_RESEND_KEYWORDS = List.of("no me llego el link", "no me llegó el link",
			"no me llego el enlace", "no me llegó el enlace", "reenviar", "reenvia", "reenvía", "mandame el link",
			"mándame el link", "mandame el enlace", "mándame el enlace", "no recibi la confirmacion",
			"no recibí la confirmación");
	private static final List<String> LINK_EXPIRED_KEYWORDS = List.of("enlace expiro", "enlace expiró", "link expiro",
			"link expiró", "link vencio", "link venció", "enlace vencio", "enlace venció", "no funciona el enlace",
			"no funciona el link", "me dice expirado");
	private static final List<String> PAYMENT_SIGNAL_KEYWORDS = List.of("senal", "señal", "abono", "abonar", "pago",
			"pagar", "link de pago", "pago online", "webpay", "transferencia");
	private static final List<String> LOCATION_QUERY_KEYWORDS = List.of("donde queda", "dónde queda", "direccion",
			"dirección", "ubicacion", "ubicación", "como llego", "cómo llego", "sucursal", "sede");
	private static final List<String> FACIAL_SERVICE_ALIASES = List.of("limpieza facial", "limpieza facial profunda",
			"limpieza de rostro", "limpieza rostro", "facial", "higiene facial", "tratamiento facial", "limpieza cutis",
			"limpieza de cutis");

	private final AestheticCenterJdbcRepository aestheticCenterJdbcRepository;
	private final OpenAiIntentClient openAiIntentClient;
	private final BusinessLocationJdbcRepository businessLocationJdbcRepository;
	private final TransactionalAgendaBookingService transactionalAgendaBookingService;

	public AestheticCenterService(AestheticCenterJdbcRepository aestheticCenterJdbcRepository,
			OpenAiIntentClient openAiIntentClient, BusinessLocationJdbcRepository businessLocationJdbcRepository,
			TransactionalAgendaBookingService transactionalAgendaBookingService) {
		this.aestheticCenterJdbcRepository = aestheticCenterJdbcRepository;
		this.openAiIntentClient = openAiIntentClient;
		this.businessLocationJdbcRepository = businessLocationJdbcRepository;
		this.transactionalAgendaBookingService = transactionalAgendaBookingService;
	}

	@Transactional(readOnly = true)
	public PagedResponse<AestheticCategoryResponse> listServiceCategories(AuthenticatedUser authenticatedUser, int page,
			int size, Boolean active) {
		return aestheticCenterJdbcRepository.findServiceCategories(authenticatedUser.businessId(), normalizePage(page),
				normalizeSize(size), active);
	}

	@Transactional(readOnly = true)
	public PagedResponse<AestheticCategoryResponse> listProductCategories(AuthenticatedUser authenticatedUser, int page,
			int size, Boolean active) {
		return aestheticCenterJdbcRepository.findProductCategories(authenticatedUser.businessId(), normalizePage(page),
				normalizeSize(size), active);
	}

	@Transactional(readOnly = true)
	public PagedResponse<AestheticServiceResponse> listServices(AuthenticatedUser authenticatedUser, int page, int size,
			String search, String categoryCode, Boolean active) {
		return aestheticCenterJdbcRepository.findServices(authenticatedUser.businessId(), normalizePage(page),
				normalizeSize(size), normalizeOptional(search), normalizeOptional(categoryCode), active);
	}

	@Transactional(readOnly = true)
	public AestheticServiceResponse getService(AuthenticatedUser authenticatedUser, UUID serviceId) {
		return aestheticCenterJdbcRepository.findService(authenticatedUser.businessId(), serviceId);
	}

	@Transactional
	public AestheticServiceResponse createService(AuthenticatedUser authenticatedUser,
			UpsertAestheticServiceRequest request) {
		return aestheticCenterJdbcRepository.insertService(authenticatedUser.businessId(),
				normalizeServiceRequest(request));
	}

	@Transactional
	public AestheticServiceResponse updateService(AuthenticatedUser authenticatedUser, UUID serviceId,
			UpsertAestheticServiceRequest request) {
		return aestheticCenterJdbcRepository.updateService(authenticatedUser.businessId(), serviceId,
				normalizeServiceRequest(request));
	}

	@Transactional(readOnly = true)
	public PagedResponse<AestheticProductResponse> listProducts(AuthenticatedUser authenticatedUser, int page, int size,
			String search, String categoryCode, Boolean active, Boolean lowStockOnly) {
		return aestheticCenterJdbcRepository.findProducts(authenticatedUser.businessId(), normalizePage(page),
				normalizeSize(size), normalizeOptional(search), normalizeOptional(categoryCode), active, lowStockOnly);
	}

	@Transactional(readOnly = true)
	public AestheticProductResponse getProduct(AuthenticatedUser authenticatedUser, UUID productId) {
		return aestheticCenterJdbcRepository.findProduct(authenticatedUser.businessId(), productId);
	}

	@Transactional
	public AestheticProductResponse createProduct(AuthenticatedUser authenticatedUser,
			UpsertAestheticProductRequest request) {
		return aestheticCenterJdbcRepository.insertProduct(authenticatedUser.businessId(),
				normalizeProductRequest(request));
	}

	@Transactional
	public AestheticProductResponse updateProduct(AuthenticatedUser authenticatedUser, UUID productId,
			UpsertAestheticProductRequest request) {
		return aestheticCenterJdbcRepository.updateProduct(authenticatedUser.businessId(), productId,
				normalizeProductRequest(request));
	}

	@Transactional(readOnly = true)
	public PagedResponse<AestheticBusinessRuleResponse> listRules(AuthenticatedUser authenticatedUser, int page,
			int size, String ruleType, Boolean active) {
		return aestheticCenterJdbcRepository.findRules(authenticatedUser.businessId(), normalizePage(page),
				normalizeSize(size), normalizeOptional(ruleType), active);
	}

	@Transactional(readOnly = true)
	public AestheticBusinessRuleResponse getRule(AuthenticatedUser authenticatedUser, UUID ruleId) {
		return aestheticCenterJdbcRepository.findRule(authenticatedUser.businessId(), ruleId);
	}

	@Transactional
	public AestheticBusinessRuleResponse createRule(AuthenticatedUser authenticatedUser,
			UpsertAestheticBusinessRuleRequest request) {
		return aestheticCenterJdbcRepository.insertRule(authenticatedUser.businessId(), normalizeRuleRequest(request));
	}

	@Transactional
	public AestheticBusinessRuleResponse updateRule(AuthenticatedUser authenticatedUser, UUID ruleId,
			UpsertAestheticBusinessRuleRequest request) {
		return aestheticCenterJdbcRepository.updateRule(authenticatedUser.businessId(), ruleId,
				normalizeRuleRequest(request));
	}

	@Transactional
	public IntentAnalysisResponse analyzeIntent(AuthenticatedUser authenticatedUser, IntentAnalysisRequest request) {
		return analyzeInboundMessage(authenticatedUser.businessId(), request.customerId(), request.conversationId(),
				request.message());
	}

	@Transactional
	public IntentAnalysisResponse analyzeInboundMessage(UUID businessId, UUID customerId, UUID conversationId,
			String rawMessage) {
		String message = rawMessage.trim();
		BusinessSnapshot snapshot = buildSnapshot(businessId, message);
		IntentAnalysisResponse aiResponse = openAiIntentClient.analyze(message, snapshot.toPromptContext())
				.orElseGet(() -> fallbackAnalyze(message, snapshot));
		IntentAnalysisResponse guardedResponse = applySafetyAndOperationalGuards(aiResponse, message, snapshot,
				customerId, conversationId);
		UUID safeCustomerId = resolveExistingCustomerId(businessId, customerId);
		UUID safeConversationId = resolveExistingConversationId(businessId, conversationId);
		aestheticCenterJdbcRepository.insertIntentLog(businessId, safeCustomerId, safeConversationId, message,
				guardedResponse);
		return guardedResponse;
	}

	@Transactional(readOnly = true)
	public PagedResponse<AestheticIntentLogResponse> listIntentLogs(AuthenticatedUser authenticatedUser, int page,
			int size) {
		return aestheticCenterJdbcRepository.findIntentLogs(authenticatedUser.businessId(), normalizePage(page),
				normalizeSize(size));
	}

	private UUID resolveExistingCustomerId(UUID businessId, UUID customerId) {
		if (customerId == null) {
			return null;
		}
		return aestheticCenterJdbcRepository.customerExists(businessId, customerId) ? customerId : null;
	}

	private UUID resolveExistingConversationId(UUID businessId, UUID conversationId) {
		if (conversationId == null) {
			return null;
		}
		return aestheticCenterJdbcRepository.conversationExists(businessId, conversationId) ? conversationId : null;
	}

	private BusinessSnapshot buildSnapshot(UUID businessId, String message) {
		PagedResponse<AestheticServiceResponse> services = aestheticCenterJdbcRepository.findServices(businessId, 0,
				MAX_PAGE_SIZE, null, null, true);
		PagedResponse<AestheticProductResponse> products = aestheticCenterJdbcRepository.findProducts(businessId, 0,
				MAX_PAGE_SIZE, null, null, true, false);
		PagedResponse<AestheticBusinessRuleResponse> rules = aestheticCenterJdbcRepository.findRules(businessId, 0,
				MAX_PAGE_SIZE, null, true);
		List<AestheticPromotionSummary> promotions = aestheticCenterJdbcRepository.findActivePromotions(businessId);
		Optional<AestheticServiceResponse> service = findMatchingService(message, services.items());
		Optional<AestheticProductResponse> product = findMatchingProduct(message, products.items());
		return new BusinessSnapshot(businessId, services.items(), products.items(), rules.items(), promotions,
				service.orElse(null), product.orElse(null));
	}

	private IntentAnalysisResponse fallbackAnalyze(String message, BusinessSnapshot snapshot) {
		String normalized = normalizeForMatching(message);
		boolean hasRisk = containsAny(normalized, RISK_KEYWORDS);
		Optional<AestheticServiceResponse> service = Optional.ofNullable(snapshot.matchedService());
		Optional<AestheticProductResponse> product = Optional.ofNullable(snapshot.matchedProduct());
		String serviceName = service.map(AestheticServiceResponse::name).orElse(null);
		String productName = product.map(AestheticProductResponse::name).orElse(null);
		String intent;
		boolean requiresDb = false;
		boolean handoff = hasRisk;
		String handoffReason = hasRisk
				? "Consulta sensible o clinica: requiere evaluacion profesional antes de recomendar o confirmar tratamiento."
				: null;
		String suggested;

		if (hasRisk) {
			intent = "derivar_atencion_humana";
			suggested = safetyHandoffResponse(service.orElse(null));
		} else if (containsAny(normalized, List.of("cancelar", "anular", "cancela", "suspender"))) {
			intent = "cancelar_reserva";
			requiresDb = true;
			suggested = "Puedo ayudarte a cancelar, pero antes necesito identificar tu reserva activa. Confirma tu nombre, telefono y horario aproximado de la cita.";
		} else if (containsAny(normalized,
				List.of("reprogramar", "cambiar hora", "cambiar mi hora", "mover", "otra hora"))) {
			intent = "reprogramar_reserva";
			requiresDb = true;
			suggested = "Puedo ayudarte a reprogramar. Necesito identificar tu reserva activa y revisar nuevos horarios disponibles antes de confirmar el cambio.";
		} else if (containsAny(normalized,
				List.of("disponibilidad", "disponible", "tienen hora", "hay hora", "horario", "agenda para"))) {
			intent = serviceName == null ? "consultar_disponibilidad_fecha" : "reservar_hora";
			requiresDb = true;
			suggested = serviceName == null
					? "Puedo revisar disponibilidad, pero primero dime que servicio quieres agendar y para que dia."
					: serviceAvailabilityResponse(service.get());
		} else if (containsAny(normalized, List.of("reservar", "agendar", "hora", "cita", "agenda"))) {
			intent = "reservar_hora";
			requiresDb = true;
			suggested = serviceName == null
					? "Claro. Que servicio te gustaria reservar? Tengo categorias como faciales, corporales, depilacion, manicure, pedicure, pestanas, cejas, peluqueria, maquillaje y evaluacion estetica."
					: serviceAvailabilityResponse(service.get());
		} else if (containsAny(normalized, List.of("precio", "valor", "cuanto sale", "cuanto cuesta", "costo"))) {
			if (service.isPresent()) {
				intent = "consultar_precio_servicio";
				suggested = servicePriceResponse(service.get());
			} else if (product.isPresent()) {
				intent = "consultar_productos";
				suggested = productPriceResponse(product.get());
			} else {
				intent = "intencion_no_clara";
				requiresDb = true;
				suggested = "Indica el servicio o producto especifico para revisar el valor vigente en el catalogo interno.";
			}
		} else if (containsAny(normalized, List.of("duracion", "demora", "cuanto dura", "tiempo"))) {
			intent = "consultar_duracion_servicio";
			suggested = serviceName != null
					? serviceDurationResponse(service.get())
					: "Indica el servicio para revisar su duracion configurada.";
			requiresDb = serviceName == null;
		} else if (containsAny(normalized,
				List.of("contraindicacion", "contraindicaciones", "puedo hacerme", "me puedo hacer", "riesgo"))) {
			intent = "consultar_contraindicaciones";
			handoff = true;
			handoffReason = "Consulta de seguridad estetica: requiere validacion profesional si aplica a una condicion personal.";
			suggested = serviceName != null
					? contraindicationResponse(service.get())
					: "Para revisar contraindicaciones necesito saber que tratamiento te interesa. Si tienes embarazo, alergias, medicamentos, enfermedades, heridas o infeccion, debe evaluarte una profesional.";
		} else if (containsAny(normalized, List.of("cuidados", "despues", "post", "posterior", "post tratamiento"))) {
			intent = "solicitar_cuidados_posteriores";
			suggested = serviceName == null
					? "Indica el tratamiento realizado o reservado para revisar sus cuidados posteriores configurados."
					: aftercareResponse(service.get());
			requiresDb = serviceName == null;
		} else if (containsAny(normalized, List.of("producto", "crema", "serum", "protector", "bloqueador", "kit",
				"gift", "stock", "mascarilla", "exfoliante", "shampoo", "aceite", "esmalte"))) {
			if (product.isPresent()) {
				intent = "consultar_productos";
				suggested = productDetailResponse(product.get());
			} else if (service.isPresent()) {
				intent = "recomendar_productos";
				suggested = productRecommendationForService(snapshot, service.get());
			} else {
				intent = "consultar_productos";
				suggested = productCatalogResponse(snapshot.products());
			}
		} else if (containsAny(normalized, List.of("promocion", "promociones", "descuento", "pack", "oferta"))) {
			intent = "consultar_promociones";
			suggested = promotionResponse(snapshot.promotions());
		} else if (containsAny(normalized,
				List.of("servicios", "catalogo", "tratamientos", "que hacen", "que ofrecen"))) {
			intent = "consultar_servicios_disponibles";
			suggested = serviceCatalogResponse(snapshot.services());
		} else if (containsAny(normalized, List.of("evaluacion", "asesoria", "recomiendas", "recomendacion",
				"que me sirve", "piel", "manchas", "flacidez", "acne", "arrugas", "relajar", "reducir"))) {
			intent = "pedir_recomendacion_tratamiento";
			if (service.isPresent()) {
				suggested = recommendationResponseForService(service.get(), snapshot);
				handoff = serviceRequiresEvaluation(service.get());
				handoffReason = handoff ? "El servicio sugerido requiere evaluacion previa antes de confirmar." : null;
			} else {
				suggested = "Para recomendar con precision necesito saber tu objetivo principal: facial, corporal, depilacion, unas, pestanas, cejas, cabello o maquillaje. Si hay sensibilidad, embarazo, alergias o medicamentos, debe revisarlo una profesional.";
			}
		} else if (containsAny(normalized, List.of("pago", "transferencia", "tarjeta", "efectivo", "webpay"))) {
			intent = "consultar_medios_pago";
			requiresDb = true;
			suggested = "Puedo ayudarte con medios de pago, pero debo revisar la configuracion vigente del centro antes de confirmar si aplica efectivo, tarjeta, transferencia o Webpay.";
		} else if (containsAny(normalized,
				List.of("estado reserva", "mi reserva", "confirmada", "confirmar mi hora"))) {
			intent = "consultar_estado_reserva";
			requiresDb = true;
			suggested = "Para revisar el estado de tu reserva necesito tu nombre, telefono y fecha aproximada de la cita.";
		} else if (containsAny(normalized, List.of("historial", "tratamientos anteriores", "ultima vez", "me hice"))) {
			intent = "consultar_historial_cliente";
			requiresDb = true;
			suggested = "Para revisar historial de tratamientos necesito identificar al cliente y consultar los registros internos del centro.";
		} else {
			intent = "intencion_no_clara";
			suggested = "No entendi completamente tu solicitud. Puedo ayudarte con servicios, precios, duracion, reservas, productos, promociones, contraindicaciones, cuidados posteriores o reprogramaciones.";
		}

		return new IntentAnalysisResponse(intent, confidenceFor(intent, serviceName, productName, hasRisk),
				new IntentEntitiesResponse(serviceName, productName, null, null, null, null), requiresDb, handoff,
				handoffReason, suggested, openAiIntentClient.modelName() + ":fallback-rules");
	}

	private IntentAnalysisResponse applySafetyAndOperationalGuards(IntentAnalysisResponse response, String message,
			BusinessSnapshot snapshot, UUID customerId, UUID conversationId) {
		String normalized = normalizeForMatching(message);
		boolean hasRisk = containsAny(normalized, RISK_KEYWORDS)
				|| containsAny(normalized, SENSITIVE_POST_TREATMENT_KEYWORDS);
		IntentEntitiesResponse currentEntities = response.entidades() == null
				? IntentEntitiesResponse.empty()
				: response.entidades();
		String serviceName = firstNonBlank(currentEntities.servicio(),
				Optional.ofNullable(snapshot.matchedService()).map(AestheticServiceResponse::name).orElse(null));
		String productName = firstNonBlank(currentEntities.producto(),
				Optional.ofNullable(snapshot.matchedProduct()).map(AestheticProductResponse::name).orElse(null));
		String date = firstNonBlank(currentEntities.fecha(), inferDateExpression(message));
		String time = firstNonBlank(currentEntities.hora(), inferTimeExpression(message));
		IntentEntitiesResponse guardedEntities = new IntentEntitiesResponse(serviceName, productName, date, time,
				currentEntities.profesional(), currentEntities.cliente());

		Optional<IntentAnalysisResponse> specialized = deterministicSpecializedResponse(response, message, normalized,
				snapshot, guardedEntities, serviceName, productName, date, time, hasRisk);
		if (specialized.isPresent()) {
			return specialized.get();
		}

		boolean operationalIntent = List.of("consultar_servicios_disponibles", "consultar_precio_servicio",
				"consultar_duracion_servicio", "reservar_hora", "cancelar_reserva", "reprogramar_reserva",
				"consultar_productos", "recomendar_productos", "consultar_promociones",
				"consultar_disponibilidad_fecha", "consultar_disponibilidad_profesional", "consultar_estado_reserva",
				"consultar_historial_cliente", "consultar_medios_pago").contains(response.intencion());

		boolean evaluationSensitiveIntent = List.of("reservar_hora", "pedir_recomendacion_tratamiento",
				"consultar_contraindicaciones", "solicitar_evaluacion_estetica", "consultar_disponibilidad_fecha",
				"consultar_disponibilidad_profesional").contains(response.intencion());
		boolean serviceNeedsProfessionalValidation = evaluationSensitiveIntent
				&& serviceRequiresEvaluation(snapshot.matchedService());
		boolean handoff = response.requiereDerivacionHumana() || hasRisk || serviceNeedsProfessionalValidation;
		String handoffReason = response.motivoDerivacion();
		if (hasRisk) {
			handoffReason = "Consulta sensible: reacción, alergia, irritación, dolor, infección u otro antecedente requiere evaluación profesional.";
		} else if (serviceNeedsProfessionalValidation && handoffReason == null) {
			handoffReason = "El servicio identificado requiere evaluacion previa o consentimiento informado antes de confirmar.";
		}

		String suggested = response.respuestaSugerida();
		if (handoff && (suggested == null || !normalizeForMatching(suggested).contains("profesional"))) {
			suggested = sensitiveHandoffResponse();
		}

		boolean bookingFlow = isBookingFlow(response.intencion(), normalized)
				|| isBookingLikeExpression(normalized, serviceName, date);
		if (!handoff && bookingFlow) {
			suggested = deterministicBookingResponse(message, serviceName, date, time, snapshot, customerId,
					conversationId);
		}

		BigDecimal adjustedConfidence = clampConfidence(response.confianza());
		if (!handoff && bookingFlow && serviceName != null && !serviceName.isBlank()) {
			adjustedConfidence = adjustedConfidence
					.max(confidenceFor("reservar_hora", serviceName, productName, hasRisk));
		}

		return new IntentAnalysisResponse(bookingFlow ? "reservar_hora" : response.intencion(), adjustedConfidence,
				guardedEntities, response.requiereConsultaBaseDatos() || operationalIntent || bookingFlow, handoff,
				handoffReason, suggested, response.modelo());
	}

	private Optional<IntentAnalysisResponse> deterministicSpecializedResponse(IntentAnalysisResponse original,
			String message, String normalized, BusinessSnapshot snapshot, IntentEntitiesResponse entities,
			String serviceName, String productName, String date, String time, boolean hasRisk) {
		String model = original.modelo();
		if (hasRisk || containsAny(normalized, SENSITIVE_POST_TREATMENT_KEYWORDS)) {
			return Optional.of(new IntentAnalysisResponse("caso_sensible_post_tratamiento", BigDecimal.valueOf(0.95),
					entities, true, true,
					"Caso sensible post tratamiento o antecedente de seguridad: requiere revisión humana.",
					sensitiveHandoffResponse(), model));
		}
		if (containsAny(normalized, HUMAN_REQUEST_KEYWORDS)) {
			return Optional.of(new IntentAnalysisResponse("solicitar_humano", BigDecimal.valueOf(0.95), entities, true,
					true, "Cliente solicita atención humana explícita.",
					"Te voy a derivar con una persona del equipo para ayudarte mejor. Un momento por favor.", model));
		}
		if (containsAny(normalized, CANCEL_BOOKING_KEYWORDS)) {
			return Optional.of(new IntentAnalysisResponse("cancelar_reserva", BigDecimal.valueOf(0.9), entities, true,
					false, null, WhatsAppMessageFormatter.cancellationRequest(), model));
		}
		if (containsAny(normalized, CHANGE_BOOKING_KEYWORDS)) {
			return Optional.of(new IntentAnalysisResponse("reprogramar_reserva", BigDecimal.valueOf(0.9), entities,
					true, false, null, WhatsAppMessageFormatter.rescheduleRequest(), model));
		}
		if (containsAny(normalized, LINK_RESEND_KEYWORDS)) {
			return Optional.of(new IntentAnalysisResponse("reenviar_enlace_confirmacion", BigDecimal.valueOf(0.9),
					entities, true, false, null,
					"🔁 *Reenvío de enlace de confirmación*\n\nRevisaré si tienes una reserva temporal vigente para reenviar el enlace de confirmación.\n\nSi no la encuentro, te pediré los datos mínimos para crear una nueva reserva.",
					model));
		}
		if (containsAny(normalized, LINK_EXPIRED_KEYWORDS)) {
			return Optional.of(new IntentAnalysisResponse("enlace_expirado", BigDecimal.valueOf(0.9), entities, true,
					false, null, WhatsAppMessageFormatter.confirmationLinkExpired(), model));
		}
		if (containsAny(normalized, PAYMENT_SIGNAL_KEYWORDS)
				&& !containsAny(normalized, List.of("reservar", "agendar", "cita", "hora"))) {
			return Optional.of(new IntentAnalysisResponse("consultar_pago_senal", BigDecimal.valueOf(0.88), entities,
					true, false, null, paymentSignalResponse(serviceName), model));
		}
		if (containsAny(normalized, PAYMENT_SIGNAL_KEYWORDS)
				&& containsAny(normalized, List.of("senal", "señal", "abono", "abonar", "pago"))) {
			return Optional.of(new IntentAnalysisResponse("consultar_pago_senal", BigDecimal.valueOf(0.88), entities,
					true, false, null, paymentSignalResponse(serviceName), model));
		}
		if (isLocationQuery(normalized)) {
			return Optional.of(new IntentAnalysisResponse("consultar_ubicacion", BigDecimal.valueOf(0.88), entities,
					true, false, null, locationResponse(snapshot.businessId(), message), model));
		}
		return Optional.empty();
	}

	private boolean isBookingFlow(String intent, String normalizedMessage) {
		if (intent != null && BOOKING_INTENTS.contains(intent)) {
			return true;
		}
		return containsAny(normalizedMessage, BOOKING_TRIGGER_WORDS);
	}

	private boolean isBookingLikeExpression(String normalizedMessage, String serviceName, String date) {
		if (serviceName == null || serviceName.isBlank()) {
			return false;
		}
		return date != null && !date.isBlank() || containsAny(normalizedMessage,
				List.of("me gustaria", "me gustaría", "quisiera", "necesito", "quiero"));
	}

	private String normalizeBookingIntent(String intent, String normalizedMessage) {
		return isBookingFlow(intent, normalizedMessage) ? "reservar_hora" : intent;
	}

	private String deterministicBookingResponse(String message, String serviceName, String date, String time,
			BusinessSnapshot snapshot, UUID customerId, UUID conversationId) {
		String traceId = AiTraceLogger.newTraceId("AESTHETIC");
		AiTraceLogger.info("AESTHETIC_BOOKING_RESPONSE_STARTED", traceId, conversationId, null,
				"AestheticCenterService",
				"message=" + message + " serviceName=" + serviceName + " date=" + date + " time=" + time);
		AestheticServiceResponse matchedService = snapshot.matchedService();
		String displayService = serviceName == null || serviceName.isBlank() ? null : displayServiceName(serviceName);
		List<BusinessLocationRecord> activeLocations = businessLocationJdbcRepository.findActive(snapshot.businessId());
		Optional<BusinessLocationRecord> mentionedLocation = findMentionedLocation(activeLocations, message);
		boolean requiresLocation = activeLocations.size() > 1 && mentionedLocation.isEmpty();

		if (displayService == null) {
			return WhatsAppMessageFormatter.askService();
		}

		String partialLocationText = mentionedLocation.map(location -> " en " + location.name()).orElse("");
		String intro = "Perfecto ✅ Puedo ayudarte a reservar " + displayService + partialLocationText
				+ bookingDateTimeText(date, time) + ".";

		if (requiresLocation) {
			return intro + "\n\nAntes de validar disponibilidad, dime en qué sucursal prefieres atenderte. Opciones: "
					+ renderLocationOptions(activeLocations) + ".";
		}
		if (date == null || date.isBlank()) {
			return intro + "\n\n¿Qué día te gustaría agendar?";
		}
		if (time == null || time.isBlank()) {
			return intro + "\n\n¿Qué horario prefieres: mañana, tarde o una hora específica?";
		}

		Optional<String> transactionalResponse = transactionalAgendaBookingService.createTemporaryBookingLink(
				snapshot.businessId(), customerId, conversationId, null, null, message, displayService,
				mentionedLocation.map(BusinessLocationRecord::name).orElse(null), date, time, false, false, traceId,
				conversationId);
		if (transactionalResponse.isPresent()) {
			return transactionalResponse.get();
		}

		String locationText = mentionedLocation.map(BusinessLocationRecord::name).or(
				() -> activeLocations.size() == 1 ? Optional.of(activeLocations.getFirst().name()) : Optional.empty())
				.map(location -> " en " + location).orElse("");
		String durationText = matchedService == null
				? ""
				: " Duración aproximada: " + matchedService.durationMinutes() + " minutos.";
		return "Perfecto ✅ Tengo los datos principales para revisar disponibilidad de " + displayService + locationText
				+ " para " + date + " a las " + time + "." + durationText
				+ "\n\nVoy a validar la agenda digital antes de confirmar. Si está disponible, crearé una reserva temporal y enviaré el enlace de confirmación por WhatsApp.";

	}

	private Optional<BusinessLocationRecord> findMentionedLocation(List<BusinessLocationRecord> locations,
			String message) {
		String normalized = normalizeForMatching(message);
		if (normalized.isBlank()) {
			return Optional.empty();
		}
		return locations.stream().map(location -> new LocationMatch(location, scoreLocation(normalized, location)))
				.filter(match -> match.score() > 0).max(Comparator.comparingInt(LocationMatch::score))
				.map(LocationMatch::location);
	}

	private int scoreLocation(String normalizedMessage, BusinessLocationRecord location) {
		int best = 0;
		String name = normalizeForMatching(location.name());
		String code = normalizeForMatching(location.code());
		String commune = normalizeForMatching(location.commune());
		String city = normalizeForMatching(location.city());
		if (!name.isBlank() && normalizedMessage.contains(name)) {
			best = Math.max(best, 1000 + name.length());
		}
		if (!code.isBlank() && normalizedMessage.contains(code)) {
			best = Math.max(best, 950 + code.length());
		}
		if (normalizedMessage.contains("sucursal " + name) || normalizedMessage.contains("sede " + name)
				|| normalizedMessage.contains("en " + name)) {
			best = Math.max(best, 1200 + name.length());
		}
		if (normalizedMessage.contains("provi") && (name.contains("providencia") || code.contains("providencia"))) {
			best = Math.max(best, 1150);
		}
		if (!commune.isBlank() && normalizedMessage.contains(commune)) {
			best = Math.max(best, 350 + commune.length());
		}
		if (!city.isBlank() && normalizedMessage.contains(city)) {
			best = Math.max(best, 150 + city.length());
		}
		return best;
	}

	private record LocationMatch(BusinessLocationRecord location, int score) {
	}

	private String renderLocationOptions(List<BusinessLocationRecord> locations) {
		if (locations.isEmpty()) {
			return "sede principal";
		}
		return locations.stream().map(BusinessLocationRecord::name).collect(Collectors.joining(", "));
	}

	private String bookingDateTimeText(String date, String time) {
		if (date != null && !date.isBlank() && time != null && !time.isBlank()) {
			return " para " + date + " a las " + time;
		}
		if (date != null && !date.isBlank()) {
			return " para " + date;
		}
		if (time != null && !time.isBlank()) {
			return " a las " + time;
		}
		return "";
	}

	private String inferDateExpression(String message) {
		String normalized = normalizeForMatching(message);
		for (String weekday : WEEKDAY_WORDS) {
			String normalizedWeekday = normalizeForMatching(weekday);
			if (containsWholeWord(normalized, normalizedWeekday)) {
				return weekday;
			}
		}
		if (normalized.contains("pasado manana")) {
			return "pasado mañana";
		}
		if (containsWholeWord(normalized, "hoy")) {
			return "hoy";
		}
		if (containsWholeWord(normalized, "manana")
				&& !containsAny(normalized, List.of("en la manana", "por la manana"))) {
			return "mañana";
		}
		if (containsAny(normalized, List.of("esta semana", "semana actual"))) {
			return "esta semana";
		}
		if (containsAny(normalized, List.of("proxima semana", "próxima semana", "la otra semana"))) {
			return "próxima semana";
		}
		Matcher matcher = DATE_EXPRESSION_PATTERN.matcher(message);
		return matcher.find() ? matcher.group() : null;
	}

	private String inferTimeExpression(String message) {
		Matcher matcher = TIME_EXPRESSION_PATTERN.matcher(message == null ? "" : message);
		if (!matcher.find()) {
			return null;
		}
		int hour = Integer.parseInt(matcher.group(1));
		int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
		return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
	}

	private boolean containsWholeWord(String normalizedText, String token) {
		if (normalizedText == null || token == null || token.isBlank()) {
			return false;
		}
		return java.util.Arrays.stream(normalizedText.split(" ")).anyMatch(token::equals);
	}

	private String displayServiceName(String value) {
		if (value == null || value.isBlank()) {
			return "el servicio";
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private String serviceCatalogResponse(List<AestheticServiceResponse> services) {
		String grouped = services.stream().collect(Collectors.groupingBy(AestheticServiceResponse::categoryName))
				.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
				.map(entry -> entry.getKey() + ": "
						+ entry.getValue().stream().sorted(Comparator.comparing(AestheticServiceResponse::name))
								.limit(8).map(AestheticServiceResponse::name).collect(Collectors.joining(", ")))
				.collect(Collectors.joining(". "));
		return "Tenemos servicios en estas categorias: " + grouped
				+ ". Si quieres, dime cual te interesa y te respondo valor, duracion, cuidados o disponibilidad.";
	}

	private String productCatalogResponse(List<AestheticProductResponse> products) {
		String grouped = products.stream().collect(Collectors.groupingBy(AestheticProductResponse::categoryName))
				.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
				.map(entry -> entry.getKey() + ": "
						+ entry.getValue().stream().sorted(Comparator.comparing(AestheticProductResponse::name))
								.limit(5).map(AestheticProductResponse::name).collect(Collectors.joining(", ")))
				.collect(Collectors.joining(". "));
		return "Tenemos productos disponibles por categoria: " + grouped
				+ ". Indica que producto o necesidad buscas para revisar precio, stock y restricciones.";
	}

	private String servicePriceResponse(AestheticServiceResponse service) {
		return "El servicio " + service.name() + " tiene un valor base de " + formatCurrency(service.priceBase())
				+ " y una duracion estimada de " + service.durationMinutes() + " minutos. "
				+ safetyNoteForService(service)
				+ " Para reservar, debo revisar disponibilidad real de agenda antes de confirmar una hora.";
	}

	private String serviceDurationResponse(AestheticServiceResponse service) {
		return "La duracion estimada de " + service.name() + " es de " + service.durationMinutes()
				+ " minutos. Su valor base configurado es " + formatCurrency(service.priceBase()) + ". "
				+ safetyNoteForService(service);
	}

	private String serviceAvailabilityResponse(AestheticServiceResponse service) {
		return "Puedo ayudarte a reservar " + service.name() + ". Dura aproximadamente " + service.durationMinutes()
				+ " minutos y su valor base es " + formatCurrency(service.priceBase())
				+ ". Debo revisar agenda, profesional y cabina disponibles antes de confirmar horario. "
				+ safetyNoteForService(service);
	}

	private String contraindicationResponse(AestheticServiceResponse service) {
		return "Para " + service.name() + ", las contraindicaciones configuradas son: " + service.contraindications()
				+ " Si esto aplica a tu caso, debe evaluarte una profesional antes de confirmar el tratamiento. No realizamos diagnosticos por WhatsApp.";
	}

	private String aftercareResponse(AestheticServiceResponse service) {
		return "Cuidados posteriores para " + service.name() + ": " + service.aftercareRecommendations()
				+ " Si presentas dolor intenso, reaccion alergica, herida, infeccion o una molestia relevante, contacta a una profesional del centro.";
	}

	private String productPriceResponse(AestheticProductResponse product) {
		return product.name() + " tiene un valor de " + formatCurrency(product.price()) + ". Stock disponible: "
				+ product.stock() + " unidad(es). " + productStockNote(product) + " Restricciones: "
				+ product.usageRestrictions();
	}

	private String productDetailResponse(AestheticProductResponse product) {
		return product.name() + ": " + product.description() + " Valor: " + formatCurrency(product.price())
				+ ". Stock disponible: " + product.stock() + " unidad(es). " + productStockNote(product)
				+ " Reglas de recomendacion: " + product.recommendationRules() + " Restricciones: "
				+ product.usageRestrictions();
	}

	private String productRecommendationForService(BusinessSnapshot snapshot, AestheticServiceResponse service) {
		String normalizedService = normalizeForMatching(service.name());
		List<AestheticProductResponse> recommended = snapshot.products().stream()
				.filter(product -> normalizeForMatching(product.compatibleServices()).contains(normalizedService)
						|| tokensOverlap(normalizeForMatching(product.compatibleServices()), normalizedService))
				.sorted(Comparator.comparing(AestheticProductResponse::name)).limit(5).toList();
		if (recommended.isEmpty()) {
			return "Para " + service.name()
					+ " no encontre un producto compatible especifico en el catalogo. Puedo derivar a una asesora para recomendar rutina segura segun tu piel y antecedentes.";
		}
		return "Para " + service.name()
				+ " puedo sugerir estos productos compatibles, validando alergias y restricciones: "
				+ recommended.stream()
						.map(product -> product.name() + " (" + formatCurrency(product.price()) + ", stock "
								+ product.stock() + ")")
						.collect(Collectors.joining("; "))
				+ ". No se recomienda usar productos contraindicados ni aplicar activos si hay irritacion.";
	}

	private String recommendationResponseForService(AestheticServiceResponse service, BusinessSnapshot snapshot) {
		return "Segun tu consulta, el servicio mas relacionado es " + service.name() + ". Descripcion: "
				+ service.description() + " Valor base: " + formatCurrency(service.priceBase()) + ", duracion: "
				+ service.durationMinutes() + " minutos. " + safetyNoteForService(service) + " "
				+ productRecommendationForService(snapshot, service);
	}

	private String promotionResponse(List<AestheticPromotionSummary> promotions) {
		if (promotions.isEmpty()) {
			return "No encontre promociones activas configuradas en este momento. Puedo revisar servicios o productos disponibles si me indicas que necesitas.";
		}
		return "Promociones activas: "
				+ promotions.stream().limit(8)
						.map(promotion -> promotion.name() + " - " + promotion.description() + " ("
								+ promotion.discountLabel() + "). Condiciones: " + promotion.conditions())
						.collect(Collectors.joining("; "))
				+ ". Las promociones se aplican solo si cumplen fecha, stock, servicio y condiciones configuradas.";
	}

	private String sensitiveHandoffResponse() {
		return WhatsAppMessageFormatter.sensitiveCase();
	}

	private String paymentSignalResponse(String serviceName) {
		if (serviceName != null && !serviceName.isBlank()) {
			return "Revisaré si " + displayServiceName(serviceName)
					+ " requiere señal según las reglas del negocio. No voy a inventar montos; usaré solo la configuración registrada.";
		}
		return "Para responder sobre señal o pago debo revisar la regla configurada del servicio o reserva. No voy a inventar montos. ¿Qué servicio quieres reservar?";
	}

	private boolean isLocationQuery(String normalized) {
		return containsAny(normalized, LOCATION_QUERY_KEYWORDS) && !containsAny(normalized, BOOKING_TRIGGER_WORDS);
	}

	private String locationResponse(UUID businessId, String message) {
		List<BusinessLocationRecord> activeLocations = businessLocationJdbcRepository.findActive(businessId);
		Optional<BusinessLocationRecord> mentioned = findMentionedLocation(activeLocations, message);
		if (mentioned.isPresent()) {
			BusinessLocationRecord location = mentioned.get();
			String address = location.address() == null || location.address().isBlank()
					? "dirección no configurada"
					: location.address();
			if ("dirección no configurada".equals(address)) {
				return "Tengo registrada la sucursal " + location.name()
						+ ", pero falta configurar su dirección o enlace de mapa. Te derivaré con una persona del equipo para confirmarlo.";
			}
			return "La sucursal " + location.name() + " está ubicada en:\n" + address;
		}
		if (activeLocations.isEmpty()) {
			return "No encontré sucursales activas configuradas. Te derivaré con una persona del equipo para confirmar la ubicación.";
		}
		return "Tenemos estas sucursales activas: " + renderLocationOptions(activeLocations)
				+ ". ¿Sobre cuál necesitas la dirección?";
	}

	private String safetyHandoffResponse(AestheticServiceResponse service) {
		if (service == null) {
			return "Gracias por contarnos. Por seguridad, esta consulta debe revisarla una profesional del centro antes de confirmar o recomendar un tratamiento.";
		}
		return "Gracias por contarnos. Para " + service.name()
				+ " existe informacion de seguridad que debe revisar una profesional antes de confirmar o recomendar el tratamiento. "
				+ "Contraindicaciones configuradas: " + service.contraindications();
	}

	private String safetyNoteForService(AestheticServiceResponse service) {
		if (service.requiresPriorEvaluation() && service.requiresInformedConsent()) {
			return "Este servicio requiere evaluacion previa y consentimiento informado.";
		}
		if (service.requiresPriorEvaluation()) {
			return "Este servicio requiere evaluacion previa antes de confirmar.";
		}
		if (service.requiresInformedConsent()) {
			return "Este servicio requiere consentimiento informado.";
		}
		return "No se debe confirmar si existen contraindicaciones personales no evaluadas.";
	}

	private String productStockNote(AestheticProductResponse product) {
		if (product.stock() <= 0) {
			return "Actualmente no hay stock disponible; no debe venderse sin reposicion.";
		}
		if (product.lowStock()) {
			return "Stock bajo: conviene validar disponibilidad antes de comprometer venta.";
		}
		return "Stock suficiente segun catalogo actual.";
	}

	private boolean tokensOverlap(String left, String right) {
		for (String token : right.split("\\s+")) {
			if (token.length() > 4 && left.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private String formatCurrency(BigDecimal value) {
		if (value == null) {
			return "$0";
		}
		return "$" + String.format(Locale.forLanguageTag("es-CL"), "%,.0f", value);
	}

	private boolean serviceRequiresEvaluation(AestheticServiceResponse service) {
		return service != null && (service.requiresPriorEvaluation() || service.requiresInformedConsent());
	}

	private Optional<AestheticServiceResponse> findMatchingService(String message,
			List<AestheticServiceResponse> services) {
		String normalizedMessage = normalizeForMatching(message);

		Optional<AestheticServiceResponse> explicitFacial = findFacialServiceByAlias(normalizedMessage, services);
		if (explicitFacial.isPresent()) {
			return explicitFacial;
		}

		return services.stream()
				.filter(service -> normalizedMessage.contains(normalizeForMatching(service.name()))
						|| containsTokens(normalizedMessage, normalizeForMatching(service.name()))
						|| serviceAliasMatches(normalizedMessage, service))
				.findFirst();
	}

	private Optional<AestheticServiceResponse> findFacialServiceByAlias(String normalizedMessage,
			List<AestheticServiceResponse> services) {
		if (!containsAny(normalizedMessage, FACIAL_SERVICE_ALIASES)) {
			return Optional.empty();
		}
		return services.stream().filter(service -> normalizeForMatching(service.name()).contains("limpieza facial"))
				.findFirst();
	}

	private boolean serviceAliasMatches(String normalizedMessage, AestheticServiceResponse service) {
		String normalizedServiceName = normalizeForMatching(service.name());
		if (normalizedServiceName.contains("limpieza facial")
				&& containsAny(normalizedMessage, FACIAL_SERVICE_ALIASES)) {
			return true;
		}
		if (normalizedServiceName.contains("depilacion bozo")
				&& containsAny(normalizedMessage, List.of("bozo", "depilacion bozo"))) {
			return true;
		}
		if (normalizedServiceName.contains("depilacion axilas")
				&& containsAny(normalizedMessage, List.of("axilas", "depilacion axilas"))) {
			return true;
		}
		if (normalizedServiceName.contains("depilacion rostro") && containsAny(normalizedMessage,
				List.of("depilacion rostro", "depilar rostro", "depilacion facial", "depilar facial"))) {
			return true;
		}
		if (normalizedServiceName.contains("depilacion piernas")
				&& containsAny(normalizedMessage, List.of("piernas", "depilacion piernas"))) {
			return true;
		}
		if (normalizedServiceName.contains("depilacion bikini")
				&& containsAny(normalizedMessage, List.of("bikini", "depilacion bikini"))) {
			return true;
		}
		return false;
	}

	private Optional<AestheticProductResponse> findMatchingProduct(String message,
			List<AestheticProductResponse> products) {
		String normalizedMessage = normalizeForMatching(message);
		return products.stream()
				.filter(product -> normalizedMessage.contains(normalizeForMatching(product.name()))
						|| containsTokens(normalizedMessage, normalizeForMatching(product.name()))
						|| productAliasMatches(normalizedMessage, product))
				.findFirst();
	}

	private boolean productAliasMatches(String normalizedMessage, AestheticProductResponse product) {
		String categoryCode = product.categoryCode();
		return switch (categoryCode) {
			case "PROTECTORES_SOLARES" ->
				containsAny(normalizedMessage, List.of("bloqueador", "solar", "fps", "protector"));
			case "CREMAS_FACIALES" -> containsAny(normalizedMessage, List.of("crema", "hidratante", "piel sensible"));
			case "SERUMS" -> containsAny(normalizedMessage, List.of("serum", "vitamina c", "niacinamida", "retinol"));
			case "EXFOLIANTES" -> containsAny(normalizedMessage, List.of("exfoliante", "exfoliacion"));
			case "MASCARILLAS" -> containsAny(normalizedMessage, List.of("mascarilla"));
			case "CAPILARES" ->
				containsAny(normalizedMessage, List.of("shampoo", "acondicionador", "capilar", "puntas"));
			case "ACEITES_CORPORALES" -> containsAny(normalizedMessage, List.of("aceite", "corporal"));
			case "ESMALTES" -> containsAny(normalizedMessage, List.of("esmalte", "top coat", "cuticulas", "removedor"));
			case "KITS_CUIDADO_FACIAL" -> containsAny(normalizedMessage, List.of("kit", "rutina facial"));
			case "POST_TRATAMIENTO" ->
				containsAny(normalizedMessage, List.of("post", "despues", "laser", "cera", "reparadora"));
			case "GIFT_CARDS" -> containsAny(normalizedMessage, List.of("gift", "regalo", "tarjeta"));
			case "PACKS_PROMOCIONALES" -> containsAny(normalizedMessage, List.of("pack", "promo", "promocion"));
			default -> false;
		};
	}

	private boolean containsTokens(String normalizedMessage, String normalizedName) {
		String[] tokens = normalizedName.split("\\s+");
		int matches = 0;
		for (String token : tokens) {
			if (token.length() > 3 && normalizedMessage.contains(token)) {
				matches++;
			}
		}
		return matches >= Math.min(2, tokens.length);
	}

	private boolean containsAny(String normalizedText, List<String> keywords) {
		return keywords.stream().map(this::normalizeForMatching).anyMatch(normalizedText::contains);
	}

	private BigDecimal confidenceFor(String intent, String serviceName, String productName, boolean hasRisk) {
		if (hasRisk) {
			return BigDecimal.valueOf(0.95);
		}
		if ("intencion_no_clara".equals(intent)) {
			return BigDecimal.valueOf(0.45);
		}
		if (serviceName != null || productName != null) {
			return BigDecimal.valueOf(0.88);
		}
		return BigDecimal.valueOf(0.72);
	}

	private BigDecimal clampConfidence(BigDecimal confidence) {
		if (confidence == null) {
			return BigDecimal.valueOf(0.5);
		}
		if (confidence.compareTo(BigDecimal.ZERO) < 0) {
			return BigDecimal.ZERO;
		}
		if (confidence.compareTo(BigDecimal.ONE) > 0) {
			return BigDecimal.ONE;
		}
		return confidence;
	}

	private UpsertAestheticServiceRequest normalizeServiceRequest(UpsertAestheticServiceRequest request) {
		String professionalRequired = request.professionalRequired();
		if (professionalRequired == null || professionalRequired.isBlank()) {
			professionalRequired = "Profesional del centro";
		}
		return new UpsertAestheticServiceRequest(normalizeCode(request.code(), request.name()),
				upper(request.categoryCode()), trimRequired(request.name()), trimRequired(request.description()),
				request.durationMinutes(), request.priceBase(), trimRequired(professionalRequired),
				normalizeOptional(request.supplies()), normalizeOptional(request.contraindications()),
				normalizeOptional(request.availabilityRules()), normalizeOptional(request.bookingRules()),
				normalizeOptional(request.cancellationRules()), normalizeOptional(request.aftercareRecommendations()),
				Boolean.TRUE.equals(request.requiresPriorEvaluation()),
				Boolean.TRUE.equals(request.requiresInformedConsent()), request.active() == null || request.active(),
				request.professionalIds(), request.roomIds());
	}

	private UpsertAestheticProductRequest normalizeProductRequest(UpsertAestheticProductRequest request) {
		return new UpsertAestheticProductRequest(normalizeCode(request.code(), request.name()),
				upper(request.categoryCode()), trimRequired(request.name()), trimRequired(request.description()),
				request.price(), request.stock() == null ? 0 : request.stock(),
				request.stockMinimum() == null ? 0 : request.stockMinimum(), normalizeOptional(request.supplier()),
				request.expirationDate(), normalizeOptional(request.compatibleServices()),
				normalizeOptional(request.recommendationRules()), normalizeOptional(request.crossSellRules()),
				normalizeOptional(request.usageRestrictions()), request.active() == null || request.active());
	}

	private UpsertAestheticBusinessRuleRequest normalizeRuleRequest(UpsertAestheticBusinessRuleRequest request) {
		return new UpsertAestheticBusinessRuleRequest(normalizeCode(request.code(), request.name()),
				trimRequired(request.name()), upper(request.ruleType()), trimRequired(request.description()),
				request.priority() == null ? 100 : request.priority(), request.active() == null || request.active(),
				normalizeJsonPayload(request.rulePayload()));
	}

	private String normalizeJsonPayload(String payload) {
		String value = normalizeOptional(payload);
		return value == null ? "{}" : value;
	}

	private String trimRequired(String value) {
		return value == null ? "" : value.trim();
	}

	private String upper(String value) {
		return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeCode(String providedCode, String name) {
		String source = normalizeOptional(providedCode);
		if (source == null) {
			source = name == null ? UUID.randomUUID().toString() : name;
		}
		String normalized = normalizeForMatching(source).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "")
				.toUpperCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		}
		return normalized.length() > 70 ? normalized.substring(0, 70) : normalized;
	}

	private int normalizePage(int page) {
		return Math.max(page, 0);
	}

	private int normalizeSize(int size) {
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String normalizeForMatching(String value) {
		if (value == null) {
			return "";
		}
		String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		return normalized.replaceAll("\\p{M}", "").replace('ñ', 'n');
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return second;
	}

	private record BusinessSnapshot(UUID businessId, List<AestheticServiceResponse> services,
			List<AestheticProductResponse> products, List<AestheticBusinessRuleResponse> rules,
			List<AestheticPromotionSummary> promotions, AestheticServiceResponse matchedService,
			AestheticProductResponse matchedProduct) {

		private String toPromptContext() {
			StringBuilder builder = new StringBuilder("Catalogo interno resumido:\nServicios activos:\n");
			services.stream().limit(120)
					.forEach(service -> builder.append("- ").append(service.name()).append(" | categoria=")
							.append(service.categoryName()).append(" | precioBase=").append(service.priceBase())
							.append(" | duracionMin=").append(service.durationMinutes()).append(" | profesional=")
							.append(service.professionalRequired()).append(" | requiereEvaluacion=")
							.append(service.requiresPriorEvaluation()).append(" | consentimiento=")
							.append(service.requiresInformedConsent()).append(" | contraindicaciones=")
							.append(service.contraindications()).append(" | cuidados=")
							.append(service.aftercareRecommendations()).append("\n"));
			builder.append("Productos activos:\n");
			products.stream().limit(120).forEach(product -> builder.append("- ").append(product.name())
					.append(" | categoria=").append(product.categoryName()).append(" | precio=").append(product.price())
					.append(" | stock=").append(product.stock()).append(" | compatible=")
					.append(product.compatibleServices()).append(" | reglas=").append(product.recommendationRules())
					.append(" | restricciones=").append(product.usageRestrictions()).append("\n"));
			builder.append("Reglas activas:\n");
			rules.stream().limit(80)
					.forEach(rule -> builder.append("- ").append(rule.code()).append(" | tipo=").append(rule.ruleType())
							.append(" | prioridad=").append(rule.priority()).append(" | descripcion=")
							.append(rule.description()).append("\n"));
			builder.append("Promociones activas:\n");
			promotions.stream().limit(30)
					.forEach(promotion -> builder.append("- ").append(promotion.name()).append(" | descuento=")
							.append(promotion.discountLabel()).append(" | condiciones=").append(promotion.conditions())
							.append("\n"));
			return builder.toString();
		}
	}

}
