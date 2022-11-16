alter table privacy_requests
  alter column date type timestamptz;

alter table demand_restrictions
  alter column from_date type timestamptz,
  alter column to_date type timestamptz;

alter table demand_recommendations
  alter column date_from type timestamptz,
  alter column date_to type timestamptz;

alter table privacy_response_events
  alter column date type timestamptz;

alter table commands_create_recommendation
  alter column date type timestamptz;

alter table commands_create_response
  alter column date type timestamptz;

alter table legal_base_events
  alter column date type timestamptz;

alter table consent_given_events
  alter column date type timestamptz;

alter table consent_revoked_events
  alter column date type timestamptz;

alter table object_events
  alter column date type timestamptz;

alter table restrict_events
  alter column date type timestamptz;