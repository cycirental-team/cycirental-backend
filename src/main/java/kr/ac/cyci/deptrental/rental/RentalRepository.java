package kr.ac.cyci.deptrental.rental;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 내 대여 목록 (최신 신청이 위로)
    List<Rental> findByUserIdOrderByIdDesc(Long userId);

    // 같은 학생이 같은 물품을 이미 그 상태로 갖고 있는지 (중복 신청 막기용)
    boolean existsByUserIdAndItemIdAndStatus(Long userId, Long itemId, RentalStatus status);
}