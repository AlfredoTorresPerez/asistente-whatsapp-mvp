package com.asistentewhatsapp.content.application;

import com.asistentewhatsapp.content.ContentItemType;
import com.asistentewhatsapp.content.api.dto.ContentItemDetailResponse;
import com.asistentewhatsapp.content.api.dto.ContentItemImageUploadResponse;
import com.asistentewhatsapp.content.api.dto.ContentItemListRequest;
import com.asistentewhatsapp.content.api.dto.ContentItemListResponse;
import com.asistentewhatsapp.content.api.dto.ContentItemStatsResponse;
import com.asistentewhatsapp.content.api.dto.ContentItemSummaryResponse;
import com.asistentewhatsapp.content.api.dto.CreateContentItemRequest;
import com.asistentewhatsapp.content.api.dto.PublicContentItemResponse;
import com.asistentewhatsapp.content.api.dto.UpdateContentItemRequest;
import com.asistentewhatsapp.content.infrastructure.ContentItemJdbcRepository;
import com.asistentewhatsapp.content.infrastructure.ContentItemRecord;
import com.asistentewhatsapp.security.application.AuditService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContentItemService {

	private final ContentItemJdbcRepository repository;
	private final FileStorageService storageService;
	private final AuditService auditService;

	public ContentItemService(ContentItemJdbcRepository repository, FileStorageService storageService,
			AuditService auditService) {
		this.repository = repository;
		this.storageService = storageService;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public ContentItemListResponse list(AuthenticatedUser user, ContentItemListRequest request) {
		UUID businessId = user.businessId();
		ContentItemType type = request.type() != null && !request.type().isBlank()
				? ContentItemType.valueOf(request.type().toUpperCase())
				: null;
		String status = request.status();

		long totalItems = repository.count(businessId, type, status);
		int totalPages = (int) Math.ceil((double) totalItems / request.size());

		List<ContentItemRecord> items = repository.findAll(businessId, type, status, request.page(), request.size());

		List<ContentItemSummaryResponse> summaries = items.stream().map(this::toSummaryResponse).toList();

		return new ContentItemListResponse(summaries, request.page(), request.size(), totalItems, totalPages,
				getStats(businessId));
	}

	@Transactional(readOnly = true)
	public ContentItemDetailResponse get(AuthenticatedUser user, UUID id) {
		ContentItemRecord record = repository.findByIdOrThrow(user.businessId(), id);
		return toDetailResponse(record);
	}

	public ContentItemDetailResponse create(AuthenticatedUser user, CreateContentItemRequest request,
			MultipartFile imageFile) {
		validateCreateRequest(request);

		ContentItemType type = ContentItemType.valueOf(request.type().toUpperCase());
		String status = request.status().toUpperCase();

		String imagePath = null;
		if (imageFile != null && !imageFile.isEmpty()) {
			imagePath = storageService.store(imageFile, user.businessId().toString());
		}

		UUID id = repository.insert(user.businessId(), type, imagePath, request.text().trim(), status, user.userId());

		auditService.record(user.businessId(), user.userId(), "CONTENT_ITEM_CREATED", "CONTENT_ITEM", id,
				"Contenido creado: " + request.text().substring(0, Math.min(50, request.text().length())),
				java.util.Map.of("type", type.name(), "status", status));

		return get(user, id);
	}

	public ContentItemDetailResponse update(AuthenticatedUser user, UUID id, UpdateContentItemRequest request,
			MultipartFile imageFile) {
		ContentItemRecord existing = repository.findByIdOrThrow(user.businessId(), id);

		validateUpdateRequest(request);

		ContentItemType type = request.type() != null && !request.type().isBlank()
				? ContentItemType.valueOf(request.type().toUpperCase())
				: existing.type();
		String text = request.text() != null ? request.text().trim() : existing.text();
		String status = request.status() != null && !request.status().isBlank()
				? request.status().toUpperCase()
				: existing.status();

		String imagePath = existing.imagePath();

		if (imageFile != null && !imageFile.isEmpty()) {
			String newImagePath = storageService.store(imageFile, user.businessId().toString());
			if (existing.imagePath() != null) {
				storageService.delete(existing.imagePath());
			}
			imagePath = newImagePath;
		}

		repository.update(user.businessId(), id, type, imagePath, text, status, user.userId(), existing.version());

		auditService.record(user.businessId(), user.userId(), "CONTENT_ITEM_UPDATED", "CONTENT_ITEM", id,
				"Contenido actualizado", java.util.Map.of("type", type.name(), "status", status));

		return get(user, id);
	}

	public ContentItemDetailResponse updateStatus(AuthenticatedUser user, UUID id, String status) {
		ContentItemRecord existing = repository.findByIdOrThrow(user.businessId(), id);

		String upperStatus = status.toUpperCase();
		if (!"ACTIVE".equals(upperStatus) && !"INACTIVE".equals(upperStatus)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Estado debe ser ACTIVE o INACTIVE");
		}

		repository.updateStatus(user.businessId(), id, upperStatus, user.userId(), existing.version());

		String action = "ACTIVE".equals(upperStatus) ? "CONTENT_ITEM_ACTIVATED" : "CONTENT_ITEM_DEACTIVATED";
		auditService.record(user.businessId(), user.userId(), action, "CONTENT_ITEM", id,
				"Contenido " + ("ACTIVE".equals(upperStatus) ? "activado" : "desactivado"),
				java.util.Map.of("status", upperStatus));

		return get(user, id);
	}

	public ContentItemImageUploadResponse uploadImage(AuthenticatedUser user, UUID id, MultipartFile imageFile) {
		ContentItemRecord existing = repository.findByIdOrThrow(user.businessId(), id);

		String newImagePath = storageService.store(imageFile, user.businessId().toString());

		if (existing.imagePath() != null) {
			storageService.delete(existing.imagePath());
		}

		repository.updateImagePath(user.businessId(), id, newImagePath, user.userId(), existing.version());

		auditService.record(user.businessId(), user.userId(), "CONTENT_ITEM_IMAGE_REPLACED", "CONTENT_ITEM", id,
				"Imagen reemplazada", java.util.Map.of("newImagePath", newImagePath));

		return new ContentItemImageUploadResponse(newImagePath, storageService.getPublicUrl(newImagePath));
	}

	public void deleteImage(AuthenticatedUser user, UUID id) {
		ContentItemRecord existing = repository.findByIdOrThrow(user.businessId(), id);

		if (existing.imagePath() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "NO_IMAGE", "El registro no tiene imagen para eliminar.");
		}

		storageService.delete(existing.imagePath());
		repository.clearImagePath(user.businessId(), id, user.userId(), existing.version());

		auditService.record(user.businessId(), user.userId(), "CONTENT_ITEM_IMAGE_DELETED", "CONTENT_ITEM", id,
				"Imagen eliminada", java.util.Map.of());
	}

	public void delete(AuthenticatedUser user, UUID id) {
		ContentItemRecord existing = repository.findByIdOrThrow(user.businessId(), id);

		if (existing.imagePath() != null) {
			storageService.delete(existing.imagePath());
		}

		repository.delete(user.businessId(), id);

		auditService.record(user.businessId(), user.userId(), "CONTENT_ITEM_DELETED", "CONTENT_ITEM", id,
				"Contenido eliminado", java.util.Map.of("type", existing.type().name()));
	}

	@Transactional(readOnly = true)
	public List<PublicContentItemResponse> getPublicContent(UUID businessId, ContentItemType type) {
		return repository.findPublicActive(businessId, type).stream().map(this::toPublicResponse).toList();
	}

	private void validateCreateRequest(CreateContentItemRequest request) {
		if (request.type() == null || request.type().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "TYPE_REQUIRED", "El tipo es obligatorio.");
		}
		try {
			ContentItemType.valueOf(request.type().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "Tipo invalido: " + request.type());
		}
		if (request.text() == null || request.text().trim().isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "TEXT_REQUIRED", "El texto es obligatorio.");
		}
		if (request.text().trim().length() > 200) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "TEXT_TOO_LONG",
					"El texto no puede superar 200 caracteres.");
		}
		if (request.status() == null || request.status().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "STATUS_REQUIRED", "El estado es obligatorio.");
		}
		String upperStatus = request.status().toUpperCase();
		if (!"ACTIVE".equals(upperStatus) && !"INACTIVE".equals(upperStatus)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Estado debe ser ACTIVE o INACTIVE");
		}
	}

	private void validateUpdateRequest(UpdateContentItemRequest request) {
		if (request.type() != null && !request.type().isBlank()) {
			try {
				ContentItemType.valueOf(request.type().toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "Tipo invalido: " + request.type());
			}
		}
		if (request.text() != null) {
			String trimmed = request.text().trim();
			if (trimmed.isEmpty()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "TEXT_EMPTY", "El texto no puede estar vacio.");
			}
			if (trimmed.length() > 200) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "TEXT_TOO_LONG",
						"El texto no puede superar 200 caracteres.");
			}
		}
		if (request.status() != null && !request.status().isBlank()) {
			String upperStatus = request.status().toUpperCase();
			if (!"ACTIVE".equals(upperStatus) && !"INACTIVE".equals(upperStatus)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Estado debe ser ACTIVE o INACTIVE");
			}
		}
	}

	private ContentItemSummaryResponse toSummaryResponse(ContentItemRecord record) {
		return new ContentItemSummaryResponse(record.id(), record.type().name(), record.type().getLabel(),
				record.imagePath(), record.imagePath() != null ? storageService.getPublicUrl(record.imagePath()) : null,
				record.text().length() > 100 ? record.text().substring(0, 100) + "..." : record.text(), record.status(),
				record.updatedAt());
	}

	private ContentItemDetailResponse toDetailResponse(ContentItemRecord record) {
		return new ContentItemDetailResponse(record.id(), record.type().name(), record.type().getLabel(),
				record.imagePath(), record.imagePath() != null ? storageService.getPublicUrl(record.imagePath()) : null,
				record.text(), record.status(), record.createdAt(), record.updatedAt(), record.createdBy(),
				record.updatedBy(), record.version());
	}

	private PublicContentItemResponse toPublicResponse(ContentItemRecord record) {
		return new PublicContentItemResponse(record.id(), record.type().name(), record.text(),
				record.imagePath() != null ? storageService.getPublicUrl(record.imagePath()) : null, record.status());
	}

	private ContentItemStatsResponse getStats(UUID businessId) {
		return new ContentItemStatsResponse(repository.count(businessId, null, null),
				repository.countByTypeAndStatus(businessId, null, "ACTIVE"),
				repository.countByTypeAndStatus(businessId, null, "INACTIVE"), repository
						.findAll(businessId, null, null, 0, 1000).stream().filter(r -> r.imagePath() == null).count());
	}
}