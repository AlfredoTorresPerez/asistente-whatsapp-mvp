create index idx_security_policy_business_id
    on security_policy (business_id);

create index idx_user_account_business_status_created_at
    on user_account (business_id, status, created_at desc);

create index idx_user_account_last_login_at
    on user_account (last_login_at desc);

create index idx_user_role_business_id
    on user_role (business_id);

create index idx_user_role_user_id
    on user_role (user_id);

create index idx_user_role_role_id
    on user_role (role_id);

create index idx_role_permission_role_id
    on role_permission (role_id);

create index idx_role_permission_permission_id
    on role_permission (permission_id);

create index idx_customer_business_created_at
    on customer (business_id, created_at desc);

create index idx_customer_active
    on customer (active);

create index idx_response_template_business_created_at
    on response_template (business_id, created_at desc);

create index idx_response_template_active
    on response_template (active);

create index idx_channel_account_business_status_created_at
    on channel_account (business_id, status, created_at desc);

create index idx_channel_account_active
    on channel_account (active);

create index idx_conversation_business_status_created_at
    on conversation (business_id, status, created_at desc);

create index idx_conversation_channel_account_id
    on conversation (channel_account_id);

create index idx_conversation_customer_id
    on conversation (customer_id);

create index idx_conversation_assigned_user_id
    on conversation (assigned_user_id);

create index idx_conversation_last_message_at
    on conversation (last_message_at desc);

create index idx_message_business_status_created_at
    on message (business_id, status, created_at desc);

create index idx_message_conversation_id
    on message (conversation_id);

create index idx_message_sent_by_user_id
    on message (sent_by_user_id);

create index idx_message_received_at
    on message (received_at desc);

create index idx_lead_business_stage_created_at
    on lead (business_id, stage, created_at desc);

create index idx_lead_customer_id
    on lead (customer_id);

create index idx_lead_conversation_id
    on lead (conversation_id);

create index idx_lead_assigned_user_id
    on lead (assigned_user_id);

create index idx_lead_active
    on lead (active);

create unique index idx_lead_business_normalized_phone_active
    on lead (business_id, normalized_phone)
    where active = true;

create index idx_lead_note_business_created_at
    on lead_note (business_id, created_at desc);

create index idx_lead_note_lead_id
    on lead_note (lead_id);

create index idx_lead_note_author_user_id
    on lead_note (author_user_id);

create index idx_booking_business_status_created_at
    on booking (business_id, status, created_at desc);

create index idx_booking_customer_id
    on booking (customer_id);

create index idx_booking_lead_id
    on booking (lead_id);

create index idx_booking_conversation_id
    on booking (conversation_id);

create index idx_booking_assigned_user_id
    on booking (assigned_user_id);

create index idx_booking_starts_at
    on booking (starts_at);

create index idx_product_category_business_created_at
    on product_category (business_id, created_at desc);

create index idx_product_category_active
    on product_category (active);

create index idx_product_service_business_created_at
    on product_service (business_id, created_at desc);

create index idx_product_service_category_id
    on product_service (category_id);

create index idx_product_service_active
    on product_service (active);

create index idx_order_request_business_status_created_at
    on order_request (business_id, status, created_at desc);

create index idx_order_request_customer_id
    on order_request (customer_id);

create index idx_order_request_lead_id
    on order_request (lead_id);

create index idx_order_request_conversation_id
    on order_request (conversation_id);

create index idx_order_request_created_by_user_id
    on order_request (created_by_user_id);

create index idx_order_request_payment_status
    on order_request (payment_status);

create index idx_order_item_business_created_at
    on order_item (business_id, created_at desc);

create index idx_order_item_order_request_id
    on order_item (order_request_id);

create index idx_order_item_product_service_id
    on order_item (product_service_id);

create index idx_payment_business_created_at
    on payment (business_id, created_at desc);

create index idx_payment_order_request_id
    on payment (order_request_id);

create index idx_payment_paid_at
    on payment (paid_at desc);

create index idx_automation_rule_business_created_at
    on automation_rule (business_id, created_at desc);

create index idx_automation_rule_response_template_id
    on automation_rule (response_template_id);

create index idx_automation_rule_assigned_user_id
    on automation_rule (assigned_user_id);

create index idx_automation_rule_active
    on automation_rule (active);

create index idx_automation_execution_business_status_executed_at
    on automation_execution (business_id, status, executed_at desc);

create index idx_automation_execution_rule_id
    on automation_execution (rule_id);

create index idx_automation_execution_conversation_id
    on automation_execution (conversation_id);

create index idx_channel_event_log_business_processing_status_received_at
    on channel_event_log (business_id, processing_status, received_at desc);

create index idx_channel_event_log_channel_account_id
    on channel_event_log (channel_account_id);

create index idx_message_delivery_log_business_status_created_at
    on message_delivery_log (business_id, delivery_status, created_at desc);

create index idx_message_delivery_log_message_id
    on message_delivery_log (message_id);

create index idx_notification_business_status_created_at
    on notification (business_id, status, created_at desc);

create index idx_notification_user_id
    on notification (user_id);

create index idx_audit_log_business_occurred_at
    on audit_log (business_id, occurred_at desc);

create index idx_audit_log_actor_user_id
    on audit_log (actor_user_id);

create index idx_audit_log_entity_type_entity_id
    on audit_log (entity_type, entity_id);

create index idx_password_reset_token_business_created_at
    on password_reset_token (business_id, created_at desc);

create index idx_password_reset_token_user_id
    on password_reset_token (user_id);

create index idx_password_reset_token_expires_at
    on password_reset_token (expires_at);
