package com.cp.workskillai.models;

import lombok.*;
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
    private String name;  // e.g., "Python"

    @NotBlank(message = "Category is required")
    private String category;  // e.g., "Programming", "Cloud"

    @Min(value = 1, message = "Proficiency must be at least 1")
    @Max(value = 5, message = "Proficiency cannot exceed 5")
    private int proficiencyLevel;  // Scale 1–5
}
