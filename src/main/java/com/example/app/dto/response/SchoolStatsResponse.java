package com.example.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolStatsResponse {
    private long totalSchools;
    private long activeSchools;
    private long offlineSchools;
    private double avgSyncRate;
}
