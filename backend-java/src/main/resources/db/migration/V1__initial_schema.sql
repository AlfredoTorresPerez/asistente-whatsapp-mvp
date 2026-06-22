create table business (
    id uuid primary key,
    code varchar(50) not null unique,
    company_name varchar(150) not null,
    business_name varchar(150) not null,
    timezone varchar(60) not null,
    currency varchar(3) not null,
    contact_email varchar(255) not null,
    support_phone varchar(30),
    address varchar(255),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table security_policy (
    id uuid primary key,
    business_id uuid not null unique,
    session_timeout_minutes integer not null,
    password_min_length integer not null,
    require_uppercase boolean not null,
    require_number boolean not null,
    require_symbol boolean not null,
    max_failed_login_attempts integer not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_security_policy_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint chk_security_policy_session_timeout
        check (session_timeout_minutes between 5 and 1440),
    constraint chk_security_policy_password_length
        check (password_min_length between 8 and 72),
    constraint chk_security_policy_failed_logins
        check (max_failed_login_attempts between 3 and 20)
);

create table role (
    id uuid primary key,
    code varchar(40) not null unique,
    name varchar(80) not null,
    description varchar(255),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table permission (
    id uuid primary key,
    code varchar(80) not null unique,
    name varchar(120) not null,
    module_name varchar(80) not null,
    description varchar(255),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table user_account (
    id uuid primary key,
    business_id uuid not null,
    first_name varchar(80) not null,
    last_name varchar(80) not null,
    email varchar(255) not null,
    phone varchar(30),
    password_hash varchar(255) not null,
    timezone varchar(60) not null,
    status varchar(20) not null default 'ACTIVE',
    last_login_at timestamp with time zone,
    failed_login_attempts integer not null default 0,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_user_account_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_user_account_business_email
        unique (business_id, email),
    constraint chk_user_account_status
        check (status in ('ACTIVE', 'INACTIVE', 'LOCKED')),
    constraint chk_user_account_failed_login_attempts
        check (failed_login_attempts >= 0)
);

create table user_role (
    id uuid primary key,
    business_id uuid not null,
    user_id uuid not null,
    role_id uuid not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_user_role_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_user_role_user
        foreign key (user_id) references user_account (id) on delete cascade,
    constraint fk_user_role_role
        foreign key (role_id) references role (id) on delete cascade,
    constraint uq_user_role_user_role
        unique (user_id, role_id)
);

create table role_permission (
    id uuid primary key,
    role_id uuid not null,
    permission_id uuid not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_role_permission_role
        foreign key (role_id) references role (id) on delete cascade,
    constraint fk_role_permission_permission
        foreign key (permission_id) references permission (id) on delete cascade,
    constraint uq_role_permission_role_permission
        unique (role_id, permission_id)
);

create table customer (
    id uuid primary key,
    business_id uuid not null,
    first_name varchar(80) not null,
    last_name varchar(80) not null,
    display_name varchar(160) not null,
    phone varchar(30) not null,
    normalized_phone varchar(30) not null,
    email varchar(255),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_customer_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_customer_business_normalized_phone
        unique (business_id, normalized_phone)
);

create table response_template (
    id uuid primary key,
    business_id uuid not null,
    name varchar(120) not null,
    category varchar(50) not null,
    body text not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_response_template_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_response_template_business_name
        unique (business_id, name)
);

create table channel_account (
    id uuid primary key,
    business_id uuid not null,
    channel_type varchar(30) not null,
    provider_name varchar(30) not null,
    session_key varchar(80) not null,
    status varchar(30) not null,
    phone_number varchar(30),
    last_qr_code text,
    last_event_at timestamp with time zone,
    connected_at timestamp with time zone,
    disconnected_at timestamp with time zone,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_channel_account_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_channel_account_business_channel
        unique (business_id, channel_type),
    constraint uq_channel_account_session_key
        unique (session_key),
    constraint chk_channel_account_channel_type
        check (channel_type = 'WHATSAPP'),
    constraint chk_channel_account_provider_name
        check (provider_name = 'WHATSAPP_WEB'),
    constraint chk_channel_account_status
        check (status in ('DISCONNECTED', 'QR_PENDING', 'SYNCING', 'CONNECTED', 'ERROR'))
);

create table conversation (
    id uuid primary key,
    business_id uuid not null,
    channel_account_id uuid not null,
    customer_id uuid not null,
    assigned_user_id uuid,
    channel_type varchar(30) not null,
    customer_name varchar(160) not null,
    customer_phone varchar(30) not null,
    status varchar(20) not null,
    unread_count integer not null default 0,
    last_message_at timestamp with time zone,
    last_message_preview varchar(500),
    opened_at timestamp with time zone,
    closed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_conversation_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_conversation_channel_account
        foreign key (channel_account_id) references channel_account (id) on delete restrict,
    constraint fk_conversation_customer
        foreign key (customer_id) references customer (id) on delete restrict,
    constraint fk_conversation_assigned_user
        foreign key (assigned_user_id) references user_account (id) on delete set null,
    constraint chk_conversation_channel_type
        check (channel_type = 'WHATSAPP'),
    constraint chk_conversation_status
        check (status in ('OPEN', 'PENDING', 'CLOSED')),
    constraint chk_conversation_unread_count
        check (unread_count >= 0)
);

create table message (
    id uuid primary key,
    business_id uuid not null,
    conversation_id uuid not null,
    sent_by_user_id uuid,
    direction varchar(20) not null,
    message_type varchar(20) not null,
    body text not null,
    status varchar(20) not null,
    external_message_id varchar(120),
    provider_event_id varchar(120),
    sent_at timestamp with time zone,
    received_at timestamp with time zone,
    failed_at timestamp with time zone,
    error_code varchar(80),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_message_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_message_conversation
        foreign key (conversation_id) references conversation (id) on delete cascade,
    constraint fk_message_sent_by_user
        foreign key (sent_by_user_id) references user_account (id) on delete set null,
    constraint chk_message_direction
        check (direction in ('INBOUND', 'OUTBOUND')),
    constraint chk_message_type
        check (message_type = 'TEXT'),
    constraint chk_message_status
        check (status in ('RECEIVED', 'QUEUED', 'SENT', 'DELIVERED', 'READ', 'FAILED'))
);

create table lead (
    id uuid primary key,
    business_id uuid not null,
    customer_id uuid not null,
    conversation_id uuid,
    source_type varchar(20) not null,
    first_name varchar(80) not null,
    last_name varchar(80) not null,
    phone varchar(30) not null,
    normalized_phone varchar(30) not null,
    email varchar(255),
    stage varchar(20) not null,
    notes text,
    assigned_user_id uuid,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_lead_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_lead_customer
        foreign key (customer_id) references customer (id) on delete restrict,
    constraint fk_lead_conversation
        foreign key (conversation_id) references conversation (id) on delete set null,
    constraint fk_lead_assigned_user
        foreign key (assigned_user_id) references user_account (id) on delete set null,
    constraint uq_lead_conversation
        unique (conversation_id),
    constraint chk_lead_source_type
        check (source_type in ('MANUAL', 'CONVERSATION')),
    constraint chk_lead_stage
        check (stage in ('NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'WON', 'LOST'))
);

create table lead_note (
    id uuid primary key,
    business_id uuid not null,
    lead_id uuid not null,
    author_user_id uuid not null,
    note_text text not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_lead_note_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_lead_note_lead
        foreign key (lead_id) references lead (id) on delete cascade,
    constraint fk_lead_note_author_user
        foreign key (author_user_id) references user_account (id) on delete restrict
);

create table booking (
    id uuid primary key,
    business_id uuid not null,
    customer_id uuid not null,
    lead_id uuid,
    conversation_id uuid,
    assigned_user_id uuid,
    subject varchar(180) not null,
    status varchar(20) not null,
    starts_at timestamp with time zone not null,
    duration_minutes integer not null,
    location varchar(180),
    notes text,
    completed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_booking_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_booking_customer
        foreign key (customer_id) references customer (id) on delete restrict,
    constraint fk_booking_lead
        foreign key (lead_id) references lead (id) on delete set null,
    constraint fk_booking_conversation
        foreign key (conversation_id) references conversation (id) on delete set null,
    constraint fk_booking_assigned_user
        foreign key (assigned_user_id) references user_account (id) on delete set null,
    constraint chk_booking_status
        check (status in ('SCHEDULED', 'RESCHEDULED', 'COMPLETED', 'CANCELLED')),
    constraint chk_booking_duration_minutes
        check (duration_minutes > 0)
);

create table product_category (
    id uuid primary key,
    business_id uuid not null,
    code varchar(50) not null,
    name varchar(120) not null,
    description varchar(255),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_product_category_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_product_category_business_code
        unique (business_id, code)
);

create table product_service (
    id uuid primary key,
    business_id uuid not null,
    category_id uuid not null,
    type varchar(20) not null,
    name varchar(120) not null,
    sku varchar(50) not null,
    description varchar(500),
    price numeric(12, 2) not null,
    duration_minutes integer,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_product_service_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_product_service_category
        foreign key (category_id) references product_category (id) on delete restrict,
    constraint uq_product_service_business_sku
        unique (business_id, sku),
    constraint chk_product_service_type
        check (type in ('PRODUCT', 'SERVICE')),
    constraint chk_product_service_price
        check (price >= 0),
    constraint chk_product_service_duration
        check (duration_minutes is null or duration_minutes > 0)
);

create table order_request (
    id uuid primary key,
    business_id uuid not null,
    customer_id uuid not null,
    lead_id uuid not null,
    conversation_id uuid,
    created_by_user_id uuid,
    status varchar(20) not null,
    payment_status varchar(20) not null,
    subtotal_amount numeric(12, 2) not null,
    discount_amount numeric(12, 2) not null default 0,
    total_amount numeric(12, 2) not null,
    paid_amount numeric(12, 2) not null default 0,
    balance_due numeric(12, 2) not null,
    currency varchar(3) not null,
    due_date date,
    notes text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_order_request_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_order_request_customer
        foreign key (customer_id) references customer (id) on delete restrict,
    constraint fk_order_request_lead
        foreign key (lead_id) references lead (id) on delete restrict,
    constraint fk_order_request_conversation
        foreign key (conversation_id) references conversation (id) on delete set null,
    constraint fk_order_request_created_by_user
        foreign key (created_by_user_id) references user_account (id) on delete set null,
    constraint chk_order_request_status
        check (status in ('DRAFT', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    constraint chk_order_request_payment_status
        check (payment_status in ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    constraint chk_order_request_subtotal_amount
        check (subtotal_amount >= 0),
    constraint chk_order_request_discount_amount
        check (discount_amount >= 0),
    constraint chk_order_request_total_amount
        check (total_amount >= 0),
    constraint chk_order_request_paid_amount
        check (paid_amount >= 0),
    constraint chk_order_request_balance_due
        check (balance_due >= 0)
);

create table order_item (
    id uuid primary key,
    business_id uuid not null,
    order_request_id uuid not null,
    product_service_id uuid not null,
    product_name_snapshot varchar(120) not null,
    sku_snapshot varchar(50) not null,
    quantity integer not null,
    unit_price numeric(12, 2) not null,
    line_total numeric(12, 2) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_order_item_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_order_item_order_request
        foreign key (order_request_id) references order_request (id) on delete cascade,
    constraint fk_order_item_product_service
        foreign key (product_service_id) references product_service (id) on delete restrict,
    constraint chk_order_item_quantity
        check (quantity > 0),
    constraint chk_order_item_unit_price
        check (unit_price >= 0),
    constraint chk_order_item_line_total
        check (line_total >= 0)
);

create table payment (
    id uuid primary key,
    business_id uuid not null,
    order_request_id uuid not null,
    amount numeric(12, 2) not null,
    method varchar(30) not null,
    paid_at timestamp with time zone not null,
    reference varchar(120),
    notes text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_payment_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_payment_order_request
        foreign key (order_request_id) references order_request (id) on delete cascade,
    constraint chk_payment_amount
        check (amount > 0)
);

create table automation_rule (
    id uuid primary key,
    business_id uuid not null,
    name varchar(120) not null,
    trigger_type varchar(40) not null,
    keyword varchar(100),
    delay_minutes integer not null default 0,
    action_type varchar(40) not null,
    response_template_id uuid,
    assigned_user_id uuid,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_automation_rule_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_automation_rule_response_template
        foreign key (response_template_id) references response_template (id) on delete set null,
    constraint fk_automation_rule_assigned_user
        foreign key (assigned_user_id) references user_account (id) on delete set null,
    constraint chk_automation_rule_trigger_type
        check (trigger_type in ('KEYWORD_MATCH', 'MANUAL', 'BOOKING_REMINDER')),
    constraint chk_automation_rule_delay_minutes
        check (delay_minutes >= 0),
    constraint chk_automation_rule_action_type
        check (action_type in ('SEND_TEMPLATE', 'ASSIGN_USER', 'CREATE_NOTIFICATION')),
    constraint chk_automation_rule_keyword_requirement
        check (
            (trigger_type <> 'KEYWORD_MATCH' and keyword is null)
            or (trigger_type = 'KEYWORD_MATCH' and keyword is not null and length(trim(keyword)) > 0)
        ),
    constraint chk_automation_rule_template_requirement
        check (
            (action_type <> 'SEND_TEMPLATE' and response_template_id is null)
            or (action_type = 'SEND_TEMPLATE' and response_template_id is not null)
        )
);

create table automation_execution (
    id uuid primary key,
    business_id uuid not null,
    rule_id uuid not null,
    conversation_id uuid,
    run_type varchar(20) not null,
    status varchar(20) not null,
    input_payload jsonb not null default '{}'::jsonb,
    result_payload jsonb not null default '{}'::jsonb,
    executed_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_automation_execution_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_automation_execution_rule
        foreign key (rule_id) references automation_rule (id) on delete cascade,
    constraint fk_automation_execution_conversation
        foreign key (conversation_id) references conversation (id) on delete set null,
    constraint chk_automation_execution_run_type
        check (run_type in ('TEST', 'LIVE')),
    constraint chk_automation_execution_status
        check (status in ('SUCCESS', 'FAILED', 'SKIPPED'))
);

create table channel_event_log (
    id uuid primary key,
    business_id uuid not null,
    channel_account_id uuid not null,
    delivery_id varchar(120) not null,
    event_type varchar(80) not null,
    payload jsonb not null default '{}'::jsonb,
    received_at timestamp with time zone not null,
    processed_at timestamp with time zone,
    processing_status varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_channel_event_log_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_channel_event_log_channel_account
        foreign key (channel_account_id) references channel_account (id) on delete cascade,
    constraint uq_channel_event_log_business_delivery
        unique (business_id, delivery_id),
    constraint chk_channel_event_log_processing_status
        check (processing_status in ('RECEIVED', 'PROCESSED', 'FAILED'))
);

create table message_delivery_log (
    id uuid primary key,
    business_id uuid not null,
    message_id uuid not null,
    delivery_status varchar(30) not null,
    provider_event_id varchar(120),
    provider_payload jsonb not null default '{}'::jsonb,
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_message_delivery_log_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_message_delivery_log_message
        foreign key (message_id) references message (id) on delete cascade,
    constraint chk_message_delivery_log_status
        check (delivery_status in ('QUEUED', 'PROVIDER_ACCEPTED', 'SENT', 'DELIVERED', 'READ', 'FAILED'))
);

create table notification (
    id uuid primary key,
    business_id uuid not null,
    user_id uuid not null,
    type varchar(50) not null,
    status varchar(20) not null,
    title varchar(150) not null,
    body text not null,
    related_entity_type varchar(50),
    related_entity_id uuid,
    read_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_notification_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_notification_user
        foreign key (user_id) references user_account (id) on delete cascade,
    constraint chk_notification_status
        check (status in ('UNREAD', 'READ'))
);

create table audit_log (
    id uuid primary key,
    business_id uuid not null,
    actor_user_id uuid,
    action_type varchar(80) not null,
    entity_type varchar(80) not null,
    entity_id uuid,
    summary varchar(255) not null,
    metadata jsonb not null default '{}'::jsonb,
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_audit_log_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_audit_log_actor_user
        foreign key (actor_user_id) references user_account (id) on delete set null
);

create table password_reset_token (
    id uuid primary key,
    business_id uuid not null,
    user_id uuid not null,
    token_hash varchar(255) not null unique,
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_password_reset_token_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_password_reset_token_user
        foreign key (user_id) references user_account (id) on delete cascade,
    constraint chk_password_reset_token_expiry
        check (expires_at > created_at)
);
