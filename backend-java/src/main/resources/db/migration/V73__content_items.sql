create table content_items (
    id uuid primary key,
    business_id uuid not null,
    type varchar(30) not null,
    image_path varchar(500),
    text varchar(200) not null,
    status varchar(20) not null default 'INACTIVE',
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    created_by uuid,
    updated_by uuid,
    version bigint not null default 0,
    constraint fk_content_items_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint chk_content_items_type
        check (type in ('CATEGORY', 'SERVICE', 'LANDING_PAGE')),
    constraint chk_content_items_status
        check (status in ('ACTIVE', 'INACTIVE')),
    constraint chk_content_items_text_length
        check (char_length(trim(text)) > 0 and char_length(text) <= 200)
);

create index idx_content_items_business on content_items (business_id);
create index idx_content_items_type on content_items (type);
create index idx_content_items_status on content_items (status);
create index idx_content_items_business_type on content_items (business_id, type);
create index idx_content_items_business_status on content_items (business_id, status);
create index idx_content_items_created_at on content_items (created_at desc);

comment on table content_items is 'Contenido visual y textual para categorias, servicios y landing page';
comment on column content_items.type is 'Tipo: CATEGORY, SERVICE, LANDING_PAGE';
comment on column content_items.image_path is 'Ruta relativa del archivo almacenado';
comment on column content_items.text is 'Texto descriptivo maximo 200 caracteres';
comment on column content_items.status is 'ACTIVE para publicacion, INACTIVE para borrador';