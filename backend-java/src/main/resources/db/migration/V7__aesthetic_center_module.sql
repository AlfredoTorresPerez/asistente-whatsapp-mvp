create table aesthetic_professional (
    id uuid primary key,
    business_id uuid not null,
    full_name varchar(160) not null,
    specialty varchar(120) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_professional_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_aesthetic_professional_business_name
        unique (business_id, full_name)
);

create table aesthetic_service_category (
    id uuid primary key,
    business_id uuid not null,
    code varchar(60) not null,
    name varchar(140) not null,
    description varchar(500),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_service_category_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_aesthetic_service_category_business_code
        unique (business_id, code)
);

create table aesthetic_service (
    id uuid primary key,
    business_id uuid not null,
    category_id uuid not null,
    code varchar(70) not null,
    name varchar(160) not null,
    description text not null,
    duration_minutes integer not null,
    price_base numeric(12, 2) not null,
    professional_required varchar(160) not null,
    supplies text,
    contraindications text,
    availability_rules text,
    booking_rules text,
    cancellation_rules text,
    aftercare_recommendations text,
    requires_prior_evaluation boolean not null default false,
    requires_informed_consent boolean not null default false,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_service_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_aesthetic_service_category
        foreign key (category_id) references aesthetic_service_category (id) on delete restrict,
    constraint uq_aesthetic_service_business_code
        unique (business_id, code),
    constraint chk_aesthetic_service_duration
        check (duration_minutes between 10 and 480),
    constraint chk_aesthetic_service_price
        check (price_base >= 0)
);

create table aesthetic_product_category (
    id uuid primary key,
    business_id uuid not null,
    code varchar(60) not null,
    name varchar(140) not null,
    description varchar(500),
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_product_category_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_aesthetic_product_category_business_code
        unique (business_id, code)
);

create table aesthetic_product (
    id uuid primary key,
    business_id uuid not null,
    category_id uuid not null,
    code varchar(70) not null,
    name varchar(160) not null,
    description text not null,
    price numeric(12, 2) not null,
    stock integer not null,
    stock_minimum integer not null,
    supplier varchar(160),
    expiration_date date,
    compatible_services text,
    recommendation_rules text,
    cross_sell_rules text,
    usage_restrictions text,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_product_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_aesthetic_product_category
        foreign key (category_id) references aesthetic_product_category (id) on delete restrict,
    constraint uq_aesthetic_product_business_code
        unique (business_id, code),
    constraint chk_aesthetic_product_price
        check (price >= 0),
    constraint chk_aesthetic_product_stock
        check (stock >= 0),
    constraint chk_aesthetic_product_stock_minimum
        check (stock_minimum >= 0)
);

create table aesthetic_business_rule (
    id uuid primary key,
    business_id uuid not null,
    code varchar(80) not null,
    name varchar(160) not null,
    rule_type varchar(60) not null,
    description text not null,
    priority integer not null default 100,
    active boolean not null default true,
    rule_payload jsonb not null default '{}'::jsonb,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_business_rule_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_aesthetic_business_rule_business_code
        unique (business_id, code),
    constraint chk_aesthetic_business_rule_priority
        check (priority between 1 and 999)
);

create table aesthetic_promotion (
    id uuid primary key,
    business_id uuid not null,
    code varchar(80) not null,
    name varchar(160) not null,
    description text not null,
    discount_type varchar(30) not null,
    discount_value numeric(12, 2) not null,
    starts_on date,
    ends_on date,
    active boolean not null default true,
    conditions text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_promotion_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint uq_aesthetic_promotion_business_code
        unique (business_id, code),
    constraint chk_aesthetic_promotion_discount_type
        check (discount_type in ('PERCENTAGE', 'FIXED_AMOUNT')),
    constraint chk_aesthetic_promotion_discount_value
        check (discount_value >= 0),
    constraint chk_aesthetic_promotion_dates
        check (starts_on is null or ends_on is null or ends_on >= starts_on)
);

create table aesthetic_treatment_history (
    id uuid primary key,
    business_id uuid not null,
    customer_id uuid not null,
    service_id uuid not null,
    professional_id uuid,
    performed_at timestamp with time zone not null,
    notes text,
    aftercare_sent boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_treatment_history_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_aesthetic_treatment_history_customer
        foreign key (customer_id) references customer (id) on delete restrict,
    constraint fk_aesthetic_treatment_history_service
        foreign key (service_id) references aesthetic_service (id) on delete restrict,
    constraint fk_aesthetic_treatment_history_professional
        foreign key (professional_id) references aesthetic_professional (id) on delete set null
);

create table aesthetic_intent_log (
    id uuid primary key,
    business_id uuid not null,
    customer_id uuid,
    conversation_id uuid,
    source_message text not null,
    intent varchar(80) not null,
    confidence numeric(4, 3) not null,
    entities jsonb not null default '{}'::jsonb,
    requires_database_lookup boolean not null,
    requires_human_handoff boolean not null,
    handoff_reason text,
    suggested_response text,
    model_name varchar(100) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_aesthetic_intent_log_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_aesthetic_intent_log_customer
        foreign key (customer_id) references customer (id) on delete set null,
    constraint fk_aesthetic_intent_log_conversation
        foreign key (conversation_id) references conversation (id) on delete set null,
    constraint chk_aesthetic_intent_log_confidence
        check (confidence between 0 and 1)
);

create index idx_aesthetic_service_business_active on aesthetic_service (business_id, active, name);
create index idx_aesthetic_product_business_active on aesthetic_product (business_id, active, name);
create index idx_aesthetic_rule_business_active on aesthetic_business_rule (business_id, active, rule_type, priority);
create index idx_aesthetic_intent_log_business_created on aesthetic_intent_log (business_id, created_at desc);

