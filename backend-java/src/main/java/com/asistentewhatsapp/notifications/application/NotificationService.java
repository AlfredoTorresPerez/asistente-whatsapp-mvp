package com.asistentewhatsapp.notifications.application;

import com.asistentewhatsapp.notifications.api.NotificationReadResponse;
import com.asistentewhatsapp.notifications.api.NotificationResponse;
import com.asistentewhatsapp.notifications.api.NotificationsReadAllResponse;
import com.asistentewhatsapp.notifications.infrastructure.NotificationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationJdbcRepository notificationJdbcRepository;

	public NotificationService(NotificationJdbcRepository notificationJdbcRepository) {
		this.notificationJdbcRepository = notificationJdbcRepository;
	}

	@Transactional(readOnly = true)
	public PagedResponse<NotificationResponse> list(AuthenticatedUser authenticatedUser, int page, int size,
			String search, String status, String type) {
		int resolvedPage = Math.max(page, 0);
		int resolvedSize = Math.min(Math.max(size, 1), 100);
		String normalizedSearch = normalizeSearch(search);
		String normalizedStatus = normalizeFilter(status);
		String normalizedType = normalizeFilter(type);

		return notificationJdbcRepository.findByUser(authenticatedUser.businessId(), authenticatedUser.userId(),
				resolvedPage, resolvedSize, normalizedSearch, normalizedStatus, normalizedType);
	}

	@Transactional
	public NotificationReadResponse markAsRead(AuthenticatedUser authenticatedUser, UUID notificationId) {
		return notificationJdbcRepository.markAsRead(authenticatedUser.businessId(), authenticatedUser.userId(),
				notificationId);
	}

	@Transactional
	public NotificationsReadAllResponse markAllAsRead(AuthenticatedUser authenticatedUser) {
		long updatedCount = notificationJdbcRepository.markAllAsRead(authenticatedUser.businessId(),
				authenticatedUser.userId());
		return new NotificationsReadAllResponse(updatedCount);
	}

	private String normalizeSearch(String search) {
		if (search == null || search.isBlank()) {
			return null;
		}
		String normalized = search.trim();
		if (normalized.length() > 80) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos invalidos.",
					Map.of("search", "La busqueda no puede superar los 80 caracteres."));
		}
		return normalized;
	}

	private String normalizeFilter(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toUpperCase();
	}
}
