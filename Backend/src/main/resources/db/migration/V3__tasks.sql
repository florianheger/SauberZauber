create table task
(
    id          bigserial primary key,
    title       varchar(100) not null,
    description varchar(500),
    icon        varchar(64)  not null,
    created_at  timestamptz  not null default now()
);

create table completion
(
    id      bigserial primary key,
    task_id bigint      not null references task (id) on delete cascade,
    user_id bigint      not null references app_user (id),
    done_at timestamptz not null default now()
);

create index completion_task_id_idx on completion (task_id);
