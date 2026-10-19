create table push_subscription
(
    id         bigserial primary key,
    user_id    bigint      not null references app_user (id) on delete cascade,
    endpoint   text        not null unique,
    p256dh     text        not null,
    auth       text        not null,
    created_at timestamptz not null default now()
);

create index push_subscription_user_id_idx on push_subscription (user_id);
