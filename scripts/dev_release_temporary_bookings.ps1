$ErrorActionPreference = "Stop"

$ComposeFile = "docker-compose.local.yml"
$TargetDate = if ($args.Count -ge 1) { $args[0] } else { "2026-06-12" }

@"
update booking_confirmation_link l
set status = 'EXPIRED',
    expired_at = current_timestamp,
    updated_at = current_timestamp
from booking b
where l.booking_id = b.id
  and l.business_id = b.business_id
  and b.starts_at::date = date '$TargetDate'
  and b.status = 'TEMPORARY'
  and l.status in ('GENERATED', 'SENT', 'OPENED');

update booking
set status = 'RELEASED',
    updated_at = current_timestamp
where starts_at::date = date '$TargetDate'
  and status = 'TEMPORARY';
"@ | docker compose -f $ComposeFile exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

Write-Host "Reservas temporales liberadas para $TargetDate"
