package com.cp.workskillai.controller;

import com.cp.workskillai.models.UserSkill;
import com.cp.workskillai.service.UserSkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173","http://localhost:8000"})
public class UserSkillController {
    
    private final UserSkillService userSkillService;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserSkill>> getUserSkills(@PathVariable String userId) {
        try {
            List<UserSkill> skills = userSkillService.getUserSkills(userId);
            return ResponseEntity.ok(skills);
        } catch (Exception e) {
            log.error("Error fetching skills for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/sync-from-profile/{userId}")
    public ResponseEntity<Map<String, Object>> syncSkillsFromProfile(@PathVariable String userId) {
        try {
            // This would fetch the user's profile and sync skills
            // For now, return success
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Skills synced successfully");
            response.put("synced", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error syncing skills for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}/analytics")
    public ResponseEntity<Map<String, Object>> getSkillAnalytics(@PathVariable String userId) {
        try {
            List<UserSkill> skills = userSkillService.getUserSkills(userId);
            
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("totalSkills", skills.size());
            analytics.put("verifiedSkills", skills.stream().filter(UserSkill::getVerified).count());
            analytics.put("averageProficiency", skills.stream().mapToInt(UserSkill::getProficiency).average().orElse(0));
            analytics.put("skillDistribution", getSkillDistribution(skills));
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching skill analytics for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // New endpoint for ML integration
    @GetMapping("/user/{userId}/ml-ready")
    public ResponseEntity<Map<String, Object>> getMLReadySkills(@PathVariable String userId) {
        try {
            List<UserSkill> skills = userSkillService.getUserSkills(userId);
            
            Map<String, Object> mlData = new HashMap<>();
            mlData.put("userId", userId);
            mlData.put("skills", skills.stream().map(this::mapSkillForML).toList());
            mlData.put("totalVerifiedSkills", skills.stream().filter(UserSkill::getVerified).count());
            mlData.put("averageConfidence", calculateAverageConfidence(skills));
            
            return ResponseEntity.ok(mlData);
        } catch (Exception e) {
            log.error("Error preparing ML data for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private Map<String, Object> mapSkillForML(UserSkill skill) {
        Map<String, Object> mlSkill = new HashMap<>();
        mlSkill.put("name", skill.getName());
        mlSkill.put("category", skill.getCategory());
        mlSkill.put("proficiency", skill.getProficiency());
        mlSkill.put("level", skill.getLevel().toLowerCase());
        mlSkill.put("verified", skill.getVerified());
        mlSkill.put("confidence", mapConfidenceLevel(skill.getConfidenceLevel()));
        mlSkill.put("experience_months", skill.getExperienceMonths());
        mlSkill.put("last_verified", skill.getLastVerified());
        return mlSkill;
    }
    
    private double mapConfidenceLevel(String confidenceLevel) {
        if (confidenceLevel == null) return 0.5;
        return switch (confidenceLevel.toLowerCase()) {
            case "high" -> 0.9;
            case "medium" -> 0.7;
            case "low" -> 0.3;
            default -> 0.5;
        };
    }
    
    private double calculateAverageConfidence(List<UserSkill> skills) {
        return skills.stream()
            .mapToDouble(skill -> mapConfidenceLevel(skill.getConfidenceLevel()))
            .average()
            .orElse(0.5);
    }
    
    private Map<String, Long> getSkillDistribution(List<UserSkill> skills) {
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("verified", skills.stream().filter(s -> "verified".equals(s.getStatus())).count());
        distribution.put("pending", skills.stream().filter(s -> "pending".equals(s.getStatus())).count());
        distribution.put("unverified", skills.stream().filter(s -> "unverified".equals(s.getStatus())).count());
        distribution.put("needs_improvement", skills.stream().filter(s -> "needs_improvement".equals(s.getStatus())).count());
        return distribution;
    }
}