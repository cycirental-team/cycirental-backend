package kr.ac.cyci.deptrental.auth.dto;
import jakarta.validation.constraints.*;
public record PasswordResetRequest(
    @NotBlank @Size(min=2,max=30) String displayName,
    @NotBlank @Pattern(regexp="(?:001|[0-9]{6,12})") String studentNumber) { }
