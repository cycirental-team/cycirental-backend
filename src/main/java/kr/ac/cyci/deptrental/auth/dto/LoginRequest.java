package kr.ac.cyci.deptrental.auth.dto;
import jakarta.validation.constraints.*;
public record LoginRequest(
    @NotBlank(message="학번을 입력해 주세요.") @Pattern(regexp="(?:001|[0-9]{6,12})", message="학번을 숫자 6~12자리로 입력해 주세요.") String studentNumber,
    @NotBlank(message="비밀번호를 입력해 주세요.") @Size(max=64, message="비밀번호는 64자 이내로 입력해 주세요.") String password) { }
