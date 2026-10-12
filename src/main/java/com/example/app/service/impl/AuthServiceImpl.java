    package com.example.app.service.impl;

    import com.example.app.dto.request.ForgotPasswordRequest;
    import com.example.app.dto.request.LoginRequest;
    import com.example.app.dto.request.RegisterRequest;
    import com.example.app.dto.request.ResetPasswordRequest;
    import com.example.app.dto.request.VerifyOtpRequest;
    import com.example.app.dto.response.LoginResponse;
    import com.example.app.dto.response.MessageResponse;
    import com.example.app.dto.response.UserResponse;
    import com.example.app.dto.response.VerifyOtpResponse;
    import com.example.app.entity.User;
    import com.example.app.enums.Role;
    import com.example.app.enums.UserStatus;
    import com.example.app.exception.DuplicateResourceException;
    import com.example.app.exception.UnauthorizedException;
    import com.example.app.repository.UserRepository;
    import com.example.app.security.JwtTokenProvider;
    import com.example.app.service.AuthService;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.security.SecureRandom;
    import java.time.LocalDateTime;
    import java.util.Collections;
    import java.util.Set;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class AuthServiceImpl implements AuthService {

        private static final int MAX_OTP_ATTEMPTS = 5;
        private static final int OTP_EXPIRY_MINUTES = 10;
        private static final Set<Role> PRIVILEGED_ROLES = Set.of(
            Role.CENTRAL_ADMIN,
            Role.CONTENT_ADMIN,
            Role.SCHOOL_ADMIN
        );

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;
        private final SecureRandom secureRandom = new SecureRandom();

        @Override
        @Transactional
        public UserResponse register(RegisterRequest request) {
            log.info("Registration attempt for email: {}", request.email());

            String normalizedEmail = request.email() != null ? request.email().trim().toLowerCase() : null;
            if (normalizedEmail == null || normalizedEmail.isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }

            if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                throw new DuplicateResourceException(
                    "A user with email '" + request.email().trim() + "' already exists");
            }

            // Security check: Never allow users to assign themselves CENTRAL_ADMIN or other privileged roles
            if (request.role() != null && isPrivilegedRole(request.role())) {
                throw new IllegalArgumentException(
                    "Registration cannot assign privileged role: " + request.role());
            }

            Role assignedRole = (request.role() != null && !isPrivilegedRole(request.role()))
                    ? request.role()
                    : Role.USER;

            String encodedPassword = passwordEncoder.encode(request.password());

            User user = User.builder()
                    .firstName(request.firstName() != null ? request.firstName().trim() : null)
                    .lastName(request.lastName() != null ? request.lastName().trim() : null)
                    .email(normalizedEmail)
                    .password(encodedPassword)
                    .phone(request.phone() != null ? request.phone().trim() : null)
                    .role(assignedRole)
                    .status(UserStatus.ACTIVE)
                    .school(request.school() != null ? request.school().trim() : null)
                    .region(request.region() != null ? request.region().trim() : null)
                    .permissions(Collections.emptySet())
                    .otpAttempts(0)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("User registered successfully with id={} email={} role={}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

            return UserResponse.from(savedUser);
        }

        private boolean isPrivilegedRole(Role role) {
            return PRIVILEGED_ROLES.contains(role);
        }

        @Override
        @Transactional
        public LoginResponse login(LoginRequest request) {
            log.info("Login attempt for email: {}", request.email());

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

            if (user.getStatus() == UserStatus.LOCKED) {
                throw new UnauthorizedException("Account is locked. Please contact support.");
            }

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new UnauthorizedException("Account is inactive.");
            }

            if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new UnauthorizedException("Invalid email or password");
            }

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtTokenProvider.generateAccessToken(user);
            log.info("User {} logged in successfully", user.getEmail());

            return new LoginResponse(token, user.getEmail(), user.getRole(), user.getPermissions());
        }

        @Override
        @Transactional
        public MessageResponse forgotPassword(ForgotPasswordRequest request) {
            log.info("Forgot password requested for email: {}", request.email());

            userRepository.findByEmail(request.email()).ifPresent(user -> {
                String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
                user.setOtp(otp);
                user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
                user.setOtpAttempts(0);
                user.setPasswordResetToken(null);
                user.setPasswordResetTokenExpiresAt(null);
                userRepository.save(user);

                // Log securely for dev/testing without exposing via HTTP payload
                log.debug("Secure OTP generated for password reset (user: {}): {}", user.getEmail(), otp);
            });

            return new MessageResponse("If the email is registered, a password reset code has been sent.");
        }

        @Override
        @Transactional
        public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
            log.info("Verify OTP attempt for email: {}", request.email());

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP"));

            if (user.getOtp() == null || user.getOtpExpiresAt() == null) {
                throw new UnauthorizedException("No active OTP found. Please request a new OTP.");
            }

            if (LocalDateTime.now().isAfter(user.getOtpExpiresAt())) {
                user.setOtp(null);
                user.setOtpExpiresAt(null);
                userRepository.save(user);
                throw new UnauthorizedException("OTP has expired. Please request a new OTP.");
            }

            int currentAttempts = user.getOtpAttempts() != null ? user.getOtpAttempts() : 0;
            if (currentAttempts >= MAX_OTP_ATTEMPTS) {
                user.setOtp(null);
                user.setOtpExpiresAt(null);
                userRepository.save(user);
                throw new UnauthorizedException("Maximum OTP attempts exceeded. Please request a new OTP.");
            }

            if (!user.getOtp().equals(request.otp())) {
                user.setOtpAttempts(currentAttempts + 1);
                userRepository.save(user);
                throw new UnauthorizedException("Invalid OTP. Attempt " + (currentAttempts + 1) + " of " + MAX_OTP_ATTEMPTS);
            }

            // OTP is valid - issue single-use reset token and invalidate OTP
            String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getEmail());
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            user.setOtp(null);
            user.setOtpExpiresAt(null);
            user.setOtpAttempts(0);
            userRepository.save(user);

            log.info("OTP verified successfully for user: {}", user.getEmail());
            return new VerifyOtpResponse("OTP verified successfully", resetToken);
        }

        @Override
        @Transactional
        public MessageResponse resetPassword(ResetPasswordRequest request) {
            log.info("Reset password attempt for email: {}", request.email());

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UnauthorizedException("Invalid password reset request"));

            boolean tokenValid = false;
            if (request.resetToken() != null && !request.resetToken().isBlank()) {
                boolean jwtOk = jwtTokenProvider.validatePasswordResetToken(request.resetToken(), request.email());
                boolean dbTokenMatches = request.resetToken().equals(user.getPasswordResetToken());
                boolean notExpired = user.getPasswordResetTokenExpiresAt() != null &&
                        LocalDateTime.now().isBefore(user.getPasswordResetTokenExpiresAt());

                tokenValid = jwtOk && dbTokenMatches && notExpired;
            } else if (request.otp() != null && !request.otp().isBlank()) {
                boolean otpMatches = user.getOtp() != null && user.getOtp().equals(request.otp());
                boolean notExpired = user.getOtpExpiresAt() != null &&
                        LocalDateTime.now().isBefore(user.getOtpExpiresAt());

                tokenValid = otpMatches && notExpired;
            }

            if (!tokenValid) {
                throw new UnauthorizedException("Invalid or expired password reset token / OTP");
            }

            // Set hashed password with BCrypt
            user.setPassword(passwordEncoder.encode(request.newPassword()));

            // Invalidate reset token and OTP immediately
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiresAt(null);
            user.setOtp(null);
            user.setOtpExpiresAt(null);
            user.setOtpAttempts(0);
            userRepository.save(user);

            log.info("Password reset successfully for user: {}", user.getEmail());
            return new MessageResponse("Password reset successfully");
        }
    }
