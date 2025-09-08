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
@Document(collection = "job_roles")
public class JobRole {

    @Id
    private String id;

    @NotBlank(message = "Role name is required")
    private String roleName;  // e.g., "Data Scientist"

    @NotEmpty(message = "Required skills list cannot be empty")
    private List<@NotBlank String> requiredSkills; // ["Python", "ML", "Statistics"]
}
