// SkillGapAnalysisController.java
package com.cp.workskillai.controller;

import com.cp.workskillai.dto.SkillGapAnalysisRequest;
import com.cp.workskillai.dto.SkillGapAnalysisResponse;
import com.cp.workskillai.service.SkillGapAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor

public class SkillGapAnalysisController {
    
    private final SkillGapAnalysisService skillGapAnalysisService;
    
    @PostMapping("/skill-gap/{userId}")
    public ResponseEntity<SkillGapAnalysisResponse> analyzeSkillGap(
            @PathVariable String userId,
            @RequestBody SkillGapAnalysisRequest request) {  // Remove @RequestParam
        try {
          
            
            request.setUserId(userId);
            SkillGapAnalysisResponse response = skillGapAnalysisService.analyzeSkillGap(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/skill-gap")
    public ResponseEntity<SkillGapAnalysisResponse> analyzeCurrentUserSkillGap(
            @RequestParam String jobRole,
            Principal principal) {
        
        try {
            // Get current user ID from security context
            String currentUserId = principal.getName(); // This should be the user ID
            
            SkillGapAnalysisRequest request = new SkillGapAnalysisRequest();
            request.setUserId(currentUserId);
            request.setJobRole(jobRole);
            
            SkillGapAnalysisResponse response = skillGapAnalysisService.analyzeSkillGap(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}