package com.asistentewhatsapp.channels.infrastructure.whatsappweb;

import com.asistentewhatsapp.administration.api.WhatsAppWebRecentEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WhatsAppWebChannelJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public WhatsAppWebChannelJdbcRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<ChannelAccountRecord> findChannelAccountByBusinessId(UUID businessId) {
        List<ChannelAccountRecord> items = jdbcTemplate.query(
                """
                        select id, business_id, session_key, status, phone_number, last_qr_code, last_event_at
                        from channel_account
                        where business_id = ?
                          and channel_type = 'WHATSAPP'
                        order by created_at asc
                        limit 1
                        """,
                new ChannelAccountRowMapper(),
                businessId);
        return items.stream().findFirst();
    }

    public Optional<ChannelAccountRecord> findChannelAccountBySessionKey(String sessionKey) {
        List<ChannelAccountRecord> items = jdbcTemplate.query(
                """
                        select id, business_id, session_key, status, phone_number, last_qr_code, last_event_at
                        from channel_account
                        where session_key = ?
                        limit 1
                        """,
                new ChannelAccountRowMapper(),
                sessionKey);
        return items.stream().findFirst();
    }

    public void updateChannelAccount(
            UUID channelAccountId,
            String status,
            String phoneNumber,
            String qrCode,
            OffsetDateTime lastEventAt) {
        jdbcTemplate.update(
                """
                        update channel_account
                        set status = ?,
                            phone_number = ?,
                            last_qr_code = ?,
                            last_event_at = ?,
                            connected_at = case when ? = 'CONNECTED' then coalesce(connected_at, cast(? as timestamptz)) else connected_at end,
                            disconnected_at = case when ? = 'DISCONNECTED' then cast(? as timestamptz) else disconnected_at end,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                status,
                phoneNumber,
                qrCode,
                lastEventAt,
                status,
                lastEventAt,
                status,
                "DISCONNECTED".equals(status) ? lastEventAt : null,
                channelAccountId);
    }

    public boolean insertChannelEventLog(
            UUID businessId,
            UUID channelAccountId,
            String deliveryId,
            String eventType,
            String payloadJson,
            OffsetDateTime receivedAt) {
        try {
            int inserted = jdbcTemplate.update(
                    """
                            insert into channel_event_log (
                                id,
                                business_id,
                                channel_account_id,
                                delivery_id,
                                event_type,
                                payload,
                                received_at,
                                processing_status
                            ) values (?, ?, ?, ?, ?, cast(? as jsonb), ?, 'RECEIVED')
                            on conflict do nothing
                            """,
                    UUID.randomUUID(),
                    businessId,
                    channelAccountId,
                    deliveryId,
                    eventType,
                    payloadJson,
                    receivedAt);
            return inserted > 0;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public void markChannelEventProcessed(
            UUID businessId,
            String deliveryId,
            String processingStatus,
            OffsetDateTime processedAt) {
        jdbcTemplate.update(
                """
                        update channel_event_log
                        set processing_status = ?,
                            processed_at = ?,
                            updated_at = current_timestamp
                        where business_id = ?
                          and delivery_id = ?
                        """,
                processingStatus,
                processedAt,
                businessId,
                deliveryId);
    }

    public List<WhatsAppWebRecentEventResponse> findRecentEvents(UUID businessId, int limit) {
        return jdbcTemplate.query(
                """
                        select delivery_id, event_type, processing_status, received_at, processed_at
                        from channel_event_log
                        where business_id = ?
                        order by received_at desc
                        limit ?
                        """,
                (resultSet, rowNum) -> new WhatsAppWebRecentEventResponse(
                        resultSet.getString("delivery_id"),
                        resultSet.getString("event_type"),
                        resultSet.getString("processing_status"),
                        resultSet.getObject("received_at", OffsetDateTime.class),
                        resultSet.getObject("processed_at", OffsetDateTime.class)),
                businessId,
                limit);
    }

    public Optional<CustomerRecord> findCustomerByPhone(UUID businessId, String normalizedPhone) {
        List<CustomerRecord> items = jdbcTemplate.query(
                """
                        select id, display_name, phone, normalized_phone
                        from customer
                        where business_id = ?
                          and normalized_phone = ?
                        limit 1
                        """,
                (resultSet, rowNum) -> new CustomerRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("display_name"),
                        resultSet.getString("phone"),
                        resultSet.getString("normalized_phone")),
                businessId,
                normalizedPhone);
        return items.stream().findFirst();
    }

    public UUID insertCustomer(
            UUID businessId,
            String normalizedPhone,
            String displayName) {
        UUID customerId = UUID.randomUUID();
        String firstName = extractFirstName(displayName);
        String lastName = extractLastName(displayName);
        jdbcTemplate.update(
                """
                        insert into customer (
                            id,
                            business_id,
                            first_name,
                            last_name,
                            display_name,
                            phone,
                            normalized_phone,
                            email,
                            active
                        ) values (?, ?, ?, ?, ?, ?, ?, null, true)
                        """,
                customerId,
                businessId,
                firstName,
                lastName,
                displayName,
                normalizedPhone,
                normalizedPhone);
        return customerId;
    }

    public Optional<ConversationRecord> findLatestConversation(
            UUID businessId,
            UUID channelAccountId,
            UUID customerId) {
        List<ConversationRecord> items = jdbcTemplate.query(
                """
                        select c.id, c.assigned_user_id, c.unread_count, c.location_id, bl.name as location_name
                        from conversation c
                        left join business_location bl
                          on bl.id = c.location_id
                         and bl.business_id = c.business_id
                        where c.business_id = ?
                          and c.channel_account_id = ?
                          and c.customer_id = ?
                        order by c.updated_at desc
                        limit 1
                        """,
                conversationRowMapper(),
                businessId,
                channelAccountId,
                customerId);
        return items.stream().findFirst();
    }

    public Optional<ConversationRecord> findConversationById(UUID businessId, UUID conversationId) {
        List<ConversationRecord> items = jdbcTemplate.query(
                """
                        select c.id, c.assigned_user_id, c.unread_count, c.location_id, bl.name as location_name
                        from conversation c
                        left join business_location bl
                          on bl.id = c.location_id
                         and bl.business_id = c.business_id
                        where c.business_id = ?
                          and c.id = ?
                        limit 1
                        """,
                conversationRowMapper(),
                businessId,
                conversationId);
        return items.stream().findFirst();
    }

    public Optional<ConversationRecord> assignConversationLocationFromMessageIfBlank(
            UUID businessId,
            UUID conversationId,
            String messageBody) {
        if (messageBody == null || messageBody.isBlank()) {
            return Optional.empty();
        }
        int updated = jdbcTemplate.update(
                """
                        update conversation c
                        set location_id = selected.id,
                            updated_at = current_timestamp
                        from (
                            select bl.id
                            from business_location bl
                            where bl.business_id = ?
                              and bl.active = true
                              and (
                                  position(translate(lower(bl.name), 'áéíóúüñ', 'aeiouun') in translate(lower(?), 'áéíóúüñ', 'aeiouun')) > 0
                                  or position(translate(lower(bl.code), 'áéíóúüñ', 'aeiouun') in translate(lower(?), 'áéíóúüñ', 'aeiouun')) > 0
                                  or position(translate(lower(coalesce(bl.commune, '')), 'áéíóúüñ', 'aeiouun') in translate(lower(?), 'áéíóúüñ', 'aeiouun')) > 0
                              )
                            order by length(bl.name) desc
                            limit 1
                        ) selected
                        where c.business_id = ?
                          and c.id = ?
                          and c.location_id is null
                        """,
                businessId,
                messageBody,
                messageBody,
                messageBody,
                businessId,
                conversationId);
        if (updated == 0) {
            return Optional.empty();
        }
        return findConversationById(businessId, conversationId);
    }

    private RowMapper<ConversationRecord> conversationRowMapper() {
        return (resultSet, rowNum) -> new ConversationRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("assigned_user_id", UUID.class),
                resultSet.getInt("unread_count"),
                resultSet.getObject("location_id", UUID.class),
                resultSet.getString("location_name"));
    }

    public UUID insertConversation(
            UUID businessId,
            UUID channelAccountId,
            UUID customerId,
            UUID assignedUserId,
            String customerName,
            String customerPhone,
            OffsetDateTime openedAt) {
        UUID conversationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into conversation (
                            id,
                            business_id,
                            channel_account_id,
                            customer_id,
                            assigned_user_id,
                            channel_type,
                            customer_name,
                            customer_phone,
                            status,
                            unread_count,
                            opened_at,
                            last_message_at,
                            last_message_preview
                        ) values (?, ?, ?, ?, ?, 'WHATSAPP', ?, ?, 'OPEN', 0, ?, ?, ?)
                        """,
                conversationId,
                businessId,
                channelAccountId,
                customerId,
                assignedUserId,
                customerName,
                customerPhone,
                openedAt,
                openedAt,
                null);
        return conversationId;
    }

    public UUID insertInboundMessage(
            UUID businessId,
            UUID conversationId,
            String body,
            String externalMessageId,
            String providerEventId,
            OffsetDateTime receivedAt) {
        UUID messageId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into message (
                            id,
                            business_id,
                            conversation_id,
                            sent_by_user_id,
                            direction,
                            message_type,
                            body,
                            status,
                            external_message_id,
                            provider_event_id,
                            sent_at,
                            received_at
                        ) values (?, ?, ?, null, 'INBOUND', 'TEXT', ?, 'RECEIVED', ?, ?, ?, ?)
                        """,
                messageId,
                businessId,
                conversationId,
                body,
                externalMessageId,
                providerEventId,
                receivedAt,
                receivedAt);
        return messageId;
    }

    public UUID insertOutboundMessage(
            UUID businessId,
            UUID conversationId,
            UUID sentByUserId,
            String body,
            OffsetDateTime createdAt) {
        UUID messageId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into message (
                            id,
                            business_id,
                            conversation_id,
                            sent_by_user_id,
                            direction,
                            message_type,
                            body,
                            status,
                            sent_at
                        ) values (?, ?, ?, ?, 'OUTBOUND', 'TEXT', ?, 'QUEUED', ?)
                        """,
                messageId,
                businessId,
                conversationId,
                sentByUserId,
                body,
                createdAt);
        return messageId;
    }


    public UUID insertExternalOutboundMessage(
            UUID businessId,
            UUID conversationId,
            String body,
            String externalMessageId,
            String providerEventId,
            OffsetDateTime sentAt) {
        UUID messageId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into message (
                            id,
                            business_id,
                            conversation_id,
                            sent_by_user_id,
                            direction,
                            message_type,
                            body,
                            status,
                            external_message_id,
                            provider_event_id,
                            sent_at
                        ) values (?, ?, ?, null, 'OUTBOUND', 'TEXT', ?, 'SENT', ?, ?, ?)
                        """,
                messageId,
                businessId,
                conversationId,
                body,
                externalMessageId,
                providerEventId,
                sentAt);
        return messageId;
    }

    public void updateOutboundMessageAccepted(
            UUID messageId,
            String externalMessageId,
            String deliveryStatus,
            OffsetDateTime sentAt) {
        jdbcTemplate.update(
                """
                        update message
                        set external_message_id = ?,
                            status = ?,
                            sent_at = ?,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                externalMessageId,
                deliveryStatus,
                sentAt,
                messageId);
    }

    public void updateOutboundMessageFailed(UUID messageId, String errorCode, OffsetDateTime failedAt) {
        jdbcTemplate.update(
                """
                        update message
                        set status = 'FAILED',
                            error_code = ?,
                            failed_at = ?,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                errorCode,
                failedAt,
                messageId);
    }

    public void updateConversationInboundActivity(
            UUID conversationId,
            String preview,
            OffsetDateTime lastMessageAt) {
        jdbcTemplate.update(
                """
                        update conversation
                        set status = 'OPEN',
                            unread_count = unread_count + 1,
                            last_message_at = ?,
                            last_message_preview = ?,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                lastMessageAt,
                truncatePreview(preview),
                conversationId);
    }

    public void updateConversationOutboundActivity(
            UUID conversationId,
            String preview,
            OffsetDateTime lastMessageAt) {
        jdbcTemplate.update(
                """
                        update conversation
                        set last_message_at = ?,
                            last_message_preview = ?,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                lastMessageAt,
                truncatePreview(preview),
                conversationId);
    }

    public Optional<UUID> findFirstActiveUserId(UUID businessId) {
        List<UUID> items = jdbcTemplate.query(
                """
                        select id
                        from user_account
                        where business_id = ?
                          and status = 'ACTIVE'
                        order by created_at asc
                        limit 1
                        """,
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                businessId);
        return items.stream().findFirst();
    }

    public void insertNotification(
            UUID businessId,
            UUID userId,
            String title,
            String body,
            UUID conversationId) {
        jdbcTemplate.update(
                """
                        insert into notification (
                            id,
                            business_id,
                            user_id,
                            type,
                            status,
                            title,
                            body,
                            related_entity_type,
                            related_entity_id,
                            read_at
                        ) values (?, ?, ?, 'NEW_MESSAGE', 'UNREAD', ?, ?, 'CONVERSATION', ?, null)
                        """,
                UUID.randomUUID(),
                businessId,
                userId,
                title,
                body,
                conversationId);
    }

    public Optional<UUID> findMessageIdByExternalMessageId(UUID businessId, String externalMessageId) {
        if (externalMessageId == null || externalMessageId.isBlank()) {
            return Optional.empty();
        }
        List<UUID> items = jdbcTemplate.query(
                """
                        select id
                        from message
                        where business_id = ?
                          and external_message_id = ?
                        limit 1
                        """,
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                businessId,
                externalMessageId);
        return items.stream().findFirst();
    }

    public void updateMessageStatus(
            UUID messageId,
            String status,
            String providerEventId,
            OffsetDateTime occurredAt) {
        jdbcTemplate.update(
                """
                        update message
                        set status = ?,
                            provider_event_id = ?,
                            sent_at = case when ? in ('SENT', 'DELIVERED', 'READ') then coalesce(sent_at, cast(? as timestamptz)) else sent_at end,
                            failed_at = case when ? = 'FAILED' then cast(? as timestamptz) else failed_at end,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                status,
                providerEventId,
                status,
                occurredAt,
                status,
                "FAILED".equals(status) ? occurredAt : null,
                messageId);
    }

    public void insertMessageDeliveryLog(
            UUID businessId,
            UUID messageId,
            String deliveryStatus,
            String providerEventId,
            Object providerPayload,
            OffsetDateTime occurredAt) {
        jdbcTemplate.update(
                """
                        insert into message_delivery_log (
                            id,
                            business_id,
                            message_id,
                            delivery_status,
                            provider_event_id,
                            provider_payload,
                            occurred_at
                        ) values (?, ?, ?, ?, ?, cast(? as jsonb), ?)
                        """,
                UUID.randomUUID(),
                businessId,
                messageId,
                deliveryStatus,
                providerEventId,
                toJson(providerPayload),
                occurredAt);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? java.util.Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el payload WhatsApp Web.", exception);
        }
    }

    private String extractFirstName(String displayName) {
        String sanitized = displayName == null || displayName.isBlank() ? "Contacto" : displayName.trim();
        String[] tokens = sanitized.split("\\s+");
        return tokens[0];
    }

    private String extractLastName(String displayName) {
        String sanitized = displayName == null || displayName.isBlank() ? "WhatsApp Web" : displayName.trim();
        String[] tokens = sanitized.split("\\s+");
        return tokens.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, tokens.length)) : "WhatsApp Web";
    }

    private String truncatePreview(String preview) {
        if (preview == null) {
            return null;
        }
        return preview.length() > 500 ? preview.substring(0, 500) : preview;
    }

    public record ChannelAccountRecord(
            UUID id,
            UUID businessId,
            String sessionKey,
            String status,
            String phoneNumber,
            String lastQrCode,
            OffsetDateTime lastEventAt) {
    }

    public record CustomerRecord(
            UUID id,
            String displayName,
            String phone,
            String normalizedPhone) {
    }

    public record ConversationRecord(
            UUID id,
            UUID assignedUserId,
            int unreadCount,
            UUID locationId,
            String locationName) {
    }

    private static class ChannelAccountRowMapper implements RowMapper<ChannelAccountRecord> {

        @Override
        public ChannelAccountRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new ChannelAccountRecord(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("business_id", UUID.class),
                    resultSet.getString("session_key"),
                    resultSet.getString("status"),
                    resultSet.getString("phone_number"),
                    resultSet.getString("last_qr_code"),
                    resultSet.getObject("last_event_at", OffsetDateTime.class));
        }
    }
}
