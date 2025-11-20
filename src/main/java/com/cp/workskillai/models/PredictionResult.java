package com.cp.workskillai.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "predictions")
public class PredictionResult {

    @Id
    private String id;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotEmpty(message = "Predicted skill gaps cannot be empty")
    private List<@NotBlank String> predictedSkillGaps;

    private List<String> recommendedTrainings;  // Training IDs
}
