package com.cp.workskillai.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "resumes")
public class ResumeDocument {
    
    @Id
    private String id;
    
    @NotBlank
    private String userId;
    
    @NotBlank
    private String fileName;
    private String originalFileName;
    
    @NotBlank
    private String fileType;
    private Long fileSize;
    
    @NotBlank
    private byte[] fileData;
    
    private LocalDateTime uploadDate;
    private LocalDateTime analyzedDate;
    
    // Extracted data matching UserProfile structure
    private String fullName;
    private String email;
    private String contactNumber;
    private String location;
    private String title;
    private String summary;
    private List<String> technicalSkills;
    private List<String> softSkills;
    private List<String> languages;
    private String totalExperience;
    
    // Using the same inner class structure as UserProfile
    private List<Education> education;
    private List<Experience> experience;
    private List<Certification> certifications;
    private List<Project> projects;
    
    private Boolean isActive;
    private Boolean analysisComplete;
    private Double confidenceScore;
    
    private String uploadSource;
    private String checksum;
    
    // Inner classes (same as in UserProfile)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Education {
        private String degree;
        private String institution;
        private String year;
        private String location;
        private String grade;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Experience {
        private String position;
        private String company;
        private String duration;
        private String description;
        private String location;
        private List<String> technologies;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Certification {
        private String name;
        private String issuingOrganization;
        private String issueDate;
        private String expiryDate;
        private String credentialId;
        private String credentialUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Project {
        private String name;
        private String description;
        private String duration;
        private List<String> technologies;
        private String projectUrl;
        private String githubUrl;
        private String role;
    }
}