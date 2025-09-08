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
@Document(collection = "courses")
public class Courses {

    @Id
    private String id;

    @NotBlank(message = "Course title is required")
    private String title;

    @NotBlank(message = "Platform is required")
    private String platform; // "Coursera", "Udemy", "LinkedIn Learning"

    @NotBlank(message = "Course URL is required")
    @Pattern(regexp = "^(http|https)://.*$", message = "Invalid course URL")
    private String url;

    private String difficultyLevel; // Beginner / Intermediate / Advanced

    private List<String> skillsCovered; // ["AI", "Cloud", "Java"]
}
