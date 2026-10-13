package com.example.app.mapper;

import com.example.app.dto.request.SchoolCreateRequest;
import com.example.app.dto.response.SchoolResponse;
import com.example.app.dto.response.SchoolStatsResponse;
import com.example.app.entity.School;
import com.example.app.entity.User;
import com.example.app.enums.SchoolStatus;
import org.springframework.stereotype.Component;

@Component
public class SchoolMapper {

    public School toEntity(SchoolCreateRequest request) {
        return toEntity(request, null);
    }

    public School toEntity(SchoolCreateRequest request, User admin) {
        if (request == null) {
            return null;
        }

        return School.builder()
                .schoolName(request.getSchoolName())
                .schoolCode(request.getSchoolCode())
                .district(request.getDistrict())
                .region(request.getRegion())
                .address(request.getAddress())
                .phone(request.getPhone())
                .admin(admin)
                .status(request.getStatus() != null
                        ? request.getStatus()
                        : SchoolStatus.ACTIVE)
                .build();
    }

    public SchoolResponse toResponse(School school) {
        if (school == null) {
            return null;
        }

        return SchoolResponse.builder()
                .id(school.getId())
                .schoolName(school.getSchoolName())
                .schoolCode(school.getSchoolCode())
                .district(school.getDistrict())
                .region(school.getRegion())
                .address(school.getAddress())
                .phone(school.getPhone())
                .adminId(school.getAdmin() != null
                        ? school.getAdmin().getId()
                        : null)
                .adminName(school.getAdmin() != null
                        ? school.getAdmin().getFullName()
                        : null)
                .status(school.getStatus())
                .studentsCount(school.getStudentsCount())
                .teachersCount(school.getTeachersCount())
                .syncRate(school.getSyncRate())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }

    public SchoolStatsResponse toStatsResponse(long total, long active, long offline, Double avgSyncRate) {
        return SchoolStatsResponse.builder()
                .totalSchools(total)
                .activeSchools(active)
                .offlineSchools(offline)
                .avgSyncRate(avgSyncRate != null ? avgSyncRate : 0.0)
                .build();
    }
}
