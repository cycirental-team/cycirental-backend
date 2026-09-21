CREATE TABLE items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description VARCHAR(500) NOT NULL,
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL
);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(40) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL
);

CREATE TABLE rentals (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    rented_at DATE NOT NULL,
    due_at DATE NOT NULL,
    returned_at DATE,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_rentals_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_rentals_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    rental_id BIGINT,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_rental FOREIGN KEY (rental_id) REFERENCES rentals(id)
);

INSERT INTO items (name, category, description, total_quantity, available_quantity) VALUES
    ('VR 기기', '실험/기타', '가상현실 체험 장비', 5, 3),
    ('빔프로젝터', '전자기기', '발표와 강의용 장비', 3, 2),
    ('카메라 (DSLR)', '촬영/영상', '사진과 영상 촬영용', 2, 0),
    ('삼각대', '촬영/영상', '촬영 보조 장비', 5, 4),
    ('조명 세트', '촬영/영상', '영상 촬영 조명', 4, 2),
    ('노트북', '전자기기', '영상 편집용', 1, 0);
