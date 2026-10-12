package com.example.app.service;

import com.example.app.dto.request.LoginRequest;
import com.example.app.dto.response.LoginResponse;
import com.example.app.dto.request.ForgotPasswordRequest;
import com.example.app.dto.request.VerifyOtpRequest;
import com.example.app.dto.request.ResetPasswordRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse forgotPassword(ForgotPasswordRequest request);
    com.example.app.dto.response.MessageResponse verifyOtp(VerifyOtpRequest request);
    com.example.app.dto.response.MessageResponse resetPassword(ResetPasswordRequest request);
}
