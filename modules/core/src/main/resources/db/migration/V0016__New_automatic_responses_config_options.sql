alter table automatic_responses_config
  rename column auto_consents to auto_revoke_consent;

alter table automatic_responses_config
  add auto_restrict boolean not null,
  add auto_object boolean not null;