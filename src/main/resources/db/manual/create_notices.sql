-- Run manually only for the remote MariaDB profile, where Flyway is disabled.
-- Verify that the notices table does not already exist before executing this DDL.
CREATE TABLE notices (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    important BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX ix_notices_list ON notices (important, created_at, id);
