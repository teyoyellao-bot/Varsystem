CREATE TABLE IF NOT EXISTS web_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NULL REFERENCES web_accounts(id) ON DELETE SET NULL,
    actor_username VARCHAR(255) NULL,
    event_type VARCHAR(80) NOT NULL,
    details VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_web_audit_logs_occurred_at ON web_audit_logs(occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_web_audit_logs_event_type ON web_audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_web_audit_logs_account_id ON web_audit_logs(account_id);
