package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
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

// H2는 원래 READ_COMMITTED라서, 신청 트랜잭션의 READ_COMMITTED 설정이 실제로 먹는지 구분이 안 됨
// 그래서 DB 기본값을 MariaDB와 같은 REPEATABLE READ로 바꿔 놓고 동시 신청을 다시 확인함
// (설정이 안 먹으면 뒤 요청이 앞 요청의 저장을 못 봐서 중복 저장이 생김)
@SpringBootTest(properties = "spring.datasource.hikari.transaction-isolation=TRANSACTION_REPEATABLE_READ")
class RentalRepeatableReadTest {

    @Autowired RentalService rentalService;
    @Autowired RentalRepository rentalRepository;
    @Autowired AuthService authService;
    @Autowired ItemRepository itemRepository;

    @Test
    @DisplayName("DB 기본값이 REPEATABLE READ여도 동시 신청은 한 건만 저장된다")
    void doubleSubmitWithMariaDbDefault() throws Exception {
        authService.signUp(new SignUpRequest("password123", "격리 수준 테스트", "20269301", "01012345678"));
        AuthResponse login = authService.login(new LoginRequest("20269301", "password123"));
        Long itemId = itemRepository.search(null, null, true).get(0).getId();
        RentalRequest request = new RentalRequest(itemId, LocalDate.now().plusDays(3));

        List<Integer> results = RentalTransactionTest.submitTwiceAtOnce(rentalService, login.accessToken(), request);

        assertTrue(results.contains(201), "한 건은 저장돼야 해요: " + results);
        assertTrue(results.contains(409), "다른 한 건은 중복으로 막혀야 해요: " + results);
        long pending = rentalRepository.findByUserIdOrderByIdDesc(login.userId()).stream()
                .filter(rental -> rental.getItemId().equals(itemId) && rental.getStatus() == RentalStatus.PENDING)
                .count();
        assertEquals(1, pending);
    }
}