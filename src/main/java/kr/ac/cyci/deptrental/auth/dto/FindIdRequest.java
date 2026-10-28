package kr.ac.cyci.deptrental.auth.dto;

import jakarta.validation.constraints.*;

public record FindIdRequest(@NotBlank @Email String email, @NotBlank @Size(max = 100) String displayName) { }
