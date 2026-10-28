package kr.ac.cyci.deptrental.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByEmailAndDisplayName(String email, String displayName);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
}
