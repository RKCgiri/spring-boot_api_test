package com.example.app.dto.request;

import com.example.app.enums.SchoolStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolCreateRequest {
    @NotBlank(message = "School name is required")
    private String schoolName;

    @NotBlank(message = "School code is required")
    private String schoolCode;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Region is required")
    private String region;

    private String address;

    private String phone;

    private Long adminId;

    private SchoolStatus status;
}
