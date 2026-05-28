
CREATE TABLE sla_policies (
    id                        BIGSERIAL PRIMARY KEY,
    priority                  VARCHAR(20)  NOT NULL UNIQUE,
    response_minutes          INT          NOT NULL,
    resolution_minutes        INT          NOT NULL,
    warning_threshold_minutes INT          NOT NULL DEFAULT 30
);

-- Seed default SLA policies
INSERT INTO sla_policies (priority, response_minutes, resolution_minutes, warning_threshold_minutes)
VALUES
    ('URGENT', 60,   60,    15),
    ('HIGH',   240,  240,   30),
    ('MEDIUM', 480,  480,   60),
    ('LOW',    1440, 1440,  120);

CREATE TABLE ticket_sla (
    id                       BIGSERIAL PRIMARY KEY,
    ticket_id                BIGINT      NOT NULL UNIQUE REFERENCES tickets(id) ON DELETE CASCADE,
    response_due_at          TIMESTAMP   NOT NULL,
    resolution_due_at        TIMESTAMP   NOT NULL,
    first_response_at        TIMESTAMP,
    resolved_at              TIMESTAMP,
    response_breached        BOOLEAN     NOT NULL DEFAULT FALSE,
    resolution_breached      BOOLEAN     NOT NULL DEFAULT FALSE,
    response_warning_sent    BOOLEAN     NOT NULL DEFAULT FALSE,
    resolution_warning_sent  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_sla_ticket         ON ticket_sla(ticket_id);
CREATE INDEX idx_sla_response_due   ON ticket_sla(response_due_at);
CREATE INDEX idx_sla_resolution_due ON ticket_sla(resolution_due_at);