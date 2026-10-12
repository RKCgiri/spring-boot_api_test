package com.example.app.dto.response;

public record VerifyOtpResponse(
    String message,
    String resetToken
) {}
