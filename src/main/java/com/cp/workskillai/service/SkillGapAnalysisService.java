// SkillGapAnalysisService.java (MongoDB version)
package com.cp.workskillai.service;

import com.cp.workskillai.dto.*;
import com.cp.workskillai.models.SkillGapAnalysis;
import com.cp.workskillai.repository.SkillGapAnalysisRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillGapAnalysisService {

    private final SkillGapAnalysisRepository analysisRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    public SkillGapAnalysisResponse analyzeSkillGap(SkillGapAnalysisRequest request) {
        try {
            log.info("Starting skill gap analysis for user: {}, role: {}", 
                    request.getUserId(), request.getJobRole());
            
            // Fetch user data
            Map<String, Object> userData = fetchUserData(request.getUserId());
            if (userData.isEmpty()) {
                throw new RuntimeException("Failed to fetch user data");
            }

            // Prepare request for Python service
            Map<String, Object> pythonRequest = preparePythonRequest(request, userData);
            
            // Call Python service
            SkillGapAnalysisResponse analysisResult = callPythonService(pythonRequest);
            
            // Save analysis to MongoDB and set as current role
            saveAnalysisToDatabase(request.getUserId(), request.getJobRole(), analysisResult, true);
            
            log.info("Skill gap analysis completed successfully for user: {}", request.getUserId());
            return analysisResult;
            
        } catch (Exception e) {
            log.error("Error in skill gap analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Skill gap analysis failed: " + e.getMessage());
        }
    }
    
    private Map<String, Object> fetchUserData(String userId) {
        try {
            String url = "http://localhost:2090/api/user/skill-gap-data/" + userId;
            log.info("Fetching user data from: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userData = response.getBody();
                log.info("✅ Successfully fetched user data for user: {}", userId);
                log.info("📊 User profile data: {}", userData.get("profile"));
                log.info("🛠️ User skills count: {}", 
                    userData.containsKey("skills") ? ((List)userData.get("skills")).size() : 0);
                
                // Debug: Log all skills being sent to Python
                if (userData.containsKey("skills")) {
                    List<Map<String, Object>> skills = (List<Map<String, Object>>) userData.get("skills");
                    log.info("🔍 Skills being sent to Python service:");
                    for (Map<String, Object> skill : skills) {
                        log.info("   - {}: {}% proficiency", 
                            skill.get("name"), skill.get("proficiency"));
                    }
                }
                
                return userData;
            } else {
                log.error("❌ Failed to fetch user data. Status: {}", response.getStatusCode());
                return createFallbackUserData(userId);
            }
        } catch (Exception e) {
            log.error("❌ Error fetching user data for user {}: {}", userId, e.getMessage());
            log.info("🔄 Using fallback user data");
            return createFallbackUserData(userId);
        }
    }

    private Map<String, Object> createFallbackUserData(String userId) {
        Map<String, Object> fallbackData = new HashMap<>();
        
        // Enhanced fallback profile data
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("firstName", "Test");
        profileData.put("lastName", "User");
        profileData.put("email", "test@example.com");
        profileData.put("currentJobRole", "Software Developer");
        profileData.put("yearsOfExperience", 3);
        profileData.put("summary", "Experienced software developer with strong programming skills");
        profileData.put("totalExperience", "3 years");
        
        fallbackData.put("profile", profileData);
        
        // Enhanced fallback skills data - MORE SKILLS
        List<Map<String, Object>> skillsData = new ArrayList<>();
        
        // Java ecosystem skills
        String[] javaSkills = {
            "Java", "Spring Boot", "Spring Framework", "Hibernate", "JPA", 
            "Maven", "Gradle", "JUnit", "Mockito", "J2EE", "Microservices",
            "REST APIs", "SOAP", "JSON", "XML"
        };
        
        // Web development skills
        String[] webSkills = {
            "HTML", "CSS", "JavaScript", "React", "Angular", "Vue.js",
            "TypeScript", "Bootstrap", "jQuery", "Node.js", "Express.js"
        };
        
        // Database skills
        String[] dbSkills = {
            "SQL", "MySQL", "PostgreSQL", "MongoDB", "Redis", 
            "Oracle", "SQL Server", "H2", "JDBC"
        };
        
        // DevOps skills
        String[] devOpsSkills = {
            "Git", "Docker", "Kubernetes", "Jenkins", "CI/CD",
            "AWS", "Azure", "Linux", "Bash", "Shell Scripting"
        };
        
        // Combine all skills
        List<String> allSkills = new ArrayList<>();
        allSkills.addAll(Arrays.asList(javaSkills));
        allSkills.addAll(Arrays.asList(webSkills));
        allSkills.addAll(Arrays.asList(dbSkills));
        allSkills.addAll(Arrays.asList(devOpsSkills));
        
        Random random = new Random();
        for (String skillName : allSkills) {
            Map<String, Object> skill = new HashMap<>();
            skill.put("name", skillName);
            skill.put("proficiency", random.nextInt(40) + 60); // 60-100% proficiency
            skill.put("level", getSkillLevel(random.nextInt(100)));
            skill.put("verified", random.nextBoolean());
            skill.put("confidence", random.nextBoolean() ? "high" : "medium");
            skill.put("experienceMonths", random.nextInt(36) + 12); // 1-4 years
            skill.put("category", getSkillCategory(skillName));
            
            skillsData.add(skill);
        }
        
        fallbackData.put("skills", skillsData);
        log.info("🔄 Created fallback data with {} skills", skillsData.size());
        
        return fallbackData;
    }

    private String getSkillLevel(int proficiency) {
        if (proficiency >= 80) return "Expert";
        if (proficiency >= 60) return "Advanced";
        if (proficiency >= 40) return "Intermediate";
        return "Beginner";
    }

    private String getSkillCategory(String skillName) {
        if (skillName.contains("Java") || skillName.contains("Spring")) return "Backend";
        if (skillName.contains("HTML") || skillName.contains("CSS") || skillName.contains("React")) return "Frontend";
        if (skillName.contains("SQL") || skillName.contains("MongoDB")) return "Database";
        if (skillName.contains("AWS") || skillName.contains("Docker") || skillName.contains("Git")) return "DevOps";
        return "Other";
    }
    public CurrentRoleResponseDTO getCurrentRoleAnalysis(String userId) {
        try {
            Optional<SkillGapAnalysis> currentAnalysis = analysisRepository.findCurrentRoleAnalysis(userId);
            
            CurrentRoleResponseDTO response = new CurrentRoleResponseDTO();
            
            if (currentAnalysis.isPresent()) {
                SkillGapAnalysis analysis = currentAnalysis.get();
                response.setCurrentRole(analysis.getJobRole());
                response.setMatchScore(analysis.getMatchScore());
                response.setHasAnalysis(true);
                
                // Convert MongoDB document to DTO
                response.setRequiredSkills(convertToSkillAnalysisList(analysis.getRequiredSkills()));
                response.setCurrentSkills(convertToUserSkillAnalysisList(analysis.getCurrentSkills()));
                response.setMissingSkills(convertToSkillAnalysisList(analysis.getMissingSkills()));
                response.setPartialMatchSkills(convertToSkillAnalysisList(analysis.getPartialMatchSkills()));
                response.setGapAnalysis(analysis.getGapAnalysis());
                response.setRecommendations(analysis.getRecommendations());
                response.setTimeToCloseGap(analysis.getTimeToCloseGap());
                response.setSalaryImpact(analysis.getSalaryImpact());
            } else {
                response.setHasAnalysis(false);
                response.setCurrentRole("Not set");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error fetching current role analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch current role analysis");
        }
    }

    public List<SkillGapAnalysisHistoryDTO> getAnalysisHistory(String userId) {
        List<SkillGapAnalysis> analyses = analysisRepository.findByUserIdOrderByAnalyzedAtDesc(userId);
        List<SkillGapAnalysisHistoryDTO> history = new ArrayList<>();
        
        for (SkillGapAnalysis analysis : analyses) {
            SkillGapAnalysisHistoryDTO dto = new SkillGapAnalysisHistoryDTO();
            dto.setId(analysis.getId());
            dto.setJobRole(analysis.getJobRole());
            dto.setMatchScore(analysis.getMatchScore());
            dto.setRequiredSkills(convertToSkillAnalysisList(analysis.getRequiredSkills()));
            dto.setCurrentSkills(convertToUserSkillAnalysisList(analysis.getCurrentSkills()));
            dto.setMissingSkills(convertToSkillAnalysisList(analysis.getMissingSkills()));
            dto.setPartialMatchSkills(convertToSkillAnalysisList(analysis.getPartialMatchSkills()));
            dto.setGapAnalysis(analysis.getGapAnalysis());
            dto.setRecommendations(analysis.getRecommendations());
            dto.setTimeToCloseGap(analysis.getTimeToCloseGap());
            dto.setSalaryImpact(analysis.getSalaryImpact());
            dto.setIsCurrentRole(analysis.getIsCurrentRole());
            dto.setAnalyzedAt(analysis.getAnalyzedAt());
            
            history.add(dto);
        }
        
        return history;
    }

    private Map<String, Object> preparePythonRequest(SkillGapAnalysisRequest request, Map<String, Object> userData) {
        Map<String, Object> pythonRequest = new HashMap<>();
        pythonRequest.put("user_id", request.getUserId());
        pythonRequest.put("job_role", request.getJobRole());
        pythonRequest.put("profile_data", userData.getOrDefault("profile", new HashMap<>()));
        pythonRequest.put("skills_data", userData.getOrDefault("skills", new ArrayList<>()));
        return pythonRequest;
    }

    private SkillGapAnalysisResponse callPythonService(Map<String, Object> request) {
        try {
            String url = pythonServiceUrl + "/internal-analyze";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            // Use String.class to get the raw response first
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Python service response received: {}", response.getBody());
                
                // Parse the response manually to handle field mapping
                return parsePythonResponse(response.getBody());
            } else {
                log.error("Python service returned error: {}", response.getStatusCode());
                throw new RuntimeException("Python service returned error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Python service: {}", e.getMessage(), e);
            throw new RuntimeException("Python service unavailable: " + e.getMessage());
        }
    }
    
    private SkillGapAnalysisResponse parsePythonResponse(String responseBody) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
            SkillGapAnalysisResponse response = new SkillGapAnalysisResponse();
            
            // Map fields from Python response to Java DTO
            response.setJobRole((String) responseMap.get("jobRole"));
            response.setMatchScore(convertToDouble(responseMap.get("matchScore")));
            response.setUserId((String) responseMap.get("userId"));
            response.setTimeToCloseGap((String) responseMap.get("timeToCloseGap"));
            response.setSalaryImpact((String) responseMap.get("salaryImpact"));
            
            // Map skills lists
            response.setRequiredSkills(parseSkillsList((List<Map<String, Object>>) responseMap.get("requiredSkills")));
            response.setCurrentSkills(parseUserSkillsList((List<Map<String, Object>>) responseMap.get("currentSkills")));
            response.setMissingSkills(parseSkillsList((List<Map<String, Object>>) responseMap.get("missingSkills")));
            response.setPartialMatchSkills(parseSkillsList((List<Map<String, Object>>) responseMap.get("partialMatchSkills")));
            
            // Map gap analysis
            response.setGapAnalysis((Map<String, Object>) responseMap.get("gapAnalysis"));
            
            // Map recommendations
            List<String> recommendations = (List<String>) responseMap.get("recommendations");
            response.setRecommendations(recommendations != null ? recommendations : new ArrayList<>());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error parsing Python response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse Python service response");
        }
    }

    private List<SkillAnalysis> parseSkillsList(List<Map<String, Object>> skillsMap) {
        List<SkillAnalysis> skills = new ArrayList<>();
        if (skillsMap == null) return skills;
        
        for (Map<String, Object> skillMap : skillsMap) {
            SkillAnalysis skill = new SkillAnalysis();
            skill.setName((String) skillMap.get("name"));
            skill.setImportance(convertToDouble(skillMap.get("importance")));
            skill.setRequiredProficiency(convertToInteger(skillMap.get("requiredProficiency")));
            skill.setCategory((String) skillMap.get("category"));
            skill.setProbability(convertToDouble(skillMap.get("probability")));
            skill.setUserProficiency(convertToInteger(skillMap.get("userProficiency")));
            skill.setGap(convertToInteger(skillMap.get("gap")));
            skill.setStatus((String) skillMap.get("status"));
            skill.setUserConfidence((String) skillMap.get("userConfidence"));
            skills.add(skill);
        }
        return skills;
    }

    private List<UserSkillAnalysis> parseUserSkillsList(List<Map<String, Object>> skillsMap) {
        List<UserSkillAnalysis> skills = new ArrayList<>();
        if (skillsMap == null) return skills;
        
        for (Map<String, Object> skillMap : skillsMap) {
            UserSkillAnalysis skill = new UserSkillAnalysis();
            skill.setName((String) skillMap.get("name"));
            skill.setProficiency(convertToInteger(skillMap.get("proficiency")));
            skill.setLevel((String) skillMap.get("level"));
            skill.setVerified(convertToBoolean(skillMap.get("verified")));
            skill.setConfidence((String) skillMap.get("confidence"));
            skills.add(skill);
        }
        return skills;
    }

    private void saveAnalysisToDatabase(String userId, String jobRole, 
                                      SkillGapAnalysisResponse analysis, boolean setAsCurrent) {
        try {
            if (setAsCurrent) {
                // Clear previous current role flag
                analysisRepository.clearCurrentRoleFlag(userId);
            }
            
            SkillGapAnalysis entity = new SkillGapAnalysis();
            entity.setUserId(userId);
            entity.setJobRole(jobRole);
            entity.setMatchScore(analysis.getMatchScore());
            entity.setAnalyzedAt(LocalDateTime.now());
            entity.setIsCurrentRole(setAsCurrent);
            
            // Convert DTOs to MongoDB-compatible maps
            entity.setRequiredSkills(convertSkillAnalysisToMap(analysis.getRequiredSkills()));
            entity.setCurrentSkills(convertUserSkillAnalysisToMap(analysis.getCurrentSkills()));
            entity.setMissingSkills(convertSkillAnalysisToMap(analysis.getMissingSkills()));
            entity.setPartialMatchSkills(convertSkillAnalysisToMap(analysis.getPartialMatchSkills()));
            entity.setGapAnalysis(analysis.getGapAnalysis());
            entity.setRecommendations(analysis.getRecommendations());
            entity.setTimeToCloseGap(analysis.getTimeToCloseGap());
            entity.setSalaryImpact(analysis.getSalaryImpact());
            
            analysisRepository.save(entity);
            log.info("Analysis saved to MongoDB for user: {}, role: {}", userId, jobRole);
            
        } catch (Exception e) {
            log.error("Error saving analysis to database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save analysis results");
        }
    }

    // Helper methods for conversion between DTO and MongoDB document
    private List<Map<String, Object>> convertSkillAnalysisToMap(List<SkillAnalysis> skills) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (skills == null) return result;
        
        for (SkillAnalysis skill : skills) {
            Map<String, Object> skillMap = new HashMap<>();
            skillMap.put("name", skill.getName());
            skillMap.put("importance", skill.getImportance());
            skillMap.put("requiredProficiency", skill.getRequiredProficiency());
            skillMap.put("category", skill.getCategory());
            skillMap.put("probability", skill.getProbability());
            skillMap.put("userProficiency", skill.getUserProficiency());
            skillMap.put("gap", skill.getGap());
            skillMap.put("status", skill.getStatus());
            skillMap.put("userConfidence", skill.getUserConfidence());
            result.add(skillMap);
        }
        return result;
    }

    private List<Map<String, Object>> convertUserSkillAnalysisToMap(List<UserSkillAnalysis> skills) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (skills == null) return result;
        
        for (UserSkillAnalysis skill : skills) {
            Map<String, Object> skillMap = new HashMap<>();
            skillMap.put("name", skill.getName());
            skillMap.put("proficiency", skill.getProficiency());
            skillMap.put("level", skill.getLevel());
            skillMap.put("verified", skill.getVerified());
            skillMap.put("confidence", skill.getConfidence());
            result.add(skillMap);
        }
        return result;
    }

    private List<SkillAnalysis> convertToSkillAnalysisList(List<Map<String, Object>> skillMaps) {
        List<SkillAnalysis> result = new ArrayList<>();
        if (skillMaps == null) return result;
        
        for (Map<String, Object> skillMap : skillMaps) {
            SkillAnalysis skill = new SkillAnalysis();
            skill.setName((String) skillMap.get("name"));
            skill.setImportance(convertToDouble(skillMap.get("importance")));
            skill.setRequiredProficiency(convertToInteger(skillMap.get("requiredProficiency")));
            skill.setCategory((String) skillMap.get("category"));
            skill.setProbability(convertToDouble(skillMap.get("probability")));
            skill.setUserProficiency(convertToInteger(skillMap.get("userProficiency")));
            skill.setGap(convertToInteger(skillMap.get("gap")));
            skill.setStatus((String) skillMap.get("status"));
            skill.setUserConfidence((String) skillMap.get("userConfidence"));
            result.add(skill);
        }
        return result;
    }

    private List<UserSkillAnalysis> convertToUserSkillAnalysisList(List<Map<String, Object>> skillMaps) {
        List<UserSkillAnalysis> result = new ArrayList<>();
        if (skillMaps == null) return result;
        
        for (Map<String, Object> skillMap : skillMaps) {
            UserSkillAnalysis skill = new UserSkillAnalysis();
            skill.setName((String) skillMap.get("name"));
            skill.setProficiency(convertToInteger(skillMap.get("proficiency")));
            skill.setLevel((String) skillMap.get("level"));
            skill.setVerified(convertToBoolean(skillMap.get("verified")));
            skill.setConfidence((String) skillMap.get("confidence"));
            result.add(skill);
        }
        return result;
    }

    // Utility conversion methods
    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof String) return Double.parseDouble((String) value);
        return 0.0;
    }

    private Integer convertToInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        return 0;
    }

    private Boolean convertToBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return false;
    }
}