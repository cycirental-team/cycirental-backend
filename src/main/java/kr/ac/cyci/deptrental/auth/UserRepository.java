package kr.ac.cyci.deptrental.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<User> findByStudentNumber(String studentNumber);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.studentNumber = :number")
    java.util.Optional<User> lockByStudentNumber(@org.springframework.data.repository.query.Param("number") String number);
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByStudentNumberAndDisplayName(String studentNumber, String displayName);
    boolean existsByLoginId(String loginId);
    boolean existsByStudentNumber(String studentNumber);
}
