package kr.ac.cyci.deptrental.rental;

import java.time.LocalDate;
import java.util.List;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.User;
import kr.ac.cyci.deptrental.auth.UserRepository;
import kr.ac.cyci.deptrental.item.Item;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import kr.ac.cyci.deptrental.rental.dto.RentalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

// 클래스 전체에 @Transactional을 붙이지 않고, 단계마다 트랜잭션을 나눠서 씀
//   ① 로그인 확인 → ② 만료 처리(바로 저장) → ③ 신청·취소·목록
// ③에서 오류(403·409)가 나도 ②에서 저장한 만료는 취소되지 않음
@Service
public class RentalService {
    private static final int MAX_RENTAL_DAYS = 14; // 앱과 같은 규칙: 최대 14일

    private final RentalRepository rentals;
    private final ItemRepository items;
    private final UserRepository users;
    private final AuthService auth;

    private final TransactionTemplate writeTx; // 저장용 (만료 처리, 취소)
    private final TransactionTemplate applyTx; // 신청용 (READ_COMMITTED)
    private final TransactionTemplate readTx;  // 조회용 (readOnly)

    public RentalService(RentalRepository rentals, ItemRepository items, UserRepository users,
                         AuthService auth, PlatformTransactionManager transactionManager) {
        this.rentals = rentals;
        this.items = items;
        this.users = users;
        this.auth = auth;

        this.writeTx = new TransactionTemplate(transactionManager);

        // 앞 신청이 끝나길 기다린 뒤, 그 신청이 저장된 걸 바로 볼 수 있게 함
        // (MariaDB 기본값은 REPEATABLE READ라서 신청 트랜잭션에만 따로 지정)
        this.applyTx = new TransactionTemplate(transactionManager);
        this.applyTx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
    }

    // 대여 신청
    public RentalResponse apply(String token, RentalRequest request) {
        User user = currentUser(token); // ① 로그인 + 비밀번호 변경 완료 확인
        expireOverdue(user);            // ② 반납 예정일이 된 승인 대기 → 만료 (바로 저장)

        return applyTx.execute(status -> { // ③ 여기부터 저장까지 한 트랜잭션
            // 같은 학생의 신청은 한 번에 하나씩 처리 (이 트랜잭션이 끝날 때까지 잠금 유지)
            users.lockByStudentNumber(user.getStudentNumber());

            LocalDate today = LocalDate.now();
            LocalDate dueAt = request.dueAt();
            if (!dueAt.isAfter(today) || dueAt.isAfter(today.plusDays(MAX_RENTAL_DAYS))) { // 날짜: 내일 ~ 14일 뒤
                throw error(HttpStatus.BAD_REQUEST, "반납 예정일은 내일부터 " + MAX_RENTAL_DAYS + "일 이내로 선택해 주세요.");
            }

            Item item = items.findById(request.itemId()) // 있는 물품인지
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "물품을 찾을 수 없습니다."));

            if (item.getAvailableQuantity() <= 0) { // 재고 있는지 (재고는 관리자가 승인할 때 줄어듦)
                throw error(HttpStatus.CONFLICT, "지금은 대여 가능한 수량이 없습니다.");
            }

            if (rentals.existsByUserIdAndItemIdAndStatus(user.getId(), item.getId(), RentalStatus.PENDING)) { // 중복 신청 막기
                throw error(HttpStatus.CONFLICT, "이미 승인 대기 중인 신청이 있습니다.");
            }

            Rental saved = rentals.save(new Rental(user.getId(), item.getId(), today, dueAt)); // 승인 대기로 저장
            return RentalResponse.of(saved, item.getName());
        });
    }

    // 내 대여 목록 (최신 신청이 위로)
    public List<RentalResponse> myRentals(String token) {
        User user = currentUser(token); // ①
        expireOverdue(user);            // ②

        return readTx.execute(status -> // ③ 조회만
                rentals.findByUserIdOrderByIdDesc(user.getId()).stream()
                        .map(rental -> RentalResponse.of(rental, itemName(rental.getItemId())))
                        .toList());
    }

    // 신청 취소: 내 신청 + 승인 대기일 때만
    public RentalResponse cancel(String token, Long rentalId) {
        User user = currentUser(token); // ①
        expireOverdue(user);            // ② 기한 지난 신청은 여기서 먼저 만료로 확정

        return writeTx.execute(status -> { // ③
            Rental rental = rentals.lockById(rentalId) // 이 신청을 잠금 (관리자 승인·거절과 동시에 와도 하나만 처리)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "대여 신청을 찾을 수 없습니다."));

            if (!rental.getUserId().equals(user.getId())) {
                throw error(HttpStatus.FORBIDDEN, "본인 신청만 취소할 수 있습니다.");
            }
            if (rental.getStatus() != RentalStatus.PENDING) {
                throw error(HttpStatus.CONFLICT, "승인 대기 중인 신청만 취소할 수 있습니다.");
            }

            rental.cancel();
            return RentalResponse.of(rental, itemName(rental.getItemId()));
        });
    }
    // 반납 요청: 내 대여 + 대여 중일 때만 (대여 중 → 반납 요청, 재고는 관리자가 반납 완료할 때 늘어남)
    public RentalResponse requestReturn(String token, Long rentalId) {
        User user = currentUser(token); // ① (만료 처리 ②는 승인 대기에만 해당돼서 여기서는 필요 없음)

        return writeTx.execute(status -> { // ③
            Rental rental = rentals.lockById(rentalId) // 이 대여를 잠금 (관리자 반납 완료와 동시에 와도 하나만 처리)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "대여 기록을 찾을 수 없습니다."));

            if (!rental.getUserId().equals(user.getId())) {
                throw error(HttpStatus.FORBIDDEN, "본인 대여만 반납 요청할 수 있습니다.");
            }
            if (rental.getStatus() != RentalStatus.RENTED) {
                throw error(HttpStatus.CONFLICT, "대여 중인 물품만 반납 요청할 수 있습니다.");
            }

            rental.requestReturn();
            return RentalResponse.of(rental, itemName(rental.getItemId()));
        });
    }

    // ① 로그인 확인 + 임시 비밀번호 상태(비밀번호 변경 전)면 막기
    private User currentUser(String token) {
        User user = auth.authenticate(token);
        if (user.mustChangePassword()) {
            throw error(HttpStatus.FORBIDDEN, "비밀번호를 변경한 뒤 이용해 주세요.");
        }
        return user;
    }

    // ② 앱과 같은 기준: 승인 대기인데 반납 예정일이 오늘이거나 지났으면 만료 (따로 저장)
    private void expireOverdue(User user) {
        writeTx.executeWithoutResult(status ->
                rentals.changeOverdueStatus(user.getId(), LocalDate.now(), RentalStatus.PENDING, RentalStatus.EXPIRED));
    }

    private String itemName(Long itemId) {
        return items.findById(itemId).map(Item::getName).orElse("삭제된 물품");
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}