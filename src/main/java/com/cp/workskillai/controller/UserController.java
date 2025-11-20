package com.cp.workskillai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cp.workskillai.models.Student;
import com.cp.workskillai.models.UserSkill;
import com.cp.workskillai.repository.StudentRepository;
import com.cp.workskillai.repository.UserSkillRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final StudentRepository studentRepository;
    private final UserSkillRepository userSkillRepository;

    @GetMapping("/skill-gap-data/{userId}")
    public ResponseEntity<Map<String, Object>> getUserSkillGapData(@PathVariable String userId) {
        try {
            log.info("Fetching skill gap data for user: {}", userId);
            
            // Find student/profile data
            Optional<Student> studentOpt = studentRepository.findById(userId);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Student student = studentOpt.get();
            
            // Find user skills
            List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);
            
            // Prepare response data
            Map<String, Object> response = new HashMap<>();
            
            // Profile data
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("firstName", student.getFirstName());
            profileData.put("lastName", student.getLastName());
            profileData.put("email", student.getEmail());
            profileData.put("currentJobRole", student.getCurrentJobRole());
            profileData.put("yearsOfExperience", student.getYearsOfExperience());
            profileData.put("summary", student.getSummary());
            profileData.put("skills", student.getSkills());
            profileData.put("certifications", student.getCertifications());
            profileData.put("education", student.getEducation());
            profileData.put("experience", student.getExperience());
            profileData.put("totalExperience", student.getYearsOfExperience() + " years");
            
            response.put("profile", profileData);
            
            // Skills data in the format expected by Python service
            List<Map<String, Object>> skillsData = userSkills.stream().map(skill -> {
                Map<String, Object> skillMap = new HashMap<>();
                skillMap.put("name", skill.getName());
                skillMap.put("proficiency", skill.getProficiency());
                skillMap.put("level", skill.getLevel());
                skillMap.put("verified", skill.getVerified());
                skillMap.put("confidence", skill.getConfidenceLevel());
                skillMap.put("experienceMonths", skill.getExperienceMonths());
                skillMap.put("category", skill.getCategory());
                return skillMap;
            }).collect(Collectors.toList());
            
            response.put("skills", skillsData);
            
            log.info("Successfully fetched skill gap data for user: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching skill gap data for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}