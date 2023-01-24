alter type storage_action
  add value 'privacy-scope';

alter table commands_invoke_storage
  add data jsonb not null;
