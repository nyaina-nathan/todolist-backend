create table if not exists refresh_token (
    id uuid primary key default gen_random_uuid(),
    id_user uuid not null references users(id) on delete cascade,
    value varchar not null,
    issued_at timestamptz default now(),
    expires_at timestamptz not null
);