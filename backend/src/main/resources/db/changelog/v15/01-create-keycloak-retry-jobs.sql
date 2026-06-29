-- V15: Keycloak retry jobs queue for failed role operations
CREATE TABLE IF NOT EXISTS keycloak_retry_jobs (
    id              BIGSERIAL PRIMARY KEY,
    operation_type  VARCHAR(100)   NOT NULL,
    payload_json    VARCHAR(2000)  NOT NULL,
    attempt_count   INT            NOT NULL DEFAULT 0,
    max_attempts    INT            NOT NULL DEFAULT 5,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    next_attempt_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    last_error      TEXT,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_retry_status ON keycloak_retry_jobs(status);
CREATE INDEX IF NOT EXISTS idx_retry_next_attempt ON keycloak_retry_jobs(next_attempt_at);
