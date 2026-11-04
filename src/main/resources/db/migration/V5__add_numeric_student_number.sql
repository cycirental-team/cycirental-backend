-- Preserve legacy email values and never guess student numbers for old accounts.
ALTER TABLE users ADD COLUMN student_number VARCHAR(12) NULL;
CREATE UNIQUE INDEX ux_users_student_number ON users (student_number);
ALTER TABLE users ADD CONSTRAINT ck_users_student_number
    CHECK (student_number IS NULL OR student_number REGEXP '^[0-9]{6,12}$');
-- Signup no longer collects email. Keep the old column only for existing data.
ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL;
