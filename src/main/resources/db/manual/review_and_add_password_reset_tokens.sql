-- MANUAL ONLY: review table definitions and take a backup before running CREATE.
-- This file is NOT loaded automatically by Flyway or the application.
-- Existing users/auth_sessions/rentals are never renamed, dropped, or cleared.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('users', 'auth_sessions', 'password_reset_tokens')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME,
       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME = 'users';

-- Run the following statement only after confirming users.user_id is signed INT,
-- users uses InnoDB, and password_reset_tokens does not already exist.
-- A different user_id type (e.g. BIGINT/UNSIGNED) requires the FK type to match.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
) ENGINE=InnoDB;

SHOW CREATE TABLE password_reset_tokens;
