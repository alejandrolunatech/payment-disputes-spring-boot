create table transactions (
    reference  varchar(40)    primary key,
    amount     numeric(12, 2) not null check (amount > 0),
    currency   varchar(3)       not null,
    merchant   varchar(120)   not null,
    booked_on  date           not null
);

create table disputes (
    id                    uuid           primary key,
    transaction_reference varchar(40)    not null references transactions (reference),
    disputed_amount       numeric(12, 2) not null check (disputed_amount > 0),
    currency              varchar(3)       not null,
    masked_statement      text           not null,
    status                varchar(30)    not null,
    reason_code           varchar(40),
    triage_source         varchar(20),
    triage_confidence     numeric(4, 3),
    review_note           varchar(300),
    opened_at             timestamptz    not null,
    response_deadline     timestamptz,
    idempotency_key       varchar(100)   not null unique,
    request_fingerprint   varchar(64)    not null,
    version               bigint         not null
);

create index disputes_needing_triage on disputes (opened_at) where status = 'NEEDS_TRIAGE';

create table model_calls (
    id             bigserial    primary key,
    called_at      timestamptz  not null,
    model          varchar(100) not null,
    input_tokens   integer      not null,
    output_tokens  integer      not null,
    latency_ms     bigint       not null,
    outcome        varchar(30)  not null,
    detail         varchar(300)
);
