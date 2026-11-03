package kr.ac.cyci.deptrental.auth;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface AccountRecoveryRepository extends JpaRepository<AccountRecoveryRequest,Long> {
    boolean existsByUserAndStatus(User user,String status);
    List<AccountRecoveryRequest> findTop100ByStatusOrderByRequestedAtAsc(String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AccountRecoveryRequest r where r.id = :id")
    Optional<AccountRecoveryRequest> lockById(@Param("id") Long id);
}
