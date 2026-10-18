create table grant_application (
    id                  uuid         primary key,
    organization_number varchar(9)   not null check (organization_number ~ '^[0-9]{9}$'),
    organization_name   varchar(255) not null,
    applicant_name      varchar(255) not null,
    applicant_email     varchar(254) not null,
    amount              bigint       not null check (amount > 0),
    purpose             text         not null,
    status              varchar(20)  not null
        check (status in ('RECEIVED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    created_at          timestamptz  not null,
    updated_at          timestamptz  not null
);

create index grant_application_status_idx on grant_application (status);
