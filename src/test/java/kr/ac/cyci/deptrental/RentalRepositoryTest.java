package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import kr.ac.cyci.deptrental.auth.User;
import kr.ac.cyci.deptrental.auth.UserRepository;
import kr.ac.cyci.deptrental.item.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional // 테스트가 끝나면 저장한 데이터를 자동으로 되돌림
class RentalRepositoryTest {

    @Autowired RentalRepository rentals;
    @Autowired UserRepository users;
    @Autowired ItemRepository items;

    @Test
    @DisplayName("대여 신청을 저장하면 승인 대기 상태로 저장되고 내 목록에서 보인다")
    void saveAndFindRental() {
        // 준비: 테스트용 학생 1명, 물품 1개
        User student = users.save(new User("20260001", "hash", "대여 테스트", "20260001", "01012345678"));
        Long itemId = items.findAll().get(0).getId();

        // 실행: 오늘 신청, 7일 뒤 반납 예정
        Rental saved = rentals.save(new Rental(student.getId(), itemId, LocalDate.now(), LocalDate.now().plusDays(7)));

        // 확인
        assertNotNull(saved.getId());
        assertEquals(RentalStatus.PENDING, saved.getStatus());
        assertEquals(1, rentals.findByUserIdOrderByIdDesc(student.getId()).size());
        assertTrue(rentals.existsByUserIdAndItemIdAndStatus(student.getId(), itemId, RentalStatus.PENDING));
        assertFalse(rentals.existsByUserIdAndItemIdAndStatus(student.getId(), itemId, RentalStatus.RENTED));
    }
}