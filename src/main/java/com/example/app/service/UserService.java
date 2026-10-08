package com.example.app.service;

import com.example.app.dto.request.UserRequest;
import com.example.app.dto.response.PagedResponse;
import com.example.app.dto.response.UserResponse;
import com.example.app.dto.response.UserStatsResponse;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;

public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse getUserById(Long id);

    PagedResponse<UserResponse> getUsers(
        String search,
        Role role,
        UserStatus status,
        String school,
        String region,
        int page,
        int size
    );

    UserResponse updateUser(Long id, UserRequest request);

    void recordLogin(Long id);

    void deleteUser(Long id);

    UserStatsResponse getStats();

    String exportCsv(String search, Role role, UserStatus status, String school, String region);
}
