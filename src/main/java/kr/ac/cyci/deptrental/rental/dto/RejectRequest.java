package kr.ac.cyci.deptrental.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 관리자가 거절할 때 보내는 내용: 거절 사유 (앱과 같은 규칙: 1~300자)
public record RejectRequest(
        @NotBlank(message = "거절 사유를 입력해 주세요.")
        @Size(max = 300, message = "거절 사유는 300자 이내로 입력해 주세요.") String reason) {
}