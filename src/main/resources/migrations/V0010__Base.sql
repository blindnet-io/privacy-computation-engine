
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
  term VARCHAR unique not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
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
    foreign key (sid)
    references selectors(id)
    on delete cascade
);

-----------------

-- LEGAL BASES

create table legitimate_interests (
  id uuid primary key,
  appid uuid not null,
  name varchar,
  description varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table legitimate_interests_scope (
  liid uuid not null,
  scid uuid not null,
  constraint legitimate_interests_scope_pk
    primary key (liid, scid),
  constraint legitimate_interest_fk
    foreign key (liid)
    references legitimate_interests(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create table necessary_legal_bases (
  id uuid primary key,
  appid uuid not null,
  name varchar,
  description varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table necessary_legal_bases_scope (
  nlbid uuid not null,
  scid uuid not null,
  constraint necessary_legal_bases_scope_pk
    primary key (nlbid, scid),
  constraint necessary_legal_base_fk
    foreign key (nlbid)
    references necessary_legal_bases(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create table contracts (
  id uuid primary key,
  appid uuid not null,
  name varchar,
  description varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table contracts_scope (
  cid uuid not null,
  scid uuid not null,
  constraint contracts_scope_pk
    primary key (cid, scid),
  constraint contract_fk
    foreign key (cid)
    references contracts(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create table consents (
  id uuid primary key,
  appid uuid not null,
  name varchar,
  description varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table consents_scope (
  cid uuid not null,
  scid uuid not null,
  constraint consents_scope_pk
    primary key (cid, scid),
  constraint consent_fk
    foreign key (cid)
    references consents(id)
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

create table legal_base_event (
  id uuid primary key,
  dsid uuid not null,
  event event_terms not null,
  date timestamp not null,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table consent_event (
  -- type given or inferred
  id uuid primary key,
  dsid uuid not null,
  date timestamp not null,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table privacy_request_event (
  id uuid primary key,
  dsid uuid not null,
  date timestamp not null,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table privacy_response_event (
  id uuid primary key,
  dsid uuid not null,
  date timestamp not null,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

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

create table privacy_scope_restriction (
  id uuid primary key,
  did uuid not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

create table privacy_scope_restriction_scope (
  psrid uuid not null,
  scid uuid not null,
  constraint privacy_scope_restriction_scope_pk
    primary key (psrid, scid),
  constraint privacy_scope_restriction_fk
    foreign key (psrid)
    references privacy_scope_restriction(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create table consent_restriction (
  id uuid primary key,
  did uuid not null,
  cid uuid not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
  constraint consent_event_fk
    foreign key (cid)
    references consent_event(id)
    on delete cascade
);

create table date_range_restriction (
  id uuid primary key,
  did uuid not null,
  from_timestamp timestamp,
  to_timestamp timestamp,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

create table provenance_restriction (
  id uuid primary key,
  did uuid not null,
  provenance_term varchar not null,
  target_term target_terms,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

create table data_reference_restriction (
  id uuid primary key,
  did uuid not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);


create view legal_bases as
	select id, appid, 'contract' from contracts
	union select id, appid, 'necessary' from necessary_legal_bases
	union select id, appid, 'legitimate' from legitimate_interests
	union select id, appid, 'consent' from consents;

-----------------


