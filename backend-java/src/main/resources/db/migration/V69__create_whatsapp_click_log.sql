create table whatsapp_click_log (
    id uuid primary key,
    business_id uuid not null,
    slug varchar(50) not null,
    clicked_at timestamp with time zone not null default current_timestamp,
    source_ip varchar(45),
    user_agent text,
    referer text,
    constraint fk_click_log_business
        foreign key (business_id) references business (id) on delete cascade
);

create index idx_whatsapp_click_log_business on whatsapp_click_log (business_id);
create index idx_whatsapp_click_log_clicked_at on whatsapp_click_log (clicked_at);

comment on table whatsapp_click_log is 'Registro de clics en boton WhatsApp desde paginas publicas';
