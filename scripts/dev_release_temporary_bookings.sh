#!/usr/bin/env bash
set -euo pipefail
COMPOSE_FILE="docker-compose.local.yml"
TARGET_DATE="${1:-2026-06-12}"
cat <<SQL | docker compose -f "$COMPOSE_FILE" exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
update booking_confirmation_link l
set status = 'EXPIRED',
    expired_at = current_timestamp,
    updated_at = current_timestamp
from booking b
where l.booking_id = b.id
  and l.business_id = b.business_id
  and b.starts_at::date = date '$TARGET_DATE'
  and b.status = 'TEMPORARY'
  and l.status in ('GENERATED', 'SENT', 'OPENED');

update booking
set status = 'RELEASED',
    updated_at = current_timestamp
where starts_at::date = date '$TARGET_DATE'
  and status = 'TEMPORARY';
SQL
printf 'Reservas temporales liberadas para %s\n' "$TARGET_DATE"
