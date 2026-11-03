-- Only administrator accounts may use the reserved short student number 001.
ALTER TABLE users DROP CONSTRAINT ck_users_student_number;
ALTER TABLE users ADD CONSTRAINT ck_users_student_number CHECK (
    student_number IS NULL OR student_number REGEXP '^[0-9]{6,12}$'
    OR (student_number = '001' AND is_admin = TRUE)
);
-- Account creation is manual; this migration does not create default credentials.
