package com.asistentewhatsapp.aiagents.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiReplyOutboxJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public AiReplyOutboxJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean enqueue(InboundAiReplyJob job, OffsetDateTime nextAttemptAt, int maxAttempts) {
        try {
            jdbcTemplate.update(
                    """
                            insert into ai_reply_outbox (
                                id,
                                business_id,
                                channel_account_id,
                                conversation_id,
                                customer_id,
                                inbound_message_id,
                                recipient_phone,
                                customer_display_name,
                                message_body,
                                location_id,
                                location_name,
                                trace_id,
                                status,
                                attempts,
                                max_attempts,
                                next_attempt_at
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?, ?)
                            """,
                    UUID.randomUUID(),
                    job.businessId(),
                    job.channelAccountId(),
                    job.conversationId(),
                    job.customerId(),
                    job.inboundMessageId(),
                    job.recipientPhone(),
                    job.customerDisplayName(),
                    job.messageBody(),
                    job.locationId(),
                    job.locationName(),
                    job.traceId(),
                    maxAttempts,
                    nextAttemptAt);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public List<AiReplyOutboxJob> claimDueJobs(int limit, long processingTimeoutMs) {
        return jdbcTemplate.query(
                """
                        with candidate as (
                            select id
                            from ai_reply_outbox
                            where (
                                    status = 'PENDING'
                                    and next_attempt_at <= current_timestamp
                                  )
                               or (
                                    status = 'PROCESSING'
                                    and locked_at < current_timestamp - (? * interval '1 millisecond')
                                  )
                            order by next_attempt_at asc, created_at asc
                            limit ?
                            for update skip locked
                        )
                        update ai_reply_outbox q
                        set status = 'PROCESSING',
                            attempts = q.attempts + 1,
                            locked_at = current_timestamp,
                            updated_at = current_timestamp
                        from candidate
                        where q.id = candidate.id
                        returning q.id,
                                  q.business_id,
                                  q.channel_account_id,
                                  q.conversation_id,
                                  q.customer_id,
                                  q.inbound_message_id,
                                  q.recipient_phone,
                                  q.customer_display_name,
                                  q.message_body,
                                  q.location_id,
                                  q.location_name,
                                  q.trace_id,
                                  q.attempts,
                                  q.max_attempts
                        """,
                (resultSet, rowNum) -> new AiReplyOutboxJob(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("business_id", UUID.class),
                        resultSet.getObject("channel_account_id", UUID.class),
                        resultSet.getObject("conversation_id", UUID.class),
                        resultSet.getObject("customer_id", UUID.class),
                        resultSet.getObject("inbound_message_id", UUID.class),
                        resultSet.getString("recipient_phone"),
                        resultSet.getString("customer_display_name"),
                        resultSet.getString("message_body"),
                        resultSet.getObject("location_id", UUID.class),
                        resultSet.getString("location_name"),
                        resultSet.getString("trace_id"),
                        resultSet.getInt("attempts"),
                        resultSet.getInt("max_attempts")),
                processingTimeoutMs,
                limit);
    }

    public void markProcessed(UUID outboxId, OffsetDateTime processedAt) {
        jdbcTemplate.update(
                """
                        update ai_reply_outbox
                        set status = 'PROCESSED',
                            processed_at = ?,
                            locked_at = null,
                            last_error_code = null,
                            last_error_message = null,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                processedAt,
                outboxId);
    }

    public void markSkipped(UUID outboxId, String reason, OffsetDateTime processedAt) {
        jdbcTemplate.update(
                """
                        update ai_reply_outbox
                        set status = 'SKIPPED',
                            processed_at = ?,
                            locked_at = null,
                            last_error_code = ?,
                            last_error_message = ?,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                processedAt,
                reason,
                reason,
                outboxId);
    }

    public void markFailedOrRetry(UUID outboxId, int attempts, int maxAttempts, String errorCode, String errorMessage, OffsetDateTime nextAttemptAt) {
        String nextStatus = attempts >= maxAttempts ? "FAILED" : "PENDING";
        jdbcTemplate.update(
                """
                        update ai_reply_outbox
                        set status = ?,
                            next_attempt_at = ?,
                            locked_at = null,
                            last_error_code = ?,
                            last_error_message = ?,
                            updated_at = current_timestamp
                        where id = ?
                        """,
                nextStatus,
                nextAttemptAt,
                errorCode,
                truncate(errorMessage, 4000),
                outboxId);
    }

    public Optional<AiReplyOutboxJob> findById(UUID outboxId) {
        List<AiReplyOutboxJob> items = jdbcTemplate.query(
                """
                        select id,
                               business_id,
                               channel_account_id,
                               conversation_id,
                               customer_id,
                               inbound_message_id,
                               recipient_phone,
                               customer_display_name,
                               message_body,
                               location_id,
                               location_name,
                               trace_id,
                               attempts,
                               max_attempts
                        from ai_reply_outbox
                        where id = ?
                        limit 1
                        """,
                (resultSet, rowNum) -> new AiReplyOutboxJob(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("business_id", UUID.class),
                        resultSet.getObject("channel_account_id", UUID.class),
                        resultSet.getObject("conversation_id", UUID.class),
                        resultSet.getObject("customer_id", UUID.class),
                        resultSet.getObject("inbound_message_id", UUID.class),
                        resultSet.getString("recipient_phone"),
                        resultSet.getString("customer_display_name"),
                        resultSet.getString("message_body"),
                        resultSet.getObject("location_id", UUID.class),
                        resultSet.getString("location_name"),
                        resultSet.getString("trace_id"),
                        resultSet.getInt("attempts"),
                        resultSet.getInt("max_attempts")),
                outboxId);
        return items.stream().findFirst();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    public record InboundAiReplyJob(
            UUID businessId,
            UUID channelAccountId,
            UUID conversationId,
            UUID customerId,
            UUID inboundMessageId,
            String recipientPhone,
            String customerDisplayName,
            String messageBody,
            UUID locationId,
            String locationName,
            String traceId) {
    }

    public record AiReplyOutboxJob(
            UUID id,
            UUID businessId,
            UUID channelAccountId,
            UUID conversationId,
            UUID customerId,
            UUID inboundMessageId,
            String recipientPhone,
            String customerDisplayName,
            String messageBody,
            UUID locationId,
            String locationName,
            String traceId,
            int attempts,
            int maxAttempts) {
    }
}
