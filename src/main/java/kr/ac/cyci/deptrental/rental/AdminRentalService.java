package kr.ac.cyci.deptrental.rental;

import java.time.LocalDate;
import java.util.List;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.User;
import kr.ac.cyci.deptrental.auth.UserRepository;
import kr.ac.cyci.deptrental.item.Item;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.AdminRentalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

// 관리자용 대여 기능 (학생용 RentalService와 같은 방식으로 단계마다 트랜잭션을 나눔)
//   ① 관리자 확인 → ② 만료 처리(바로 저장) → ③ 조회·처리
@Service
public class AdminRentalService {
    private final RentalRepository rentals;
    private final ItemRepository items;
    private final UserRepository users;
    private final AuthService auth;

    private final TransactionTemplate writeTx; // 저장용 (만료 처리, 승인, 거절, 반납 완료)
    private final TransactionTemplate readTx;  // 조회용 (readOnly)

    public AdminRentalService(RentalRepository rentals, ItemRepository items, UserRepository users,
                              AuthService auth, PlatformTransactionManager transactionManager) {
        this.rentals = rentals;
        this.items = items;
        this.users = users;
        this.auth = auth;

        this.writeTx = new TransactionTemplate(transactionManager);
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
    }

    // 관리자 대여 목록 (status가 없으면 전체, 최근 신청이 위로)
    public List<AdminRentalResponse> list(String token, RentalStatus status) {
        auth.requireAdmin(token); // ① 관리자 + 비밀번호 변경 완료 확인 (아니면 401·403)
        expireOverdue();          // ② 기한 지난 승인 대기 → 만료 (바로 저장)

        return readTx.execute(tx -> { // ③ 조회만
            List<Rental> found = (status == null)
                    ? rentals.findAllByOrderByIdDesc()
                    : rentals.findByStatusOrderByIdDesc(status);
            return found.stream().map(this::toResponse).toList();
        });
    }

    // 대여 승인: 승인 대기 → 대여 중, 대여 가능 수량 -1
    public AdminRentalResponse approve(String token, Long rentalId) {
        auth.requireAdmin(token); // ①
        expireOverdue();          // ② 기한 지난 신청은 먼저 만료로 확정 (만료된 신청은 승인 안 됨)

        return writeTx.execute(tx -> { // ③ 잠금부터 저장까지 한 트랜잭션
            Rental rental = rentals.lockById(rentalId) // 이 신청을 잠금 (다른 관리자가 동시에 처리 못 하게)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "대여 신청을 찾을 수 없습니다."));

            if (rental.getStatus() != RentalStatus.PENDING) {
                throw error(HttpStatus.CONFLICT, "승인 대기 중인 신청만 승인할 수 있습니다.");
            }

            if (items.decreaseAvailable(rental.getItemId()) == 0) { // 재고가 있을 때만 1 줄임
                throw error(HttpStatus.CONFLICT, "대여 가능한 수량이 없습니다.");
            }

            rental.approve(); // 재고를 줄인 다음에 상태 변경 (재고가 없으면 여기까지 안 옴)
            return toResponse(rental);
        });
    }

    // 대여 거절: 승인 대기 → 거절, 거절 사유 저장 (재고는 그대로)
    public AdminRentalResponse reject(String token, Long rentalId, String reason) {
        auth.requireAdmin(token); // ①
        expireOverdue();          // ② 기한 지난 신청은 먼저 만료로 확정 (만료된 신청은 거절 안 됨)

        return writeTx.execute(tx -> { // ③ 잠금부터 저장까지 한 트랜잭션
            Rental rental = rentals.lockById(rentalId) // 이 신청을 잠금 (승인과 거절이 동시에 눌려도 하나만 처리)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "대여 신청을 찾을 수 없습니다."));

            if (rental.getStatus() != RentalStatus.PENDING) {
                throw error(HttpStatus.CONFLICT, "승인 대기 중인 신청만 거절할 수 있습니다.");
            }

            rental.reject(reason.trim()); // 앞뒤 공백은 빼고 저장
            return toResponse(rental);
        });
    }

    // 반납 완료: 반납 요청 → 반납 완료, 실제 반납일 기록, 대여 가능 수량 +1
    public AdminRentalResponse completeReturn(String token, Long rentalId) {
        auth.requireAdmin(token); // ① (만료 처리 ②는 승인 대기에만 해당돼서 여기서는 필요 없음)

        return writeTx.execute(tx -> { // ③ 잠금부터 저장까지 한 트랜잭션
            Rental rental = rentals.lockById(rentalId) // 이 대여를 잠금 (두 관리자가 동시에 눌러도 한 번만 처리)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "대여 기록을 찾을 수 없습니다."));

            if (rental.getStatus() != RentalStatus.RETURN_REQUESTED) {
                throw error(HttpStatus.CONFLICT, "반납 요청된 대여만 반납 완료할 수 있습니다.");
            }

            items.increaseAvailable(rental.getItemId()); // 재고 +1 (전체 수량은 넘지 않음)
            rental.completeReturn(LocalDate.now());      // 앱은 반납 완료 상태에 반납일이 꼭 있어야 함
            return toResponse(rental);
        });
    }

    // ② 앱과 같은 기준: 승인 대기인데 반납 예정일이 오늘이거나 지났으면 만료 (모든 학생 대상)
    private void expireOverdue() {
        writeTx.executeWithoutResult(tx ->
                rentals.changeAllOverdueStatus(LocalDate.now(), RentalStatus.PENDING, RentalStatus.EXPIRED));
    }

    // 대여 기록 → 관리자 화면용 응답 (물품 이름, 학생 이름·학번을 찾아서 붙임)
    private AdminRentalResponse toResponse(Rental rental) {
        String itemName = items.findById(rental.getItemId()).map(Item::getName).orElse("삭제된 물품");
        User student = users.findById(rental.getUserId()).orElse(null);
        String studentName = (student == null) ? "알 수 없음" : student.getDisplayName();
        String studentNumber = (student == null) ? null : student.getStudentNumber();
        return AdminRentalResponse.of(rental, itemName, studentName, studentNumber);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}