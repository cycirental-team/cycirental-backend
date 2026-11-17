package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.dto.AuthResponse;
import kr.ac.cyci.deptrental.auth.dto.LoginRequest;
import kr.ac.cyci.deptrental.auth.dto.SignUpRequest;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

// 트랜잭션이 실제로 나뉘어 저장되는지 보는 테스트
// @Transactional을 붙이지 않음: 붙이면 전부 한 트랜잭션이 돼서 롤백·동시 처리 문제를 확인할 수 없음
// (그래서 테스트마다 서로 다른 학번을 씀)
@SpringBootTest
class RentalTransactionTest {

    @Autowired RentalService rentalService;
    @Autowired RentalRepository rentalRepository;
    @Autowired AuthService authService;
    @Autowired ItemRepository itemRepository;

    private AuthResponse signUpAndLogin(String studentNumber) {
        authService.signUp(new SignUpRequest("password123", "트랜잭션 테스트", studentNumber, "01012345678"));
        return authService.login(new LoginRequest(studentNumber, "password123"));
    }

    private Long availableItemId() {
        return itemRepository.search(null, null, true).get(0).getId();
    }

    private long pendingCount(Long userId, Long itemId) {
        return rentalRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(rental -> rental.getItemId().equals(itemId) && rental.getStatus() == RentalStatus.PENDING)
                .count();
    }

    @Test
    @DisplayName("같은 학생이 같은 물품을 동시에 두 번 신청하면 201 한 건, 409 한 건, DB에는 승인 대기 한 건")
    void sameStudentDoubleSubmit() throws Exception {
        AuthResponse login = signUpAndLogin("20269201");
        Long itemId = availableItemId();
        RentalRequest request = new RentalRequest(itemId, LocalDate.now().plusDays(3));

        List<Integer> results = submitTwiceAtOnce(rentalService, login.accessToken(), request);

        assertTrue(results.contains(201), "한 건은 저장돼야 해요: " + results);
        assertTrue(results.contains(409), "다른 한 건은 중복으로 막혀야 해요: " + results);
        assertEquals(1, pendingCount(login.userId(), itemId));
    }

    @Test
    @DisplayName("기한 지난 승인 대기를 취소하면 409, DB에는 만료(EXPIRED)로 남는다")
    void cancelOverduePendingKeepsExpired() {
        AuthResponse login = signUpAndLogin("20269202");
        LocalDate today = LocalDate.now();
        Rental overdue = rentalRepository.save(new Rental(login.userId(), availableItemId(), today.minusDays(3), today)); // 반납 예정일이 오늘인 승인 대기

        var error = assertThrows(ResponseStatusException.class,
                () -> rentalService.cancel(login.accessToken(), overdue.getId()));

        assertEquals(409, error.getStatusCode().value());
        assertEquals(RentalStatus.EXPIRED, rentalRepository.findById(overdue.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("기한 지난 승인 대기는 만료되고, 같은 물품을 다시 신청할 수 있다")
    void reapplyAfterExpiry() {
        AuthResponse login = signUpAndLogin("20269203");
        Long itemId = availableItemId();
        LocalDate today = LocalDate.now();
        Rental overdue = rentalRepository.save(new Rental(login.userId(), itemId, today.minusDays(3), today));

        var again = rentalService.apply(login.accessToken(), new RentalRequest(itemId, today.plusDays(3)));

        assertEquals(RentalStatus.PENDING, again.status());
        assertEquals(RentalStatus.EXPIRED, rentalRepository.findById(overdue.getId()).orElseThrow().getStatus());
        assertEquals(1, pendingCount(login.userId(), itemId));
    }

    // 버튼을 두 번 빠르게 누른 것처럼 같은 신청을 동시에 두 번 보내고, 각각의 응답 코드를 돌려줌
    // (RentalRepeatableReadTest에서도 같이 씀)
    static List<Integer> submitTwiceAtOnce(RentalService rentalService, String token, RentalRequest request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> applyOnce = () -> {
            start.await(); // 두 요청이 같이 출발하도록 대기
            try {
                rentalService.apply(token, request);
                return 201;
            } catch (ResponseStatusException e) {
                return e.getStatusCode().value();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(applyOnce);
            var second = pool.submit(applyOnce);
            start.countDown(); // 출발!
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
        }
    }
}