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
}