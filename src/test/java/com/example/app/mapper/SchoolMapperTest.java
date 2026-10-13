package com.example.app.mapper;

import com.example.app.dto.request.SchoolCreateRequest;
import com.example.app.dto.response.SchoolResponse;
import com.example.app.dto.response.SchoolStatsResponse;
import com.example.app.entity.School;
import com.example.app.entity.User;
import com.example.app.enums.SchoolStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SchoolMapperTest {

    private SchoolMapper schoolMapper;

    @BeforeEach
    void setUp() {
        schoolMapper = new SchoolMapper();
    }

    @Test
    void testToEntity_withValidRequestAndAdmin() {
        User admin = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();

        SchoolCreateRequest request = SchoolCreateRequest.builder()
                .schoolName("Springfield High")
                .schoolCode("SH001")
                .district("District 1")
                .region("North")
                .address("123 Main St")
                .phone("1234567890")
                .status(SchoolStatus.ACTIVE)
                .build();

        School school = schoolMapper.toEntity(request, admin);

        assertNotNull(school);
        assertEquals("Springfield High", school.getSchoolName());
        assertEquals("SH001", school.getSchoolCode());
        assertEquals("District 1", school.getDistrict());
        assertEquals("North", school.getRegion());
        assertEquals("123 Main St", school.getAddress());
        assertEquals("1234567890", school.getPhone());
        assertEquals(SchoolStatus.ACTIVE, school.getStatus());
        assertEquals(admin, school.getAdmin());
    }

    @Test
    void testToEntity_withNullStatus_defaultsToActive() {
        SchoolCreateRequest request = SchoolCreateRequest.builder()
                .schoolName("Springfield High")
                .schoolCode("SH001")
                .district("District 1")
                .region("North")
                .status(null)
                .build();

        School school = schoolMapper.toEntity(request);

        assertNotNull(school);
        assertEquals(SchoolStatus.ACTIVE, school.getStatus());
        assertNull(school.getAdmin());
    }

    @Test
    void testToEntity_withNullRequest_returnsNull() {
        assertNull(schoolMapper.toEntity(null));
        assertNull(schoolMapper.toEntity(null, null));
    }

    @Test
    void testToResponse_withValidSchool() {
        User admin = User.builder()
                .id(10L)
                .firstName("Alice")
                .lastName("Smith")
                .build();

        LocalDateTime now = LocalDateTime.now();

        School school = School.builder()
                .id(5L)
                .schoolName("Central Academy")
                .schoolCode("CA100")
                .district("Metro")
                .region("Central")
                .address("456 Elm St")
                .phone("9876543210")
                .admin(admin)
                .status(SchoolStatus.ACTIVE)
                .studentsCount(250)
                .teachersCount(20)
                .syncRate(98.5)
                .createdAt(now)
                .updatedAt(now)
                .build();

        SchoolResponse response = schoolMapper.toResponse(school);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Central Academy", response.getSchoolName());
        assertEquals("CA100", response.getSchoolCode());
        assertEquals("Metro", response.getDistrict());
        assertEquals("Central", response.getRegion());
        assertEquals("456 Elm St", response.getAddress());
        assertEquals("9876543210", response.getPhone());
        assertEquals(10L, response.getAdminId());
        assertEquals("Alice Smith", response.getAdminName());
        assertEquals(SchoolStatus.ACTIVE, response.getStatus());
        assertEquals(250, response.getStudentsCount());
        assertEquals(20, response.getTeachersCount());
        assertEquals(98.5, response.getSyncRate());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    void testToResponse_withNullAdmin_setsAdminFieldsNull() {
        School school = School.builder()
                .id(5L)
                .schoolName("Central Academy")
                .admin(null)
                .build();

        SchoolResponse response = schoolMapper.toResponse(school);

        assertNotNull(response);
        assertNull(response.getAdminId());
        assertNull(response.getAdminName());
    }

    @Test
    void testToResponse_withNullSchool_returnsNull() {
        assertNull(schoolMapper.toResponse(null));
    }

    @Test
    void testToStatsResponse() {
        SchoolStatsResponse stats = schoolMapper.toStatsResponse(10, 8, 2, 85.5);

        assertNotNull(stats);
        assertEquals(10, stats.getTotalSchools());
        assertEquals(8, stats.getActiveSchools());
        assertEquals(2, stats.getOfflineSchools());
        assertEquals(85.5, stats.getAvgSyncRate());
    }

    @Test
    void testToStatsResponse_withNullAvgSync_defaultsToZero() {
        SchoolStatsResponse stats = schoolMapper.toStatsResponse(10, 8, 2, null);

        assertNotNull(stats);
        assertEquals(0.0, stats.getAvgSyncRate());
    }
}
