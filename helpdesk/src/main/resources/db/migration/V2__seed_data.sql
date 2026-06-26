-- =========================
-- USERS
-- =========================

INSERT INTO users (id, name, email, password_hash, role, phone, timezone, active, login_attempts, created_at,
                   updated_at)
VALUES (1, 'System Administrator', 'admin@helpdesk.gov.za',
        '$2a$12$Dq3LmU9rmppkPK4gESHofuVRdDs6xqjBC810hzB628GZLEPkOvwCu',
        'ADMIN', NULL, 'Africa/Johannesburg', true, 0, now(), now()),

       (2, 'Thabo Mokoena', 'thabo.m@company.co.za',
        '$2a$12$nInN3zhMyi013gZ6GPKZIulq8OkmwbK743kkhBhGJrKC7X8Cvms8O',
        'AGENT', '+27 11 555 0101', 'Africa/Johannesburg', true, 0, now(), now()),

       (3, 'Sarah Jenkins', 's.jenkins@it.com',
        '$2a$12$nInN3zhMyi013gZ6GPKZIulq8OkmwbK743kkhBhGJrKC7X8Cvms8O',
        'AGENT', '+27 11 555 0102', 'Africa/Johannesburg', true, 0, now(), now()),

       (4, 'Lerato Dlamini', 'lerato.d@client.org',
        '$2a$12$nInN3zhMyi013gZ6GPKZIulq8OkmwbK743kkhBhGJrKC7X8Cvms8O',
        'USER', '+27 82 555 9988', 'Africa/Johannesburg', true, 0, now(), now()),

       (5, 'Mark Thompson', 'm.thompson@gmail.com',
        '$2a$12$nInN3zhMyi013gZ6GPKZIulq8OkmwbK743kkhBhGJrKC7X8Cvms8O',
        'USER', '+27 71 444 2233', 'Africa/Johannesburg', true, 0, now(), now()),

       (6, 'Alice Zuberi', 'alice.z@corp.net',
        '$2a$12$nInN3zhMyi013gZ6GPKZIulq8OkmwbK743kkhBhGJrKC7X8Cvms8O',
        'USER', '+27 60 123 4567', 'Africa/Johannesburg', true, 0, now(), now());


-- =========================
-- AGENTS
-- =========================

INSERT INTO agents (id, user_id, department, availability)
VALUES (1, 2, 'Hardware Support', 'ONLINE'),
       (2, 3, 'Software & Licensing', 'BUSY');


-- =========================
-- TICKETS
-- =========================

INSERT INTO tickets (id, subject, description, status, priority, category, requester_id, assignee_id, escalated,
                     created_at, updated_at)
VALUES (5, 'Laptop wont start', 'Black screen after update. Tried hard reset, no luck.',
        'OPEN', 'HIGH', 'Hardware', 4, 1, false, now(), now()),

       (6, 'VPN Access Request', 'Need access to the secondary cloud for the dev profile.',
        'IN_PROGRESS', 'MEDIUM', 'Access', 5, 2, false, now(), now()),

       (7, 'Printer Jam - 4th Floor', 'The main Xerox machine is jammed again.',
        'OPEN', 'LOW', 'Maintenance', 6, NULL, false, now(), now()),

       (8, 'URGENT: Server Down', 'The production database is unresponsive!',
        'ESCALATED', 'URGENT', 'Infrastructure', 4, 1, false, now(), now());


-- =========================
-- COMMENTS
-- =========================

INSERT INTO comments (id, ticket_id, author_id, parent_id, body, internal, type, created_at)
VALUES (7, 6, 3, NULL, 'Checking permissions with the Cloud Admin.', true, 'NOTE', now()),
       (8, 6, 3, NULL, 'Hi Mark, I am processing this now. Can you provide your employee ID?', false, 'REPLY', now()),
       (9, 6, 5, NULL, 'Sure, it is EMP-99812.', false, 'REPLY', now()),
       (10, 6, 2, 7, 'Hi, can you check now?', false, 'REPLY', now());


-- =========================
-- ATTACHMENTS
-- =========================

INSERT INTO attachments (id, ticket_id, uploader_id, filename, content_type, size_bytes, storage_path, created_at)
VALUES (1, 5, 4, 'error_log.txt', 'text/plain', 1024, '/storage/tickets/1/error_log.txt', now()),
       (2, 8, 1, 'server_metrics.png', 'image/png', 54200, '/storage/tickets/4/metrics.png', now());


-- =========================
-- AUDIT LOG
-- =========================

INSERT INTO audit_log (id, ticket_id, actor_id, action, old_value, new_value, created_at)
VALUES (1, 5, 1, 'STATUS_CHANGE', 'OPEN', 'IN_PROGRESS', now()),
       (2, 8, 2, 'PRIORITY_UPGRADE', 'MEDIUM', 'URGENT', now()),
       (3, 8, 2, 'ESCALATION', 'FALSE', 'TRUE', now());


-- =========================
-- RESET SEQUENCES
-- =========================

SELECT setval('users_id_seq', 6, true);
SELECT setval('agents_id_seq', 2, true);
SELECT setval('tickets_id_seq', 8, true);
SELECT setval('comments_id_seq', 10, true);
SELECT setval('attachments_id_seq', 2, true);
SELECT setval('audit_log_id_seq', 3, true);