alter table aesthetic_professional
    add column if not exists qualification_level integer,
    add column if not exists certification_valid_until date;

alter table aesthetic_professional
    drop constraint if exists chk_aesthetic_professional_qualification_level,
    add constraint chk_aesthetic_professional_qualification_level
        check (qualification_level is null or qualification_level >= 0);

alter table aesthetic_service
    add column if not exists required_professional_level integer,
    add column if not exists requires_professional_certification boolean not null default false;

alter table aesthetic_service
    drop constraint if exists chk_aesthetic_service_required_professional_level,
    add constraint chk_aesthetic_service_required_professional_level
        check (required_professional_level is null or required_professional_level >= 0);

create index if not exists idx_aesthetic_professional_qualification
    on aesthetic_professional (business_id, active, qualification_level, certification_valid_until);

comment on column aesthetic_professional.qualification_level is
    'Nivel profesional configurable para reglas de servicios. Null equivale a sin nivel declarado.';
comment on column aesthetic_professional.certification_valid_until is
    'Fecha de vigencia de certificacion profesional cuando el servicio la exige.';
comment on column aesthetic_service.required_professional_level is
    'Nivel minimo requerido para atender el servicio. Null equivale a sin requisito.';
comment on column aesthetic_service.requires_professional_certification is
    'Exige certificacion profesional vigente para atender el servicio.';
