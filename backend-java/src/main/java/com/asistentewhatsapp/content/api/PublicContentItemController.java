package com.asistentewhatsapp.content.api;

import com.asistentewhatsapp.content.api.dto.PublicContentItemResponse;
import com.asistentewhatsapp.content.application.ContentItemService;
import com.asistentewhatsapp.content.ContentItemType;
import com.asistentewhatsapp.cloudapi.onboarding.MetaOnboardingRepository;
import com.asistentewhatsapp.security.infrastructure.BusinessRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/public/v1/content-items", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicContentItemController {

    private final ContentItemService service;
    private final MetaOnboardingRepository metaOnboardingRepository;
    private final BusinessRepository businessRepository;

    public PublicContentItemController(
            ContentItemService service,
            MetaOnboardingRepository metaOnboardingRepository,
            BusinessRepository businessRepository) {
        this.service = service;
        this.metaOnboardingRepository = metaOnboardingRepository;
        this.businessRepository = businessRepository;
    }

    private UUID getDefaultBusinessId() {
        return metaOnboardingRepository.findCentralizedChannel()
                .map(channel -> businessRepository.findById(channel.businessId())
                        .orElseThrow(() -> new IllegalStateException("El canal centralizado no tiene un negocio valido.")))
                .orElseGet(() -> businessRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                        .orElseThrow(() -> new IllegalStateException("No hay un negocio configurado en el sistema.")))
                .getId();
    }

    @GetMapping
    public List<PublicContentItemResponse> list(
            @RequestParam(required = false) String type) {
        UUID businessId = getDefaultBusinessId();
        ContentItemType contentType = type != null && !type.isBlank() ? ContentItemType.valueOf(type.toUpperCase()) : null;
        return service.getPublicContent(businessId, contentType);
    }
}