-- =========================
-- USERS
-- =========================

CREATE TABLE users
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    phone          VARCHAR(30),
    timezone       VARCHAR(60)  NOT NULL DEFAULT 'Africa/Johannesburg',
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    login_attempts INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMP             DEFAULT now() NOT NULL,
    updated_at     TIMESTAMP             DEFAULT now() NOT NULL,

    CONSTRAINT users_role_check
        CHECK (role IN ('USER', 'AGENT', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users (email);

-- =========================
-- AGENTS
-- =========================

CREATE TABLE agents
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL UNIQUE,
    department   VARCHAR(100),
    availability VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',

    CONSTRAINT agents_availability_check
        CHECK (availability IN ('ONLINE', 'BUSY', 'AWAY', 'OFFLINE')),

    CONSTRAINT fk_agents_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- =========================
-- TICKETS
-- =========================

CREATE TABLE tickets
(
    id           BIGSERIAL PRIMARY KEY,
    subject      VARCHAR(255) NOT NULL,
    description  TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    category     VARCHAR(100),
    requester_id BIGINT       NOT NULL,
    assignee_id  BIGINT,
    escalated    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP             DEFAULT now() NOT NULL,
    updated_at   TIMESTAMP             DEFAULT now() NOT NULL,

    CONSTRAINT tickets_status_check
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED')),

    CONSTRAINT tickets_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),

    FOREIGN KEY (requester_id) REFERENCES users (id),
    FOREIGN KEY (assignee_id) REFERENCES agents (id)
);

CREATE INDEX idx_tickets_status ON tickets (status, created_at DESC);
CREATE INDEX idx_tickets_requester ON tickets (requester_id);
CREATE INDEX idx_tickets_assignee ON tickets (assignee_id, status);

-- =========================
-- COMMENTS
-- =========================

CREATE TABLE comments
(
    id         BIGSERIAL PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL,
    author_id  BIGINT      NOT NULL,
    parent_id  BIGINT,
    body       TEXT        NOT NULL,
    internal   BOOLEAN     NOT NULL DEFAULT FALSE,
    type       VARCHAR(20) NOT NULL DEFAULT 'REPLY',
    created_at TIMESTAMP            DEFAULT now() NOT NULL,

    CONSTRAINT comments_type_check
        CHECK (type IN ('REPLY', 'NOTE', 'RESOLUTION')),

    FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users (id),
    FOREIGN KEY (parent_id) REFERENCES comments (id)
);

CREATE INDEX idx_comments_ticket ON comments (ticket_id, created_at);

-- =========================
-- ATTACHMENTS
-- =========================

CREATE TABLE attachments
(
    id           BIGSERIAL PRIMARY KEY,
    ticket_id    BIGINT                  NOT NULL,
    uploader_id  BIGINT                  NOT NULL,
    filename     VARCHAR(255)            NOT NULL,
    content_type VARCHAR(100)            NOT NULL,
    size_bytes   BIGINT                  NOT NULL,
    storage_path VARCHAR(500)            NOT NULL,
    created_at   TIMESTAMP DEFAULT now() NOT NULL,

    FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    FOREIGN KEY (uploader_id) REFERENCES users (id)
);

CREATE INDEX idx_attachments_ticket ON attachments (ticket_id);

-- =========================
-- AUDIT LOG
-- =========================

CREATE TABLE audit_log
(
    id         BIGSERIAL PRIMARY KEY,
    ticket_id  BIGINT                  NOT NULL,
    actor_id   BIGINT                  NOT NULL,
    action     VARCHAR(50)             NOT NULL,
    old_value  VARCHAR(255),
    new_value  VARCHAR(255)            NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,

    FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    FOREIGN KEY (actor_id) REFERENCES users (id)
);

CREATE INDEX idx_audit_ticket ON audit_log (ticket_id, created_at DESC);