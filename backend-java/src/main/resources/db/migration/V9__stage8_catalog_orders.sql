create extension if not exists pgcrypto;

alter table product_service
    add column if not exists stock_quantity integer not null default 0,
    add column if not exists stock_minimum integer not null default 0,
    add column if not exists supplier varchar(160),
    add column if not exists expires_at date;

alter table order_request
    alter column lead_id drop not null;

alter table order_request
    drop constraint if exists chk_order_request_status;

update order_request
set status = 'DELIVERED'
where status = 'COMPLETED';

alter table order_request
    add constraint chk_order_request_status
        check (status in ('DRAFT', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED'));

alter table order_request
    drop constraint if exists chk_order_request_payment_status;

update order_request
set payment_status = 'PARTIAL'
where payment_status = 'PARTIALLY_PAID';

update order_request
set payment_status = 'PENDING'
where payment_status = 'OVERDUE';

alter table order_request
    add constraint chk_order_request_payment_status
        check (payment_status in ('PENDING', 'PAID', 'PARTIAL'));

create index if not exists idx_product_service_business_type_active
    on product_service (business_id, type, active);

create index if not exists idx_product_service_business_sku
    on product_service (business_id, sku);

create index if not exists idx_product_service_stock
    on product_service (business_id, stock_quantity, stock_minimum)
    where type = 'PRODUCT';

insert into product_category (id, business_id, code, name, description, active)
select gen_random_uuid(), b.id, seed.code, seed.name, seed.description, true
from business b
cross join (values
    ('facial-care', 'Cuidado facial', 'Productos para limpieza, hidratacion y rutina facial'),
    ('solar-care', 'Proteccion solar', 'Protectores solares y productos post tratamiento'),
    ('body-care', 'Cuidado corporal', 'Aceites, exfoliantes y productos corporales'),
    ('hair-care', 'Cuidado capilar', 'Productos para tratamientos capilares'),
    ('nails', 'Manicure y pedicure', 'Esmaltes, bases, top coat y cuidado de unas'),
    ('gift-cards', 'Gift cards', 'Tarjetas de regalo y packs promocionales')
) as seed(code, name, description)
where not exists (
    select 1
    from product_category pc
    where pc.business_id = b.id
      and pc.code = seed.code
);

insert into product_service (
    id, business_id, category_id, type, name, sku, description, price, duration_minutes,
    active, stock_quantity, stock_minimum, supplier
)
select gen_random_uuid(), b.id, pc.id, 'PRODUCT', seed.name, seed.sku, seed.description,
       seed.price, null, true, seed.stock_quantity, seed.stock_minimum, seed.supplier
from business b
join product_category pc
  on pc.business_id = b.id
cross join (values
    ('facial-care', 'Crema hidratante facial 50 ml', 'CREMA-HID-050', 'Crema hidratante para rutina diaria y post limpieza facial.', 12990::numeric, 30, 6, 'Proveedor estetico local'),
    ('facial-care', 'Serum vitamina C', 'SERUM-VITC-030', 'Serum antioxidante para luminosidad facial.', 18990::numeric, 20, 5, 'Proveedor estetico local'),
    ('solar-care', 'Protector solar FPS 50', 'SOLAR-FPS50-050', 'Protector solar recomendado para uso diario y post tratamiento.', 15990::numeric, 35, 8, 'Proveedor dermocosmetico'),
    ('facial-care', 'Mascarilla hidratante', 'MASC-HID-001', 'Mascarilla facial hidratante para cuidado semanal.', 8990::numeric, 25, 5, 'Proveedor estetico local'),
    ('body-care', 'Aceite corporal relajante', 'ACEITE-REL-100', 'Aceite corporal para masajes relajantes.', 10990::numeric, 18, 4, 'Proveedor spa'),
    ('hair-care', 'Shampoo reparador post tratamiento', 'SHAMP-REP-250', 'Producto capilar de apoyo para tratamientos de peluqueria.', 11990::numeric, 22, 5, 'Proveedor capilar'),
    ('nails', 'Esmalte permanente nude', 'ESM-PERM-NUDE', 'Esmalte permanente para manicure.', 6990::numeric, 40, 10, 'Proveedor manicure'),
    ('gift-cards', 'Gift card $30.000', 'GIFT-30000', 'Tarjeta de regalo aplicable a servicios y productos.', 30000::numeric, 100, 1, 'Centro estetico')
) as seed(category_code, name, sku, description, price, stock_quantity, stock_minimum, supplier)
where pc.code = seed.category_code
  and not exists (
      select 1
      from product_service ps
      where ps.business_id = b.id
        and ps.sku = seed.sku
  );
