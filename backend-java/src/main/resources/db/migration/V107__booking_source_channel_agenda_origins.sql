alter table booking
    drop constraint if exists chk_booking_source_channel,
    add constraint chk_booking_source_channel
        check (source_channel in (
            'WHATSAPP',
            'ADMIN',
            'SYSTEM',
            'AGENDA',
            'WEB',
            'TELEFONO',
            'PRESENCIAL'
        ));
