-- Existing accounts keep NULL. New signups require a phone number in the API.
ALTER TABLE users ADD COLUMN phone_number VARCHAR(11) NULL;
