
create table apps (
  id uuid primary key
);

-- GENERAL INFORMATION
-- parts of TRANSPARENCY requests and ROPA

create table general_information (
  id uuid primary key,
  appid uuid not null,
  countries varchar[],
  data_consumer_categories varchar[],
  access_policies varchar[],
  privacy_policy_link varchar,
  data_security_information varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table general_information_organization (
  id uuid primary key,
  gid uuid not null,
  name varchar,
  constraint general_information_fk
    foreign key (gid)
    references general_information(id)
    on delete cascade
);
 
create table dpo (
  id uuid primary key,
  gid uuid not null,
  name varchar,
  contact varchar,
  constraint general_information_fk
    foreign key (gid)
    references general_information(id)
    on delete cascade
);

-- PRIVACY SCOPE

create table data_categories (
  id uuid primary key,
  appid uuid not null,
  term varchar unique not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table processing_categories (
  id uuid primary key,
  appid uuid not null,
  term VARCHAR unique not null
);

create table processing_purposes (
  id uuid primary key,
  appid uuid not null,
  term VARCHAR unique not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table scope (
  id uuid primary key,
  dcid uuid not null,
  pcid uuid not null,
  ppid uuid not null,
  constraint data_category_fk
    foreign key (dcid)
    references data_categories(id)
    on delete cascade,
  constraint processing_category_fk
    foreign key (pcid)
    references processing_categories(id)
    on delete cascade,
  constraint processing_purpose_fk
    foreign key (ppid)
    references processing_purposes(id)
    on delete cascade
  -- constraint scope_pk
  --     primary key (user_id, app_id, device_id, id),
);

-----------------

-- SELECTOR

create type target_terms as enum ('ORGANIZATION', 'PARTNERS', 'SYSTEM', 'PARTNERS.DOWNWARD', 'PARTNERS.UPWARD');
create type policy_terms as enum ('NO-LONGER-THAN, NO-LESS-THAN');
create type event_terms as enum ('CAPTURE-DATE', 'RELATIONSHIP-END', 'RELATIONSHIP-START', 'SERVICE-END', 'SERVICE-START');

create table selectors (
  id uuid primary key,
  appid uuid not null,
  name varchar,
  target target_terms,
  provenance varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table selector_scope (
  slid uuid not null,
  scid uuid not null,
  constraint selector_scope_pk
    primary key (slid, scid),
  constraint selector_fk
    foreign key (slid)
    references selectors(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create table retention_policies (
  id uuid primary key,
  sid uuid unique,
  policy policy_terms not null,
  duration integer not null, -- for now days, should be https://www.rfc-editor.org/rfc/pdfrfc/rfc3339.txt.pdf duration in appendix a
  after event_terms not null,
  constraint selector_fk
    foreign key (slid)
    references selectors(id)
    on delete cascade
)

-----------------

-- LEGAL BASES

-- very likely, particular legal bases will have additional properties
create table legal_bases (
  id uuid primary key,
  appid uuid not null,
  name varchar,
  description varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table legitimate_interests (
) inherits(legal_bases);

create table necessary_legal_bases (
) inherits(legal_bases);

create table contracts (
) inherits(legal_bases);

create table consents (
) inherits(legal_bases);

create table legal_base_scope (
  lbid uuid not null,
  scid uuid not null,
  constraint legal_base_scope_pk
    primary key (lbid, scid),
  constraint legal_base_fk
    foreign key (lbid)
    references legal_bases(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);



-----------------

-- DATA SUBJECT

create table data_subjects (
  id uuid primary key,
  appid uuid not null,
  schema varchar unique not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

-----------------

-- EVENT

create table events (
  id uuid primary key,
  dsid uuid not null,
  date timestamp not null,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table legal_base_event (
  event event_terms not null,
) inherits(events);

create table consent_event (
  -- type given or inferred
) inherits(events);

create table privacy_request_event (
) inherits(events);

create table privacy_response_event (
) inherits(events);

-----------------

-- PRIVACY REQUEST

create table privacy_requests (
  id uuid primary key,
  appid uuid not null,
  dsid uuid not null,
  date timestamp not null,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table demands (
  id uuid primary key,
  prid uuid not null,
  action varchar not null, -- TODO: enum
  message varchar,
  lang varchar,
  constraint privacy_request_fk
    foreign key (prid)
    references privacy_requests(id)
    on delete cascade
);

create table restrictions (
  id uuid primary key,
  did uuid not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

create table privacy_scope_restriction (
) inherits(restrictions);

create table privacy_scope_restriction_scope (
  psrid uuid not null,
  scid uuid not null,
  constraint privacy_scope_restriction_scope_pk
    primary key (psrid, scid),
  constraint privacy_scope_restriction_fk
    foreign key (psrid)
    references restrictions(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
) inherits(restrictions);

create table consent_restriction (
  cid uuid not null,
  constraint consent_event_fk
    foreign key (cid)
    references events(id)
    on delete cascade
) inherits(restrictions);

create table date_range_restriction (
  from_timestamp timestamp,
  to_timestamp timestamp
) inherits(restrictions);

create table provenance_restriction (
  provenance_term varchar not null,
  target_term target_terms
) inherits(restrictions);

create table data_reference_restriction (
) inherits(restrictions);

-----------------


