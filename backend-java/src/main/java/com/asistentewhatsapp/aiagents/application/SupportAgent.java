package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import com.asistentewhatsapp.aiagents.domain.AgentType;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository;
import com.asistentewhatsapp.locations.infrastructure.BusinessLocationJdbcRepository.BusinessLocationRecord;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SupportAgent extends AbstractAgentHandler {

	private final BusinessLocationJdbcRepository businessLocationJdbcRepository;

	public SupportAgent(BusinessLocationJdbcRepository businessLocationJdbcRepository) {
		this.businessLocationJdbcRepository = businessLocationJdbcRepository;
	}

	@Override
	public AgentType type() {
		return AgentType.SUPPORT;
	}

	@Override
	public AgentRoutingResult handle(AgentConversationRequest request, IntentDetectionResult intent,
			Map<String, String> entities, List<String> missingData) {
		if (intent.primaryIntent() == AgentIntent.TECHNICAL_MESSAGE) {
			return result(request, intent, type(), entities, missing("solicitud_valida_centro_estetico"),
					"Ese mensaje parece técnico y no corresponde a una solicitud de atención del centro estético. ¿Quieres que te ayude con servicios, precios o agenda?",
					false, null);
		}

		if (intent.primaryIntent() == AgentIntent.BUSINESS_HOURS_QUERY) {
			return result(request, intent, type(), entities, missing("sucursal_si_necesita_horario_especifico"),
					"Puedo ayudarte con horarios de atención. Para no confundirlo con disponibilidad de citas, indícame la sucursal que quieres revisar.",
					false, null);
		}

		String normalizedMessage = TextNormalizer.normalize(request.messageBody());
		if (containsAny(normalizedMessage, "donde queda", "direccion", "ubicacion", "como llego", "sucursal", "sede")) {
			LocationAnswer locationAnswer = locationResponse(request, normalizedMessage);
			return result(request, intent, type(), entities,
					locationAnswer.locationWasIdentified() ? List.of() : missing("sucursal_si_no_fue_indicada"),
					locationAnswer.response(), false, null);
		}

		String response = has(entities, "numero_solicitud")
				? "Gracias. Revisaré la información disponible y, si requiere validación interna, lo derivaré al equipo."
				: "Puedo ayudarte con eso. ¿Me indicas el número de solicitud o el correo asociado?";
		return result(request, intent, type(), entities, missing("numero_solicitud_o_correo"), response, false, null);
	}

	private LocationAnswer locationResponse(AgentConversationRequest request, String normalizedMessage) {
		List<BusinessLocationRecord> locations = businessLocationJdbcRepository.findActive(request.businessId());
		Optional<BusinessLocationRecord> mentioned = findMentionedLocation(locations, normalizedMessage);
		if (mentioned.isPresent()) {
			BusinessLocationRecord location = mentioned.get();
			if (location.address() == null || location.address().isBlank()) {
				return new LocationAnswer("Tengo registrada la sucursal " + location.name()
						+ ", pero falta configurar su dirección o enlace de mapa. Te derivaré con una persona del equipo para confirmarlo.",
						true);
			}
			return new LocationAnswer("La sucursal " + location.name() + " está ubicada en:\n" + location.address(),
					true);
		}
		if (locations.isEmpty()) {
			return new LocationAnswer(
					"No encontré sucursales activas configuradas. Te derivaré con una persona del equipo para confirmar la ubicación.",
					false);
		}
		return new LocationAnswer("Tenemos estas sucursales activas: " + locations.stream()
				.map(BusinessLocationRecord::name).reduce((l, r) -> l + ", " + r).orElse("sede principal")
				+ ". ¿Sobre cuál necesitas la dirección?", false);
	}

	private record LocationAnswer(String response, boolean locationWasIdentified) {
	}

	private Optional<BusinessLocationRecord> findMentionedLocation(List<BusinessLocationRecord> locations,
			String normalizedMessage) {
		Optional<BusinessLocationRecord> byNameOrCode = locations.stream()
				.filter(location -> containsNormalized(normalizedMessage, location.name())
						|| containsNormalized(normalizedMessage, location.code()))
				.findFirst();
		if (byNameOrCode.isPresent()) {
			return byNameOrCode;
		}
		return locations.stream().filter(location -> containsNormalized(normalizedMessage, location.commune())
				|| containsNormalized(normalizedMessage, location.city())).findFirst();
	}

	private boolean containsNormalized(String normalized, String value) {
		String normalizedValue = TextNormalizer.normalize(value);
		return !normalizedValue.isBlank() && normalized.contains(normalizedValue);
	}

	private boolean containsAny(String normalized, String... values) {
		if (normalized == null || normalized.isBlank()) {
			return false;
		}
		for (String value : values) {
			if (normalized.contains(TextNormalizer.normalize(value))) {
				return true;
			}
		}
		return false;
	}
}
