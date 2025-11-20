package com.cp.workskillai.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_skills")
public class UserSkill {
    
    @Id
    private String id;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Skill name is required")
    private String name;
    
    private String category;
    private Integer proficiency; // 0-100
    private String level; // Beginner, Intermediate, Advanced, Expert
    private String status; // pending, verified, unverified, needs_improvement
    private Integer score; // Exam score 0-100
    private Boolean verified;
    private LocalDateTime lastVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // External exam integration
    private String externalExamId;
    private String externalExamProvider; // "hackerrank", "leetcode", "custom"
    private String examUrl;
    
    // Skill metadata
    private Integer experienceMonths;
    private List<String> projects;
    private String confidenceLevel; // low, medium, high
}