-- Existing accounts created before the verification flow are grandfathered in.
UPDATE web_accounts SET email_verified = true WHERE email_verified = false;
