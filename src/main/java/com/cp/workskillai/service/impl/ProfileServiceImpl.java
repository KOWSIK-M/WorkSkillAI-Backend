package com.cp.workskillai.service.impl;

import com.cp.workskillai.dto.ProfileUpdateRequest;
import com.cp.workskillai.dto.ResumeAnalysisResponse;
import com.cp.workskillai.models.*;
import com.cp.workskillai.repository.ResumeRepository;
import com.cp.workskillai.repository.StudentRepository;
import com.cp.workskillai.repository.UserProfileRepository;
import com.cp.workskillai.repository.UserSkillRepository;
import com.cp.workskillai.service.GeminiAIService;
import com.cp.workskillai.service.ProfileService;
import com.cp.workskillai.service.UserSkillService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserSkillRepository userSkillRepository;
    private final ResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final GeminiAIService geminiAIService;

    @Override
    public UserProfile getProfile(String userId) {
        log.info("Fetching profile for user: {}", userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        
        // Sync skills from profile to UserSkill model
        syncSkillsFromProfile(userId, profile.getTechnicalSkills());
        
        return profile;
    }

    @Override
    public UserProfile updateProfile(String userId, ProfileUpdateRequest request) {
        log.info("Updating profile for user: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        
        // Update all fields from request
        updateProfileFromRequest(profile, request);
        profile.setUpdatedAt(LocalDateTime.now().toString());
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        
        // Sync skills to UserSkill model
        syncSkillsFromProfile(userId, savedProfile.getTechnicalSkills());
        
        log.info("Profile updated successfully for user: {}", userId);
        
        return savedProfile;
    }

    @Override
    public UserProfile createProfile(String userId, ProfileUpdateRequest request) {
        log.info("Creating new profile for user: {}", userId);
        
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + userId));
        
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .fullName(request.getFullName() != null ? request.getFullName() : student.getFirstName() + " " + student.getLastName())
                .email(request.getEmail() != null ? request.getEmail() : student.getEmail())
                .contactNumber(request.getContactNumber())
                .location(request.getLocation())
                .linkedInUrl(request.getLinkedInUrl())
                .githubUrl(request.getGithubUrl())
                .portfolioUrl(request.getPortfolioUrl())
                .title(request.getTitle())
                .summary(request.getSummary())
                .technicalSkills(request.getTechnicalSkills() != null ? request.getTechnicalSkills() : new ArrayList<>())
                .softSkills(request.getSoftSkills() != null ? request.getSoftSkills() : new ArrayList<>())
                .languages(request.getLanguages() != null ? request.getLanguages() : new ArrayList<>())
                .totalExperience(request.getTotalExperience())
                .education(request.getEducation() != null ? request.getEducation() : new ArrayList<>())
                .experience(request.getExperience() != null ? request.getExperience() : new ArrayList<>())
                .certifications(request.getCertifications() != null ? request.getCertifications() : new ArrayList<>())
                .projects(request.getProjects() != null ? request.getProjects() : new ArrayList<>())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .seekingOpportunities(request.getSeekingOpportunities() != null ? request.getSeekingOpportunities() : true)
                .preferredRoles(request.getPreferredRoles() != null ? request.getPreferredRoles() : new ArrayList<>())
                .expectedSalary(request.getExpectedSalary())
                .noticePeriod(request.getNoticePeriod())
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        
        // Sync skills to UserSkill model
        syncSkillsFromProfile(userId, savedProfile.getTechnicalSkills());
        
        log.info("New profile created for user: {}", userId);
        
        return savedProfile;
    }

    // ========== SKILL SYNC METHODS ==========

    /**
     * Sync technical skills from profile to UserSkill model
     */
    private void syncSkillsFromProfile(String userId, List<String> technicalSkills) {
        if (technicalSkills == null || technicalSkills.isEmpty()) {
            log.info("No technical skills to sync for user: {}", userId);
            return;
        }

        try {
            List<UserSkill> existingSkills = userSkillRepository.findByUserId(userId);
            Map<String, UserSkill> existingSkillsMap = existingSkills.stream()
                    .collect(Collectors.toMap(
                            skill -> skill.getName().toLowerCase(),
                            skill -> skill
                    ));

            List<UserSkill> skillsToSave = new ArrayList<>();
            Set<String> processedSkills = new HashSet<>();

            for (String skillName : technicalSkills) {
                if (skillName == null || skillName.trim().isEmpty()) {
                    continue;
                }

                String normalizedSkillName = skillName.trim();
                String lowerCaseSkillName = normalizedSkillName.toLowerCase();

                if (processedSkills.contains(lowerCaseSkillName)) {
                    continue; // Skip duplicates
                }

                processedSkills.add(lowerCaseSkillName);

                if (existingSkillsMap.containsKey(lowerCaseSkillName)) {
                    // Skill exists, update if needed
                    UserSkill existingSkill = existingSkillsMap.get(lowerCaseSkillName);
                    boolean needsUpdate = updateExistingSkill(existingSkill, normalizedSkillName);
                    if (needsUpdate) {
                        skillsToSave.add(existingSkill);
                    }
                } else {
                    // Create new skill
                    UserSkill newSkill = createNewUserSkill(userId, normalizedSkillName);
                    skillsToSave.add(newSkill);
                }
            }

            // Save all skills
            if (!skillsToSave.isEmpty()) {
                userSkillRepository.saveAll(skillsToSave);
                log.info("Synced {} skills for user: {}", skillsToSave.size(), userId);
            }

            // Handle skills that were removed from profile
            handleRemovedSkills(userId, existingSkills, processedSkills);

        } catch (Exception e) {
            log.error("Error syncing skills for user: {}", userId, e);
        }
    }

    /**
     * Update existing skill if needed
     */
    private boolean updateExistingSkill(UserSkill existingSkill, String skillName) {
        boolean needsUpdate = false;

        // Update category if it's different or empty
        String newCategory = determineCategory(skillName);
        if (!newCategory.equals(existingSkill.getCategory())) {
            existingSkill.setCategory(newCategory);
            needsUpdate = true;
        }

        // Update timestamp
        existingSkill.setUpdatedAt(LocalDateTime.now());

        return needsUpdate;
    }

    /**
     * Create new UserSkill from profile skill
     */
    private UserSkill createNewUserSkill(String userId, String skillName) {
        String category = determineCategory(skillName);
        
        return UserSkill.builder()
                .userId(userId)
                .name(skillName)
                .category(category)
                .proficiency(0)
                .score(0)
                .status("pending")
                .level("Pending")
                .verified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .experienceMonths(0)
                .confidenceLevel("low")
                .build();
    }

    /**
     * Handle skills that were removed from profile
     */
    private void handleRemovedSkills(String userId, List<UserSkill> existingSkills, Set<String> currentSkillNames) {
        List<UserSkill> skillsToRemove = existingSkills.stream()
                .filter(skill -> !currentSkillNames.contains(skill.getName().toLowerCase()))
                .collect(Collectors.toList());

        if (!skillsToRemove.isEmpty()) {
            // Instead of deleting, mark them as inactive or keep for history
            // userSkillRepository.deleteAll(skillsToRemove);
            log.info("Found {} skills removed from profile for user: {}", skillsToRemove.size(), userId);
            // You can choose to delete or just log the removed skills
        }
    }

    /**
     * Enhanced skill categorization
     */
    private String determineCategory(String skillName) {
        if (skillName == null) {
            return "Other";
        }

        String lowerSkill = skillName.toLowerCase();

        // Programming Languages
        if (lowerSkill.matches(".*\\b(java|python|javascript|typescript|c\\+\\+|c#|go|rust|kotlin|swift|php|ruby|scala|r|matlab|perl|haskell|elixir|clojure|dart)\\b.*")) {
            return "Programming";
        }
        // Frontend Technologies
        else if (lowerSkill.matches(".*\\b(react|angular|vue|svelte|ember|backbone|jquery|html|css|sass|less|bootstrap|tailwind|webpack|vite|babel|redux|mobx|next\\.?js|nuxt\\.?js|gatsby)\\b.*")) {
            return "Frontend";
        }
        // Backend Technologies
        else if (lowerSkill.matches(".*\\b(node\\.?js|express|spring|django|flask|fastapi|laravel|ruby on rails|asp\\.net|nestjs|koa|hapi|micronaut|quarkus|graphql|rest api|microservices|serverless)\\b.*")) {
            return "Backend";
        }
        // Database Technologies
        else if (lowerSkill.matches(".*\\b(mysql|postgresql|mongodb|redis|elasticsearch|cassandra|oracle|sql server|sqlite|dynamodb|cosmosdb|firebase|realm|hbase|couchbase|neo4j|arangodb)\\b.*")) {
            return "Database";
        }
        // Cloud Technologies
        else if (lowerSkill.matches(".*\\b(aws|azure|gcp|google cloud|amazon web services|docker|kubernetes|terraform|ansible|jenkins|gitlab|github actions|circleci|travis ci|helm|istio|linkerd|openshift)\\b.*")) {
            return "Cloud & DevOps";
        }
        // Mobile Development
        else if (lowerSkill.matches(".*\\b(android|ios|react native|flutter|xamarin|ionic|cordova|phonegap|swiftui|jetpack compose|kotlin multiplatform)\\b.*")) {
            return "Mobile";
        }
        // Data Science & AI/ML
        else if (lowerSkill.matches(".*\\b(tensorflow|pytorch|keras|scikit-learn|pandas|numpy|matplotlib|seaborn|jupyter|tableau|power bi|apache spark|hadoop|kafka|airflow|mlflow|kubeflow|hugging face|openai)\\b.*")) {
            return "Data Science & AI";
        }
        // Testing
        else if (lowerSkill.matches(".*\\b(junit|testng|jest|mocha|chai|cypress|selenium|playwright|pytest|rspec|cucumber|jmeter|postman|soapui)\\b.*")) {
            return "Testing";
        }
        // Tools & Methodologies
        else if (lowerSkill.matches(".*\\b(git|svn|mercurial|jira|confluence|slack|teams|zoom|agile|scrum|kanban|waterfall|devops|ci/cd|tdd|bdd|domain driven design|clean architecture)\\b.*")) {
            return "Tools & Methodologies";
        }
        // Soft Skills
        else if (lowerSkill.matches(".*\\b(communication|leadership|teamwork|problem solving|critical thinking|adaptability|time management|creativity|collaboration|presentation|negotiation|conflict resolution|emotional intelligence)\\b.*")) {
            return "Soft Skills";
        }
        else {
            return "Other";
        }
    }

    // ========== EXISTING METHODS (Keep all your existing methods) ==========

    @Override
    public ResumeAnalysisResponse uploadAndAnalyzeResume(String userId, MultipartFile file) {
        log.info("Uploading and analyzing resume for user: {}", userId);
        
        try {
            // Validate file
            validateFile(file);
            
            // Check and enforce 4-resume limit
            enforceResumeLimit(userId);
            
            // Create and save resume document
            ResumeDocument resume = createResumeDocument(userId, file);
            ResumeDocument savedResume = resumeRepository.save(resume);
            
            // Analyze resume with AI
            Map<String, Object> analysisResult = geminiAIService.analyzeResume(file);
            
            // Update resume with analysis results
            updateResumeWithAnalysis(savedResume, analysisResult);
            resumeRepository.save(savedResume);
            
            // Update or create user profile with extracted data
            UserProfile updatedProfile = updateProfileFromResume(userId, savedResume);
            
            // Set as active resume
            setActiveResume(savedResume.getId(), userId);
            
            // Sync skills from the analyzed resume
            if (savedResume.getTechnicalSkills() != null && !savedResume.getTechnicalSkills().isEmpty()) {
                syncSkillsFromProfile(userId, savedResume.getTechnicalSkills());
            }
            
            log.info("Resume analysis completed successfully for user: {}", userId);
            
            return new ResumeAnalysisResponse(
                true,
                "Resume uploaded and analyzed successfully",
                analysisResult,
                savedResume.getId(),
                updatedProfile.getId(),
                savedResume.getConfidenceScore()
            );
            
        } catch (Exception e) {
            log.error("Error during resume upload and analysis for user: {}", userId, e);
            throw new RuntimeException("Resume processing failed: " + e.getMessage());
        }
    }

    @Override
    public List<ResumeDocument> getUserResumes(String userId) {
        log.info("Fetching resumes for user: {}", userId);
        return resumeRepository.findByUserIdOrderByUploadDateDesc(userId);
    }

    @Override
    public ResumeDocument getResumeById(String resumeId) {
        log.info("Fetching resume by ID: {}", resumeId);
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found: " + resumeId));
    }

    @Override
    public boolean deleteResume(String resumeId, String userId) {
        log.info("Deleting resume: {} for user: {}", resumeId, userId);
        
        ResumeDocument resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or access denied"));
        
        resumeRepository.delete(resume);
        
        // If this was the active resume, set another one as active
        if (resume.getIsActive()) {
            List<ResumeDocument> otherResumes = resumeRepository.findByUserIdOrderByUploadDateDesc(userId);
            if (!otherResumes.isEmpty()) {
                setActiveResume(otherResumes.get(0).getId(), userId);
            } else {
                // No resumes left, clear currentResumeId from profile
                UserProfile profile = getProfile(userId);
                profile.setCurrentResumeId(null);
                userProfileRepository.save(profile);
            }
        }
        
        return true;
    }

    @Override
    public boolean setActiveResume(String resumeId, String userId) {
        log.info("Setting active resume: {} for user: {}", resumeId, userId);
        
        // Deactivate all other resumes for this user
        List<ResumeDocument> userResumes = resumeRepository.findByUserId(userId);
        userResumes.forEach(resume -> {
            resume.setIsActive(false);
            resumeRepository.save(resume);
        });
        
        // Activate the specified resume
        ResumeDocument activeResume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or access denied"));
        
        activeResume.setIsActive(true);
        resumeRepository.save(activeResume);
        
        // Update user profile with current resume ID
        UserProfile profile = getProfile(userId);
        profile.setCurrentResumeId(resumeId);
        userProfileRepository.save(profile);
        
        return true;
    }

    @Override
    public List<ResumeDocument> getResumeHistory(String userId) {
        log.info("Fetching resume history for user: {}", userId);
        return resumeRepository.findTop4ByUserIdOrderByUploadDateDesc(userId);
    }

    @Override
    public ResumeAnalysisResponse analyzeExistingResume(String resumeId) {
        log.info("Re-analyzing resume: {}", resumeId);
        
        ResumeDocument resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found: " + resumeId));
        
        try {
            MultipartFile virtualFile = createVirtualMultipartFile(resume);
            Map<String, Object> analysisResult = geminiAIService.analyzeResume(virtualFile);
            
            updateResumeWithAnalysis(resume, analysisResult);
            resumeRepository.save(resume);
            
            // Update user profile with new analysis
            UserProfile updatedProfile = updateProfileFromResume(resume.getUserId(), resume);
            
            // Sync skills from the re-analyzed resume
            if (resume.getTechnicalSkills() != null && !resume.getTechnicalSkills().isEmpty()) {
                syncSkillsFromProfile(resume.getUserId(), resume.getTechnicalSkills());
            }
            
            return new ResumeAnalysisResponse(
                true,
                "Resume re-analyzed successfully",
                analysisResult,
                resume.getId(),
                updatedProfile.getId(),
                resume.getConfidenceScore()
            );
            
        } catch (Exception e) {
            log.error("Error re-analyzing resume: {}", resumeId, e);
            throw new RuntimeException("Resume re-analysis failed: " + e.getMessage());
        }
    }

    // ========== HELPER METHODS ==========

    private UserProfile createDefaultProfile(String userId) {
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + userId));
        
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .fullName(student.getFirstName() + " " + student.getLastName())
                .email(student.getEmail())
                .isPublic(true)
                .seekingOpportunities(true)
                .technicalSkills(new ArrayList<>())
                .softSkills(new ArrayList<>())
                .languages(new ArrayList<>())
                .education(new ArrayList<>())
                .experience(new ArrayList<>())
                .certifications(new ArrayList<>())
                .projects(new ArrayList<>())
                .preferredRoles(new ArrayList<>())
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
        
        return userProfileRepository.save(profile);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
        );
        
        if (!allowedTypes.contains(contentType)) {
            throw new RuntimeException("Invalid file type. Only PDF, DOCX, and TXT files are allowed.");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size must be less than 5MB");
        }
    }


    private void enforceResumeLimit(String userId) {
        long resumeCount = resumeRepository.countByUserId(userId);
        if (resumeCount >= 4) {
            List<ResumeDocument> userResumes = resumeRepository.findByUserIdOrderByUploadDateDesc(userId);
            if (!userResumes.isEmpty()) {
                ResumeDocument oldestResume = userResumes.get(userResumes.size() - 1);
                resumeRepository.delete(oldestResume);
                log.info("Deleted oldest resume to maintain limit: {}", oldestResume.getId());
            }
        }
    }

    private ResumeDocument createResumeDocument(String userId, MultipartFile file) throws IOException {
        return ResumeDocument.builder()
                .userId(userId)
                .fileName(generateFileName(file.getOriginalFilename()))
                .originalFileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileData(file.getBytes())
                .uploadDate(LocalDateTime.now())
                .analysisComplete(false)
                .isActive(false)
                .confidenceScore(0.0)
                .uploadSource("web_upload")
                .build();
    }

    private void updateProfileFromRequest(UserProfile profile, ProfileUpdateRequest request) {
        // Personal Information
        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getContactNumber() != null) profile.setContactNumber(request.getContactNumber());
        if (request.getLocation() != null) profile.setLocation(request.getLocation());
        if (request.getLinkedInUrl() != null) profile.setLinkedInUrl(request.getLinkedInUrl());
        if (request.getGithubUrl() != null) profile.setGithubUrl(request.getGithubUrl());
        if (request.getPortfolioUrl() != null) profile.setPortfolioUrl(request.getPortfolioUrl());
        
        // Professional Information
        if (request.getTitle() != null) profile.setTitle(request.getTitle());
        if (request.getSummary() != null) profile.setSummary(request.getSummary());
        if (request.getTechnicalSkills() != null) profile.setTechnicalSkills(request.getTechnicalSkills());
        if (request.getSoftSkills() != null) profile.setSoftSkills(request.getSoftSkills());
        if (request.getLanguages() != null) profile.setLanguages(request.getLanguages());
        if (request.getTotalExperience() != null) profile.setTotalExperience(request.getTotalExperience());
        
        // Collections
        if (request.getEducation() != null) profile.setEducation(request.getEducation());
        if (request.getExperience() != null) profile.setExperience(request.getExperience());
        if (request.getCertifications() != null) profile.setCertifications(request.getCertifications());
        if (request.getProjects() != null) profile.setProjects(request.getProjects());
        
        // Settings
        if (request.getIsPublic() != null) profile.setIsPublic(request.getIsPublic());
        if (request.getSeekingOpportunities() != null) profile.setSeekingOpportunities(request.getSeekingOpportunities());
        if (request.getPreferredRoles() != null) profile.setPreferredRoles(request.getPreferredRoles());
        if (request.getExpectedSalary() != null) profile.setExpectedSalary(request.getExpectedSalary());
        if (request.getNoticePeriod() != null) profile.setNoticePeriod(request.getNoticePeriod());
    }

    private void updateResumeWithAnalysis(ResumeDocument resume, Map<String, Object> analysisResult) {
        resume.setFullName((String) analysisResult.getOrDefault("fullName", ""));
        resume.setEmail((String) analysisResult.getOrDefault("email", ""));
        resume.setContactNumber((String) analysisResult.getOrDefault("contactNumber", ""));
        resume.setTitle((String) analysisResult.getOrDefault("title", ""));
        resume.setSummary((String) analysisResult.getOrDefault("summary", ""));
        resume.setTechnicalSkills((List<String>) analysisResult.getOrDefault("skills", new ArrayList<>()));
        
        // Convert analysis result to proper sub-document types
        resume.setEducation(convertToEducationList((List<Map<String, String>>) analysisResult.getOrDefault("education", new ArrayList<>())));
        resume.setExperience(convertToExperienceList((List<Map<String, String>>) analysisResult.getOrDefault("experience", new ArrayList<>())));
        resume.setCertifications(convertToCertificationList((List<String>) analysisResult.getOrDefault("certifications", new ArrayList<>())));
        
        resume.setAnalyzedDate(LocalDateTime.now());
        resume.setAnalysisComplete(true);
        resume.setConfidenceScore(0.85);
    }

    private UserProfile updateProfileFromResume(String userId, ResumeDocument resume) {
        UserProfile profile = getProfile(userId);
        
        // Only update empty fields with resume data (don't overwrite user edits)
        if (isEmpty(profile.getFullName())) profile.setFullName(resume.getFullName());
        if (isEmpty(profile.getEmail())) profile.setEmail(resume.getEmail());
        if (isEmpty(profile.getContactNumber())) profile.setContactNumber(resume.getContactNumber());
        if (isEmpty(profile.getTitle())) profile.setTitle(resume.getTitle());
        if (isEmpty(profile.getSummary())) profile.setSummary(resume.getSummary());
        
        // Merge skills (avoid duplicates)
        if (profile.getTechnicalSkills() == null) profile.setTechnicalSkills(new ArrayList<>());
        if (resume.getTechnicalSkills() != null) {
            for (String skill : resume.getTechnicalSkills()) {
                if (!profile.getTechnicalSkills().contains(skill)) {
                    profile.getTechnicalSkills().add(skill);
                }
            }
        }
        
        // Merge education (only if not already present)
        if (profile.getEducation() == null) profile.setEducation(new ArrayList<>());
        if (resume.getEducation() != null) {
            for (ResumeDocument.Education edu : resume.getEducation()) {
                if (!containsEducation(profile.getEducation(), edu)) {
                    // Convert ResumeDocument.Education to UserProfile.Education
                    UserProfile.Education profileEdu = new UserProfile.Education();
                    profileEdu.setDegree(edu.getDegree());
                    profileEdu.setInstitution(edu.getInstitution());
                    profileEdu.setYear(edu.getYear());
                    profileEdu.setLocation(edu.getLocation());
                    profileEdu.setGrade(edu.getGrade());
                    profile.getEducation().add(profileEdu);
                }
            }
        }
        
        // Merge experience
        if (profile.getExperience() == null) profile.setExperience(new ArrayList<>());
        if (resume.getExperience() != null) {
            for (ResumeDocument.Experience exp : resume.getExperience()) {
                if (!containsExperience(profile.getExperience(), exp)) {
                    UserProfile.Experience profileExp = new UserProfile.Experience();
                    profileExp.setPosition(exp.getPosition());
                    profileExp.setCompany(exp.getCompany());
                    profileExp.setDuration(exp.getDuration());
                    profileExp.setDescription(exp.getDescription());
                    profileExp.setLocation(exp.getLocation());
                    profileExp.setTechnologies(exp.getTechnologies());
                    profile.getExperience().add(profileExp);
                }
            }
        }
        
        // Merge certifications
        if (profile.getCertifications() == null) profile.setCertifications(new ArrayList<>());
        if (resume.getCertifications() != null) {
            for (ResumeDocument.Certification cert : resume.getCertifications()) {
                if (!containsCertification(profile.getCertifications(), cert)) {
                    UserProfile.Certification profileCert = new UserProfile.Certification();
                    profileCert.setName(cert.getName());
                    profileCert.setIssuingOrganization(cert.getIssuingOrganization());
                    profileCert.setIssueDate(cert.getIssueDate());
                    profileCert.setExpiryDate(cert.getExpiryDate());
                    profileCert.setCredentialId(cert.getCredentialId());
                    profileCert.setCredentialUrl(cert.getCredentialUrl());
                    profile.getCertifications().add(profileCert);
                }
            }
        }
        
        profile.setUpdatedAt(LocalDateTime.now().toString());
        
        return userProfileRepository.save(profile);
    }

    // Conversion methods for analysis results
    private List<ResumeDocument.Education> convertToEducationList(List<Map<String, String>> educationMaps) {
        return educationMaps.stream()
                .map(eduMap -> ResumeDocument.Education.builder()
                        .degree(eduMap.getOrDefault("degree", ""))
                        .institution(eduMap.getOrDefault("institution", ""))
                        .year(eduMap.getOrDefault("year", ""))
                        .location("")
                        .grade("")
                        .build())
                .collect(Collectors.toList());
    }

    private List<ResumeDocument.Experience> convertToExperienceList(List<Map<String, String>> experienceMaps) {
        return experienceMaps.stream()
                .map(expMap -> ResumeDocument.Experience.builder()
                        .position(expMap.getOrDefault("position", ""))
                        .company(expMap.getOrDefault("company", ""))
                        .duration(expMap.getOrDefault("duration", ""))
                        .description(expMap.getOrDefault("description", ""))
                        .location("")
                        .technologies(new ArrayList<>())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ResumeDocument.Certification> convertToCertificationList(List<String> certificationNames) {
        return certificationNames.stream()
                .map(name -> ResumeDocument.Certification.builder()
                        .name(name)
                        .issuingOrganization("")
                        .issueDate("")
                        .expiryDate("")
                        .credentialId("")
                        .credentialUrl("")
                        .build())
                .collect(Collectors.toList());
    }

    // Helper methods for checking duplicates
    private boolean containsEducation(List<UserProfile.Education> educationList, ResumeDocument.Education education) {
        return educationList.stream()
                .anyMatch(edu -> edu.getDegree().equals(education.getDegree()) && 
                                edu.getInstitution().equals(education.getInstitution()));
    }

    private boolean containsExperience(List<UserProfile.Experience> experienceList, ResumeDocument.Experience experience) {
        return experienceList.stream()
                .anyMatch(exp -> exp.getPosition().equals(experience.getPosition()) && 
                                exp.getCompany().equals(experience.getCompany()));
    }

    private boolean containsCertification(List<UserProfile.Certification> certificationList, ResumeDocument.Certification certification) {
        return certificationList.stream()
                .anyMatch(cert -> cert.getName().equals(certification.getName()));
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String generateFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = originalFileName != null && originalFileName.contains(".") 
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : "";
        return "resume_" + timestamp + extension;
    }

    private MultipartFile createVirtualMultipartFile(ResumeDocument resume) {
        return new MultipartFile() {
            @Override public String getName() { return "file"; }
            @Override public String getOriginalFilename() { return resume.getOriginalFileName(); }
            @Override public String getContentType() { return resume.getFileType(); }
            @Override public boolean isEmpty() { return resume.getFileData() == null || resume.getFileData().length == 0; }
            @Override public long getSize() { return resume.getFileData().length; }
            @Override public byte[] getBytes() throws IOException { return resume.getFileData(); }
            @Override public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(resume.getFileData()); }
            @Override public void transferTo(File dest) throws IOException, IllegalStateException { Files.write(dest.toPath(), resume.getFileData()); }
        };
    }
    
    private final UserSkillService userSkillService;
    
    public UserProfile getUserProfile(String userId) {
        Optional<UserProfile> profile = userProfileRepository.findByUserId(userId);
        return profile.orElse(null);
    }
    
    public Map<String, Object> getSkillGapData(String userId) {
        Map<String, Object> skillGapData = new HashMap<>();
        
        try {
            // Get user profile
            UserProfile profile = getUserProfile(userId);
            if (profile == null) {
                throw new RuntimeException("User profile not found");
            }
            
            // Get user skills
            List<UserSkill> skills = userSkillService.getUserSkills(userId);
            
            // Prepare comprehensive data for ML analysis
            skillGapData.put("profile", mapProfileData(profile));
            skillGapData.put("skills", mapSkillsData(skills));
            skillGapData.put("analytics", getSkillAnalytics(skills));
            
        } catch (Exception e) {
            log.error("Error preparing skill gap data for user: {}", userId, e);
            throw new RuntimeException("Failed to prepare skill gap data", e);
        }
        
        return skillGapData;
    }
    
    private Map<String, Object> mapProfileData(UserProfile profile) {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("userId", profile.getUserId());
        profileData.put("fullName", profile.getFullName());
        profileData.put("email", profile.getEmail());
        profileData.put("contactNumber", profile.getContactNumber());
        profileData.put("location", profile.getLocation());
        profileData.put("title", profile.getTitle());
        profileData.put("summary", profile.getSummary());
        profileData.put("totalExperience", profile.getTotalExperience());
        profileData.put("education", profile.getEducation());
        profileData.put("experience", profile.getExperience());
        profileData.put("certifications", profile.getCertifications());
        profileData.put("technicalSkills", profile.getTechnicalSkills());
        profileData.put("preferredRoles", profile.getPreferredRoles());
        
        return profileData;
    }
    
    public List<Map<String, Object>> mapSkillsData(List<UserSkill> skills) {
        return skills.stream().map(skill -> {
            Map<String, Object> skillData = new HashMap<>();
            skillData.put("name", skill.getName());
            skillData.put("category", skill.getCategory());
            skillData.put("proficiency", skill.getProficiency());
            skillData.put("level", skill.getLevel());
            skillData.put("score", skill.getScore());
            skillData.put("status", skill.getStatus());
            skillData.put("verified", skill.getVerified());
            skillData.put("confidenceLevel", skill.getConfidenceLevel());
            skillData.put("experienceMonths", skill.getExperienceMonths());
            skillData.put("lastVerified", skill.getLastVerified());
            return skillData;
        }).toList();
    }
    
    public Map<String, Object> getSkillAnalytics(List<UserSkill> skills) {
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("totalSkills", skills.size());
        analytics.put("verifiedSkills", skills.stream().filter(UserSkill::getVerified).count());
        analytics.put("averageProficiency", skills.stream()
            .mapToInt(UserSkill::getProficiency)
            .average()
            .orElse(0.0));
        
        // Skill distribution by level
        Map<String, Long> levelDistribution = new HashMap<>();
        levelDistribution.put("expert", skills.stream().filter(s -> "Expert".equals(s.getLevel())).count());
        levelDistribution.put("advanced", skills.stream().filter(s -> "Advanced".equals(s.getLevel())).count());
        levelDistribution.put("intermediate", skills.stream().filter(s -> "Intermediate".equals(s.getLevel())).count());
        levelDistribution.put("beginner", skills.stream().filter(s -> "Beginner".equals(s.getLevel())).count());
        levelDistribution.put("pending", skills.stream().filter(s -> "Pending".equals(s.getLevel())).count());
        
        analytics.put("levelDistribution", levelDistribution);
        
        // Skill distribution by category
        Map<String, Long> categoryDistribution = skills.stream()
            .collect(HashMap::new, 
                (map, skill) -> map.merge(skill.getCategory(), 1L, Long::sum),
                HashMap::putAll);
        
        analytics.put("categoryDistribution", categoryDistribution);
        
        return analytics;
    }
}