package com.asistentewhatsapp.administration.api;

import java.util.List;
import java.util.UUID;

public record AssignmentGroupResponse(UUID serviceId, String serviceName, String serviceCode,
		List<AssignmentResponse> professionals, List<AssignmentResponse> rooms, int professionalsCount, int roomsCount,
		boolean covered) {
}
