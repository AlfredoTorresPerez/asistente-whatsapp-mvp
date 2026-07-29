alter table booking
    add column if not exists requires_informed_consent boolean not null default false,
    add column if not exists informed_consent_accepted boolean not null default false,
    add column if not exists informed_consent_accepted_at timestamp with time zone,
    add column if not exists customer_birth_date date,
    add column if not exists guardian_name varchar(160),
    add column if not exists guardian_phone varchar(30);

create index if not exists idx_booking_consent_required
    on booking (business_id, requires_informed_consent, informed_consent_accepted)
    where requires_informed_consent = true;

comment on column booking.requires_informed_consent is
    'Indica si la reserva correspondia a un servicio que exigia consentimiento informado.';
comment on column booking.informed_consent_accepted is
    'Aceptacion explicita del consentimiento informado al momento de crear la reserva.';
comment on column booking.customer_birth_date is
    'Fecha de nacimiento declarada para validar edad y tutor cuando aplica.';
comment on column booking.guardian_name is
    'Nombre del tutor responsable cuando el cliente es menor de edad.';
comment on column booking.guardian_phone is
    'Telefono del tutor responsable cuando el cliente es menor de edad.';