insert into aesthetic_professional (id, business_id, full_name, specialty, active)
values
    ('71000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Carla Mendez', 'Cosmetologia facial y evaluacion estetica', true),
    ('71000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Valentina Rios', 'Tratamientos corporales y masoterapia', true),
    ('71000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Daniela Soto', 'Depilacion, cejas y pestanas', true),
    ('71000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'Marcela Fuentes', 'Manicure, pedicure y peluqueria', true)
on conflict (business_id, full_name) do nothing;

insert into aesthetic_service_category (id, business_id, code, name, description, active)
values
    ('72000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'FACIAL', 'Tratamientos faciales', 'Servicios de limpieza, hidratacion, renovacion y rejuvenecimiento facial.', true),
    ('72000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'CORPORAL', 'Tratamientos corporales', 'Servicios corporales reductivos, relajantes y drenantes.', true),
    ('72000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'DEPILACION', 'Depilacion', 'Servicios de depilacion laser, cera y perfilado.', true),
    ('72000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'MANICURE_PEDICURE', 'Manicure y pedicure', 'Servicios de manos, pies y esmaltes.', true),
    ('72000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'PESTANAS_CEJAS', 'Pestanas y cejas', 'Servicios de lifting, extension, laminado y tinte.', true),
    ('72000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'PELUQUERIA', 'Peluqueria', 'Servicios de corte, color, brushing y tratamientos capilares.', true),
    ('72000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'MAQUILLAJE', 'Maquillaje', 'Servicios sociales, novia, eventos y clases.', true),
    ('72000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'MEDICINA_NO_INVASIVA', 'Medicina estetica no invasiva', 'Evaluaciones, asesorias y procedimientos no quirurgicos permitidos.', true)
on conflict (business_id, code) do nothing;

insert into aesthetic_service (
    id, business_id, category_id, code, name, description, duration_minutes, price_base,
    professional_required, supplies, contraindications, availability_rules, booking_rules,
    cancellation_rules, aftercare_recommendations, requires_prior_evaluation, requires_informed_consent, active
)
values
    ('73000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000001', 'FAC-LIMPIEZA', 'Limpieza facial profunda', 'Limpieza facial con higienizacion, extraccion controlada, mascarilla y sellado hidratante.', 60, 34990, 'Cosmetologa facial', 'Limpiador, tonico, vapor, mascarilla, hidratante, protector solar', 'Heridas abiertas, infeccion activa, alergia severa a cosmeticos o irritacion intensa.', 'Lunes a sabado; requiere cabina facial disponible.', 'Solicitar nombre, telefono, fecha preferida y advertir contraindicaciones.', 'Reprogramacion permitida con 12 horas de anticipacion.', 'Usar protector solar y evitar exfoliantes por 48 horas.', false, false, true),
    ('73000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000001', 'FAC-HIDRATACION', 'Hidratacion facial', 'Tratamiento hidratante para piel opaca o deshidratada.', 45, 29990, 'Cosmetologa facial', 'Serum hidratante, mascarilla hidratante, crema selladora', 'Alergia activa o brote dermatologico sin evaluar.', 'Disponible con profesional facial.', 'Puede combinarse con limpieza si hay disponibilidad extendida.', 'Reprogramacion permitida con 12 horas de anticipacion.', 'Mantener hidratacion y protector solar.', false, false, true),
    ('73000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000001', 'FAC-PEELING', 'Peeling estetico', 'Renovacion superficial no invasiva orientada a luminosidad y textura.', 50, 39990, 'Cosmetologa facial certificada', 'Acidos cosmeticos permitidos, neutralizante, mascarilla calmante', 'Embarazo, lactancia, isotretinoina reciente, heridas, piel irritada o exposicion solar intensa.', 'Requiere evaluacion previa si hay piel sensible o medicacion dermatologica.', 'Exigir consentimiento informado y ficha de antecedentes.', 'Cancelacion con 24 horas por preparacion de insumos.', 'Evitar sol, calor intenso y exfoliantes por 7 dias.', true, true, true),
    ('73000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000001', 'FAC-RADIOFRECUENCIA', 'Radiofrecuencia facial', 'Tratamiento facial no invasivo de apoyo a tonicidad y apariencia de firmeza.', 45, 42990, 'Profesional estetica avanzada', 'Gel conductor, equipo radiofrecuencia, mascarilla calmante', 'Marcapasos, embarazo, implantes metalicos cercanos, enfermedad activa no evaluada.', 'Requiere equipo disponible y evaluacion si existen antecedentes medicos.', 'Solicitar consentimiento informado.', 'Reprogramacion con 24 horas.', 'Hidratar y evitar calor intenso por 24 horas.', true, true, true),
    ('73000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000001', 'FAC-REJUVENECIMIENTO', 'Rejuvenecimiento facial no invasivo', 'Protocolo combinado de hidratacion, radiofrecuencia y mascarilla tensora.', 75, 59990, 'Profesional estetica avanzada', 'Serum, equipo, mascarilla tensora, protector solar', 'Contraindicaciones asociadas a equipos y piel lesionada.', 'Validar disponibilidad de cabina y equipo.', 'Requiere evaluacion estetica inicial.', 'Reprogramacion con 24 horas.', 'Usar protector solar y no exponerse al sol directo.', true, true, true),
    ('73000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000001', 'FAC-DERMAPEN', 'Dermapen estetico', 'Microestimulacion superficial realizada solo tras evaluacion profesional.', 60, 54990, 'Profesional certificada', 'Cartucho esteril, serum compatible, mascarilla calmante', 'Embarazo, anticoagulantes, acne activo severo, infecciones, queloides o heridas.', 'Solo con evaluacion y consentimiento.', 'Requiere ficha, consentimiento y derivacion ante duda clinica.', 'Cancelacion con 24 horas.', 'No maquillaje por 24 horas y protector solar estricto.', true, true, true),
    ('73000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000001', 'FAC-MICRODERMO', 'Microdermoabrasion', 'Exfoliacion mecanica controlada para mejorar textura superficial.', 45, 36990, 'Cosmetologa facial', 'Equipo microdermoabrasion, mascarilla calmante, hidratante', 'Rosacea activa, piel inflamada, heridas o infeccion.', 'No combinar con peeling el mismo dia.', 'Confirmar sensibilidad de piel antes de reservar.', 'Reprogramacion con 12 horas.', 'Evitar sol y exfoliantes por 72 horas.', true, false, true),
    ('73000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000002', 'CORP-REDUCCION', 'Tratamiento reductivo', 'Sesion corporal reductiva no invasiva segun evaluacion estetica.', 60, 44990, 'Especialista corporal', 'Gel conductor, crema reductiva, equipo corporal', 'Embarazo, enfermedades no controladas, marcapasos, trombosis o lesion activa.', 'Requiere evaluacion previa y disponibilidad de cabina corporal.', 'No prometer resultados garantizados.', 'Reprogramacion con 24 horas.', 'Hidratarse y seguir indicaciones profesionales.', true, true, true),
    ('73000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000002', 'CORP-DRENAJE', 'Drenaje linfatico', 'Masaje manual suave de apoyo a sensacion de alivio y drenaje.', 60, 38990, 'Masoterapeuta', 'Aceite neutro, camilla, toallas', 'Embarazo sin autorizacion, fiebre, infeccion, trombosis, insuficiencia cardiaca o renal.', 'Derivar ante embarazo o condicion medica.', 'Solicitar antecedentes y no confirmar si hay riesgo.', 'Reprogramacion con 12 horas.', 'Beber agua y evitar comidas pesadas.', true, false, true),
    ('73000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000002', 'CORP-MASAJE-REDUCTIVO', 'Masaje reductivo', 'Masaje corporal intenso orientado a modelacion estetica.', 60, 36990, 'Masoterapeuta', 'Aceite corporal, crema modeladora, camilla', 'Moretones, lesiones, embarazo, dolor intenso o patologia vascular.', 'No combinar con procedimientos irritantes el mismo dia.', 'Pedir zona a tratar.', 'Reprogramacion con 12 horas.', 'Hidratar y observar reaccion de la piel.', true, false, true),
    ('73000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000002', 'CORP-MASAJE-RELAJANTE', 'Masaje relajante', 'Masaje de relajacion general en camilla.', 60, 34990, 'Masoterapeuta', 'Aceite neutro, aromaterapia opcional, toallas', 'Fiebre, infeccion, heridas o dolor no evaluado.', 'Disponible segun cabina y profesional.', 'Pedir zona de molestia y preferencia horaria.', 'Reprogramacion con 12 horas.', 'Hidratar y descansar.', false, false, true),
    ('73000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000002', 'CORP-CAVITACION', 'Cavitacion', 'Tratamiento corporal con equipo no invasivo sujeto a evaluacion.', 45, 42990, 'Profesional estetica avanzada', 'Gel conductor, equipo cavitacion, crema corporal', 'Embarazo, marcapasos, enfermedad hepatica o renal, trombosis.', 'Requiere evaluacion y equipo disponible.', 'Solicitar consentimiento.', 'Reprogramacion con 24 horas.', 'Hidratacion antes y despues.', true, true, true),
    ('73000000-0000-0000-0000-000000000013', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000002', 'CORP-PRESOTERAPIA', 'Presoterapia', 'Sesion de presoterapia para apoyo de bienestar corporal.', 40, 29990, 'Especialista corporal', 'Equipo presoterapia, mallas higienicas', 'Trombosis, infeccion, embarazo sin evaluacion, problemas circulatorios severos.', 'Requiere equipo disponible.', 'Derivar ante antecedentes circulatorios.', 'Reprogramacion con 12 horas.', 'Hidratarse despues de la sesion.', true, false, true),
    ('73000000-0000-0000-0000-000000000014', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000002', 'CORP-RADIOFRECUENCIA', 'Radiofrecuencia corporal', 'Tratamiento corporal con equipo de radiofrecuencia no invasiva.', 50, 46990, 'Profesional estetica avanzada', 'Gel conductor, equipo radiofrecuencia, crema post tratamiento', 'Marcapasos, embarazo, implantes metalicos, enfermedad activa.', 'Requiere disponibilidad de equipo y evaluacion previa.', 'Solicitar consentimiento informado.', 'Reprogramacion con 24 horas.', 'Evitar calor intenso por 24 horas.', true, true, true),
    ('73000000-0000-0000-0000-000000000015', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000003', 'DEP-LASER', 'Depilacion laser', 'Sesion de depilacion laser segun zona.', 30, 24990, 'Tecnica depilacion laser', 'Equipo laser, gel conductor, protector ocular', 'Embarazo, fotosensibilidad, medicacion fotosensible, piel lesionada o exposicion solar reciente.', 'Requiere equipo laser disponible.', 'Solicitar zona y advertir que precio depende de zona.', 'Reprogramacion con 24 horas.', 'No exponerse al sol y usar protector solar.', true, true, true),
    ('73000000-0000-0000-0000-000000000016', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000003', 'DEP-CERA', 'Depilacion con cera', 'Depilacion tradicional con cera segun zona.', 30, 15990, 'Tecnica depilacion', 'Cera, bandas, aceite post depilatorio', 'Piel irritada, heridas, quemaduras solares o uso reciente de acidos fuertes.', 'Disponible por zona y profesional.', 'Solicitar zona a depilar.', 'Reprogramacion con 12 horas.', 'Evitar sol y exfoliacion por 24 horas.', false, false, true),
    ('73000000-0000-0000-0000-000000000017', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000003', 'DEP-CEJAS', 'Perfilado de cejas', 'Diseno y perfilado de cejas.', 25, 12990, 'Especialista cejas', 'Pinza, cera facial, tijera, gel calmante', 'Irritacion, heridas o alergia a cera.', 'Disponible con especialista de cejas.', 'Preguntar si desea tinte adicional.', 'Reprogramacion con 6 horas.', 'Evitar maquillaje en la zona por 12 horas.', false, false, true),
    ('73000000-0000-0000-0000-000000000018', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000003', 'DEP-BOZO', 'Depilacion bozo', 'Depilacion facial zona bozo.', 15, 8990, 'Tecnica depilacion', 'Cera facial, gel calmante', 'Piel irritada, heridas o uso reciente de retinoides/acidos.', 'Disponible por agenda breve.', 'Advertir cuidado posterior.', 'Reprogramacion con 6 horas.', 'Evitar sol y maquillaje por 12 horas.', false, false, true),
    ('73000000-0000-0000-0000-000000000019', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000003', 'DEP-AXILAS', 'Depilacion axilas', 'Depilacion de axilas con cera o laser segun modalidad.', 25, 19990, 'Tecnica depilacion', 'Cera o equipo laser segun modalidad, gel calmante', 'Irritacion activa, heridas o fotosensibilidad si es laser.', 'Confirmar modalidad antes de reservar.', 'Pedir modalidad y fecha.', 'Reprogramacion con 12 horas.', 'Evitar desodorante irritante por 12 horas.', false, false, true),
    ('73000000-0000-0000-0000-000000000020', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000003', 'DEP-PIERNAS', 'Depilacion piernas', 'Depilacion de piernas parcial o completa.', 45, 29990, 'Tecnica depilacion', 'Cera o equipo laser, gel calmante', 'Irritacion, heridas o exposicion solar reciente.', 'Precio puede variar por modalidad y extension.', 'Confirmar piernas completas o medias piernas.', 'Reprogramacion con 12 horas.', 'Hidratar y evitar sol por 24 horas.', false, false, true),
    ('73000000-0000-0000-0000-000000000021', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000003', 'DEP-ROSTRO', 'Depilacion rostro', 'Depilacion facial por zonas.', 30, 18990, 'Tecnica depilacion', 'Cera facial, pinza, gel calmante', 'Piel sensible activa, heridas, retinoides o peeling reciente.', 'No combinar con peeling o microdermoabrasion el mismo dia.', 'Pedir zona y antecedentes de sensibilidad.', 'Reprogramacion con 12 horas.', 'Evitar maquillaje y sol directo.', true, false, true),
    ('73000000-0000-0000-0000-000000000022', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000004', 'MAN-TRADICIONAL', 'Manicure tradicional', 'Servicio de limado, cuticula y esmalte tradicional.', 45, 15990, 'Manicurista', 'Esmalte, lima, removedor, crema manos', 'Hongos, heridas o infeccion ungueal.', 'Disponible con mesa de manicure.', 'Pedir color preferido si aplica.', 'Reprogramacion con 6 horas.', 'Evitar golpes en esmalte reciente.', false, false, true),
    ('73000000-0000-0000-0000-000000000023', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000004', 'MAN-PERMANENTE', 'Manicure permanente', 'Manicure con esmaltado permanente.', 60, 22990, 'Manicurista', 'Base, esmalte permanente, lampara, top coat', 'Alergia a acrilatos, infeccion o una lesion.', 'Disponible con lampara y manicurista.', 'Informar retiro previo si tiene esmalte.', 'Reprogramacion con 12 horas.', 'Usar aceite de cuticulas.', false, false, true),
    ('73000000-0000-0000-0000-000000000024', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000004', 'PED-SPA', 'Pedicure spa', 'Pedicure con hidratacion y cuidado de pies.', 60, 24990, 'Pedicurista', 'Sales, crema, exfoliante, esmalte', 'Heridas, infeccion, pie diabetico no evaluado.', 'Derivar si hay condicion medica relevante.', 'Solicitar antecedentes si menciona diabetes o heridas.', 'Reprogramacion con 12 horas.', 'Mantener hidratacion de pies.', true, false, true),
    ('73000000-0000-0000-0000-000000000025', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000004', 'MAN-ACRILICAS', 'Unas acrilicas', 'Aplicacion de unas acrilicas.', 90, 39990, 'Manicurista avanzada', 'Acrilico, tips o moldes, lima, top coat', 'Alergia a acrilatos, infeccion o unas debilitadas severamente.', 'Requiere bloque de 90 minutos.', 'Consultar largo y diseno.', 'Reprogramacion con 24 horas.', 'No forzar retiro manual.', false, false, true),
    ('73000000-0000-0000-0000-000000000026', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000004', 'MAN-GEL', 'Unas gel', 'Aplicacion o refuerzo de gel.', 75, 34990, 'Manicurista avanzada', 'Gel, lampara, lima, top coat', 'Alergia a geles, infeccion o lesiones.', 'Requiere lampara disponible.', 'Consultar diseno.', 'Reprogramacion con 24 horas.', 'Cuidar cuticulas y evitar golpes.', false, false, true),
    ('73000000-0000-0000-0000-000000000027', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000004', 'MAN-RETIRO', 'Retiro de esmalte', 'Retiro seguro de esmalte tradicional o permanente.', 25, 8990, 'Manicurista', 'Removedor, limas, hidratante', 'Unas con dolor, inflamacion o infeccion deben evaluarse.', 'Disponible como agenda breve.', 'Confirmar tipo de esmalte.', 'Reprogramacion con 6 horas.', 'Hidratar unas y cuticulas.', false, false, true),
    ('73000000-0000-0000-0000-000000000028', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000005', 'PC-LIFTING-PESTANAS', 'Lifting de pestanas', 'Curvatura y realce de pestanas naturales.', 60, 26990, 'Especialista pestanas', 'Soluciones lifting, tinte opcional, parches', 'Alergia ocular, infeccion, irritacion o cirugia ocular reciente.', 'No realizar con irritacion ocular.', 'Preguntar alergias o sensibilidad ocular.', 'Reprogramacion con 12 horas.', 'No mojar pestanas por 24 horas.', true, false, true),
    ('73000000-0000-0000-0000-000000000029', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000005', 'PC-EXTENSION-PESTANAS', 'Extension de pestanas', 'Aplicacion de extensiones segun efecto seleccionado.', 120, 44990, 'Especialista pestanas', 'Pestanas, adhesivo, parches, removedor', 'Alergia a adhesivos, infeccion ocular o irritacion.', 'Requiere bloque de 120 minutos.', 'Consultar efecto deseado y alergias.', 'Reprogramacion con 24 horas.', 'No usar aceites ni mojar por 24 horas.', true, false, true),
    ('73000000-0000-0000-0000-000000000030', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000005', 'PC-LAMINADO-CEJAS', 'Laminado de cejas', 'Peinado semipermanente de cejas.', 45, 24990, 'Especialista cejas', 'Solucion laminado, neutralizante, hidratante', 'Alergia, heridas, irritacion o piel sensibilizada.', 'Disponible con especialista.', 'Preguntar si desea perfilado adicional.', 'Reprogramacion con 12 horas.', 'No mojar zona por 24 horas.', false, false, true),
    ('73000000-0000-0000-0000-000000000031', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000005', 'PC-TINTE-CEJAS', 'Tinte de cejas', 'Coloracion semipermanente de cejas.', 30, 14990, 'Especialista cejas', 'Tinte, oxidante, protector piel', 'Alergia a tintes, irritacion o heridas.', 'Recomendar prueba si hay antecedente de alergia.', 'Preguntar alergias.', 'Reprogramacion con 6 horas.', 'Evitar exfoliantes en cejas por 24 horas.', true, false, true),
    ('73000000-0000-0000-0000-000000000032', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000006', 'PEL-CORTE', 'Corte de cabello', 'Corte segun estilo solicitado.', 45, 18990, 'Estilista', 'Tijera, capa, productos de peinado', 'No aplica salvo heridas o afecciones del cuero cabelludo.', 'Disponible con estilista.', 'Consultar largo y estilo.', 'Reprogramacion con 12 horas.', 'Seguir recomendaciones de peinado.', false, false, true),
    ('73000000-0000-0000-0000-000000000033', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000006', 'PEL-LAVADO', 'Lavado de cabello', 'Lavado y acondicionamiento capilar.', 25, 9990, 'Estilista', 'Shampoo, acondicionador, toalla', 'Irritacion severa del cuero cabelludo.', 'Disponible como servicio breve.', 'Puede combinarse con brushing.', 'Reprogramacion con 6 horas.', 'No aplica.', false, false, true),
    ('73000000-0000-0000-0000-000000000034', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000006', 'PEL-BRUSHING', 'Brushing', 'Peinado con secado y forma.', 45, 16990, 'Estilista', 'Secador, cepillo, protector termico', 'Cuero cabelludo lesionado.', 'Disponible con estilista.', 'Consultar largo del cabello.', 'Reprogramacion con 6 horas.', 'Usar protector termico posteriormente.', false, false, true),
    ('73000000-0000-0000-0000-000000000035', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000006', 'PEL-TINTURA', 'Tintura', 'Coloracion capilar completa o parcial segun evaluacion.', 120, 49990, 'Colorista', 'Tinte, oxidante, tratamiento protector', 'Alergia a tintes, heridas o irritacion de cuero cabelludo.', 'Requiere evaluacion de color si hay procesos previos.', 'Recomendar prueba de alergia ante antecedentes.', 'Reprogramacion con 24 horas.', 'Usar shampoo para color.', true, false, true),
    ('73000000-0000-0000-0000-000000000036', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000006', 'PEL-BALAYAGE', 'Balayage', 'Tecnica de iluminacion capilar personalizada.', 180, 89990, 'Colorista avanzada', 'Decolorante, toner, tratamiento protector', 'Cabello muy sensibilizado, alergia o cuero cabelludo lesionado.', 'Requiere evaluacion previa.', 'No prometer resultado exacto sin evaluacion.', 'Reprogramacion con 24 horas.', 'Usar productos post coloracion.', true, false, true),
    ('73000000-0000-0000-0000-000000000037', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000006', 'PEL-ALISADO', 'Alisado', 'Servicio de alisado capilar sujeto a evaluacion.', 180, 79990, 'Estilista avanzada', 'Producto alisado, plancha, shampoo tecnico', 'Embarazo, alergias, cuero cabelludo lesionado o cabello muy danado.', 'Requiere evaluacion previa y ventilacion adecuada.', 'Derivar si hay embarazo o alergias.', 'Reprogramacion con 24 horas.', 'Seguir lavado y productos recomendados.', true, true, true),
    ('73000000-0000-0000-0000-000000000038', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000006', 'PEL-TRATAMIENTO-CAPILAR', 'Tratamiento capilar', 'Hidratacion o reparacion capilar segun necesidad.', 60, 29990, 'Estilista', 'Mascarilla capilar, ampolla, calor controlado', 'Cuero cabelludo lesionado o alergia a productos.', 'Disponible con estilista.', 'Consultar necesidad principal del cabello.', 'Reprogramacion con 12 horas.', 'Mantener rutina recomendada.', false, false, true),
    ('73000000-0000-0000-0000-000000000039', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000007', 'MAQ-SOCIAL', 'Maquillaje social', 'Maquillaje para ocasiones sociales.', 60, 34990, 'Maquilladora', 'Base, sombras, brochas, fijador', 'Alergia activa o irritacion ocular/piel.', 'Disponible con maquilladora.', 'Consultar tipo de evento.', 'Reprogramacion con 24 horas.', 'Retirar maquillaje adecuadamente.', false, false, true),
    ('73000000-0000-0000-0000-000000000040', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000007', 'MAQ-NOVIA', 'Maquillaje novia', 'Maquillaje de novia con preparacion personalizada.', 90, 69990, 'Maquilladora profesional', 'Kit maquillaje profesional, fijador, pestañas opcionales', 'Alergia activa o irritacion sin evaluar.', 'Recomendar prueba previa.', 'Coordinar fecha, hora y prueba.', 'Reprogramacion con 48 horas.', 'Kit de retoque sugerido.', false, false, true),
    ('73000000-0000-0000-0000-000000000041', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000007', 'MAQ-EVENTOS', 'Maquillaje eventos', 'Maquillaje para eventos corporativos o celebraciones.', 60, 39990, 'Maquilladora', 'Productos maquillaje, fijador', 'Alergia activa o irritacion.', 'Disponible con maquilladora.', 'Consultar horario del evento.', 'Reprogramacion con 24 horas.', 'Retirar maquillaje al finalizar el dia.', false, false, true),
    ('73000000-0000-0000-0000-000000000042', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000007', 'MAQ-AUTOMAQUILLAJE', 'Clase de automaquillaje', 'Clase guiada para aprender rutina de maquillaje personal.', 120, 59990, 'Maquilladora docente', 'Kit demostracion, brochas, guia de rutina', 'Alergias a cosmeticos deben informarse antes.', 'Requiere bloque de 120 minutos.', 'Consultar nivel y objetivo.', 'Reprogramacion con 24 horas.', 'Enviar guia posterior.', false, false, true),
    ('73000000-0000-0000-0000-000000000043', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000008', 'MED-EVALUACION', 'Evaluacion estetica', 'Evaluacion profesional para orientar tratamientos no invasivos.', 45, 19990, 'Profesional estetica avanzada', 'Ficha, consentimiento si aplica, pauta de evaluacion', 'No aplica como diagnostico medico.', 'Disponible con profesional senior.', 'Aclarar que no reemplaza consulta medica.', 'Reprogramacion con 12 horas.', 'Enviar plan sugerido sin prometer resultados.', false, false, true),
    ('73000000-0000-0000-0000-000000000044', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000008', 'MED-ASESORIA', 'Asesoria estetica', 'Asesoria para construir plan estetico seguro y realista.', 45, 24990, 'Profesional estetica avanzada', 'Ficha, pauta de objetivos, registro fotografico autorizado', 'Ante signos medicos, derivar a profesional de salud.', 'Disponible con profesional senior.', 'No diagnosticar ni prometer resultados.', 'Reprogramacion con 12 horas.', 'Entregar recomendaciones generales.', false, false, true),
    ('73000000-0000-0000-0000-000000000045', '11111111-1111-1111-1111-111111111111', '72000000-0000-0000-0000-000000000008', 'MED-NO-QUIRURGICO', 'Tratamiento no quirurgico permitido', 'Tratamientos esteticos no quirurgicos permitidos por normativa local, sujetos a evaluacion.', 60, 59990, 'Profesional autorizado segun tratamiento', 'Insumos especificos del tratamiento, consentimiento', 'Embarazo, alergias, enfermedades activas, medicacion relevante o contraindicaciones especificas.', 'Siempre requiere evaluacion, ficha y consentimiento.', 'Derivar ante riesgo, ambiguedad o informacion insuficiente.', 'Reprogramacion con 24 horas.', 'Seguir cuidados personalizados indicados por profesional.', true, true, true)
on conflict (business_id, code) do nothing;

insert into aesthetic_product_category (id, business_id, code, name, description, active)
values
    ('74000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'CREMAS_FACIALES', 'Cremas faciales', 'Hidratantes y cremas para rutina facial.', true),
    ('74000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'SERUMS', 'Serums', 'Serums faciales de apoyo cosmetico.', true),
    ('74000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'PROTECTORES_SOLARES', 'Protectores solares', 'Fotoproteccion diaria y post tratamiento.', true),
    ('74000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'EXFOLIANTES', 'Exfoliantes', 'Productos de exfoliacion cosmetica.', true),
    ('74000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'MASCARILLAS', 'Mascarillas', 'Mascarillas faciales y capilares.', true),
    ('74000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'CAPILARES', 'Productos capilares', 'Cuidado post coloracion y tratamientos capilares.', true),
    ('74000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'ACEITES_CORPORALES', 'Aceites corporales', 'Aceites para masaje y cuidado corporal.', true),
    ('74000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'ESMALTES', 'Esmaltes', 'Esmaltes tradicionales y permanentes.', true),
    ('74000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'KITS_CUIDADO_FACIAL', 'Kits de cuidado facial', 'Kits de rutina facial para casa.', true),
    ('74000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'POST_TRATAMIENTO', 'Productos post tratamiento', 'Productos recomendados despues de servicios esteticos.', true),
    ('74000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'GIFT_CARDS', 'Gift cards', 'Tarjetas de regalo para servicios y productos.', true),
    ('74000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'PACKS_PROMOCIONALES', 'Packs promocionales', 'Packs combinados de servicios y productos.', true)
on conflict (business_id, code) do nothing;

insert into aesthetic_product (
    id, business_id, category_id, code, name, description, price, stock, stock_minimum,
    supplier, expiration_date, compatible_services, recommendation_rules, cross_sell_rules,
    usage_restrictions, active
)
values
    ('75000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000001', 'PROD-CREMA-HIDRATANTE', 'Crema hidratante facial', 'Crema facial de uso diario para apoyar hidratacion.', 12990, 25, 5, 'Dermocosmetica Demo', '2027-12-31', 'Limpieza facial, hidratacion facial, peeling estetico', 'Recomendar despues de limpieza o hidratacion si el cliente busca rutina simple.', 'Ofrecer con protector solar para rutina diaria.', 'No usar si existe alergia conocida a sus componentes.', true),
    ('75000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000002', 'PROD-SERUM-HIALURONICO', 'Serum acido hialuronico', 'Serum hidratante cosmetico.', 18990, 18, 4, 'Dermocosmetica Demo', '2027-10-31', 'Hidratacion facial, rejuvenecimiento facial no invasivo', 'Recomendar para piel deshidratada sin lesion activa.', 'Ofrecer con crema hidratante.', 'Suspender si causa irritacion.', true),
    ('75000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000003', 'PROD-FPS50', 'Protector solar FPS 50', 'Protector solar facial de amplio uso cosmetico.', 15990, 32, 8, 'Solar Care Demo', '2028-01-31', 'Peeling estetico, microdermoabrasion, depilacion laser, limpieza facial', 'Recomendar siempre despues de servicios faciales o laser.', 'Ofrecer con kit de cuidado facial.', 'Reaplicar segun exposicion; no reemplaza indicacion medica.', true),
    ('75000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000004', 'PROD-EXFOLIANTE-SUAVE', 'Exfoliante facial suave', 'Exfoliante cosmetico de uso controlado.', 11990, 14, 4, 'Dermocosmetica Demo', '2027-09-30', 'Limpieza facial', 'Recomendar solo si no hubo peeling, dermapen ni irritacion reciente.', 'Ofrecer con crema hidratante.', 'No usar despues de peeling, laser o piel irritada.', true),
    ('75000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000005', 'PROD-MASCARILLA-CALMANTE', 'Mascarilla calmante', 'Mascarilla facial de apoyo a sensacion calmante.', 9990, 20, 5, 'Dermocosmetica Demo', '2027-11-30', 'Limpieza facial, microdermoabrasion, peeling estetico', 'Recomendar despues de tratamientos faciales no invasivos.', 'Ofrecer con protector solar.', 'No aplicar sobre heridas o alergia conocida.', true),
    ('75000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000006', 'PROD-SHAMPOO-COLOR', 'Shampoo post coloracion', 'Shampoo para cuidado de cabello tinturado.', 14990, 16, 4, 'Capilar Demo', '2028-03-31', 'Tintura, balayage, tratamiento capilar', 'Recomendar despues de servicios de color.', 'Ofrecer con mascarilla capilar.', 'Evitar contacto ocular.', true),
    ('75000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000007', 'PROD-ACEITE-CORPORAL', 'Aceite corporal relajante', 'Aceite corporal para masaje y cuidado en casa.', 10990, 22, 5, 'Corporal Demo', '2027-08-31', 'Masaje relajante, drenaje linfatico', 'Recomendar despues de masajes si no hay alergias.', 'Ofrecer con masaje relajante.', 'No usar en piel lesionada.', true),
    ('75000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000008', 'PROD-ESMALTE-NUDE', 'Esmalte nude', 'Esmalte tradicional color nude.', 5990, 35, 10, 'Nails Demo', null, 'Manicure tradicional, pedicure spa', 'Recomendar con manicure tradicional.', 'Ofrecer como venta adicional en servicios de unas.', 'Mantener fuera del alcance de ninos.', true),
    ('75000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000009', 'PROD-KIT-FACIAL-BASICO', 'Kit cuidado facial basico', 'Kit con limpiador, crema hidratante y protector solar.', 34990, 10, 3, 'Dermocosmetica Demo', '2027-12-31', 'Limpieza facial, hidratacion facial', 'Recomendar para clientes que empiezan rutina en casa.', 'Ofrecer despues de limpieza facial.', 'Validar alergias conocidas.', true),
    ('75000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000010', 'PROD-POST-LASER', 'Gel post depilacion laser', 'Gel calmante post depilacion laser.', 13990, 18, 5, 'Laser Care Demo', '2027-07-31', 'Depilacion laser', 'Recomendar despues de depilacion laser si no hay irritacion severa.', 'Ofrecer con protector solar.', 'No usar sobre heridas.', true),
    ('75000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000011', 'PROD-GIFT-50000', 'Gift card $50.000', 'Tarjeta de regalo canjeable en servicios o productos.', 50000, 50, 5, 'Centro Estetico Bella', null, 'Todos los servicios activos', 'Recomendar para regalos o fechas especiales.', 'Ofrecer con packs promocionales.', 'No canjeable por dinero en efectivo.', true),
    ('75000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', '74000000-0000-0000-0000-000000000012', 'PROD-PACK-LIMPIEZA-KIT', 'Pack limpieza facial + kit basico', 'Pack promocional de limpieza facial profunda y kit de cuidado facial.', 62990, 8, 2, 'Centro Estetico Bella', '2027-12-31', 'Limpieza facial profunda', 'Recomendar a clientes nuevos que desean rutina completa.', 'Incluye servicio y kit facial.', 'Sujeto a disponibilidad de agenda y stock.', true)
on conflict (business_id, code) do nothing;

insert into aesthetic_business_rule (id, business_id, code, name, rule_type, description, priority, active, rule_payload)
values
    ('76000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'RESERVA_DATOS_MINIMOS', 'Datos minimos para reservar', 'BOOKING', 'Para reservar se requiere servicio, nombre, telefono, fecha u opcion de horario.', 10, true, '{"requiredFields":["service","customerName","phone","dateOrSlot"]}'::jsonb),
    ('76000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'BLOQUEO_HORARIOS', 'Bloqueo de horarios no disponibles', 'AVAILABILITY', 'No ofrecer horarios ocupados, fuera de atencion o sin profesional compatible.', 20, true, '{"mustCheckDatabase":true,"rejectIfOverlaps":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'DURACION_MIN_MAX', 'Duracion minima y maxima por servicio', 'BOOKING', 'Toda reserva debe respetar la duracion configurada del servicio.', 30, true, '{"minMinutes":10,"maxMinutes":480}'::jsonb),
    ('76000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'DISPONIBILIDAD_PROFESIONAL', 'Disponibilidad por profesional', 'AVAILABILITY', 'Solo confirmar reservas cuando el profesional requerido este activo y disponible.', 40, true, '{"professionalRequired":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'EVALUACION_PREVIA', 'Servicios con evaluacion previa', 'SAFETY', 'Servicios marcados con evaluacion previa no se confirman automaticamente ante dudas o antecedentes sensibles.', 50, true, '{"requiresPriorEvaluation":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'NO_COMBINAR_MISMO_DIA', 'Servicios incompatibles el mismo dia', 'SAFETY', 'No combinar peeling, microdermoabrasion, laser facial o depilacion rostro el mismo dia sin evaluacion profesional.', 60, true, '{"blockedPairs":[["FAC-PEELING","FAC-MICRODERMO"],["FAC-PEELING","DEP-ROSTRO"],["DEP-LASER","FAC-PEELING"]]}'::jsonb),
    ('76000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'CONSENTIMIENTO', 'Consentimiento informado', 'SAFETY', 'Servicios con equipos, peelings, dermapen o procedimientos no quirurgicos requieren consentimiento informado.', 70, true, '{"requiresInformedConsent":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'CONTRAINDICACIONES', 'Contraindicaciones y derivacion', 'SAFETY', 'Si el cliente menciona embarazo, alergia, medicamentos, enfermedad, heridas, infeccion o procedimiento invasivo, derivar a profesional.', 5, true, '{"keywords":["embarazo","embarazada","alergia","medicamento","isotretinoina","anticoagulante","diabetes","herida","infeccion","fiebre","marcapasos","trombosis","lactancia"]}'::jsonb),
    ('76000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'VENTA_CRUZADA', 'Productos recomendados por tratamiento', 'RECOMMENDATION', 'Recomendar productos compatibles despues de validar el servicio y sus restricciones.', 80, true, '{"mustUseCompatibleServices":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'PROMOCIONES_PACK', 'Promociones por paquete', 'COMMERCIAL', 'Aplicar promociones activas solo cuando se cumplan condiciones de servicio, producto, fecha y stock.', 90, true, '{"mustCheckActivePromotion":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'CLIENTE_FRECUENTE', 'Descuento cliente frecuente', 'COMMERCIAL', 'Cliente frecuente puede acceder a descuento si cumple historial minimo configurado.', 100, true, '{"minCompletedTreatments":5,"discountPercentage":10}'::jsonb),
    ('76000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'CANCELACION', 'Cancelacion de reserva', 'BOOKING', 'Cancelar solo despues de identificar cita activa y recibir confirmacion explicita del cliente.', 110, true, '{"requiresExplicitConfirmation":true,"minimumHoursBefore":12}'::jsonb),
    ('76000000-0000-0000-0000-000000000013', '11111111-1111-1111-1111-111111111111', 'REPROGRAMACION', 'Reprogramacion de reserva', 'BOOKING', 'Reprogramar solo si existe cita activa y nuevo horario disponible.', 120, true, '{"mustHaveActiveBooking":true,"mustCheckNewSlot":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000014', '11111111-1111-1111-1111-111111111111', 'PAGO_PARCIAL_TOTAL', 'Pago parcial o total', 'PAYMENT', 'Servicios definidos pueden exigir abono o pago total antes de confirmar.', 130, true, '{"partialPaymentAllowed":true,"mustRecordPaymentStatus":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000015', '11111111-1111-1111-1111-111111111111', 'CONTROL_STOCK', 'Control de stock al vender', 'INVENTORY', 'No vender productos sin stock suficiente; descontar stock al confirmar venta.', 140, true, '{"mustCheckStock":true,"stockCannotBeNegative":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000016', '11111111-1111-1111-1111-111111111111', 'DESCUENTO_INSUMOS', 'Descuento de insumos por servicio', 'INVENTORY', 'Los insumos asociados al servicio deben registrarse y descontarse de inventario cuando aplique.', 150, true, '{"discountSuppliesOnCompletion":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000017', '11111111-1111-1111-1111-111111111111', 'ALERTA_STOCK_BAJO', 'Alerta por stock bajo', 'INVENTORY', 'Generar alerta si stock de producto queda bajo o igual al minimo.', 160, true, '{"triggerWhenStockLessOrEqualMinimum":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000018', '11111111-1111-1111-1111-111111111111', 'ALERTA_VENCIMIENTO', 'Alerta por vencimiento', 'INVENTORY', 'Generar alerta para productos con vencimiento dentro de 30 dias.', 170, true, '{"daysBeforeExpiration":30}'::jsonb),
    ('76000000-0000-0000-0000-000000000019', '11111111-1111-1111-1111-111111111111', 'HISTORIAL_TRATAMIENTOS', 'Historial de tratamientos', 'CUSTOMER_HISTORY', 'Registrar tratamientos realizados para recomendaciones y trazabilidad.', 180, true, '{"mustRegisterCompletedTreatments":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000020', '11111111-1111-1111-1111-111111111111', 'RECOMENDACION_HISTORIAL', 'Recomendaciones segun historial', 'RECOMMENDATION', 'Recomendar usando historial, compatibilidad, restricciones y disponibilidad; si falta informacion pedir aclaracion.', 190, true, '{"mustUseHistoryWhenAvailable":true,"askClarifyingQuestionWhenMissingData":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000021', '11111111-1111-1111-1111-111111111111', 'NO_INVENTAR_DATOS', 'No inventar datos operativos', 'AI_RESPONSE', 'El asistente no debe inventar precios, horarios, stock ni disponibilidad; debe consultar base de datos o derivar.', 1, true, '{"mustCheckInternalSource":true}'::jsonb),
    ('76000000-0000-0000-0000-000000000022', '11111111-1111-1111-1111-111111111111', 'NO_DIAGNOSTICO', 'No emitir diagnosticos medicos', 'AI_RESPONSE', 'El asistente no debe diagnosticar, prometer resultados ni recomendar tratamientos contraindicados.', 2, true, '{"medicalDiagnosisForbidden":true,"guaranteedResultsForbidden":true}'::jsonb)
on conflict (business_id, code) do nothing;

insert into aesthetic_promotion (id, business_id, code, name, description, discount_type, discount_value, starts_on, ends_on, active, conditions)
values
    ('77000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'PACK-FACIAL-KIT', 'Pack limpieza facial y kit facial', 'Promocion para clientes que agendan limpieza facial y compran kit de cuidado facial.', 'FIXED_AMOUNT', 6990, '2026-01-01', '2026-12-31', true, 'Aplica con limpieza facial profunda y kit cuidado facial basico.'),
    ('77000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'FRECUENTE-10', 'Cliente frecuente 10%', 'Descuento para clientes con cinco o mas tratamientos completados.', 'PERCENTAGE', 10, '2026-01-01', null, true, 'No acumulable con otras promociones.')
on conflict (business_id, code) do nothing;
