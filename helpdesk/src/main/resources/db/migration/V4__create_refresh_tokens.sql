-- V4__create_refresh_tokens.sql
CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(512) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_value ON refresh_tokens (token);
CREATE INDEX idx_refresh_token_user ON refresh_tokens (user_id);