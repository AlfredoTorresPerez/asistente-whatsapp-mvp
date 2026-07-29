drop index if exists uq_booking_customer_professional_active;

create unique index uq_booking_customer_professional_active
    on booking (customer_id, professional_id, starts_at)
    where professional_id is not null
      and status not in ('CANCELADA', 'CANCELADA_POR_CLIENTE', 'EXPIRADA', 'ATENDIDA', 'NO_ASISTE');
