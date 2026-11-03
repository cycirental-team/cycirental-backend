-- MANUAL ONLY. Apply the reviewed schema changes through V7 before running.
-- Back up first and inspect these results. Do not overwrite or promote a matching account.
SELECT user_id, login_id, student_number, is_admin FROM users
WHERE login_id = '001' OR student_number = '001';
-- Run the following DDL only after confirming the existing check constraint's name.
ALTER TABLE users DROP CONSTRAINT ck_users_student_number;
ALTER TABLE users ADD CONSTRAINT ck_users_student_number CHECK (
 student_number IS NULL OR student_number REGEXP '^[0-9]{6,12}$'
 OR (student_number = '001' AND is_admin = TRUE)
);
-- Explicitly requested administrator: student number 001, initial password admin.
-- The application verifies this PBKDF2 hash through the usual login API.
-- Existing accounts and passwords are never changed. No phone number is invented.
INSERT INTO users (login_id, student_number, user_name, password, is_admin, must_change_password)
SELECT '001', '001', '관리자', '210000:TSZAqHO2gmd08pL1yMUVMA==:59zLj4mrqH9vFYg9Y62DOvS6EHV/cDlmQ8Q3aDZ2yis=', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE login_id = '001' OR student_number = '001');
