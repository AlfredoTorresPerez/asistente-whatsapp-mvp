alter table booking
    drop constraint if exists chk_booking_status;

alter table booking
    add constraint chk_booking_status check (
        status in (
            'SOLICITADA',
            'PENDIENTE_CONFIRMACION',
            'PENDIENTE_PAGO',
            'CONFIRMADA',
            'REPROGRAMACION_PENDIENTE',
            'REPROGRAMADA',
            'CANCELADA',
            'CANCELADA_POR_CLIENTE',
            'EXPIRADA',
            'ATENDIDA',
            'NO_ASISTE'
        )
    );
