create table business_public_config (
    id uuid primary key,
    business_id uuid not null,
    slug varchar(50) not null,
    primary_color varchar(7) not null default '#EC4899',
    secondary_color varchar(7) not null default '#8B5CF6',
    welcome_title varchar(200),
    welcome_subtitle text,
    about_title varchar(200),
    about_text text,
    benefits jsonb not null default '[]'::jsonb,
    testimonials jsonb not null default '[]'::jsonb,
    whatsapp_message_template varchar(500) not null default 'Hola, quiero más información',
    header_logo_url varchar(500),
    hero_image_url varchar(500),
    show_services boolean not null default true,
    show_promotions boolean not null default true,
    show_testimonials boolean not null default true,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_public_config_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_public_config_business
        unique (business_id),
    constraint uq_public_config_slug
        unique (slug)
);

comment on table business_public_config is 'Configuracion de pagina publica por centro de estetica';
comment on column business_public_config.slug is 'Identificador unico para la URL publica /centros/{slug}';
comment on column business_public_config.benefits is 'JSONB array de beneficios: [{"icon": "heart", "title": "...", "text": "..."}]';
comment on column business_public_config.testimonials is 'JSONB array de testimonios: [{"name": "...", "text": "...", "rating": 5}]';
