create type storage_action as enum ('get', 'delete');

create table commands_invoke_storage (
  id uuid primary key,
  did uuid not null,
  preid uuid not null,
  action storage_action not null,
  date timestamp not null,
  retries smallint not null default 0,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
  constraint privacy_response_event_fk
    foreign key (preid)
    references privacy_response_events(id)
    on delete cascade
);