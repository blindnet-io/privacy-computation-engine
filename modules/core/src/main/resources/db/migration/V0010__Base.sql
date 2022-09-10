
create table apps (
  id uuid primary key,
  active boolean default true
);

create table dac (
  appid uuid unique not null,
  active boolean not null,
  uri varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table automatic_responses_config (
  appid uuid unique not null,
  auto_transparency boolean not null,
  auto_consents boolean not null,
  auto_access boolean not null,
  auto_delete boolean not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

-- GENERAL INFORMATION
-- parts of TRANSPARENCY requests and ROPA

create table general_information (
  id uuid primary key,
  appid uuid not null,
  organization varchar not null,
  dpo varchar not null,
  countries varchar[] not null,
  data_consumer_categories varchar[] not null,
  access_policies varchar[] not null,
  privacy_policy_link varchar,
  data_security_information varchar,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

-- PRIVACY SCOPE

create table data_categories (
  id uuid primary key,
  term varchar unique not null,
  selector boolean not null default false, -- selector
  appid uuid, -- selector
  active boolean not null default true, -- selector
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create unique index uniq_selector_name on data_categories (appid, term, active);

create table processing_categories (
  id uuid primary key,
  term VARCHAR unique not null
);

create table processing_purposes (
  id uuid primary key,
  term VARCHAR unique not null
);

create table scope (
  id uuid primary key,
  dcid uuid not null,
  pcid uuid not null,
  ppid uuid not null,
  appid uuid,
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
    on delete cascade,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

insert into data_categories (id, term) values (gen_random_uuid(), '*'), (gen_random_uuid(), 'AFFILIATION'), (gen_random_uuid(), 'AFFILIATION.MEMBERSHIP'), (gen_random_uuid(), 'AFFILIATION.MEMBERSHIP.UNION'), (gen_random_uuid(), 'AFFILIATION.SCHOOL'), (gen_random_uuid(), 'AFFILIATION.WORKPLACE'), (gen_random_uuid(), 'BEHAVIOR'), (gen_random_uuid(), 'BEHAVIOR.ACTIVITY'), (gen_random_uuid(), 'BEHAVIOR.CONNECTION'), (gen_random_uuid(), 'BEHAVIOR.PREFERENCE'), (gen_random_uuid(), 'BEHAVIOR.TELEMETRY'), (gen_random_uuid(), 'BIOMETRIC'), (gen_random_uuid(), 'CONTACT'), (gen_random_uuid(), 'CONTACT.EMAIL'), (gen_random_uuid(), 'CONTACT.ADDRESS'), (gen_random_uuid(), 'CONTACT.PHONE'), (gen_random_uuid(), 'DEMOGRAPHIC'), (gen_random_uuid(), 'DEMOGRAPHIC.AGE'), (gen_random_uuid(), 'DEMOGRAPHIC.BELIEFS'), (gen_random_uuid(), 'DEMOGRAPHIC.GENDER'), (gen_random_uuid(), 'DEMOGRAPHIC.ORIGIN'), (gen_random_uuid(), 'DEMOGRAPHIC.RACE'), (gen_random_uuid(), 'DEMOGRAPHIC.SEXUAL-ORIENTATION'), (gen_random_uuid(), 'DEVICE'), (gen_random_uuid(), 'FINANCIAL'), (gen_random_uuid(), 'FINANCIAL.BANK-ACCOUNT'), (gen_random_uuid(), 'GENETIC'), (gen_random_uuid(), 'HEALTH'), (gen_random_uuid(), 'IMAGE'), (gen_random_uuid(), 'LOCATION'), (gen_random_uuid(), 'NAME'), (gen_random_uuid(), 'PROFILING'), (gen_random_uuid(), 'RELATIONSHIPS'), (gen_random_uuid(), 'UID'), (gen_random_uuid(), 'UID.ID'), (gen_random_uuid(), 'UID.IP'), (gen_random_uuid(), 'UID.USER-ACCOUNT'), (gen_random_uuid(), 'UID.SOCIAL-MEDIA'), (gen_random_uuid(), 'OTHER-DATA');

insert into processing_categories values (gen_random_uuid(), '*'), (gen_random_uuid(), 'ANONYMIZATION'), (gen_random_uuid(), 'AUTOMATED-INFERENCE'), (gen_random_uuid(), 'AUTOMATED-DECISION-MAKING'), (gen_random_uuid(), 'COLLECTION'), (gen_random_uuid(), 'GENERATING'), (gen_random_uuid(), 'PUBLISHING'), (gen_random_uuid(), 'STORING'), (gen_random_uuid(), 'SHARING'), (gen_random_uuid(), 'USING'), (gen_random_uuid(), 'OTHER-PROCESSING');

insert into processing_purposes values (gen_random_uuid(), '*'), (gen_random_uuid(), 'ADVERTISING'), (gen_random_uuid(), 'COMPLIANCE'), (gen_random_uuid(), 'EMPLOYMENT'), (gen_random_uuid(), 'JUSTICE'), (gen_random_uuid(), 'MARKETING'), (gen_random_uuid(), 'MEDICAL'), (gen_random_uuid(), 'PERSONALIZATION'), (gen_random_uuid(), 'PUBLIC-INTERESTS'), (gen_random_uuid(), 'RESEARCH'), (gen_random_uuid(), 'SALE'), (gen_random_uuid(), 'SECURITY'), (gen_random_uuid(), 'SERVICES'), (gen_random_uuid(), 'SERVICES.ADDITIONAL-SERVICES'), (gen_random_uuid(), 'SERVICES.BASIC-SERVICE'), (gen_random_uuid(), 'SOCIAL-PROTECTION'), (gen_random_uuid(), 'TRACKING'), (gen_random_uuid(), 'VITAL-INTERESTS'), (gen_random_uuid(), 'OTHER-PURPOSE');

insert into "scope" (
  select gen_random_uuid() as id, dc.id as dcid, pc.id as pcid, pp.id as ppid
  from data_categories dc, processing_categories pc, processing_purposes pp
);


-- PROVENANCES

create type target_terms as enum ('*', 'ORGANIZATION', 'PARTNERS', 'SYSTEM', 'PARTNERS.DOWNWARD', 'PARTNERS.UPWARD');
create type provenance_terms as enum ('*', 'DERIVED', 'TRANSFERRED', 'USER', 'USER.DATA-SUBJECT');

create table provenances (
  id uuid primary key,
  appid uuid not null,
  dcid uuid not null,
  provenance provenance_terms not null,
  system varchar not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade,
  constraint data_category_fk
    foreign key (dcid)
    references data_categories(id)
    on delete cascade
);

-- RETENTION POLICIES

create type policy_terms as enum ('NO-LONGER-THAN', 'NO-LESS-THAN');
create type event_terms as enum ('CAPTURE-DATE', 'RELATIONSHIP-END', 'RELATIONSHIP-START', 'SERVICE-END', 'SERVICE-START');

create table retention_policies (
  id uuid primary key,
  appid uuid not null,
  dcid uuid not null,
  policy policy_terms not null,
  duration varchar not null, -- https://www.rfc-editor.org/rfc/pdfrfc/rfc3339.txt.pdf duration in appendix a
  after event_terms not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade,
  constraint data_category_fk
    foreign key (dcid)
    references data_categories(id)
    on delete cascade
);


-- LEGAL BASES

create type legal_base_terms as enum ('CONTRACT', 'CONSENT', 'LEGITIMATE-INTEREST', 'NECESSARY', 'NECESSARY.LEGAL-OBLIGATION', 'NECESSARY.PUBLIC-INTEREST', 'NECESSARY.VITAL-INTEREST', 'OTHER-LEGAL-BASE');

create table legal_bases (
  id uuid primary key,
  appid uuid not null,
  type legal_base_terms not null,
  name varchar,
  description varchar,
  active boolean default true,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table legal_bases_scope (
  lbid uuid not null,
  scid uuid not null,
  constraint legal_bases_scope_pk
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


-- REGULATIONS

create table regulations (
  id uuid primary key,
  name varchar not null,
  description varchar
);

create table regulation_legal_base_must_include_scope (
  rid uuid not null,
  legal_base legal_base_terms not null,
  scid uuid not null,
  constraint regulation_fk
    foreign key (rid)
    references regulations(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create table regulation_legal_base_forbidden_scope (
  rid uuid not null,
  legal_base legal_base_terms not null,
  scid uuid not null,
  constraint regulation_fk
    foreign key (rid)
    references regulations(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create table app_regulations (
  appid uuid not null,
  rid uuid not null,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade,
  constraint regulation_fk
    foreign key (rid)
    references regulations(id)
    on delete cascade
);


-- DATA SUBJECT

create table data_subjects (
  id varchar not null,
  appid uuid not null,
  schema varchar,
  constraint data_subjects_pk
    primary key (id, appid),
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

-- PRIVACY REQUEST

create type action_terms as enum ('ACCESS', 'DELETE', 'MODIFY', 'OBJECT', 'PORTABILITY', 'RESTRICT',
'REVOKE-CONSENT', 'TRANSPARENCY', 'TRANSPARENCY.DATA-CATEGORIES', 'TRANSPARENCY.DPO', 'TRANSPARENCY.KNOWN', 'TRANSPARENCY.LEGAL-BASES', 'TRANSPARENCY.ORGANIZATION', 'TRANSPARENCY.POLICY', 'TRANSPARENCY.PROCESSING-CATEGORIES', 'TRANSPARENCY.PROVENANCE', 'TRANSPARENCY.PURPOSE', 'TRANSPARENCY.RETENTION', 'TRANSPARENCY.WHERE', 'TRANSPARENCY.WHO');

create table privacy_requests (
  id uuid primary key,
  appid uuid not null,
  dsid varchar,
  provided_dsids varchar[],
  date timestamp not null,
  target target_terms not null,
  email varchar,
  constraint data_subject_fk
    foreign key (dsid, appid)
    references data_subjects(id, appid)
    on delete restrict,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create table demands (
  id uuid primary key,
  prid uuid not null,
  action action_terms not null,
  message varchar,
  lang varchar,
  constraint privacy_request_fk
    foreign key (prid)
    references privacy_requests(id)
    on delete cascade
);

create type restriction_type as enum ('PRIVACY_SCOPE', 'CONSENT', 'DATE_RANGE', 'PROVENANCE', 'DATA_REFERENCE');

create table demand_restrictions(
  id uuid primary key,
  did uuid not null,
  type restriction_type not null,
  cid uuid,
  from_date timestamp,
  to_date timestamp,
  provenance_term provenance_terms,
  target_term target_terms,
  data_reference varchar[],
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
  constraint consent_fk
    foreign key (cid)
    references legal_bases(id)
    on delete cascade
);

create table demand_restriction_scope (
  drid uuid not null,
  scid uuid not null,
  constraint demand_restriction_scope_pk
    primary key (drid, scid),
  constraint demand_restriction_fk
    foreign key (drid)
    references demand_restrictions(id)
    on delete cascade,
  constraint scope_fk
    foreign key (scid)
    references scope(id)
    on delete cascade
);

create type motive_terms as enum ('IDENTITY-UNCONFIRMED', 'LANGUAGE-UNSUPPORTED', 'VALID-REASONS', 'IMPOSSIBLE', 'NO-SUCH-DATA', 'REQUEST-UNSUPPORTED', 'USER-UNKNOWN', 'OTHER-MOTIVE');

create type status_terms as enum ('GRANTED', 'DENIED', 'PARTIALLY-GRANTED', 'UNDER-REVIEW', 'CANCELED');

create table demand_recommendations (
  id uuid primary key,
  did uuid not null,
  status status_terms,
  motive motive_terms,
  data_categories varchar[],
  date_from timestamp,
  date_to timestamp,
  provenance provenance_terms,
  target target_terms,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

-- PRIVACY RESPONSE

-- per demand
create table privacy_responses (
  id uuid primary key,
  did uuid not null,
  parent uuid, -- included in
  action action_terms not null,
  system varchar,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
  constraint included_in_fk
    foreign key (parent)
    references privacy_responses(id)
    on delete cascade
);

create table privacy_response_events (
  id uuid primary key,
  prid uuid not null,
  date timestamp not null,
  status status_terms not null,
  motive motive_terms,
  message varchar,
  lang varchar,
  answer varchar,
  constraint privacy_response_fk
    foreign key (prid)
    references privacy_responses(id)
    on delete cascade
);

create table privacy_response_events_data (
  preid uuid not null,
  data varchar,
  constraint privacy_response_event_fk
    foreign key (preid)
    references privacy_response_events(id)
    on delete cascade
);


create table pending_demands_to_review (
  did uuid unique not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

create table commands_create_recommendation (
  id uuid primary key,
  did uuid not null,
  date timestamp not null,
  data jsonb,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

create table commands_create_response (
  id uuid primary key,
  did uuid not null,
  date timestamp not null,
  data jsonb,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);


-- EVENT

-- this needs a better model
-- a single table with many columns?

create table legal_base_events (
  id uuid primary key,
  lbid uuid not null,
  dsid varchar not null,
  appid uuid not null,
  event event_terms not null,
  date timestamp not null,
  constraint legal_base_fk
    foreign key (lbid)
    references legal_bases(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid, appid)
    references data_subjects(id, appid)
    on delete restrict
);

create table consent_given_events (
  id uuid primary key,
  lbid uuid not null,
  dsid varchar not null,
  appid uuid not null,
  date timestamp not null,
  constraint legal_base_fk
    foreign key (lbid)
    references legal_bases(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid, appid)
    references data_subjects(id, appid)
    on delete restrict
);

create table consent_revoked_events (
  id uuid primary key,
  lbid uuid not null,
  dsid varchar not null,
  appid uuid not null,
  date timestamp not null,
  constraint legal_base_fk
    foreign key (lbid)
    references legal_bases(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid, appid)
    references data_subjects(id, appid)
    on delete restrict
);

create table object_events (
  id uuid primary key,
  did uuid not null,
  dsid varchar not null,
  appid uuid not null,
  date timestamp not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid, appid)
    references data_subjects(id, appid)
    on delete restrict
);

create table restrict_events (
  id uuid primary key,
  did uuid not null,
  dsid varchar not null,
  appid uuid not null,
  date timestamp not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid, appid)
    references data_subjects(id, appid)
    on delete restrict
);
