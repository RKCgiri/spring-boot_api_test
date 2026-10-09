package com.example.app.service;

import com.example.app.dto.request.UserRequest;
import com.example.app.dto.response.PagedResponse;
import com.example.app.dto.response.UserResponse;
import com.example.app.dto.response.UserStatsResponse;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;

public interface UserService {

    UserResponse createUser(UserRequest request);

    PagedResponse<UserResponse> getUsers(
        String search,
        Role role,
        UserStatus status,
        String school,
        String region,
        int page,
        int size
    );

    UserStatsResponse getStats();

    String exportCsv(String search, Role role, UserStatus status, String school, String region);
}
