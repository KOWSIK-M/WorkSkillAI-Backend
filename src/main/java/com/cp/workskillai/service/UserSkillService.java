package com.cp.workskillai.service;

import com.cp.workskillai.models.UserSkill;
import com.cp.workskillai.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSkillService {
    
    private final UserSkillRepository userSkillRepository;
    private final GeminiExamService geminiExamService;
    
    public List<UserSkill> getUserSkills(String userId) {
        return userSkillRepository.findByUserId(userId);
    }
    
    public UserSkill saveUserSkill(UserSkill userSkill) {
        if (userSkill.getId() == null) {
            userSkill.setCreatedAt(LocalDateTime.now());
        }
        userSkill.setUpdatedAt(LocalDateTime.now());
        return userSkillRepository.save(userSkill);
    }
    
    public UserSkill updateSkillProficiency(String skillId, Integer score, String status) {
        Optional<UserSkill> optionalSkill = userSkillRepository.findById(skillId);
        if (optionalSkill.isPresent()) {
            UserSkill skill = optionalSkill.get();
            skill.setScore(score);
            skill.setProficiency(score);
            skill.setStatus(status);
            skill.setVerified("verified".equals(status));
            skill.setLastVerified(LocalDateTime.now());
            
            // Determine level based on score
            if (score >= 80) skill.setLevel("Expert");
            else if (score >= 60) skill.setLevel("Advanced");
            else if (score >= 40) skill.setLevel("Intermediate");
            else skill.setLevel("Beginner");
            
            return userSkillRepository.save(skill);
        }
        return null;
    }
    
    public void syncSkillsFromProfile(String userId, List<String> technicalSkills) {
        List<UserSkill> existingSkills = getUserSkills(userId);
        
        for (String skillName : technicalSkills) {
            boolean exists = existingSkills.stream()
                .anyMatch(skill -> skill.getName().equalsIgnoreCase(skillName));
            
            if (!exists) {
                UserSkill newSkill = UserSkill.builder()
                    .userId(userId)
                    .name(skillName)
                    .category(determineCategory(skillName))
                    .proficiency(0)
                    .score(0)
                    .status("pending")
                    .level("Pending")
                    .verified(false)
                    .createdAt(LocalDateTime.now())
                    .build();
                
                userSkillRepository.save(newSkill);
            }
        }
    }
    
    private String determineCategory(String skillName) {
        // Simple categorization logic
        String lowerSkill = skillName.toLowerCase();
        if (lowerSkill.contains("react") || lowerSkill.contains("angular") || lowerSkill.contains("vue")) {
            return "Frontend";
        } else if (lowerSkill.contains("node") || lowerSkill.contains("spring") || lowerSkill.contains("django")) {
            return "Backend";
        } else if (lowerSkill.contains("aws") || lowerSkill.contains("azure") || lowerSkill.contains("gcp")) {
            return "Cloud";
        } else if (lowerSkill.contains("python") || lowerSkill.contains("java") || lowerSkill.contains("javascript")) {
            return "Programming";
        } else {
            return "Other";
        }
    }
}