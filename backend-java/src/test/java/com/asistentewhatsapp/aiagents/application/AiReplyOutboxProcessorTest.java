package com.asistentewhatsapp.aiagents.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.asistentewhatsapp.aesthetic.application.AestheticCenterService;
import com.asistentewhatsapp.aiagents.infrastructure.AiReplyOutboxJdbcRepository;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.infrastructure.WhatsAppChannelJdbcRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AiReplyOutboxProcessorTest {

	@Test
	void processDueJobsDoesNotProcessAnythingWhenNoJobsAreClaimed() {
		AiReplyOutboxJdbcRepository outboxRepository = mock(AiReplyOutboxJdbcRepository.class);
		WhatsAppChannelJdbcRepository channelRepository = mock(WhatsAppChannelJdbcRepository.class);
		AestheticCenterService aestheticCenterService = mock(AestheticCenterService.class);
		AgentCoordinatorService agentCoordinatorService = mock(AgentCoordinatorService.class);
		ChannelDispatchService channelDispatchService = mock(ChannelDispatchService.class);
		AiAgentProperties properties = new AiAgentProperties();
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		AiReplyOutboxProcessor processor = new AiReplyOutboxProcessor(outboxRepository, channelRepository,
				aestheticCenterService, agentCoordinatorService, channelDispatchService, properties, jdbcTemplate, 10,
				120000L, 30000L, 900000L);
		when(outboxRepository.claimDueJobs(10, 120000)).thenReturn(List.of());

		processor.processDueJobs();

		verify(outboxRepository).claimDueJobs(10, 120000);
		verifyNoInteractions(channelRepository, aestheticCenterService, agentCoordinatorService,
				channelDispatchService);
	}
}
