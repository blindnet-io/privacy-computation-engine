insert into apps values ('6f083c15-4ada-4671-a6d1-c671bc9105dc');

insert into dac values ('6f083c15-4ada-4671-a6d1-c671bc9105dc', 'https://test');

insert into automatic_responses_config values ('6f083c15-4ada-4671-a6d1-c671bc9105dc', true, true, true, false);

insert into general_information values
('0a32d896-702a-49c7-b063-ec7a76f1de0d', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'blindnet', 'dpo@fakemail.me', array ['France', 'USA'], array ['dc cat 1', 'dc cat 2'], array ['policy 1', 'policy 2'], 'https://blindnet.io/privacy', 'your data is secure');

-- scope
insert into data_subjects values ('fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', null);

insert into data_categories (id, term) values ('55c30115-dbb9-4173-92ae-870a1e0142c9', '*'), ('65042d3f-e7d6-4b31-811b-58334988d86c', 'AFFILIATION'), ('ef8f05c9-b4b0-4479-92d6-a77366b9610f', 'AFFILIATION.MEMBERSHIP'), ('8ec24b22-f4e9-4a42-91da-64ad2ca40c6b', 'AFFILIATION.MEMBERSHIP.UNION'), ('42d15e49-c6a2-4601-bddc-1826486340fb', 'AFFILIATION.SCHOOL'), ('3d796c7c-0b70-4411-89b7-c678221d6cc8', 'AFFILIATION.WORKPLACE'), ('1d8d090f-6c50-4313-9ba3-c56ab6b8b9f1', 'BEHAVIOR'), ('4f331ac4-4bd6-46c2-bfdd-9dcb77273d9f', 'BEHAVIOR.ACTIVITY'), ('1dec09d0-8f6a-4d79-8fad-f3867fdff91a', 'BEHAVIOR.CONNECTION'), ('87768c7a-bfa2-458a-a009-f25d730f21a7', 'BEHAVIOR.PREFERENCE'), ('b07e270b-9278-44bc-9d36-49a0aadfb6a1', 'BEHAVIOR.TELEMETRY'), ('aacffbb8-57a1-4569-a2b4-078d4235b8d3', 'BIOMETRIC'), ('2e38f24e-614a-47c3-a4d9-94a5d825d8ae', 'CONTACT'), ('4a595882-2654-46f6-84f3-85aa25f6e42c', 'CONTACT.EMAIL'), ('06104514-632e-4969-862b-0ff36018a10c', 'CONTACT.ADDRESS'), ('269a706c-de49-4639-9273-42fbaecd70cd', 'CONTACT.PHONE'), ('5a0c3136-8700-42a3-92aa-1e74298e57f8', 'DEMOGRAPHIC'), ('ddfa6419-65b9-4604-bdc3-c7df40dab928', 'DEMOGRAPHIC.AGE'), ('0313fbf7-11cd-4ba0-89fa-55057eb2d8ee', 'DEMOGRAPHIC.BELIEFS'), ('2ff3fd75-2ff8-4d5c-8e13-163a45f834c4', 'DEMOGRAPHIC.GENDER'), ('9e7f51be-478e-4eaa-9393-60f1d1165f33', 'DEMOGRAPHIC.ORIGIN'), ('e9adaf94-8c69-4b44-9ea5-b093154f073c', 'DEMOGRAPHIC.RACE'), ('820209d7-e712-4ecc-88b0-9a01ee57e7c7', 'DEMOGRAPHIC.SEXUAL-ORIENTATION'), ('c4f4f712-08b3-40eb-9699-d90e76295fb1', 'DEVICE'), ('17574290-b6d8-4378-a461-b725b860bed4', 'FINANCIAL'), ('05e2be9f-db2b-49b9-890f-3917dc24390c', 'FINANCIAL.BANK-ACCOUNT'), ('fe3122e7-b37f-4205-9a96-ed044c8cea83', 'GENETIC'), ('16317419-371c-413d-bc3e-4a395fb5dcb2', 'HEALTH'), ('ae584cba-cd3c-4a85-ad33-7e8c8148d990', 'IMAGE'), ('119274ca-0741-4c3c-a997-85cac02d7357', 'LOCATION'), ('d9e8b307-b999-42b9-9f0d-37fd84e02b02', 'NAME'), ('2d2464ff-0cfa-4668-bda9-39e9543a2d07', 'PROFILING'), ('9926cc63-a5dc-43e5-8836-e52a47774259', 'RELATIONSHIPS'), ('4832b5bd-8db3-4a79-8655-34e8547f375e', 'UID'), ('e06b85ef-241d-470c-a398-bb57dad60616', 'UID.ID'), ('4448dec9-8418-49c9-99eb-a69890550b17', 'UID.IP'), ('d71ca714-75f1-4f42-af13-5f6e47037fb1', 'UID.USER-ACCOUNT'), ('80f8639b-8c56-40a9-9513-ddce0ff3ebc2', 'UID.SOCIAL-MEDIA'), ('823b4129-c4c3-4cb2-9fc5-b94e94ffc985', 'OTHER-DATA');

