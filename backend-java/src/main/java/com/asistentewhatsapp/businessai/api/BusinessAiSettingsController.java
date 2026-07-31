package com.asistentewhatsapp.businessai.api;

import com.asistentewhatsapp.businessai.application.BusinessAiSettingsService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class BusinessAiSettingsController {

	private final BusinessAiSettingsService service;

	public BusinessAiSettingsController(BusinessAiSettingsService service) {
		this.service = service;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BUSINESS_AI_VIEW')")
	@GetMapping({"/api/business-ai/settings", "/api/v1/business-ai/settings"})
	public BusinessAiSettingsResponse getSettings(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return service.getSettings(authenticatedUser.businessId());
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BUSINESS_AI_MANAGE')")
	@PutMapping({"/api/business-ai/settings", "/api/v1/business-ai/settings"})
	public BusinessAiSettingsResponse saveSettings(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody UpsertBusinessAiSettingsRequest request) {
		return service.saveSettings(authenticatedUser.businessId(), authenticatedUser.userId(), request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BUSINESS_AI_VIEW')")
	@GetMapping({"/api/business-ai/prompts", "/api/v1/business-ai/prompts"})
	public List<PromptTemplateResponse> getPrompts(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return service.getPrompts(authenticatedUser.businessId());
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BUSINESS_AI_MANAGE')")
	@PostMapping({"/api/business-ai/prompts", "/api/v1/business-ai/prompts"})
	public PromptTemplateResponse createPrompt(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody UpsertPromptTemplateRequest request) {
		return service.createPrompt(authenticatedUser.businessId(), request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'BUSINESS_AI_MANAGE')")
	@PostMapping({"/api/business-ai/prompts/activate", "/api/v1/business-ai/prompts/activate"})
	public void activatePrompt(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody ActivatePromptRequest request) {
		service.activatePrompt(authenticatedUser.businessId(), request.promptId());
	}
}
