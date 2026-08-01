package com.asistentewhatsapp.channels.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class WhatsAppChannelJdbcRepositoryTest {

	@Test
	void insertChannelEventLogIsIdempotentWhenConflictDoesNothing() {
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		WhatsAppChannelJdbcRepository repository = new WhatsAppChannelJdbcRepository(jdbcTemplate, new ObjectMapper());
		when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

		boolean inserted = repository.insertChannelEventLog(UUID.randomUUID(), UUID.randomUUID(), "delivery-1",
				"SESSION_STATUS_CHANGED", "{}", OffsetDateTime.now());

		assertThat(inserted).isFalse();
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(Object[].class));
		assertThat(sql.getValue()).contains("on conflict do nothing");
	}

	@Test
	void updateMessageStatusKeepsAckProgressionFields() {
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		WhatsAppChannelJdbcRepository repository = new WhatsAppChannelJdbcRepository(jdbcTemplate, new ObjectMapper());

		repository.updateMessageStatus(UUID.randomUUID(), "DELIVERED", "provider-ack", OffsetDateTime.now());

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sql.capture(), any(Object[].class));
		assertThat(sql.getValue()).contains("set status = ?")
				.contains("sent_at = case when ? in ('SENT', 'DELIVERED', 'READ')").contains("provider_event_id = ?");
	}
}
