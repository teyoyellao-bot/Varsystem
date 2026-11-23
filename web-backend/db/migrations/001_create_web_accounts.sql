-- Additive migration for VAARL web accounts. Desktop local data is untouched.
CREATE TABLE IF NOT EXISTS web_accounts (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(60) NOT NULL UNIQUE,
    phone VARCHAR(40) NOT NULL,
    account_role VARCHAR(20) NOT NULL CHECK (account_role IN ('VEHICLE_OWNER', 'REPAIR_SHOP', 'ADMIN')),
    shop_name VARCHAR(160),
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT web_accounts_shop_role_check CHECK (
        (account_role = 'REPAIR_SHOP' AND shop_name IS NOT NULL AND length(trim(shop_name)) > 0)
        OR (account_role <> 'REPAIR_SHOP')
    )
);
CREATE INDEX IF NOT EXISTS idx_web_accounts_role ON web_accounts(account_role);