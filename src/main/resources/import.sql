-- 1. Progetti
insert into project(id, name) values(nextval('project_seq'), 'LTRAD');
insert into project(id, name) values(nextval('project_seq'), 'FIRE');
insert into project(id, name) values(nextval('project_seq'), 'VOLCANO');

-- 2. Utenti
insert into app_user(id, name, surname, date_of_birth, phone_number)values(1, 'Flaminia', 'Balduini', '2003-10-16', '3737397589');

insert into app_user(id, name, surname, date_of_birth, phone_number)values(2, 'Flaminia', 'Balduini', '2003-10-16', '3737397589');
insert into app_user(id, name, surname, date_of_birth, phone_number)values(3, 'Luca', 'Bussi', '2003-09-17', '3315988152');
insert into app_user(id, name, surname, date_of_birth, phone_number)values(4, 'Jhon', 'Herrera', '2004-01-30', '3887258823');


-- 3. Credenziali (devono esistere gli utenti e il progetto)
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(1, 1, 1, 'fbalduini@icloud.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'SUPERADMIN', 'mimi16|SUPERADMIN', 'mimi16');

insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(2, 1, 2, 'flamy003@gmail.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'LTRAD_ADMIN', 'mimi|LTRAD', 'mimi');
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(3, 51, 3, 'luca.bussi@outlook.it','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'FIRE_ADMIN', 'kuca|FIRE', 'kuca');
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(4, 101, 4, 'jhon30.herrera@gmail.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'VOLCANO_ADMIN', 'jem|VOLCANO', 'jem');

-- 3b. Associazione admin-credentials
insert into admin(id, credentials_id) values (1, 2);   -- LTRAD_ADMIN
insert into admin(id, credentials_id) values (2, 3);   -- FIRE_ADMIN
insert into admin(id, credentials_id) values (3, 4);   -- VOLCANO_ADMIN

insert into superadmin(id, credentials_id) values (1, 1);
insert into superadmin_admin_emails(superadmin_id, admin_email) values (1, 'flamy003@gmail.com');
insert into superadmin_admin_emails(superadmin_id, admin_email) values (1, 'luca.bussi@outlook.it');
insert into superadmin_admin_emails(superadmin_id, admin_email) values (1, 'jhon30.herrera@gmail.com');

insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(5, 1, null, 'flaminia.balduini@fastwebnet.it','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'LTRAD_OPERATOR', 'Flaminia|LTRAD', 'Flaminia');
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(6, 51, null, 'lucabussi03@gmail.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'FIRE_OPERATOR', 'Luca|FIRE', 'Luca');
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(7, 101, null, 'jhonherrera30@icloud.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'VOLCANO_OPERATOR', 'Jhon|VOLCANO', 'Jhon');
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(8, 1, null, 'giulia.rossi@example.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'LTRAD_OPERATOR', 'Giulia|LTRAD', 'Giulia');
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(9, 51, null, 'marco.verdi@example.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'FIRE_OPERATOR', 'Marco|FIRE', 'Marco');
insert into credentials(id, project_id, user_id, email, password, role, username, visible_username)values(10, 101, null, 'carlos.gomez@example.com','$2a$10$cHSpwVdP8S33zR6PHIfvs.g9TZl6QbhHX9KTayP/91cxVScdb/c.W', 'VOLCANO_OPERATOR', 'Carlos|VOLCANO', 'Carlos');

-- 3c. Associazione operatori-admin (employer)
update credentials set employer_id = 1 where email = 'flaminia.balduini@fastwebnet.it';
update credentials set employer_id = 1 where email = 'giulia.rossi@example.com';
update credentials set employer_id = 2 where email = 'lucabussi03@gmail.com';
update credentials set employer_id = 2 where email = 'marco.verdi@example.com';
update credentials set employer_id = 3 where email = 'jhonherrera30@icloud.com';
update credentials set employer_id = 3 where email = 'carlos.gomez@example.com';

-- 4. Gruppi (devono esistere le credentials, e il progetto)
insert into app_group(credentials_id, id, project_id, name)values (2, 1, 1, 'Roma');
insert into app_group(credentials_id, id, project_id, name)values (2, 2, 1, 'Milano');

insert into type_of_device(id, name)values(1, 'LoRaWan')
insert into type_of_device(id, name)values(2, '4Spark 2')
insert into type_of_device(id, name)values(3, 'Device4G ')


insert into project_tods(project_id, tods_id)values(1, 1)
insert into project_tods(project_id, tods_id)values(51, 2)
insert into project_tods(project_id, tods_id)values(101, 3)


-- 5. Dispositivi (devono esistere i gruppi e i progetti)

/* MIMI - Dispositivi posizionati sulle principali uscite del Grande Raccordo Anulare (A90) */
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.884167, 12.380556, 1, 1, 1, 'flamy003@gmail.com', '21:06:1C:EC:E7:20', 'Device1', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.958056, 12.579444, 1, 2, 1, 'flamy003@gmail.com', '34:09:1C:EC:E7:21', 'Device2', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.945556, 12.591667, 1, 3, 1, 'flamy003@gmail.com', '56:08:1C:EC:E7:22', 'Device3', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.939000, 12.600000, 1, 4, 1, 'flamy003@gmail.com', '15:05:1B:EC:E7:23', 'Device4', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.801111, 12.453889, 1, 5, 1, 'flamy003@gmail.com', '17:04:1C:EC:E7:24', 'Device5', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, activated)values(41.807500, 12.420278, 1, 6, 1, 'flamy003@gmail.com', '18:03:1C:EC:E7:25', 'Device6', 1, false);

/* Dispositivi aggiuntivi MIMI */
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.812778, 12.396389, 1, 24, 1, 'flamy003@gmail.com', 'E3:06:1C:EC:E7:3D', 'Device7', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.818056, 12.390833, 1, 25, 1, 'flamy003@gmail.com', 'F4:06:1C:EC:E7:3E', 'Device8', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.823889, 12.388889, 1, 26, 1, 'flamy003@gmail.com', '10:07:1C:EC:E7:3F', 'Device9', 1, 5, true);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.850556, 12.378889, 1, 27, 1, 'flamy003@gmail.com', '21:07:1C:EC:E7:40', 'Device10', 1, 5, false);
insert into device(latitude, longitude, group_id, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(41.865556, 12.373889, 1, 28, 1, 'flamy003@gmail.com', '32:07:1C:EC:E7:41', 'Device10', 1, 5, true);


/* KUCA - sparsi nel nord Italia */
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(45.440001, 10.995001, 7, 51, 'luca.bussi@outlook.it','11:06:1C:EC:E8:26', 'Red1', 2, 6, true);  -- Verona
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(44.494889, 11.342616, 8, 51, 'luca.bussi@outlook.it','14:06:1C:EC:E8:27', 'Red2', 2, 6, true);  -- Bologna
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(45.070339, 7.686864, 9, 51, 'luca.bussi@outlook.it','15:06:1C:EC:E8:28', 'Red3', 2, 6, true);   -- Torino
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(46.062008, 11.121083, 10, 51, 'luca.bussi@outlook.it', '23:06:1C:EC:E8:29', 'Red4', 2, 6, true); -- Trento
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(45.649526, 13.776818, 11, 51, 'luca.bussi@outlook.it', '39:06:1C:EC:E8:30', 'Red5', 2, 6, true); -- Trieste
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(44.801485, 10.327903, 12, 51, 'luca.bussi@outlook.it', '4A:06:1C:EC:E8:31', 'Red6', 2, 6, false); -- Parma

/* Dispositivi aggiuntivi KUCA */
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(45.184724, 9.158207, 29, 51, 'luca.bussi@outlook.it', '43:07:1C:EC:E8:42', 'Red7', 2, 6, true); -- Pavia
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(44.405650, 8.946256, 30, 51, 'luca.bussi@outlook.it', '54:07:1C:EC:E8:43', 'Red8', 2, 6, true); -- Genova
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(45.465422, 9.185924, 31, 51, 'luca.bussi@outlook.it', '65:07:1C:EC:E8:44', 'Red9', 2, 6, true); -- Milano
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(45.327064, 8.419981, 32, 51, 'luca.bussi@outlook.it', '76:07:1C:EC:E8:45', 'Red10', 2, 6, false); -- Novara
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(46.498295, 11.354758, 33, 51, 'luca.bussi@outlook.it', '87:07:1C:EC:E8:46', 'Red11', 2, 6, true); -- Bolzano


/* JEM - sud e isole */
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(40.821371, 14.426468, 13, 101, 'jhon30.herrera@gmail.com', '26:06:1C:EC:E7:32', 'Device1', 3, 7, true); -- Vesuvio
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(37.751005, 14.993356, 14, 101, 'jhon30.herrera@gmail.com', '37:06:1C:EC:E7:33', 'Device2', 3, 7, true); -- Etna
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(38.792520, 15.213890, 15, 101, 'jhon30.herrera@gmail.com', '58:06:1C:EC:E7:34', 'Device3', 3, 7, true); -- Stromboli
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(38.403837, 14.962126, 16, 101, 'jhon30.herrera@gmail.com', '6B:06:1C:EC:E7:35', 'Device4', 3, 7, true);  -- Isola di Vulcano
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(40.827907, 14.139006, 17, 101, 'jhon30.herrera@gmail.com', '7C:06:1C:EC:E7:36', 'Device5', 3, 7, true); -- Campi Flegrei
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(42.895464, 11.615520, 18, 101, 'jhon30.herrera@gmail.com', '8D:06:1C:EC:E7:37', 'Device6', 3, 7, false); -- Monte Amiata

/* Dispositivi aggiuntivi VOLCANO */
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(40.748470, 14.485210, 19, 101, 'jhon30.herrera@gmail.com', '9E:06:1C:EC:E7:38', 'Device7', 3, 7, true); -- Pompei
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(40.805705, 14.348053, 20, 101, 'jhon30.herrera@gmail.com', 'AF:06:1C:EC:E7:39', 'Device8', 3, 7, true); -- Ercolano
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(37.290037, 13.586708, 21, 101, 'jhon30.herrera@gmail.com', 'B0:06:1C:EC:E7:3A', 'Device9', 3, 7, true); -- Valle dei Templi (Agrigento)
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(40.423006, 15.005619, 22, 101, 'jhon30.herrera@gmail.com', 'C1:06:1C:EC:E7:3B', 'Device10', 3, 7, false); -- Paestum
insert into device(latitude, longitude, id, project_id, email_owner, mac_address, name, tod_id, operator_id, activated)values(38.357722, 38.316898, 23, 101, 'jhon30.herrera@gmail.com', 'D2:06:1C:EC:E7:3C', 'Device11', 3, 7, true); -- Arslantepe

insert into spec(id, measurement, unit_of_measurement, component)values(1, 'Temperature', '°C' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(2, 'Relative Humidity', '%RH' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(3, 'VOC', 'Index (0-500)' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(4, 'PM 1.0', 'µg/m³' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(5, 'PM 2.5', 'µg/m³' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(6, 'PM 4.0', 'µg/m³' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(7, 'PM 10.0', 'µg/m³' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(8, 'NOx', 'index' ,'Sen5x')
insert into spec(id, measurement, unit_of_measurement, component)values(9, 'Temperature', '°C' ,'SCD30')
insert into spec(id, measurement, unit_of_measurement, component)values(10, 'PM 1.0', 'µg/m³' ,'SPS30')
insert into spec(id, measurement, unit_of_measurement, component)values(11, 'PM 2.5', 'µg/m³' ,'SPS30')
insert into spec(id, measurement, unit_of_measurement, component)values(12, 'PM 4.0', 'µg/m³' ,'SPS30')
insert into spec(id, measurement, unit_of_measurement, component)values(13, 'PM 10.0', 'µg/m³' ,'SPS30')
insert into spec(id, measurement, unit_of_measurement, component)values(14, 'Temperature', '°C' ,'Dallas')

insert into type_of_device_specs(specs_id, type_of_device_id)values(1, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(2, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(3, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(4, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(5, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(6, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(7, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(8, 1)
insert into type_of_device_specs(specs_id, type_of_device_id)values(9, 3)
insert into type_of_device_specs(specs_id, type_of_device_id)values(10, 3)
insert into type_of_device_specs(specs_id, type_of_device_id)values(11, 3)
insert into type_of_device_specs(specs_id, type_of_device_id)values(12, 3)
insert into type_of_device_specs(specs_id, type_of_device_id)values(13, 3)
insert into type_of_device_specs(specs_id, type_of_device_id)values(14, 2)



-- 6. Reset sequenze
SELECT setval('app_user_seq', (SELECT MAX(id) FROM app_user));
SELECT setval('credentials_seq', (SELECT MAX(id) FROM credentials));
SELECT setval('admin_seq', (SELECT MAX(id) FROM admin));
SELECT setval('superadmin_seq', (SELECT MAX(id) FROM superadmin));
SELECT setval('app_group_seq', (SELECT MAX(id) FROM app_group));
SELECT setval('device_seq', (SELECT MAX(id) FROM device));
SELECT setval('type_of_device_seq', (SELECT MAX(id) FROM type_of_device));
SELECT setval('spec_seq', (SELECT MAX(id) FROM spec));





