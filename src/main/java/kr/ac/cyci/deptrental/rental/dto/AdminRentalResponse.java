package kr.ac.cyci.deptrental.rental.dto;

import java.time.LocalDate;
import kr.ac.cyci.deptrental.rental.Rental;
import kr.ac.cyci.deptrental.rental.RentalStatus;

// 관리자 화면용 대여 정보 (내부 사용자 번호 대신 학생 이름·학번을 보여줌)
public record AdminRentalResponse(
        Long id,
        Long itemId,
        String itemName,
        String studentName,
        String studentNumber,
        LocalDate rentedAt,
        LocalDate dueAt,
        LocalDate returnedAt,
        RentalStatus status,
        String rejectReason) {

    public static AdminRentalResponse of(Rental rental, String itemName, String studentName, String studentNumber) {
        return new AdminRentalResponse(
                rental.getId(),
                rental.getItemId(),
                itemName,
                studentName,
                studentNumber,
                rental.getRentedAt(),
                rental.getDueAt(),
                rental.getReturnedAt(),
                rental.getStatus(),
                rental.getRejectReason());
    }
}