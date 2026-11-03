-- MANUAL ONLY. Back up structure + data and review this SELECT first.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
ORDER BY ORDINAL_POSITION;

-- Only if student_number is absent; keep users/user_id/FKs and all existing data.
ALTER TABLE users ADD COLUMN student_number VARCHAR(12) NULL;
CREATE UNIQUE INDEX ux_users_student_number ON users (student_number);
ALTER TABLE users ADD CONSTRAINT ck_users_student_number
    CHECK (student_number IS NULL OR student_number REGEXP '^[0-9]{6,12}$');
ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL;

-- Legacy student numbers remain NULL until confirmed by the owner/admin.
-- Do NOT copy login_id/email to student_number or delete the email column/data.
