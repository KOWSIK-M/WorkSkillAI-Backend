package com.cp.workskillai.models;

import lombok.*;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "skills")
public class Skill {

    @Id
    private String id;

    @NotBlank(message = "Skill name is required")
    private String name;  // e.g., "Python", "Spring Boot"

    @NotBlank(message = "Category is required")
    private String category;  // e.g., "Programming", "Cloud", "Framework", "Database"

    @Min(value = 1, message = "Proficiency must be at least 1")
    @Max(value = 5, message = "Proficiency cannot exceed 5")
    private int proficiencyLevel;  // Scale 1–5

    private String description; // Brief description of the skill
    
    private List<String> relatedSkills; // ["Java", "JPA", "Spring Security"]
    
    private String difficultyLevel; // "Beginner", "Intermediate", "Advanced"
    
    private Integer demandScore; // Market demand score 1-100
    
    private List<String> commonCertifications; // Related certifications
    
    // Timestamps
    private String createdAt;
    private String updatedAt;
}