package com.example.app.service;

import com.example.app.dto.request.LoginRequest;
import com.example.app.dto.response.LoginResponse;
import com.example.app.dto.request.ForgotPasswordRequest;
import com.example.app.dto.request.VerifyOtpRequest;
import com.example.app.dto.request.ResetPasswordRequest;
import com.example.app.dto.response.MessageResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse verifyOtp(VerifyOtpRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}
