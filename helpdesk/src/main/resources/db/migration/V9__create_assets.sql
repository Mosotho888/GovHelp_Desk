-- =========================
-- ASSETS
-- =========================

CREATE TABLE assets
(
    id                   BIGSERIAL PRIMARY KEY,
    asset_tag            VARCHAR(50)  NOT NULL UNIQUE,
    name                 VARCHAR(150) NOT NULL,
    type                 VARCHAR(30)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'IN_STORAGE',
    serial_number        VARCHAR(100) UNIQUE,
    manufacturer         VARCHAR(100),
    model                VARCHAR(100),
    assigned_user_id     BIGINT REFERENCES users (id),
    location             VARCHAR(150),
    vendor               VARCHAR(150),
    purchase_date        DATE,
    purchase_cost        NUMERIC(12, 2),
    warranty_expiry_date DATE,
    notes                TEXT,
    created_at           TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT assets_type_check
        CHECK (type IN ('LAPTOP', 'DESKTOP', 'PRINTER', 'MONITOR', 'NETWORKING_EQUIPMENT',
                        'SOFTWARE_LICENSE', 'OTHER')),
    CONSTRAINT assets_status_check
        CHECK (status IN ('IN_USE', 'IN_STORAGE', 'UNDER_REPAIR', 'RETIRED', 'LOST'))
);

CREATE INDEX idx_assets_status ON assets (status);
CREATE INDEX idx_assets_type ON assets (type);
CREATE INDEX idx_assets_assigned_user ON assets (assigned_user_id);

-- =========================
-- TICKET_ASSETS (link table)
-- =========================

CREATE TABLE ticket_assets
(
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT       NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    asset_id       BIGINT       NOT NULL REFERENCES assets (id),
    linked_by_id   BIGINT       NOT NULL REFERENCES users (id),
    linked_by_name VARCHAR(100) NOT NULL,
    linked_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uq_ticket_assets_ticket_asset UNIQUE (ticket_id, asset_id)
);

CREATE INDEX idx_ticket_assets_ticket ON ticket_assets (ticket_id);
CREATE INDEX idx_ticket_assets_asset ON ticket_assets (asset_id, linked_at DESC);

-- =========================
-- SEED: demo assets
-- =========================
-- Synthetic inventory for local/demo environments, referencing the seeded users from
-- V2__seed_data.sql. Ownership and warranty dates are chosen to exercise both an active and an
-- expired/expiring warranty when demoing the feature.

INSERT INTO assets (id, asset_tag, name, type, status, serial_number, manufacturer, model,
                    assigned_user_id, location, vendor, purchase_date, purchase_cost,
                    warranty_expiry_date)
VALUES (1, 'AST-000001', 'Dell Latitude 5420 - Finance', 'LAPTOP', 'IN_USE', 'SN-DL5420-0001',
        'Dell', 'Latitude 5420', 4, 'Head Office - 2nd Floor', 'Dell South Africa',
        '2023-02-10', 18500.00, '2026-02-10'),

       (2, 'AST-000002', 'HP LaserJet Pro - 4th Floor', 'PRINTER', 'IN_USE', 'SN-HPLJ-0002',
        'HP', 'LaserJet Pro M404dn', NULL, 'Head Office - 4th Floor', 'HP Inc.',
        '2021-06-01', 6200.00, '2023-06-01'),

       (3, 'AST-000003', 'Cisco Catalyst Switch - Server Room', 'NETWORKING_EQUIPMENT', 'IN_USE',
        'SN-CISCO-0003', 'Cisco', 'Catalyst 2960-X', NULL, 'Head Office - Server Room',
        'Cisco Systems', '2022-09-15', 42000.00, '2025-09-15'),

       (4, 'AST-000004', 'Microsoft 365 E3 License - IT Dept', 'SOFTWARE_LICENSE', 'IN_USE', NULL,
        'Microsoft', '365 E3', 5, NULL, 'Microsoft', '2024-01-01', 0.00, '2027-01-01'),

       (5, 'AST-000005', 'Dell OptiPlex 7010 - Spare', 'DESKTOP', 'IN_STORAGE', 'SN-OP7010-0005',
        'Dell', 'OptiPlex 7010', NULL, 'Head Office - Storeroom', 'Dell South Africa',
        '2020-03-20', 12000.00, '2023-03-20');

SELECT setval('assets_id_seq', (SELECT MAX(id) FROM assets));

-- Link a couple of the seeded tickets (see V2__seed_data.sql) to the assets they concern.
INSERT INTO ticket_assets (ticket_id, asset_id, linked_by_id, linked_by_name)
VALUES (5, 1, 1, 'System Administrator'), -- "Laptop wont start" -> AST-000001
       (7, 2, 1, 'System Administrator'); -- "Printer Jam - 4th Floor" -> AST-000002
