-- V49: Unify business hours — single source of truth
-- agenda_business_hours and agenda_professional_hours are the official sources
-- Migrate data from professional_location_schedule, drop it, remove dead columns

-- 1. Clear seed data that was injected by V20/V31/V41 — from now on managed by UI/CRUD
delete from agenda_business_hours;
delete from agenda_professional_hours;

-- 2. Migrate existing professional_location_schedule data into agenda_professional_hours
insert into agenda_professional_hours (
    id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active, created_at, updated_at
)
select
    id, business_id, professional_id, location_id, day_of_week, start_time, end_time, active, created_at, updated_at
from professional_location_schedule
on conflict (business_id, professional_id, location_id, day_of_week, start_time, end_time) do nothing;

-- 3. Drop the now-unused professional_location_schedule table
drop table if exists professional_location_schedule;

-- 4. Remove dead opening_hours JSONB column from business_location
alter table business_location drop column if exists opening_hours;
