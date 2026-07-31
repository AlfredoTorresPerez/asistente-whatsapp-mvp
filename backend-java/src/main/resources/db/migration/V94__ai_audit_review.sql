alter table ai_agent_decision_log
    add column if not exists source varchar(30);

alter table ai_agent_decision_log
    add column if not exists reviewed boolean not null default false;

alter table ai_agent_decision_log
    add column if not exists reviewed_by uuid;

alter table ai_agent_decision_log
    add column if not exists reviewed_at timestamp with time zone;

alter table ai_agent_decision_log
    add column if not exists review_result varchar(20);

alter table ai_agent_decision_log
    add column if not exists review_note text;

create table if not exists ai_decision_review (
    id uuid primary key,
    decision_id uuid not null,
    business_id uuid not null,
    reviewed_by uuid not null,
    review_result varchar(20) not null,
    original_response text,
    corrected_response text,
    missing_data_added jsonb not null default '[]'::jsonb,
    note text,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_ai_decision_review_decision
        foreign key (decision_id) references ai_agent_decision_log (id) on delete cascade,
    constraint fk_ai_decision_review_business
        foreign key (business_id) references business (id) on delete cascade,
    constraint fk_ai_decision_review_user
        foreign key (reviewed_by) references user_account (id) on delete cascade
);

create index idx_ai_decision_review_decision_id on ai_decision_review (decision_id);
create index idx_ai_agent_decision_log_source on ai_agent_decision_log (source);
create index idx_ai_agent_decision_log_reviewed on ai_agent_decision_log (business_id, reviewed);
