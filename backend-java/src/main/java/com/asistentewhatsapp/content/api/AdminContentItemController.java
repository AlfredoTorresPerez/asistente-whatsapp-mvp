package com.asistentewhatsapp.content.api;

import com.asistentewhatsapp.content.api.dto.ContentItemDetailResponse;
import com.asistentewhatsapp.content.api.dto.ContentItemImageUploadResponse;
import com.asistentewhatsapp.content.api.dto.ContentItemListRequest;
import com.asistentewhatsapp.content.api.dto.ContentItemListResponse;
import com.asistentewhatsapp.content.api.dto.ContentItemSummaryResponse;
import com.asistentewhatsapp.content.api.dto.CreateContentItemRequest;
import com.asistentewhatsapp.content.api.dto.UpdateContentItemRequest;
import com.asistentewhatsapp.content.api.dto.UpdateContentItemStatusRequest;
import com.asistentewhatsapp.content.application.ContentItemService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/content-items", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminContentItemController {

    private final ContentItemService service;

    public AdminContentItemController(ContentItemService service) {
        this.service = service;
    }

@PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_VIEW')")
    @GetMapping
    public ContentItemListResponse list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ContentItemListRequest request = new ContentItemListRequest(page, size, null, type, status);
        return service.list(user, request);
    }

    @PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_VIEW')")
    @GetMapping("/{id}")
    public ContentItemDetailResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return service.get(user, id);
    }

    @PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_MANAGE')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContentItemDetailResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestPart("request") CreateContentItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return service.create(user, request, image);
    }

    @PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_MANAGE')")
    @PutMapping("/{id}")
    public ContentItemDetailResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentItemRequest request) {
        return service.update(user, id, request, null);
    }

    @PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_MANAGE')")
    @PatchMapping("/{id}/status")
    public ContentItemDetailResponse updateStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentItemStatusRequest request) {
        return service.updateStatus(user, id, request.status());
    }

@PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_MANAGE')")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContentItemImageUploadResponse uploadImage(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestPart("image") MultipartFile image) {
        return service.uploadImage(user, id, image);
    }

    @PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_MANAGE')")
    @DeleteMapping("/{id}/image")
    public void deleteImage(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        service.deleteImage(user, id);
    }

    @PreAuthorize("hasPermission(#user.businessId(), 'CONTENT_MANAGE')")
    @DeleteMapping("/{id}")
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        service.delete(user, id);
    }
}