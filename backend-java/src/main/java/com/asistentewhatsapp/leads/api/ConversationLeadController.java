package com.asistentewhatsapp.leads.api;

import com.asistentewhatsapp.leads.application.LeadService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ConversationLeadController {

	private final LeadService leadService;

	public ConversationLeadController(LeadService leadService) {
		this.leadService = leadService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'LEAD_MANAGE')")
	@PostMapping(value = {"/api/v1/conversations/{conversationId}/prospects",
			"/api/v1/conversations/{conversationId}/leads"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public LeadDetailResponse createFromConversation(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID conversationId, @Valid @RequestBody CreateLeadFromConversationRequest request) {
		return leadService.createFromConversation(authenticatedUser, conversationId, request);
	}
}
