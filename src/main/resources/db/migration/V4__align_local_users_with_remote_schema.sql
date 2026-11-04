-- Forward-only migration for the local Flyway schema. Keep V1/V2/V3 unchanged.
-- The deployed MariaDB already uses these names and has Flyway disabled.
ALTER TABLE users RENAME COLUMN id TO user_id;
ALTER TABLE users RENAME COLUMN student_id TO login_id;
ALTER TABLE users RENAME COLUMN password_hash TO password;
ALTER TABLE users RENAME COLUMN display_name TO user_name;

-- Widen login IDs without removing existing data; do not shrink existing names.
ALTER TABLE users MODIFY COLUMN login_id VARCHAR(50) NOT NULL;
ALTER TABLE users ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;
