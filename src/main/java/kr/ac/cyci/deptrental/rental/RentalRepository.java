package kr.ac.cyci.deptrental.rental;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 내 대여 목록 (최신 신청이 위로)
    List<Rental> findByUserIdOrderByIdDesc(Long userId);

    // 같은 학생이 같은 물품을 이미 그 상태로 갖고 있는지 (중복 신청 막기용)
    boolean existsByUserIdAndItemIdAndStatus(Long userId, Long itemId, RentalStatus status);

    // 이 학생의 신청 중 반납 예정일이 오늘이거나 지난 것을 from 상태 → to 상태로 한 번에 바꿈
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Rental r set r.status = :to where r.userId = :userId and r.status = :from and r.dueAt <= :today")
    int changeOverdueStatus(@Param("userId") Long userId, @Param("today") LocalDate today,
                            @Param("from") RentalStatus from, @Param("to") RentalStatus to);

    // 관리자 목록: 전체 / 상태별 (최근 신청이 위로)
    List<Rental> findAllByOrderByIdDesc();
    List<Rental> findByStatusOrderByIdDesc(RentalStatus status);

    // 모든 학생의 신청 중 반납 예정일이 오늘이거나 지난 것을 from 상태 → to 상태로 한 번에 바꿈 (관리자 화면용)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Rental r set r.status = :to where r.status = :from and r.dueAt <= :today")
    int changeAllOverdueStatus(@Param("today") LocalDate today,
                               @Param("from") RentalStatus from, @Param("to") RentalStatus to);
}