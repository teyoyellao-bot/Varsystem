CREATE TABLE IF NOT EXISTS web_email_verification_tokens (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES web_accounts(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_web_email_verify_account ON web_email_verification_tokens(account_id);
CREATE INDEX IF NOT EXISTS idx_web_email_verify_expiry ON web_email_verification_tokens(expires_at);
