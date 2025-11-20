package com.cp.workskillai.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SkillGapAnalysisRequest {
    @NotBlank(message = "Job role is required")
    private String jobRole;
    
    private String userId;
}