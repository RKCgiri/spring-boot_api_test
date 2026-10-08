package com.example.app.dto.request;

import com.example.app.enums.Permission;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request body for creating or updating a user.
 * All fields are validated via Bean Validation.
 */
public record UserRequest(

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    String email,

    @Size(max = 255, message = "Password must be at most 255 characters")
    String password,

    @Size(max = 20, message = "Phone must be at most 20 characters")
    String phone,

    @NotNull(message = "Role is required")
    Role role,

    @NotNull(message = "Status is required")
    UserStatus status,

    /** School – required when role is SCHOOL_ADMIN */
    String school,

    String region,

    /** Permissions the user is granted (can be empty). */
    Set<Permission> permissions
) {}
