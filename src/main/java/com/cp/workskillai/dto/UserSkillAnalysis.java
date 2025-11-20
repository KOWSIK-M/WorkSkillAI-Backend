package com.cp.workskillai.dto;

import lombok.Data;

@Data
public class UserSkillAnalysis {
    private String name;
    private Integer proficiency;
    private String level;
    private Boolean verified;
    private String confidence;
}
