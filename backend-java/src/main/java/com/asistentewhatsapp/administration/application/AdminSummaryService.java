package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.AdminSummaryResponse;
import com.asistentewhatsapp.administration.infrastructure.AdministrationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSummaryService {

	private final AdministrationJdbcRepository administrationJdbcRepository;

	public AdminSummaryService(AdministrationJdbcRepository administrationJdbcRepository) {
		this.administrationJdbcRepository = administrationJdbcRepository;
	}

	@Transactional(readOnly = true)
	public AdminSummaryResponse getSummary(AuthenticatedUser authenticatedUser) {
		AdminAccessGuard.requireOwnerAdminOrSupervisor(authenticatedUser);
		return administrationJdbcRepository.findSummary(authenticatedUser.businessId());
	}
}
