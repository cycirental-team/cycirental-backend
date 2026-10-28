package kr.ac.cyci.deptrental.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    long deleteByTokenHash(String tokenHash);
}
