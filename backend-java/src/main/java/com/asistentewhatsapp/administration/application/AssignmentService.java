package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.AssignmentRequest;
import com.asistentewhatsapp.administration.api.AssignmentResponse;
import com.asistentewhatsapp.administration.infrastructure.AssignmentJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

	private final AssignmentJdbcRepository assignmentJdbcRepository;

	public AssignmentService(AssignmentJdbcRepository assignmentJdbcRepository) {
		this.assignmentJdbcRepository = assignmentJdbcRepository;
	}

	@Transactional(readOnly = true)
	public List<AssignmentResponse> listAssignments(AuthenticatedUser authenticatedUser, UUID serviceId,
			UUID professionalId, UUID roomId) {
		return assignmentJdbcRepository.findAssignments(authenticatedUser.businessId(), serviceId, professionalId,
				roomId);
	}

	@Transactional
	public AssignmentResponse assignProfessionalToService(AuthenticatedUser authenticatedUser,
			AssignmentRequest request) {
		return assignmentJdbcRepository.insertProfessionalService(authenticatedUser.businessId(), request.serviceId(),
				request.professionalId());
	}

	@Transactional
	public AssignmentResponse assignRoomToService(AuthenticatedUser authenticatedUser, AssignmentRequest request) {
		return assignmentJdbcRepository.insertRoomService(authenticatedUser.businessId(), request.serviceId(),
				request.roomId());
	}

	@Transactional
	public void removeAssignment(AuthenticatedUser authenticatedUser, UUID assignmentId) {
		assignmentJdbcRepository.deleteAssignment(authenticatedUser.businessId(), assignmentId);
	}
}
