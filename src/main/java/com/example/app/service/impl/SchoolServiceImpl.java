package com.example.app.service.impl;

import com.example.app.dto.request.SchoolCreateRequest;
import com.example.app.dto.response.SchoolResponse;
import com.example.app.dto.response.SchoolStatsResponse;
import com.example.app.entity.School;
import com.example.app.entity.User;
import com.example.app.enums.SchoolStatus;
import com.example.app.repository.SchoolRepository;
import com.example.app.repository.UserRepository;
import com.example.app.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SchoolResponse createSchool(SchoolCreateRequest request) {
        if (schoolRepository.findBySchoolCode(request.getSchoolCode()).isPresent()) {
            throw new IllegalArgumentException("School with code " + request.getSchoolCode() + " already exists");
        }

        User admin = null;
        if (request.getAdminId() != null) {
            admin = userRepository.findById(request.getAdminId())
                    .orElseThrow(() -> new IllegalArgumentException("Admin user not found with id: " + request.getAdminId()));
        }

        School school = School.builder()
                .schoolName(request.getSchoolName())
                .schoolCode(request.getSchoolCode())
                .district(request.getDistrict())
                .region(request.getRegion())
                .address(request.getAddress())
                .phone(request.getPhone())
                .admin(admin)
                .status(request.getStatus() != null ? request.getStatus() : SchoolStatus.ACTIVE)
                .build();

        School savedSchool = schoolRepository.save(school);
        return mapToResponse(savedSchool);
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolStatsResponse getSchoolStats() {
        long total = schoolRepository.count();
        long active = schoolRepository.countByStatus(SchoolStatus.ACTIVE);
        long offline = schoolRepository.countByStatus(SchoolStatus.OFFLINE);
        Double avgSync = schoolRepository.getAverageSyncRate();

        return SchoolStatsResponse.builder()
                .totalSchools(total)
                .activeSchools(active)
                .offlineSchools(offline)
                .avgSyncRate(avgSync != null ? avgSync : 0.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SchoolResponse> searchSchools(String name, String region, SchoolStatus status, Pageable pageable) {
        Page<School> schools = schoolRepository.searchSchools(name, region, status, pageable);
        return schools.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportSchools(String name, String region, SchoolStatus status) {
        List<School> schools = schoolRepository.searchSchoolsForExport(name, region, status);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        // CSV Header
        pw.println("School Name,School Code,District,Region,Students,Teachers,Sync Rate,Status");
        
        for (School school : schools) {
            pw.printf("%s,%s,%s,%s,%d,%d,%.2f,%s%n",
                    escapeCsv(school.getSchoolName()),
                    escapeCsv(school.getSchoolCode()),
                    escapeCsv(school.getDistrict()),
                    escapeCsv(school.getRegion()),
                    school.getStudentsCount(),
                    school.getTeachersCount(),
                    school.getSyncRate(),
                    school.getStatus());
        }
        
        return sw.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private SchoolResponse mapToResponse(School school) {
        return SchoolResponse.builder()
                .id(school.getId())
                .schoolName(school.getSchoolName())
                .schoolCode(school.getSchoolCode())
                .district(school.getDistrict())
                .region(school.getRegion())
                .address(school.getAddress())
                .phone(school.getPhone())
                .adminId(school.getAdmin() != null ? school.getAdmin().getId() : null)
                .adminName(school.getAdmin() != null ? school.getAdmin().getFullName() : null)
                .status(school.getStatus())
                .studentsCount(school.getStudentsCount())
                .teachersCount(school.getTeachersCount())
                .syncRate(school.getSyncRate())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}
