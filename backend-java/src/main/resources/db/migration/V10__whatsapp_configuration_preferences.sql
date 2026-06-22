create table whatsapp_configuration_preferences (
    business_id uuid primary key,
    new_message_notifications boolean not null default true,
    auto_reassignment boolean not null default true,
    agent_signature boolean not null default true,
    out_of_hours_message boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_whatsapp_configuration_preferences_business
        foreign key (business_id) references business (id) on delete cascade
);

insert into whatsapp_configuration_preferences (
    business_id,
    new_message_notifications,
    auto_reassignment,
    agent_signature,
    out_of_hours_message
)
select id, true, true, true, false
from business
on conflict (business_id) do nothing;