insert into processing_categories values ('9540ab5d-bea9-4ddd-874a-82c8ecf0b9f5', '*'), ('cb277f6e-4607-44de-948b-fe64b226142a', 'ANONYMIZATION'), ('ebc2c6c3-d1eb-4517-bb92-6a237d5ac4d0', 'AUTOMATED-INFERENCE'), ('c7bc5163-252d-4dc1-8bc2-4104a4b10c77', 'AUTOMATED-DECISION-MAKING'), ('3fddc9ce-7e57-4fcc-abb0-bf410f1f42a7', 'COLLECTION'), ('50a0baef-6c0a-4d66-95c3-f5afd43a055d', 'GENERATING'), ('ce9fba3a-5829-4fcc-8f1e-d5073442fd00', 'PUBLISHING'), ('2b4cab1c-9f1b-4fab-a4d3-b64d2a00ffbe', 'STORING'), ('5e331001-13ab-4f57-a6a6-0f2986623f83', 'SHARING'), ('a064affe-cc23-406f-bf95-79956a7e7e8c', 'USING'), ('0e6df34c-f0c5-4ec5-b9c8-dde1b01c94d2', 'OTHER-PROCESSING');

insert into processing_purposes values ('640a111e-74dc-4a35-94f6-89bd84dd2009', '*'), ('429067fa-8ca3-46a5-8731-2ff0ae67ecfe', 'ADVERTISING'), ('f7cc5d4b-2336-4d60-b295-09db012341ed', 'COMPLIANCE'), ('f7983bd5-5d01-4ada-bac0-62c0450bc6ff', 'EMPLOYMENT'), ('1c734709-78c3-41db-8427-fe73c8337b88', 'JUSTICE'), ('12cda115-cd49-4e1e-917b-b1a5162eb453', 'MARKETING'), ('c1cb325d-f2f8-4340-9e6e-a667e9979e20', 'MEDICAL'), ('b737925e-1790-4c4a-894f-268bb2257a0e', 'PERSONALIZATION'), ('edbeb300-dd9d-4de0-9ad4-2511aa04d1c8', 'PUBLIC-INTERESTS'), ('f249aa17-1b3b-4ea1-8edf-7df3d8a99595', 'RESEARCH'), ('f3efbc9c-f8b9-4856-9c3a-8a7bc70c0649', 'SALE'), ('91e28880-90b1-48e3-a8b5-a7288e750661', 'SECURITY'), ('24c31e2d-61af-4931-9631-5a665e32d9d5', 'SERVICES'), ('47c8e4d9-fcbc-4e8d-b065-e59891d29825', 'SERVICES.ADDITIONAL-SERVICES'), ('93e9c52d-9005-4446-9a3f-8a1d6913e274', 'SERVICES.BASIC-SERVICE'), ('fa1ec04d-35db-457a-aa7d-dd849d952794', 'SOCIAL-PROTECTION'), ('043c18bf-a481-44a3-827f-ce975b26d089', 'TRACKING'), ('3efc3eb3-3109-49d9-ac2b-14daa6171adc', 'VITAL-INTERESTS'), ('b641a679-f29f-4436-8182-5ab65213984f', 'OTHER-PURPOSE');

