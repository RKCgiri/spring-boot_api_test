package com.example.app.service.impl;

import com.example.app.dto.request.SchoolCreateRequest;
import com.example.app.dto.response.SchoolResponse;
import com.example.app.dto.response.SchoolStatsResponse;
import com.example.app.entity.School;
import com.example.app.entity.User;
import com.example.app.enums.SchoolStatus;
import com.example.app.mapper.SchoolMapper;
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
    private final SchoolMapper schoolMapper;

    @Override
    @Transactional
    public SchoolResponse createSchool(SchoolCreateRequest request) {

        if (schoolRepository.findBySchoolCode(request.getSchoolCode()).isPresent()) {
            throw new IllegalArgumentException(
                    "School with code " + request.getSchoolCode() + " already exists"
            );
        }

        User admin = null;

        if (request.getAdminId() != null) {
            admin = userRepository.findById(request.getAdminId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Admin user not found with id: " + request.getAdminId()
                    ));
        }

        School school = schoolMapper.toEntity(request, admin);
        School savedSchool = schoolRepository.save(school);

        return schoolMapper.toResponse(savedSchool);
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolStatsResponse getSchoolStats() {

        long total = schoolRepository.count();
        long active = schoolRepository.countByStatus(SchoolStatus.ACTIVE);
        long offline = schoolRepository.countByStatus(SchoolStatus.OFFLINE);

        Double avgSync = schoolRepository.getAverageSyncRate();

        return schoolMapper.toStatsResponse(total, active, offline, avgSync);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SchoolResponse> searchSchools(
            String name,
            String region,
            SchoolStatus status,
            Pageable pageable) {

        // Convert null or blank school names to an empty string.
        String normalizedName = normalizeSchoolName(name);

        Page<School> schools = schoolRepository.searchSchools(
                normalizedName,
                region,
                status,
                pageable
        );

        return schools.map(schoolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportSchools(
            String name,
            String region,
            SchoolStatus status) {

        // Use the same normalization for CSV export.
        String normalizedName = normalizeSchoolName(name);

        List<School> schools = schoolRepository.searchSchoolsForExport(
                normalizedName,
                region,
                status
        );

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // CSV Header
        pw.println(
                "School Name,School Code,District,Region,Students,Teachers,Sync Rate,Status"
        );

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

        pw.flush();
        return sw.toString();
    }

    private String normalizeSchoolName(String name) {
        return name == null ? "" : name.trim();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r")) {

            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
