// UserDataService.java
package com.cp.workskillai.service;

import com.cp.workskillai.models.UserProfile;
import com.cp.workskillai.models.UserSkill;
import com.cp.workskillai.repository.UserProfileRepository;
import com.cp.workskillai.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDataService {
    
    private final UserProfileRepository userProfileRepository;
    private final UserSkillRepository userSkillRepository;
    
    public Map<String, Object> getUserSkillGapData(String userId) {
        Map<String, Object> response = new HashMap<>();
        
        // Get user profile
        Optional<UserProfile> userProfileOpt = userProfileRepository.findByUserId(userId);
        if (userProfileOpt.isEmpty()) {
            throw new RuntimeException("User profile not found for ID: " + userId);
        }
        
        UserProfile userProfile = userProfileOpt.get();
        
        // Get user skills
        List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);
        
        // Prepare profile data
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("summary", userProfile.getSummary());
        profileData.put("certifications", userProfile.getCertifications());
        profileData.put("education", userProfile.getEducation());
        profileData.put("experience", userProfile.getExperience());
        profileData.put("totalExperience", userProfile.getTotalExperience());
        
        // Prepare skills data
        List<Map<String, Object>> skillsData = userSkills.stream()
            .map(this::convertToSkillMap)
            .toList();
        
        response.put("profile", profileData);
        response.put("skills", skillsData);
        response.put("userId", userId);
        
        return response;
    }
    
    private Map<String, Object> convertToSkillMap(UserSkill userSkill) {
        Map<String, Object> skillMap = new HashMap<>();
        skillMap.put("name", userSkill.getName());
        skillMap.put("proficiency", userSkill.getProficiency());
        skillMap.put("level", userSkill.getLevel());
        skillMap.put("confidenceLevel", userSkill.getConfidenceLevel());
        skillMap.put("verified", userSkill.getVerified());
        skillMap.put("experienceMonths", userSkill.getExperienceMonths());
        skillMap.put("category", userSkill.getCategory());
        return skillMap;
    }
}