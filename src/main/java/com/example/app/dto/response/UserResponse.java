package com.example.app.dto.response;

import com.example.app.enums.Permission;
import com.example.app.enums.Role;
import com.example.app.entity.User;
import com.example.app.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phone,
    Role role,
    UserStatus status,
    String school,
    String region,
    LocalDateTime lastLogin,
    Set<Permission> permissions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /** Factory method — converts a JPA entity to this DTO. */
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole(),
            user.getStatus(),
            user.getSchool(),
            user.getRegion(),
            user.getLastLogin(),
            user.getPermissions(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
