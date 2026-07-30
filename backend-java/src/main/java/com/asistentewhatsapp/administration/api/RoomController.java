package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.RoomService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ROOM_VIEW')")
	@GetMapping(value = "/api/v1/admin/rooms", produces = MediaType.APPLICATION_JSON_VALUE)
	public PagedResponse<RoomResponse> listRooms(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
			@RequestParam(required = false) String search, @RequestParam(required = false) UUID locationId,
			@RequestParam(required = false) String roomType, @RequestParam(required = false) Boolean active) {
		return roomService.listRooms(authenticatedUser, page, size, search, locationId, roomType, active);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ROOM_VIEW')")
	@GetMapping(value = "/api/v1/admin/rooms/{roomId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public RoomResponse getRoom(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID roomId) {
		return roomService.getRoom(authenticatedUser, roomId);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ROOM_MANAGE')")
	@PostMapping(value = "/api/v1/admin/rooms", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public RoomResponse createRoom(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody RoomRequest request) {
		return roomService.createRoom(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ROOM_MANAGE')")
	@PatchMapping(value = "/api/v1/admin/rooms/{roomId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public RoomResponse updateRoom(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID roomId, @Valid @RequestBody RoomRequest request) {
		return roomService.updateRoom(authenticatedUser, roomId, request);
	}
}
