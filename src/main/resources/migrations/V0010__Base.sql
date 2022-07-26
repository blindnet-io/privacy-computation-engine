
create table apps (
  id uuid primary key,
  active boolean default true
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
  parent uuid, -- selector
  appid uuid, -- selector
  active boolean not null default true, -- selector
  constraint data_categories_fk
    foreign key (parent)
    references data_categories(id)
    on delete cascade,
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

create type target_terms as enum ('ORGANIZATION', 'PARTNERS', 'SYSTEM', 'PARTNERS.DOWNWARD', 'PARTNERS.UPWARD');
create type provenance_terms as enum ('DERIVED', 'TRANSFERRED', 'USER', 'USER.DATA-SUBJECT');

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

create type policy_terms as enum ('NO-LONGER-THAN', 'NO-LESS-THAN');
create type event_terms as enum ('CAPTURE-DATE', 'RELATIONSHIP-END', 'RELATIONSHIP-START', 'SERVICE-END', 'SERVICE-START');

create table retention_policies (
  id uuid primary key,
  appid uuid not null,
  dcid uuid not null,
  policy policy_terms not null,
  duration integer not null, -- for now days, should be https://www.rfc-editor.org/rfc/pdfrfc/rfc3339.txt.pdf duration in appendix a
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

-----------------

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
  constraint consent_fk
    foreign key (cid)
    references legal_bases(id)
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

create type status_terms as enum ('GRANTED', 'DENIED', 'PARTIALLY-GRANTED', 'UNDER-REVIEW', 'CANCELED');

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

-----------------

-- EVENT

-- this needs a better model
-- a single table with many columns?

create table legal_base_events (
  id uuid primary key,
  lbid uuid not null,
  dsid uuid not null,
  event event_terms not null,
  date timestamp not null,
  constraint legal_base_fk
    foreign key (lbid)
    references legal_bases(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table consent_given_events (
  id uuid primary key,
  lbid uuid not null,
  dsid uuid not null,
  date timestamp not null,
  constraint legal_base_fk
    foreign key (lbid)
    references legal_bases(id)
    on delete cascade,
  constraint data_subject_fk
    foreign key (dsid)
    references data_subjects(id)
    on delete restrict
);

create table consent_revoked_events (
  id uuid primary key,
  lbid uuid not null,
  dsid uuid not null,
  date timestamp not null,
  constraint legal_base_fk
    foreign key (lbid)
    references legal_bases(id)
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

-- VIEWS

-- materialized view is a better performant alternative
-- in Postgresql, MV is not updated on each db change but has to be recreated with REFRESH MATERIALIZED VIEW
-- create index legal_bases_type ON legal_bases ("type");
-- create index legal_bases_id ON legal_bases (id);
-- create index legal_bases_appid ON legal_bases (appid);
-- create view legal_bases as
-- select 'CONTRACT' as type, c.subcat as subcat, c.id as id, c.appid as appid, c.name as name, c.description as description, active as c.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
-- from contracts c
-- 	join contracts_scope cs on cs.cid = c.id
-- 	join "scope" s on s.id = cs.scid
-- 	join data_categories dc on dc.id = s.dcid
-- 	join processing_categories pc on pc.id = s.pcid
-- 	join processing_purposes pp on pp.id = s.ppid
-- where dc.active = true
-- group by c.id
-- union
-- select 'NECESSARY' as type, nlb.subcat as subcat, nlb.id as id, nlb.appid as appid, nlb.name as name, nlb.description as description, active as c.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
-- from necessary_legal_bases nlb
-- 	join necessary_legal_bases_scope nlbs on nlbs.nlbid = nlb.id
-- 	join "scope" s on s.id = nlbs.scid
-- 	join data_categories dc on dc.id = s.dcid
-- 	join processing_categories pc on pc.id = s.pcid
-- 	join processing_purposes pp on pp.id = s.ppid
-- where dc.active = true
-- group by nlb.id
-- union
-- select 'LEGITIMATE-INTEREST' as type, li.subcat as subcat, li.id as id, li.appid as appid, li.name as name, li.description as description, active as c.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
-- from legitimate_interests li
-- 	join legitimate_interests_scope lis on lis.liid = li.id
-- 	join "scope" s on s.id = lis.scid
-- 	join data_categories dc on dc.id = s.dcid
-- 	join processing_categories pc on pc.id = s.pcid
-- 	join processing_purposes pp on pp.id = s.ppid
-- where dc.active = true
-- group by li.id
-- union
-- select 'CONSENT' as type, c2.subcat as subcat, c2.id as id, c2.appid as appid, c2.name as name, c2.description as description, active as c.active, array_agg(dc.term) as dc, array_agg(pc.term) as pc, array_agg(pp.term) as pp
-- from consents c2
-- 	join consents_scope cs2 on cs2.cid = c2.id
-- 	join "scope" s on s.id = cs2.scid
-- 	join data_categories dc on dc.id = s.dcid
-- 	join processing_categories pc on pc.id = s.pcid
-- 	join processing_purposes pp on pp.id = s.ppid
-- where dc.active = true
-- group by c2.id;

-- create view general_information_view as
-- select gi.id, gi.appid, gi.countries, array_agg(distinct gio."name") as organizations, array_agg(distinct array[dpo."name", dpo.contact]) as dpo, gi.data_consumer_categories, gi.access_policies, gi.privacy_policy_link, gi.data_security_information
-- from general_information gi
-- join general_information_organization gio on gio.gid = gi.id
-- join dpo on dpo.gid = gi.id
-- group by gi.id;
