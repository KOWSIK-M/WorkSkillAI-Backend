package com.cp.workskillai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResponse {
    private boolean success;
    private String message;
    private Map<String, Object> extractedData;
    private String resumeId;
    private String profileId;
    private Double confidenceScore;
}