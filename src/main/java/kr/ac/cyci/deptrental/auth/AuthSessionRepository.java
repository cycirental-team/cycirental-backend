package kr.ac.cyci.deptrental.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    java.util.Optional<AuthSession> findByTokenHash(String tokenHash);
    long deleteByUser(User user);
    long deleteByTokenHash(String tokenHash);
}
