package com.cp.workskillai.models;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathStep {
    private Integer step;
    private String title;
    private String description;
    private String duration;
    private List<String> skills;
    private String status;
    private List<String> courses;
}
