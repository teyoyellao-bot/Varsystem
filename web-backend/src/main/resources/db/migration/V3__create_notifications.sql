-- Persistent notifications for the VAARL Web System.
CREATE TABLE IF NOT EXISTS web_notifications (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES web_accounts(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    related_request_id UUID REFERENCES web_assistance_requests(id) ON DELETE SET NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_web_notifications_account_created
    ON web_notifications(account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_web_notifications_account_unread
    ON web_notifications(account_id, is_read);
