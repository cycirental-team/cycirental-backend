package kr.ac.cyci.deptrental.rental.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// 앱이 대여 신청할 때 보내는 내용: 어떤 물품을, 언제까지
public record RentalRequest(
        @NotNull(message = "물품을 선택해 주세요.") Long itemId,
        @NotNull(message = "반납 예정일을 선택해 주세요.") LocalDate dueAt) {
}