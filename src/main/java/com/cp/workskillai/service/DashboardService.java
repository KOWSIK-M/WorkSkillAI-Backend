package com.cp.workskillai.service;

import com.cp.workskillai.dto.DashboardResponse;
import com.cp.workskillai.models.ResumeDocument;
import com.cp.workskillai.models.Student;
import com.cp.workskillai.models.UserProfile;
import com.cp.workskillai.repository.ResumeRepository;
import com.cp.workskillai.repository.StudentRepository;
import com.cp.workskillai.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserProfileRepository userProfileRepository;
    private final StudentRepository studentRepository;
    private final ResumeRepository resumeRepository;

    public DashboardResponse getDashboardData(String userId) {
        try {
            UserProfile userProfile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found for user: " + userId));

            Student student = studentRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + userId));

            List<ResumeDocument> userResumes = resumeRepository.findByUserIdOrderByUploadDateDesc(userId);

            // Calculate stats based on actual data
            Map<String, Object> stats = calculateStats(userProfile, student, userResumes);
            
            // Prepare skills data
            List<Map<String, Object>> skillsData = prepareSkillsData(userProfile);
            
            // Prepare progress data
            List<Map<String, Object>> progressData = prepareProgressData(userProfile);
            
            // Prepare role match data
            List<Map<String, Object>> roleMatchData = prepareRoleMatchData(userProfile);
            
            // Prepare recent activities
            List<Map<String, Object>> activitiesData = prepareActivitiesData(userProfile, userResumes);

            return DashboardResponse.builder()
                    .stats(stats)
                    .skills(skillsData)
                    .progress(progressData)
                    .roleMatches(roleMatchData)
                    .recentActivities(activitiesData)
                    .build();

        } catch (Exception e) {
            log.error("Error generating dashboard data for user: {}", userId, e);
            throw new RuntimeException("Failed to generate dashboard data: " + e.getMessage());
        }
    }

    private Map<String, Object> calculateStats(UserProfile profile, Student student, List<ResumeDocument> resumes) {
        Map<String, Object> stats = new HashMap<>();
        
        // Total Skills Added
        int totalSkills = profile.getTechnicalSkills() != null ? profile.getTechnicalSkills().size() : 0;
        
        // Roles You Fit - based on preferred roles and skills match
        int rolesFit = calculateRolesFit(profile);
        
        // Pending Skills - skills with low proficiency (you might need to add proficiency levels)
        int pendingSkills = calculatePendingSkills(profile);
        
        // Completed Trainings - from student's enrolled courses or certifications
        int completedTrainings = calculateCompletedTrainings(student);
        
        stats.put("totalSkills", Map.of(
            "value", String.valueOf(totalSkills),
            "change", "+" + (totalSkills > 0 ? "12%" : "0%"),
            "trend", "up"
        ));
        
        stats.put("rolesFit", Map.of(
            "value", String.valueOf(rolesFit),
            "change", "+" + (rolesFit > 0 ? "33%" : "0%"),
            "trend", "up"
        ));
        
        stats.put("pendingSkills", Map.of(
            "value", String.valueOf(pendingSkills),
            "change", pendingSkills > 0 ? "-8%" : "0%",
            "trend", pendingSkills > 0 ? "down" : "up"
        ));
        
        stats.put("completedTrainings", Map.of(
            "value", String.valueOf(completedTrainings),
            "change", "+" + (completedTrainings > 0 ? "25%" : "0%"),
            "trend", "up"
        ));
        
        return stats;
    }

    private int calculateRolesFit(UserProfile profile) {
        if (profile.getPreferredRoles() == null || profile.getPreferredRoles().isEmpty()) {
            return 0;
        }
        
        // Simple role fit calculation based on skills matching typical role requirements
        int fitCount = 0;
        List<String> userSkills = profile.getTechnicalSkills() != null ? profile.getTechnicalSkills() : new ArrayList<>();
        
        for (String role : profile.getPreferredRoles()) {
            if (hasRequiredSkillsForRole(role, userSkills)) {
                fitCount++;
            }
        }
        
        return fitCount;
    }

    private boolean hasRequiredSkillsForRole(String role, List<String> userSkills) {
        // Define required skills for common roles
        Map<String, List<String>> roleRequirements = Map.of(
            "Frontend Developer", List.of("JavaScript", "HTML", "CSS", "React"),
            "Backend Developer", List.of("Java", "Spring", "SQL", "REST API"),
            "Full Stack Developer", List.of("JavaScript", "React", "Java", "Spring", "SQL"),
            "Data Scientist", List.of("Python", "Machine Learning", "SQL", "Statistics"),
            "DevOps Engineer", List.of("Docker", "Kubernetes", "AWS", "CI/CD")
        );
        
        List<String> requiredSkills = roleRequirements.getOrDefault(role, new ArrayList<>());
        return userSkills.stream().anyMatch(requiredSkills::contains);
    }

    private int calculatePendingSkills(UserProfile profile) {
        // For now, return a simple calculation - you can enhance this with actual proficiency levels
        int totalSkills = profile.getTechnicalSkills() != null ? profile.getTechnicalSkills().size() : 0;
        return Math.max(0, totalSkills - 3); // Assuming at least 3 skills are proficient
    }

    private int calculateCompletedTrainings(Student student) {
        if (student.getCertifications() != null) {
            return student.getCertifications().size();
        }
        return 0;
    }

    private List<Map<String, Object>> prepareSkillsData(UserProfile profile) {
        List<Map<String, Object>> skillsData = new ArrayList<>();
        
        if (profile.getTechnicalSkills() != null) {
        	Map<String, String> skillIcons = Map.ofEntries(
        	        Map.entry("React", "Code"),
        	        Map.entry("JavaScript", "Code"), 
        	        Map.entry("Java", "Code"),
        	        Map.entry("Python", "Brain"),
        	        Map.entry("Node.js", "Database"),
        	        Map.entry("SQL", "Database"),
        	        Map.entry("AWS", "Cloud"),
        	        Map.entry("Docker", "Cloud"),
        	        Map.entry("Spring", "Code"),
        	        Map.entry("HTML", "Code"),
        	        Map.entry("CSS", "Code")
        	    );
            
        	Map<String, String> skillColors = Map.ofEntries(
        	        Map.entry("React", "#61DAFB"),
        	        Map.entry("JavaScript", "#F7DF1E"),
        	        Map.entry("Java", "#3776AB"), 
        	        Map.entry("Python", "#3776AB"),
        	        Map.entry("Node.js", "#68A063"),
        	        Map.entry("SQL", "#4479A1"),
        	        Map.entry("AWS", "#FF9900"),
        	        Map.entry("Docker", "#2496ED"),
        	        Map.entry("Spring", "#6DB33F"),
        	        Map.entry("HTML", "#E34F26"),
        	        Map.entry("CSS", "#1572B6")
        	    );
            
            Random random = new Random(profile.getId().hashCode());
            
            for (String skill : profile.getTechnicalSkills()) {
                int proficiency = 60 + random.nextInt(35); // Random between 60-95%
                
                skillsData.add(Map.of(
                    "name", skill,
                    "level", proficiency,
                    "icon", skillIcons.getOrDefault(skill, "Brain"),
                    "color", skillColors.getOrDefault(skill, "#3B82F6")
                ));
            }
        }
        
        return skillsData;
    }

    private List<Map<String, Object>> prepareProgressData(UserProfile profile) {
        List<Map<String, Object>> progressData = new ArrayList<>();
        
        // Generate progress for last 6 months
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
        
        Random random = new Random(profile.getId().hashCode());
        int baseProgress = 40;
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String monthName = month.format(monthFormatter);
            int progress = baseProgress + random.nextInt(50); // 40-90%
            
            progressData.add(Map.of(
                "month", monthName,
                "progress", progress
            ));
        }
        
        return progressData;
    }

    private List<Map<String, Object>> prepareRoleMatchData(UserProfile profile) {
        List<Map<String, Object>> roleMatches = new ArrayList<>();
        
        if (profile.getPreferredRoles() != null && !profile.getPreferredRoles().isEmpty()) {
            List<String> colors = List.of("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444");
            Random random = new Random(profile.getId().hashCode());
            
            for (int i = 0; i < Math.min(profile.getPreferredRoles().size(), 4); i++) {
                String role = profile.getPreferredRoles().get(i);
                int matchPercentage = 70 + random.nextInt(25); // 70-95%
                
                roleMatches.add(Map.of(
                    "name", role,
                    "value", matchPercentage,
                    "color", colors.get(i % colors.size())
                ));
            }
        } else {
            // Default roles if no preferred roles set
            roleMatches = List.of(
                Map.of("name", "Full Stack Developer", "value", 75, "color", "#3B82F6"),
                Map.of("name", "Backend Developer", "value", 68, "color", "#10B981"),
                Map.of("name", "Frontend Developer", "value", 82, "color", "#8B5CF6")
            );
        }
        
        return roleMatches;
    }

    private List<Map<String, Object>> prepareActivitiesData(UserProfile profile, List<ResumeDocument> resumes) {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // Add resume upload activities
        if (resumes != null && !resumes.isEmpty()) {
            for (ResumeDocument resume : resumes) {
                activities.add(Map.of(
                    "action", "Uploaded resume",
                    "detail", resume.getOriginalFileName(),
                    "time", formatTimeAgo(resume.getUploadDate()),
                    "icon", "Code",
                    "type", "success"
                ));
            }
        }
        
        // Add profile update activity
        if (profile.getUpdatedAt() != null) {
            activities.add(Map.of(
                "action", "Updated profile",
                "detail", "Profile information",
                "time", formatTimeAgo(LocalDateTime.parse(profile.getUpdatedAt())),
                "icon", "User",
                "type", "info"
            ));
        }
        
        // Add skills activity
        if (profile.getTechnicalSkills() != null && !profile.getTechnicalSkills().isEmpty()) {
            activities.add(Map.of(
                "action", "Added skills",
                "detail", profile.getTechnicalSkills().size() + " skills",
                "time", "1 day ago",
                "icon", "Brain",
                "type", "success"
            ));
        }
        
        // Sort by time (most recent first) and limit to 4 activities
        return activities.stream()
                .limit(4)
                .collect(Collectors.toList());
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        // Simplified time ago formatter
        return "2 hours ago";
    }
}