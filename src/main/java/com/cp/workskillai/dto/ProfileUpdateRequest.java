package com.cp.workskillai.dto;

import com.cp.workskillai.models.UserProfile;
import lombok.Data;

import java.util.List;

@Data
public class ProfileUpdateRequest {
    // Personal Information
    private String fullName;
    private String email;
    private String contactNumber;
    private String location;
    private String linkedInUrl;
    private String githubUrl;
    private String portfolioUrl;

    // Professional Information
    private String title;
    private String summary;
    private List<String> technicalSkills;
    private List<String> softSkills;
    private List<String> languages;
    private String totalExperience;

    // Education, Experience, Certifications, Projects
    private List<UserProfile.Education> education;
    private List<UserProfile.Experience> experience;
    private List<UserProfile.Certification> certifications;
    private List<UserProfile.Project> projects;

    // Settings
    private Boolean isPublic;
    private Boolean seekingOpportunities;
    private List<String> preferredRoles;
    private String expectedSalary;
    private String noticePeriod;
}