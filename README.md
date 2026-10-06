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
│  │  │     └─ item
│  │  │        ├─ Item.java
│  │  │        ├─ ItemController.java
│  │  │        └─ ItemRepository.java
│  │  │
│  │  └─ resources
│  │     ├─ application.yml
│  │     ├─ application-mariadb.yml
│  │     └─ db/migration
│  │        └─ V1__initial_schema.sql
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
| GET | `/api/items?query=검색어` | 물품 이름 검색 |
| GET | `/api/items/{id}` | 물품 상세 조회 |

예시:

```text
GET http://127.0.0.1:8080/api/health
GET http://127.0.0.1:8080/api/items
GET http://127.0.0.1:8080/api/items?query=VR
GET http://127.0.0.1:8080/api/items/1
```

현재 구현된 API만 이 문서에 작성하며,
새 API가 추가되면 실제 코드와 함께 README도 수정합니다.

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
