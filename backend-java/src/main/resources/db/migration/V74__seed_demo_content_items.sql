-- V74__seed_demo_content_items.sql
-- Seed demo content items for landing page, services and categories.
-- Uses ON CONFLICT DO NOTHING for idempotent re-runs.

insert into content_items (id, business_id, type, text, status, created_by, updated_by, created_at, updated_at)
values ('70000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'LANDING_PAGE', 'Realza tu belleza y disfruta una experiencia pensada para ti', 'ACTIVE', '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026-07-01T12:00:00Z', '2026-07-01T12:00:00Z'),
       ('70000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'LANDING_PAGE', 'En Centro Estética Bella ofrecemos tratamientos faciales, hidratación, depilación y cuidado estético personalizado.', 'ACTIVE', '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026-07-01T12:00:00Z', '2026-07-01T12:00:00Z'),
       ('70000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'SERVICE', 'Limpieza facial profunda con extracción y mascarilla para renovar tu piel.', 'ACTIVE', '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026-07-01T12:00:00Z', '2026-07-01T12:00:00Z'),
       ('70000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'SERVICE', 'Hidratación facial express para recuperar luminosidad, suavidad y equilibrio.', 'ACTIVE', '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026-07-01T12:00:00Z', '2026-07-01T12:00:00Z'),
       ('70000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'SERVICE', 'Depilación láser profesional con resultados duraderos y piel suave.', 'ACTIVE', '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026-07-01T12:00:00Z', '2026-07-01T12:00:00Z'),
       ('70000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'CATEGORY', 'Faciales: tratamientos diseñados para limpiar, renovar y mejorar tu piel.', 'ACTIVE', '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026-07-01T12:00:00Z', '2026-07-01T12:00:00Z'),
       ('70000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'CATEGORY', 'Depilación: alternativas profesionales para una piel suave y cuidada.', 'ACTIVE', '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026-07-01T12:00:00Z', '2026-07-01T12:00:00Z')
on conflict (id) do nothing;