-- selectors
insert into data_categories (id, term, selector, appid, active) values
('ff44a3a2-227c-4ab5-8378-4cc4512d594c', 'AFFILIATION.selector_1', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true),
('d9c0d430-1b1f-4682-a7c5-abe9a48cbeb2', 'AFFILIATION.MEMBERSHIP.selector_2', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true),
('e24c2c93-9308-4b09-8c98-f9ec5a103d29', 'FINANCIAL.BANK-ACCOUNT.selector_3', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true),
('e37d9c60-cadb-43f4-9a7a-4a75a7dfb7cf', 'FINANCIAL.BANK-ACCOUNT.selector_4', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true),
('e39e09b0-f3d7-4b63-9e63-a683f667043b', 'FINANCIAL.BANK-ACCOUNT.selector_5', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true),
('18abc787-79b7-481a-8699-1b05cc5ad4a7', 'RELATIONSHIPS.selector_6', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true),
('2970e8f8-3dfd-4a5f-bb0f-723fa05818a9', 'RELATIONSHIPS.selector_7', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true),
('1970e8f8-3dfd-4a5f-bb0f-723fa05818a9', 'RELATIONSHIPS.inactive_selector_1', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', false),
('967ebb89-55db-43ba-87c2-efc4b177e57c', 'OTHER-DATA.PROOF', true, '6f083c15-4ada-4671-a6d1-c671bc9105dc', true);

insert into provenances values
(gen_random_uuid(), '6f083c15-4ada-4671-a6d1-c671bc9105dc', '55c30115-dbb9-4173-92ae-870a1e0142c9', 'USER', 'demo');

insert into retention_policies values
(gen_random_uuid(), '6f083c15-4ada-4671-a6d1-c671bc9105dc', '55c30115-dbb9-4173-92ae-870a1e0142c9', 'NO-LONGER-THAN', '10', 'RELATIONSHIP-END');

insert into "scope" (select gen_random_uuid() as id, dc.id as dcid, pc.id as pcid, pp.id as ppid from data_categories dc, processing_categories pc, processing_purposes pp);


-- legal bases

insert into legal_bases (id, appid, type, name, description, active) values
('f58b660d-39c9-4f20-be6b-7dd2f2a94f45', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONTRACT', 'contract one', 'desc', true),
('fe518122-a85e-4473-8a1f-322cee2f3787', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONTRACT', 'contract two', 'desc', true),
('87630175-e5ff-4f71-9fba-8f1851ff5dca', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONTRACT', 'contract three', 'desc', true),
('436f3112-0241-4eef-971a-e13c6787bf91', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'NECESSARY', 'necessary one', 'desc', true),
('c8c594ee-357a-4ae5-ae40-794f3953cda7', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'NECESSARY.LEGAL-OBLIGATION', 'necessary two', 'desc', true),
('b84170f9-2d67-40da-bcd2-7db8f6eeb622', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'LEGITIMATE-INTEREST', 'legitimate interest one', 'desc', true),
('3cb26496-64a0-4425-af66-f2cec82e3c62', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'LEGITIMATE-INTEREST', 'legitimate interest two', 'desc', true),
('30b153ea-533e-49c2-a577-535c2423ae50', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONSENT', 'consent one', 'desc', true),
('ce8768f2-d1cf-4721-934b-5bb797046c2c', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONSENT', 'consent two', 'desc', true),
('056949f5-5520-4701-8ad0-6bf0355f631d', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONSENT', 'consent three', 'desc', true),
('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'CONSENT', 'Prizes consent', '', true);

insert into legal_bases_scope SELECT 'f58b660d-39c9-4f20-be6b-7dd2f2a94f45', id FROM "scope" ORDER BY RANDOM() LIMIT 1000;
insert into legal_bases_scope SELECT 'fe518122-a85e-4473-8a1f-322cee2f3787', id FROM "scope" where "pcid"='a064affe-cc23-406f-bf95-79956a7e7e8c';
insert into legal_bases_scope SELECT '87630175-e5ff-4f71-9fba-8f1851ff5dca', id FROM "scope" where "dcid"='65042d3f-e7d6-4b31-811b-58334988d86c';

insert into legal_bases_scope SELECT '436f3112-0241-4eef-971a-e13c6787bf91', id FROM "scope" where "dcid"='80f8639b-8c56-40a9-9513-ddce0ff3ebc2';
insert into legal_bases_scope SELECT 'c8c594ee-357a-4ae5-ae40-794f3953cda7', id FROM "scope" where "pcid"='cb277f6e-4607-44de-948b-fe64b226142a';

insert into legal_bases_scope SELECT 'b84170f9-2d67-40da-bcd2-7db8f6eeb622', id FROM "scope" ORDER BY RANDOM() limit 20;
insert into legal_bases_scope SELECT '3cb26496-64a0-4425-af66-f2cec82e3c62', id FROM "scope" ORDER BY RANDOM() LIMIT 100;

insert into legal_bases_scope SELECT '30b153ea-533e-49c2-a577-535c2423ae50', id FROM "scope" ORDER BY RANDOM() limit 100;
insert into legal_bases_scope SELECT 'ce8768f2-d1cf-4721-934b-5bb797046c2c', id FROM "scope" ORDER BY RANDOM() limit 10;
insert into legal_bases_scope SELECT '056949f5-5520-4701-8ad0-6bf0355f631d', id FROM "scope";

insert into legal_bases_scope values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (select id from scope where dcid = '4a595882-2654-46f6-84f3-85aa25f6e42c' and pcid='9540ab5d-bea9-4ddd-874a-82c8ecf0b9f5' and ppid = '640a111e-74dc-4a35-94f6-89bd84dd2009'));
insert into legal_bases_scope values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (select id from scope where dcid = 'd9e8b307-b999-42b9-9f0d-37fd84e02b02' and pcid='9540ab5d-bea9-4ddd-874a-82c8ecf0b9f5' and ppid = '640a111e-74dc-4a35-94f6-89bd84dd2009'));
insert into legal_bases_scope values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (select id from scope where dcid = '967ebb89-55db-43ba-87c2-efc4b177e57c' and pcid='9540ab5d-bea9-4ddd-874a-82c8ecf0b9f5' and ppid = '640a111e-74dc-4a35-94f6-89bd84dd2009'));
insert into legal_bases_scope values ('28b5bee0-9db8-40ec-840e-64eafbfb9ddd', (select id from scope where dcid = 'e06b85ef-241d-470c-a398-bb57dad60616' and pcid='9540ab5d-bea9-4ddd-874a-82c8ecf0b9f5' and ppid = '640a111e-74dc-4a35-94f6-89bd84dd2009'));

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

-- events

insert into legal_base_events (id, lbid, dsid, appid, event, date) values
(gen_random_uuid(), 'f58b660d-39c9-4f20-be6b-7dd2f2a94f45', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'RELATIONSHIP-START', LOCALTIMESTAMP - INTERVAL '200 DAY'),
(gen_random_uuid(), '436f3112-0241-4eef-971a-e13c6787bf91', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'RELATIONSHIP-START', LOCALTIMESTAMP - INTERVAL '190 DAY'),
(gen_random_uuid(), 'c8c594ee-357a-4ae5-ae40-794f3953cda7', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'SERVICE-START', LOCALTIMESTAMP - INTERVAL '180 DAY'),
(gen_random_uuid(), 'f58b660d-39c9-4f20-be6b-7dd2f2a94f45', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'RELATIONSHIP-END', LOCALTIMESTAMP - INTERVAL '170 DAY'),
(gen_random_uuid(), 'c8c594ee-357a-4ae5-ae40-794f3953cda7', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'SERVICE-END', LOCALTIMESTAMP - INTERVAL '160 DAY'),
(gen_random_uuid(), 'fe518122-a85e-4473-8a1f-322cee2f3787', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', 'SERVICE-START', LOCALTIMESTAMP - INTERVAL '150 DAY');

insert into consent_given_events (id, lbid, dsid, appid, date) values
(gen_random_uuid(), '30b153ea-533e-49c2-a577-535c2423ae50', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc',  LOCALTIMESTAMP - INTERVAL '185 DAY'),
(gen_random_uuid(), 'ce8768f2-d1cf-4721-934b-5bb797046c2c', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc',  LOCALTIMESTAMP - INTERVAL '175 DAY'),
(gen_random_uuid(), '056949f5-5520-4701-8ad0-6bf0355f631d', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc',  LOCALTIMESTAMP - INTERVAL '140 DAY');

insert into consent_revoked_events (id, lbid, dsid, appid, date) values
(gen_random_uuid(), '056949f5-5520-4701-8ad0-6bf0355f631d', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc',  LOCALTIMESTAMP - INTERVAL '100 DAY');
