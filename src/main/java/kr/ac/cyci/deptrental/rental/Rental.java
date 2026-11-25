package kr.ac.cyci.deptrental.rental;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rentals")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;        // 누가

    @Column(name = "item_id", nullable = false)
    private Long itemId;        // 뭘

    @Column(name = "rented_at", nullable = false)
    private LocalDate rentedAt; // 신청일

    @Column(name = "due_at", nullable = false)
    private LocalDate dueAt;    // 반납 예정일

    @Column(name = "returned_at")
    private LocalDate returnedAt; // 실제 반납일 (반납 전엔 비어 있음)

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // DB에 상태를 글자(VARCHAR)로 저장
    @Column(nullable = false, length = 30)
    private RentalStatus status;

    @Column(name = "reject_reason", length = 300)
    private String rejectReason; // 거절 사유 (거절된 경우만 값이 있음)

    protected Rental() {}

    // 새 대여 신청: 처음엔 항상 "승인 대기"
    public Rental(Long userId, Long itemId, LocalDate rentedAt, LocalDate dueAt) {
        this.userId = userId;
        this.itemId = itemId;
        this.rentedAt = rentedAt;
        this.dueAt = dueAt;
        this.status = RentalStatus.PENDING;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getItemId() { return itemId; }
    public LocalDate getRentedAt() { return rentedAt; }
    public LocalDate getDueAt() { return dueAt; }
    public LocalDate getReturnedAt() { return returnedAt; }
    public RentalStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }

    // 신청 취소: 상태만 "취소됨"으로 바꿈 (취소해도 되는지는 RentalService가 먼저 확인)
    public void cancel() {
        this.status = RentalStatus.CANCELLED;
    }
    // 관리자 승인: 상태만 "대여 중"으로 바꿈 (승인해도 되는지·재고는 AdminRentalService가 먼저 확인)
    public void approve() {
        this.status = RentalStatus.RENTED;
    }
    // 관리자 거절: "거절됨"으로 바꾸고 사유를 저장 (거절해도 되는지는 AdminRentalService가 먼저 확인)
    public void reject(String reason) {
        this.status = RentalStatus.REJECTED;
        this.rejectReason = reason;
    }
    // 학생 반납 요청: 상태만 "반납 요청"으로 바꿈 (요청해도 되는지는 RentalService가 먼저 확인)
    public void requestReturn() {
        this.status = RentalStatus.RETURN_REQUESTED;
    }
    // 관리자 반납 완료: "반납 완료"로 바꾸고 실제 반납일을 기록 (완료해도 되는지는 AdminRentalService가 먼저 확인)
    public void completeReturn(LocalDate returnedAt) {
        this.status = RentalStatus.RETURNED;
        this.returnedAt = returnedAt;
    }
}

