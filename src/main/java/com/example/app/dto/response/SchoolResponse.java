package com.example.app.dto.response;

import com.example.app.enums.SchoolStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolResponse {
    private Long id;
    private String schoolName;
    private String schoolCode;
    private String district;
    private String region;
    private String address;
    private String phone;
    private Long adminId;
    private String adminName;
    private SchoolStatus status;
    private Integer studentsCount;
    private Integer teachersCount;
    private Double syncRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
