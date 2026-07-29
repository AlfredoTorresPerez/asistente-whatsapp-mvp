package com.asistentewhatsapp.notifications.api;

import com.asistentewhatsapp.notifications.application.NotificationService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'NOTIFICATION_VIEW')")
	@GetMapping("/api/v1/notifications")
	public PagedResponse<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String search, @RequestParam(required = false) String status,
			@RequestParam(required = false) String type) {
		return notificationService.list(authenticatedUser, page, size, search, status, type);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'NOTIFICATION_VIEW')")
	@PatchMapping("/api/v1/notifications/{notificationId}/read")
	public NotificationReadResponse markAsRead(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID notificationId) {
		return notificationService.markAsRead(authenticatedUser, notificationId);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'NOTIFICATION_VIEW')")
	@PatchMapping("/api/v1/notifications/read-all")
	public NotificationsReadAllResponse markAllAsRead(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return notificationService.markAllAsRead(authenticatedUser);
	}
}
