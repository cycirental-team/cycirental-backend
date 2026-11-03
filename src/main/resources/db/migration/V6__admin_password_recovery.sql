ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN temporary_password_expires_at TIMESTAMP NULL;
CREATE TABLE password_reset_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL,
    processed_by BIGINT NULL,
    processed_at TIMESTAMP NULL,
    CONSTRAINT fk_recovery_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_recovery_admin FOREIGN KEY (processed_by) REFERENCES users(user_id)
);
CREATE INDEX idx_recovery_status_time ON password_reset_requests(status, requested_at);
