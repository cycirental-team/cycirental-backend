-- Remote users.user_id is INT signed. Review schema and back up before applying.
-- Apply add_student_number_preserve_email.sql first; never guess legacy student numbers.
-- This script is prepared only, NOT executed by the application.
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN temporary_password_expires_at DATETIME NULL;
CREATE TABLE password_reset_requests (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL,
 requested_at DATETIME NOT NULL, status VARCHAR(16) NOT NULL,
 processed_by INT NULL, processed_at DATETIME NULL,
 CONSTRAINT fk_recovery_user FOREIGN KEY(user_id) REFERENCES users(user_id),
 CONSTRAINT fk_recovery_admin FOREIGN KEY(processed_by) REFERENCES users(user_id),
 INDEX idx_recovery_status_time(status, requested_at)
);
