package com.example.app.dto.response;

/**
 * Paginated list wrapper returned by {@code GET /api/users}.
 *
 * @param content    List of users on the current page.
 * @param page       0-based current page number.
 * @param size       Number of items per page.
 * @param totalItems Total number of matching records.
 * @param totalPages Total number of pages.
 */
public record PagedResponse<T>(
    java.util.List<T> content,
    int page,
    int size,
    long totalItems,
    int totalPages
) {}
