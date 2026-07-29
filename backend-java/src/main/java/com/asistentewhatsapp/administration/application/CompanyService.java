package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.CompanySettingsRequest;
import com.asistentewhatsapp.administration.api.CompanySettingsResponse;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.domain.BusinessEntity;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.security.infrastructure.BusinessRepository;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

	private final BusinessRepository businessRepository;
	private final AuditLogJdbcRepository auditLogJdbcRepository;

	public CompanyService(BusinessRepository businessRepository, AuditLogJdbcRepository auditLogJdbcRepository) {
		this.businessRepository = businessRepository;
		this.auditLogJdbcRepository = auditLogJdbcRepository;
	}

	@Transactional(readOnly = true)
	public CompanySettingsResponse getCurrent(AuthenticatedUser authenticatedUser) {
		BusinessEntity businessEntity = loadBusiness(authenticatedUser);
		return toResponse(businessEntity);
	}

	@Transactional
	public CompanySettingsResponse updateCurrent(AuthenticatedUser authenticatedUser, CompanySettingsRequest request) {
		BusinessEntity businessEntity = loadBusiness(authenticatedUser);
		businessEntity.setCompanyName(request.companyName().trim());
		businessEntity.setBusinessName(request.businessName().trim());
		businessEntity.setTimezone(request.timezone().trim());
		businessEntity.setCurrency(request.currency().trim().toUpperCase());
		businessEntity.setContactEmail(request.contactEmail().trim().toLowerCase());
		businessEntity.setSupportPhone(blankToNull(request.supportPhone()));
		businessEntity.setAddress(blankToNull(request.address()));
		businessRepository.save(businessEntity);

		auditLogJdbcRepository
				.insert(authenticatedUser.businessId(), authenticatedUser.userId(), "COMPANY_UPDATED", "BUSINESS",
						businessEntity.getId(), "Configuracion de empresa actualizada.",
						Map.of("companyName", businessEntity.getCompanyName(), "businessName",
								businessEntity.getBusinessName(), "contactEmail", businessEntity.getContactEmail()),
						OffsetDateTime.now(ZoneOffset.UTC));

		return toResponse(businessEntity);
	}

	private BusinessEntity loadBusiness(AuthenticatedUser authenticatedUser) {
		return businessRepository.findById(authenticatedUser.businessId())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontro la empresa actual."));
	}

	private CompanySettingsResponse toResponse(BusinessEntity businessEntity) {
		return new CompanySettingsResponse(businessEntity.getId(), businessEntity.getCompanyName(),
				businessEntity.getBusinessName(), businessEntity.getTimezone(), businessEntity.getCurrency(),
				businessEntity.getContactEmail(), businessEntity.getSupportPhone(), businessEntity.getAddress());
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
