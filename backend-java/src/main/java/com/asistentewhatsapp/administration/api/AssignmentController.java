package com.asistentewhatsapp.administration.api;

import com.asistentewhatsapp.administration.application.AssignmentService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssignmentController {

	private final AssignmentService assignmentService;

	public AssignmentController(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ASSIGNMENT_VIEW')")
	@GetMapping(value = "/api/v1/admin/assignments", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<AssignmentResponse> listAssignments(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestParam(required = false) UUID serviceId, @RequestParam(required = false) UUID professionalId,
			@RequestParam(required = false) UUID roomId) {
		return assignmentService.listAssignments(authenticatedUser, serviceId, professionalId, roomId);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ASSIGNMENT_MANAGE')")
	@PostMapping(value = "/api/v1/admin/assignments/professional-service", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public AssignmentResponse assignProfessionalToService(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody AssignmentRequest request) {
		return assignmentService.assignProfessionalToService(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ASSIGNMENT_MANAGE')")
	@PostMapping(value = "/api/v1/admin/assignments/room-service", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public AssignmentResponse assignRoomToService(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody AssignmentRequest request) {
		return assignmentService.assignRoomToService(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'ASSIGNMENT_MANAGE')")
	@DeleteMapping("/api/v1/admin/assignments/{assignmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeAssignment(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID assignmentId) {
		assignmentService.removeAssignment(authenticatedUser, assignmentId);
	}
}
