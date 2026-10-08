package com.example.app.service.impl;

import com.example.app.dto.request.LoginRequest;
import com.example.app.dto.response.LoginResponse;
import com.example.app.entity.User;
import com.example.app.repository.UserRepository;
import com.example.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new com.example.app.exception.UnauthorizedException("Invalid email or password"));

        // Normally we'd use BCrypt to check the password. Since this is simple:
        if (user.getPassword() == null || !user.getPassword().equals(request.password())) {
            throw new com.example.app.exception.UnauthorizedException("Invalid email or password");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        user.setOtp(otp);
        userRepository.save(user);

        log.info("Generated OTP for user {}: {}", request.email(), otp);

        return new LoginResponse("OTP generated successfully", otp);
    }

    @Override
    @Transactional
    public LoginResponse forgotPassword(com.example.app.dto.request.ForgotPasswordRequest request) {
        log.info("Forgot password attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new com.example.app.exception.ResourceNotFoundException("User not found with email: " + request.email()));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        user.setOtp(otp);
        userRepository.save(user);

        log.info("Generated OTP for password reset for user {}: {}", request.email(), otp);

        return new LoginResponse("Password reset OTP generated successfully", otp);
    }

    @Override
    @Transactional
    public com.example.app.dto.response.MessageResponse verifyOtp(com.example.app.dto.request.VerifyOtpRequest request) {
        log.info("Verify OTP attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new com.example.app.exception.ResourceNotFoundException("User not found with email: " + request.email()));

        if (user.getOtp() == null || !user.getOtp().equals(request.otp())) {
            throw new com.example.app.exception.UnauthorizedException("Invalid OTP");
        }

        return new com.example.app.dto.response.MessageResponse("OTP verified successfully");
    }

    @Override
    @Transactional
    public com.example.app.dto.response.MessageResponse resetPassword(com.example.app.dto.request.ResetPasswordRequest request) {
        log.info("Reset password attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new com.example.app.exception.ResourceNotFoundException("User not found with email: " + request.email()));

        if (user.getOtp() == null || !user.getOtp().equals(request.otp())) {
            throw new com.example.app.exception.UnauthorizedException("Invalid OTP");
        }

        user.setPassword(request.newPassword());
        user.setOtp(null); // Clear OTP after successful reset
        userRepository.save(user);

        log.info("Password reset successfully for user {}", request.email());

        return new com.example.app.dto.response.MessageResponse("Password reset successfully");
    }
}
