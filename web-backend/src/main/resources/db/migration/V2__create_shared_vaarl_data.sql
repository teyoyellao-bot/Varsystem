-- Shared VAARL data schema (additive; does not alter the desktop application's local files).
-- Apply after 001_create_web_accounts.sql.

CREATE TABLE IF NOT EXISTS web_repair_shops (
    id UUID PRIMARY KEY,
    account_id UUID UNIQUE REFERENCES web_accounts(id) ON DELETE SET NULL,
    shop_name VARCHAR(160) NOT NULL,
    location_text VARCHAR(255),
    phone VARCHAR(40),
    hours_text VARCHAR(120),
    about_text TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_web_repair_shops_name ON web_repair_shops(lower(shop_name));
CREATE INDEX IF NOT EXISTS idx_web_repair_shops_status ON web_repair_shops(status);

CREATE TABLE IF NOT EXISTS web_shop_services (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES web_repair_shops(id) ON DELETE CASCADE,
    service_name VARCHAR(120) NOT NULL,
    UNIQUE(shop_id, service_name)
);

CREATE TABLE IF NOT EXISTS web_mechanics (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES web_repair_shops(id) ON DELETE CASCADE,
    mechanic_name VARCHAR(120) NOT NULL,
    phone VARCHAR(40),
    availability VARCHAR(30) NOT NULL DEFAULT 'Available',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS web_assistance_requests (
    id UUID PRIMARY KEY,
    requester_account_id UUID REFERENCES web_accounts(id) ON DELETE SET NULL,
    shop_id UUID NOT NULL REFERENCES web_repair_shops(id) ON DELETE RESTRICT,
    mechanic_id UUID REFERENCES web_mechanics(id) ON DELETE SET NULL,
    requester_name VARCHAR(120) NOT NULL,
    requester_phone VARCHAR(40),
    vehicle VARCHAR(120),
    problem VARCHAR(255),
    location_text VARCHAR(255),
    details TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    payment_method VARCHAR(80) DEFAULT 'Cash / GCash',
    fee NUMERIC(12,2) DEFAULT 0,
    travel_fee NUMERIC(12,2) DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_web_requests_shop_status ON web_assistance_requests(shop_id, status);
CREATE INDEX IF NOT EXISTS idx_web_requests_requester ON web_assistance_requests(requester_account_id);

CREATE TABLE IF NOT EXISTS web_feedback (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE REFERENCES web_assistance_requests(id) ON DELETE CASCADE,
    shop_id UUID NOT NULL REFERENCES web_repair_shops(id) ON DELETE CASCADE,
    requester_account_id UUID REFERENCES web_accounts(id) ON DELETE SET NULL,
    requester_name VARCHAR(120),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS web_support_requests (
    id UUID PRIMARY KEY,
    account_id UUID REFERENCES web_accounts(id) ON DELETE SET NULL,
    requester_name VARCHAR(120) NOT NULL,
    email VARCHAR(255),
    subject VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);
