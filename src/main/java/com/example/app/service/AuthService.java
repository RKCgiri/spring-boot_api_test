package com.example.app.service;

import com.example.app.dto.request.LoginRequest;
import com.example.app.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse forgotPassword(com.example.app.dto.request.ForgotPasswordRequest request);
    com.example.app.dto.response.MessageResponse verifyOtp(com.example.app.dto.request.VerifyOtpRequest request);
    com.example.app.dto.response.MessageResponse resetPassword(com.example.app.dto.request.ResetPasswordRequest request);
}
