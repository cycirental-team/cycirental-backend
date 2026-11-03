package kr.ac.cyci.deptrental.auth.dto;

import jakarta.validation.constraints.*;

public record FindIdRequest(
    @NotBlank @Pattern(regexp = "[0-9]{6,12}") String studentNumber,
    @NotBlank @Size(max = 50) String displayName) { }
