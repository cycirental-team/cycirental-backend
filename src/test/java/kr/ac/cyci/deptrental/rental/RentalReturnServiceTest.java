package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.dto.AuthResponse;
import kr.ac.cyci.deptrental.auth.dto.LoginRequest;
import kr.ac.cyci.deptrental.auth.dto.SignUpRequest;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.AdminRentalResponse;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import kr.ac.cyci.deptrental.rental.dto.RentalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 반납 흐름 테스트: 학생 반납 요청 → 관리자 반납 완료
@SpringBootTest
@Transactional // 테스트가 끝나면 저장한 데이터를 자동으로 되돌림
class RentalReturnServiceTest {

    @Autowired RentalService rentalService;
    @Autowired AdminRentalService adminRentalService;
    @Autowired AuthService authService;
    @Autowired ItemRepository itemRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    // 계정을 만들고 로그인 (admin이 true면 관리자로 바꿔서 로그인)
    private AuthResponse account(String studentNumber, String name, boolean admin) {
        authService.signUp(new SignUpRequest("password123", name, studentNumber, "01012345678"));
        em.flush();
        if (admin) {
            jdbc.update("UPDATE users SET is_admin = TRUE WHERE student_number = ?", studentNumber);
            em.clear();
        }
        return authService.login(new LoginRequest(studentNumber, "password123"));
    }

    private Long availableItemId() {
        return itemRepository.search(null, null, true).get(0).getId();
    }

    // 지금 DB에 저장된 대여 가능 수량 (JPA 캐시가 아니라 DB에서 직접 읽음)
    private int availableQuantity(Long itemId) {
        return jdbc.queryForObject("SELECT available_quantity FROM items WHERE id = ?", Integer.class, itemId);
    }

    // 학생이 신청하고 관리자가 승인해서 "대여 중"인 대여를 하나 만듦
    private Long rent(String student, String admin, Long itemId) {
        Long rentalId = rentalService.apply(student, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();
        adminRentalService.approve(admin, rentalId);
        return rentalId;
    }

    @Test
    @DisplayName("대여 중인 물품을 반납 요청하면 반납 요청 상태가 되고, 재고는 아직 그대로다")
    void requestReturnChangesStatus() {
        String admin = account("20269801", "관리자 테스트", true).accessToken();
        String student = account("20269802", "대여 학생", false).accessToken();
        Long itemId = availableItemId();
        Long rentalId = rent(student, admin, itemId);
        int stockWhileRented = availableQuantity(itemId);

        RentalResponse requested = rentalService.requestReturn(student, rentalId);

        assertEquals(RentalStatus.RETURN_REQUESTED, requested.status());
        assertEquals(stockWhileRented, availableQuantity(itemId)); // 관리자가 확인하기 전에는 재고가 늘지 않음
        assertEquals(RentalStatus.RETURN_REQUESTED, rentalService.myRentals(student).get(0).status());
    }

    @Test
    @DisplayName("승인 대기 신청이나 이미 반납 요청한 대여는 반납 요청할 수 없다 (409)")
    void onlyRentedCanRequestReturn() {
        String admin = account("20269803", "관리자 테스트", true).accessToken();
        String student = account("20269804", "대여 학생", false).accessToken();
        Long rentalId = rentalService.apply(student, new RentalRequest(availableItemId(), LocalDate.now().plusDays(3))).id();

        var notRentedYet = assertThrows(ResponseStatusException.class, () -> rentalService.requestReturn(student, rentalId));
        assertEquals(409, notRentedYet.getStatusCode().value());

        adminRentalService.approve(admin, rentalId);
        rentalService.requestReturn(student, rentalId);

        var twice = assertThrows(ResponseStatusException.class, () -> rentalService.requestReturn(student, rentalId));
        assertEquals(409, twice.getStatusCode().value());
    }

    @Test
    @DisplayName("남의 대여는 반납 요청할 수 없다 (403)")
    void onlyOwnerCanRequestReturn() {
        String admin = account("20269805", "관리자 테스트", true).accessToken();
        String owner = account("20269806", "대여 학생", false).accessToken();
        String other = account("20269807", "다른 학생", false).accessToken();
        Long rentalId = rent(owner, admin, availableItemId());

        var error = assertThrows(ResponseStatusException.class, () -> rentalService.requestReturn(other, rentalId));

        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    @DisplayName("반납 완료하면 반납 완료 상태가 되고, 반납일이 기록되고, 재고가 대여 전으로 돌아온다")
    void completeReturnRestoresStock() {
        String admin = account("20269811", "관리자 테스트", true).accessToken();
        String student = account("20269812", "대여 학생", false).accessToken();
        Long itemId = availableItemId();
        int stockBeforeRent = availableQuantity(itemId);
        Long rentalId = rent(student, admin, itemId);
        rentalService.requestReturn(student, rentalId);

        AdminRentalResponse completed = adminRentalService.completeReturn(admin, rentalId);

        assertEquals(RentalStatus.RETURNED, completed.status());
        assertEquals(LocalDate.now(), completed.returnedAt());
        assertEquals(stockBeforeRent, availableQuantity(itemId)); // 승인 때 -1, 반납 완료 때 +1
        RentalResponse mine = rentalService.myRentals(student).get(0);
        assertEquals(RentalStatus.RETURNED, mine.status());
        assertEquals(LocalDate.now(), mine.returnedAt());
    }

    @Test
    @DisplayName("반납 요청이 없는 대여나 이미 반납 완료한 대여는 반납 완료할 수 없다 (409), 재고는 한 번만 늘어난다")
    void onlyReturnRequestedCanComplete() {
        String admin = account("20269813", "관리자 테스트", true).accessToken();
        String student = account("20269814", "대여 학생", false).accessToken();
        Long itemId = availableItemId();
        int stockBeforeRent = availableQuantity(itemId);
        Long rentalId = rent(student, admin, itemId);

        var noRequest = assertThrows(ResponseStatusException.class, () -> adminRentalService.completeReturn(admin, rentalId));
        assertEquals(409, noRequest.getStatusCode().value());

        rentalService.requestReturn(student, rentalId);
        adminRentalService.completeReturn(admin, rentalId);

        var twice = assertThrows(ResponseStatusException.class, () -> adminRentalService.completeReturn(admin, rentalId));
        assertEquals(409, twice.getStatusCode().value());
        assertEquals(stockBeforeRent, availableQuantity(itemId));
    }

    @Test
    @DisplayName("학생은 반납 완료할 수 없다 (403)")
    void studentCannotCompleteReturn() {
        String admin = account("20269815", "관리자 테스트", true).accessToken();
        String student = account("20269816", "대여 학생", false).accessToken();
        Long rentalId = rent(student, admin, availableItemId());
        rentalService.requestReturn(student, rentalId);

        var error = assertThrows(ResponseStatusException.class, () -> adminRentalService.completeReturn(student, rentalId));

        assertEquals(403, error.getStatusCode().value());
    }
}