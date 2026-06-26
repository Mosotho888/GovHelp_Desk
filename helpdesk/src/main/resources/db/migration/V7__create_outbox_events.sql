CREATE TABLE outbox_events
(
    id             BIGSERIAL PRIMARY KEY,
    event_type     VARCHAR(100) NOT NULL, -- e.g. 'AUDIT', 'TICKET_EMAIL'
    aggregate_type VARCHAR(100) NOT NULL, -- e.g. 'TICKET', 'AUTH'
    aggregate_id   BIGINT,                -- the entity ID (nullable for auth events)
    payload        JSONB        NOT NULL, -- serialised message DTO
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | PROCESSING | PROCESSED | FAILED
    attempts       INT          NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP
);

-- Relay reads only PENDING rows ordered by creation - this index covers that query
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at) WHERE status = 'PENDING';