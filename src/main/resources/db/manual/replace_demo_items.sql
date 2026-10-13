-- 원격 MariaDB에서 DBeaver로 한 번만 실행하는 물품 기초 데이터 교체 스크립트입니다.
-- 기존 rentals 행이 아래 물품을 참조하면 외래 키 제약으로 DELETE가 실패합니다.
-- 실패할 경우 대여 이력을 임의로 삭제하지 말고 Backend 담당자와 확인합니다.

START TRANSACTION;

DELETE FROM items
WHERE name IN (
    'VR 기기',
    '빔프로젝터',
    '카메라 (DSLR)',
    '삼각대',
    '조명 세트',
    '노트북',
    '4090본체',
    '모니터'
);

INSERT INTO items (name, category, description, total_quantity, available_quantity) VALUES
    ('4090본체', '전자기기', '2210호에 배치된 본체', 18, 18),
    ('4090본체', '전자기기', '2211호에 배치된 본체', 27, 27),
    ('모니터', '전자기기', '2210호에 배치된 모니터', 30, 30),
    ('모니터', '전자기기', '2211호에 배치된 모니터', 24, 24),
    ('VR 기기', '실험/기타', '3401호에 배치된 VR 기기', 3, 3);

COMMIT;

SELECT id, name, category, description, total_quantity, available_quantity
FROM items
ORDER BY id;
