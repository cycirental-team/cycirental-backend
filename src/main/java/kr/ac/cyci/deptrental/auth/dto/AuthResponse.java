package kr.ac.cyci.deptrental.auth.dto;

public record AuthResponse(Long userId, String loginId, String displayName, String accessToken, String tokenType, boolean isAdmin, String studentNumber, boolean mustChangePassword) { }
