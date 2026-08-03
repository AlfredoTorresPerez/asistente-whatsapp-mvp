create index if not exists idx_customer_reports_business_normalized_phone
    on customer (business_id, normalized_phone)
    where normalized_phone is not null;

create index if not exists idx_booking_reminder_reports_pending
    on booking_reminder (business_id, status, scheduled_at)
    where status in ('PENDING', 'FAILED');
