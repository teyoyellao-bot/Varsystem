CREATE TABLE IF NOT EXISTS web_login_attempts (
    login_key VARCHAR(255) PRIMARY KEY,
    failures INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_web_login_attempts_locked_until ON web_login_attempts(locked_until);
