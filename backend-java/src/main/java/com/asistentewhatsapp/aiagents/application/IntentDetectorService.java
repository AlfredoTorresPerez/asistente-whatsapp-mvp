package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.shared.observability.LogSanitizer;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class IntentDetectorService {

    private static final Pattern EXPLICIT_TIME_PATTERN = Pattern.compile("\\b(?:a\\s+las\\s+)?(?:[01]?\\d|2[0-3])(?::[0-5]\\d)?\\s*(?:hrs?|horas?)?\\b");
    private static final Pattern EXPLICIT_DATE_PATTERN = Pattern.compile("\\b(?:hoy|manana|mañana|pasado\\s+manana|pasado\\s+mañana|lunes|martes|miercoles|miércoles|jueves|viernes|sabado|sábado|domingo|\\d{1,2}\\s+de\\s+[a-záéíóúñ]+)\\b", Pattern.CASE_INSENSITIVE);

    private static final List<String> HUMAN_WORDS = List.of(
            "ejecutivo", "humano", "persona", "asesor", "supervisor", "llamenme", "llamarme", "quiero hablar");
    private static final List<String> COMPLAINT_WORDS = List.of(
            "reclamo", "molesto", "molesta", "pesimo", "horrible", "denuncia", "fraude", "estafa", "urgente",
            "nadie responde", "problema grave", "amenaza", "devolucion", "devolución");
    private static final List<String> PAYMENT_WORDS = List.of(
            "pagar", "pago", "transferencia", "comprobante", "factura", "boleta", "deuda", "cobro", "link de pago");
    private static final List<String> PAYMENT_PROBLEM_WORDS = List.of(
            "pago duplicado", "no aparece", "no se reflejo", "no se reflejó", "monto incorrecto", "me cobraron",
            "reembolso", "devolucion", "devolución");
    private static final List<String> BOOKING_WORDS = List.of(
            "agendar", "reservar", "cita", "hora", "turno", "reunion", "reunión", "visita", "mañana", "manana");
    private static final List<String> BOOKING_STATUS_WORDS = List.of(
            "tengo agendado", "tengo agendada", "tengo reserva", "tengo una reserva", "mi reserva", "mis reservas",
            "revisar agenda", "revisa la agenda", "revisala la agenda", "agenda de junio",
            "agenda de este mes", "estado reserva", "confirmar mi hora", "ver mi cita", "tengo cita", "tengo una cita");
    private static final List<String> CHANGE_BOOKING_WORDS = List.of(
            "reagendar", "reprogramar", "reprogramacion", "reprogramación", "cambiar hora", "cambiar mi hora",
            "cambiar reserva", "cambiar mi reserva", "cambiar cita", "cambiar mi cita", "cambio de hora",
            "modificar cita", "modificar mi cita", "mover", "mover mi hora", "mover mi reserva");
    private static final List<String> CANCEL_BOOKING_WORDS = List.of("cancelar", "anular", "cancelacion", "cancelación");
    private static final List<String> PRICE_WORDS = List.of("precio", "valor", "cuanto cuesta", "cuánto cuesta", "tarifa", "sale");
    private static final List<String> QUOTE_WORDS = List.of("cotizar", "cotizacion", "cotización", "presupuesto");
    private static final List<String> SALES_WORDS = List.of(
            "producto", "servicio", "plan", "promocion", "promoción", "comprar", "contratar", "disponible", "stock",
            "depilacion", "depilación", "axilas", "piernas", "bikini", "bozo", "rostro", "facial",
            "limpieza facial", "laser", "láser", "manicure", "pedicure", "masaje");
    private static final List<String> SUPPORT_WORDS = List.of(
            "ayuda", "soporte", "problema", "error", "falla", "no funciona", "horario", "ubicacion", "ubicación", "direccion", "dirección");
    private static final List<String> KNOWLEDGE_WORDS = List.of(
            "politica", "política", "manual", "documento", "faq", "preguntas frecuentes", "catalogo", "catálogo", "terminos", "términos");
    private static final List<String> FOLLOW_UP_WORDS = List.of(
            "seguimiento", "retomar", "cotizacion pendiente", "cotización pendiente", "recordatorio", "me contactaron");
    private static final List<String> SOCIAL_GREETING_WORDS = List.of(
            "como estas", "como esta", "que tal", "hola como estas", "hola que tal", "buen dia", "buen día");
    private static final List<String> TECHNICAL_COMMAND_WORDS = List.of(
            "docker compose", "docker", "kubectl", "mvn", "maven", "gradle", "npm", "pnpm", "yarn",
            "git ", "curl", "http://", "https://", "localhost", "stacktrace", "exception", "sql ",
            "select ", "insert ", "update ", "delete ", "dockerfile", "compose up", "--build");

    private static final List<String> SENSITIVE_WORDS = List.of(
            "reaccion", "reacción", "ardor", "me ardio", "me ardió", "inflamacion", "inflamación", "alergia", "irritacion", "irritación", "quemadura", "dolor fuerte", "infeccion", "infección");
    private static final List<String> LINK_RESEND_WORDS = List.of(
            "no me llego el link", "no me llegó el link", "no me llego el enlace", "no me llegó el enlace", "reenviar", "reenvia", "reenvía", "mandame el link", "mándame el link", "mandame el enlace", "mándame el enlace");
    private static final List<String> LINK_EXPIRED_WORDS = List.of(
            "enlace expiro", "enlace expiró", "link expiro", "link expiró", "link vencio", "link venció", "no funciona el enlace", "no funciona el link", "me dice expirado");
    private static final List<String> LOCATION_WORDS = List.of(
            "donde queda", "dónde queda", "direccion", "dirección", "ubicacion", "ubicación", "como llego", "cómo llego", "sucursal", "sede");

    public IntentDetectionResult detect(AgentConversationRequest request) {
        String traceId = AiTraceLogger.traceId(request);
        String text = normalize(request.messageBody());
        AiTraceLogger.info("MESSAGE_NORMALIZED", traceId, request.conversationId(), null, "IntentDetectorService",
                LogSanitizer.messageSummary("message", request.messageBody())
                        + " normalizedLength=" + text.length());
        AiTraceLogger.info("INTENT_CANDIDATES", traceId, request.conversationId(), null, "IntentDetectorService",
                "human=" + containsAny(text, HUMAN_WORDS)
                        + " sensitive=" + containsAny(text, SENSITIVE_WORDS)
                        + " cancel=" + containsAny(text, CANCEL_BOOKING_WORDS)
                        + " change=" + containsAny(text, CHANGE_BOOKING_WORDS)
                        + " booking=" + containsAny(text, BOOKING_WORDS)
                        + " sales=" + containsAny(text, SALES_WORDS)
                        + " payment=" + containsAny(text, PAYMENT_WORDS)
                        + " location=" + containsAny(text, LOCATION_WORDS));

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
            return new IntentDetectionResult(AgentIntent.COMPLAINT, null, 0.96, "alto", true, "caso sensible o reacción post tratamiento");
        }

        if (containsHumanRequest(text)) {
            return new IntentDetectionResult(AgentIntent.HUMAN_REQUEST, null, 0.96, "alto", true, "cliente solicita atencion humana");
        }

        if (containsAny(text, KNOWLEDGE_WORDS)) {
            return new IntentDetectionResult(AgentIntent.KNOWLEDGE_QUERY, null, 0.82, "bajo", false, null);
        }

        if (containsAny(text, FOLLOW_UP_WORDS)) {
            return new IntentDetectionResult(AgentIntent.FOLLOW_UP, null, 0.8, "bajo", false, null);
        }

        if (containsAny(text, CANCEL_BOOKING_WORDS)) {
            return new IntentDetectionResult(AgentIntent.BOOKING_CANCEL, null, 0.9, "medio", false, null);
        }

        if (containsAny(text, CHANGE_BOOKING_WORDS)) {
            return new IntentDetectionResult(AgentIntent.BOOKING_CHANGE, null, 0.9, "medio", false, null);
        }

        if (containsAny(text, LINK_RESEND_WORDS) || containsAny(text, LINK_EXPIRED_WORDS)) {
            return new IntentDetectionResult(AgentIntent.BOOKING_STATUS, null, 0.9, "medio", false, null);
        }

        if (containsAny(text, SOCIAL_GREETING_WORDS)) {
            return new IntentDetectionResult(AgentIntent.GREETING, null, 0.78, "bajo", false, null);
        }

        boolean hasPayment = containsAny(text, PAYMENT_WORDS);
        boolean hasPaymentProblem = containsAny(text, PAYMENT_PROBLEM_WORDS);
        if (hasPaymentProblem) {
            return new IntentDetectionResult(AgentIntent.PAYMENT_PROBLEM, null, 0.92, "alto", true, "problema de pago requiere revision humana");
        }
        if (hasPayment) {
            return new IntentDetectionResult(AgentIntent.PAYMENT_INQUIRY, null, 0.88, "medio", false, null);
        }

        if (containsAny(text, COMPLAINT_WORDS)) {
            return new IntentDetectionResult(AgentIntent.COMPLAINT, null, 0.94, "alto", true, "reclamo, molestia o urgencia");
        }

        boolean hasBooking = containsAny(text, BOOKING_WORDS);
        boolean hasBookingStatus = containsAny(text, BOOKING_STATUS_WORDS);
        boolean hasPrice = containsAny(text, PRICE_WORDS);
        boolean hasQuote = containsAny(text, QUOTE_WORDS);
        boolean hasSales = containsAny(text, SALES_WORDS) || hasPrice || hasQuote;
        boolean hasExplicitCommercialQuestion = hasPrice || hasQuote;
        boolean hasSchedulingDate = EXPLICIT_DATE_PATTERN.matcher(text).find();
        boolean hasSchedulingTime = hasExplicitTime(text);
        boolean hasSchedulingLocation = containsAny(text, LOCATION_WORDS) || text.contains(" providencia") || text.contains(" las condes") || text.contains(" en providencia") || text.contains(" en las condes");
        boolean hasSchedulingData = hasSchedulingTime || (hasSchedulingDate && hasSchedulingLocation);

        if (hasBookingStatus) {
            return new IntentDetectionResult(AgentIntent.BOOKING_STATUS, null, 0.9, "medio", false, null);
        }

        if (hasBooking && hasExplicitCommercialQuestion) {
            return new IntentDetectionResult(AgentIntent.COMMERCIAL_AND_BOOKING, AgentIntent.BOOKING_REQUEST, 0.9, "bajo", false, null);
        }

        if (!hasBooking && hasSales && hasSchedulingData) {
            return new IntentDetectionResult(AgentIntent.COMMERCIAL_AND_BOOKING, AgentIntent.BOOKING_REQUEST, 0.9, "bajo", false, null);
        }

        if (hasBooking && hasSales) {
            return new IntentDetectionResult(AgentIntent.COMMERCIAL_AND_BOOKING, AgentIntent.BOOKING_REQUEST, 0.9, "bajo", false, null);
        }

        if (hasBooking && containsAny(text, CANCEL_BOOKING_WORDS)) {
            return new IntentDetectionResult(AgentIntent.BOOKING_CANCEL, null, 0.9, "medio", false, null);
        }

        if (hasBooking && containsAny(text, CHANGE_BOOKING_WORDS)) {
            return new IntentDetectionResult(AgentIntent.BOOKING_CHANGE, null, 0.9, "medio", false, null);
        }

        if (hasBooking) {
            return new IntentDetectionResult(AgentIntent.BOOKING_REQUEST, null, 0.86, "bajo", false, null);
        }

        if (containsAny(text, QUOTE_WORDS)) {
            return new IntentDetectionResult(AgentIntent.QUOTE_REQUEST, null, 0.88, "bajo", false, null);
        }

        if (containsAny(text, PRICE_WORDS)) {
            return new IntentDetectionResult(AgentIntent.PRICE_REQUEST, null, 0.88, "bajo", false, null);
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
            return new IntentDetectionResult(AgentIntent.SUPPORT_GENERAL, null, 0.82, "medio", false, null);
        }

        if (containsAny(text, SUPPORT_WORDS)) {
            return new IntentDetectionResult(AgentIntent.SUPPORT_GENERAL, null, 0.78, "medio", false, null);
        }

        if (isGreeting(text)) {
            return new IntentDetectionResult(AgentIntent.GREETING, null, 0.74, "bajo", false, null);
        }

        return new IntentDetectionResult(AgentIntent.AMBIGUOUS, null, 0.58, "bajo", false, null);
    }

    private boolean isNameIntroduction(String text) {
        return Pattern.compile("^(?:soy|me llamo|mi nombre es)\s+[a-z][a-z ]{1,60}$").matcher(text.trim()).matches();
    }

    private boolean isGreeting(String text) {
        String trimmed = text.trim();
        return trimmed.equals("hola")
                || trimmed.equals("buenas")
                || trimmed.equals("buenos dias")
                || trimmed.equals("buenos días")
                || trimmed.equals("buenas tardes")
                || trimmed.equals("buenas noches")
                || trimmed.startsWith("hola ");
    }

    private boolean hasExplicitTime(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.contains(":") || text.contains(" a las ") || text.contains(" horas") || text.contains(" hrs")) {
            return EXPLICIT_TIME_PATTERN.matcher(text).find();
        }
        return false;
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
        return TextNormalizer.normalize(value);
    }
}
