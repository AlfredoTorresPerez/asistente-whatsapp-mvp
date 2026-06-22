package com.asistentewhatsapp.conversations.infrastructure;

import com.asistentewhatsapp.conversations.api.ConversationCustomerResponse;
import com.asistentewhatsapp.conversations.api.ConversationDetailResponse;
import com.asistentewhatsapp.conversations.api.ConversationMessageResponse;
import com.asistentewhatsapp.conversations.api.ConversationMetricsResponse;
import com.asistentewhatsapp.conversations.api.ConversationSummaryResponse;
import com.asistentewhatsapp.conversations.api.ResponseTemplateResponse;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationJdbcRepository {

    private static final Pattern SERIALIZED_WHATSAPP_CHAT_ID = Pattern.compile(
            "(?:true|false)_([^_]+@(?:c\\.us|s\\.whatsapp\\.net|lid|g\\.us))_.*",
            Pattern.CASE_INSENSITIVE);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConversationJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public ConversationMetricsResponse findConversationMetrics(UUID businessId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("businessId", businessId);
        return jdbcTemplate.queryForObject(
                """
                        select
                            (select count(*) from conversation c where c.business_id = :businessId and c.status in ('OPEN', 'PENDING')) as active_conversations,
                            (select count(*) from conversation c where c.business_id = :businessId and c.status <> 'CLOSED' and c.unread_count > 0) as unattended_conversations,
                            (select count(*) from lead l where l.business_id = :businessId and l.active = true and l.created_at >= current_timestamp - interval '7 days') as new_prospects,
                            (select count(*) from order_request o where o.business_id = :businessId and o.status in ('DRAFT', 'CONFIRMED')) as active_orders
                        """,
                parameters,
                (resultSet, rowNum) -> new ConversationMetricsResponse(
                        resultSet.getLong("active_conversations"),
                        resultSet.getLong("unattended_conversations"),
                        resultSet.getLong("new_prospects"),
                        resultSet.getLong("active_orders")));
    }

    public PagedResponse<ConversationSummaryResponse> findConversations(
            UUID businessId,
            int page,
            int size,
            String search,
            String status,
            UUID ownerUserId) {
        QueryParts queryParts = buildConversationListQuery(businessId, search, status, ownerUserId);
        Long totalItems = jdbcTemplate.queryForObject(
                "select count(*) " + queryParts.fromAndWhere(),
                queryParts.parameters(),
                Long.class);
        long resolvedTotalItems = totalItems == null ? 0 : totalItems;
        int totalPages = resolvedTotalItems == 0 ? 0 : (int) Math.ceil((double) resolvedTotalItems / size);

        MapSqlParameterSource parameters = queryParts.parameters()
                .addValue("limit", size)
                .addValue("offset", page * size);

        List<ConversationSummaryResponse> items = jdbcTemplate.query(
                """
                        select
                            c.id,
                            c.customer_name,
                            c.customer_phone,
                            c.status,
                            c.unread_count,
                            c.last_message_preview,
                            c.last_message_at,
                            c.channel_type,
                            c.assigned_user_id,
                            case
                                when ua.id is null then null
                                else concat(ua.first_name, ' ', ua.last_name)
                            end as assigned_user_name,
                            l.id as prospect_id,
                            c.location_id,
                            bl.name as location_name
                        """
                        + queryParts.fromAndWhere()
                        + """
                                order by coalesce(c.last_message_at, c.created_at) desc, c.created_at desc
                                limit :limit
                                offset :offset
                                """,
                parameters,
                conversationSummaryRowMapper());

        return new PagedResponse<>(items, page, size, resolvedTotalItems, totalPages);
    }

    public ConversationDetailResponse findConversationDetail(UUID businessId, UUID conversationId) {
        MapSqlParameterSource parameters = conversationParameters(businessId, conversationId);
        List<ConversationDetailRow> rows = jdbcTemplate.query(
                """
                        select
                            c.id,
                            c.status,
                            c.channel_type,
                            c.unread_count,
                            c.last_message_preview,
                            c.last_message_at,
                            c.opened_at,
                            c.closed_at,
                            c.assigned_user_id,
                            case
                                when ua.id is null then null
                                else concat(ua.first_name, ' ', ua.last_name)
                            end as assigned_user_name,
                            l.id as prospect_id,
                            c.location_id,
                            bl.name as location_name,
                            cu.id as customer_id,
                            cu.first_name as customer_first_name,
                            cu.last_name as customer_last_name,
                            cu.display_name as customer_display_name,
                            cu.phone as customer_phone,
                            cu.email as customer_email
                        from conversation c
                        join customer cu on cu.id = c.customer_id
                        left join user_account ua on ua.id = c.assigned_user_id
                        left join lead l on l.conversation_id = c.id
                          and l.business_id = c.business_id
                          and l.active = true
                        left join business_location bl
                          on bl.id = c.location_id
                         and bl.business_id = c.business_id
                        where c.business_id = :businessId
                          and c.id = :conversationId
                        """,
                parameters,
                conversationDetailRowMapper());

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
        }

        ConversationDetailRow conversation = rows.getFirst();
        List<ConversationMessageResponse> messages = findConversationMessages(businessId, conversationId);
        return new ConversationDetailResponse(
                conversation.id(),
                conversation.status(),
                conversation.channelType(),
                conversation.unreadCount(),
                conversation.lastMessagePreview(),
                conversation.lastMessageAt(),
                conversation.openedAt(),
                conversation.closedAt(),
                conversation.assignedUserId(),
                conversation.assignedUserName(),
                conversation.prospectId(),
                conversation.locationId(),
                conversation.locationName(),
                new ConversationCustomerResponse(
                        conversation.customerId(),
                        conversation.customerFirstName(),
                        conversation.customerLastName(),
                        conversation.customerDisplayName(),
                        conversation.customerPhone(),
                        conversation.customerEmail()),
                messages);
    }

    public ConversationContextRecord findConversationContext(UUID businessId, UUID conversationId) {
        MapSqlParameterSource parameters = conversationParameters(businessId, conversationId);
        List<ConversationContextRecord> rows = jdbcTemplate.query(
                """
                        select
                            c.id,
                            c.channel_type,
                            c.status,
                            c.customer_phone,
                            cu.display_name as customer_display_name
                        from conversation c
                        join customer cu on cu.id = c.customer_id
                        where c.business_id = :businessId
                          and c.id = :conversationId
                        """,
                parameters,
                (resultSet, rowNum) -> new ConversationContextRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("channel_type"),
                        resultSet.getString("status"),
                        resultSet.getString("customer_phone"),
                        resultSet.getString("customer_display_name")));

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
        }

        return rows.getFirst();
    }

    public boolean assignConversationLocationFromMessageIfBlank(
            UUID businessId,
            UUID conversationId,
            String messageBody) {
        if (messageBody == null || messageBody.isBlank()) {
            return false;
        }
        int updated = jdbcTemplate.update(
                """
                        update conversation c
                        set location_id = selected.id,
                            updated_at = current_timestamp
                        from (
                            select bl.id
                            from business_location bl
                            where bl.business_id = :businessId
                              and bl.active = true
                              and (
                                  position(translate(lower(bl.name), 'áéíóúüñ', 'aeiouun') in translate(lower(:messageBody), 'áéíóúüñ', 'aeiouun')) > 0
                                  or position(translate(lower(bl.code), 'áéíóúüñ', 'aeiouun') in translate(lower(:messageBody), 'áéíóúüñ', 'aeiouun')) > 0
                                  or position(translate(lower(coalesce(bl.commune, '')), 'áéíóúüñ', 'aeiouun') in translate(lower(:messageBody), 'áéíóúüñ', 'aeiouun')) > 0
                              )
                            order by
                                case
                                    when position(translate(lower(bl.name), 'áéíóúüñ', 'aeiouun') in translate(lower(:messageBody), 'áéíóúüñ', 'aeiouun')) > 0 then 300
                                    when position(translate(lower(bl.code), 'áéíóúüñ', 'aeiouun') in translate(lower(:messageBody), 'áéíóúüñ', 'aeiouun')) > 0 then 250
                                    when position(translate(lower(coalesce(bl.commune, '')), 'áéíóúüñ', 'aeiouun') in translate(lower(:messageBody), 'áéíóúüñ', 'aeiouun')) > 0 then 100
                                    else 0
                                end desc,
                                case when bl.code in ('providencia', 'maipu', 'santiago-centro') then 1 else 0 end desc,
                                length(bl.name) asc
                            limit 1
                        ) selected
                        where c.business_id = :businessId
                          and c.id = :conversationId
                          and c.location_id is null
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("conversationId", conversationId)
                        .addValue("messageBody", messageBody));
        return updated > 0;
    }

    public Optional<String> findLatestProviderChatId(UUID businessId, UUID conversationId) {
        MapSqlParameterSource parameters = conversationParameters(businessId, conversationId)
                .addValue("limit", 20);
        List<ProviderAddressRow> rows = jdbcTemplate.query(
                """
                        select
                            m.external_message_id,
                            l.provider_payload ->> 'chatId' as chat_id,
                            coalesce(l.occurred_at, m.created_at) as event_at
                        from message m
                        left join message_delivery_log l on l.message_id = m.id
                        where m.business_id = :businessId
                          and m.conversation_id = :conversationId
                          and (
                              m.external_message_id ilike '%@lid%'
                              or m.external_message_id ilike '%@c.us%'
                              or m.external_message_id ilike '%@s.whatsapp.net%'
                              or m.external_message_id ilike '%@g.us%'
                              or jsonb_extract_path_text(l.provider_payload, 'chatId') is not null
                          )
                        order by coalesce(l.occurred_at, m.created_at) desc
                        limit :limit
                        """,
                parameters,
                (resultSet, rowNum) -> new ProviderAddressRow(
                        resultSet.getString("external_message_id"),
                        resultSet.getString("chat_id")));

        for (ProviderAddressRow row : rows) {
            Optional<String> chatId = normalizeProviderChatId(row.chatId());
            if (chatId.isPresent()) {
                return chatId;
            }
            Optional<String> externalIdChatId = extractChatIdFromExternalMessageId(row.externalMessageId());
            if (externalIdChatId.isPresent()) {
                return externalIdChatId;
            }
        }
        return Optional.empty();
    }

    public List<ConversationMessageResponse> findConversationMessages(UUID businessId, UUID conversationId) {
        return jdbcTemplate.query(
                """
                        select
                            m.id,
                            m.direction,
                            m.message_type,
                            m.body,
                            m.status,
                            m.external_message_id,
                            m.sent_by_user_id,
                            case
                                when ua.id is null then null
                                else concat(ua.first_name, ' ', ua.last_name)
                            end as sent_by_user_name,
                            m.sent_at,
                            m.received_at,
                            m.failed_at,
                            m.created_at
                        from message m
                        left join user_account ua on ua.id = m.sent_by_user_id
                        where m.business_id = :businessId
                          and m.conversation_id = :conversationId
                        order by coalesce(m.sent_at, m.received_at, m.created_at) asc, m.created_at asc
                        """,
                conversationParameters(businessId, conversationId),
                conversationMessageRowMapper());
    }

    public ConversationMessageResponse findMessageById(UUID businessId, UUID conversationId, UUID messageId) {
        List<ConversationMessageResponse> rows = jdbcTemplate.query(
                """
                        select
                            m.id,
                            m.direction,
                            m.message_type,
                            m.body,
                            m.status,
                            m.external_message_id,
                            m.sent_by_user_id,
                            case
                                when ua.id is null then null
                                else concat(ua.first_name, ' ', ua.last_name)
                            end as sent_by_user_name,
                            m.sent_at,
                            m.received_at,
                            m.failed_at,
                            m.created_at
                        from message m
                        left join user_account ua on ua.id = m.sent_by_user_id
                        where m.business_id = :businessId
                          and m.conversation_id = :conversationId
                          and m.id = :messageId
                        """,
                conversationParameters(businessId, conversationId).addValue("messageId", messageId),
                conversationMessageRowMapper());

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro el mensaje solicitado.");
        }
        return rows.getFirst();
    }

    public Optional<ConversationMessageResponse> findOutboundMessageByIdempotencyKey(
            UUID businessId,
            UUID conversationId,
            String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        List<ConversationMessageResponse> rows = jdbcTemplate.query(
                """
                        select
                            m.id,
                            m.direction,
                            m.message_type,
                            m.body,
                            m.status,
                            m.external_message_id,
                            m.sent_by_user_id,
                            case
                                when ua.id is null then null
                                else concat(ua.first_name, ' ', ua.last_name)
                            end as sent_by_user_name,
                            m.sent_at,
                            m.received_at,
                            m.failed_at,
                            m.created_at
                        from message m
                        left join user_account ua on ua.id = m.sent_by_user_id
                        where m.business_id = :businessId
                          and m.conversation_id = :conversationId
                          and m.direction = 'OUTBOUND'
                          and m.idempotency_key = :idempotencyKey
                        order by m.created_at desc
                        limit 1
                        """,
                conversationParameters(businessId, conversationId).addValue("idempotencyKey", idempotencyKey),
                conversationMessageRowMapper());

        return rows.stream().findFirst();
    }

    public Optional<ChannelAccountRecord> findPrimaryActiveChannelAccount(UUID businessId) {
        List<ChannelAccountRecord> items = jdbcTemplate.query(
                """
                        select id, channel_type
                        from channel_account
                        where business_id = :businessId
                          and active = true
                        order by created_at asc
                        limit 1
                        """,
                new MapSqlParameterSource().addValue("businessId", businessId),
                (resultSet, rowNum) -> new ChannelAccountRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("channel_type")));
        return items.stream().findFirst();
    }

    public Optional<UUID> findUserId(UUID businessId, UUID userId) {
        List<UUID> items = jdbcTemplate.query(
                """
                        select id
                        from user_account
                        where business_id = :businessId
                          and id = :userId
                          and status = 'ACTIVE'
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("userId", userId),
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class));
        return items.stream().findFirst();
    }

    public CustomerRecord findCustomerById(UUID businessId, UUID customerId) {
        List<CustomerRecord> items = jdbcTemplate.query(
                """
                        select
                            id,
                            first_name,
                            last_name,
                            display_name,
                            phone,
                            email
                        from customer
                        where business_id = :businessId
                          and id = :customerId
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("customerId", customerId),
                customerRowMapper());

        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro el cliente solicitado.");
        }
        return items.getFirst();
    }

    public Optional<CustomerRecord> findCustomerByNormalizedPhone(UUID businessId, String normalizedPhone) {
        List<CustomerRecord> items = jdbcTemplate.query(
                """
                        select
                            id,
                            first_name,
                            last_name,
                            display_name,
                            phone,
                            email
                        from customer
                        where business_id = :businessId
                          and normalized_phone = :normalizedPhone
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("normalizedPhone", normalizedPhone),
                customerRowMapper());
        return items.stream().findFirst();
    }

    public UUID insertCustomer(
            UUID businessId,
            String firstName,
            String lastName,
            String displayName,
            String phone,
            String email) {
        UUID customerId = UUID.randomUUID();
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
                        ) values (
                            :id,
                            :businessId,
                            :firstName,
                            :lastName,
                            :displayName,
                            :phone,
                            :normalizedPhone,
                            :email,
                            true
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", customerId)
                        .addValue("businessId", businessId)
                        .addValue("firstName", firstName)
                        .addValue("lastName", lastName)
                        .addValue("displayName", displayName)
                        .addValue("phone", phone)
                        .addValue("normalizedPhone", phone)
                        .addValue("email", email));
        return customerId;
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
                            last_message_at,
                            last_message_preview,
                            opened_at,
                            closed_at
                        ) values (
                            :id,
                            :businessId,
                            :channelAccountId,
                            :customerId,
                            :assignedUserId,
                            'WHATSAPP',
                            :customerName,
                            :customerPhone,
                            'OPEN',
                            0,
                            null,
                            null,
                            :openedAt,
                            null
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", conversationId)
                        .addValue("businessId", businessId)
                        .addValue("channelAccountId", channelAccountId)
                        .addValue("customerId", customerId)
                        .addValue("assignedUserId", assignedUserId)
                        .addValue("customerName", customerName)
                        .addValue("customerPhone", customerPhone)
                        .addValue("openedAt", openedAt));
        return conversationId;
    }

    public void updateConversationAssignment(
            UUID businessId,
            UUID conversationId,
            UUID assignedUserId,
            OffsetDateTime updatedAt) {
        int updated = jdbcTemplate.update(
                """
                        update conversation
                        set assigned_user_id = :assignedUserId,
                            updated_at = :updatedAt
                        where business_id = :businessId
                          and id = :conversationId
                        """,
                conversationParameters(businessId, conversationId)
                        .addValue("assignedUserId", assignedUserId)
                        .addValue("updatedAt", updatedAt));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
        }
    }


    public void markConversationRead(
            UUID businessId,
            UUID conversationId,
            OffsetDateTime updatedAt) {
        int updated = jdbcTemplate.update(
                """
                        update conversation
                        set unread_count = 0,
                            updated_at = :updatedAt
                        where business_id = :businessId
                          and id = :conversationId
                        """,
                conversationParameters(businessId, conversationId)
                        .addValue("updatedAt", updatedAt));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
        }
    }

    public void updateConversationStatus(
            UUID businessId,
            UUID conversationId,
            String status,
            OffsetDateTime updatedAt) {
        int updated = jdbcTemplate.update(
                """
                        update conversation
                        set status = :status,
                            closed_at = case when :status = 'CLOSED' then cast(:updatedAt as timestamptz) else null end,
                            updated_at = :updatedAt
                        where business_id = :businessId
                          and id = :conversationId
                        """,
                conversationParameters(businessId, conversationId)
                        .addValue("status", status)
                        .addValue("updatedAt", updatedAt));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
        }
    }

    public UUID insertOutboundMessage(
            UUID businessId,
            UUID conversationId,
            UUID sentByUserId,
            String body,
            String status,
            String externalMessageId,
            OffsetDateTime sentAt,
            String idempotencyKey) {
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
                            idempotency_key,
                            sent_at,
                            received_at,
                            failed_at
                        ) values (
                            :id,
                            :businessId,
                            :conversationId,
                            :sentByUserId,
                            'OUTBOUND',
                            'TEXT',
                            :body,
                            :status,
                            :externalMessageId,
                            :idempotencyKey,
                            :sentAt,
                            null,
                            case when :status = 'FAILED' then cast(:sentAt as timestamptz) else null end
                        )
                        """,
                conversationParameters(businessId, conversationId)
                        .addValue("id", messageId)
                        .addValue("sentByUserId", sentByUserId)
                        .addValue("body", body)
                        .addValue("status", status)
                        .addValue("externalMessageId", externalMessageId)
                        .addValue("idempotencyKey", idempotencyKey)
                        .addValue("sentAt", sentAt));
        return messageId;
    }

    public void updateOutboundMessageDelivery(
            UUID businessId,
            UUID conversationId,
            UUID messageId,
            String status,
            String externalMessageId,
            OffsetDateTime occurredAt) {
        int updated = jdbcTemplate.update(
                """
                        update message
                        set status = :status,
                            external_message_id = :externalMessageId,
                            sent_at = case when :status <> 'FAILED' then cast(:occurredAt as timestamptz) else sent_at end,
                            failed_at = case when :status = 'FAILED' then cast(:occurredAt as timestamptz) else null end
                        where business_id = :businessId
                          and conversation_id = :conversationId
                          and id = :messageId
                        """,
                conversationParameters(businessId, conversationId)
                        .addValue("messageId", messageId)
                        .addValue("status", status)
                        .addValue("externalMessageId", externalMessageId)
                        .addValue("occurredAt", occurredAt));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro el mensaje saliente solicitado.");
        }
    }

    public void updateConversationOutboundActivity(
            UUID businessId,
            UUID conversationId,
            String preview,
            OffsetDateTime occurredAt) {
        int updated = jdbcTemplate.update(
                """
                        update conversation
                        set last_message_at = :occurredAt,
                            last_message_preview = :preview,
                            updated_at = :occurredAt
                        where business_id = :businessId
                          and id = :conversationId
                """,
                conversationParameters(businessId, conversationId)
                        .addValue("preview", truncatePreview(preview))
                        .addValue("occurredAt", occurredAt));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro la conversacion solicitada.");
        }
    }

    public void insertMessageDeliveryLog(
            UUID businessId,
            UUID messageId,
            String deliveryStatus,
            String providerEventId,
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
                        ) values (
                            :id,
                            :businessId,
                            :messageId,
                            :deliveryStatus,
                            :providerEventId,
                            '{}'::jsonb,
                            :occurredAt
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("businessId", businessId)
                        .addValue("messageId", messageId)
                        .addValue("deliveryStatus", deliveryStatus)
                        .addValue("providerEventId", providerEventId)
                        .addValue("occurredAt", occurredAt));
    }

    public List<ResponseTemplateResponse> findTemplates(UUID businessId, Boolean active) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("businessId", businessId);
        StringBuilder sql = new StringBuilder(
                """
                        select
                            id,
                            name,
                            category,
                            body,
                            active,
                            created_at,
                            updated_at
                        from response_template
                        where business_id = :businessId
                        """);

        if (active != null) {
            sql.append(" and active = :active ");
            parameters.addValue("active", active);
        }

        sql.append(" order by active desc, name asc ");
        return jdbcTemplate.query(sql.toString(), parameters, templateRowMapper());
    }

    public ResponseTemplateResponse findTemplateById(UUID businessId, UUID templateId) {
        List<ResponseTemplateResponse> items = jdbcTemplate.query(
                """
                        select
                            id,
                            name,
                            category,
                            body,
                            active,
                            created_at,
                            updated_at
                        from response_template
                        where business_id = :businessId
                          and id = :templateId
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("templateId", templateId),
                templateRowMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la plantilla solicitada.");
        }
        return items.getFirst();
    }

    public TemplateRecord findTemplateRecordById(UUID businessId, UUID templateId) {
        List<TemplateRecord> items = jdbcTemplate.query(
                """
                        select id, name, category, body, active
                        from response_template
                        where business_id = :businessId
                          and id = :templateId
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("templateId", templateId),
                (resultSet, rowNum) -> new TemplateRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name"),
                        resultSet.getString("category"),
                        resultSet.getString("body"),
                        resultSet.getBoolean("active")));
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la plantilla solicitada.");
        }
        return items.getFirst();
    }

    public boolean existsTemplateName(UUID businessId, String name, UUID excludeTemplateId) {
        String sql;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("name", name);

        if (excludeTemplateId == null) {
            sql = """
                    select count(*)
                    from response_template
                    where business_id = :businessId
                      and lower(name) = lower(:name)
                    """;
        } else {
            sql = """
                    select count(*)
                    from response_template
                    where business_id = :businessId
                      and lower(name) = lower(:name)
                      and id <> :excludeTemplateId
                    """;
            parameters.addValue("excludeTemplateId", excludeTemplateId);
        }

        Long total = jdbcTemplate.queryForObject(
                sql,
                parameters,
                Long.class);
        return total != null && total > 0;
    }

    public UUID insertTemplate(UUID businessId, String name, String category, String body, boolean active) {
        UUID templateId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into response_template (
                            id,
                            business_id,
                            name,
                            category,
                            body,
                            active
                        ) values (
                            :id,
                            :businessId,
                            :name,
                            :category,
                            :body,
                            :active
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", templateId)
                        .addValue("businessId", businessId)
                        .addValue("name", name)
                        .addValue("category", category)
                        .addValue("body", body)
                        .addValue("active", active));
        return templateId;
    }

    public void updateTemplate(UUID businessId, UUID templateId, String name, String category, String body) {
        int updated = jdbcTemplate.update(
                """
                        update response_template
                        set name = :name,
                            category = :category,
                            body = :body,
                            updated_at = current_timestamp
                        where business_id = :businessId
                          and id = :templateId
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("templateId", templateId)
                        .addValue("name", name)
                        .addValue("category", category)
                        .addValue("body", body));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro la plantilla solicitada.");
        }
    }

    public void updateTemplateStatus(UUID businessId, UUID templateId, boolean active) {
        int updated = jdbcTemplate.update(
                """
                        update response_template
                        set active = :active,
                            updated_at = current_timestamp
                        where business_id = :businessId
                          and id = :templateId
                        """,
                new MapSqlParameterSource()
                        .addValue("businessId", businessId)
                        .addValue("templateId", templateId)
                        .addValue("active", active));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro la plantilla solicitada.");
        }
    }

    private QueryParts buildConversationListQuery(
            UUID businessId,
            String search,
            String status,
            UUID ownerUserId) {
        StringBuilder sql = new StringBuilder(
                """
                        from conversation c
                        left join user_account ua on ua.id = c.assigned_user_id
                        left join lead l on l.conversation_id = c.id
                          and l.business_id = c.business_id
                          and l.active = true
                        left join business_location bl
                          on bl.id = c.location_id
                         and bl.business_id = c.business_id
                        where c.business_id = :businessId
                        """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("businessId", businessId);

        if (search != null) {
            sql.append("""
                     and (
                        c.customer_name ilike :search
                        or c.customer_phone ilike :search
                        or coalesce(bl.name, '') ilike :search
                        or coalesce(c.last_message_preview, '') ilike :search
                     )
                    """);
            parameters.addValue("search", "%" + search + "%");
        }

        if (status != null) {
            sql.append(" and c.status = :status ");
            parameters.addValue("status", status);
        }

        if (ownerUserId != null) {
            sql.append(" and c.assigned_user_id = :ownerUserId ");
            parameters.addValue("ownerUserId", ownerUserId);
        }

        return new QueryParts(sql.toString(), parameters);
    }

    private MapSqlParameterSource conversationParameters(UUID businessId, UUID conversationId) {
        return new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("conversationId", conversationId);
    }

    private Optional<String> normalizeProviderChatId(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.matches("(?i).+@(c\\.us|s\\.whatsapp\\.net|lid|g\\.us)$")) {
            return Optional.of(normalized);
        }
        return Optional.empty();
    }

    private Optional<String> extractChatIdFromExternalMessageId(String externalMessageId) {
        if (externalMessageId == null || externalMessageId.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = SERIALIZED_WHATSAPP_CHAT_ID.matcher(externalMessageId.trim());
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private String truncatePreview(String preview) {
        if (preview == null) {
            return null;
        }
        return preview.length() > 500 ? preview.substring(0, 500) : preview;
    }

    private RowMapper<ConversationSummaryResponse> conversationSummaryRowMapper() {
        return (resultSet, rowNum) -> new ConversationSummaryResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("customer_name"),
                resultSet.getString("customer_phone"),
                resultSet.getString("status"),
                resultSet.getInt("unread_count"),
                resultSet.getString("last_message_preview"),
                resultSet.getObject("last_message_at", OffsetDateTime.class),
                resultSet.getString("channel_type"),
                resultSet.getObject("assigned_user_id", UUID.class),
                resultSet.getString("assigned_user_name"),
                resultSet.getObject("prospect_id", UUID.class),
                resultSet.getObject("location_id", UUID.class),
                resultSet.getString("location_name"));
    }

    private RowMapper<ConversationDetailRow> conversationDetailRowMapper() {
        return (resultSet, rowNum) -> new ConversationDetailRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("status"),
                resultSet.getString("channel_type"),
                resultSet.getInt("unread_count"),
                resultSet.getString("last_message_preview"),
                resultSet.getObject("last_message_at", OffsetDateTime.class),
                resultSet.getObject("opened_at", OffsetDateTime.class),
                resultSet.getObject("closed_at", OffsetDateTime.class),
                resultSet.getObject("assigned_user_id", UUID.class),
                resultSet.getString("assigned_user_name"),
                resultSet.getObject("prospect_id", UUID.class),
                resultSet.getObject("location_id", UUID.class),
                resultSet.getString("location_name"),
                resultSet.getObject("customer_id", UUID.class),
                resultSet.getString("customer_first_name"),
                resultSet.getString("customer_last_name"),
                resultSet.getString("customer_display_name"),
                resultSet.getString("customer_phone"),
                resultSet.getString("customer_email"));
    }

    private RowMapper<ConversationMessageResponse> conversationMessageRowMapper() {
        return (resultSet, rowNum) -> new ConversationMessageResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("direction"),
                resultSet.getString("message_type"),
                resultSet.getString("body"),
                resultSet.getString("status"),
                resultSet.getString("external_message_id"),
                resultSet.getObject("sent_by_user_id", UUID.class),
                resultSet.getString("sent_by_user_name"),
                resultSet.getObject("sent_at", OffsetDateTime.class),
                resultSet.getObject("received_at", OffsetDateTime.class),
                resultSet.getObject("failed_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }

    private RowMapper<ResponseTemplateResponse> templateRowMapper() {
        return (resultSet, rowNum) -> new ResponseTemplateResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                resultSet.getString("category"),
                resultSet.getString("body"),
                resultSet.getBoolean("active"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    private RowMapper<CustomerRecord> customerRowMapper() {
        return (resultSet, rowNum) -> new CustomerRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("display_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"));
    }

    private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
    }

    private record ConversationDetailRow(
            UUID id,
            String status,
            String channelType,
            int unreadCount,
            String lastMessagePreview,
            OffsetDateTime lastMessageAt,
            OffsetDateTime openedAt,
            OffsetDateTime closedAt,
            UUID assignedUserId,
            String assignedUserName,
            UUID prospectId,
            UUID locationId,
            String locationName,
            UUID customerId,
            String customerFirstName,
            String customerLastName,
            String customerDisplayName,
            String customerPhone,
            String customerEmail) {
    }

    public record ChannelAccountRecord(UUID id, String channelType) {
    }

    public record CustomerRecord(
            UUID id,
            String firstName,
            String lastName,
            String displayName,
            String phone,
            String email) {
    }

    public record ConversationContextRecord(
            UUID id,
            String channelType,
            String status,
            String customerPhone,
            String customerDisplayName) {
    }

    private record ProviderAddressRow(String externalMessageId, String chatId) {
    }

    public record TemplateRecord(
            UUID id,
            String name,
            String category,
            String body,
            boolean active) {
    }
}
