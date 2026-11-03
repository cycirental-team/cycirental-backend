package kr.ac.cyci.deptrental.auth.dto;
import jakarta.validation.constraints.*;
public record ChangePasswordRequest(@NotBlank @Size(max=64) String currentPassword,
    @NotBlank @Size(min=8,max=64) String newPassword) { }
