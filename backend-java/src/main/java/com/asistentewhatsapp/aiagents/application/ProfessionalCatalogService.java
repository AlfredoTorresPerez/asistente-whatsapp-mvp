package com.asistentewhatsapp.aiagents.application;

import com.asistentewhatsapp.agenda.api.AgendaFilterOptionResponse;
import com.asistentewhatsapp.agenda.infrastructure.CompleteAgendaJdbcRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfessionalCatalogService {

	private final CompleteAgendaJdbcRepository completeAgendaJdbcRepository;

	public ProfessionalCatalogService(CompleteAgendaJdbcRepository completeAgendaJdbcRepository) {
		this.completeAgendaJdbcRepository = completeAgendaJdbcRepository;
	}

	public record ProfessionalInfo(UUID id, String name, String specialty, UUID locationId) {
	}

	public List<ProfessionalInfo> findActive(UUID businessId) {
		List<AgendaFilterOptionResponse> options = completeAgendaJdbcRepository
				.findProfessionalFilterOptions(businessId, null);
		if (options == null || options.isEmpty()) {
			return List.of();
		}
		return options.stream().filter(AgendaFilterOptionResponse::active)
				.map(option -> new ProfessionalInfo(option.id(), option.name(), option.detail(), option.locationId()))
				.toList();
	}

	public List<ProfessionalInfo> findByLocation(UUID businessId, UUID locationId) {
		if (locationId == null) {
			return findActive(businessId);
		}
		List<AgendaFilterOptionResponse> options = completeAgendaJdbcRepository
				.findProfessionalFilterOptions(businessId, locationId);
		if (options == null || options.isEmpty()) {
			return List.of();
		}
		return options.stream().filter(AgendaFilterOptionResponse::active)
				.map(option -> new ProfessionalInfo(option.id(), option.name(), option.detail(), option.locationId()))
				.toList();
	}

	public Optional<ProfessionalInfo> findByName(UUID businessId, String nameText) {
		if (nameText == null || nameText.isBlank()) {
			return Optional.empty();
		}
		String normalized = normalize(nameText);
		List<ProfessionalInfo> professionals = findActive(businessId);
		Optional<ProfessionalInfo> exact = professionals.stream()
				.filter(professional -> normalize(professional.name()).equals(normalized)).findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		Optional<ProfessionalInfo> contains = professionals.stream()
				.filter(professional -> normalize(professional.name()).contains(normalized)
						|| normalized.contains(normalize(professional.name())))
				.findFirst();
		if (contains.isPresent()) {
			return contains;
		}
		if (!normalized.contains(" ")) {
			String firstName = normalized;
			return professionals.stream().filter(professional -> firstName(professional.name()).equals(firstName))
					.findFirst();
		}
		return Optional.empty();
	}

	public List<ProfessionalInfo> findBySpecialty(UUID businessId, String specialtyText) {
		if (specialtyText == null || specialtyText.isBlank()) {
			return List.of();
		}
		String normalized = normalize(specialtyText);
		return findActive(businessId).stream()
				.filter(professional -> containsAnyNormalized(normalize(professional.specialty()), normalized))
				.toList();
	}

	public boolean isActiveProfessional(UUID businessId, String nameText) {
		return findByName(businessId, nameText).isPresent();
	}

	private boolean containsAnyNormalized(String specialty, String normalizedText) {
		for (String token : normalizedText.split(" ")) {
			if (token.length() > 2 && specialty.contains(token)) {
				return true;
			}
		}
		return specialty.contains(normalizedText) || normalizedText.contains(specialty);
	}

	private String firstName(String fullName) {
		if (fullName == null || fullName.isBlank()) {
			return "";
		}
		return normalize(fullName).split(" ")[0];
	}

	private String normalize(String value) {
		return TextNormalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT));
	}
}