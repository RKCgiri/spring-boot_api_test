package com.example.app.security;

import com.example.app.entity.User;
import com.example.app.enums.Role;
import com.example.app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTests {

    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_PASSWORD = "Admin@1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);
        createUser(ADMIN_EMAIL, ADMIN_PASSWORD, "CENTRAL_ADMIN");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * ADJUST THIS to match your User entity (field names / role type).
     */
    private User createUser(String email, String rawPassword, String role) {
        User u = new User();
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(Role.valueOf(role));
        return userRepository.save(u);
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private MvcResult postJson(String url, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andReturn();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(node.hasNonNull("accessToken"), "Login response must contain accessToken");
        return node.get("accessToken").asText();
    }

    // ------------------------------------------------------------------
    // Public endpoints
    // ------------------------------------------------------------------

    @Test
    void pingAndHealth_arePublic() throws Exception {
        mockMvc.perform(get("/api/ping")).andExpect(status().isOk());
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void register_isPublicEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "firstName", "Public",
                                "lastName", "User",
                                "email", "public.user@test.com",
                                "password", "Public@1234"
                        ))))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Test
    void login_withValidCredentials_returnsJwt_andNoOtp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(node.hasNonNull("accessToken"));
        assertFalse(node.has("otp"), "OTP must never be returned by login");
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", "Wrong@1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@test.com", "password", "Wrong@1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIsStoredHashed() {
        User u = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        assertFalse(ADMIN_PASSWORD.equals(u.getPassword()), "Password must not be stored in plaintext");
        assertTrue(u.getPassword().startsWith("$2"), "Expected a BCrypt hash");
    }

    // ------------------------------------------------------------------
    // Forgot password / OTP
    // ------------------------------------------------------------------

    @Test
    void forgotPassword_doesNotLeakOtp() throws Exception {
        MvcResult result = postJson("/api/auth/forgot-password", Map.of("email", ADMIN_EMAIL));

        org.junit.jupiter.api.Assertions.assertEquals(200, result.getResponse().getStatus());
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(node.has("otp"), "OTP must not be in the response");
    }

    @Test
    void forgotPassword_unknownEmail_returnsSameStatusAsKnownEmail() throws Exception {
        int known = postJson("/api/auth/forgot-password", Map.of("email", ADMIN_EMAIL))
                .getResponse().getStatus();
        int unknown = postJson("/api/auth/forgot-password", Map.of("email", "ghost@test.com"))
                .getResponse().getStatus();

        org.junit.jupiter.api.Assertions.assertEquals(known, unknown,
                "Response must not reveal whether an email is registered");
    }

    @Test
    void verifyOtp_wrongOtp_returns401() throws Exception {
        postJson("/api/auth/forgot-password", Map.of("email", ADMIN_EMAIL));

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "otp", "000000"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyOtp_expiredOtp_returns401() throws Exception {
        postJson("/api/auth/forgot-password", Map.of("email", ADMIN_EMAIL));

        User u = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        u.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.saveAndFlush(u);

        // Even a correct OTP must be rejected once expired.
        // If you store only a hash of the OTP, replace u.getOtp() with a known test value.
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "otp", String.valueOf(u.getOtp())))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyOtp_lockedAfterFiveFailedAttempts() throws Exception {
        postJson("/api/auth/forgot-password", Map.of("email", ADMIN_EMAIL));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", ADMIN_EMAIL, "otp", "000000"))))
                    .andExpect(status().isUnauthorized());
        }

        User u = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        // Correct OTP after lockout must still fail.
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "otp", String.valueOf(u.getOtp())))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Reset password
    // ------------------------------------------------------------------

    @Test
    void resetPassword_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "resetToken", "not-a-real-token", "newPassword", "Str0ng@Pass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPassword_withWeakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "resetToken", "anything", "newPassword", "weak"))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Unauthenticated access -> 401
    // ------------------------------------------------------------------

    @Test
    void securedEndpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/status")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/export/csv")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/schools")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/schools/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/schools/export")).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Authorization (RBAC) -> 403 / 200
    // ------------------------------------------------------------------

    @Test
    void centralAdmin_canAccessUserAndSchoolReads() throws Exception {
        mockMvc.perform(get("/api/users").with(user("a").roles("CENTRAL_ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/export/csv").with(user("a").roles("CENTRAL_ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/schools/export").with(user("a").roles("CENTRAL_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void contentAdmin_canList_butCannotExport_orCreate() throws Exception {
        mockMvc.perform(get("/api/users").with(user("c").roles("CONTENT_ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/status").with(user("c").roles("CONTENT_ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/schools/stats").with(user("c").roles("CONTENT_ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/export/csv").with(user("c").roles("CONTENT_ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/schools/export").with(user("c").roles("CONTENT_ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users").with(user("c").roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/schools").with(user("c").roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void schoolAdmin_canListSchools_butNotUsers() throws Exception {
        mockMvc.perform(get("/api/schools").with(user("s").roles("SCHOOL_ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users").with(user("s").roles("SCHOOL_ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/schools/stats").with(user("s").roles("SCHOOL_ADMIN")))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Real JWT end-to-end
    // ------------------------------------------------------------------

    @Test
    void jwtFromLogin_grantsAccessToProtectedEndpoint() throws Exception {
        String token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertNotNull(token);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Pagination bounds validation
    // ------------------------------------------------------------------

    @Test
    void listUsers_sizeAboveMax_returns400() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "101")
                        .with(user("a").roles("CENTRAL_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_sizeZero_returns400() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "0")
                        .with(user("a").roles("CENTRAL_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "-1")
                        .param("size", "10")
                        .with(user("a").roles("CENTRAL_ADMIN")))
                .andExpect(status().isBadRequest());
    }
}