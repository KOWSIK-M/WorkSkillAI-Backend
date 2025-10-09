package com.cp.workskillai.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "profiles")
public class UserProfile {

    @Id
    private String id;

    @NotBlank(message = "User ID is required")
    private String userId; // Reference to Student id

    // Personal Information
    private String fullName;
    private String email;
    private String contactNumber;
    private String location;
    private String linkedInUrl;
    private String githubUrl;
    private String portfolioUrl;

    // Professional Information
    private String title; // "Full Stack Developer", "Data Scientist"
    private String summary;
    private List<String> technicalSkills;
    private List<String> softSkills;
    private List<String> languages; // Spoken languages
    private String totalExperience; // "3 years"

    // Education
    private List<Education> education;

    // Experience
    private List<Experience> experience;

    // Certifications
    private List<Certification> certifications;

    // Projects
    private List<Project> projects;

    // Resume Management
    private String currentResumeId;
    private List<ResumeHistory> resumeHistory;

    // Settings
    private Boolean isPublic;
    private Boolean seekingOpportunities;
    private List<String> preferredRoles;
    private String expectedSalary;
    private String noticePeriod;

    // Timestamps
    private String createdAt;
    private String updatedAt;

    // Inner classes (make them public static)
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeHistory {
        private String resumeId;
        private String fileName;
        private String uploadDate;
        private String analyzedDate;
        private Long fileSize;
        private String fileType;
    }
}