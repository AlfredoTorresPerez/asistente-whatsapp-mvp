package com.asistentewhatsapp.leads.application;

import com.asistentewhatsapp.leads.api.AddLeadNoteRequest;
import com.asistentewhatsapp.leads.api.CreateLeadFromConversationRequest;
import com.asistentewhatsapp.leads.api.CreateLeadRequest;
import com.asistentewhatsapp.leads.api.LeadDetailResponse;
import com.asistentewhatsapp.leads.api.LeadNoteResponse;
import com.asistentewhatsapp.leads.api.LeadSummaryResponse;
import com.asistentewhatsapp.leads.api.UpdateLeadRequest;
import com.asistentewhatsapp.leads.api.UpdateLeadStageRequest;
import com.asistentewhatsapp.leads.infrastructure.LeadJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {

    private final LeadJdbcRepository leadJdbcRepository;

    public LeadService(LeadJdbcRepository leadJdbcRepository) {
        this.leadJdbcRepository = leadJdbcRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadSummaryResponse> list(
            AuthenticatedUser authenticatedUser,
            int page,
            int size,
            String search,
            String stage,
            String origin,
            UUID responsibleUserId) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), 100);

        return leadJdbcRepository.findLeads(
                authenticatedUser.businessId(),
                resolvedPage,
                resolvedSize,
                normalizeSearch(search),
                normalizeOptionalStage(stage),
                normalizeOptionalOrigin(origin),
                resolveResponsibleUserId(authenticatedUser, responsibleUserId));
    }

    @Transactional(readOnly = true)
    public LeadDetailResponse getDetail(AuthenticatedUser authenticatedUser, UUID leadId) {
        return leadJdbcRepository.findLeadDetail(authenticatedUser.businessId(), leadId);
    }

    @Transactional
    public LeadDetailResponse create(AuthenticatedUser authenticatedUser, CreateLeadRequest request) {
        String firstName = normalizeRequiredValue(request.firstName(), "firstName", 80);
        String lastName = normalizeRequiredValue(request.lastName(), "lastName", 80);
        String phone = normalizePhone(request.phone(), "phone");
        String email = normalizeOptionalEmail(request.email());
        String notes = normalizeOptionalText(request.notes(), 2000);
        String stage = normalizeStage(request.stage());
        UUID assignedUserId = resolveResponsibleUserId(authenticatedUser, request.assignedUserId());

        LeadJdbcRepository.CustomerRecord customer = resolveOrCreateCustomer(
                authenticatedUser.businessId(),
                null,
                firstName,
                lastName,
                phone,
                email);

        UUID leadId = leadJdbcRepository.insertLead(
                authenticatedUser.businessId(),
                customer.id(),
                null,
                "MANUAL",
                firstName,
                lastName,
                phone,
                email,
                stage,
                notes,
                assignedUserId);
        return leadJdbcRepository.findLeadDetail(authenticatedUser.businessId(), leadId);
    }

    @Transactional
    public LeadDetailResponse createFromConversation(
            AuthenticatedUser authenticatedUser,
            UUID conversationId,
            CreateLeadFromConversationRequest request) {
        LeadJdbcRepository.ConversationLeadContextRecord conversation =
                leadJdbcRepository.findConversationLeadContext(authenticatedUser.businessId(), conversationId);

        UUID existingConversationLeadId = leadJdbcRepository
                .findLeadIdByConversation(authenticatedUser.businessId(), conversationId)
                .orElse(null);
        if (existingConversationLeadId != null) {
            return leadJdbcRepository.findLeadDetail(authenticatedUser.businessId(), existingConversationLeadId);
        }

        String derivedDisplayName = normalizeRequiredValue(conversation.customerDisplayName(), "conversationId", 160);
        NameParts baseName = splitDisplayName(derivedDisplayName);
        String firstName = normalizeOptionalValue(request.firstName(), 80);
        if (firstName == null) {
            firstName = conversation.customerFirstName() != null && !conversation.customerFirstName().isBlank()
                    ? normalizeRequiredValue(conversation.customerFirstName(), "firstName", 80)
                    : baseName.firstName();
        }

        String lastName = normalizeOptionalValue(request.lastName(), 80);
        if (lastName == null) {
            lastName = conversation.customerLastName() != null && !conversation.customerLastName().isBlank()
                    ? normalizeRequiredValue(conversation.customerLastName(), "lastName", 80)
                    : baseName.lastName();
        }

        String phone = request.phone() == null || request.phone().isBlank()
                ? normalizePhone(conversation.customerPhone(), "phone")
                : normalizePhone(request.phone(), "phone");

        UUID existingPhoneLeadId = leadJdbcRepository
                .findActiveLeadIdByNormalizedPhone(authenticatedUser.businessId(), phone)
                .orElse(null);
        if (existingPhoneLeadId != null) {
            leadJdbcRepository.linkLeadToConversationIfUnlinked(
                    authenticatedUser.businessId(),
                    existingPhoneLeadId,
                    conversationId);
            return leadJdbcRepository.findLeadDetail(authenticatedUser.businessId(), existingPhoneLeadId);
        }

        String email = request.email() == null ? normalizeOptionalEmail(conversation.customerEmail()) : normalizeOptionalEmail(request.email());
        String notes = normalizeOptionalText(request.notes(), 2000);
        String stage = normalizeStage(request.stage());
        UUID assignedUserId = resolveResponsibleUserId(
                authenticatedUser,
                request.assignedUserId() != null ? request.assignedUserId() : conversation.assignedUserId());

        LeadJdbcRepository.CustomerRecord customer = resolveOrCreateCustomer(
                authenticatedUser.businessId(),
                conversation.customerId(),
                firstName,
                lastName,
                phone,
                email);

        UUID leadId = leadJdbcRepository.insertLead(
                authenticatedUser.businessId(),
                customer.id(),
                conversationId,
                "CONVERSATION",
                firstName,
                lastName,
                phone,
                email,
                stage,
                notes,
                assignedUserId);
        return leadJdbcRepository.findLeadDetail(authenticatedUser.businessId(), leadId);
    }

    @Transactional
    public LeadDetailResponse update(
            AuthenticatedUser authenticatedUser,
            UUID leadId,
            UpdateLeadRequest request) {
        LeadJdbcRepository.LeadContextRecord currentLead =
                leadJdbcRepository.findLeadContext(authenticatedUser.businessId(), leadId);

        String firstName = normalizeRequiredValue(request.firstName(), "firstName", 80);
        String lastName = normalizeRequiredValue(request.lastName(), "lastName", 80);
        String phone = normalizePhone(request.phone(), "phone");
        String email = normalizeOptionalEmail(request.email());
        String notes = normalizeOptionalText(request.notes(), 2000);
        String stage = normalizeStage(request.stage());
        UUID assignedUserId = resolveResponsibleUserId(authenticatedUser, request.assignedUserId());

        LeadJdbcRepository.CustomerRecord customer = resolveOrCreateCustomer(
                authenticatedUser.businessId(),
                currentLead.customerId(),
                firstName,
                lastName,
                phone,
                email);

        leadJdbcRepository.updateLead(
                authenticatedUser.businessId(),
                leadId,
                customer.id(),
                firstName,
                lastName,
                phone,
                email,
                stage,
                notes,
                assignedUserId);
        return leadJdbcRepository.findLeadDetail(authenticatedUser.businessId(), leadId);
    }

    @Transactional
    public LeadNoteResponse addNote(
            AuthenticatedUser authenticatedUser,
            UUID leadId,
            AddLeadNoteRequest request) {
        leadJdbcRepository.findLeadContext(authenticatedUser.businessId(), leadId);
        UUID noteId = leadJdbcRepository.insertLeadNote(
                authenticatedUser.businessId(),
                leadId,
                authenticatedUser.userId(),
                normalizeRequiredValue(request.noteText(), "noteText", 2000));
        return leadJdbcRepository.findLeadNoteById(authenticatedUser.businessId(), leadId, noteId);
    }

    @Transactional
    public LeadDetailResponse updateStage(
            AuthenticatedUser authenticatedUser,
            UUID leadId,
            UpdateLeadStageRequest request) {
        leadJdbcRepository.updateLeadStage(
                authenticatedUser.businessId(),
                leadId,
                normalizeRequiredStage(request.stage()));
        return leadJdbcRepository.findLeadDetail(authenticatedUser.businessId(), leadId);
    }

    private LeadJdbcRepository.CustomerRecord resolveOrCreateCustomer(
            UUID businessId,
            UUID currentCustomerId,
            String firstName,
            String lastName,
            String phone,
            String email) {
        LeadJdbcRepository.CustomerRecord existingByPhone = leadJdbcRepository
                .findCustomerByNormalizedPhone(businessId, phone)
                .orElse(null);

        if (existingByPhone != null) {
            leadJdbcRepository.updateCustomer(
                    businessId,
                    existingByPhone.id(),
                    firstName,
                    lastName,
                    displayName(firstName, lastName),
                    phone,
                    email);
            return leadJdbcRepository.findCustomerById(businessId, existingByPhone.id());
        }

        if (currentCustomerId != null) {
            leadJdbcRepository.updateCustomer(
                    businessId,
                    currentCustomerId,
                    firstName,
                    lastName,
                    displayName(firstName, lastName),
                    phone,
                    email);
            return leadJdbcRepository.findCustomerById(businessId, currentCustomerId);
        }

        UUID customerId = leadJdbcRepository.insertCustomer(
                businessId,
                firstName,
                lastName,
                displayName(firstName, lastName),
                phone,
                email);
        return leadJdbcRepository.findCustomerById(businessId, customerId);
    }

    private UUID resolveResponsibleUserId(AuthenticatedUser authenticatedUser, UUID responsibleUserId) {
        if (responsibleUserId == null) {
            return authenticatedUser.userId();
        }

        return leadJdbcRepository.findUserId(authenticatedUser.businessId(), responsibleUserId)
                .orElseThrow(() -> validationError("assignedUserId", "El responsable indicado no existe."));
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String normalized = search.trim();
        if (normalized.length() > 80) {
            throw validationError("search", "La busqueda no puede superar los 80 caracteres.");
        }
        return normalized;
    }

    private String normalizeRequiredStage(String stage) {
        String normalized = normalizeRequiredValue(stage, "stage", 20);
        return normalizeStage(normalized);
    }

    private String normalizeStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return "NEW";
        }
        return switch (stage.trim().toUpperCase()) {
            case "NEW", "CONTACTED", "INTERESTED", "SCHEDULED", "WON", "LOST" -> stage.trim().toUpperCase();
            case "QUALIFIED" -> "INTERESTED";
            case "PROPOSAL" -> "SCHEDULED";
            default -> throw validationError("stage", "La etapa indicada no es valida.");
        };
    }

    private String normalizeOptionalStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return null;
        }
        return normalizeStage(stage);
    }

    private String normalizeOptionalOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }

        return switch (origin.trim().toUpperCase()) {
            case "MANUAL", "CONVERSATION" -> origin.trim().toUpperCase();
            default -> throw validationError("origin", "El origen indicado no es valido.");
        };
    }

    private String normalizeRequiredValue(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw validationError(field, "Este campo es obligatorio.");
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validationError(field, "El valor supera el largo maximo permitido.");
        }
        return normalized;
    }

    private String normalizeOptionalValue(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validationError("value", "El valor supera el largo maximo permitido.");
        }
        return normalized;
    }

    private String normalizePhone(String phone, String field) {
        String normalized = normalizeRequiredValue(phone, field, 30).replace(" ", "");
        if (normalized.length() < 8) {
            throw validationError(field, "El telefono debe tener al menos 8 caracteres.");
        }
        return normalized;
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String normalized = email.trim();
        if (normalized.length() > 255) {
            throw validationError("email", "El correo supera el largo maximo permitido.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw validationError("notes", "El texto supera el largo maximo permitido.");
        }
        return normalized;
    }

    private String displayName(String firstName, String lastName) {
        return (firstName + " " + lastName).trim();
    }

    private NameParts splitDisplayName(String value) {
        String[] parts = value.trim().split("\\s+", 2);
        if (parts.length == 1) {
            return new NameParts(parts[0], parts[0]);
        }
        return new NameParts(parts[0], parts[1]);
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "La solicitud contiene datos invalidos.",
                Map.of(field, message));
    }

    private record NameParts(String firstName, String lastName) {
    }
}
