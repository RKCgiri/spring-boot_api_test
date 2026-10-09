package com.example.app.service;

import com.example.app.dto.request.SchoolCreateRequest;
import com.example.app.dto.response.SchoolResponse;
import com.example.app.dto.response.SchoolStatsResponse;
import com.example.app.enums.SchoolStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SchoolService {
    
    SchoolResponse createSchool(SchoolCreateRequest request);
    
    SchoolStatsResponse getSchoolStats();
    
    Page<SchoolResponse> searchSchools(String name, String region, SchoolStatus status, Pageable pageable);
    
    String exportSchools(String name, String region, SchoolStatus status);
}
