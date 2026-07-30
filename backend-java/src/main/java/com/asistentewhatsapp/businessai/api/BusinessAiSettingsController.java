package com.asistentewhatsapp.businessai.api;

import com.asistentewhatsapp.businessai.application.BusinessAiSettingsService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
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

	@GetMapping({"/api/business-ai/settings", "/api/v1/business-ai/settings"})
	public BusinessAiSettingsResponse getSettings(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return service.getSettings(authenticatedUser.businessId());
	}

	@PutMapping({"/api/business-ai/settings", "/api/v1/business-ai/settings"})
	public BusinessAiSettingsResponse saveSettings(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody UpsertBusinessAiSettingsRequest request) {
		return service.saveSettings(authenticatedUser.businessId(), authenticatedUser.userId(), request);
	}

	@GetMapping({"/api/business-ai/prompts", "/api/v1/business-ai/prompts"})
	public List<PromptTemplateResponse> getPrompts(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return service.getPrompts(authenticatedUser.businessId());
	}

	@PostMapping({"/api/business-ai/prompts", "/api/v1/business-ai/prompts"})
	public PromptTemplateResponse createPrompt(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody UpsertPromptTemplateRequest request) {
		return service.createPrompt(authenticatedUser.businessId(), request);
	}

	@PostMapping({"/api/business-ai/prompts/activate", "/api/v1/business-ai/prompts/activate"})
	public void activatePrompt(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody ActivatePromptRequest request) {
		service.activatePrompt(authenticatedUser.businessId(), request.promptId());
	}
}
