
create table apps (
  id uuid primary key,
  active boolean default true
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
  term varchar unique not null
);

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
);

-----------------

-- SELECTOR

create type target_terms as enum ('ORGANIZATION', 'PARTNERS', 'SYSTEM', 'PARTNERS.DOWNWARD', 'PARTNERS.UPWARD');
create type policy_terms as enum ('NO-LONGER-THAN', 'NO-LESS-THAN');
create type event_terms as enum ('CAPTURE-DATE', 'RELATIONSHIP-END', 'RELATIONSHIP-START', 'SERVICE-END', 'SERVICE-START');

create table selectors (
  id uuid primary key,
  appid uuid not null,
  name varchar,
  target target_terms,
  provenance varchar,
  active boolean default true,
  constraint app_fk
    foreign key (appid)
    references apps(id)
    on delete cascade
);

create unique index uniq_selector_name on selectors (appid, name, active);

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
  sid uuid not null,
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
  subcat varchar not null default 'LEGITIMATE-INTEREST',
  name varchar,
  description varchar,
  active boolean default true,
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
  subcat varchar not null default 'NECESSARY',
  name varchar,
  description varchar,
  active boolean default true,
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
  subcat varchar not null default 'CONTRACT',
  name varchar,
  description varchar,
  active boolean default true,
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
  subcat varchar not null default 'CONSENT',
  name varchar,
  description varchar,
  active boolean default true,
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

-- this needs a better model
-- a single table with many columns?

create table necessary_legal_base_events (
  id uuid primary key,
  nlbid uuid not null,
  dsid uuid not null,
  event event_terms not null,
  date timestamp not null,
  constraint necessary_legal_base_fk
    foreign key (nlbid)
    references necessary_legal_bases(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table legitimate_interest_events (
  id uuid primary key,
  liid uuid not null,
  dsid uuid not null,
  event event_terms not null,
  date timestamp not null,
  constraint legitimate_interest_fk
    foreign key (liid)
    references legitimate_interests(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table contract_events (
  id uuid primary key,
  cbid uuid not null,
  dsid uuid not null,
  event event_terms not null,
  date timestamp not null,
  constraint contract_fk
    foreign key (cbid)
    references contracts(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table consent_given_events (
  id uuid primary key,
  cbid uuid not null,
  dsid uuid not null,
  constraint cosent_fk
    foreign key (cbid)
    references consents(id)
    on delete cascade,
  date timestamp not null,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table consent_revoked_events (
  id uuid primary key,
  cbid uuid not null,
  dsid uuid not null,
  date timestamp not null,
  constraint cosent_fk
    foreign key (cbid)
    references consents(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table object_events (
  id uuid primary key,
  did uuid not null,
  dsid uuid not null,
  date timestamp not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table restrict_events (
  id uuid primary key,
  did uuid not null,
  dsid uuid not null,
  date timestamp not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade,
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

-- PRIVACY RESPONSE

create type status_terms as enum ('GRANTED', 'DENIED', 'PARTIALLY-GRANTED', 'UNDER-REVIEW');

-- per demand
create table privacy_response (
  id uuid primary key,
  did uuid not null,
  constraint demand_fk
    foreign key (did)
    references demands(id)
    on delete cascade
);

create table privacy_response_event (
  id uuid primary key,
  prid uuid not null,
  date timestamp not null,
  status status_terms not null,
  message varchar,
  lang varchar not null,
  constraint privacy_response_fk
    foreign key (prid)
    references privacy_response(id)
    on delete cascade
);
-----------------

-- VIEWS

-- materialized view is a better performant alternative
-- in Postgresql, MV is not updated on each db change but has to be recreated with REFRESH MATERIALIZED VIEW
-- create index legal_bases_type ON legal_bases ("type");
-- create index legal_bases_id ON legal_bases (id);
-- create index legal_bases_appid ON legal_bases (appid);
create view legal_bases as
select 'CONTRACT' as type, c.subcat as subcat, c.id as id, c.appid as appid, c.name as name, c.description as description, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
from contracts c
	join contracts_scope cs on cs.cid = c.id
	join "scope" s on s.id = cs.scid
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
group by c.id
union
select 'NECESSARY' as type, nlb.subcat as subcat, nlb.id as id, nlb.appid as appid, nlb.name as name, nlb.description as description, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
from necessary_legal_bases nlb
	join necessary_legal_bases_scope nlbs on nlbs.nlbid = nlb.id
	join "scope" s on s.id = nlbs.scid
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
group by nlb.id
union
select 'LEGITIMATE-INTEREST' as type, li.subcat as subcat, li.id as id, li.appid as appid, li.name as name, li.description as description, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
from legitimate_interests li
	join legitimate_interests_scope lis on lis.liid = li.id
	join "scope" s on s.id = lis.scid
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
group by li.id
union
select 'CONSENT' as type, c2.subcat as subcat, c2.id as id, c2.appid as appid, c2.name as name, c2.description as description, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
from consents c2
	join consents_scope cs2 on cs2.cid = c2.id
	join "scope" s on s.id = cs2.scid
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
group by c2.id;

create view general_information_view as
select gi.id, gi.appid, gi.countries, array_agg(distinct gio."name") as organizations, array_agg(distinct array[dpo."name", dpo.contact]) as dpo, gi.data_consumer_categories, gi.access_policies, gi.privacy_policy_link, gi.data_security_information
from general_information gi
join general_information_organization gio on gio.gid = gi.id
join dpo on dpo.gid = gi.id
group by gi.id;
