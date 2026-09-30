# CYCIRENTAL Backend

학과 대여 물품 관리 앱 CYCIRENTAL의 Spring Boot 백엔드 저장소입니다.

- Frontend: https://github.com/jinwook0308/cycirental
- Java: 21
- Framework: Spring Boot
- Database: H2(기본 개발용), MariaDB(원격 프로필)

## 실행

```powershell
.\mvnw.cmd spring-boot:run
```

기본 설정은 H2 인메모리 데이터베이스를 사용하며 `127.0.0.1:8080`에서 실행됩니다.

확인 가능한 API:

- `GET http://127.0.0.1:8080/api/health`
- `GET http://127.0.0.1:8080/api/items`
- `GET http://127.0.0.1:8080/api/items?query=VR`

## 테스트

```powershell
.\mvnw.cmd test
```

## MariaDB 연결

필요한 환경 변수 이름은 `.env.example`에서 확인합니다. 실제 DB 주소, 계정 및 비밀번호는 팀 관리자에게 개인적으로 받고 Git에 올리지 않습니다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "mariadb"
$env:DB_HOST = "팀에서 받은 호스트"
$env:DB_PORT = "3306"
$env:DB_NAME = "팀에서 받은 DB 이름"
$env:DB_USER = "팀에서 받은 사용자"
$env:DB_PASSWORD = "팀에서 받은 비밀번호"
.\mvnw.cmd spring-boot:run
```

Spring Boot가 `.env` 파일을 자동으로 읽는 것은 아니므로 IntelliJ 실행 설정 또는 현재 셸의 환경 변수로 값을 전달합니다.

원격 DB 보호를 위해 MariaDB 프로필에서는 Flyway 자동 실행과 Hibernate 자동 DDL을 비활성화했습니다.
