package com.example.app.mapper;

import com.example.app.dto.request.RegisterRequest;
import com.example.app.dto.request.UserRequest;
import com.example.app.dto.response.LoginResponse;
import com.example.app.dto.response.UserResponse;
import com.example.app.dto.response.UserStatsResponse;
import com.example.app.entity.User;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UserMapper {

    public User toEntity(UserRequest request, String encodedPassword) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .role(request.role())
                .status(request.status() != null ? request.status() : UserStatus.ACTIVE)
                .school(request.school())
                .region(request.region())
                .permissions(request.permissions() != null ? request.permissions() : Collections.emptySet())
                .password(encodedPassword)
                .build();
    }

    public User toEntity(RegisterRequest request, String normalizedEmail, String encodedPassword, Role assignedRole) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .firstName(request.firstName() != null ? request.firstName().trim() : null)
                .lastName(request.lastName() != null ? request.lastName().trim() : null)
                .email(normalizedEmail)
                .password(encodedPassword)
                .phone(request.phone() != null ? request.phone().trim() : null)
                .role(assignedRole)
                .status(UserStatus.ACTIVE)
                .school(request.school() != null ? request.school().trim() : null)
                .region(request.region() != null ? request.region().trim() : null)
                .permissions(Collections.emptySet())
                .otpAttempts(0)
                .build();
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

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

    public LoginResponse toLoginResponse(User user, String token) {
        if (user == null) {
            return null;
        }

        return new LoginResponse(token, user.getEmail(), user.getRole(), user.getPermissions());
    }

    public UserStatsResponse toStatsResponse(
            long totalUsers,
            long newThisMonth,
            long centralAdmins,
            long activeCentralAdmins,
            long contentAdmins,
            long activeContentAdmins,
            long schoolAdmins,
            long inactiveSchoolAdmins
    ) {
        return new UserStatsResponse(
                totalUsers,
                newThisMonth,
                centralAdmins,
                activeCentralAdmins,
                contentAdmins,
                activeContentAdmins,
                schoolAdmins,
                inactiveSchoolAdmins
        );
    }
}
