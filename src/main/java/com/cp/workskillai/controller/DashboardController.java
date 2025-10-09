package com.cp.workskillai.controller;

import com.cp.workskillai.dto.DashboardResponse;
import com.cp.workskillai.models.Student;
import com.cp.workskillai.models.UserProfile;
import com.cp.workskillai.service.DashboardService;
import com.cp.workskillai.service.ProfileService;
import com.cp.workskillai.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProfileService profileService;
    private final StudentService studentService;

    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<DashboardResponse> getDashboardData(@PathVariable String userId) {
        try {
            log.info("Fetching dashboard data for user: {}", userId);
            DashboardResponse dashboardData = dashboardService.getDashboardData(userId);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error fetching dashboard data for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String userId) {
        try {
            log.info("Fetching user profile for: {}", userId);
            
            UserProfile profile = profileService.getProfile(userId);
            Student student = studentService.getStudentById(userId);
            
            // Check if profile is complete based on your criteria
            boolean isProfileComplete = isProfileComplete(profile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", profile.getId());
            response.put("name", profile.getFullName() != null ? profile.getFullName() : student.getFirstName() + " " + student.getLastName());
            response.put("email", profile.getEmail() != null ? profile.getEmail() : student.getEmail());
            response.put("isProfileComplete", isProfileComplete);
            response.put("profile", profile);
            response.put("student", student);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user profile for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isProfileComplete(UserProfile profile) {
        if (profile == null) return false;
        
        // Define your profile completion criteria
        boolean hasBasicInfo = profile.getFullName() != null && !profile.getFullName().trim().isEmpty() &&
                              profile.getEmail() != null && !profile.getEmail().trim().isEmpty();
        
        boolean hasProfessionalInfo = profile.getTitle() != null && !profile.getTitle().trim().isEmpty() &&
                                     profile.getTechnicalSkills() != null && !profile.getTechnicalSkills().isEmpty();
        
        boolean hasExperience = profile.getExperience() != null && !profile.getExperience().isEmpty();
        
        return hasBasicInfo && hasProfessionalInfo && hasExperience;
    }
}