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
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.exception.UnauthorizedException;
import com.example.app.dto.request.ForgotPasswordRequest;
import com.example.app.dto.request.VerifyOtpRequest;
import com.example.app.dto.request.ResetPasswordRequest;
import com.example.app.dto.response.MessageResponse;

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
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Normally we'd use BCrypt to check the password. Since this is simple:
        if (user.getPassword() == null || !user.getPassword().equals(request.password())) {
            throw new UnauthorizedException("Invalid email or password");
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
    public LoginResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        user.setOtp(otp);
        userRepository.save(user);

        log.info("Generated OTP for password reset for user {}: {}", request.email(), otp);

        return new LoginResponse("Password reset OTP generated successfully", otp);
    }

    @Override
    @Transactional
    public MessageResponse verifyOtp(VerifyOtpRequest request) {
        log.info("Verify OTP attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (user.getOtp() == null || !user.getOtp().equals(request.otp())) {
            throw new UnauthorizedException("Invalid OTP");
        }

        return new MessageResponse("OTP verified successfully");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        log.info("Reset password attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (user.getOtp() == null || !user.getOtp().equals(request.otp())) {
            throw new UnauthorizedException("Invalid OTP");
        }

        user.setPassword(request.newPassword());
        user.setOtp(null); // Clear OTP after successful reset
        userRepository.save(user);

        log.info("Password reset successfully for user {}", request.email());

        return new MessageResponse("Password reset successfully");
    }
}
