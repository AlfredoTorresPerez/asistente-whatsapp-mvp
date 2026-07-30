package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.RoomRequest;
import com.asistentewhatsapp.administration.api.RoomResponse;
import com.asistentewhatsapp.administration.infrastructure.RoomJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

	private final RoomJdbcRepository roomJdbcRepository;

	public RoomService(RoomJdbcRepository roomJdbcRepository) {
		this.roomJdbcRepository = roomJdbcRepository;
	}

	@Transactional(readOnly = true)
	public PagedResponse<RoomResponse> listRooms(AuthenticatedUser authenticatedUser, int page, int size, String search,
			UUID locationId, String roomType, Boolean active) {
		return roomJdbcRepository.findRooms(authenticatedUser.businessId(), Math.max(page, 0),
				Math.min(Math.max(size, 1), 100), normalize(search), locationId, normalize(roomType), active);
	}

	@Transactional(readOnly = true)
	public RoomResponse getRoom(AuthenticatedUser authenticatedUser, UUID roomId) {
		return roomJdbcRepository.findRoom(authenticatedUser.businessId(), roomId);
	}

	@Transactional
	public RoomResponse createRoom(AuthenticatedUser authenticatedUser, RoomRequest request) {
		return roomJdbcRepository.insertRoom(authenticatedUser.businessId(), request);
	}

	@Transactional
	public RoomResponse updateRoom(AuthenticatedUser authenticatedUser, UUID roomId, RoomRequest request) {
		return roomJdbcRepository.updateRoom(authenticatedUser.businessId(), roomId, request);
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
