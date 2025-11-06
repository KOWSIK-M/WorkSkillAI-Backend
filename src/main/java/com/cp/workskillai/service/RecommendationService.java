// RecommendationService.java
package com.cp.workskillai.service;

import com.cp.workskillai.models.*;
import com.cp.workskillai.repository.SkillGapAnalysisRepository;
import com.cp.workskillai.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final StudentRepository studentRepository;
    private final RestTemplate restTemplate;
    
    @Value("${python.ml.service.url:http://localhost:8000}")
    private String pythonMlServiceUrl;
    
    public RecommendationResponse generateRecommendations(String userId) {
        try {
            log.info("Generating recommendations for user: {}", userId);
            
            // Get latest skill gap analysis
            Optional<SkillGapAnalysis> latestAnalysis = skillGapAnalysisRepository
                    .findTopByUserIdOrderByAnalyzedAtDesc(userId);
            
            if (latestAnalysis.isEmpty()) {
                log.warn("No skill gap analysis found for user: {}", userId);
                return createFallbackRecommendations(userId);
            }
            
            SkillGapAnalysis analysis = latestAnalysis.get();
            
            // Get user profile for additional context
            Optional<Student> student = studentRepository.findById(userId);
            String currentJobRole = student.map(Student::getCurrentJobRole).orElse("Software Engineer");
            
            // Prepare REAL profile data
            Map<String, Object> profileData = new HashMap<>();
            if (student.isPresent()) {
                Student studentData = student.get();
                profileData.put("firstName", studentData.getFirstName());
                profileData.put("lastName", studentData.getLastName());
                profileData.put("email", studentData.getEmail());
                profileData.put("currentJobRole", studentData.getCurrentJobRole());
                profileData.put("yearsOfExperience", studentData.getYearsOfExperience());
                profileData.put("department", studentData.getDepartment());
                profileData.put("company", studentData.getCompanyName() != null ? studentData.getCompanyName() : "Not specified");
                
                // Add education if available
                if (studentData.getEducation() != null && !studentData.getEducation().isEmpty()) {
                    profileData.put("education", studentData.getEducation());
                }
                
                // Add experience if available
                if (studentData.getExperience() != null && !studentData.getExperience().isEmpty()) {
                    profileData.put("experience", studentData.getExperience());
                }
                
                log.info("✅ Loaded real profile data for user: {}", studentData.getEmail());
            } else {
                // Fallback profile data
                profileData.put("currentJobRole", currentJobRole);
                profileData.put("yearsOfExperience", 0);
                profileData.put("department", "Not specified");
                log.info("⚠️ Using fallback profile data for user: {}", userId);
            }
            
            // Prepare skills data - use the current skills from analysis
            List<Map<String, Object>> skillsData = analysis.getCurrentSkills();
            
            // Prepare request for Python ML service with REAL DATA
            Map<String, Object> mlRequest = new HashMap<>();
            mlRequest.put("user_id", userId);
            mlRequest.put("job_role", analysis.getJobRole());
            mlRequest.put("current_job_role", currentJobRole);
            mlRequest.put("missing_skills", extractMissingSkills(analysis));
            mlRequest.put("current_skills", analysis.getCurrentSkills());
            
            // Add the REAL profile and skills data
            mlRequest.put("profile_data", profileData);
            mlRequest.put("skills_data", skillsData);
            
            log.info("📤 Sending request to Python ML service with REAL user data");
            log.info("👤 Profile data: {}", profileData.keySet());
            log.info("🛠️ Skills data count: {}", skillsData.size());
            
            // Call Python ML service for course recommendations
            try {
                ResponseEntity<Map> mlResponse = restTemplate.postForEntity(
                    pythonMlServiceUrl + "/api/recommendations/generate",
                    mlRequest,
                    Map.class
                );
                
                if (mlResponse.getStatusCode().is2xxSuccessful() && mlResponse.getBody() != null) {
                    log.info("✅ Python ML service responded successfully with real data");
                    return mapMlResponseToRecommendationResponse(mlResponse.getBody(), analysis);
                } else {
                    log.warn("⚠️ Python ML service returned non-200 status: {}", mlResponse.getStatusCode());
                }
            } catch (Exception e) {
                log.error("❌ Failed to call Python ML service: {}", e.getMessage());
            }
            
            // Fallback to basic recommendations if ML service fails
            log.info("🔄 Using fallback recommendations");
            return createBasicRecommendations(analysis, currentJobRole);
            
        } catch (Exception e) {
            log.error("❌ Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
            return createFallbackRecommendations(userId);
        }
    }
    
    private List<Map<String, Object>> extractMissingSkills(SkillGapAnalysis analysis) {
        return analysis.getMissingSkills().stream()
                .map(skill -> {
                    Map<String, Object> skillMap = new HashMap<>();
                    skillMap.put("name", skill.get("name"));
                    skillMap.put("importance", skill.get("importance"));
                    skillMap.put("category", skill.get("category"));
                    skillMap.put("priority", getPriorityFromImportance((Double) skill.get("importance")));
                    return skillMap;
                })
                .collect(Collectors.toList());
    }
    
    private String getPriorityFromImportance(Double importance) {
        if (importance > 0.1) return "High";
        if (importance > 0.05) return "Medium";
        return "Low";
    }
    
    private RecommendationResponse mapMlResponseToRecommendationResponse(Map<String, Object> mlResponse, 
            SkillGapAnalysis analysis) {
try {
// Extract missing skills with enhanced data
List<Map<String, Object>> missingSkills = analysis.getMissingSkills().stream()
.map(skill -> {
Map<String, Object> enhancedSkill = new HashMap<>();
enhancedSkill.put("name", skill.get("name"));
enhancedSkill.put("description", getSkillDescription((String) skill.get("name")));
enhancedSkill.put("importance", skill.get("importance"));
enhancedSkill.put("category", skill.get("category"));
enhancedSkill.put("priority", getPriorityFromImportance((Double) skill.get("importance")));
return enhancedSkill;
})
.collect(Collectors.toList());

// Calculate progress
double progressPercentage = calculateProgressPercentage(analysis);

// Get current job role from student profile
String currentJobRole = getCurrentJobRole(analysis.getUserId());

return RecommendationResponse.builder()
.missingSkills(missingSkills)
.courseRecommendations(extractCoursesFromMlResponse(mlResponse))
.learningPathway(extractLearningPathwayFromMlResponse(mlResponse))
.insights(extractInsightsFromMlResponse(mlResponse, analysis))
.progressPercentage(progressPercentage)
.currentJobRole(currentJobRole)
.targetJobRole(analysis.getJobRole())
.build();

} catch (Exception e) {
log.error("Error mapping ML response: {}", e.getMessage());
return createBasicRecommendations(analysis, "Software Engineer");
}
}

private String getCurrentJobRole(String userId) {
try {
Optional<Student> student = studentRepository.findById(userId);
return student.map(Student::getCurrentJobRole)
.orElse("Software Engineer");
} catch (Exception e) {
return "Software Engineer";
}
}
    
    private List<CourseRecommendation> extractCoursesFromMlResponse(Map<String, Object> mlResponse) {
        try {
            List<Map<String, Object>> courseData = (List<Map<String, Object>>) mlResponse.get("courseRecommendations");
            if (courseData == null) return new ArrayList<>();
            
            return courseData.stream()
                    .map(course -> CourseRecommendation.builder()
                            .id((String) course.get("id"))
                            .skillId((String) course.get("skillId"))
                            .skillName((String) course.get("skillName"))
                            .platform((String) course.get("platform"))
                            .title((String) course.get("title"))
                            .instructor((String) course.get("instructor"))
                            .duration((String) course.get("duration"))
                            .difficulty((String) course.get("difficulty"))
                            .rating(course.get("rating") != null ? ((Number) course.get("rating")).doubleValue() : 4.0)
                            .studentCount(course.get("students") != null ? ((Number) course.get("students")).intValue() : 1000)
                            .price((String) course.get("price"))
                            .originalPrice((String) course.get("originalPrice"))
                            .url((String) course.get("url"))
                            .features((List<String>) course.get("features"))
                            .durationCategory((String) course.get("durationCategory"))
                            .platformIcon((String) course.get("platformIcon"))
                            .description((String) course.get("description"))
                            .relevanceScore(course.get("relevanceScore") != null ? 
                                    ((Number) course.get("relevanceScore")).doubleValue() : 0.8)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error extracting courses from ML response: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<LearningPathStep> extractLearningPathwayFromMlResponse(Map<String, Object> mlResponse) {
        try {
            List<Map<String, Object>> pathwayData = (List<Map<String, Object>>) mlResponse.get("learningPathway");
            if (pathwayData == null) return new ArrayList<>();
            
            return pathwayData.stream()
                    .map(step -> LearningPathStep.builder()
                            .step(((Number) step.get("step")).intValue())
                            .title((String) step.get("title"))
                            .description((String) step.get("description"))
                            .duration((String) step.get("duration"))
                            .skills((List<String>) step.get("skills"))
                            .status((String) step.get("status"))
                            .courses((List<String>) step.get("courses"))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error extracting learning pathway from ML response: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<String> extractInsightsFromMlResponse(Map<String, Object> mlResponse, SkillGapAnalysis analysis) {
        try {
            List<String> insights = (List<String>) mlResponse.get("insights");
            if (insights != null && !insights.isEmpty()) {
                return insights;
            }
        } catch (Exception e) {
            log.error("Error extracting insights from ML response: {}", e.getMessage());
        }
        
        // Fallback insights based on analysis
        return generateFallbackInsights(analysis);
    }
    
    private double calculateProgressPercentage(SkillGapAnalysis analysis) {
        int totalSkills = analysis.getRequiredSkills().size();
        int matchedSkills = analysis.getCurrentSkills().size();
        return totalSkills > 0 ? (double) matchedSkills / totalSkills * 100 : 0;
    }
    
    private String getSkillDescription(String skillName) {
    	Map<String, String> skillDescriptions = new HashMap<>();
        skillDescriptions.put("Cloud Computing", "Managing and deploying applications on cloud platforms");
        skillDescriptions.put("Automation", "Automating processes and workflows");
        skillDescriptions.put("CI/CD", "Continuous Integration and Continuous Deployment practices");
        skillDescriptions.put("Python", "Versatile programming language for various applications");
        skillDescriptions.put("Linux", "Operating system and command-line proficiency");
        skillDescriptions.put("Git", "Version control system for collaborative development");
        skillDescriptions.put("SQL", "Database querying and management");
        skillDescriptions.put("Security", "Application and data security practices");
        skillDescriptions.put("Java", "Object-oriented programming language");
        skillDescriptions.put("JavaScript", "Client-side and server-side scripting");
        skillDescriptions.put("CSS", "Styling and layout for web applications");
        skillDescriptions.put("HTML", "Markup language for web content");
        skillDescriptions.put("React", "JavaScript library for building user interfaces");
        return skillDescriptions.getOrDefault(skillName, "Essential skill for professional development");
    }
    
    private List<String> generateFallbackInsights(SkillGapAnalysis analysis) {
        List<String> insights = new ArrayList<>();
        double matchScore = analysis.getMatchScore();
        
        if (matchScore >= 80) {
            insights.add("🎉 Excellent match! Focus on mastering advanced concepts in your strongest areas.");
        } else if (matchScore >= 60) {
            insights.add("📈 Good foundation! Work on your missing skills to become highly competitive.");
        } else {
            insights.add("🚀 Great opportunity for growth! Start with foundational skills and build systematically.");
        }
        
        // Add skill-specific insights
        if (analysis.getMissingSkills().stream().anyMatch(skill -> 
            ((String) skill.get("name")).toLowerCase().contains("cloud"))) {
            insights.add("☁️ Cloud skills are in high demand and can significantly increase your market value.");
        }
        
        insights.add("⏱️ Complete the recommended courses in order for maximum learning efficiency.");
        insights.add("🎯 Focus on practical projects to reinforce your learning.");
        
        return insights;
    }
    
    private RecommendationResponse createBasicRecommendations(SkillGapAnalysis analysis, String currentJobRole) {
        List<Map<String, Object>> missingSkills = extractMissingSkills(analysis);
        double progressPercentage = calculateProgressPercentage(analysis);
        
        return RecommendationResponse.builder()
                .missingSkills(missingSkills)
                .courseRecommendations(new ArrayList<>()) // Will be populated by frontend fallback
                .learningPathway(generateBasicLearningPathway(analysis))
                .insights(generateFallbackInsights(analysis))
                .progressPercentage(progressPercentage)
                .build();
    }
    
    private RecommendationResponse createFallbackRecommendations(String userId) {
        List<Map<String, Object>> missingSkills = List.of(
            Map.of("name", "Node.js", "description", "JavaScript runtime", "importance", 0.8, "category", "Technical", "priority", "High"),
            Map.of("name", "MongoDB", "description", "NoSQL database", "importance", 0.7, "category", "Technical", "priority", "High"),
            Map.of("name", "Git", "description", "Version control", "importance", 0.6, "category", "Technical", "priority", "Medium")
        );
        
        return RecommendationResponse.builder()
                .missingSkills(missingSkills)
                .courseRecommendations(new ArrayList<>())
                .learningPathway(generateFallbackLearningPathway())
                .insights(List.of(
                    "Start with foundational skills and build systematically.",
                    "Focus on practical projects to reinforce learning.",
                    "Complete courses in order for maximum efficiency."
                ))
                .progressPercentage(0.0)
                .build();
    }
    
    private List<LearningPathStep> generateBasicLearningPathway(SkillGapAnalysis analysis) {
        List<LearningPathStep> pathway = new ArrayList<>();
        
        // Group skills by importance
        List<String> highPrioritySkills = analysis.getMissingSkills().stream()
                .filter(skill -> ((Double) skill.get("importance")) > 0.1)
                .map(skill -> (String) skill.get("name"))
                .limit(3)
                .collect(Collectors.toList());
        
        List<String> mediumPrioritySkills = analysis.getMissingSkills().stream()
                .filter(skill -> ((Double) skill.get("importance")) <= 0.1 && 
                                ((Double) skill.get("importance")) > 0.05)
                .map(skill -> (String) skill.get("name"))
                .limit(3)
                .collect(Collectors.toList());
        
        int step = 1;
        
        if (!highPrioritySkills.isEmpty()) {
            pathway.add(LearningPathStep.builder()
                    .step(step++)
                    .title("Master " + String.join(", ", highPrioritySkills.subList(0, Math.min(2, highPrioritySkills.size()))))
                    .description("Build strong foundation in high-priority technologies")
                    .duration("3 weeks")
                    .skills(highPrioritySkills)
                    .status("current")
                    .courses(List.of("course_1", "course_2"))
                    .build());
        }
        
        if (!mediumPrioritySkills.isEmpty()) {
            pathway.add(LearningPathStep.builder()
                    .step(step)
                    .title("Learn Additional Core Technologies")
                    .description("Expand your skill set with important supporting technologies")
                    .duration("2 weeks")
                    .skills(mediumPrioritySkills)
                    .status("upcoming")
                    .courses(List.of("course_3", "course_4"))
                    .build());
        }
        
        return pathway;
    }
    
    private List<LearningPathStep> generateFallbackLearningPathway() {
        return List.of(
            LearningPathStep.builder()
                    .step(1)
                    .title("Master Node.js & MongoDB Fundamentals")
                    .description("Build strong foundation in backend technologies")
                    .duration("2 weeks")
                    .skills(List.of("Node.js", "MongoDB"))
                    .status("current")
                    .courses(List.of("course_1", "course_2"))
                    .build(),
            LearningPathStep.builder()
                    .step(2)
                    .title("Learn Git & Collaboration")
                    .description("Master version control and team workflows")
                    .duration("1 week")
                    .skills(List.of("Git"))
                    .status("upcoming")
                    .courses(List.of("course_3"))
                    .build()
        );
    }
}