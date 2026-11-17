package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.dto.LoginRequest;
import kr.ac.cyci.deptrental.auth.dto.SignUpRequest;
import kr.ac.cyci.deptrental.item.Item;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import kr.ac.cyci.deptrental.rental.dto.RentalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional // 테스트가 끝나면 저장한 데이터를 자동으로 되돌림
class RentalServiceTest {

    @Autowired RentalService rentalService;
    @Autowired AuthService authService;
    @Autowired ItemRepository itemRepository;

    // 테스트용 학생을 가입시키고 로그인해서 토큰을 받음
    private String loginAs(String studentNumber) {
        authService.signUp(new SignUpRequest("password123", "대여 테스트", studentNumber, "01012345678"));
        return authService.login(new LoginRequest(studentNumber, "password123")).accessToken();
    }

    // 재고가 있는 물품 하나
    private Item availableItem() {
        return itemRepository.search(null, null, true).get(0);
    }

    @Test
    @DisplayName("대여 신청하면 승인 대기로 저장되고 내 목록에 보인다")
    void applyAndMyList() {
        String token = loginAs("20269001");
        Item item = availableItem();

        RentalResponse saved = rentalService.apply(token, new RentalRequest(item.getId(), LocalDate.now().plusDays(7)));

        assertEquals(RentalStatus.PENDING, saved.status());
        assertEquals(item.getName(), saved.itemName());
        assertEquals(1, rentalService.myRentals(token).size());
    }

    @Test
    @DisplayName("같은 물품을 승인 대기 중에 또 신청하면 막힌다")
    void duplicatePendingIsBlocked() {
        String token = loginAs("20269002");
        Long itemId = availableItem().getId();
        rentalService.apply(token, new RentalRequest(itemId, LocalDate.now().plusDays(3)));

        var error = assertThrows(ResponseStatusException.class,
                () -> rentalService.apply(token, new RentalRequest(itemId, LocalDate.now().plusDays(3))));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    @DisplayName("반납일은 내일부터 14일 이내만 된다")
    void dueDateRule() {
        String token = loginAs("20269003");
        Long itemId = availableItem().getId();

        var today = assertThrows(ResponseStatusException.class,
                () -> rentalService.apply(token, new RentalRequest(itemId, LocalDate.now())));
        var tooLate = assertThrows(ResponseStatusException.class,
                () -> rentalService.apply(token, new RentalRequest(itemId, LocalDate.now().plusDays(15))));
        assertEquals(400, today.getStatusCode().value());
        assertEquals(400, tooLate.getStatusCode().value());
    }

    @Test
    @DisplayName("로그인 안 하면 신청할 수 없다")
    void loginRequired() {
        var error = assertThrows(ResponseStatusException.class,
                () -> rentalService.apply(null, new RentalRequest(availableItem().getId(), LocalDate.now().plusDays(1))));
        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    @DisplayName("내 승인 대기 신청은 취소되고, 남의 신청이나 이미 취소한 건 안 된다")
    void cancelRules() {
        String mine = loginAs("20269004");
        String other = loginAs("20269005");
        Long rentalId = rentalService.apply(mine, new RentalRequest(availableItem().getId(), LocalDate.now().plusDays(2))).id();

        var notMine = assertThrows(ResponseStatusException.class, () -> rentalService.cancel(other, rentalId));
        assertEquals(403, notMine.getStatusCode().value());

        assertEquals(RentalStatus.CANCELLED, rentalService.cancel(mine, rentalId).status());

        var again = assertThrows(ResponseStatusException.class, () -> rentalService.cancel(mine, rentalId));
        assertEquals(409, again.getStatusCode().value());
    }
}