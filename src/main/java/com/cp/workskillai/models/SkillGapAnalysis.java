// SkillGapAnalysis.java (MongoDB Document)
package com.cp.workskillai.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "skill_gap_analyses")
@Data
public class SkillGapAnalysis {
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Field("job_role")
    private String jobRole;
    
    @Field("match_score")
    private Double matchScore;
    
    @Field("required_skills")
    private List<Map<String, Object>> requiredSkills;
    
    @Field("current_skills")
    private List<Map<String, Object>> currentSkills;
    
    @Field("missing_skills")
    private List<Map<String, Object>> missingSkills;
    
    @Field("partial_match_skills")
    private List<Map<String, Object>> partialMatchSkills;
    
    @Field("gap_analysis")
    private Map<String, Object> gapAnalysis;
    
    private List<String> recommendations;
    
    @Field("time_to_close_gap")
    private String timeToCloseGap;
    
    @Field("salary_impact")
    private String salaryImpact;
    
    @Field("is_current_role")
    private Boolean isCurrentRole = false;
    
    @Field("analyzed_at")
    private LocalDateTime analyzedAt;
}