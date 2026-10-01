-- Active: 1790841303608@@127.0.0.1@5432@todo_list
create table if not exists users (
    id uuid primary key default gen_random_uuid(),
    email varchar(255) not null unique,
    username varchar(255) not null,
    password_hash varchar(60),
    created_at timestamptz default now(),
    updated_at timestamptz
);

create table if not exists todos (
    id uuid primary key default gen_random_uuid(),
    id_user uuid not null references users(id) on delete cascade,
    title varchar(255) not null,
    description text,
    is_done boolean default false,
    created_at timestamptz default now(),
    due_date timestamptz not null
);

create table if not exists events (
    id uuid primary key default gen_random_uuid(),
    id_todo uuid not null references todos(id) on delete cascade,
    title varchar(255) not null,
    description text,
    start_time timestamptz not null,
    end_time timestamptz not null
);