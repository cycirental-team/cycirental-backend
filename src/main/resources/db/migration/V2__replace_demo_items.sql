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
