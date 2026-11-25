package kr.ac.cyci.deptrental.rental.dto;

import java.time.LocalDate;
import kr.ac.cyci.deptrental.rental.Rental;
import kr.ac.cyci.deptrental.rental.RentalStatus;

// 서버가 앱에 돌려주는 대여 정보 (물품 이름도 같이 보내서 앱이 바로 화면에 띄울 수 있게)
public record RentalResponse(
        Long id,
        Long itemId,
        String itemName,
        LocalDate rentedAt,
        LocalDate dueAt,
        LocalDate returnedAt,
        RentalStatus status,
        String rejectReason) {

    public static RentalResponse of(Rental rental, String itemName) {
        return new RentalResponse(
                rental.getId(),
                rental.getItemId(),
                itemName,
                rental.getRentedAt(),
                rental.getDueAt(),
                rental.getReturnedAt(),
                rental.getStatus(),
                rental.getRejectReason());
    }
}