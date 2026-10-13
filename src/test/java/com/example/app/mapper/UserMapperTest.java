package com.example.app.mapper;

import com.example.app.dto.request.RegisterRequest;
import com.example.app.dto.request.UserRequest;
import com.example.app.dto.response.LoginResponse;
import com.example.app.dto.response.UserResponse;
import com.example.app.dto.response.UserStatsResponse;
import com.example.app.entity.User;
import com.example.app.enums.Permission;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    void testToEntity_fromUserRequest() {
        UserRequest request = new UserRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "plainPassword",
                "1234567890",
                Role.SCHOOL_ADMIN,
                UserStatus.ACTIVE,
                "Springfield High",
                "North",
                Set.of(Permission.DASHBOARD)
        );

        User user = userMapper.toEntity(request, "encodedPassword");

        assertNotNull(user);
        assertEquals("Jane", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("jane.doe@example.com", user.getEmail());
        assertEquals("1234567890", user.getPhone());
        assertEquals(Role.SCHOOL_ADMIN, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("Springfield High", user.getSchool());
        assertEquals("North", user.getRegion());
        assertEquals(Set.of(Permission.DASHBOARD), user.getPermissions());
        assertEquals("encodedPassword", user.getPassword());
    }

    @Test
    void testToEntity_fromUserRequest_withNullStatusAndPermissions_defaultsProperly() {
        UserRequest request = new UserRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                null,
                "1234567890",
                Role.USER,
                null,
                null,
                null,
                null
        );

        User user = userMapper.toEntity(request, null);

        assertNotNull(user);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.getPermissions().isEmpty());
        assertNull(user.getPassword());
    }

    @Test
    void testToEntity_fromUserRequest_nullReturnsNull() {
        assertNull(userMapper.toEntity((UserRequest) null, "pass"));
    }

    @Test
    void testToEntity_fromRegisterRequest() {
        RegisterRequest request = new RegisterRequest(
                "  Bob  ",
                "  Builder  ",
                "  bob@example.com  ",
                "pass123",
                "  111222333  ",
                Role.USER,
                "  Springfield Elementary  ",
                "  West  "
        );

        User user = userMapper.toEntity(request, "bob@example.com", "encodedPassword", Role.USER);

        assertNotNull(user);
        assertEquals("Bob", user.getFirstName());
        assertEquals("Builder", user.getLastName());
        assertEquals("bob@example.com", user.getEmail());
        assertEquals("encodedPassword", user.getPassword());
        assertEquals("111222333", user.getPhone());
        assertEquals(Role.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("Springfield Elementary", user.getSchool());
        assertEquals("West", user.getRegion());
        assertEquals(0, user.getOtpAttempts());
        assertTrue(user.getPermissions().isEmpty());
    }

    @Test
    void testToEntity_fromRegisterRequest_nullReturnsNull() {
        assertNull(userMapper.toEntity((RegisterRequest) null, "email", "pass", Role.USER));
    }

    @Test
    void testToResponse() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(99L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("5551234")
                .role(Role.CONTENT_ADMIN)
                .status(UserStatus.ACTIVE)
                .school("High School")
                .region("South")
                .lastLogin(now)
                .permissions(Set.of(Permission.REPORTS))
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertNotNull(response);
        assertEquals(99L, response.id());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("John Doe", response.fullName());
        assertEquals("john@example.com", response.email());
        assertEquals("5551234", response.phone());
        assertEquals(Role.CONTENT_ADMIN, response.role());
        assertEquals(UserStatus.ACTIVE, response.status());
        assertEquals("High School", response.school());
        assertEquals("South", response.region());
        assertEquals(now, response.lastLogin());
        assertEquals(Set.of(Permission.REPORTS), response.permissions());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void testToResponse_nullReturnsNull() {
        assertNull(userMapper.toResponse(null));
    }

    @Test
    void testToLoginResponse() {
        User user = User.builder()
                .email("test@example.com")
                .role(Role.CENTRAL_ADMIN)
                .permissions(Set.of(Permission.USER_MANAGEMENT))
                .build();

        LoginResponse response = userMapper.toLoginResponse(user, "jwt.token.here");

        assertNotNull(response);
        assertEquals("jwt.token.here", response.accessToken());
        assertEquals("test@example.com", response.email());
        assertEquals(Role.CENTRAL_ADMIN, response.role());
        assertEquals(Set.of(Permission.USER_MANAGEMENT), response.permissions());
    }

    @Test
    void testToStatsResponse() {
        UserStatsResponse stats = userMapper.toStatsResponse(100, 10, 5, 4, 3, 2, 20, 1);

        assertNotNull(stats);
        assertEquals(100, stats.totalUsers());
        assertEquals(10, stats.newThisMonth());
        assertEquals(5, stats.centralAdmins());
        assertEquals(4, stats.activeCentralAdmins());
        assertEquals(3, stats.contentAdmins());
        assertEquals(2, stats.activeContentAdmins());
        assertEquals(20, stats.schoolAdmins());
        assertEquals(1, stats.inactiveSchoolAdmins());
    }
}
