package com.asistentewhatsapp.cloudapi.onboarding;

import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingService.CompleteOnboardingRequest;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingService.OnboardingResult;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingService.OnboardingStatus;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/integrations/whatsapp-cloud", produces = MediaType.APPLICATION_JSON_VALUE)
public class MetaOnboardingController {

	private final MetaOnboardingService onboardingService;

	public MetaOnboardingController(MetaOnboardingService onboardingService) {
		this.onboardingService = onboardingService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/onboarding/complete")
	public ResponseEntity<OnboardingResult> completeOnboarding(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody CompleteOnboardingRequest request) {
		OnboardingResult result = onboardingService.completeOnboarding(authenticatedUser.businessId(), request);
		return ResponseEntity.ok(result);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_VIEW')")
	@GetMapping("/status")
	public ResponseEntity<OnboardingStatus> getStatus(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		OnboardingStatus status = onboardingService.getStatus(authenticatedUser.businessId());
		return ResponseEntity.ok(status);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/revalidate")
	public ResponseEntity<OnboardingResult> revalidate(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		OnboardingResult result = onboardingService.revalidate(authenticatedUser.businessId());
		return ResponseEntity.ok(result);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/disconnect")
	public ResponseEntity<Void> disconnect(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		onboardingService.disconnect(authenticatedUser.businessId());
		return ResponseEntity.ok().build();
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'WHATSAPP_CONFIG_MANAGE')")
	@PostMapping("/disconnect-from-meta")
	public ResponseEntity<Void> disconnectFromMeta(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		onboardingService.disconnectFromMeta(authenticatedUser.businessId());
		return ResponseEntity.ok().build();
	}
}
