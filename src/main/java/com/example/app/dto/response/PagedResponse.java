package com.example.app.dto.response;


public record PagedResponse<T>(
    java.util.List<T> content,
    int page,
    int size,
    long totalItems,
    int totalPages
) {}
