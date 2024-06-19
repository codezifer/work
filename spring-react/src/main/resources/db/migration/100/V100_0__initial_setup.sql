create schema if not exists tutorials;

create table if not exists tutorials.tutorial (
    id bigserial primary key,
    title text not null,
    description text not null,
    published boolean not null default false,
    created timestamptz not null default current_timestamp,
    modified timestmaptz not null default current_timestamp,
    unique(title)
);