alter table booking
    drop constraint if exists chk_booking_status;

update booking
set status = 'CONFIRMED'
where status = 'SCHEDULED';

alter table booking
    add constraint chk_booking_status
        check (status in ('REQUESTED', 'CONFIRMED', 'RESCHEDULED', 'CANCELLED', 'COMPLETED'));
