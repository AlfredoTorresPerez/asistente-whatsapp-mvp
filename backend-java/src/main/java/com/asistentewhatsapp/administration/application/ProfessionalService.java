package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.ProfessionalRequest;
import com.asistentewhatsapp.administration.api.ProfessionalResponse;
import com.asistentewhatsapp.administration.infrastructure.ProfessionalJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalService {

	private final ProfessionalJdbcRepository professionalJdbcRepository;

	public ProfessionalService(ProfessionalJdbcRepository professionalJdbcRepository) {
		this.professionalJdbcRepository = professionalJdbcRepository;
	}

	@Transactional(readOnly = true)
	public PagedResponse<ProfessionalResponse> listProfessionals(AuthenticatedUser authenticatedUser, int page,
			int size, String search, Boolean active) {
		return professionalJdbcRepository.findProfessionals(authenticatedUser.businessId(), Math.max(page, 0),
				Math.min(Math.max(size, 1), 100), normalize(search), active);
	}

	@Transactional(readOnly = true)
	public ProfessionalResponse getProfessional(AuthenticatedUser authenticatedUser, UUID professionalId) {
		return professionalJdbcRepository.findProfessional(authenticatedUser.businessId(), professionalId);
	}

	@Transactional
	public ProfessionalResponse createProfessional(AuthenticatedUser authenticatedUser, ProfessionalRequest request) {
		return professionalJdbcRepository.insertProfessional(authenticatedUser.businessId(), normalizeRequest(request));
	}

	@Transactional
	public ProfessionalResponse updateProfessional(AuthenticatedUser authenticatedUser, UUID professionalId,
			ProfessionalRequest request) {
		return professionalJdbcRepository.updateProfessional(authenticatedUser.businessId(), professionalId,
				normalizeRequest(request));
	}

	private ProfessionalRequest normalizeRequest(ProfessionalRequest request) {
		return request;
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
