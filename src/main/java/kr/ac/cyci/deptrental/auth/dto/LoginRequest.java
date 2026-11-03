package kr.ac.cyci.deptrental.auth.dto;
import jakarta.validation.constraints.*;
public record LoginRequest(
    @NotBlank @Pattern(regexp="(?:001|[0-9]{6,12})", message="학번을 숫자 6~12자리로 입력해 주세요.") String studentNumber,
    @NotBlank @Size(max=64) String password) { }
