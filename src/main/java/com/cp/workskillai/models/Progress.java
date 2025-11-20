package com.cp.workskillai.models;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "progress")
public class Progress {

    @Id
    private String id;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Training ID is required")
    private String trainingId;

    @Min(value = 0, message = "Completion percentage cannot be negative")
    @Max(value = 100, message = "Completion percentage cannot exceed 100")
    private int completionPercentage;

    @NotBlank(message = "Status is required")
    private String status;  // "Not Started", "In Progress", "Completed"
}
