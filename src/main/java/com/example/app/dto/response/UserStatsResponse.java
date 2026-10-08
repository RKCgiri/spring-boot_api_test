package com.example.app.dto.response;

/**
 * Summary statistics shown in the four cards on the Users Management page.
 *
 * @param totalUsers       Total number of users in the system.
 * @param newThisMonth     Users created in the current calendar month.
 * @param centralAdmins    Total CENTRAL_ADMIN users.
 * @param activeCentralAdmins  Active CENTRAL_ADMIN users.
 * @param contentAdmins    Total CONTENT_ADMIN users.
 * @param activeContentAdmins  Active CONTENT_ADMIN users.
 * @param schoolAdmins     Total SCHOOL_ADMIN users.
 * @param inactiveSchoolAdmins Inactive SCHOOL_ADMIN users.
 */
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
