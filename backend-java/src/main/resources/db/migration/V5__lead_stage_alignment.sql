alter table lead
drop constraint if exists chk_lead_stage;

update lead
set stage = 'INTERESTED'
where stage = 'QUALIFIED';

update lead
set stage = 'SCHEDULED'
where stage = 'PROPOSAL';

alter table lead
add constraint chk_lead_stage
check (stage in ('NEW', 'CONTACTED', 'INTERESTED', 'SCHEDULED', 'WON', 'LOST'));
