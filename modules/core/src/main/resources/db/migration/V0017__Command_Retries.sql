alter table commands_create_recommendation
  add retries smallint not null default 0;

alter table commands_create_response
  add retries smallint not null default 0;