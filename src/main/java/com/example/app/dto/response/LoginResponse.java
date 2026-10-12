package com.example.app.dto.response;

import com.example.app.enums.Permission;
import com.example.app.enums.Role;

import java.util.Set;

public record LoginResponse(
    String accessToken,
    String tokenType,
    String email,
    Role role,
    Set<Permission> permissions
) {
    public LoginResponse(String accessToken, String email, Role role, Set<Permission> permissions) {
        this(accessToken, "Bearer", email, role, permissions);
    }
}
