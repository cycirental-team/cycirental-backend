# CYCIRENTAL Backend

학과 대여 물품 관리 앱 **CYCIRENTAL**의 Backend 저장소입니다.

Backend는 **Spring Boot / Java 21**을 사용하며,
Frontend에 필요한 REST API 제공, Database 연동 및 실제 서버 배포를 담당합니다.

---

## 저장소

- **Backend:** 현재 저장소
- **Frontend:** [CYCIRENTAL Frontend](https://github.com/jinwook0308/cycirental)

Frontend와 Backend는 각각 별도의 저장소에서 개발하며 REST API를 통해 연동합니다.

---

## Backend 담당 범위

이 저장소에서는 다음 영역을 관리합니다.

- REST API 개발
- 사용자 / 물품 / 대여 관련 Backend 로직
- Database 연동
- H2 기반 로컬 개발 환경
- MariaDB 원격 Database 연동
- Backend 테스트
- 실제 서버용 Backend 빌드 및 실행
- GitHub Actions를 이용한 자동 배포

Frontend 화면 및 UI / UX 관련 내용은
[Frontend 저장소](https://github.com/jinwook0308/cycirental)에서 관리합니다.

---

## 개발 환경

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build | Maven Wrapper |
| ORM | Spring Data JPA |
| Local Database | H2 |
| Remote Database | MariaDB |
| Migration | Flyway |
| CI / CD | GitHub Actions |
| Server | Self-hosted Windows Server |

---

## 프로젝트 구조

```text
cycirental-backend
├─ .github
│  └─ workflows
│     └─ deploy.yml
│
├─ src
│  ├─ main
│  │  ├─ java
│  │  │  └─ kr/ac/cyci/deptrental
│  │  │     ├─ DeptRentalApiApplication.java
│  │  │     ├─ HealthController.java
│  │  │     ├─ auth  # 학번 인증·관리자 초기화
│  │  │     └─ item
│  │  │        ├─ Item.java
│  │  │        ├─ ItemController.java
│  │  │        └─ ItemRepository.java
│  │  │
│  │  └─ resources
│  │     ├─ application.yml
│  │     ├─ application-mariadb.yml
│  │     └─ db
│  │        ├─ migration
│  │        │  ├─ V1__initial_schema.sql
│  │        │  ├─ V2__replace_demo_items.sql
│  │        │  └─ V3~V8 인증·원격 매핑·학번·초기화·전화번호·001 제약 변경
│  │        └─ manual
│  │           └─ replace_demo_items.sql
│  │
│  └─ test
│
├─ .env.example
├─ pom.xml
├─ mvnw
├─ mvnw.cmd
└─ README.md
```

---

## 로컬 실행

Windows PowerShell 기준:

```powershell
.\mvnw.cmd spring-boot:run
```

기본 설정은 H2 인메모리 Database를 사용합니다.

```text
Address : 127.0.0.1
Port    : 8080
```

기본 주소:

```text
http://127.0.0.1:8080
```

---

## 현재 확인 가능한 API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/health` | Backend 정상 실행 여부 확인 |
| GET | `/api/items` | 전체 물품 조회 |
| GET | `/api/items?query=검색어` | 물품 이름 검색 (대소문자 무시) |
| GET | `/api/items?category=카테고리` | 카테고리별 물품 조회 |
| GET | `/api/items?availableOnly=true` | 대여 가능한 물품만 조회 |
| GET | `/api/items/categories` | 카테고리 목록 조회 |
| GET | `/api/items/{id}` | 물품 상세 조회 |

`query`, `category`, `availableOnly`는 함께 사용할 수 있습니다.

### 인증 API

인증 기능은 `auth` 모듈에 분리되어 있습니다. 비밀번호 원문과 액세스 토큰 원문은 DB에 저장하지 않습니다. 관리자 초기화의 임시 비밀번호도 해시만 저장합니다.

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 및 Bearer 액세스 토큰 발급 (7일) |
| POST | `/api/auth/logout` | 현재 Bearer 토큰 폐기 |
| POST | `/api/auth/password-reset/requests` | 이름·학번으로 관리자 초기화 요청 |
| POST | `/api/auth/password/change` | 인증 후 비밀번호 변경 |

회원가입 요청 예시:

```json
{
  "password": "safe-password",
  "displayName": "홍길동",
  "studentNumber": "20260001",
  "phoneNumber": "01012345678"
}
```

로그인은 `studentNumber`와 `password`를 보냅니다. 이전 프론트의 `loginId`·`email` 요청 형식은 현재 계약과 다르며 필수 학번·전화번호가 빠지면 400으로 응답합니다.

```json
{"studentNumber":"20260001","password":"safe-password"}
```

`displayName`은 API 필드명이며 DB의 기존 `users.user_name` 컬럼에 저장됩니다. `studentNumber`는 `student_number`, `phoneNumber`는 `phone_number`에 저장하고 기존 이메일 데이터는 보존합니다. 일반 가입은 학생 권한으로 생성하며 비밀번호는 해시로 저장합니다.

Android Medium Phone에서 로컬 백엔드에 접근할 때 프론트 공용 기본값은 `http://10.0.2.2:8080`이고 백엔드 기본 포트는 8080입니다. 배포 서버의 8081 포트는 별도 실행 설정이며, 배포 포트에 맞춘다고 로컬 기본값을 함께 바꾸지 않습니다. 다른 주소는 프론트의 로컬 `API_BASE_URL` 설정으로만 전달합니다.

로그인 응답의 `accessToken`은 이후 보호 API 호출 시 다음과 같이 보냅니다.

```text
Authorization: Bearer {accessToken}
```

비밀번호 초기화는 아래의 학번 로그인·관리자 처리 절차를 사용합니다. 이메일 발송과 기존 재설정 토큰 방식은 사용하지 않습니다.

예시:

```text
GET http://127.0.0.1:8080/api/health
GET http://127.0.0.1:8080/api/items
GET http://127.0.0.1:8080/api/items?query=VR
GET http://127.0.0.1:8080/api/items?category=전자기기
GET http://127.0.0.1:8080/api/items?category=전자기기&availableOnly=true
GET http://127.0.0.1:8080/api/items/categories
GET http://127.0.0.1:8080/api/items/1
```

현재 구현된 API만 이 문서에 작성하며,
새 API가 추가되면 실제 코드와 함께 README도 수정합니다.

---

## 구현 완료 기능

| 기능 | 상태 | 구현 내용 |
| --- | --- | --- |
| Backend 상태 확인 | 완료 | `GET /api/health`로 서버 실행 상태 확인 |
| 전체 물품 조회 | 완료 | `GET /api/items`로 DB에 저장된 전체 물품 반환 |
| 물품 이름 검색 | 완료 | `GET /api/items?query=검색어`로 이름 검색 |
| 카테고리별 물품 조회 | 완료 | `GET /api/items?category=카테고리`로 카테고리 필터 |
| 대여 가능 물품 조회 | 완료 | `GET /api/items?availableOnly=true`로 남은 수량이 있는 물품만 조회 |
| 카테고리 목록 조회 | 완료 | `GET /api/items/categories`로 카테고리 목록 반환 |
| 물품 상세 조회 | 완료 | `GET /api/items/{id}`로 단일 물품 조회 |
| 로컬 Database | 완료 | H2와 Flyway를 이용해 스키마 및 기준 물품 데이터 자동 구성 |
| 원격 Database 연결 설정 | 완료 | `mariadb` 프로필과 환경 변수로 MariaDB 접속 가능 |
| 원격 기준 데이터 반영 스크립트 | 완료 | DBeaver에서 검토 후 실행할 수 있는 수동 SQL 제공 |
| 회원가입·로그인 | 완료 | 비밀번호 해시 저장, 로그인 및 Bearer 토큰 발급 |
| 로그아웃 | 완료 | 발급된 Bearer 토큰 폐기 |
| 아이디 찾기 | 제거 | 학번을 로그인 아이디로 사용 |
| 비밀번호 초기화 | 로컬 구현 | 관리자 본인 확인·임시 비밀번호·변경 강제, 배포 전 |

대여 신청·반납, 알림 API는 아직 구현 전입니다.
Frontend의 대여·반납 기능은 해당 API가 추가되기 전까지 앱 내부 임시 상태로 동작합니다.

---

## 현재 물품 기준 데이터

로컬 H2 환경에서는 Flyway `V2__replace_demo_items.sql`이 기존 예시 물품을 제거하고
다음 5개 물품을 등록합니다.

| 이름 | 카테고리 | 설명 | 전체 수량 | 대여 가능 수량 |
| --- | --- | --- | ---: | ---: |
| 4090본체 | 전자기기 | 2210호에 배치된 본체 | 18 | 18 |
| 4090본체 | 전자기기 | 2211호에 배치된 본체 | 27 | 27 |
| 모니터 | 전자기기 | 2210호에 배치된 모니터 | 30 | 30 |
| 모니터 | 전자기기 | 2211호에 배치된 모니터 | 24 | 24 |
| VR 기기 | 실험/기타 | 3401호에 배치된 VR 기기 | 3 | 3 |

현재는 초기 대여 가능 수량을 전체 수량과 동일하게 설정합니다.

---

## 테스트

```powershell
.\mvnw.cmd test
```

Merge 전에는 수정한 Backend 기능과 기존 API가 정상적으로 동작하는지 확인합니다.

---

## Database

### 로컬 개발

기본 프로필에서는 **H2 인메모리 Database**를 사용합니다.

로컬 개발 환경은 실제 MariaDB를 직접 수정하지 않고
Backend 기능을 실행하고 테스트하기 위한 용도로 사용합니다.

---

### MariaDB 연결

원격 MariaDB를 사용할 때는 `mariadb` 프로필을 활성화합니다.

필요한 환경 변수 이름은 [.env.example](.env.example)에서 확인할 수 있습니다.

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
```

PowerShell 실행 예시:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mariadb"
$env:DB_HOST = "팀에서 받은 호스트"
$env:DB_PORT = "3306"
$env:DB_NAME = "팀에서 받은 DB 이름"
$env:DB_USER = "팀에서 받은 사용자"
$env:DB_PASSWORD = "팀에서 받은 비밀번호"

.\mvnw.cmd spring-boot:run
```

Spring Boot는 `.env` 파일을 자동으로 읽지 않습니다.

환경 변수는 현재 PowerShell 세션 또는 IntelliJ 실행 설정을 통해 전달합니다.

#### IntelliJ에서 원격 DB로 실행하기

백엔드 폴더에서 다음 설정 도구를 실행합니다. 호스트·포트·DB 이름·사용자와 비밀번호는 창에서 입력하며 비밀번호는 가려집니다.

```powershell
.\tool\configure_mariadb.cmd
```

도구는 이 PC에만 사용하는 `.run/DeptRental_MariaDB.local.run.xml` 실행 설정을 만듭니다. 이 파일과 `.env`는 Git에서 제외됩니다. 비밀번호는 이 로컬 실행 설정에 저장되므로 해당 파일을 공유하지 않습니다.

IntelliJ 상단에서 **DeptRental - MariaDB (local 8080)**을 선택합니다. 기존 8080 백엔드를 Stop한 뒤 새 설정으로 Run합니다. 새 설정이 보이지 않으면 File → Reload All from Disk로 프로젝트 파일을 새로 읽습니다.

이 실행 설정은 `mariadb` 프로필과 DB 환경 변수를 적용하고 로컬 API 포트는 8080을 유지합니다. 따라서 Android Medium Phone의 프론트 기본 주소 `http://10.0.2.2:8080`은 바꿀 필요가 없습니다. 백엔드가 PC의 8080에서 요청을 받고 원격 MariaDB에 저장합니다.

기본 **DeptRentalApiApplication** 실행 항목과 Maven 테스트는 여전히 H2를 사용할 수 있습니다. 가입 성공 여부만 보지 말고, 실행 프로필과 연결 DB가 의도한 MariaDB인지 확인합니다. H2에 가입했던 계정이 원격 DB로 자동 복사되지는 않습니다. 원격 프로필은 기존과 같이 Flyway 비활성화·`ddl-auto: none`을 유지해 테이블을 자동 변경하지 않습니다.

실제 DB 주소, 계정 및 비밀번호는 GitHub에 Commit하지 않습니다.

---

## MariaDB 보호 설정

원격 MariaDB 프로필에서는 자동으로 Database 구조를 변경하지 않도록 설정되어 있습니다.

```text
Hibernate ddl-auto : none
Flyway             : disabled
```

따라서 원격 Database의 테이블 또는 컬럼 구조를 변경해야 하는 경우
임의로 자동 생성하지 않고 Database 담당자와 변경 내용을 확인한 뒤 진행합니다.

`src/main/resources/db/migration`의 Migration 파일은
로컬 개발 및 스키마 기준 확인 용도로 존재하며,
현재 원격 MariaDB 프로필에서는 Flyway가 자동 실행되지 않습니다.

원격 MariaDB의 예시 물품을 위 기준 데이터로 교체할 때는
DBeaver에서 다음 파일을 열어 내용을 확인한 뒤 한 번만 실행합니다.

```text
src/main/resources/db/manual/replace_demo_items.sql
```

기존 대여 이력이 교체 대상 물품을 참조하고 있으면 외래 키 제약으로 삭제가 실패합니다.
이 경우 대여 이력을 임의로 삭제하지 말고 Backend 담당자와 데이터 처리 방식을 먼저 결정합니다.

---

## 실제 서버 / 자동 배포

Backend `main` 브랜치에 변경사항이 반영되면
GitHub Actions를 통해 실제 Backend 서버에 자동 배포됩니다.

현재 배포 흐름:

```text
back/*
   ↓
back-dev
   ↓
main
   ↓
GitHub Actions
   ↓
Self-hosted Windows Runner
   ↓
Maven Build
   ↓
Spring Boot 실행
   ↓
Health Check
```

자동 배포 시:

- Java 21 환경을 사용합니다.
- Maven으로 Backend JAR을 빌드합니다.
- `mariadb` 프로필을 사용합니다.
- GitHub Secrets의 Database 환경 변수를 사용합니다.
- 실제 서버에서는 `0.0.0.0:8081`로 실행합니다.
- 배포 후 `/api/health`를 호출하여 정상 실행 여부를 확인합니다.

```text
로컬 개발 : 127.0.0.1:8080
실제 서버 : 0.0.0.0:8081
```

### 주의

`main`은 실제 서버 자동 배포와 연결되어 있습니다.

따라서 검토되지 않은 코드를 `main`에 직접 반영하지 않습니다.

자동 배포 설정 파일:

- [.github/workflows/deploy.yml](.github/workflows/deploy.yml)

---

# GitHub 작업 흐름

Backend는 다음 브랜치 흐름을 사용합니다.

```text
back/*
   ↓
back-dev
   ↓
main
```

### `main`

실제 서버 배포 기준이 되는 안정화 브랜치입니다.

- 직접 기능 개발하지 않습니다.
- 검토 및 통합이 완료된 `back-dev`만 반영합니다.
- 변경사항이 반영되면 자동 배포가 실행됩니다.

### `back-dev`

Backend 통합 브랜치입니다.

- Backend 기능을 통합합니다.
- Backend 작업 브랜치는 `back-dev`를 기준으로 생성합니다.
- API 및 Database 연동 상태를 확인합니다.

### `back/*`

Backend 기능 및 작업용 브랜치입니다.

예시:

```text
back/auth
back/items
back/rental
back/notification
back/docs-refactor
```

Commit, Pull Request, Code Review, Merge 등 공통 협업 규칙은
Frontend 저장소의 [CONTRIBUTING.md](https://github.com/jinwook0308/cycirental/blob/main/CONTRIBUTING.md)를 기준으로 합니다.

팀원 역할 및 담당은
[TEAM.md](https://github.com/jinwook0308/cycirental/blob/main/TEAM.md)를 기준으로 합니다.

---

## 문서 관리

Backend 구조 또는 실행 방식이 변경된 경우 README도 함께 수정합니다.

- Backend 실행 방법 → `README.md`
- API 추가 / 변경 → `README.md`
- Database 연결 방식 → `README.md`
- 서버 및 자동 배포 방식 → `README.md`
- 환경 변수 예시 → `.env.example`
- 자동 배포 설정 → `.github/workflows/deploy.yml`
- 공통 Git / GitHub 협업 규칙 → Frontend 저장소 `CONTRIBUTING.md`
- 팀원 역할 및 담당 → Frontend 저장소 `TEAM.md`

같은 공통 규칙이나 역할 분담 내용을 여러 README에 중복해서 정의하지 않습니다.

---

## 보안 및 주의사항

- 실제 Database 비밀번호를 GitHub에 올리지 않습니다.
- 실제 `.env` 파일을 Commit하지 않습니다.
- GitHub Secrets의 값을 코드 또는 문서에 직접 작성하지 않습니다.
- Database 구조 변경은 관련 담당자와 공유한 뒤 진행합니다.
- `main` 변경은 실제 서버 자동 배포로 이어질 수 있으므로 반드시 검토 후 Merge합니다.

---

## 관련 문서

- [Frontend 저장소](https://github.com/jinwook0308/cycirental)
- [공통 협업 규칙](https://github.com/jinwook0308/cycirental/blob/main/CONTRIBUTING.md)
- [팀원 역할 및 담당](https://github.com/jinwook0308/cycirental/blob/main/TEAM.md)
- [.env.example](.env.example)
- [자동 배포 Workflow](.github/workflows/deploy.yml)

---

## 확인 기록

- 2026년 9월30일 김명숙 - 셋업확인 완료... 열심히 파이팅!!!

## 인증 스키마와 관리자 초기화

| 작업 | API | 요청 / 응답 |
| --- | --- | --- |
| 가입 | POST /api/auth/signup | displayName, studentNumber, phoneNumber, password → 201 |
| 로그인 | POST /api/auth/login | studentNumber, password → 사용자·토큰 → 200 |
| 로그아웃 | POST /api/auth/logout | Bearer 토큰 → 204 |
| 초기화 요청 | POST /api/auth/password-reset/requests | displayName, studentNumber → 202 |
| 관리자 대기 목록 | GET /api/admin/password-reset-requests | Bearer 토큰 → 오래된 순 최대 100건 |
| 관리자 초기화 | POST /api/admin/password-reset-requests/{id}/approve | Bearer + identityConfirmed:true → temporaryPassword, expiresAt |
| 관리자 거절 | POST /api/admin/password-reset-requests/{id}/reject | Bearer → 200 |
| 비밀번호 변경 | POST /api/auth/password/change | Bearer + currentPassword, newPassword → 새 사용자·토큰 → 200 |

회원가입은 이름 / 학번 / 전화번호 / 비밀번호 / 비밀번호 확인을 입력합니다. 요청에는 확인용 비밀번호를 보내지 않습니다.
이름은 2~30자, 일반 학번은 숫자 6~12자리, 비밀번호는 8~64자입니다. 전화번호는 국내 휴대전화번호를 숫자 10~11자리로 받습니다. 학번과 전화번호는 앞자리 0을 보존하는 문자열입니다.
학번을 로그인 아이디로 사용하며 별도 아이디·이메일 입력과 아이디 찾기는 제거했습니다. 새 계정은 학생 권한으로만 생성됩니다.
예약 관리자 학번 `001`은 로그인·초기화 요청에서만 예외로 허용합니다. 일반 회원가입으로 만들 수 없고, DB의 관리자 계정과 서버 인증 성공 응답이 필요합니다. 앱에 고정 로그인 우회는 없습니다.

초기화는 이름·학번으로 요청한 뒤 관리자가 학생증 등으로 본인을 확인해 처리합니다. 이름·학번·전화번호 입력만으로 본인 인증을 완료한 것으로 보지 않습니다.
계정이 없거나 정보가 달라도 요청 응답은 같은 202이며, 동일 계정의 대기 요청은 중복 생성하지 않습니다.
관리자 승인 시 서버가 무작위 임시 비밀번호를 만들고 해시만 저장합니다. 임시 비밀번호는 24시간 유효하며 관리자 화면에 한 번 표시하므로 본인에게 별도로 전달합니다. 문자 발송·전화번호 인증은 연결하지 않았습니다.
초기화 시 기존 세션을 폐기합니다. `mustChangePassword=true`로 로그인하면 홈 진입 전 새 비밀번호 설정을 완료해야 합니다. 서버도 변경 전 관리자 API 사용을 거부합니다.
변경 완료 시 기존 세션을 폐기하고 새 토큰을 발급합니다. 일반 로그인 토큰은 7일 유효합니다. 기존 토큰 방식의 POST /api/auth/password-reset은 410, /api/auth/find-id는 제거 상태입니다.

예약 관리자 초기 계정 `001 / admin`은 검토한 수동 SQL로만 등록합니다. 앱에 비밀번호를 하드코딩하거나 Flyway 실행 시 기본 계정을 생성하지 않습니다.
기존 001 계정이 있으면 덮어쓰기·비밀번호 변경·권한 승격 없이 별도 확인합니다.

로컬 H2는 Flyway V1~V8을 사용합니다. 기존 V1~V3을 수정하지 않고 V4~V8로 전진 변경했습니다.
원격 MariaDB는 `ddl-auto: none`, Flyway 비활성화 상태이므로 main 자동 배포만으로 DB가 바뀌지 않습니다.
학번·전화번호·초기화 구조의 수동 적용과 백업 절차는 [REMOTE_SCHEMA_ALIGNMENT.md](REMOTE_SCHEMA_ALIGNMENT.md)를 확인합니다.
로컬 코드 검증·Git 전 준비는 [RELEASE_PREPARATION.md](RELEASE_PREPARATION.md)를 확인합니다.

2026-11-03 로컬 검증 기록: 백엔드 테스트 22개 통과. 원격 적용과 배포 성공은 별도로 기록해야 합니다.

원격 반영 기록(2026-11-03): DB 백업과 학번·전화번호·초기화 스키마 적용·기존 행 수 보존 검증을 완료했습니다. 001 관리자 계정 생성과 관리자 권한 검증까지 완료했습니다. 새 백엔드 배포와 실폰 로그인 검증은 별도 단계입니다. 자세한 상태는 REMOTE_SCHEMA_ALIGNMENT.md를 확인합니다.
