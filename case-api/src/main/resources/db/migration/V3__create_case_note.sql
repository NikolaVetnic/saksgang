create table case_note (
    id                   uuid        primary key,
    grant_application_id uuid        not null references grant_application (id),
    text                 text        not null,
    created_at           timestamptz not null
);

create index case_note_grant_application_id_idx on case_note (grant_application_id);
