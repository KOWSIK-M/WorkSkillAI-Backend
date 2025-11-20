package com.cp.workskillai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Map<String, Object> stats;
    private List<Map<String, Object>> skills;
    private List<Map<String, Object>> progress;
    private List<Map<String, Object>> roleMatches;
    private List<Map<String, Object>> recentActivities;
}