create unique index if not exists uq_channel_event_log_session_status_idempotency
    on channel_event_log (
        business_id,
        channel_account_id,
        event_type,
        coalesce(payload ->> 'sessionKey', ''),
        coalesce(payload ->> 'occurredAt', ''),
        coalesce(payload #>> '{payload,connectionStatus}', payload #>> '{payload,status}', payload #>> '{payload,sessionStatus}', '')
    )
    where event_type = 'SESSION_STATUS_CHANGED';
