package com.cp.workskillai.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "students")
public class Student {

    @Id
    private String id;

    // Personal Information
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    private LocalDate dob;

    @NotBlank(message = "Role is required")
    private String role; // "employee" or "hr"

    // Professional Information
    private String companyName; // Only for HR role
    private String currentJobRole; // Only for employee role
    private Integer yearsOfExperience;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Additional fields for profile management
    private String department;
    private List<String> skills;
    private List<String> certifications;
    private List<Education> education;
    private List<Experience> experience;
    private String summary;
    private Double academicScore;
    private List<String> enrolledCourses;
    private List<ResumeHistory> resumeHistory;
    private String currentResumeId;

    // Account status and timestamps
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // Security fields
    private String verificationToken;
    private LocalDateTime verificationTokenExpiry;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Education {
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
class Experience {
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
class ResumeHistory {
    private String resumeId;
    private String fileName;
    private LocalDateTime uploadDate;
    private LocalDateTime analyzedDate;
    private Long fileSize;
    private String fileType;
    private ResumeData extractedData;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ResumeData {
    private String fullName;
    private String email;
    private String contactNumber;
    private List<String> skills;
    private List<String> certifications;
    private List<Education> education;
    private List<Experience> experience;
    private String summary;
}