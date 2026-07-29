package com.asistentewhatsapp.conversations.application;

import com.asistentewhatsapp.aesthetic.infrastructure.AestheticCenterJdbcRepository;
import com.asistentewhatsapp.aiagents.application.AgentCoordinatorService;
import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.conversations.api.ConversationMessageResponse;
import com.asistentewhatsapp.conversations.api.SendConversationMessageRequest;
import com.asistentewhatsapp.conversations.infrastructure.ConversationJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.exception.MessagingChannelUnavailableException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceDispatchConsistencyTest {

	private final ConversationJdbcRepository repository = mock(ConversationJdbcRepository.class);
	private final ChannelDispatchService channelDispatchService = mock(ChannelDispatchService.class);
	private final TemplateVariableRenderer templateVariableRenderer = mock(TemplateVariableRenderer.class);
	private final AgentCoordinatorService agentCoordinatorService = mock(AgentCoordinatorService.class);
	private final AestheticCenterJdbcRepository aestheticCenterJdbcRepository = mock(
			AestheticCenterJdbcRepository.class);
	private final PlatformTransactionManager transactionManager = immediateTransactionManager();
	private final ConversationService service = new ConversationService(repository, channelDispatchService,
			templateVariableRenderer, agentCoordinatorService, aestheticCenterJdbcRepository, transactionManager);

	@Test
	void sendMessagePersistsPendingBeforeDispatchAndMarksSimulated() {
		AuthenticatedUser user = authenticatedUser();
		UUID conversationId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		ConversationJdbcRepository.ConversationContextRecord context = context(conversationId);
		when(repository.findConversationContext(user.businessId(), conversationId)).thenReturn(context);
		when(repository.findOutboundMessageByIdempotencyKey(user.businessId(), conversationId, "retry-1"))
				.thenReturn(Optional.empty());
		when(repository.findLatestProviderChatId(user.businessId(), conversationId)).thenReturn(Optional.empty());
		when(repository.insertOutboundMessage(eq(user.businessId()), eq(conversationId), eq(user.userId()), eq("Hola"),
				eq("PENDING"), eq(null), eq(null), eq("retry-1"))).thenReturn(messageId);
		when(channelDispatchService.dispatch(any(ChannelDispatchRequest.class))).thenReturn(new ChannelDispatchResponse(
				MessageChannelType.WHATSAPP, "sim-1", "SIMULATED", Instant.parse("2026-06-05T12:00:00Z")));
		when(repository.findMessageById(user.businessId(), conversationId, messageId))
				.thenReturn(message(messageId, "SIMULATED"));

		ConversationMessageResponse response = service.sendMessage(user, conversationId,
				new SendConversationMessageRequest("Hola", null, "retry-1", null));

		assertThat(response.status()).isEqualTo("SIMULATED");
		inOrder(repository, channelDispatchService).verify(repository).insertOutboundMessage(eq(user.businessId()),
				eq(conversationId), eq(user.userId()), eq("Hola"), eq("PENDING"), eq(null), eq(null), eq("retry-1"));
		verify(repository).insertMessageDeliveryLog(eq(user.businessId()), eq(messageId), eq("PENDING"), eq(null),
				any());
		verify(repository).updateOutboundMessageDelivery(eq(user.businessId()), eq(conversationId), eq(messageId),
				eq("SIMULATED"), eq("sim-1"), any());
	}

	@Test
	void sendMessageMarksFailedWhenProviderIsDown() {
		AuthenticatedUser user = authenticatedUser();
		UUID conversationId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		when(repository.findConversationContext(user.businessId(), conversationId)).thenReturn(context(conversationId));
		when(repository.findLatestProviderChatId(user.businessId(), conversationId)).thenReturn(Optional.empty());
		when(repository.insertOutboundMessage(any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(messageId);
		when(channelDispatchService.dispatch(any(ChannelDispatchRequest.class)))
				.thenThrow(new MessagingChannelUnavailableException("Canal caido"));
		when(repository.findMessageById(user.businessId(), conversationId, messageId))
				.thenReturn(message(messageId, "FAILED"));

		ConversationMessageResponse response = service.sendMessage(user, conversationId,
				new SendConversationMessageRequest("Hola", null, null, null));

		assertThat(response.status()).isEqualTo("FAILED");
		verify(repository).insertMessageDeliveryLog(eq(user.businessId()), eq(messageId), eq("PENDING"), eq(null),
				any());
		verify(repository).updateOutboundMessageDelivery(eq(user.businessId()), eq(conversationId), eq(messageId),
				eq("FAILED"), eq(null), any());
		verify(repository).insertMessageDeliveryLog(eq(user.businessId()), eq(messageId), eq("FAILED"), eq(null),
				any());
	}

	@Test
	void sendMessageReturnsExistingMessageForRepeatedIdempotencyKey() {
		AuthenticatedUser user = authenticatedUser();
		UUID conversationId = UUID.randomUUID();
		ConversationMessageResponse existing = message(UUID.randomUUID(), "SENT");
		when(repository.findConversationContext(user.businessId(), conversationId)).thenReturn(context(conversationId));
		when(repository.findOutboundMessageByIdempotencyKey(user.businessId(), conversationId, "retry-1"))
				.thenReturn(Optional.of(existing));

		ConversationMessageResponse response = service.sendMessage(user, conversationId,
				new SendConversationMessageRequest("Hola", null, "retry-1", null));

		assertThat(response).isEqualTo(existing);
		verify(repository, never()).insertOutboundMessage(any(), any(), any(), any(), any(), any(), any(), any());
		verify(channelDispatchService, never()).dispatch(any(ChannelDispatchRequest.class));
	}

	private AuthenticatedUser authenticatedUser() {
		return new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), "Centro Demo", "Ana", "Admin",
				"ana@example.com", "America/Santiago", List.of("ADMIN"), List.of());
	}

	private ConversationJdbcRepository.ConversationContextRecord context(UUID conversationId) {
		return new ConversationJdbcRepository.ConversationContextRecord(conversationId, "WHATSAPP", "OPEN",
				"+56911112222", "Cliente Demo");
	}

	private ConversationMessageResponse message(UUID messageId, String status) {
		return new ConversationMessageResponse(messageId, "OUTBOUND", "TEXT", "Hola", status, null, UUID.randomUUID(),
				"Ana Admin", OffsetDateTime.now(), null, "FAILED".equals(status) ? OffsetDateTime.now() : null,
				OffsetDateTime.now());
	}

	private PlatformTransactionManager immediateTransactionManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) {
				return new SimpleTransactionStatus();
			}

			@Override
			public void commit(TransactionStatus status) {
			}

			@Override
			public void rollback(TransactionStatus status) {
			}
		};
	}
}
