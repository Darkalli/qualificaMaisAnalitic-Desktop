package com.qualificamaisanaliticinterface.features.login.dto;

public record LoginResponse(String token, String tokenType, String expiresAt, UserInfo user) {
    public record UserInfo(Long id, String username, String role) {}
}
