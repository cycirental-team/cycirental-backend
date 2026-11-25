package kr.ac.cyci.deptrental.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeRequest(
        @NotBlank(message = "공지 제목을 입력해 주세요.") @Size(max = 200, message = "공지 제목은 200자 이내로 입력해 주세요.") String title,
        @NotBlank(message = "공지 내용을 입력해 주세요.") @Size(max = 10000, message = "공지 내용은 10,000자 이내로 입력해 주세요.") String content,
        boolean important) { }
