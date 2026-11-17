package kr.ac.cyci.deptrental.rental;

import java.time.LocalDate;
import java.util.List;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.User;
import kr.ac.cyci.deptrental.item.Item;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import kr.ac.cyci.deptrental.rental.dto.RentalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RentalService {
    private static final int MAX_RENTAL_DAYS = 14; // 앱과 같은 규칙: 최대 14일

    private final RentalRepository rentals;
    private final ItemRepository items;
    private final AuthService auth;

    public RentalService(RentalRepository rentals, ItemRepository items, AuthService auth) {
        this.rentals = rentals;
        this.items = items;
        this.auth = auth;
    }

    // 대여 신청
    public RentalResponse apply(String token, RentalRequest request) {
        User user = auth.authenticate(token); // 1. 로그인한 사람인지

        LocalDate today = LocalDate.now();
        LocalDate dueAt = request.dueAt();
        if (!dueAt.isAfter(today) || dueAt.isAfter(today.plusDays(MAX_RENTAL_DAYS))) { // 2. 날짜: 내일 ~ 14일 뒤
            throw error(HttpStatus.BAD_REQUEST, "반납 예정일은 내일부터 " + MAX_RENTAL_DAYS + "일 이내로 선택해 주세요.");
        }

        Item item = items.findById(request.itemId()) // 3. 있는 물품인지
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "물품을 찾을 수 없습니다."));

        if (item.getAvailableQuantity() <= 0) { // 4. 재고 있는지 (재고는 관리자가 승인할 때 줄어듦)
            throw error(HttpStatus.CONFLICT, "지금은 대여 가능한 수량이 없습니다.");
        }

        if (rentals.existsByUserIdAndItemIdAndStatus(user.getId(), item.getId(), RentalStatus.PENDING)) { // 5. 중복 신청 막기
            throw error(HttpStatus.CONFLICT, "이미 승인 대기 중인 신청이 있습니다.");
        }

        Rental saved = rentals.save(new Rental(user.getId(), item.getId(), today, dueAt)); // 6. 승인 대기로 저장
        return RentalResponse.of(saved, item.getName());
    }

    // 내 대여 목록 (최신 신청이 위로)
    @Transactional(readOnly = true)
    public List<RentalResponse> myRentals(String token) {
        User user = auth.authenticate(token);
        return rentals.findByUserIdOrderByIdDesc(user.getId()).stream()
                .map(rental -> RentalResponse.of(rental, itemName(rental.getItemId())))
                .toList();
    }

    // 신청 취소: 내 신청 + 승인 대기일 때만
    public RentalResponse cancel(String token, Long rentalId) {
        User user = auth.authenticate(token);
        Rental rental = rentals.findById(rentalId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "대여 신청을 찾을 수 없습니다."));

        if (!rental.getUserId().equals(user.getId())) {
            throw error(HttpStatus.FORBIDDEN, "본인 신청만 취소할 수 있습니다.");
        }
        if (rental.getStatus() != RentalStatus.PENDING) {
            throw error(HttpStatus.CONFLICT, "승인 대기 중인 신청만 취소할 수 있습니다.");
        }

        rental.cancel();
        return RentalResponse.of(rental, itemName(rental.getItemId()));
    }

    private String itemName(Long itemId) {
        return items.findById(itemId).map(Item::getName).orElse("삭제된 물품");
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}