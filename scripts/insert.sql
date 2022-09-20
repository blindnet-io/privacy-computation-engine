insert into apps values ('6f083c15-4ada-4671-a6d1-c671bc9105dc');

insert into dac values ('6f083c15-4ada-4671-a6d1-c671bc9105dc', true, 'https://test');

insert into automatic_responses_config values ('6f083c15-4ada-4671-a6d1-c671bc9105dc', true, true, true, false);

-- data subject

insert into data_subjects values ('fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc', null);

-- events

insert into consent_given_events (id, lbid, dsid, appid, date) values
(gen_random_uuid(), '28b5bee0-9db8-40ec-840e-64eafbfb9ddd', 'fdfc95a6-8fd8-4581-91f7-b3d236a6a10e', '6f083c15-4ada-4671-a6d1-c671bc9105dc',  LOCALTIMESTAMP - INTERVAL '185 DAY');
