-- =========================
-- TICKET CATEGORIES
-- =========================

CREATE TABLE IF NOT EXISTS ticket_categories
(
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    slug               VARCHAR(120) NOT NULL UNIQUE,
    parent_id          BIGINT,
    level              SMALLINT     NOT NULL DEFAULT 0,
    default_department VARCHAR(100),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP             DEFAULT now() NOT NULL,
    updated_at         TIMESTAMP             DEFAULT now() NOT NULL,

    CONSTRAINT ticket_categories_level_check
    CHECK (level BETWEEN 0 AND 2),

    CONSTRAINT uq_ticket_categories_parent_name
    UNIQUE (parent_id, name),

    FOREIGN KEY (parent_id) REFERENCES ticket_categories (id) ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_ticket_categories_parent ON ticket_categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_ticket_categories_active ON ticket_categories (active);

-- =========================
-- SEED: top level categories
-- =========================

INSERT INTO ticket_categories (id, name, slug, parent_id, level, default_department)
VALUES (1, 'Hardware', 'hardware', NULL, 0, 'Desktop Support'),
       (2, 'Software', 'software', NULL, 0, 'Applications'),
       (3, 'Network', 'network', NULL, 0, 'Infrastructure'),
       (4, 'Accounts', 'accounts', NULL, 0, 'Identity & Access'),
       (5, 'Security', 'security', NULL, 0, 'Information Security')
    ON CONFLICT (id) DO UPDATE
                            SET name = EXCLUDED.name,
                            slug = EXCLUDED.slug,
                            default_department = EXCLUDED.default_department;

-- Advance sequence past explicit IDs to avoid collision on subcategory insertions
SELECT setval(pg_get_serial_sequence('ticket_categories', 'id'), COALESCE((SELECT MAX(id) FROM ticket_categories), 1));

-- =========================
-- SEED: subcategories
-- =========================

INSERT INTO ticket_categories (name, slug, parent_id, level, default_department)
VALUES ('Laptop', 'hardware-laptop', 1, 1, 'Desktop Support'),
       ('Desktop', 'hardware-desktop', 1, 1, 'Desktop Support'),
       ('Printer', 'hardware-printer', 1, 1, 'Desktop Support'),
       ('Peripherals', 'hardware-peripherals', 1, 1, 'Desktop Support'),

       ('Operating System', 'software-operating-system', 2, 1, 'Applications'),
       ('Business Application', 'software-business-application', 2, 1, 'Applications'),
       ('Installation Request', 'software-installation-request', 2, 1, 'Applications'),

       ('VPN', 'network-vpn', 3, 1, 'Infrastructure'),
       ('Wi-Fi', 'network-wifi', 3, 1, 'Infrastructure'),
       ('Connectivity Outage', 'network-connectivity-outage', 3, 1, 'Infrastructure'),

       ('Password Reset', 'accounts-password-reset', 4, 1, 'Identity & Access'),
       ('Access Request', 'accounts-access-request', 4, 1, 'Identity & Access'),
       ('New User Onboarding', 'accounts-new-user-onboarding', 4, 1, 'Identity & Access'),

       ('Phishing Report', 'security-phishing-report', 5, 1, 'Information Security'),
       ('Suspected Breach', 'security-suspected-breach', 5, 1, 'Information Security')
    ON CONFLICT (slug) DO NOTHING;

-- Final sequence sync to ensure auto-generated IDs are aligned
SELECT setval(pg_get_serial_sequence('ticket_categories', 'id'), COALESCE((SELECT MAX(id) FROM ticket_categories), 1));

-- =========================
-- TICKETS: link to categories
-- =========================

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES ticket_categories (id);

CREATE INDEX IF NOT EXISTS idx_tickets_category ON tickets (category_id);

-- Best-effort backfill from the old free-text category column onto matching top level category
UPDATE tickets t
SET category_id = tc.id
    FROM ticket_categories tc
WHERE tc.parent_id IS NULL
  AND lower(t.category) = lower(tc.name);

UPDATE tickets
SET category_id = (SELECT id FROM ticket_categories WHERE slug = 'network')
WHERE category_id IS NULL AND lower(category) = 'infrastructure';

UPDATE tickets
SET category_id = (SELECT id FROM ticket_categories WHERE slug = 'accounts')
WHERE category_id IS NULL AND lower(category) = 'access';

UPDATE tickets
SET category_id = (SELECT id FROM ticket_categories WHERE slug = 'hardware')
WHERE category_id IS NULL AND lower(category) = 'maintenance';

-- The hierarchical category_id column replaces the old free-text column entirely
ALTER TABLE tickets
DROP COLUMN IF EXISTS category;