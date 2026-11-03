# 원격 DB 백업·인증 스키마 준비

2026-11-03 기준 로컬 코드의 적용 절차입니다. 실행 결과는 하단 상태 기록과 별도로 확인합니다. 이 문서를 읽거나 백엔드 main을 배포하는 것만으로 SQL이 적용되지는 않습니다.

## 로컬과 원격 구분

로컬 기본 H2는 Flyway V1~V8로 생성합니다. 원격 MariaDB 프로필은 Flyway가 꺼져 있고 `ddl-auto: none`이므로 필요한 변경을 따로 적용합니다.
원격은 기존 `users.user_id`가 signed INT인 구조를 기준으로 검토했습니다. 로컬의 사용자 번호는 BIGINT입니다. 외래 키 형식이 다르므로 로컬 마이그레이션을 원격에 그대로 실행하지 않습니다.
기존 Flyway 파일을 수정하거나 원격에서 baseline/repair를 임의 실행하지 않습니다.

## 1. 변경 전 백업

1. DBeaver 연결이 의도한 서버의 `rental_system`인지 확인합니다. 다른 로컬·실습 연결과 구분합니다.
2. 해당 DB의 전체 테이블을 선택해 구조와 데이터를 함께 SQL로 백업합니다. `Structure only`와 `No CREATE statements`는 해제합니다.
3. InnoDB 백업은 `--single-transaction`으로 일관된 스냅샷을 사용합니다. 백업 중 DDL 변경을 병행하지 않습니다.
4. 출력은 저장소 밖에 둡니다. 백업에는 개인정보·비밀번호 해시·세션 해시가 포함될 수 있으므로 Git에 넣지 않습니다.
5. 작업 성공·파일 크기·덤프 완료 표시·전체 테이블 CREATE 구문·기존 데이터 INSERT 포함 여부를 확인합니다. 빈 파일이나 오류로 끝난 파일은 백업으로 취급하지 않습니다.
6. 가능하면 별도 임시 DB에 복원해 테이블과 행 수를 비교합니다. 운영 DB에 백업을 덮어써서 복원 시험하지 않습니다.

DBeaver가 선택한 도구가 MariaDB의 `mariadb-dump`인지 MySQL의 `mysqldump`인지 확인합니다. `--column-statistics=0`은 MySQL용이며 MariaDB 도구에는 전달하지 않습니다. 새 도구 다운로드·실행이 필요하면 해당 실행 절차를 확인합니다.

## 2. 실제 구조 확인

[읽기 전용 사전 점검](src/main/resources/db/manual/preflight_auth_schema.sql)을 먼저 실행합니다. 컬럼 형식, NULL 허용, 인덱스, CHECK, FK, 행 수를 기록합니다.

```sql
SHOW CREATE TABLE users;
SHOW CREATE TABLE auth_sessions;
SHOW CREATE TABLE rentals;
```

현재 코드가 사용하는 주요 구조:

| 테이블 / 값 | 필요한 항목 |
| --- | --- |
| users 식별자 | user_id, login_id, user_name, password, is_admin |
| users 학번·연락처 | student_number VARCHAR(12), phone_number VARCHAR(11) |
| users 임시 비밀번호 | must_change_password, temporary_password_expires_at |
| 기존 이메일 | email 값을 보존하고 NULL 허용 |
| auth_sessions | id, token_hash, user_id, expires_at |
| password_reset_requests | id, user_id, requested_at, status, processed_by, processed_at |

## 3. 빠진 변경만 적용

아래 파일은 전체 실행 목록이 아니라 검토용 변경 단위입니다. 이미 존재하는 컬럼·인덱스·제약·테이블을 다시 만들지 않습니다. DDL은 자동 커밋될 수 있으므로 한 단계씩 실행하고 즉시 확인합니다.

| 순서 | 수동 SQL | 적용 조건 |
| --- | --- | --- |
| 1 | [add_student_number_preserve_email.sql](src/main/resources/db/manual/add_student_number_preserve_email.sql) | 학번 컬럼·유일 인덱스·숫자 CHECK가 없거나 email이 필수인 경우 필요한 구문만 |
| 2 | [add_admin_password_recovery.sql](src/main/resources/db/manual/add_admin_password_recovery.sql) | 임시 비밀번호 컬럼·초기화 요청 테이블이 없는 경우 |
| 3 | [add_signup_phone_number.sql](src/main/resources/db/manual/add_signup_phone_number.sql) | 전화번호 컬럼이 없는 경우 |
| 4 | [create_reserved_admin_001.sql](src/main/resources/db/manual/create_reserved_admin_001.sql) | 001 CHECK 예외와 관리자 계정이 필요한 경우 |

