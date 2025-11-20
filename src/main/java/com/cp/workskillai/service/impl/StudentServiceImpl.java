package com.cp.workskillai.service.impl;

import com.cp.workskillai.dto.SignUpRequest;
import com.cp.workskillai.dto.SignUpResponse;
import com.cp.workskillai.models.Student;
import com.cp.workskillai.repository.StudentRepository;
import com.cp.workskillai.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.cp.workskillai.dto.LoginRequest;
import com.cp.workskillai.dto.LoginResponse;
import com.cp.workskillai.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse loginStudent(LoginRequest loginRequest) {
        try {
            log.info("Attempting login for email: {}", loginRequest.getEmail());

            // Find student by email
            Optional<Student> studentOpt = studentRepository.findByEmail(loginRequest.getEmail().toLowerCase());
            
            if (studentOpt.isEmpty()) {
                log.warn("Login failed: Email not found - {}", loginRequest.getEmail());
                return new LoginResponse(false, "Invalid email or password", null, null, null, null, null, null);
            }

            Student student = studentOpt.get();

            // Check if account is active
            if (!student.getIsActive()) {
                log.warn("Login failed: Account inactive - {}", loginRequest.getEmail());
                return new LoginResponse(false, "Account is deactivated", null, null, null, null, null, null);
            }

            // Check if email is verified
            if (!student.getEmailVerified()) {
                log.warn("Login failed: Email not verified - {}", loginRequest.getEmail());
                return new LoginResponse(false, "Please verify your email before logging in", null, null, null, null, null, null);
            }

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), student.getPassword())) {
                log.warn("Login failed: Invalid password for email - {}", loginRequest.getEmail());
                return new LoginResponse(false, "Invalid email or password", null, null, null, null, null, null);
            }

            // Check role if specified
            if (loginRequest.getRole() != null && !loginRequest.getRole().equals(student.getRole())) {
                log.warn("Login failed: Role mismatch for email - {}", loginRequest.getEmail());
                return new LoginResponse(false, "Invalid role for this account", null, null, null, null, null, null);
            }

            // Update last login
            student.setLastLoginAt(LocalDateTime.now());
            studentRepository.save(student);

            // Generate JWT token
            String token = jwtUtil.generateToken(student.getEmail(), student.getId(), student.getRole());

            log.info("Login successful for email: {}", loginRequest.getEmail());
            
            return new LoginResponse(
                true, 
                "Login successful", 
                token, 
                student.getId(), 
                student.getEmail(), 
                student.getRole(),
                student.getFirstName(),
                student.getLastName()
            );

        } catch (Exception e) {
            log.error("Error during login for email: {}", loginRequest.getEmail(), e);
            return new LoginResponse(false, "Login failed due to server error", null, null, null, null, null, null);
        }
    }

    @Override
    public SignUpResponse registerStudent(SignUpRequest signUpRequest) {
        try {
            log.info("Attempting to register student with email: {}", signUpRequest.getEmail());

            // Check if email already exists
            if (studentRepository.existsByEmail(signUpRequest.getEmail())) {
                log.warn("Email already exists: {}", signUpRequest.getEmail());
                return new SignUpResponse(false, "Email already registered", null, null, null);
            }

            // Validate password match
            if (!signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword())) {
                log.warn("Password confirmation mismatch for email: {}", signUpRequest.getEmail());
                return new SignUpResponse(false, "Passwords do not match", null, null, null);
            }

            // Validate role-specific fields
            if ("hr".equals(signUpRequest.getRole()) && 
                (signUpRequest.getCompanyName() == null || signUpRequest.getCompanyName().trim().isEmpty())) {
                return new SignUpResponse(false, "Company name is required for HR role", null, null, null);
            }

            if ("employee".equals(signUpRequest.getRole()) && 
                (signUpRequest.getCurrentJobRole() == null || signUpRequest.getCurrentJobRole().trim().isEmpty())) {
                return new SignUpResponse(false, "Current job role is required for Employee role", null, null, null);
            }

            // Create student entity
            Student student = Student.builder()
                    .firstName(signUpRequest.getFirstName())
                    .lastName(signUpRequest.getLastName())
                    .email(signUpRequest.getEmail().toLowerCase())
                    .phoneNumber(signUpRequest.getPhoneNumber())
                    .dob(signUpRequest.getDob())
                    .role(signUpRequest.getRole())
                    .companyName(signUpRequest.getCompanyName())
                    .currentJobRole(signUpRequest.getCurrentJobRole())
                    .yearsOfExperience(signUpRequest.getYearsOfExperience())
                    .password(passwordEncoder.encode(signUpRequest.getPassword()))
                    .department("Computer Science") // Default, can be updated later
                    .isActive(true)
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .verificationToken(generateVerificationToken())
                    .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                    .build();

            // Save student
            Student savedStudent = studentRepository.save(student);
            log.info("Student registered successfully with ID: {}", savedStudent.getId());

            // Send verification email (implement this method)
            sendVerificationEmail(savedStudent);

            return new SignUpResponse(true, "Registration successful. Please check your email for verification.", 
                                    savedStudent.getId(), savedStudent.getEmail(), savedStudent.getRole());

        } catch (Exception e) {
            log.error("Error during student registration for email: {}", signUpRequest.getEmail(), e);
            return new SignUpResponse(false, "Registration failed due to server error", null, null, null);
        }
    }

    @Override
    public boolean verifyEmail(String token) {
        try {
            Optional<Student> studentOpt = studentRepository.findByVerificationToken(token);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                
                // Check if token is expired
                if (student.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
                    log.warn("Verification token expired for student: {}", student.getEmail());
                    return false;
                }

                // Update student
                student.setEmailVerified(true);
                student.setVerificationToken(null);
                student.setVerificationTokenExpiry(null);
                student.setUpdatedAt(LocalDateTime.now());
                
                studentRepository.save(student);
                log.info("Email verified successfully for student: {}", student.getEmail());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error during email verification for token: {}", token, e);
            return false;
        }
    }

    // Helper methods
    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    private void sendVerificationEmail(Student student) {
        // Implement email sending logic here
        // You can use JavaMailSender, SendGrid, or any other email service
        log.info("Sending verification email to: {}", student.getEmail());
        // Example: emailService.sendVerificationEmail(student.getEmail(), student.getVerificationToken());
    }

    // Other method implementations
    @Override
    public boolean resendVerificationEmail(String email) {
        // Implementation for resending verification email
        return false;
    }

    @Override
    public boolean initiatePasswordReset(String email) {
        // Implementation for password reset initiation
        return false;
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        // Implementation for password reset
        return false;
    }

    @Override
    public Student getStudentProfile(String studentId) {
        return studentRepository.findById(studentId).orElse(null);
    }

    @Override
    public Student updateStudentProfile(String studentId, Student student) {
        // Implementation for profile update
        return null;
    }
    
    public Student getStudentById(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }
}