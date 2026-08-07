package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.application.AiKnowledgeRepository.IntentExpression;
import com.asistentewhatsapp.aiagents.catalog.LanguageNormalizer;
import com.asistentewhatsapp.aiagents.catalog.MasterConversationCatalog;
import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntentDetectorService {

	private final ConversationSpecCatalog conversationSpecCatalog;
	private final IntentExpressionService intentExpressionService;
	private final MasterConversationCatalog masterCatalog;
	private final LanguageNormalizer languageNormalizer;

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
	private static final Pattern STANDALONE_ORA = Pattern.compile("(?<![a-z])ora(?![a-z])");

	private final List<String> humanWords;
	private final List<String> complaintWords;
	private final List<String> paymentWords;
	private final List<String> paymentProblemWords;
	private final List<String> bookingWords;
	private final List<String> availabilityWords;
	private final List<String> recommendationWords;
	private final List<String> serviceInformationWords;
	private final List<String> professionalWords;
	private final List<String> businessHoursWords;
	private final List<String> thanksWords;
	private final List<String> bookingStatusWords;
	private final List<String> changeBookingWords;
	private final List<String> cancelBookingWords;
	private final List<String> priceWords;
	private final List<String> quoteWords;
	private final List<String> salesWords;
	private final List<String> supportWords;
	private final List<String> knowledgeWords;
	private final List<String> followUpWords;
	private final List<String> socialGreetingWords;
	private final List<String> technicalCommandWords;
	private final List<String> sensitiveWords;
	private final List<String> linkResendWords;
	private final List<String> linkExpiredWords;
	private final List<String> locationWords;
	private final List<String> waitlistWords;
	private final List<String> helpWords;
	private final List<String> negatedAgendaActionWords;
	private final List<String> infoOnlyMarkersWords;
	private final Set<String> greetingWords;

	public IntentDetectorService() {
		this(new ConversationSpecCatalog(), null);
	}

	public IntentDetectorService(ConversationSpecCatalog conversationSpecCatalog) {
		this(conversationSpecCatalog, null);
	}

	@Autowired
	public IntentDetectorService(ConversationSpecCatalog conversationSpecCatalog,
			IntentExpressionService intentExpressionService) {
		this(conversationSpecCatalog, intentExpressionService, MasterConversationCatalog.shared());
	}

	public IntentDetectorService(ConversationSpecCatalog conversationSpecCatalog,
			IntentExpressionService intentExpressionService, MasterConversationCatalog masterCatalog) {
		this.conversationSpecCatalog = conversationSpecCatalog == null
				? new ConversationSpecCatalog()
				: conversationSpecCatalog;
		this.intentExpressionService = intentExpressionService;
		this.masterCatalog = masterCatalog == null ? MasterConversationCatalog.shared() : masterCatalog;
		this.languageNormalizer = LanguageNormalizer.shared();

		this.humanWords = group("HUMAN_WORDS");
		this.complaintWords = group("COMPLAINT_WORDS");
		this.paymentWords = group("PAYMENT_WORDS");
		this.paymentProblemWords = group("PAYMENT_PROBLEM_WORDS");
		this.bookingWords = group("BOOKING_WORDS");
		this.availabilityWords = group("AVAILABILITY_WORDS");
		this.recommendationWords = group("RECOMMENDATION_WORDS");
		this.serviceInformationWords = group("SERVICE_INFORMATION_WORDS");
		this.professionalWords = group("PROFESSIONAL_WORDS");
		this.businessHoursWords = group("BUSINESS_HOURS_WORDS");
		this.thanksWords = group("THANKS_WORDS");
		this.bookingStatusWords = group("BOOKING_STATUS_WORDS");
		this.changeBookingWords = group("CHANGE_BOOKING_WORDS");
		this.cancelBookingWords = group("CANCEL_BOOKING_WORDS");
		this.priceWords = group("PRICE_WORDS");
		this.quoteWords = group("QUOTE_WORDS");
		this.salesWords = group("SALES_WORDS");
		this.supportWords = group("SUPPORT_WORDS");
		this.knowledgeWords = group("KNOWLEDGE_WORDS");
		this.followUpWords = group("FOLLOW_UP_WORDS");
		this.socialGreetingWords = group("SOCIAL_GREETING_WORDS");
		this.technicalCommandWords = group("TECHNICAL_COMMAND_WORDS");
		this.sensitiveWords = group("SENSITIVE_WORDS");
		this.linkResendWords = group("LINK_RESEND_WORDS");
		this.linkExpiredWords = group("LINK_EXPIRED_WORDS");
		this.locationWords = group("LOCATION_WORDS");
		this.waitlistWords = group("WAITLIST_WORDS");
		this.helpWords = group("HELP_WORDS");
		this.negatedAgendaActionWords = group("NEGATED_AGENDA_ACTION_WORDS");
		this.infoOnlyMarkersWords = group("INFO_ONLY_MARKERS_WORDS");
		this.greetingWords = Set.copyOf(group("GREETING_WORDS"));
	}

	private List<String> group(String name) {
		return List.copyOf(masterCatalog.synonymGroup(name));
	}

	public IntentDetectionResult detect(AgentConversationRequest request) {
		String traceId = AiTraceLogger.traceId(request);
		String text = normalize(request.messageBody());
		String rawText = normalizeRaw(request.messageBody());
		AiTraceLogger.info("MESSAGE_NORMALIZED", traceId, request.conversationId(), null, "IntentDetectorService",
				LogSanitizer.messageSummary("message", request.messageBody()) + " normalizedLength=" + text.length());
		boolean isQuestionText = isQuestion(text);
		AiTraceLogger.info("INTENT_CANDIDATES", traceId, request.conversationId(), null, "IntentDetectorService",
				"human=" + containsAny(text, humanWords) + " sensitive=" + containsAny(text, sensitiveWords)
						+ " cancel=" + containsAny(text, cancelBookingWords) + " change="
						+ containsAny(text, changeBookingWords) + " booking=" + containsAny(text, bookingWords)
						+ " sales=" + containsAny(text, salesWords) + " payment=" + containsAny(text, paymentWords)
						+ " location=" + containsAny(text, locationWords) + " question=" + isQuestionText);

		if (text.isBlank() || text.equals("mensaje recibido sin texto")) {
			return new IntentDetectionResult(AgentIntent.AMBIGUOUS, null, 0.1, "bajo", false, null);
		}

		if (containsAny(text, technicalCommandWords)) {
			return new IntentDetectionResult(AgentIntent.TECHNICAL_MESSAGE, null, 0.91, "bajo", false, null);
		}

		if (isNameIntroduction(text)) {
			return new IntentDetectionResult(AgentIntent.GREETING, null, 0.76, "bajo", false, "cliente entrega nombre");
		}

		if (containsAny(text, sensitiveWords)) {
			return new IntentDetectionResult(AgentIntent.COMPLAINT, null, 0.96, "alto", true,
					"caso sensible o reacción post tratamiento");
		}

		if (containsHumanRequest(text)) {
			return new IntentDetectionResult(AgentIntent.HUMAN_REQUEST, null, 0.96, "alto", true,
					"cliente solicita atencion humana");
		}

		if (containsAny(text, knowledgeWords)) {
			return new IntentDetectionResult(AgentIntent.KNOWLEDGE_QUERY, null, 0.82, "bajo", false, null);
		}

		if (containsAny(text, followUpWords)) {
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

		if (!isInfoQueryNotAction(text) && containsAny(text, cancelBookingWords)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_CANCEL, null, 0.9, "medio", false, null);
		}

		if (!isInfoQueryNotAction(text) && containsAny(text, changeBookingWords)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_CHANGE, null, 0.9, "medio", false, null);
		}

		if (containsAny(text, linkResendWords) || containsAny(text, linkExpiredWords)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_STATUS, null, 0.9, "medio", false, null);
		}

		if (containsAny(text, socialGreetingWords)) {
			return new IntentDetectionResult(AgentIntent.GREETING, null, 0.78, "bajo", false, null);
		}

		boolean hasBooking = containsExplicitBookingRequest(text);

		boolean hasPayment = containsAny(text, paymentWords);
		boolean hasPaymentProblem = containsAny(text, paymentProblemWords);
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

		if (containsAny(text, complaintWords)) {
			return new IntentDetectionResult(AgentIntent.COMPLAINT, null, 0.94, "alto", true,
					"reclamo, molestia o urgencia");
		}
		boolean hasBookingStatus = containsAny(text, bookingStatusWords);
		boolean hasPrice = containsAny(text, priceWords);
		boolean hasQuote = containsAny(text, quoteWords);
		boolean hasSales = containsAny(text, salesWords) || hasPrice || hasQuote;
		boolean hasExplicitCommercialQuestion = hasPrice || hasQuote;
		boolean hasSchedulingDate = EXPLICIT_DATE_PATTERN.matcher(text).find();
		boolean hasSchedulingTime = hasExplicitTime(text);
		boolean hasSchedulingLocation = containsAny(text, locationWords) || text.contains(" providencia")
				|| text.contains(" las condes") || text.contains(" en providencia") || text.contains(" en las condes");
		boolean hasSchedulingData = hasSchedulingTime || (hasSchedulingDate && hasSchedulingLocation);
		boolean hasAvailabilityQuestion = containsAny(text, availabilityWords);
		boolean hasRecommendation = containsAny(text, recommendationWords);
		boolean hasServiceInformation = containsAny(text, serviceInformationWords);
		boolean hasProfessional = containsAny(text, professionalWords);
		boolean hasLocationQuery = containsAny(text, locationWords);
		boolean hasBusinessHoursQuery = containsAny(text, businessHoursWords);

		if (hasHelpQuery(text)) {
			return new IntentDetectionResult(AgentIntent.COMMERCIAL_INQUIRY, null, 0.86, "bajo", false, null);
		}

		if (containsAny(text, waitlistWords)) {
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

		if (containsAny(text, quoteWords)) {
			return new IntentDetectionResult(AgentIntent.QUOTE_REQUEST, null, 0.88, "bajo", false, null);
		}

		if (containsAny(text, priceWords)) {
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

		if (!isInfoQueryNotAction(text) && hasBooking && containsAny(text, cancelBookingWords)) {
			return new IntentDetectionResult(AgentIntent.BOOKING_CANCEL, null, 0.9, "medio", false, null);
		}

		if (!isInfoQueryNotAction(text) && hasBooking && containsAny(text, changeBookingWords)) {
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

		if (containsAny(text, knowledgeWords)) {
			return new IntentDetectionResult(AgentIntent.KNOWLEDGE_QUERY, null, 0.82, "bajo", false, null);
		}

		if (containsAny(text, followUpWords)) {
			return new IntentDetectionResult(AgentIntent.FOLLOW_UP, null, 0.8, "bajo", false, null);
		}

		if (containsAny(text, locationWords)) {
			return new IntentDetectionResult(AgentIntent.LOCATION_QUERY, null, 0.82, "medio", false, null);
		}

		if (containsAny(text, supportWords)) {
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
		if (containsAny(text, bookingWords)) {
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
			Optional<AgentIntent> mapped = masterCatalog.mapCatalogCodeToAgentIntent(expression.code());
			if (mapped.isEmpty()) {
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
			AgentIntent intent = mapped.get();
			AiTraceLogger.info("INTENT_DB_CATALOG", traceId, request.conversationId(), null, "IntentDetectorService",
					"intent=" + intent + " expressionType=" + expression.expressionType() + " confidence=" + confidence
							+ " source=DATABASE");
			return Optional.of(new IntentDetectionResult(intent, null, confidence, "bajo", expression.requiresHuman(),
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

	private String normalizeRaw(String value) {
		return TextNormalizer.normalize(value);
	}

	private Optional<IntentDetectionResult> detectNegatedAgendaActionInformation(String text) {
		boolean negatedAgendaAction = containsAny(text, negatedAgendaActionWords);
		if (!negatedAgendaAction) {
			return Optional.empty();
		}
		if (containsAny(text, priceWords)) {
			return Optional.of(new IntentDetectionResult(AgentIntent.PRICE_REQUEST, null, 0.9, "bajo", false,
					"negacion explicita inhibe accion de agenda"));
		}
		if (containsAny(text, serviceInformationWords)) {
			return Optional.of(new IntentDetectionResult(AgentIntent.SERVICE_INFORMATION, null, 0.86, "bajo", false,
					"negacion explicita inhibe accion de agenda"));
		}
		boolean asksInformation = containsAny(text, infoOnlyMarkersWords);
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
		return containsAny(trimmed, thanksWords);
	}

	private boolean isNameIntroduction(String text) {
		return Pattern.compile("^(?:soy|me llamo|mi nombre es)\s+[a-z][a-z ]{1,60}$").matcher(text.trim()).matches();
	}

	private boolean isGreeting(String text) {
		String trimmed = text.trim();
		if (greetingWords.contains(trimmed)) {
			return true;
		}
		return trimmed.startsWith("hola ");
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
		return containsAny(text, helpWords);
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
		for (String candidate : humanWords) {
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
		String t = languageNormalizer.normalizeWithTypoFix(value);
		return STANDALONE_ORA.matcher(t).replaceAll("hora");
	}
}