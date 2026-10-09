package com.example.app.dto.response;

public record UserStatsResponse(
    long totalUsers,
    long newThisMonth,
    long centralAdmins,
    long activeCentralAdmins,
    long contentAdmins,
    long activeContentAdmins,
    long schoolAdmins,
    long inactiveSchoolAdmins
) {}
