alter table if exists lead drop constraint if exists chk_lead_source_type;
alter table if exists lead add constraint chk_lead_source_type check (source_type in ('MANUAL', 'CONVERSATION', 'LANDING_PAGE'));
