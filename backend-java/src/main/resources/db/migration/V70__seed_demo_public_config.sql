insert into business_public_config (
    id,
    business_id,
    slug,
    primary_color,
    secondary_color,
    welcome_title,
    welcome_subtitle,
    about_title,
    about_text,
    benefits,
    testimonials,
    whatsapp_message_template,
    show_services,
    show_promotions,
    show_testimonials,
    active
)
values (
    '70000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    'centro-estetico-bella',
    '#EC4899',
    '#8B5CF6',
    'Tu centro de estética de confianza',
    'En Centro Estético Bella combinamos tecnología y cuidado personal para ofrecerte los mejores tratamientos faciales, corporales y de bienestar en Santiago.',
    'Sobre nosotros',
    'Con más de 10 años de experiencia, somos el centro de estética líder en Santiago. Nuestro equipo de profesionales altamente calificados utiliza tecnología de punta para brindarte resultados visibles desde la primera sesión. Creemos en la belleza natural y trabajamos para potenciar tu bienestar integral.',
    '[
        {"icon": "heart", "title": "Tratamientos faciales", "text": "Limpieza facial profunda, hidratación, peelings y más cuidados para tu rostro."},
        {"icon": "shield", "title": "Corporales", "text": "Masajes relajantes, drenaje linfático, exfoliaciones y tratamientos reductores."},
        {"icon": "star", "title": "Bienestar", "text": "Depilación láser, aromaterapia y tratamientos personalizados para tu bienestar integral."}
    ]'::jsonb,
    '[
        {"name": "María García", "text": "Excelente atención, muy profesionales. Mi piel nunca había lucido tan bien. Súper recomendadas.", "rating": 5},
        {"name": "Carolina Muñoz", "text": "Llevo un año viniendo y los resultados son increíbles. El equipo es maravilloso.", "rating": 5},
        {"name": "Andrea Vega", "text": "Me encanta la calidad del servicio y la calidez de todo el equipo. 100% recomendado.", "rating": 5}
    ]'::jsonb,
    'Hola, quiero más información sobre sus servicios',
    true,
    true,
    true,
    true
)
on conflict (business_id) do nothing;
