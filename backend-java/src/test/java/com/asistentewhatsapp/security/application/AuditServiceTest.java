package com.asistentewhatsapp.security.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.asistentewhatsapp.security.domain.AuditLog;
import com.asistentewhatsapp.security.infrastructure.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditServiceTest {

	@Test
	void recordSerializesStructuredMetadata() {
		AuditLogRepository repository = mock(AuditLogRepository.class);
		AuditService auditService = new AuditService(repository, new ObjectMapper());
		UUID businessId = UUID.randomUUID();
		UUID entityId = UUID.randomUUID();

		auditService.record(businessId, null, "BOOKING_CONFIRMED", "BOOKING", entityId, "Reserva confirmada.",
				Map.of("previousStatus", "PENDIENTE_CONFIRMACION", "newStatus", "CONFIRMADA"));

		ArgumentCaptor<AuditLog> auditLog = ArgumentCaptor.forClass(AuditLog.class);
		verify(repository).save(auditLog.capture());
		assertThat(auditLog.getValue().getMetadata()).contains("\"previousStatus\":\"PENDIENTE_CONFIRMACION\"")
				.contains("\"newStatus\":\"CONFIRMADA\"");
	}
}
