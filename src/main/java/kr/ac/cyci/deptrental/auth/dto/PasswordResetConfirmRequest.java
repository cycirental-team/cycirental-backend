package kr.ac.cyci.deptrental.auth.dto;

import jakarta.validation.constraints.*;

public record PasswordResetConfirmRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 72) String newPassword) { }
