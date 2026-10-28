package kr.ac.cyci.deptrental.auth.dto;

import jakarta.validation.constraints.*;

public record PasswordResetRequest(@NotBlank @Size(max = 50) String loginId, @NotBlank @Email String email) { }
