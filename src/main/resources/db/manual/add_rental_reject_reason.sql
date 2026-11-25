-- 대여 거절 사유 칸 추가 (원격 MariaDB 수동 적용용)
-- 적용 전: 백업하고, SHOW CREATE TABLE rentals; 로 reject_reason 칸이 없는지 확인
ALTER TABLE rentals ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(300) NULL AFTER status;