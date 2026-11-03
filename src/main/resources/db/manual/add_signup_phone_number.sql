-- Prepared for manual review only; not executed automatically.
-- Check whether the column already exists before running ALTER.
SHOW COLUMNS FROM users LIKE 'phone_number';
ALTER TABLE users ADD COLUMN phone_number VARCHAR(11) NULL;
-- Existing accounts retain NULL; no phone verification or SMS integration is enabled.
