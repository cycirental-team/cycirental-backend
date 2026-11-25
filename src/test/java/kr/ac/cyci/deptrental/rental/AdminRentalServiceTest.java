package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.dto.AuthResponse;
import kr.ac.cyci.deptrental.auth.dto.LoginRequest;
import kr.ac.cyci.deptrental.auth.dto.SignUpRequest;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.AdminRentalResponse;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional // 테스트가 끝나면 저장한 데이터를 자동으로 되돌림
class AdminRentalServiceTest {

    @Autowired AdminRentalService adminRentalService;
    @Autowired RentalService rentalService;
    @Autowired RentalRepository rentalRepository;
    @Autowired AuthService authService;
    @Autowired ItemRepository itemRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    // 계정을 만들고 로그인 (admin이 true면 관리자로 바꿔서 로그인) - AccountRecoveryIntegrationTest와 같은 방식
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


    // 목록에서 특정 대여 번호 찾기 (다른 테스트가 만든 데이터도 섞여 있을 수 있어서 번호로 찾음)
    private AdminRentalResponse findIn(List<AdminRentalResponse> list, Long rentalId) {
        return list.stream().filter(row -> row.id().equals(rentalId)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("관리자 목록에 학생 이름·학번과 물품 이름이 보인다")
    void adminSeesStudentNameAndNumber() {
        String admin = account("20269501", "관리자 테스트", true).accessToken();
        String student = account("20269502", "대여 학생", false).accessToken();
        Long itemId = availableItemId();
        Long rentalId = rentalService.apply(student, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();

        AdminRentalResponse row = findIn(adminRentalService.list(admin, RentalStatus.PENDING), rentalId);

        assertEquals("대여 학생", row.studentName());
        assertEquals("20269502", row.studentNumber());
        assertEquals(itemRepository.findById(itemId).orElseThrow().getName(), row.itemName());
        assertEquals(RentalStatus.PENDING, row.status());
    }

    @Test
    @DisplayName("status를 주면 그 상태만 나온다")
    void filterByStatus() {
        String admin = account("20269503", "관리자 테스트", true).accessToken();
        String student = account("20269507", "대여 학생", false).accessToken();
        Long rentalId = rentalService.apply(student, new RentalRequest(availableItemId(), LocalDate.now().plusDays(2))).id();
        rentalService.cancel(student, rentalId); // 취소된 신청을 하나 만들어 둠

        List<AdminRentalResponse> cancelled = adminRentalService.list(admin, RentalStatus.CANCELLED);

        assertTrue(cancelled.stream().allMatch(row -> row.status() == RentalStatus.CANCELLED));
        assertEquals(RentalStatus.CANCELLED, findIn(cancelled, rentalId).status());
    }

    @Test
    @DisplayName("기한 지난 승인 대기는 관리자 목록에서 만료로 보인다")
    void overduePendingShowsExpired() {
        String admin = account("20269504", "관리자 테스트", true).accessToken();
        AuthResponse student = account("20269505", "대여 학생", false);
        LocalDate today = LocalDate.now();
        Long rentalId = rentalRepository.save(new Rental(student.userId(), availableItemId(), today.minusDays(3), today)).getId();

        AdminRentalResponse row = findIn(adminRentalService.list(admin, null), rentalId);

        assertEquals(RentalStatus.EXPIRED, row.status());
    }

    @Test
    @DisplayName("학생 토큰이면 403, 토큰이 없으면 401")
    void onlyAdminCanSeeList() {
        String student = account("20269506", "대여 학생", false).accessToken();

        var forbidden = assertThrows(ResponseStatusException.class, () -> adminRentalService.list(student, null));
        var noLogin = assertThrows(ResponseStatusException.class, () -> adminRentalService.list(null, null));

        assertEquals(403, forbidden.getStatusCode().value());
        assertEquals(401, noLogin.getStatusCode().value());
    }
    @Test
    @DisplayName("승인하면 대여 중이 되고 대여 가능 수량이 1 줄어든다")
    void approveDecreasesStock() {
        String admin = account("20269511", "관리자 테스트", true).accessToken();
        String student = account("20269512", "대여 학생", false).accessToken();
        Long itemId = availableItemId();
        Long rentalId = rentalService.apply(student, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();
        int before = availableQuantity(itemId);

        AdminRentalResponse approved = adminRentalService.approve(admin, rentalId);

        assertEquals(RentalStatus.RENTED, approved.status());
        assertEquals(before - 1, availableQuantity(itemId));
    }

    @Test
    @DisplayName("이미 승인한 신청을 또 승인하면 409, 재고는 한 번만 줄어든다")
    void approveOnlyPending() {
        String admin = account("20269513", "관리자 테스트", true).accessToken();
        String student = account("20269514", "대여 학생", false).accessToken();
        Long itemId = availableItemId();
        Long rentalId = rentalService.apply(student, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();
        int before = availableQuantity(itemId);
        adminRentalService.approve(admin, rentalId);

        var again = assertThrows(ResponseStatusException.class, () -> adminRentalService.approve(admin, rentalId));

        assertEquals(409, again.getStatusCode().value());
        assertEquals(before - 1, availableQuantity(itemId));
    }

    @Test
    @DisplayName("재고가 0이면 409, 신청은 승인 대기 그대로")
    void approveWithoutStock() {
        String admin = account("20269515", "관리자 테스트", true).accessToken();
        String student = account("20269516", "대여 학생", false).accessToken();
        Long itemId = availableItemId();
        Long rentalId = rentalService.apply(student, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();
        jdbc.update("UPDATE items SET available_quantity = 0 WHERE id = ?", itemId); // 신청 후 재고가 다 나간 상황

        var error = assertThrows(ResponseStatusException.class, () -> adminRentalService.approve(admin, rentalId));

        assertEquals(409, error.getStatusCode().value());
        assertEquals(0, availableQuantity(itemId));
        assertEquals(RentalStatus.PENDING, rentalRepository.findById(rentalId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("기한 지난(만료된) 신청은 승인할 수 없다")
    void approveExpired() {
        String admin = account("20269517", "관리자 테스트", true).accessToken();
        AuthResponse student = account("20269518", "대여 학생", false);
        LocalDate today = LocalDate.now();
        Long rentalId = rentalRepository.save(new Rental(student.userId(), availableItemId(), today.minusDays(3), today)).getId();

        var error = assertThrows(ResponseStatusException.class, () -> adminRentalService.approve(admin, rentalId));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    @DisplayName("학생은 승인할 수 없다 (403)")
    void studentCannotApprove() {
        String student = account("20269519", "대여 학생", false).accessToken();
        Long rentalId = rentalService.apply(student, new RentalRequest(availableItemId(), LocalDate.now().plusDays(3))).id();

        var error = assertThrows(ResponseStatusException.class, () -> adminRentalService.approve(student, rentalId));

        assertEquals(403, error.getStatusCode().value());
    }
}
