// PythonServiceClient.java
package com.cp.workskillai.service;

import com.cp.workskillai.dto.SkillGapAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonServiceClient {
    
    private final RestTemplate restTemplate;
    private final String PYTHON_SERVICE_URL = "http://localhost:8000";
    
    public SkillGapAnalysisResponse analyzeSkillGap(Map<String, Object> userData, String jobRole) {
        try {
            // Prepare request for Python service
            Map<String, Object> requestBody = Map.of(
                "user_id", userData.get("userId"),
                "job_role", jobRole,
                "profile_data", userData.get("profile"),
                "skills_data", userData.get("skills")
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("Calling Python service for skill gap analysis...");
            
            ResponseEntity<SkillGapAnalysisResponse> response = restTemplate.exchange(
                PYTHON_SERVICE_URL + "/internal-analyze",
                HttpMethod.POST,
                requestEntity,
                SkillGapAnalysisResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully received analysis from Python service");
                return response.getBody();
            } else {
                throw new RuntimeException("Python service returned error: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error calling Python service: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze skill gap: " + e.getMessage());
        }
    }
}