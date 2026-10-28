package kr.ac.cyci.deptrental.auth.dto;

import jakarta.validation.constraints.*;

public record SignUpRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{4,50}") String loginId,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Email @Size(max = 255) String email) { }
