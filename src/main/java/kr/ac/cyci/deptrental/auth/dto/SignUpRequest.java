package kr.ac.cyci.deptrental.auth.dto;
import jakarta.validation.constraints.*;
public record SignUpRequest(
    @NotBlank(message="비밀번호를 입력해 주세요.") @Size(min=8, max=64, message="비밀번호는 8~64자로 입력해 주세요.") String password,
    @NotBlank(message="이름을 입력해 주세요.") @Size(min=2, max=30, message="이름은 2~30자로 입력해 주세요.") String displayName,
    @NotBlank(message="학번을 입력해 주세요.") @Pattern(regexp="[0-9]{6,12}", message="학번은 숫자 6~12자리로 입력해 주세요.") String studentNumber,
    @NotBlank(message="전화번호를 입력해 주세요.")
    @Pattern(regexp="01[016789][0-9]{7,8}", message="휴대전화번호를 숫자 10~11자리로 입력해 주세요.") String phoneNumber) { }
