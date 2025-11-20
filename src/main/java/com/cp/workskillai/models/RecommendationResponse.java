package com.cp.workskillai.models;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private List<Map<String, Object>> missingSkills;
    private List<CourseRecommendation> courseRecommendations;
    private List<LearningPathStep> learningPathway;
    private List<String> insights;
    private Double progressPercentage;
    
    // Add these new fields
    private String currentJobRole;
    private String targetJobRole;
}