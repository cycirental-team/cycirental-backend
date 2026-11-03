# Git 업로드 전 준비 — Backend

## 순서

1. [REMOTE_SCHEMA_ALIGNMENT.md](REMOTE_SCHEMA_ALIGNMENT.md)에 따라 원격 DB 전체 백업을 검증합니다.
2. 실제 DB 구조와 비교해 필요한 인증·학번·전화번호·초기화 구조만 적용합니다.
3. 기존 계정은 보존하고 예약 관리자 001 등록 결과를 확인합니다.
4. 로컬 코드 검사와 문서를 정리합니다.
5. Git에 포함할 파일과 브랜치를 검토합니다. push·PR·main 병합·서버 배포는 사용자가 직접 합니다.

## 로컬 검증

```powershell
cd C:\fq_mob\cycirental-backend
.\mvnw.cmd --batch-mode --no-transfer-progress test
.\mvnw.cmd --batch-mode --no-transfer-progress -DskipTests package
git status --short
git diff --check
git diff --stat
```

테스트는 기본 H2 환경에서 실행하며 원격 MariaDB 계정을 테스트 환경으로 사용하지 않습니다. 실행 환경에 DB 관련 변수를 설정했다면 테스트가 H2를 사용하는지 먼저 확인합니다.
원격 구조는 로컬과 사용자 번호 타입이 달라 H2 테스트 성공만으로 원격 SQL이 검증되지는 않습니다.

## 포함 파일 확인

코드·테스트·마이그레이션·수동 SQL·문서 변경을 함께 확인합니다. 새 V4~V8은 추가 파일이며 기존 V1~V3은 수정하지 않습니다.
실제 IP, DB 비밀번호, .env, 덤프, 로그, 빌드 산출물은 올리지 않습니다. .env.example에는 자리표시자만 둡니다.
수동 관리자 등록 SQL은 기존 계정 덮어쓰기가 없는지 확인합니다. 001 계정은 SQL을 실행해야 생기며 배포로 자동 생성되지 않습니다.

## 프론트와 분리

백엔드 `back/* → back-dev → main`, 프론트 `front/* → front-dev → main` 흐름을 유지합니다. 공통 협업 규칙을 바꾸지 않습니다.
이미 main에 로컬 변경이 있으면 최신화나 브랜치 전환 전에 변경을 보존하고 각 저장소 상태를 확인합니다.
백엔드 main 반영은 `.github/workflows/deploy.yml`을 실행합니다. 원격 Flyway가 비활성화돼 있어 DB DDL은 자동 적용되지 않습니다.
프론트 Git 반영은 서버 배포와 별개입니다. Android Studio에서 `mobile` 프로젝트와 Git 제외된 로컬 API 설정으로 휴대폰을 선택해 Run합니다.

새 백엔드 배포 전까지 `/api/admin/password-reset-requests`가 404일 수 있습니다. 배포 후 무토큰 요청이 401인지, 유효한 관리자 토큰 요청이 200인지 구분해 검증합니다.
