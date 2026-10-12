package com.example.app.service;

import com.example.app.dto.request.ForgotPasswordRequest;
import com.example.app.dto.request.LoginRequest;
import com.example.app.dto.request.RegisterRequest;
import com.example.app.dto.request.ResetPasswordRequest;
import com.example.app.dto.request.VerifyOtpRequest;
import com.example.app.dto.response.LoginResponse;
import com.example.app.dto.response.MessageResponse;
import com.example.app.dto.response.UserResponse;
import com.example.app.dto.response.VerifyOtpResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}
