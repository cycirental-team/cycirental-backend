package kr.ac.cyci.deptrental.rental;

import java.time.LocalDate;
import java.util.List;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.User;
import kr.ac.cyci.deptrental.auth.UserRepository;
import kr.ac.cyci.deptrental.item.Item;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.rental.dto.AdminRentalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// 관리자용 대여 기능 (학생용 RentalService와 같은 방식으로 단계마다 트랜잭션을 나눔)
//   ① 관리자 확인 → ② 만료 처리(바로 저장) → ③ 조회·처리
@Service
public class AdminRentalService {
    private final RentalRepository rentals;
    private final ItemRepository items;
    private final UserRepository users;
    private final AuthService auth;

    private final TransactionTemplate writeTx; // 저장용 (만료 처리)
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
}