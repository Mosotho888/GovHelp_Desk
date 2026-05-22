-- V3__refactor_audit_log.sql

-- 1. Drop old indexes first
DROP INDEX IF EXISTS idx_audit_ticket;

-- 2. Drop old foreign key constraints
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_ticket_id_fkey;
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_actor_id_fkey;

-- 3. Rename ticket_id → entity_id
ALTER TABLE audit_log RENAME COLUMN ticket_id TO entity_id;

-- 4. Add all the new columns
ALTER TABLE audit_log
    ADD COLUMN entity_type  VARCHAR(30),
    ADD COLUMN actor_name   VARCHAR(100),
    ADD COLUMN actor_role   VARCHAR(20),
    ADD COLUMN ip_address   VARCHAR(45),
    ADD COLUMN description  VARCHAR(500);

-- 5. Backfill entity_type for all existing rows
UPDATE audit_log SET entity_type = 'TICKET' WHERE entity_type IS NULL;

-- 6. Backfill actor_name and actor_role from the users table
UPDATE audit_log al
SET
    actor_name = u.name,
    actor_role = u.role
    FROM users u
WHERE al.actor_id = u.id;

-- 7. Apply NOT NULL constraints after backfill
ALTER TABLE audit_log
    ALTER COLUMN entity_type SET NOT NULL,
ALTER COLUMN actor_name  SET NOT NULL,
    ALTER COLUMN actor_role  SET NOT NULL;

-- 8. Widen value columns and drop NOT NULL on new_value
ALTER TABLE audit_log
ALTER COLUMN old_value TYPE VARCHAR(255),
    ALTER COLUMN new_value TYPE VARCHAR(255),
    ALTER COLUMN new_value DROP NOT NULL;

-- 9. Add new indexes
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_actor  ON audit_log(actor_id,    created_at DESC);
CREATE INDEX idx_audit_action ON audit_log(action,      created_at DESC);