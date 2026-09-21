-- =========================
-- KNOWLEDGE_ARTICLES
-- =========================

CREATE TABLE knowledge_articles
(
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(200) NOT NULL,
    slug              VARCHAR(220) NOT NULL UNIQUE,
    summary           VARCHAR(500),
    content           TEXT         NOT NULL,
    type              VARCHAR(30)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    category_id       BIGINT REFERENCES ticket_categories (id),
    author_id         BIGINT       NOT NULL REFERENCES users (id),
    view_count        BIGINT       NOT NULL DEFAULT 0,
    helpful_count     INT          NOT NULL DEFAULT 0,
    not_helpful_count INT          NOT NULL DEFAULT 0,
    usage_count       INT          NOT NULL DEFAULT 0,
    published_at      TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT knowledge_articles_type_check
        CHECK (type IN ('TROUBLESHOOTING_GUIDE', 'FAQ', 'STANDARD_OPERATING_PROCEDURE', 'GENERAL')),
    CONSTRAINT knowledge_articles_status_check
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

-- Generated column rather than a Java-maintained one, so the search index can never drift out of
-- sync with title/summary/content - Postgres recomputes it on every write to those columns.
-- Weighted A/B/C so a title match ranks above a match buried in the body text.
ALTER TABLE knowledge_articles
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(summary, '')), 'B') ||
            setweight(to_tsvector('english', coalesce(content, '')), 'C')
            ) STORED;

CREATE INDEX idx_knowledge_articles_search ON knowledge_articles USING GIN (search_vector);
CREATE INDEX idx_knowledge_articles_status ON knowledge_articles (status);
CREATE INDEX idx_knowledge_articles_type ON knowledge_articles (type);
CREATE INDEX idx_knowledge_articles_category ON knowledge_articles (category_id);

-- =========================
-- KNOWLEDGE_ARTICLE_TAGS
-- =========================

CREATE TABLE knowledge_article_tags
(
    article_id BIGINT      NOT NULL REFERENCES knowledge_articles (id) ON DELETE CASCADE,
    tag        VARCHAR(50) NOT NULL,

    PRIMARY KEY (article_id, tag)
);

CREATE INDEX idx_knowledge_article_tags_tag ON knowledge_article_tags (tag);

-- =========================
-- KNOWLEDGE_ARTICLE_FEEDBACK
-- =========================

