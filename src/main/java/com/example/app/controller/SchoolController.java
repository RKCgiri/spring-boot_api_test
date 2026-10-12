package com.example.app.controller;

import com.example.app.dto.request.SchoolCreateRequest;
import com.example.app.dto.response.SchoolResponse;
import com.example.app.dto.response.SchoolStatsResponse;
import com.example.app.enums.SchoolStatus;
import com.example.app.service.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @PostMapping
    @PreAuthorize("hasRole('CENTRAL_ADMIN')")
    public ResponseEntity<SchoolResponse> createSchool(@Valid @RequestBody SchoolCreateRequest request) {
        SchoolResponse response = schoolService.createSchool(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('CENTRAL_ADMIN', 'CONTENT_ADMIN')")
    public ResponseEntity<SchoolStatsResponse> getSchoolStats() {
        return ResponseEntity.ok(schoolService.getSchoolStats());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CENTRAL_ADMIN', 'CONTENT_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<Page<SchoolResponse>> searchSchools(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) SchoolStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(schoolService.searchSchools(name, region, status, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('CENTRAL_ADMIN')")
    public ResponseEntity<byte[]> exportSchools(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) SchoolStatus status) {
        
        String csvData = schoolService.exportSchools(name, region, status);
        byte[] data = csvData.getBytes(StandardCharsets.UTF_8);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schools.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
