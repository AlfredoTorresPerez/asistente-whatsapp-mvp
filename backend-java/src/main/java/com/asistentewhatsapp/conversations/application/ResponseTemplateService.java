package com.asistentewhatsapp.conversations.application;

import com.asistentewhatsapp.conversations.api.CreateResponseTemplateRequest;
import com.asistentewhatsapp.conversations.api.ResponseTemplateResponse;
import com.asistentewhatsapp.conversations.api.UpdateResponseTemplateRequest;
import com.asistentewhatsapp.conversations.api.UpdateTemplateStatusRequest;
import com.asistentewhatsapp.conversations.infrastructure.ConversationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResponseTemplateService {

    private final ConversationJdbcRepository conversationJdbcRepository;

    public ResponseTemplateService(ConversationJdbcRepository conversationJdbcRepository) {
        this.conversationJdbcRepository = conversationJdbcRepository;
    }

    @Transactional(readOnly = true)
    public List<ResponseTemplateResponse> list(AuthenticatedUser authenticatedUser, Boolean active) {
        return conversationJdbcRepository.findTemplates(authenticatedUser.businessId(), active);
    }

    @Transactional
    public ResponseTemplateResponse create(
            AuthenticatedUser authenticatedUser,
            CreateResponseTemplateRequest request) {
        String name = request.name().trim();
        validateTemplateName(authenticatedUser.businessId(), name, null);
        UUID templateId = conversationJdbcRepository.insertTemplate(
                authenticatedUser.businessId(),
                name,
                request.category().trim().toUpperCase(),
                request.body().trim(),
                request.active() == null || request.active());
        return conversationJdbcRepository.findTemplateById(authenticatedUser.businessId(), templateId);
    }

    @Transactional
    public ResponseTemplateResponse update(
            AuthenticatedUser authenticatedUser,
            UUID templateId,
            UpdateResponseTemplateRequest request) {
        String name = request.name().trim();
        validateTemplateName(authenticatedUser.businessId(), name, templateId);
        conversationJdbcRepository.updateTemplate(
                authenticatedUser.businessId(),
                templateId,
                name,
                request.category().trim().toUpperCase(),
                request.body().trim());
        return conversationJdbcRepository.findTemplateById(authenticatedUser.businessId(), templateId);
    }

    @Transactional
    public ResponseTemplateResponse updateStatus(
            AuthenticatedUser authenticatedUser,
            UUID templateId,
            UpdateTemplateStatusRequest request) {
        conversationJdbcRepository.updateTemplateStatus(
                authenticatedUser.businessId(),
                templateId,
                request.active());
        return conversationJdbcRepository.findTemplateById(authenticatedUser.businessId(), templateId);
    }

    private void validateTemplateName(UUID businessId, String name, UUID templateId) {
        if (conversationJdbcRepository.existsTemplateName(businessId, name, templateId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La solicitud contiene datos invalidos.",
                    Map.of("name", "Ya existe una plantilla con ese nombre."));
        }
    }
}