CREATE TABLE knowledge_article_feedback
(
    id         BIGSERIAL PRIMARY KEY,
    article_id BIGINT    NOT NULL REFERENCES knowledge_articles (id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    helpful    BOOLEAN   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_knowledge_article_feedback_article_user UNIQUE (article_id, user_id)
);

-- =========================
-- TICKET_KNOWLEDGE_ARTICLES (link table)
-- =========================

CREATE TABLE ticket_knowledge_articles
(
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT       NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    article_id     BIGINT       NOT NULL REFERENCES knowledge_articles (id),
    linked_by_id   BIGINT       NOT NULL REFERENCES users (id),
    linked_by_name VARCHAR(100) NOT NULL,
    linked_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uq_ticket_knowledge_articles_ticket_article UNIQUE (ticket_id, article_id)
);

CREATE INDEX idx_ticket_kb_articles_ticket ON ticket_knowledge_articles (ticket_id);
CREATE INDEX idx_ticket_kb_articles_article ON ticket_knowledge_articles (article_id, linked_at DESC);

-- =========================
-- SEED: demo articles
-- =========================
-- Authored by the seeded System Administrator (user id 1, see V2__seed_data.sql). Linked to
-- subcategories seeded in V8__create_ticket_categories.sql by slug, since those rows don't have
-- fixed ids. Four published, one left as a draft to exercise the DRAFT -> PUBLISHED workflow.

INSERT INTO knowledge_articles (title, slug, summary, content, type, status, category_id, author_id,
                                helpful_count, not_helpful_count, published_at)
VALUES ('How to Fix a Laptop That Won''t Turn On',
        'how-to-fix-a-laptop-that-wont-turn-on',
        'Quick checks to run before logging a hardware ticket for a laptop that appears dead.',
        $body$1. Hold the power button for 15 seconds to force a full discharge, then try again.
        2. Confirm the charging light is on; try a different power outlet and cable if not.
3. Remove any USB devices or docking stations and retry.
4. If a battery is removable, take it out and attempt to boot on mains power alone.

If none of these steps work, log a ticket under Hardware > Laptop with the steps you already
tried - this saves the technician repeating them.$body$,
        'TROUBLESHOOTING_GUIDE', 'PUBLISHED',
        (SELECT id FROM ticket_categories WHERE slug = 'hardware-laptop'),
        1, 12, 1, now()),

       ('Requesting VPN Access',
        'requesting-vpn-access',
        'Who can request VPN access and how long approval typically takes.',
        $body$VPN access is available to staff who need to work remotely or connect to internal
systems from outside the office network.

To request access, log a ticket under Network > VPN with your manager's name for approval.
Access is typically provisioned within one business day once approved. You will receive a
client configuration file and setup instructions by email.$body$,
        'FAQ', 'PUBLISHED',
        (SELECT id FROM ticket_categories WHERE slug = 'network-vpn'),
        1, 8, 0, now()),

       ('Password Reset Self-Service Guide',
        'password-reset-self-service-guide',
        'Reset your own password without waiting for an agent.',
        $body$1. Go to the login page and select "Forgot password".
2. Enter your registered email address - you will receive a one-time PIN (OTP).
3. Enter the OTP and choose a new password meeting the complexity requirements.

OTPs expire after 15 minutes. If you don't receive one, check your spam folder before logging
a ticket under Accounts > Password Reset.$body$,
        'STANDARD_OPERATING_PROCEDURE', 'PUBLISHED',
        (SELECT id FROM ticket_categories WHERE slug = 'accounts-password-reset'),
        1, 21, 2, now()),

       ('Reporting a Phishing Email',
        'reporting-a-phishing-email',
        'What to do if you receive a suspicious email claiming to be from IT or a supplier.',
        $body$Do not click any links or open attachments. Do not reply to the sender.

Forward the email as an attachment to the security team, or log a ticket under Security >
Phishing Report with a screenshot. Delete the email afterwards. If you already clicked a link
or entered credentials, log a ticket immediately and mention this so the account can be
secured as a priority.$body$,
        'FAQ', 'PUBLISHED',
        (SELECT id FROM ticket_categories WHERE slug = 'security-phishing-report'),
        1, 15, 0, now()),

       ('Draft: New Starter Onboarding Checklist',
        'draft-new-starter-onboarding-checklist',
        'Internal checklist for provisioning a new employee''s accounts and equipment.',
        $body$Draft - pending review before publishing.

1. Create user account and assign default role.
2. Allocate a laptop asset and record it against the new starter.
3. Grant access to required systems per their department.
4. Schedule a welcome call to walk through VPN and password reset self-service.$body$,
        'STANDARD_OPERATING_PROCEDURE', 'DRAFT',
        (SELECT id FROM ticket_categories WHERE slug = 'accounts-new-user-onboarding'),
        1, 0, 0, NULL);

INSERT INTO knowledge_article_tags (article_id, tag)
SELECT ka.id, seed.tag
FROM (VALUES
          ('how-to-fix-a-laptop-that-wont-turn-on', 'laptop'),
          ('how-to-fix-a-laptop-that-wont-turn-on', 'power'),
          ('how-to-fix-a-laptop-that-wont-turn-on', 'hardware'),
          ('requesting-vpn-access', 'vpn'),
          ('requesting-vpn-access', 'remote-work'),
          ('password-reset-self-service-guide', 'password'),
          ('password-reset-self-service-guide', 'account'),
          ('reporting-a-phishing-email', 'security'),
          ('reporting-a-phishing-email', 'phishing'),
          ('draft-new-starter-onboarding-checklist', 'onboarding')
     ) AS seed(slug, tag)
         JOIN knowledge_articles ka ON ka.slug = seed.slug;
