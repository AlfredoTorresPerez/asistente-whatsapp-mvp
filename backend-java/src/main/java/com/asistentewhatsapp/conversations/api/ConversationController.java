package com.asistentewhatsapp.conversations.api;

import com.asistentewhatsapp.conversations.application.ConversationService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_VIEW')")
    @GetMapping("/api/v1/conversations/metrics")
    public ConversationMetricsResponse metrics(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return conversationService.getMetrics(authenticatedUser);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_VIEW')")
    @GetMapping("/api/v1/conversations")
    public PagedResponse<ConversationSummaryResponse> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID ownerUserId) {
        return conversationService.list(authenticatedUser, page, size, search, status, ownerUserId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_VIEW')")
    @GetMapping("/api/v1/conversations/{conversationId}")
    public ConversationDetailResponse detail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId) {
        return conversationService.getDetail(authenticatedUser, conversationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_REPLY')")
    @PostMapping(value = "/api/v1/conversations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ConversationDetailResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateConversationRequest request) {
        return conversationService.create(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_REPLY')")
    @PostMapping(value = "/api/v1/conversations/{conversationId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ConversationMessageResponse sendMessage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendConversationMessageRequest request) {
        return conversationService.sendMessage(authenticatedUser, conversationId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_ASSIGN')")
    @PostMapping(value = "/api/v1/conversations/{conversationId}/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ConversationDetailResponse assign(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody AssignConversationRequest request) {
        return conversationService.assign(authenticatedUser, conversationId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_VIEW')")
    @PostMapping("/api/v1/conversations/{conversationId}/mark-read")
    public ConversationDetailResponse markRead(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId) {
        return conversationService.markRead(authenticatedUser, conversationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_REPLY')")
    @PostMapping("/api/v1/conversations/{conversationId}/preview-ai")
    public ConversationAiReplyResponse previewAiReply(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId) {
        return conversationService.previewAiReply(authenticatedUser, conversationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_REPLY')")
    @PostMapping("/api/v1/conversations/{conversationId}/close")
    public ConversationDetailResponse close(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId) {
        return conversationService.close(authenticatedUser, conversationId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CONVERSATIONS_REPLY')")
    @PostMapping("/api/v1/conversations/{conversationId}/reopen")
    public ConversationDetailResponse reopen(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID conversationId) {
        return conversationService.reopen(authenticatedUser, conversationId);
    }
}
