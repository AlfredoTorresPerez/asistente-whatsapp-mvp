alter table aesthetic_service_category
    add column if not exists display_order integer not null default 0;

update aesthetic_service_category set display_order = 1 where code = 'FACIAL';
update aesthetic_service_category set display_order = 2 where code = 'CORPORAL';
update aesthetic_service_category set display_order = 3 where code = 'MASAJES';
update aesthetic_service_category set display_order = 4 where code = 'MANICURE_PEDICURE';
update aesthetic_service_category set display_order = 5 where code = 'DEPILACION';
update aesthetic_service_category set display_order = 6 where code = 'PESTANAS_CEJAS';
update aesthetic_service_category set display_order = 7 where code = 'PELUQUERIA';
update aesthetic_service_category set display_order = 8 where code = 'MAQUILLAJE';
update aesthetic_service_category set display_order = 9 where code = 'MEDICINA_NO_INVASIVA';
