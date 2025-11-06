package com.cp.workskillai.dto;

import lombok.Data;

@Data
public class SkillAnalysis {
    private String name;
    private Double importance;
    private Integer requiredProficiency;
    private String category;
    private Double probability;
    private Integer userProficiency;
    private Integer gap;
    private String status;
    private String userConfidence;
}