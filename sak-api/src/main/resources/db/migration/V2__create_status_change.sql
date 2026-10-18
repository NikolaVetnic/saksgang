create table status_change (
    id                   uuid        primary key,
    grant_application_id uuid        not null references grant_application (id),
    from_status          varchar(20) not null
        check (from_status in ('RECEIVED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    to_status            varchar(20) not null
        check (to_status in ('RECEIVED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    reason               text        not null,
    changed_at           timestamptz not null
);

create index status_change_grant_application_id_idx on status_change (grant_application_id);
