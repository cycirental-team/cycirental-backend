package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.dto.LoginRequest;
import kr.ac.cyci.deptrental.auth.dto.SignUpRequest;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

// 관리자 승인이 동시에 들어와도 재고가 정확한지 보는 테스트
// @Transactional을 붙이지 않음: 두 승인이 각자 따로 저장돼야 동시 상황을 만들 수 있음
// 기존 물품 5개의 수량은 다른 테스트(DeptRentalApiApplicationTests)가 확인하므로, 이 테스트 전용 물품을 만들고 끝나면 지움
@SpringBootTest
class AdminRentalConcurrencyTest {

    @Autowired AdminRentalService adminRentalService;
    @Autowired RentalService rentalService;
    @Autowired AuthService authService;
    @Autowired JdbcTemplate jdbc;

    private String login(String studentNumber, boolean admin) {
        authService.signUp(new SignUpRequest("password123", "동시 승인 테스트", studentNumber, "01012345678"));
        if (admin) {
            jdbc.update("UPDATE users SET is_admin = TRUE WHERE student_number = ?", studentNumber);
        }
        return authService.login(new LoginRequest(studentNumber, "password123")).accessToken();
    }

    private Long createItem(String name, int quantity) {
        jdbc.update("INSERT INTO items (name, category, description, total_quantity, available_quantity) VALUES (?, ?, ?, ?, ?)",
                name, "테스트", "동시 승인 테스트 전용", quantity, quantity);
        return jdbc.queryForObject("SELECT MAX(id) FROM items WHERE name = ?", Long.class, name);
    }

    private void removeItem(Long itemId) {
        jdbc.update("DELETE FROM rentals WHERE item_id = ?", itemId);
        jdbc.update("DELETE FROM items WHERE id = ?", itemId);
    }

    private int availableQuantity(Long itemId) {
        return jdbc.queryForObject("SELECT available_quantity FROM items WHERE id = ?", Integer.class, itemId);
    }

    @Test
    @DisplayName("재고 1개에 서로 다른 신청 두 건을 동시에 승인하면 한 건만 승인되고 재고는 0")
    void lastItemApprovedOnce() throws Exception {
        String admin = login("20269701", true);
        String studentA = login("20269702", false);
        String studentB = login("20269703", false);
        Long itemId = createItem("동시 승인 테스트 물품 A", 1);
        try {
            Long rentalA = rentalService.apply(studentA, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();
            Long rentalB = rentalService.apply(studentB, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();

            List<Integer> results = runTogether(
                    () -> adminRentalService.approve(admin, rentalA),
                    () -> adminRentalService.approve(admin, rentalB));

            assertTrue(results.contains(200) && results.contains(409), "한 건만 승인돼야 해요: " + results);
            assertEquals(0, availableQuantity(itemId));
        } finally {
            removeItem(itemId);
        }
    }

    @Test
    @DisplayName("같은 신청을 동시에 두 번 승인하면 한 번만 처리되고 재고는 1만 줄어든다")
    void sameRentalApprovedOnce() throws Exception {
        String admin = login("20269704", true);
        String student = login("20269705", false);
        Long itemId = createItem("동시 승인 테스트 물품 B", 3);
        try {
            Long rentalId = rentalService.apply(student, new RentalRequest(itemId, LocalDate.now().plusDays(3))).id();

            List<Integer> results = runTogether(
                    () -> adminRentalService.approve(admin, rentalId),
                    () -> adminRentalService.approve(admin, rentalId));

            assertTrue(results.contains(200) && results.contains(409), "한 번만 승인돼야 해요: " + results);
            assertEquals(2, availableQuantity(itemId));
        } finally {
            removeItem(itemId);
        }
    }

    // 두 작업을 동시에 출발시키고 각각의 결과 코드를 돌려줌 (성공이면 200)
    private List<Integer> runTogether(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> statusOf(first, start));
            var b = pool.submit(() -> statusOf(second, start));
            start.countDown(); // 출발!
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
        }
    }

    private static int statusOf(Callable<?> action, CountDownLatch start) throws Exception {
        start.await();
        try {
            action.call();
            return 200;
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
    }
}