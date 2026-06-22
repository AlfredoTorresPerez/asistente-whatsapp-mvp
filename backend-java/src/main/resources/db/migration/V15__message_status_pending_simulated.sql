alter table message
    drop constraint if exists chk_message_status;

alter table message
    add constraint chk_message_status
        check (status in ('RECEIVED', 'PENDING', 'QUEUED', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'SIMULATED', 'DRY_RUN'));

alter table message_delivery_log
    drop constraint if exists chk_message_delivery_log_status;

alter table message_delivery_log
    add constraint chk_message_delivery_log_status
        check (delivery_status in ('PENDING', 'QUEUED', 'PROVIDER_ACCEPTED', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'SIMULATED', 'DRY_RUN'));
