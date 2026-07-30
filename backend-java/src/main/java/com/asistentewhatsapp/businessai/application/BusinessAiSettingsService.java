package com.asistentewhatsapp.businessai.application;

import com.asistentewhatsapp.businessai.api.BusinessAiSettingsResponse;
import com.asistentewhatsapp.businessai.api.PromptTemplateResponse;
import com.asistentewhatsapp.businessai.api.UpsertBusinessAiSettingsRequest;
import com.asistentewhatsapp.businessai.api.UpsertPromptTemplateRequest;
import com.asistentewhatsapp.businessai.infrastructure.BusinessAiSettingsJdbcRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessAiSettingsService {

	private final BusinessAiSettingsJdbcRepository repository;

	public BusinessAiSettingsService(BusinessAiSettingsJdbcRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public BusinessAiSettingsResponse getSettings(UUID businessId) {
		return repository.findSettings(businessId).orElseGet(() -> createDefaultSettings(businessId, null));
	}

	@Transactional
	public BusinessAiSettingsResponse saveSettings(UUID businessId, UUID userId,
			UpsertBusinessAiSettingsRequest request) {
		return repository.upsertSettings(businessId, userId, request);
	}

	@Transactional(readOnly = true)
	public List<PromptTemplateResponse> getPrompts(UUID businessId) {
		return repository.findPrompts(businessId);
	}

	@Transactional
	public PromptTemplateResponse createPrompt(UUID businessId, UpsertPromptTemplateRequest request) {
		return repository.insertPrompt(businessId, request);
	}

	@Transactional
	public void activatePrompt(UUID businessId, UUID promptId) {
		repository.activatePrompt(businessId, promptId);
	}

	@Transactional(readOnly = true)
	public Optional<BusinessAiSettingsResponse> findSettingsOpt(UUID businessId) {
		return repository.findSettings(businessId);
	}

	private BusinessAiSettingsResponse createDefaultSettings(UUID businessId, UUID userId) {
		var request = new UpsertBusinessAiSettingsRequest(false, "suggest", "Cercano", "es",
				new java.math.BigDecimal("0.30"), false, true, false, true, List.of(), List.of());
		return repository.upsertSettings(businessId, userId, request);
	}
}
