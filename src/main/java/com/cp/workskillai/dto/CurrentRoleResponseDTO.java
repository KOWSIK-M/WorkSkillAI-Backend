package com.cp.workskillai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentRoleResponseDTO {
    private String userId;
    private String currentRole;
    private Double matchScore;
    private Boolean hasAnalysis;
    private LocalDateTime analyzedAt;
    
    // Add all the missing fields that your service is trying to set
    private List<SkillAnalysis> requiredSkills;
    private List<UserSkillAnalysis> currentSkills;
    private List<SkillAnalysis> missingSkills;
    private List<SkillAnalysis> partialMatchSkills;
    private Map<String, Object> gapAnalysis;
    private List<String> recommendations;
    private String timeToCloseGap;
    private String salaryImpact;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapAnalysisDTO {
        private Integer totalSkillsRequired;
        private Integer skillsMatched;
        private Integer skillsPartial;
        private Integer skillsMissing;
        private Double averageGap;
        private Double totalImportanceScore;
        private List<String> topMissingSkills;
        private List<String> topMatchedSkills;
    }
}