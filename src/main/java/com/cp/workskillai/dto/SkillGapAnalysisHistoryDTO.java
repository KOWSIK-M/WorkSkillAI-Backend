// SkillGapAnalysisHistoryDTO.java
package com.cp.workskillai.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SkillGapAnalysisHistoryDTO {
    private String id;
    private String jobRole;
    private Double matchScore;
    private List<SkillAnalysis> requiredSkills;
    private List<UserSkillAnalysis> currentSkills;
    private List<SkillAnalysis> missingSkills;
    private List<SkillAnalysis> partialMatchSkills;
    private Map<String, Object> gapAnalysis;
    private List<String> recommendations;
    private String timeToCloseGap;
    private String salaryImpact;
    private Boolean isCurrentRole;
    private LocalDateTime analyzedAt;
}