`password_reset_requests.user_id`와 `processed_by`는 실제 `users.user_id`와 같은 타입이어야 합니다. signed/unsigned도 맞춥니다.
`create_reserved_admin_001.sql`의 CHECK 교체 구문은 기존 CHECK 이름이 실제로 `ck_users_student_number`인지 확인한 뒤 실행합니다. 이미 최종 CHECK가 있으면 교체를 건너뛰고 INSERT 필요 여부만 확인합니다.
`replace_demo_items.sql`은 물품을 삭제·교체하는 별도 스크립트이므로 이번 인증 변경에 실행하지 않습니다.
`review_and_add_password_reset_tokens.sql`은 이전 토큰 방식용입니다. 새 초기화 흐름은 그 테이블을 사용하지 않으므로 없다는 이유로 새 흐름의 필수 작업으로 실행하지 않습니다. 이미 있는 테이블과 데이터는 보존합니다.

## 4. 기존 계정과 001 관리자

새 계정은 `login_id=student_number`로 저장하지만 로그인 조회는 `student_number`로 합니다. 기존 login_id·email·해시·대여·세션 데이터는 보존합니다.
기존 학번은 소유자 확인 없이 추정하거나 일괄 변환하지 않습니다. 학번이 NULL인 계정은 올바른 학번을 별도로 등록해야 새 방식으로 로그인할 수 있습니다.

예약 관리자 계정은 학번 `001`, 초기 비밀번호 `admin`으로 요청됐습니다. SQL에는 PBKDF2 해시가 들어 있고 앱 인증 우회나 자동 기본 계정 생성은 없습니다.
동일 login_id 또는 student_number가 있으면 INSERT하지 않습니다. 기존 계정의 비밀번호·권한을 덮어쓰지 않습니다. 전화번호·이메일 값을 임의로 채우지 않습니다.
일반 회원가입은 6~12자리 학번 규칙을 유지하고 001을 거부합니다. DB CHECK도 001에 대해서는 is_admin=true를 요구합니다.

## 5. 적용 후 확인

- 기존 users/items/rentals/auth_sessions 행 수와 FK가 보존됐는지 비교합니다. 관리자 등록 시 users는 필요한 경우에만 1행 늘어납니다.
- 새 컬럼·유일 인덱스·최종 CHECK·요청 테이블을 다시 조회합니다.
- 실제 사용자·비밀번호·전화번호·덤프 내용을 문서나 Git 로그에 기록하지 않습니다.
- 새 백엔드 배포 후 회원가입·학번 로그인·관리자 메뉴·초기화·비밀번호 변경·로그아웃을 실폰에서 확인합니다.
- 배포 전 서버는 예전 API일 수 있습니다. `/api/health` 성공만으로 새 인증과 DB 적용 성공을 판단하지 않습니다.

## 실행 상태

- 변경 전 백업: 2026-11-03 18:51 KST 생성 완료. 5,526바이트, 테이블 4개·데이터 INSERT 3개·덤프 완료 표시와 SHA-256을 확인했습니다. 별도 DB 복원 시험은 하지 않았습니다.
- 원격 스키마 변경: 2026-11-03 18:57~18:58 KST 적용 완료. users 컬럼 4개·email NULL 허용·학번 유일 인덱스·최종 CHECK·초기화 요청 테이블·FK 2개를 추가했습니다.
- 18:59 KST 검증: 기존 users 1 / items 5 / rentals 0 / auth_sessions 1행 보존, 새 컬럼 4개·요청 테이블 1개·외래 키 2개 확인.
- 001 관리자 계정: 사용자 승인 후 2026-11-03 21:26 KST 생성 완료. 비밀번호는 해시로 저장했습니다. users 2 / items 5 / rentals 0 / auth_sessions 1행과 관리자 001 1건·기존 사용자 1건 보존을 확인했습니다. 새 백엔드 배포 후 실폰 로그인을 확인해야 합니다.
- 백엔드 배포·Git 업로드: 사용자가 별도 진행.

실제 완료 시각·백업 파일 위치·변경 전후 행 수는 저장소 밖의 로컬 작업 기록에 남기고, 이 상태 항목도 실제 결과에 맞춰 갱신합니다.
