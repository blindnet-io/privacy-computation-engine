insert into apps values ('6f083c15-4ada-4671-a6d1-c671bc9105dc');

insert into dac values ('6f083c15-4ada-4671-a6d1-c671bc9105dc', true, 'https://test');

insert into automatic_responses_config values ('6f083c15-4ada-4671-a6d1-c671bc9105dc', true, true, true, false);

insert into general_information values
('0a32d896-702a-49c7-b063-ec7a76f1de0d', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'blindnet', 'dpo@fakemail.me', array ['France', 'USA'], array ['dc cat 1', 'dc cat 2'], array ['policy 1', 'policy 2'], 'https://blindnet.io/privacy', 'your data is secure');

-- data subject

insert into data_subjects values ('fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', null);

-- selectors
insert into data_categories (id, term, selector, appid, active) values
('967ebb89-55db-43ba-87c2-efc4b177e57c', 'OTHER-DATA.PROOF', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true);

insert into provenances (
	select gen_random_uuid(), '6f083c15-4ada-4671-a6d1-c671bc9105dc', id, 'USER', 'demo' from data_categories
);

insert into retention_policies (
	select gen_random_uuid(), '6f083c15-4ada-4671-a6d1-c671bc9105dc', id, 'NO-LONGER-THAN', '10', 'RELATIONSHIP-END' from data_categories
);

insert into "scope" (
	select gen_random_uuid() as id, dc.id as dcid, pc.id as pcid, pp.id as ppid
	from data_categories dc, processing_categories pc, processing_purposes pp
	where dc.selector = true
);


-- legal bases

insert into legal_bases (id, appid, type, name, description, active) values
('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONSENT', 'Prizes consent', '', true);

insert into legal_bases_scope
values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (
	select s.id from scope s
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'CONTACT.EMAIL' and pc.term='*' and pp.term = '*')
);

insert into legal_bases_scope
values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (
	select s.id from scope s
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'NAME' and pc.term='*' and pp.term = '*')
);

insert into legal_bases_scope
values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (
	select s.id from scope s
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'UID.ID' and pc.term='*' and pp.term = '*')
);

insert into legal_bases_scope
values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (
	select s.id from scope s
	join data_categories dc on dc.id = s.dcid
	join processing_categories pc on pc.id = s.pcid
	join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'OTHER-DATA.PROOF' and pc.term='*' and pp.term = '*')
);

-- events

insert into consent_given_events (id, lbid, dsid, appid, date) values
(gen_random_uuid(), '28b5bee0-9db8-40ec-840e-64eafbfb9ddd', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc',  LOCALTIMESTAMP - INTERVAL '185 DAY');


-- regulations

insert into regulations values
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'GDPR'),
('93a6cefc-da22-4f60-9258-506355278399', 'GDPR-health');

insert into app_regulations values
('6f083c15-4ada-4671-a6d1-c671bc9105dc', '77c568c0-e4a5-4cde-b51f-cefd06c258a2');

insert into regulation_legal_base_forbidden_scope values
-- prohibited CONTRACT under GDPR
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'AFFILIATION.MEMBERSHIP.UNION' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.ORIGIN' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.RACE' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.BELIEFS' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.SEXUAL-ORIENTATION' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'GENETIC' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'HEALTH' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'BIOMETRIC' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'CONTRACT', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'AFFILIATION.MEMBERSHIP.UNION' and pc.term = '*' and pp.term = '*')),
-- prohibited LEGITIMATE-INTEREST under GDPR
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'AFFILIATION.MEMBERSHIP.UNION' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.ORIGIN' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.RACE' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.BELIEFS' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.SEXUAL-ORIENTATION' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'GENETIC' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'HEALTH' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'BIOMETRIC' and pc.term = '*' and pp.term = '*')),
('77c568c0-e4a5-4cde-b51f-cefd06c258a2', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'AFFILIATION.MEMBERSHIP.UNION' and pc.term = '*' and pp.term = '*')),
-- prohibited LEGITIMATE-INTEREST under GDPR for health
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'AFFILIATION.MEMBERSHIP.UNION' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.ORIGIN' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.RACE' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.BELIEFS' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'DEMOGRAPHIC.SEXUAL-ORIENTATION' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'GENETIC' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'HEALTH' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'BIOMETRIC' and pc.term = '*' and pp.term = '*')),
('93a6cefc-da22-4f60-9258-506355278399', 'LEGITIMATE-INTEREST', (select s.id from scope s join data_categories dc on dc.id = s.dcid join processing_categories pc on pc.id = s.pcid join processing_purposes pp on pp.id = s.ppid
	where dc.term = 'AFFILIATION.MEMBERSHIP.UNION' and pc.term = '*' and pp.term = '*'));
