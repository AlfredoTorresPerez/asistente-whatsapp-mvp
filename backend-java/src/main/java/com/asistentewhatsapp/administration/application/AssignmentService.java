package com.asistentewhatsapp.administration.application;

import com.asistentewhatsapp.administration.api.AssignmentGroupResponse;
import com.asistentewhatsapp.administration.api.AssignmentRequest;
import com.asistentewhatsapp.administration.api.AssignmentResponse;
import com.asistentewhatsapp.administration.api.AssignmentSummaryResponse;
import com.asistentewhatsapp.administration.infrastructure.AssignmentJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

	private static final int MAX_PAGE_SIZE = 200;

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

	@Transactional(readOnly = true)
	public PagedResponse<AssignmentGroupResponse> listGroups(AuthenticatedUser authenticatedUser, int page, int size,
			String search, UUID serviceId, String coverage) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		return assignmentJdbcRepository.findGroups(authenticatedUser.businessId(), safePage, safeSize, search,
				serviceId, coverage);
	}

	@Transactional(readOnly = true)
	public AssignmentSummaryResponse summary(AuthenticatedUser authenticatedUser) {
		return assignmentJdbcRepository.summary(authenticatedUser.businessId());
	}

	@Transactional
	public AssignmentResponse setAssignmentActive(AuthenticatedUser authenticatedUser, UUID assignmentId,
			boolean active) {
		return assignmentJdbcRepository.updateActive(authenticatedUser.businessId(), assignmentId, active);
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
