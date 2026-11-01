package com.example.app.dto.response;

import com.example.app.enums.Permission;
import com.example.app.enums.Role;

import java.util.Set;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long refreshTokenExpiresInMs,
    String email,
    Role role,
    Set<Permission> permissions
) {
    public LoginResponse(String accessToken, String refreshToken, long refreshTokenExpiresInMs,
                         String email, Role role, Set<Permission> permissions) {
        this(accessToken, refreshToken, "Bearer", refreshTokenExpiresInMs, email, role, permissions);
    }
}
