package com.asistentewhatsapp.aesthetic.infrastructure.openai;

import com.asistentewhatsapp.aesthetic.api.IntentAnalysisResponse;
import com.asistentewhatsapp.aesthetic.api.IntentEntitiesResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiIntentClient {

	private final RestClient restClient;
	private final OpenAiIntentProperties properties;
	private final ObjectMapper objectMapper;

	public OpenAiIntentClient(OpenAiIntentProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		RestClient.Builder builder = RestClient.builder().baseUrl(properties.resolvedBaseUrl())
				.defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		if (properties.hasApiKey()) {
			builder.defaultHeader("Authorization", "Bearer " + properties.apiKey());
		}
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(properties.resolvedTimeoutSeconds()));
		requestFactory.setReadTimeout(Duration.ofSeconds(properties.resolvedTimeoutSeconds()));
		this.restClient = builder.requestFactory(requestFactory).build();
	}

	public Optional<IntentAnalysisResponse> analyze(String message, String businessSnapshot) {
		if (!properties.enabled() || !properties.hasApiKey()) {
			return Optional.empty();
		}
		try {
			String rawBody = restClient.post().uri("").body(buildPayload(message, businessSnapshot)).retrieve()
					.body(String.class);
			if (rawBody == null || rawBody.isBlank()) {
				return Optional.empty();
			}
			return parseResponse(rawBody);
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public String modelName() {
		return properties.resolvedModel();
	}

	private Map<String, Object> buildPayload(String message, String businessSnapshot) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("additionalProperties", false);
		schema.put("required", List.of("intencion", "confianza", "entidades", "requiereConsultaBaseDatos",
				"requiereDerivacionHumana", "motivoDerivacion", "respuestaSugerida"));
		schema.put("properties", Map.of("intencion", Map.of("type", "string"), "confianza",
				Map.of("type", "number", "minimum", 0, "maximum", 1), "entidades",
				Map.of("type", "object", "additionalProperties", false, "required",
						List.of("servicio", "producto", "fecha", "hora", "profesional", "cliente"), "properties",
						Map.of("servicio", nullableString(), "producto", nullableString(), "fecha", nullableString(),
								"hora", nullableString(), "profesional", nullableString(), "cliente",
								nullableString())),
				"requiereConsultaBaseDatos", Map.of("type", "boolean"), "requiereDerivacionHumana",
				Map.of("type", "boolean"), "motivoDerivacion", nullableString(), "respuestaSugerida",
				Map.of("type", "string")));

		String systemInstruction = """
				Eres un clasificador y redactor controlado para un centro estetico. Devuelve solo JSON valido.
				Usa exclusivamente el catalogo interno entregado en el mensaje de usuario para construir respuestaSugerida.
				Si el catalogo interno trae precio, duracion, stock, cuidados, restricciones, reglas o promociones, puedes usarlos literalmente.
				No inventes precios, horarios, stock, disponibilidad, promociones ni medios de pago.
				Para agenda, reserva, reprogramacion, estado de reserva, historial o pago, marca requiereConsultaBaseDatos=true si falta validar informacion operacional.
				Si hay embarazo, alergias, medicamentos, enfermedades, heridas, infecciones, procedimientos invasivos o ambiguedad de seguridad, marca requiereDerivacionHumana=true.
				No diagnostiques, no prometas resultados y no recomiendes tratamientos contraindicados.
				Si faltan datos, pide una aclaracion concreta y breve.
				Intenciones permitidas: consultar_servicios_disponibles, consultar_precio_servicio, consultar_duracion_servicio,
				pedir_recomendacion_tratamiento, reservar_hora, cancelar_reserva, reprogramar_reserva,
				consultar_productos, recomendar_productos, consultar_promociones, consultar_disponibilidad_fecha,
				consultar_disponibilidad_profesional, consultar_contraindicaciones, solicitar_cuidados_posteriores,
				solicitar_evaluacion_estetica, consultar_estado_reserva, consultar_historial_cliente,
				consultar_medios_pago, derivar_atencion_humana, intencion_no_clara.
				""";

		return Map.of("model", properties.resolvedModel(), "input",
				List.of(Map.of("role", "system", "content", systemInstruction),
						Map.of("role", "user", "content", businessSnapshot + "\n\nMensaje cliente:\n" + message)),
				"text", Map.of("format", Map.of("type", "json_schema", "name", "esthetic_intent_analysis", "schema",
						schema, "strict", true)));
	}

	private Map<String, Object> nullableString() {
		return Map.of("type", List.of("string", "null"));
	}

	private Optional<IntentAnalysisResponse> parseResponse(String rawBody) {
		try {
			JsonNode root = objectMapper.readTree(rawBody);
			Optional<String> outputText = findFirstText(root);
			if (outputText.isEmpty()) {
				return Optional.empty();
			}
			JsonNode parsed = objectMapper.readTree(outputText.get());
			JsonNode entities = parsed.path("entidades");
			IntentEntitiesResponse entityResponse = new IntentEntitiesResponse(nullIfMissing(entities, "servicio"),
					nullIfMissing(entities, "producto"), nullIfMissing(entities, "fecha"),
					nullIfMissing(entities, "hora"), nullIfMissing(entities, "profesional"),
					nullIfMissing(entities, "cliente"));
			return Optional.of(new IntentAnalysisResponse(textValue(parsed, "intencion", "intencion_no_clara"),
					BigDecimal.valueOf(parsed.path("confianza").asDouble(0.5)), entityResponse,
					parsed.path("requiereConsultaBaseDatos").asBoolean(true),
					parsed.path("requiereDerivacionHumana").asBoolean(false), nullIfMissing(parsed, "motivoDerivacion"),
					textValue(parsed, "respuestaSugerida", "Necesito revisar informacion interna antes de responder."),
					properties.resolvedModel()));
		} catch (RuntimeException | java.io.IOException exception) {
			return Optional.empty();
		}
	}

	private Optional<String> findFirstText(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return Optional.empty();
		}
		if (node.has("output_text") && node.path("output_text").isTextual()) {
			return Optional.of(node.path("output_text").asText());
		}
		if (node.has("text") && node.path("text").isTextual()) {
			return Optional.of(node.path("text").asText());
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				Optional<String> found = findFirstText(child);
				if (found.isPresent()) {
					return found;
				}
			}
		} else if (node.isObject()) {
			for (JsonNode child : node) {
				Optional<String> found = findFirstText(child);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}

	private String textValue(JsonNode node, String fieldName, String fallback) {
		String value = nullIfMissing(node, fieldName);
		return value == null || value.isBlank() ? fallback : value;
	}

	private String nullIfMissing(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		String text = value.asText();
		return text == null || text.isBlank() ? null : text;
	}
}
