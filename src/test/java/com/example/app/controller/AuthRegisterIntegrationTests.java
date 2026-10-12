package com.example.app.controller;

import com.example.app.entity.User;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRegisterIntegrationTests {

    private static final String VALID_FIRST_NAME = "Jane";
    private static final String VALID_LAST_NAME = "Doe";
    private static final String VALID_EMAIL = "jane.doe@example.com";
    private static final String VALID_PASSWORD = "Password@123";

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
        userRepository.findByEmail(VALID_EMAIL).ifPresent(userRepository::delete);
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> map = new HashMap<>();
        map.put("firstName", VALID_FIRST_NAME);
        map.put("lastName", VALID_LAST_NAME);
        map.put("email", VALID_EMAIL);
        map.put("password", VALID_PASSWORD);
        return map;
    }

    @Test
    void register_publicWithoutJwt_returns201_andSafeUserResponse() throws Exception {
        Map<String, Object> request = validPayload();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.fullName").value(VALID_FIRST_NAME + " " + VALID_LAST_NAME))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(jsonNode.has("password"), "Password must never be exposed in response");
        assertFalse(jsonNode.has("otp"), "OTP must not be exposed in response");
        assertFalse(jsonNode.has("passwordResetToken"), "Reset token must not be exposed");

        // Verify entity in database
        User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertNotEquals(VALID_PASSWORD, savedUser.getPassword(), "Password must not be stored in plaintext");
        assertTrue(savedUser.getPassword().startsWith("$2"), "Password must be BCrypt hashed");
        assertTrue(passwordEncoder.matches(VALID_PASSWORD, savedUser.getPassword()), "BCrypt hash must match original password");
    }

    @Test
    void register_canLoginImmediatelyWithRegisteredCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validPayload())))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", VALID_EMAIL, "password", VALID_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        assertTrue(loginResponse.hasNonNull("accessToken"), "Successful login after registration must return JWT");
        assertEquals(VALID_EMAIL, loginResponse.get("email").asText());
        assertEquals("USER", loginResponse.get("role").asText());
    }

    @Test
    void register_withOptionalFields_savesAndReturnsThem() throws Exception {
        Map<String, Object> request = validPayload();
        request.put("phone", "+1234567890");
        request.put("school", "Oakridge High");
        request.put("region", "North District");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.school").value("Oakridge High"))
                .andExpect(jsonPath("$.region").value("North District"));

        User saved = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertEquals("+1234567890", saved.getPhone());
        assertEquals("Oakridge High", saved.getSchool());
        assertEquals("North District", saved.getRegion());
    }

    @Test
    void register_duplicateEmail_returns409Conflict() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validPayload())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validPayload())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void register_duplicateEmail_caseInsensitive_returns409Conflict() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validPayload())))
                .andExpect(status().isCreated());

        Map<String, Object> duplicateUpper = validPayload();
        duplicateUpper.put("email", VALID_EMAIL.toUpperCase());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(duplicateUpper)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_attemptingCentralAdminRole_returns400BadRequest() throws Exception {
        Map<String, Object> request = validPayload();
        request.put("role", "CENTRAL_ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("privileged role")));

        assertFalse(userRepository.existsByEmail(VALID_EMAIL), "User must not be saved when privileged role attempt fails");
    }

    @Test
    void register_attemptingContentAdminRole_returns400BadRequest() throws Exception {
        Map<String, Object> request = validPayload();
        request.put("role", "CONTENT_ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void register_attemptingSchoolAdminRole_returns400BadRequest() throws Exception {
        Map<String, Object> request = validPayload();
        request.put("role", "SCHOOL_ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void register_explicitUserRole_isAllowed() throws Exception {
        Map<String, Object> request = validPayload();
        request.put("role", "USER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_missingRequiredFields_returns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_invalidEmailFormat_returns400BadRequest() throws Exception {
        Map<String, Object> request = validPayload();
        request.put("email", "not-a-valid-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void register_weakPassword_returns400BadRequest() throws Exception {
        Map<String, Object> request = validPayload();
        request.put("password", "weakpass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_malformedJson_returns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());
    }
}
